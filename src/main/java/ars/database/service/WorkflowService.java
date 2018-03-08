package ars.database.service;

import java.util.List;
import java.io.InputStream;
import java.io.IOException;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;
import ars.database.model.Model;
import ars.database.service.Service;
import ars.database.activiti.ActivityNode;

/**
 * 工作流业务操作接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface WorkflowService<T extends Model> extends Service<T> {
	/**
	 * 启动流程
	 * 
	 * @param requester
	 *            请求对象
	 */
	@Api("start")
	public void start(Requester requester);

	/**
	 * 完成任务
	 * 
	 * @param requester
	 *            请求对象
	 */
	@Api("complete")
	public void complete(Requester requester);

	/**
	 * 获取当前用户任务量
	 * 
	 * @param requester
	 *            请求对象
	 * @return 任务量
	 */
	@Api("workload")
	public int workload(Requester requester);

	/**
	 * 获取当前用户任务列表
	 * 
	 * @param requester
	 *            请求对象
	 * @return 对象实例列表
	 */
	@Api("tasks")
	public List<T> tasks(Requester requester);

	/**
	 * 获取当前用户任务进度（已完成任务量）
	 * 
	 * @param requester
	 *            请求对象
	 * @return 已完成任务量
	 */
	@Api("progress")
	public int progress(Requester requester);

	/**
	 * 获取当前用户历史任务
	 * 
	 * @param requester
	 *            请求对象
	 * @return 对象实例列表
	 */
	@Api("histories")
	public List<T> histories(Requester requester);

	/**
	 * 获取流程图（如果参数为空则获取部署流程图，如果参数不为空则获取实例流程图）
	 * 
	 * @param requester
	 *            请求对象
	 * @return 流程图文件输入流
	 * @throws IOException
	 *             IO操作异常
	 */
	@Api("diagram")
	public InputStream diagram(Requester requester) throws IOException;

	/**
	 * 获取节点列表
	 * 
	 * @param requester
	 *            请求对象
	 * @return 状态列表
	 */
	@Api("nodes")
	public List<ActivityNode> nodes(Requester requester);

}
