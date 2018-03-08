package ars.database.service;

import ars.invoke.local.Api;
import ars.invoke.local.Param;
import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 数据修改外部调用接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface UpdateService<T> extends Service<T> {
	/**
	 * 修改对象实体
	 * 
	 * @param requester
	 *            请求对象
	 * @param identifiers
	 *            对象主键数组
	 */
	@Api("update")
	public void update(Requester requester, @Param(name = "id", required = true) Object[] identifiers);

}
