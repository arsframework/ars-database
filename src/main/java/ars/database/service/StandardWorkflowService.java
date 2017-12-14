package ars.database.service;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.io.InputStream;
import java.io.IOException;

import org.activiti.engine.task.Task;
import org.activiti.engine.TaskService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.image.ProcessDiagramGenerator;

import ars.util.Nfile;
import ars.util.Beans;
import ars.invoke.request.Requester;
import ars.database.model.Model;
import ars.database.repository.Query;
import ars.database.repository.Repositories;
import ars.database.activiti.ActivityNode;
import ars.database.activiti.ProcessConfiguration;
import ars.database.service.WorkflowService;
import ars.database.service.StandardGeneralService;

/**
 * 工作流业务操作接口抽象实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public abstract class StandardWorkflowService<T extends Model> extends
		StandardGeneralService<T> implements WorkflowService<T> {
	protected ProcessEngine processEngine;

	/**
	 * 获取流程配置
	 * 
	 * @return 流程对象
	 */
	protected ProcessConfiguration getProcessConfiguration() {
		return ((ProcessConfiguration) this.processEngine
				.getProcessEngineConfiguration());
	}

	/**
	 * 获取流程节点
	 * 
	 * @return 流程节点列表
	 */
	protected List<ActivityNode> getActivityNodes() {
		return this.getProcessConfiguration().getNodes(this.getModel());
	}

	/**
	 * 获取对象实体流程实例
	 * 
	 * @param entity
	 *            对象实体
	 * @return 流程实例
	 */
	protected ProcessInstance getProcessInstance(T entity) {
		return this.processEngine.getRuntimeService()
				.createProcessInstanceQuery()
				.processInstanceId(entity.getProcess()).singleResult();
	}

	/**
	 * 获取当前用户对象实体任务
	 * 
	 * @param assignee
	 *            任务接收者标识
	 * @param entity
	 *            对象实体
	 * @return 任务对象
	 */
	protected Task getTask(String assignee, T entity) {
		return this.processEngine.getTaskService().createTaskQuery()
				.taskCandidateUser(assignee)
				.processInstanceId(entity.getProcess()).singleResult();
	}

	/**
	 * 启动工作流
	 * 
	 * @param requester
	 *            请求对象
	 * @param entity
	 *            对象实体
	 * @param parameters
	 *            请求参数
	 * @return 工作流实例
	 */
	protected ProcessInstance startProcess(Requester requester, T entity,
			Map<String, Object> parameters) {
		String process = entity.getProcess();
		if (process != null) {
			throw new RuntimeException("Process is already started:" + entity);
		}
		RuntimeService runtimeService = this.processEngine.getRuntimeService();
		ProcessConfiguration configuration = this.getProcessConfiguration();
		ProcessInstance processInstance = runtimeService
				.startProcessInstanceByKey(
						configuration.getKey(this.getModel()), parameters);
		List<ActivityNode> nodes = configuration.getNodes(this.getModel());
		entity.setActive(false);
		entity.setProcess(processInstance.getId());
		if (nodes.size() > 1) {
			entity.setStatus(nodes.get(1).getId());
		}
		this.updateObject(requester, entity);
		return processInstance;
	}

	/**
	 * 完成任务
	 * 
	 * @param requester
	 *            请求对象
	 * @param assignee
	 *            任务接收者标识
	 * @param entity
	 *            对象实体
	 * @param parameters
	 *            工作流上下文参数
	 * @return 任务对象
	 */
	protected Task completeTask(Requester requester, String assignee, T entity,
			Map<String, Object> parameters) {
		Task task = this.getTask(assignee, entity);
		if (task == null) {
			throw new RuntimeException("Task is already processed:" + entity);
		}
		TaskService taskService = this.processEngine.getTaskService();
		taskService.claim(task.getId(), assignee);
		ProcessConfiguration configuration = this.getProcessConfiguration();
		ActivityNode node = configuration.getNode(this.getModel(),
				entity.getStatus()); // 当前节点
		if (node == null) {
			throw new RuntimeException("Activity node does not exist with id:"
					+ entity.getStatus());
		}
		taskService.complete(task.getId(), parameters);
		synchronized (entity.getProcess().intern()) {
			ProcessInstance processInstance = this.getProcessInstance(entity);
			if (processInstance == null) { // 流程已完成
				entity.setActive(true);
				List<ActivityNode> nodes = configuration.getNodes(this
						.getModel());
				if (!nodes.isEmpty()) {
					entity.setStatus(nodes.get(nodes.size() - 1).getId());
				}
				this.updateObject(requester, entity);
			} else { // 节点改变
				List<String> activities = this.processEngine
						.getRuntimeService().getActiveActivityIds(
								entity.getProcess());
				if (activities.isEmpty()
						|| !activities.get(0).equals(node.getCode())) {
					String activity = activities.get(0);
					ActivityNode next = configuration.getNode(this.getModel(),
							activity);
					if (next == null) {
						throw new RuntimeException(
								"Activity node does not exist with code:"
										+ activity);
					}
					entity.setStatus(next.getId());
					this.updateObject(requester, entity);
				}
			}
		}
		return task;
	}

	/**
	 * 获取用户为完成任务对象集合
	 * 
	 * @param requester
	 *            请求对象
	 * @param assignee
	 *            任务接收者标识
	 * @param parameters
	 *            请求参数
	 * @return 对象集合
	 */
	protected Query<T> getTaskQuery(Requester requester, String assignee,
			Map<String, Object> parameters) {
		List<Task> tasks = this.processEngine.getTaskService()
				.createTaskQuery().taskCandidateUser(assignee).list();
		if (tasks.isEmpty()) {
			return Repositories.emptyQuery();
		}
		String[] processes = new String[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			processes[i] = tasks.get(i).getProcessInstanceId();
		}
		return this.getQuery(requester).in("process", processes)
				.custom(parameters);
	}

	/**
	 * 获取用户已完成任务对象集合
	 * 
	 * @param requester
	 *            请求对象
	 * @param assignee
	 *            任务接收者标识
	 * @param parameters
	 *            请求参数
	 * @return 对象集合
	 */
	protected Query<T> getFinishQuery(Requester requester, String assignee,
			Map<String, Object> parameters) {
		HistoryService historyService = this.processEngine.getHistoryService();
		List<HistoricTaskInstance> historicTaskInstances = historyService
				.createHistoricTaskInstanceQuery().taskAssignee(assignee)
				.list();
		if (historicTaskInstances.isEmpty()) {
			return Repositories.emptyQuery();
		}
		String[] processes = new String[historicTaskInstances.size()];
		for (int i = 0; i < historicTaskInstances.size(); i++) {
			processes[i] = historicTaskInstances.get(i).getProcessInstanceId();
		}
		return this.getQuery(requester).in("process", processes)
				.custom(parameters);
	}

	@Override
	public void setProcessEngine(ProcessEngine processEngine) {
		this.processEngine = processEngine;
	}

	@Override
	public void start(Requester requester, Map<String, Object> parameters) {
		String primary = this.getRepository().getPrimary();
		Object[] identifiers = Beans.toArray(Object.class,
				parameters.get(primary));
		if (identifiers.length > 0) {
			List<T> objects = this.getQuery(requester).or(primary, identifiers)
					.list();
			for (int i = 0; i < objects.size(); i++) {
				this.startProcess(requester, objects.get(i), parameters);
			}
		}
	}

	@Override
	public void complete(Requester requester, Map<String, Object> parameters) {
		String primary = this.getRepository().getPrimary();
		Object[] identifiers = Beans.toArray(Object.class,
				parameters.get(primary));
		if (identifiers.length > 0) {
			List<T> objects = this.getQuery(requester).or(primary, identifiers)
					.list();
			for (int i = 0; i < objects.size(); i++) {
				this.completeTask(requester, requester.getUser(),
						objects.get(i), parameters);
			}
		}
	}

	@Override
	public int workload(Requester requester, Map<String, Object> parameters) {
		return this.getTaskQuery(requester, requester.getUser(), parameters)
				.count();
	}

	@Override
	public List<T> tasks(Requester requester, Map<String, Object> parameters) {
		return this.getTaskQuery(requester, requester.getUser(), parameters)
				.list();
	}

	@Override
	public int progress(Requester requester, Map<String, Object> parameters) {
		return this.getFinishQuery(requester, requester.getUser(), parameters)
				.count();
	}

	@Override
	public List<T> histories(Requester requester, Map<String, Object> parameters) {
		return this.getFinishQuery(requester, requester.getUser(), parameters)
				.list();
	}

	@Override
	public Nfile diagram(Requester requester, Map<String, Object> parameters)
			throws IOException {
		T entity = this.object(requester, parameters);
		List<ActivityNode> nodes = this.getActivityNodes();
		ProcessEngineConfiguration configuration = this.processEngine
				.getProcessEngineConfiguration();
		ProcessDiagramGenerator diagramGenerator = configuration
				.getProcessDiagramGenerator();
		RepositoryService repositoryService = this.processEngine
				.getRepositoryService();
		String name = new StringBuilder(String.valueOf(System
				.currentTimeMillis())).append(".png").toString();
		String identifier = ((ProcessConfiguration) configuration)
				.getIdentifier(this.getModel());
		List<String> activities = entity == null ? Arrays.asList(nodes.get(0)
				.getCode()) : entity.getStatus().equals(
				nodes.get(nodes.size() - 1).getId()) ? Arrays.asList(nodes.get(
				nodes.size() - 1).getCode()) : this.processEngine
				.getRuntimeService().getActiveActivityIds(entity.getProcess());
		InputStream is = diagramGenerator.generateDiagram(
				repositoryService.getBpmnModel(identifier), "png", activities,
				Collections.<String> emptyList(),
				configuration.getActivityFontName(),
				configuration.getLabelFontName(),
				configuration.getClassLoader(), 1.0);
		return new Nfile(name, is);
	}

	@Override
	public List<ActivityNode> nodes(Requester requester,
			Map<String, Object> parameters) {
		return this.getActivityNodes();
	}

}
