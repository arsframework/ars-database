package ars.database.repository;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.MethodInterceptor;

import ars.util.Beans;
import ars.util.Strings;
import ars.util.Conditions;
import ars.util.Conditions.Or;
import ars.util.Conditions.And;
import ars.util.Conditions.Logic;
import ars.util.Conditions.Condition;
import ars.invoke.request.Requester;
import ars.database.repository.Query;
import ars.database.repository.Transform;
import ars.database.repository.EmptyQuery;
import ars.database.repository.Repositories;
import ars.database.repository.TransferManager;
import ars.database.repository.DataConstraintException;

/**
 * 数据转换管理接口标准实现
 * 
 * @author yongqiangwu
 * 
 */
public class StandardTransferManager implements TransferManager {
	private Map<Class<?>, Map<String, Transform>> mappings = new HashMap<Class<?>, Map<String, Transform>>();

	@Override
	public boolean isRegistered(Class<?> model, String property) {
		Map<String, Transform> transforms = this.mappings.get(model);
		return transforms != null && transforms.containsKey(property);
	}

	@Override
	public void register(Class<?> model, String property, Transform transform) {
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		if (property == null) {
			throw new IllegalArgumentException("Illegal property:" + property);
		}
		if (transform == null) {
			throw new IllegalArgumentException("Illegal transform:" + transform);
		}
		Map<String, Transform> transforms = this.mappings.get(model);
		if (transforms == null) {
			transforms = new HashMap<String, Transform>();
			this.mappings.put(model, transforms);
		}
		if (transforms.containsKey(property)) {
			throw new RuntimeException("Transform is already registered:" + model.getName() + "." + property);
		}
		transforms.put(property, transform);
	}

	@Override
	public void register(Class<?> model, Map<String, Transform> transforms) {
		for (Entry<String, Transform> entry : transforms.entrySet()) {
			this.register(model, entry.getKey(), entry.getValue());
		}
	}

	@Override
	public <T> Query<T> getTransferQuery(Requester requester, Query<T> query) {
		return new TransferQuery<T>(requester, query);
	}

	/**
	 * 数据转换执行对象
	 * 
	 * @author yongqiangwu
	 * 
	 */
	class TransferExecutor {
		private Transform transform; // 转换条件对象
		private String property; // 目标属性名称

		public TransferExecutor(Transform transform, String property) {
			if (transform == null) {
				throw new IllegalArgumentException("Illegal transform:" + transform);
			}
			if (property == null) {
				throw new IllegalArgumentException("Illegal property:" + property);
			}
			this.transform = transform;
			this.property = property;
		}

		public Transform getTransform() {
			return transform;
		}

		public String getProperty() {
			return property;
		}

		/**
		 * 数据转换调用
		 * 
		 * @param requester
		 *            请求对象
		 * @param handle
		 *            匹配模式
		 * @param value
		 *            匹配值
		 * @return 调用结果（对象数组）
		 */
		public Object[] invoke(Requester requester, String handle, Object value) {
			StringBuilder key = new StringBuilder(this.property);
			if (!Strings.isEmpty(handle)) {
				key.append(Query.DELIMITER).append(handle);
			}
			Map<String, Object> parameters = new HashMap<String, Object>(1);
			parameters.put(key.toString(), value);
			Object object = requester.execute(this.transform.getResource(), parameters);
			if (object instanceof RuntimeException) {
				throw (RuntimeException) object;
			} else if (object instanceof Exception) {
				throw new RuntimeException((Exception) object);
			}
			List<?> targets = (List<?>) Beans.getAssemblePropertyValue(object, this.transform.getKey());
			return targets == null || targets.isEmpty() ? Beans.EMPTY_ARRAY : targets.toArray();
		}

	}

	/**
	 * 获取数据查询转换对象
	 * 
	 * @param model
	 *            数据模型
	 * @param property
	 *            查询属性名称
	 * @return 数据查询转换对象
	 */
	protected TransferExecutor getTransferExecutor(Class<?> model, String property) {
		int split = property.indexOf('.');
		String direct = split > 0 ? property.substring(0, split) : property;
		Map<String, Transform> transforms = this.mappings.get(model);
		Transform transform = transforms == null ? null : transforms.get(direct);
		return transform == null ? null
				: new TransferExecutor(transform, split > 0 ? property.substring(split + 1) : transform.getKey());
	}

	/**
	 * 数据转换查询对象
	 * 
	 * @author yongqiangwu
	 * 
	 * @param <T>
	 *            数据模型
	 */
	class TransferQuery<T> implements Query<T> {
		private T object; // 单个实例
		private List<T> objects; // 实例列表
		private boolean loaded; // 数据是否已加载
		private Query<T> query; // 数据查询对象
		private Requester requester; // 请求对象

