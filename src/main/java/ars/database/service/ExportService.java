package ars.database.service;

import java.io.File;

import ars.invoke.local.Api;
import ars.invoke.local.Param;
import ars.invoke.request.Requester;
import ars.database.service.Service;
import ars.database.service.Imexports;

/**
 * 数据导出业务接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface ExportService<T> extends Service<T> {
	/**
	 * 数据批量导出
	 * 
	 * @param requester
	 *            请求对象
	 * @return 导出结果
	 * @throws Exception
	 *             操作异常
	 */
	@Api("output")
	public Imexports.Result output(Requester requester) throws Exception;

	/**
	 * 下载批量导出文件
	 * 
	 * @param requester
	 *            请求对象
	 * @param name
	 *            文件名称
	 * @return 文件对象
	 */
	@Api("download")
	public File download(Requester requester, @Param(name = "name", required = true) String name);

}
