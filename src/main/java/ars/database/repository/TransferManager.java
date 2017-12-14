package ars.database.repository;

import java.util.Map;

import ars.invoke.request.Requester;
import ars.database.repository.Query;
import ars.database.repository.Transform;

/**
 * 数据转换管理接口
 * 
 * @author yongqiangwu
 * 
 */
public interface TransferManager {
	/**
	 * 数据模型转型是否已注册
	 * 
	 * @param model
	 *            数据模型
	 * @param property
	 *            被转换属性名称
	 * @return true/false
	 */
	public boolean isRegistered(Class<?> model, String property);

	/**
	 * 数据转换注册
	 * 
	 * @param model
	 *            数据模型
	 * @param property
	 *            被转换属性名称
	 * @param transform
	 *            数据转换对象
	 */
	public void register(Class<?> model, String property, Transform transform);

	/**
	 * 数据转换注册
	 * 
	 * @param model
	 *            数据模型
	 * @param transforms
	 *            属性名/数据转换对象映射
	 */
	public void register(Class<?> model, Map<String, Transform> transforms);

	/**
	 * 获取数据转换查询对象
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param query
	 *            原始数据查询对象
	 * @return 数据转换查询对象
	 */
	public <T> Query<T> getTransferQuery(Requester requester, Query<T> query);

}
