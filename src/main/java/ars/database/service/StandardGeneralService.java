package ars.database.service;

import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.io.Serializable;

import ars.util.Beans;
import ars.invoke.request.Requester;
import ars.database.model.TreeModel;
import ars.database.repository.Query;
import ars.database.repository.Repositories;
import ars.database.repository.Repository;
import ars.database.service.AbstractService;

/**
 * 通用业务操作接口抽象实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public abstract class StandardGeneralService<T> extends AbstractService<T> {
	/**
	 * 空参数匹配后缀
	 */
	private static final String EMPTY_PARAM_SUFFIX = new StringBuilder(Query.DELIMITER).append(Query.EMPTY).toString();

	/**
	 * 非空参数匹配后缀
	 */
	private static final String NONEMPTY_PARAM_SUFFIX = new StringBuilder(Query.DELIMITER).append(Query.NOT_EMPTY)
			.toString();

	/**
	 * 新增对象实体
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            对象实体参数
	 * @return 新增对象实体主键
	 */
	public Serializable add(Requester requester, Map<String, Object> parameters) {
		T entity = Beans.getInstance(this.getModel());
		this.initObject(requester, entity, parameters);
		return this.saveObject(requester, entity);
	}

	/**
	 * 删除对象
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            数据过滤参数
	 */
	public void delete(Requester requester, Map<String, Object> parameters) {
		boolean effective = false;
		Query<T> query = this.getQuery(requester);
		for (Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (Beans.isEmpty(value)) {
				key = key.toLowerCase();
				if (!key.endsWith(EMPTY_PARAM_SUFFIX) && !key.endsWith(NONEMPTY_PARAM_SUFFIX)) {
					continue;
				}
			}
			query.custom(entry.getKey(), value);
			effective = true;
		}
		if (effective) {
			List<T> entities = query.list();
			for (int i = 0; i < entities.size(); i++) {
				this.deleteObject(requester, entities.get(i));
			}
		}
	}

	/**
	 * 修改对象实体
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            对象实体参数
	 */
	@SuppressWarnings("unchecked")
	public void update(Requester requester, Map<String, Object> parameters) {
		Repository<T> repository = this.getRepository();
		String primary = repository.getPrimary();
		Object[] identifiers = Beans.toArray(Object.class, parameters.get(primary));
		if (identifiers.length > 0) {
			List<T> entities = this.getQuery(requester).or(primary, identifiers).list();
			for (int i = 0; i < entities.size(); i++) {
				T entity = entities.get(i);
				Boolean active = entity instanceof TreeModel ? ((TreeModel<?>) entity).getActive() : null;
				this.initObject(requester, entity, parameters);
				this.updateObject(requester, entity);
				if (active != null && active != ((TreeModel<?>) entity).getActive()) {
					TreeModel<?> tree = (TreeModel<?>) entity;
					if (tree.getActive() == Boolean.TRUE) {
						TreeModel<?> parent = (TreeModel<?>) tree.getParent();
						while (parent != null) {
							if (parent.getActive() != Boolean.TRUE) {
								parent.setActive(true);
								((Repository<TreeModel<?>>) repository).update(parent);
							}
							parent = (TreeModel<?>) parent.getParent();
						}
					} else if (tree.getActive() == Boolean.FALSE) {
						List<T> children = repository.query().ne(primary, tree.getId()).eq("active", true)
								.start("key", tree.getKey()).list();
						for (int j = 0; j < children.size(); j++) {
							TreeModel<?> child = (TreeModel<?>) children.get(j);
							if (child.getActive() != Boolean.FALSE) {
								child.setActive(false);
								((Repository<TreeModel<?>>) repository).update(child);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 统计数量
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 数量
	 */
	public int count(Requester requester, Map<String, Object> parameters) {
		Query<T> query = this.getQuery(requester);
		for (Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey().toLowerCase();
			if (key.equals(Query.PAGE) || key.equals(Query.SIZE)) {
				continue;
			}
			query.custom(entry.getKey(), entry.getValue());
		}
		return query.count();
	}

	/**
	 * 数据统计
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 统计数据列表
	 */
	public List<?> stats(Requester requester, Map<String, Object> parameters) {
		return this.getQuery(requester).custom(parameters).stats();
	}

	/**
	 * 获取单个对象
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例
	 */
	public T object(Requester requester, Map<String, Object> parameters) {
		boolean effective = false;
		Query<T> query = this.getQuery(requester);
		for (Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (Beans.isEmpty(value)) {
				key = key.toLowerCase();
				if (!key.endsWith(EMPTY_PARAM_SUFFIX) && !key.endsWith(NONEMPTY_PARAM_SUFFIX)) {
					continue;
				}
			}
			query.custom(entry.getKey(), value);
			effective = true;
		}
		return effective ? query.single() : null;
	}

	/**
	 * 获取对象列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例列表
	 */
	public List<T> objects(Requester requester, Map<String, Object> parameters) {
		return this.getQuery(requester).custom(parameters).list();
	}

	/**
	 * 获取树对象列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 树对象实例列表
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<T> trees(Requester requester, Map<String, Object> parameters) {
		List<T> objects = this.getQuery(requester).custom(parameters).list();
		return (List<T>) Repositories.mergeTrees((List<TreeModel>) objects);
	}

}