		public TransferQuery(Requester requester, Query<T> query) {
			if (query == null) {
				throw new IllegalArgumentException("Illegal query:" + query);
			}
			if (requester == null) {
				throw new IllegalArgumentException("Illegal requester:" + requester);
			}
			this.query = query;
			this.requester = requester;
		}

		/**
		 * 实体转换属性数据加载
		 * 
		 * @param entity
		 *            对象实体
		 * @param transforms
		 *            属性名称/数据转换对象映射
		 * @return 加载完成后的对象实体
		 */
		@SuppressWarnings("unchecked")
		protected T load(T entity, Map<String, Transform> transforms) {
			final Map<String, Transform> lazies = new HashMap<String, Transform>(transforms.size());
			for (Entry<String, Transform> entry : transforms.entrySet()) {
				String property = entry.getKey();
				Transform transform = entry.getValue();
				Object value = Beans.getValue(entity, transform.getTarget());
				if (value != null) {
					if (transform.isLazy()) {
						lazies.put(property, transform);
						continue;
					}
					Beans.setValue(entity, property, this.transfer(property, value, transform));
				}
			}
			if (lazies.isEmpty()) {
				return entity;
			}
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(this.getModel());
			enhancer.setCallback(new MethodInterceptor() {

				@Override
				public Object intercept(Object target, Method method, Object[] args, MethodProxy proxy)
						throws Throwable {
					String name = method.getName();
					Object value = proxy.invokeSuper(target, args);
					if (value != null || !name.startsWith("get")) {
						return value;
					}
					String property = name.substring(3).toLowerCase();
					Transform transform = lazies.get(property);
					if (transform == null) {
						return value;
					}
					Object transfer = transfer(property, Beans.getValue(target, transform.getTarget()), transform);
					Beans.setValue(target, property, transfer);
					return transfer;
				}

			});
			T proxy = (T) enhancer.create();
			Beans.copy(entity, proxy);
			return proxy;
		}

		/**
		 * 条件逻辑对象转换
		 * 
		 * @param logic
		 *            条件逻辑对象
		 * @return 条件逻辑对象
		 */
		protected Logic transfer(Logic logic) {
			if (logic == null) {
				return null;
			} else if (logic instanceof Or) {
				Logic[] logics = ((Or) logic).getLogics();
				for (int i = 0; i < logics.length; i++) {
					logics[i] = this.transfer(logics[i]);
				}
				return logic;
			} else if (logic instanceof And) {
				Logic[] logics = ((And) logic).getLogics();
				for (int i = 0; i < logics.length; i++) {
					Logic transfer = this.transfer(logics[i]);
					if (transfer == null) {
						return null;
					}
					logics[i] = transfer;
				}
				return logic;
			} else if (logic instanceof Condition) {
				Condition condition = (Condition) logic;
				String property = condition.getKey();
				Object value = condition.getValue();
				String handle = EQ;
				int index = property.indexOf(DELIMITER);
				if (index > 0) {
					handle = property.substring(index + DELIMITER.length());
					property = property.substring(0, index);
				}
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor == null) {
					return logic;
				}
				Object[] targets = executor.invoke(this.requester, handle, value);
				if (targets == null || targets.length == 0) {
					return null;
				}
				String key = new StringBuilder(executor.getTransform().getTarget()).append(DELIMITER).append(IN)
						.toString();
				return new Condition(key, targets);
			}
			throw new RuntimeException("Not support query logic:" + logic);
		}

		/**
		 * 数据转化
		 * 
		 * @param property
		 *            属性名称
		 * @param value
		 *            属性值
		 * @param transform
		 *            数据转换对象
		 * @return 转换后数据对象
		 */
		protected Object transfer(String property, Object value, Transform transform) {
			Class<?> targetClass = Beans.getField(this.getModel(), property).getType();
			Map<String, Object> parameters = new HashMap<String, Object>(1);
			parameters.put(transform.getKey(), value);
			Object result = this.requester.execute(transform.getResource(), parameters);
			if (result instanceof RuntimeException) {
				throw (RuntimeException) result;
			} else if (result instanceof Exception) {
				throw new RuntimeException((Exception) result);
			}
			List<?> targets = (List<?>) result;
			if (targets == null || targets.isEmpty()) {
				throw new DataConstraintException("Object does not exist:" + targetClass.getName() + parameters);
			}
			return Beans.initialize(targetClass, (Map<?, ?>) targets.get(0));
		}

