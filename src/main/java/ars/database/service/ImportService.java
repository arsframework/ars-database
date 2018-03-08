package ars.database.service;

import java.io.File;

import ars.util.Nfile;
import ars.invoke.local.Api;
import ars.invoke.local.Param;
import ars.invoke.request.Requester;
import ars.database.service.Service;
import ars.database.service.Imexports;

/**
 * 数据导入业务接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface ImportService<T> extends Service<T> {
	/**
	 * 数据批量导入
	 * 
	 * @param requester
	 *            请求对象
	 * @param file
	 *            导入文件对象
	 * @param start
	 *            开始数据行下标（从0开始）
	 * @return 导入结果对象
	 * @throws Exception
	 *             操作异常
	 */
	@Api("input")
	public Imexports.Result input(Requester requester, @Param(name = "file", required = true) Nfile file,
			@Param(name = "start", required = true, regex = "^[0-9]+$") Integer start) throws Exception;

	/**
	 * 下载批量导入失败文件
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
