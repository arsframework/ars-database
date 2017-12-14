package ars.database.service;

import java.util.Map;
import java.util.List;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 数据查询外部调用接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface SearchService<T> extends Service<T> {
	/**
	 * 统计数量
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 数量
	 */
	@Api("count")
	public int count(Requester requester, Map<String, Object> parameters);

	/**
	 * 数据统计
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 统计数据列表
	 */
	@Api("stats")
	public List<?> stats(Requester requester, Map<String, Object> parameters);

	/**
	 * 获取单个对象
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例
	 */
	@Api("object")
	public T object(Requester requester, Map<String, Object> parameters);

	/**
	 * 获取对象列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例列表
	 */
	@Api("objects")
	public List<T> objects(Requester requester, Map<String, Object> parameters);

}