		/**
		 * 执行数据数据转换查询
		 * 
		 * @param executor
		 *            数据查询转换对象
		 * @param handle
		 *            匹配模式
		 * @param value
		 *            匹配值
		 * @return 数据查询对象
		 */
		protected Query<T> execute(TransferExecutor executor, String handle, Object value) {
			Object[] targets = executor.invoke(this.requester, handle, value);
			if (targets == null || targets.length == 0) {
				return Repositories.emptyQuery();
			}
			this.query.in(executor.getTransform().getTarget(), targets);
			return this;
		}

		@Override
		public Iterator<T> iterator() {
			return this.list().iterator();
		}

		@Override
		public Class<T> getModel() {
			return this.query.getModel();
		}

		@Override
		public Query<T> empty(String... properties) {
			if (properties != null && properties.length > 0) {
				for (String property : properties) {
					TransferExecutor executor = getTransferExecutor(this.getModel(), property);
					if (executor == null) {
						this.query.empty(property);
						continue;
					} else if (executor.getProperty().equals(executor.getTransform().getTarget())) {
						this.query.empty(executor.getProperty());
						continue;
					}
					return this.execute(executor, EMPTY, null);
				}
			}
			return this;
		}

		@Override
		public Query<T> nonempty(String... properties) {
			if (properties != null && properties.length > 0) {
				for (String property : properties) {
					TransferExecutor executor = getTransferExecutor(this.getModel(), property);
					if (executor == null) {
						this.query.nonempty(property);
						continue;
					} else if (executor.getProperty().equals(executor.getTransform().getTarget())) {
						this.query.nonempty(executor.getProperty());
						continue;
					}
					return this.execute(executor, NOT_EMPTY, null);
				}
			}
			return this;
		}

