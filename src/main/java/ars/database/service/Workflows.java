package ars.database.service;

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

import ars.util.Beans;
import ars.database.model.Model;
import ars.database.repository.Query;
import ars.database.repository.Repositories;
import ars.database.activiti.ActivityNode;
import ars.database.activiti.ProcessConfiguration;
import ars.invoke.request.Requester;

/**
 * 工作流处理工具类
 * 
 * @author yongqiangwu
 *
 */
public final class Workflows {
	private static ProcessEngine engine;

	private Workflows() {

	}

	/**
	 * 获取流程引擎对象
	 * 
	 * @return 流程引擎对象
	 */
	public static ProcessEngine getEngine() {
		if (engine == null) {
			throw new RuntimeException("Process engine has not been initialize");
		}
		return engine;
	}

	/**
	 * 设置流程引擎对象
	 * 
	 * @param engine
	 *            流程引擎对象
	 */
	public static void setEngine(ProcessEngine engine) {
		if (engine == null) {
			throw new IllegalArgumentException("Illegal engine:" + engine);
		}
		if (Workflows.engine == null) {
			synchronized (Workflows.class) {
				if (Workflows.engine == null) {
					Workflows.engine = engine;
				}
			}
		}
	}

	/**
	 * 获取流程配置
	 * 
	 * @return 流程配置对象
	 */
	public static ProcessConfiguration getConfiguration() {
		return ((ProcessConfiguration) getEngine().getProcessEngineConfiguration());
	}

	/**
	 * 获取流程节点
	 * 
	 * @param model
	 *            数据模型
	 * @return 流程节点列表
	 */
	public static List<ActivityNode> getNodes(Class<?> model) {
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		return getConfiguration().getNodes(model);
	}

