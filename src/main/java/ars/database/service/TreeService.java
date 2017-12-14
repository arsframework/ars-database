package ars.database.service;

import java.util.Map;
import java.util.List;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 树形数据业务操作接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface TreeService<T> extends Service<T> {
	/**
	 * 获取树对象列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 树对象实例列表
	 */
	@Api("trees")
	public List<T> trees(Requester requester, Map<String, Object> parameters);

}
