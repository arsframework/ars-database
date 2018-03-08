package ars.database.service;

import java.io.Serializable;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 数据新增外部调用接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface AddService<T> extends Service<T> {
	/**
	 * 新增对象实体
	 * 
	 * @param requester
	 *            请求对象
	 * @return 新增对象实体主键
	 */
	@Api("add")
	public Serializable add(Requester requester);

}
