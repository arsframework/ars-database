package ars.database.service;

import java.util.Map;
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
	 * @param parameters
	 *            请求参数
	 */
	public void start(Requester requester, Map<String, Object> parameters) {
		Workflows.startProcess(this, requester, parameters);
	}

	/**
	 * 完成任务
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            附加参数
	 */
	public void complete(Requester requester, Map<String, Object> parameters) {
		Workflows.completeTask(this, requester, parameters);
	}

	/**
	 * 获取当前用户任务量
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 任务量
	 */
	public int workload(Requester requester, Map<String, Object> parameters) {
		return Workflows.getWorkload(this, requester, parameters);
	}

	/**
	 * 获取当前用户任务列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例列表
	 */
	public List<T> tasks(Requester requester, Map<String, Object> parameters) {
		return Workflows.getTasks(this, requester, parameters);
	}

	/**
	 * 获取当前用户任务进度（已完成任务量）
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 已完成任务量
	 */
	public int progress(Requester requester, Map<String, Object> parameters) {
		return Workflows.getProgress(this, requester, parameters);
	}

	/**
	 * 获取当前用户历史任务
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例列表
	 */
	public List<T> histories(Requester requester, Map<String, Object> parameters) {
		return Workflows.getHistories(this, requester, parameters);
	}

	/**
	 * 获取流程图（如果参数为空则获取部署流程图，如果参数不为空则获取实例流程图）
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 流程图文件输入流
	 * @throws IOException
	 *             IO操作异常
	 */
	public InputStream diagram(Requester requester, Map<String, Object> parameters) throws IOException {
		return Workflows.getDiagram(this, requester, parameters);
	}

	/**
	 * 获取节点列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            请求参数
	 * @return 状态列表
	 */
	public List<ActivityNode> nodes(Requester requester, Map<String, Object> parameters) {
		return Workflows.getNodes(this.getModel());
	}

}
