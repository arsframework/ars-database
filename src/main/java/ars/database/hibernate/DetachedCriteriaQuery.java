package ars.database.hibernate;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;

import org.hibernate.Session;
import org.hibernate.Criteria;
import org.hibernate.type.Type;
import org.hibernate.sql.JoinType;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Subqueries;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.metadata.ClassMetadata;

import ars.util.Beans;
import ars.util.Strings;
import ars.util.Conditions;
import ars.util.Conditions.Or;
import ars.util.Conditions.And;
import ars.util.Conditions.Logic;
import ars.util.Conditions.Condition;
import ars.database.repository.Query;
import ars.database.hibernate.Hibernates;
import ars.database.repository.Repositories;

/**
 * Hibernate离线查询实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public class DetachedCriteriaQuery<T> implements Query<T> {
	private int page; // 分页页码
	private int size; // 分页数量
	private T object; // 单个实例
	private List<?> stats; // 统计数据列表
	private List<T> objects; // 实例列表
	private Integer count; // 实例数量
	private Class<T> model; // 数据模型
	private boolean loaded; // 数据是否已加载
	private boolean subquery; // 是否需要子查询
	private DetachedCriteria criteria; // 离线查询对象
	private SessionFactory sessionFactory; // 会话工厂对象
	private List<String> orders = new LinkedList<String>();
	private ProjectionList projections = Projections.projectionList();
	private Map<String, String> aliases = new HashMap<String, String>();

	public DetachedCriteriaQuery(SessionFactory sessionFactory, Class<T> model) {
		if (sessionFactory == null) {
			throw new IllegalArgumentException("Illegal sessionFactory:" + sessionFactory);
		}
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		this.model = model;
		this.sessionFactory = sessionFactory;
		this.criteria = DetachedCriteria.forClass(model);
	}

	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	public DetachedCriteria getDetachedCriteria() {
		return this.criteria;
	}

	/**
	 * 获取在线查询对象
	 * 
	 * @param session
	 *            持久化会话对象
	 * @return 在线查询对象
	 */
	protected Criteria getExecutableCriteria(Session session) {
		if (this.subquery) {
			String primary = Repositories.getPrimary(this.getModel());
			this.criteria = DetachedCriteria.forClass(this.getModel())
					.add(Subqueries.propertyIn(primary, this.criteria.setProjection(Property.forName(primary))));
			this.aliases.clear();
		}
		for (String property : this.orders) {
			boolean asc = property.charAt(0) == '+';
			String alias = this.getCriteriaAlias(property.substring(1), JoinType.LEFT_OUTER_JOIN);
			if (asc) {
				this.criteria.addOrder(Order.asc(alias));
			} else {
				this.criteria.addOrder(Order.desc(alias));
			}
		}
		Criteria criteria = this.criteria.getExecutableCriteria(session)
				.setResultTransformer(DetachedCriteria.ROOT_ENTITY);
		int index = (this.page - 1) * this.size;
		if (index > 0) {
			criteria.setFirstResult(index);
		}
		if (this.size > 0) {
			criteria.setMaxResults(this.size);
		}
		return criteria;
	}

	/**
	 * 获取属性查询描述
	 * 
	 * @param property
	 *            属性名称，可使用“.”号隔开
	 * @param 外键关联类型
	 * @return 描述名称
	 */
	private String _getCriteriaAlias(String property, JoinType joinType) {
		String alias = this.aliases.get(property);
		if (alias == null) {
			int index = property.lastIndexOf('.');
			if (index <= 0) {
				alias = property + this.aliases.size();
				this.criteria.createAlias(property, alias, joinType);
			} else {
				alias = property.substring(index + 1) + this.aliases.size();
				this.criteria.createAlias(
						this._getCriteriaAlias(property.substring(0, index), joinType) + property.substring(index),
						alias);
			}
			this.aliases.put(property, alias);
		}
		return alias;
	}

	/**
	 * 获取属性查询描述
	 * 
	 * @param property
	 *            属性名称，可使用“.”号隔开
	 * @return 描述名称
	 */
	protected String getCriteriaAlias(String property) {
		return this.getCriteriaAlias(property, JoinType.LEFT_OUTER_JOIN);
	}

	/**
	 * 获取属性查询描述
	 * 
	 * @param property
	 *            属性名称，可使用“.”号隔开
	 * @param joinType
	 *            外键关联类型
	 * @return 描述名称
	 */
	protected String getCriteriaAlias(String property, JoinType joinType) {
		int index = property.lastIndexOf('.');
		if (index <= 0) {
			return property;
		}
		return this._getCriteriaAlias(property.substring(0, index), joinType) + property.substring(index);
	}

	/**
	 * 条件包装对象
	 * 
	 * @author yongqiangwu
	 * 
	 */
	class ConditionWrapper {
		private String property; // 条件名称
		private Object value; // 条件值

		public ConditionWrapper(String property, Object value) {
			this.property = property;
			this.value = value;
		}

		public String getProperty() {
			return property;
		}

		public Object getValue() {
			return value;
		}

	}

	/**
	 * 获取条件包装对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件包装对象实例
	 */
	protected ConditionWrapper getConditionWrapper(String property, Object value) {
		if (!this.subquery) {
			int sign = property.indexOf('.');
			this.subquery = Hibernates
					.getPropertyType(this.sessionFactory, this.model, sign > 0 ? property.substring(0, sign) : property)
					.isCollectionType();
		}
		Type type = Hibernates.getPropertyType(this.sessionFactory, this.model, property);
		Class<?> meta = Hibernates.getPropertyTypeClass(this.sessionFactory, type);
		if (type.isEntityType() || type.isCollectionType()) {
			ClassMetadata metadata = Hibernates.getClassMetadata(this.sessionFactory, meta);
			Class<?> primaryType = metadata.getIdentifierType().getReturnedClass();
			property = new StringBuilder(property).append('.').append(metadata.getIdentifierPropertyName()).toString();
			if (value instanceof Collection || value instanceof Object[]) {
				Collection<?> values = value instanceof Collection ? (Collection<?>) value
						: Arrays.asList((Object[]) value);
				List<Object> converts = new ArrayList<Object>(values.size());
				for (Object v : values) {
					if (meta.isAssignableFrom(v.getClass())) {
						converts.add(Hibernates.getIdentifier(this.sessionFactory, v));
					} else {
						converts.add(Beans.toObject(primaryType, v));
					}
				}
				return new ConditionWrapper(property, converts);
			} else if (value != null && meta.isAssignableFrom(value.getClass())) {
				return new ConditionWrapper(property, Hibernates.getIdentifier(this.sessionFactory, value));
			}
			return new ConditionWrapper(property, Beans.toObject(primaryType, value));
		} else if (value instanceof Collection || value instanceof Object[]) {
			Collection<?> values = value instanceof Collection ? (Collection<?>) value
					: Arrays.asList((Object[]) value);
			List<Object> converts = new ArrayList<Object>(values.size());
			for (Object v : values) {
				converts.add(Beans.toObject(meta, v));
			}
			return new ConditionWrapper(property, converts);
		}
		return new ConditionWrapper(property, Beans.toObject(meta, value));
	}

	/**
	 * 获取空条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @return 条件匹配对象
	 */
	protected Criterion getEmptyCriterion(String property) {
		if (Hibernates.getPropertyType(this.sessionFactory, this.model, property).isCollectionType()) {
			return Restrictions.isEmpty(this.getCriteriaAlias(property));
		}
		return Restrictions.isNull(this.getCriteriaAlias(property));
	}

	/**
	 * 获取空条件匹配对象
	 * 
	 * @param properties
	 *            属性名称列表
	 * @return 条件匹配对象
	 */
	protected Criterion getEmptyCriterion(Collection<String> properties) {
		int i = 0;
		Criterion[] criterions = new Criterion[properties.size()];
		for (String property : properties) {
			if (Hibernates.getPropertyType(this.sessionFactory, this.model, property).isCollectionType()) {
				criterions[i++] = Restrictions.isEmpty(this.getCriteriaAlias(property));
			} else {
				criterions[i++] = Restrictions.isNull(this.getCriteriaAlias(property));
			}
		}
		return criterions.length == 1 ? criterions[0] : Restrictions.and(criterions);
	}

	/**
	 * 获取非空条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @return 条件匹配对象
	 */
	protected Criterion getNonemptyCriterion(String property) {
		if (Hibernates.getPropertyType(this.sessionFactory, this.model, property).isCollectionType()) {
			return Restrictions.isNotEmpty(this.getCriteriaAlias(property));
		}
		return Restrictions.isNotNull(this.getCriteriaAlias(property));
	}

	/**
	 * 获取非空条件匹配对象
	 * 
	 * @param properties
	 *            属性名称列表
	 * @return 条件匹配对象
	 */
	protected Criterion getNonemptyCriterion(Collection<String> properties) {
		int i = 0;
		Criterion[] criterions = new Criterion[properties.size()];
		for (String property : properties) {
			if (Hibernates.getPropertyType(this.sessionFactory, this.model, property).isCollectionType()) {
				criterions[i++] = Restrictions.isNotEmpty(this.getCriteriaAlias(property));
			} else {
				criterions[i++] = Restrictions.isNotNull(this.getCriteriaAlias(property));
			}
		}
		return criterions.length == 1 ? criterions[0] : Restrictions.and(criterions);
	}

	/**
	 * 获取等于条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getEqualCriterion(String property, Object value) {
		ConditionWrapper condition = this.getConditionWrapper(property, value);
		return Restrictions.eq(this.getCriteriaAlias(condition.getProperty()), condition.getValue());
	}

	/**
	 * 获取不等于条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getNotEqualCriterion(String property, Object value) {
		ConditionWrapper condition = this.getConditionWrapper(property, value);
		return Restrictions.ne(this.getCriteriaAlias(condition.getProperty()), condition.getValue());
	}

	/**
	 * 获取大于条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getGreaterCriterion(String property, Object value) {
		ConditionWrapper condition = this.getConditionWrapper(property, value);
		return Restrictions.gt(this.getCriteriaAlias(condition.getProperty()), condition.getValue());
	}

	/**
	 * 获取大于或等于条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getGreaterEqualCriterion(String property, Object value) {
		ConditionWrapper condition = this.getConditionWrapper(property, value);
		return Restrictions.ge(this.getCriteriaAlias(condition.getProperty()), condition.getValue());
	}

	/**
	 * 获取小于条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getLessCriterion(String property, Object value) {
		ConditionWrapper condition = this.getConditionWrapper(property, value);
		return Restrictions.lt(this.getCriteriaAlias(condition.getProperty()), condition.getValue());
	}

	/**
	 * 获取小于或等于条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getLessEqualCriterion(String property, Object value) {
		ConditionWrapper condition = this.getConditionWrapper(property, value);
		return Restrictions.le(this.getCriteriaAlias(condition.getProperty()), condition.getValue());
	}

	/**
	 * 获取区间条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param low
	 *            最小值
	 * @param high
	 *            最大值
	 * @return 条件匹配对象
	 */
	protected Criterion getBetweenCriterion(String property, Object low, Object high) {
		ConditionWrapper condition = this.getConditionWrapper(property, Arrays.asList(low, high));
		List<?> values = (List<?>) condition.getValue();
		return Restrictions.between(this.getCriteriaAlias(condition.getProperty()), values.get(0), values.get(1));
	}

	/**
	 * 获取模糊查询开始条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getStartCriterion(String property, String value) {
		ConditionWrapper condition = this.getConditionWrapper(property, null);
		return Restrictions.ilike(this.getCriteriaAlias(condition.getProperty()), value, MatchMode.START);
	}

	/**
	 * 获取模糊查询开始条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getStartCriterion(String property, Collection<String> values) {
		int i = 0;
		ConditionWrapper condition = this.getConditionWrapper(property, null);
		String alias = this.getCriteriaAlias(condition.getProperty());
		Criterion[] criterions = new Criterion[values.size()];
		for (String value : values) {
			criterions[i++] = Restrictions.ilike(alias, value, MatchMode.START);
		}
		return values.size() == 1 ? criterions[0] : Restrictions.or(criterions);
	}

	/**
	 * 获取排除模糊查询开始条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getNstartCriterion(String property, String value) {
		return Restrictions.not(this.getStartCriterion(property, value));
	}

	/**
	 * 获取排除模糊查询开始条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getNstartCriterion(String property, Collection<String> values) {
		return Restrictions.not(this.getStartCriterion(property, values));
	}

	/**
	 * 获取模糊查询结束结束条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getEndCriterion(String property, String value) {
		ConditionWrapper condition = this.getConditionWrapper(property, null);
		return Restrictions.ilike(this.getCriteriaAlias(condition.getProperty()), value, MatchMode.END);
	}

	/**
	 * 获取模糊查询结束结束条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getEndCriterion(String property, Collection<String> values) {
		int i = 0;
		ConditionWrapper condition = this.getConditionWrapper(property, null);
		String alias = this.getCriteriaAlias(condition.getProperty());
		Criterion[] criterions = new Criterion[values.size()];
		for (String value : values) {
			criterions[i++] = Restrictions.ilike(alias, value, MatchMode.END);
		}
		return values.size() == 1 ? criterions[0] : Restrictions.or(criterions);
	}

	/**
	 * 排除模糊查询结束结束条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getNendCriterion(String property, String value) {
		return Restrictions.not(this.getEndCriterion(property, value));
	}

	/**
	 * 排除模糊查询结束结束条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getNendCriterion(String property, Collection<String> values) {
		return Restrictions.not(this.getEndCriterion(property, values));
	}

	/**
	 * 获取模糊查询条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getLikeCriterion(String property, String value) {
		ConditionWrapper condition = this.getConditionWrapper(property, null);
		return Restrictions.ilike(this.getCriteriaAlias(condition.getProperty()), value, MatchMode.ANYWHERE);
	}

	/**
	 * 获取模糊查询条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getLikeCriterion(String property, Collection<String> values) {
		int i = 0;
		ConditionWrapper condition = this.getConditionWrapper(property, null);
		String alias = this.getCriteriaAlias(condition.getProperty());
		Criterion[] criterions = new Criterion[values.size()];
		for (String value : values) {
			criterions[i++] = Restrictions.ilike(alias, value, MatchMode.ANYWHERE);
		}
		return values.size() == 1 ? criterions[0] : Restrictions.or(criterions);
	}

	/**
	 * 排除模糊查询条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	protected Criterion getNlikeCriterion(String property, String value) {
		return Restrictions.not(this.getLikeCriterion(property, value));
	}

	/**
	 * 排除模糊查询条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getNlikeCriterion(String property, Collection<String> values) {
		return Restrictions.not(this.getLikeCriterion(property, values));
	}

	/**
	 * 获取区间条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getInCriterion(String property, Collection<?> values) {
		ConditionWrapper condition = this.getConditionWrapper(property, values);
		return Restrictions.in(this.getCriteriaAlias(condition.getProperty()),
				((List<?>) condition.getValue()).toArray());
	}

	/**
	 * 获取或条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值集合
	 * @return 条件匹配对象
	 */
	protected Criterion getOrCriterion(String property, Collection<?> values) {
		Criterion[] criterions = new Criterion[values.size()];
		ConditionWrapper condition = this.getConditionWrapper(property, values);
		String alias = this.getCriteriaAlias(condition.getProperty());
		List<?> converts = (List<?>) condition.getValue();
		for (int i = 0; i < converts.size(); i++) {
			criterions[i] = Restrictions.eq(alias, converts.get(i));
		}
		return Restrictions.or(criterions);
	}

	/**
	 * 获取非条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param values
	 *            属性值数组
	 * @return 条件匹配对象
	 */
	protected Criterion getNotCriterion(String property, Collection<?> values) {
		return Restrictions.not(this.getOrCriterion(property, values));
	}

	/**
	 * 获取属性等于匹配条件
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 条件匹配对象
	 */
	protected Criterion getPropertyEqualCriterion(String property, String other) {
		return Restrictions.eqProperty(this.getCriteriaAlias(property), this.getCriteriaAlias(other));
	}

	/**
	 * 获取属性不等于匹配条件
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 条件匹配对象
	 */
	protected Criterion getPropertyNotEqualCriterion(String property, String other) {
		return Restrictions.neProperty(this.getCriteriaAlias(property), this.getCriteriaAlias(other));
	}

	/**
	 * 获取属性小于匹配条件
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 条件匹配对象
	 */
	protected Criterion getPropertyLessCriterion(String property, String other) {
		return Restrictions.ltProperty(this.getCriteriaAlias(property), this.getCriteriaAlias(other));
	}

	/**
	 * 获取属性小于或等于匹配条件
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 条件匹配对象
	 */
	protected Criterion getPropertyLessEqualCriterion(String property, String other) {
		return Restrictions.leProperty(this.getCriteriaAlias(property), this.getCriteriaAlias(other));
	}

	/**
	 * 获取属性大于匹配条件
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 条件匹配对象
	 */
	protected Criterion getPropertyGreaterCriterion(String property, String other) {
		return Restrictions.gtProperty(this.getCriteriaAlias(property), this.getCriteriaAlias(other));
	}

	/**
	 * 获取属性大于或等于匹配条件
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 条件匹配对象
	 */
	protected Criterion getPropertyGreaterEqualCriterion(String property, String other) {
		return Restrictions.geProperty(this.getCriteriaAlias(property), this.getCriteriaAlias(other));
	}

	/**
	 * 获取自定义条件匹配对象
	 * 
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 * @return 条件匹配对象
	 */
	@SuppressWarnings("unchecked")
	protected Criterion getConditionCriterion(String property, Object value) {
		String key = property, handle = EQ;
		int index = key.indexOf(DELIMITER);
		if (index > 0) {
			handle = key.substring(index + DELIMITER.length()).toLowerCase();
			key = key.substring(0, index);
		}
		if (handle.equals(EMPTY)) {
			return this.getEmptyCriterion(key);
		} else if (handle.equals(NOT_EMPTY)) {
			return this.getNonemptyCriterion(key);
		} else if (Beans.isEmpty(value)) {
			return null;
		}
		if (handle.equals(START)) {
			if (value instanceof Collection) {
				return this.getStartCriterion(key, (Collection<String>) value);
			} else if (value instanceof String[]) {
				return this.getStartCriterion(key, Arrays.asList((String[]) value));
			}
			return this.getStartCriterion(key, value.toString());
		} else if (handle.equals(NOT_START)) {
			if (value instanceof Collection) {
				return this.getNstartCriterion(key, (Collection<String>) value);
			} else if (value instanceof String[]) {
				return this.getNstartCriterion(key, Arrays.asList((String[]) value));
			}
			return this.getNstartCriterion(key, value.toString());
		} else if (handle.equals(END)) {
			if (value instanceof Collection) {
				return this.getEndCriterion(key, (Collection<String>) value);
			} else if (value instanceof String[]) {
				return this.getEndCriterion(key, Arrays.asList((String[]) value));
			}
			return this.getEndCriterion(key, value.toString());
		} else if (handle.equals(NOT_END)) {
			if (value instanceof Collection) {
				return this.getNendCriterion(key, (Collection<String>) value);
			} else if (value instanceof String[]) {
				return this.getNendCriterion(key, Arrays.asList((String[]) value));
			}
			return this.getNendCriterion(key, value.toString());
		} else if (handle.equals(LIKE)) {
			if (value instanceof Collection) {
				return this.getLikeCriterion(key, (Collection<String>) value);
			} else if (value instanceof String[]) {
				return this.getLikeCriterion(key, Arrays.asList((String[]) value));
			}
			return this.getLikeCriterion(key, value.toString());
		} else if (handle.equals(NOT_LIKE)) {
			if (value instanceof Collection) {
				return this.getNlikeCriterion(key, (Collection<String>) value);
			} else if (value instanceof String[]) {
				return this.getNlikeCriterion(key, Arrays.asList((String[]) value));
			}
			return this.getNlikeCriterion(key, value.toString());
		} else if (handle.equals(EQ)) {
			return this.getEqualCriterion(key, value);
		} else if (handle.equals(NE)) {
			return this.getNotEqualCriterion(key, value);
		} else if (handle.equals(GT)) {
			return this.getGreaterCriterion(key, value);
		} else if (handle.equals(GE)) {
			return this.getGreaterEqualCriterion(key, value);
		} else if (handle.equals(LT)) {
			return this.getLessCriterion(key, value);
		} else if (handle.equals(LE)) {
			return this.getLessEqualCriterion(key, value);
		} else if (handle.equals(IN)) {
			if (value instanceof Collection) {
				Collection<?> values = (Collection<?>) value;
				if (values.size() == 1) {
					return this.getEqualCriterion(key, values.iterator().next());
				}
				return this.getInCriterion(key, values);
			} else if (value instanceof Object[]) {
				Object[] values = (Object[]) value;
				if (values.length == 1) {
					return this.getEqualCriterion(key, values[0]);
				}
				return this.getInCriterion(key, Arrays.asList(values));
			}
			return this.getEqualCriterion(key, value);
		} else if (handle.equals(OR)) {
			if (value instanceof Collection) {
				Collection<?> values = (Collection<?>) value;
				if (values.size() == 1) {
					return this.getEqualCriterion(key, values.iterator().next());
				}
				return this.getOrCriterion(key, values);
			} else if (value instanceof Object[]) {
				Object[] values = (Object[]) value;
				if (values.length == 1) {
					return this.getEqualCriterion(key, values[0]);
				}
				return this.getOrCriterion(key, Arrays.asList(values));
			}
			return this.getEqualCriterion(key, value);
		} else if (handle.equals(NOT)) {
			if (value instanceof Collection) {
				Collection<?> values = (Collection<?>) value;
				if (values.size() == 1) {
					return this.getNotEqualCriterion(key, values.iterator().next());
				}
				return this.getNotCriterion(key, values);
			} else if (value instanceof Object[]) {
				Object[] values = (Object[]) value;
				if (values.length == 1) {
					return this.getNotEqualCriterion(key, values[0]);
				}
				return this.getNotCriterion(key, Arrays.asList(values));
			}
			return this.getNotEqualCriterion(key, value);
		} else if (handle.equals(PEQ)) {
			return this.getPropertyEqualCriterion(key, value.toString());
		} else if (handle.equals(PNE)) {
			return this.getPropertyNotEqualCriterion(key, value.toString());
		} else if (handle.equals(PLT)) {
			return this.getPropertyLessCriterion(key, value.toString());
		} else if (handle.equals(PLE)) {
			return this.getPropertyLessEqualCriterion(key, value.toString());
		} else if (handle.equals(PGT)) {
			return this.getPropertyGreaterCriterion(key, value.toString());
		} else if (handle.equals(PGE)) {
			return this.getPropertyGreaterEqualCriterion(key, value.toString());
		}
		throw new RuntimeException("Not support query property:" + property);
	}

	/**
	 * 获取自定义条件匹配对象
	 * 
	 * @param logic
	 *            条件逻辑对象
	 * @return 条件匹配对象
	 */
	protected Criterion getConditionCriterion(Logic logic) {
		if (logic == null) {
			return null;
		} else if (logic instanceof Or) {
			Logic[] logics = ((Or) logic).getLogics();
			if (logics.length == 1) {
				return this.getConditionCriterion(logics[0]);
			}
			List<Criterion> criterions = new LinkedList<Criterion>();
			for (int i = 0; i < logics.length; i++) {
				Criterion criterion = this.getConditionCriterion(logics[i]);
				if (criterion != null) {
					criterions.add(criterion);
				}
			}
			return criterions.isEmpty() ? null : Restrictions.or(criterions.toArray(new Criterion[0]));
		} else if (logic instanceof And) {
			Logic[] logics = ((And) logic).getLogics();
			if (logics.length == 1) {
				return this.getConditionCriterion(logics[0]);
			}
			List<Criterion> criterions = new LinkedList<Criterion>();
			for (int i = 0; i < logics.length; i++) {
				Criterion criterion = this.getConditionCriterion(logics[i]);
				if (criterion != null) {
					criterions.add(criterion);
				}
			}
			return criterions.isEmpty() ? null : Restrictions.and(criterions.toArray(new Criterion[0]));
		} else if (logic instanceof Condition) {
			Condition condition = (Condition) logic;
			return this.getConditionCriterion(condition.getKey(), condition.getValue());
		}
		throw new RuntimeException("Not support query logic:" + logic);
	}

	/**
	 * 根据属性名称排序（倒叙以“-”号开头）
	 * 
	 * @param property
	 *            属性名称
	 */
	protected void order(String property) {
		if (property != null && !property.isEmpty()) {
			boolean desc = property.charAt(0) == '-';
			String name = desc || property.charAt(0) == '+' ? property.substring(1) : property;
			if (desc) {
				this.desc(name);
			} else {
				this.asc(name);
			}
		}
	}

	@Override
	public Iterator<T> iterator() {
		return this.list().iterator();
	}

	@Override
	public Class<T> getModel() {
		return this.model;
	}

	@Override
	public Query<T> empty(String... properties) {
		if (properties != null && properties.length > 0) {
			if (properties.length == 1) {
				this.criteria.add(this.getEmptyCriterion(properties[0]));
			} else {
				this.criteria.add(this.getEmptyCriterion(Arrays.asList(properties)));
			}
		}
		return this;
	}

	@Override
	public Query<T> nonempty(String... properties) {
		if (properties != null && properties.length > 0) {
			if (properties.length == 1) {
				this.criteria.add(this.getNonemptyCriterion(properties[0]));
			} else {
				this.criteria.add(this.getNonemptyCriterion(Arrays.asList(properties)));
			}
		}
		return this;
	}

	@Override
	public Query<T> eq(String property, Object value) {
		if (property != null && value != null) {
			this.criteria.add(this.getEqualCriterion(property, value));
		}
		return this;
	}

	@Override
	public Query<T> ne(String property, Object value) {
		if (property != null && value != null) {
			this.criteria.add(this.getNotEqualCriterion(property, value));
		}
		return this;
	}

	@Override
	public Query<T> gt(String property, Object value) {
		if (property != null && value != null) {
			this.criteria.add(this.getGreaterCriterion(property, value));
		}
		return this;
	}

	@Override
	public Query<T> ge(String property, Object value) {
		if (property != null && value != null) {
			this.criteria.add(this.getGreaterEqualCriterion(property, value));
		}
		return this;
	}

	@Override
	public Query<T> lt(String property, Object value) {
		if (property != null && value != null) {
			this.criteria.add(this.getLessCriterion(property, value));
		}
		return this;
	}

	@Override
	public Query<T> le(String property, Object value) {
		if (property != null && value != null) {
			this.criteria.add(this.getLessEqualCriterion(property, value));
		}
		return this;
	}

	@Override
	public Query<T> between(String property, Object low, Object high) {
		if (property != null && low != null && high != null) {
			this.criteria.add(this.getBetweenCriterion(property, low, high));
		}
		return this;
	}

	@Override
	public Query<T> start(String property, String... values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getStartCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getStartCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> nstart(String property, String... values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getNstartCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getNstartCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> end(String property, String... values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getEndCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getEndCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> nend(String property, String... values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getNendCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getNendCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> like(String property, String... values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getLikeCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getLikeCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> nlike(String property, String... values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getNlikeCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getNlikeCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> in(String property, Object[] values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getEqualCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getInCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> or(String property, Object[] values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getEqualCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getOrCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@Override
	public Query<T> not(String property, Object[] values) {
		if (property != null && values != null && values.length > 0) {
			if (values.length == 1) {
				this.criteria.add(this.getNotEqualCriterion(property, values[0]));
			} else {
				this.criteria.add(this.getNotCriterion(property, Arrays.asList(values)));
			}
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Query<T> custom(String property, Object value) {
		if (Strings.isEmpty(property)) {
			return this;
		}
		String lproperty = property.toLowerCase();
		if (lproperty.equals(MIN)) {
			if (!Beans.isEmpty(value)) {
				this.min(Beans.toArray(String.class, value));
			}
		} else if (lproperty.equals(MAX)) {
			if (!Beans.isEmpty(value)) {
				this.max(Beans.toArray(String.class, value));
			}
		} else if (lproperty.equals(SUM)) {
			if (!Beans.isEmpty(value)) {
				this.sum(Beans.toArray(String.class, value));
			}
		} else if (lproperty.equals(AVG)) {
			if (!Beans.isEmpty(value)) {
				this.avg(Beans.toArray(String.class, value));
			}
		} else if (lproperty.equals(GROUP)) {
			if (!Beans.isEmpty(value)) {
				this.group(Beans.toArray(String.class, value));
			}
		} else if (lproperty.equals(NUMBER)) {
			if (!Beans.isEmpty(value)) {
				this.number(Beans.toArray(String.class, value));
			}
		} else if (lproperty.equals(PROPERTY)) {
			if (!Beans.isEmpty(value)) {
				this.property(Beans.toArray(String.class, value));
			}
		} else if (lproperty.equals(PAGE)) {
			if (!Beans.isEmpty(value)) {
				this.page = Integer.parseInt(value.toString());
			}
		} else if (lproperty.equals(SIZE)) {
			if (!Beans.isEmpty(value)) {
				this.size = Integer.parseInt(value.toString());
			}
		} else if (lproperty.equals(ORDER)) {
			if (value instanceof Collection) {
				for (String order : (Collection<String>) value) {
					this.order(order);
				}
			} else if (value instanceof String[]) {
				for (String order : (String[]) value) {
					this.order(order);
				}
			} else if (!Beans.isEmpty(value)) {
				this.order(value.toString());
			}
		} else if (lproperty.equals(CONDITION)) {
			if (!Beans.isEmpty(value)) {
				Logic logic = value instanceof Logic ? (Logic) value : Conditions.parse(value.toString());
				this.condition(logic);
			}
		} else {
			this.condition(property, value);
		}
		return this;
	}

	@Override
	public Query<T> custom(Map<String, Object> parameters) {
		if (parameters != null && !parameters.isEmpty()) {
			for (Entry<String, Object> entry : parameters.entrySet()) {
				this.custom(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}

	@Override
	public Query<T> condition(Logic logic) {
		if (logic != null) {
			Criterion criterion = this.getConditionCriterion(logic);
			if (criterion != null) {
				this.criteria.add(criterion);
			}
		}
		return this;
	}

	@Override
	public Query<T> condition(String property, Object value) {
		if (property != null) {
			Criterion criterion = this.getConditionCriterion(property, value);
			if (criterion != null) {
				this.criteria.add(criterion);
			}
		}
		return this;
	}

	@Override
	public Query<T> condition(Map<String, Object> parameters) {
		if (parameters != null && !parameters.isEmpty()) {
			for (Entry<String, Object> entry : parameters.entrySet()) {
				this.condition(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}

	@Override
	public Query<T> eqProperty(String property, String other) {
		if (property != null && other != null) {
			this.criteria.add(this.getPropertyEqualCriterion(property, other));
		}
		return this;
	}

	@Override
	public Query<T> neProperty(String property, String other) {
		if (property != null && other != null) {
			this.criteria.add(this.getPropertyNotEqualCriterion(property, other));
		}
		return this;
	}

	@Override
	public Query<T> ltProperty(String property, String other) {
		if (property != null && other != null) {
			this.criteria.add(this.getPropertyLessCriterion(property, other));
		}
		return this;
	}

	@Override
	public Query<T> leProperty(String property, String other) {
		if (property != null && other != null) {
			this.criteria.add(this.getPropertyLessEqualCriterion(property, other));
		}
		return this;
	}

	@Override
	public Query<T> gtProperty(String property, String other) {
		if (property != null && other != null) {
			this.criteria.add(this.getPropertyGreaterCriterion(property, other));
		}
		return this;
	}

	@Override
	public Query<T> geProperty(String property, String other) {
		if (property != null && other != null) {
			this.criteria.add(this.getPropertyGreaterEqualCriterion(property, other));
		}
		return this;
	}

	@Override
	public Query<T> asc(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.orders.add(new StringBuilder("+").append(property).toString());
			}
		}
		return this;
	}

	@Override
	public Query<T> desc(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.orders.add(new StringBuilder("-").append(property).toString());
			}
		}
		return this;
	}

	@Override
	public Query<T> paging(int page, int size) {
		if (page < 1) {
			throw new IllegalArgumentException("Illegal page:" + page);
		}
		if (size < 1) {
			throw new IllegalArgumentException("Illegal size:" + size);
		}
		this.page = page;
		this.size = size;
		return this;
	}

	@Override
	public Query<T> min(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.projections.add(Projections.min(this.getCriteriaAlias(property)));
			}
		}
		return this;
	}

	@Override
	public Query<T> max(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.projections.add(Projections.max(this.getCriteriaAlias(property)));
			}
		}
		return this;
	}

	@Override
	public Query<T> avg(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.projections.add(Projections.avg(this.getCriteriaAlias(property)));
			}
		}
		return this;
	}

	@Override
	public Query<T> sum(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.projections.add(Projections.sum(this.getCriteriaAlias(property)));
			}
		}
		return this;
	}

	@Override
	public Query<T> number(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.projections.add(Projections.count(this.getCriteriaAlias(property)));
			}
		}
		return this;
	}

	@Override
	public Query<T> group(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.projections.add(Projections.groupProperty(this.getCriteriaAlias(property)));
			}
		}
		return this;
	}

	@Override
	public Query<T> property(String... properties) {
		if (properties != null && properties.length > 0) {
			for (String property : properties) {
				this.projections.add(Projections.property(this.getCriteriaAlias(property)));
			}
		}
		return this;
	}

	@Override
	public int count() {
		if (this.count == null) {
			if (this.loaded) {
				this.count = this.objects.size();
			} else {
				Session session = this.sessionFactory.openSession();
				try {
					this.count = (int) ((Long) this.getExecutableCriteria(session).setProjection(Projections.rowCount())
							.uniqueResult()).longValue();
				} finally {
					session.close();
				}
			}
		}
		return this.count;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T single() {
		if (!this.loaded) {
			this.loaded = true;
			Session session = this.sessionFactory.openSession();
			try {
				this.object = (T) this.getExecutableCriteria(session).uniqueResult();
			} finally {
				session.close();
			}
		}
		return this.object;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> list() {
		if (!this.loaded) {
			this.loaded = true;
			Session session = this.sessionFactory.openSession();
			try {
				this.objects = this.getExecutableCriteria(session).list();
			} finally {
				session.close();
			}
		}
		return this.objects;
	}

	@Override
	public List<?> stats() {
		if (!this.loaded) {
			this.loaded = true;
			if (this.projections.getLength() == 0) {
				this.stats = new ArrayList<Object>(0);
			} else {
				Session session = this.sessionFactory.openSession();
				try {
					this.stats = this.getExecutableCriteria(session).setProjection(this.projections).list();
					Iterator<?> iterator = this.stats.iterator();
					while (iterator.hasNext()) {
						if (iterator.next() == null) {
							iterator.remove();
						}
					}
				} finally {
					session.close();
				}
			}
		}
		return this.stats;
	}

}
