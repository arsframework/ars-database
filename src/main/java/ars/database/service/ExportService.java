package ars.database.service;

import java.util.Map;

import ars.util.Nfile;
import ars.invoke.local.Api;
import ars.invoke.local.Param;
import ars.invoke.request.Requester;
import ars.database.service.Service;

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
	 * 数据导出结果类
	 * 
	 * @author yongqiangwu
	 *
	 */
	class Result {
		private int total; // 数据总量
		private String file; // 文件名称
		private String size; // 文件大小
		private String spend; // 操作耗时

		public int getTotal() {
			return total;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		public String getFile() {
			return file;
		}

		public void setFile(String file) {
			this.file = file;
		}

		public String getSize() {
			return size;
		}

		public void setSize(String size) {
			this.size = size;
		}

		public String getSpend() {
			return spend;
		}

		public void setSpend(String spend) {
			this.spend = spend;
		}

	}

	/**
	 * 数据批量导出
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            请求参数
	 * @return 导出结果
	 * @throws Exception 操作异常
	 */
	@Api("export")
	public Result export(Requester requester, Map<String, Object> parameters) throws Exception;

	/**
	 * 下载批量导出文件
	 * 
	 * @param requester
	 *            请求对象
	 * @param name
	 *            文件名称
	 * @param parameters
	 *            请求参数
	 * @return 文件对象
	 */
	@Api("file")
	public Nfile file(Requester requester, @Param(name = "name", required = true) String name,
			Map<String, Object> parameters);

}
