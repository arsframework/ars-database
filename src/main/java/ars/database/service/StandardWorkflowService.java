package ars.database.service;

import java.util.List;
import java.io.InputStream;
import java.io.IOException;

import ars.database.model.Model;
import ars.database.service.Workflows;
import ars.database.service.StandardGeneralService;
import ars.database.activiti.ActivityNode;
import ars.invoke.request.Requester;

/**
 * 基于工作流业务操作接口抽象实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public abstract class StandardWorkflowService<T extends Model> extends StandardGeneralService<T> {
	/**
	 * 启动流程
	 * 
	 * @param requester
	 *            请求对象
	 */
	public void start(Requester requester) {
		Workflows.startProcess(requester, this);
	}

	/**
	 * 完成任务
	 * 
	 * @param requester
	 *            请求对象
	 */
	public void complete(Requester requester) {
		Workflows.completeTask(requester, this);
	}

	/**
	 * 获取当前用户任务量
	 * 
	 * @param requester
	 *            请求对象
	 * @return 任务量
	 */
	public int workload(Requester requester) {
		return Workflows.getWorkload(requester, this);
	}

	/**
	 * 获取当前用户任务列表
	 * 
	 * @param requester
	 *            请求对象
	 * @return 对象实例列表
	 */
	public List<T> tasks(Requester requester) {
		return Workflows.getTasks(requester, this);
	}

	/**
	 * 获取当前用户任务进度（已完成任务量）
	 * 
	 * @param requester
	 *            请求对象
	 * @return 已完成任务量
	 */
	public int progress(Requester requester) {
		return Workflows.getProgress(requester, this);
	}

	/**
	 * 获取当前用户历史任务
	 * 
	 * @param requester
	 *            请求对象
	 * @return 对象实例列表
	 */
	public List<T> histories(Requester requester) {
		return Workflows.getHistories(requester, this);
	}

	/**
	 * 获取流程图（如果参数为空则获取部署流程图，如果参数不为空则获取实例流程图）
	 * 
	 * @param requester
	 *            请求对象
	 * @return 流程图文件输入流
	 * @throws IOException
	 *             IO操作异常
	 */
	public InputStream diagram(Requester requester) throws IOException {
		return Workflows.getDiagram(requester, this);
	}

	/**
	 * 获取节点列表
	 * 
	 * @param requester
	 *            请求对象
	 * @return 状态列表
	 */
	public List<ActivityNode> nodes(Requester requester) {
		return Workflows.getNodes(this.getModel());
	}

}
