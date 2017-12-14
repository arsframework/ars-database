package ars.database.repository;

import java.util.Map;

import ars.database.repository.Repository;

/**
 * 数据持久化操作工厂接口
 * 
 * @author yongqiangwu
 * 
 */
public interface RepositoryFactory {
	/**
	 * 获取所有数据持久化对象
	 * 
	 * @return 数据模型/持久化对象映射
	 */
	public Map<Class<?>, Repository<?>> getRepositories();

	/**
	 * 根据数据模型获取数据持久化操作对象
	 * 
	 * @param <T>
	 *            数据类型
	 * @param model
	 *            数据模型
	 * @return 数据持久化操作对象
	 */
	public <T> Repository<T> getRepository(Class<T> model);

}
