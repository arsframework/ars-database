package ars.database.repository;

import java.io.Serializable;

import ars.database.repository.Query;

/**
 * 数据持久化操作接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface Repository<T> {
	/**
	 * 获取数据模型
	 * 
	 * @return 数据模型
	 */
	public Class<T> getModel();

	/**
	 * 获取主键名称
	 * 
	 * @return 主键名称
	 */
	public String getPrimary();

	/**
	 * 获取数据查询对象
	 * 
	 * @return 数据查询对象
	 */
	public Query<T> query();

	/**
	 * 根据主键获取对象实例
	 * 
	 * @param id
	 *            主键
	 * @return 对象实例
	 */
	public T get(Object id);

	/**
	 * 将对象持久化
	 * 
	 * @param object
	 *            数据对象
	 * @return 主键标识
	 */
	public Serializable save(T object);

	/**
	 * 修改对象
	 * 
	 * @param object
	 *            数据对象
	 */
	public void update(T object);

	/**
	 * 删除数据
	 * 
	 * @param object
	 *            数据对象
	 */
	public void delete(T object);

}