		@Override
		public Query<T> eq(String property, Object value) {
			if (property != null && value != null) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, EQ, value);
				}
				this.query.eq(property, value);
			}
			return this;
		}

		@Override
		public Query<T> ne(String property, Object value) {
			if (property != null && value != null) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, NE, value);
				}
				this.query.ne(property, value);
			}
			return this;
		}

		@Override
		public Query<T> gt(String property, Object value) {
			if (property != null && value != null) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, GT, value);
				}
				this.query.gt(property, value);
			}
			return this;
		}

		@Override
		public Query<T> ge(String property, Object value) {
			if (property != null && value != null) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, GE, value);
				}
				this.query.ge(property, value);
			}
			return this;
		}

		@Override
		public Query<T> lt(String property, Object value) {
			if (property != null && value != null) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, LT, value);
				}
				this.query.lt(property, value);
			}
			return this;
		}

		@Override
		public Query<T> le(String property, Object value) {
			if (property != null && value != null) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, LE, value);
				}
				this.query.le(property, value);
			}
			return this;
		}

		@Override
		public Query<T> between(String property, Object low, Object high) {
			if (property != null && low != null && high != null) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, IN, new Object[] { low, high });
				}
				this.query.between(property, low, high);
			}
			return this;
		}

		@Override
		public Query<T> start(String property, String... values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, START, values);
				}
				this.query.start(property, values);
			}
			return this;
		}

		@Override
		public Query<T> nstart(String property, String... values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, NOT_START, values);
				}
				this.query.nstart(property, values);
			}
			return this;
		}

		@Override
		public Query<T> end(String property, String... values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, END, values);
				}
				this.query.end(property, values);
			}
			return this;
		}

		@Override
		public Query<T> nend(String property, String... values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, NOT_END, values);
				}
				this.query.nend(property, values);
			}
			return this;
		}

		@Override
		public Query<T> like(String property, String... values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, LIKE, values);
				}
				this.query.like(property, values);
			}
			return this;
		}

		@Override
		public Query<T> nlike(String property, String... values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, NOT_LIKE, values);
				}
				this.query.nlike(property, values);
			}
			return this;
		}

		@Override
		public Query<T> in(String property, Object[] values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, IN, values);
				}
				this.query.in(property, values);
			}
			return this;
		}

		@Override
		public Query<T> or(String property, Object[] values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, OR, values);
				}
				this.query.or(property, values);
			}
			return this;
		}

		@Override
		public Query<T> not(String property, Object[] values) {
			if (property != null && values != null && values.length > 0) {
				TransferExecutor executor = getTransferExecutor(this.getModel(), property);
				if (executor != null) {
					return this.execute(executor, NOT, values);
				}
				this.query.not(property, values);
			}
			return this;
		}

		@Override
		public Query<T> custom(String property, Object value) {
			if (Strings.isEmpty(property) || property.equals(PAGE) || property.equals(SIZE) || property.equals(ORDER)) {
				this.query.custom(property, value);
				return this;
			} else if (property.equals(CONDITION)) {
				if (!Beans.isEmpty(value)) {
					Logic logic = value instanceof Logic ? (Logic) value : Conditions.parse(value.toString());
					return this.condition(logic);
				}
				return this;
			}
			return this.condition(property, value);
		}

		@Override
		public Query<T> custom(Map<String, Object> parameters) {
			if (parameters != null && !parameters.isEmpty()) {
				for (Entry<String, Object> entry : parameters.entrySet()) {
					Query<T> query = this.custom(entry.getKey(), entry.getValue());
					if (query instanceof EmptyQuery) {
						return query;
					}
				}
			}
			return this;
		}

		@Override
		public Query<T> condition(Logic logic) {
			if (logic != null) {
				Logic transfer = this.transfer(logic);
				if (transfer == null) {
					return Repositories.emptyQuery();
				}
				this.query.condition(transfer);
			}
			return this;
		}

		@Override
		public Query<T> condition(String property, Object value) {
			String handle = EQ;
			int index = property.indexOf(DELIMITER);
			if (index > 0) {
				handle = property.substring(index + DELIMITER.length());
				property = property.substring(0, index);
			}
			TransferExecutor executor = getTransferExecutor(this.getModel(), property);
			if (executor != null) {
				return this.execute(executor, handle, value);
			}
			this.query.custom(property, value);
			return this;
		}

		@Override
		public Query<T> condition(Map<String, Object> parameters) {
			if (parameters != null && !parameters.isEmpty()) {
				for (Entry<String, Object> entry : parameters.entrySet()) {
					Query<T> query = this.condition(entry.getKey(), entry.getValue());
					if (query instanceof EmptyQuery) {
						return query;
					}
				}
			}
			return this;
		}

		@Override
		public Query<T> eqProperty(String property, String other) {
			this.query.eqProperty(property, other);
			return this;
		}

		@Override
		public Query<T> neProperty(String property, String other) {
			this.query.neProperty(property, other);
			return this;
		}

		@Override
		public Query<T> ltProperty(String property, String other) {
			this.query.ltProperty(property, other);
			return this;
		}

		@Override
		public Query<T> leProperty(String property, String other) {
			this.query.leProperty(property, other);
			return this;
		}

		@Override
		public Query<T> gtProperty(String property, String other) {
			this.query.gtProperty(property, other);
			return this;
		}

		@Override
		public Query<T> geProperty(String property, String other) {
			this.query.geProperty(property, other);
			return this;
		}

		@Override
		public Query<T> asc(String... properties) {
			this.query.asc(properties);
			return this;
		}

		@Override
		public Query<T> desc(String... properties) {
			this.query.desc(properties);
			return this;
		}

		@Override
		public Query<T> paging(int page, int size) {
			this.query.paging(page, size);
			return this;
		}

		@Override
		public Query<T> min(String... properties) {
			this.query.min(properties);
			return this;
		}

		@Override
		public Query<T> max(String... properties) {
			this.query.max(properties);
			return this;
		}

		@Override
		public Query<T> avg(String... properties) {
			this.query.avg(properties);
			return this;
		}

		@Override
		public Query<T> sum(String... properties) {
			this.query.sum(properties);
			return this;
		}

		@Override
		public Query<T> number(String... properties) {
			this.query.number(properties);
			return this;
		}

		@Override
		public Query<T> group(String... properties) {
			this.query.group(properties);
			return this;
		}

		@Override
		public Query<T> property(String... properties) {
			this.query.property(properties);
			return this;
		}

		@Override
		public int count() {
			return this.query.count();
		}

		@Override
		public T single() {
			if (!this.loaded) {
				this.loaded = true;
				this.object = this.query.single();
				Map<String, Transform> transforms = mappings.get(this.getModel());
				if (transforms != null) {
					this.object = this.load(this.object, transforms);
				}
			}
			return this.object;
		}

		@Override
		public List<T> list() {
			if (!this.loaded) {
				this.loaded = true;
				this.objects = this.query.list();
				Map<String, Transform> transforms = mappings.get(this.getModel());
				if (transforms != null) {
					List<T> loads = new ArrayList<T>(this.objects.size());
					for (int i = 0; i < this.objects.size(); i++) {
						loads.add(this.load(this.objects.get(i), transforms));
					}
					this.objects = loads;
				}

			}
			return this.objects;
		}

		@Override
		public List<?> stats() {
			return this.query.stats();
		}

	}

}
