package ars.database.service;

import org.apache.poi.ss.usermodel.Row;

import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * Excel数据对象适配接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface ExcelAdapter<T> {
	/**
	 * 开始适配
	 * 
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务操作对象
	 */
	public void begin(Requester requester, Service<T> service);

	/**
	 * 获取标题
	 * 
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务操作对象
	 * @return 标题数组
	 */
	public String[] getTitles(Requester requester, Service<T> service);

	/**
	 * 读取Excel数据行并转换成对象实体
	 * 
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务操作对象
	 * @param row
	 *            数据行对象
	 * @return 对象实体
	 */
	public T read(Requester requester, Service<T> service, Row row);

	/**
	 * 将对象实体写入到Excel数据行
	 * 
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务操作对象
	 * @param entity
	 *            对象实体
	 * @param row
	 *            数据行对象
	 */
	public void write(Requester requester, Service<T> service, T entity, Row row);

	/**
	 * 完成适配
	 * 
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务操作对象
	 */
	public void complete(Requester requester, Service<T> service);

}