	/**
	 * 获取流程实例
	 * 
	 * @param <T>
	 *            数据类型
	 * @param id
	 *            流程标识
	 * @return 流程实例
	 */
	public static <T extends Model> ProcessInstance getProcess(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Illegal id:" + id);
		}
		return getEngine().getRuntimeService().createProcessInstanceQuery().processInstanceId(id).singleResult();
	}

	/**
	 * 获取用户任务实例
	 * 
	 * @param <T>
	 *            数据类型
	 * @param process
	 *            流程标识
	 * @param assignee
	 *            任务接收者标识
	 * @return 任务对象
	 */
	public static <T extends Model> Task getTask(String process, String assignee) {
		if (process == null) {
			throw new IllegalArgumentException("Illegal process:" + process);
		}
		if (assignee == null) {
			throw new IllegalArgumentException("Illegal assignee:" + assignee);
		}
		return getEngine().getTaskService().createTaskQuery().taskCandidateUser(assignee).processInstanceId(process)
				.singleResult();
	}

	/**
	 * 启动流程
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @param entity
	 *            对象实体
	 * @return 工作流实例
	 */
	public static <T extends Model> ProcessInstance startProcess(Requester requester, Service<T> service, T entity) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		if (entity == null) {
			throw new IllegalArgumentException("Illegal entity:" + entity);
		}
		if (entity.getProcess() != null) {
			throw new RuntimeException("Process is already started:" + entity);
		}
		RuntimeService runtimeService = getEngine().getRuntimeService();
		ProcessConfiguration configuration = getConfiguration();
		ProcessInstance process = runtimeService.startProcessInstanceByKey(configuration.getKey(service.getModel()),
				requester.getParameters());
		List<ActivityNode> nodes = configuration.getNodes(service.getModel());
		entity.setActive(false);
		entity.setProcess(process.getId());
		if (nodes.size() > 1) {
			entity.setStatus(nodes.get(1).getId());
		}
		service.updateObject(requester, entity);
		return process;
	}

	/**
	 * 批量启动流程
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 */
	public static <T extends Model> void startProcess(Requester requester, Service<T> service) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		String primary = service.getRepository().getPrimary();
		Object[] identifiers = Beans.toArray(Object.class, requester.getParameter(primary));
		if (identifiers.length > 0) {
			List<T> objects = service.getQuery(requester).or(primary, identifiers).list();
			for (int i = 0; i < objects.size(); i++) {
				startProcess(requester, service, objects.get(i));
			}
		}
	}

	/**
	 * 完成任务
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @param entity
	 *            对象实体
	 * @param assignee
	 *            任务接收者标识
	 * @return 任务对象
	 */
	public static <T extends Model> Task completeTask(Requester requester, Service<T> service, T entity,
			String assignee) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		if (entity == null) {
			throw new IllegalArgumentException("Illegal entity:" + entity);
		}
		if (assignee == null) {
			throw new IllegalArgumentException("Illegal assignee:" + assignee);
		}
		Task task = getTask(entity.getProcess(), assignee);
		if (task == null) {
			throw new RuntimeException("Task is already processed:" + entity);
		}
		TaskService taskService = getEngine().getTaskService();
		taskService.claim(task.getId(), assignee);
		ProcessConfiguration configuration = getConfiguration();
		ActivityNode node = configuration.getNode(service.getModel(), entity.getStatus()); // 当前节点
		if (node == null) {
			throw new RuntimeException("Activity node does not exist with id:" + entity.getStatus());
		}
		taskService.complete(task.getId(), requester.getParameters());
		synchronized (entity.getProcess().intern()) {
			if (getProcess(entity.getProcess()) == null) { // 流程已完成
				entity.setActive(true);
				List<ActivityNode> nodes = configuration.getNodes(service.getModel());
				if (!nodes.isEmpty()) {
					entity.setStatus(nodes.get(nodes.size() - 1).getId());
				}
				service.updateObject(requester, entity);
			} else { // 节点改变
				List<String> activities = getEngine().getRuntimeService().getActiveActivityIds(entity.getProcess());
				if (activities.isEmpty() || !activities.get(0).equals(node.getCode())) {
					String activity = activities.get(0);
					ActivityNode next = configuration.getNode(service.getModel(), activity);
					if (next == null) {
						throw new RuntimeException("Activity node does not exist with code:" + activity);
					}
					entity.setStatus(next.getId());
					service.updateObject(requester, entity);
				}
			}
		}
		return task;
	}

	/**
	 * 批量完成任务
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 */
	public static <T extends Model> void completeTask(Requester requester, Service<T> service) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		String primary = service.getRepository().getPrimary();
		Object[] identifiers = Beans.toArray(Object.class, requester.getParameter(primary));
		if (identifiers.length > 0) {
			List<T> objects = service.getQuery(requester).or(primary, identifiers).list();
			for (int i = 0; i < objects.size(); i++) {
				completeTask(requester, service, objects.get(i), requester.getUser());
			}
		}
	}

	/**
	 * 获取用户为完成任务对象集合
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @param assignee
	 *            任务接收者标识
	 * @return 对象集合
	 */
	public static <T extends Model> Query<T> getTaskQuery(Requester requester, Service<T> service, String assignee) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		if (assignee == null) {
			throw new IllegalArgumentException("Illegal assignee:" + assignee);
		}
		List<Task> tasks = getEngine().getTaskService().createTaskQuery().taskCandidateUser(assignee).list();
		if (tasks.isEmpty()) {
			return Repositories.emptyQuery();
		}
		String[] processes = new String[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			processes[i] = tasks.get(i).getProcessInstanceId();
		}
		return service.getQuery(requester).in("process", processes).custom(requester.getParameters());
	}

	/**
	 * 获取用户已完成任务对象集合
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @param assignee
	 *            任务接收者标识
	 * @return 对象集合
	 */
	public static <T extends Model> Query<T> getFinishQuery(Requester requester, Service<T> service, String assignee) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		if (assignee == null) {
			throw new IllegalArgumentException("Illegal assignee:" + assignee);
		}
		HistoryService historyService = getEngine().getHistoryService();
		List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
				.taskAssignee(assignee).list();
		if (historicTaskInstances.isEmpty()) {
			return Repositories.emptyQuery();
		}
		String[] processes = new String[historicTaskInstances.size()];
		for (int i = 0; i < historicTaskInstances.size(); i++) {
			processes[i] = historicTaskInstances.get(i).getProcessInstanceId();
		}
		return service.getQuery(requester).in("process", processes).custom(requester.getParameters());
	}

	/**
	 * 获取当前用户任务量
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @return 任务量
	 */
	public static <T extends Model> int getWorkload(Requester requester, Service<T> service) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		return getTaskQuery(requester, service, requester.getUser()).count();
	}

	/**
	 * 获取当前用户任务列表
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @return 任务列表
	 */
	public static <T extends Model> List<T> getTasks(Requester requester, Service<T> service) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		return getTaskQuery(requester, service, requester.getUser()).list();
	}

	/**
	 * 获取当前用户任务进度（已完成任务量）
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @return 已完成任务量
	 */
	public static <T extends Model> int getProgress(Requester requester, Service<T> service) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		return getFinishQuery(requester, service, requester.getUser()).count();
	}

	/**
	 * 获取当前用户历史任务
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @return 历史任务列表
	 */
	public static <T extends Model> List<T> getHistories(Requester requester, Service<T> service) {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		return getFinishQuery(requester, service, requester.getUser()).list();
	}

	/**
	 * 获取流程图（如果参数为空则获取部署流程图，如果参数不为空则获取实例流程图）
	 * 
	 * @param <T>
	 *            数据类型
	 * @param requester
	 *            请求对象
	 * @param service
	 *            业务处理对象
	 * @return 流程图数据输入流
	 * @throws IOException
	 *             IO操作异常
	 */
	public static <T extends Model> InputStream getDiagram(Requester requester, Service<T> service) throws IOException {
		if (requester == null) {
			throw new IllegalArgumentException("Illegal requester:" + requester);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		T entity = service.getQuery(requester, true).custom(requester.getParameters()).single();
		List<ActivityNode> nodes = getNodes(service.getModel());
		ProcessEngineConfiguration configuration = getEngine().getProcessEngineConfiguration();
		ProcessDiagramGenerator diagramGenerator = configuration.getProcessDiagramGenerator();
		RepositoryService repositoryService = getEngine().getRepositoryService();
		String identifier = ((ProcessConfiguration) configuration).getIdentifier(service.getModel());
		List<String> activities = entity == null ? Arrays.asList(nodes.get(0).getCode())
				: entity.getStatus().equals(nodes.get(nodes.size() - 1).getId())
						? Arrays.asList(nodes.get(nodes.size() - 1).getCode())
						: getEngine().getRuntimeService().getActiveActivityIds(entity.getProcess());
		return diagramGenerator.generateDiagram(repositoryService.getBpmnModel(identifier), "png", activities,
				Collections.<String>emptyList(), configuration.getActivityFontName(), configuration.getLabelFontName(),
				configuration.getClassLoader(), 1.0);
	}

}
