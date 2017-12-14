package ars.database.service;

import java.util.Map;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 数据删除外部调用接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface DeleteService<T> extends Service<T> {
	/**
	 * 删除对象
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            数据过滤参数
	 */
	@Api("delete")
	public void delete(Requester requester, Map<String, Object> parameters);

}
