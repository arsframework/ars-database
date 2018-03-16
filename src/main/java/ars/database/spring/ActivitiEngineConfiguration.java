package ars.database.spring;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Collections;

import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import ars.database.service.Workflows;
import ars.database.activiti.ActivityNode;
import ars.database.activiti.ProcessConfiguration;

/**
 * Activiti引擎配置
 * 
 * @author yongqiangwu
 * 
 */
public class ActivitiEngineConfiguration extends SpringProcessEngineConfiguration implements ProcessConfiguration,
		ApplicationListener<ApplicationEvent> {
	private boolean deploied; // 流程是否已经发布
	private Map<Class<?>, String> keys = new HashMap<Class<?>, String>(); // 模型/流程标识映射
	private Map<Class<?>, String> processes = new HashMap<Class<?>, String>(); // 模型/流程文件映射
	private Map<Class<?>, String> identifiers = new HashMap<Class<?>, String>(); // 模型/流程主键映射
	private Map<String, List<ActivityNode>> nodes = new HashMap<String, List<ActivityNode>>(); // 流程标识/流程节点列表映射

	public Map<Class<?>, String> getProcesses() {
		return processes;
	}

	public void setProcesses(Map<Class<?>, String> processes) {
		this.processes = processes;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);
		List<ActivitiEventListener> listeners = new ArrayList<ActivitiEventListener>(applicationContext.getBeansOfType(
				ActivitiEventListener.class).values());
		this.setEventListeners(listeners);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent && !this.deploied) {
			Workflows.setEngine(((ApplicationContext) event.getSource()).getBean(ProcessEngine.class));
			this.deploy(this.processes);
			this.deploied = true;
		}
	}

	@Override
	public String getKey(Class<?> model) {
		return this.keys.get(model);
	}

	@Override
	public String getIdentifier(Class<?> model) {
		return this.identifiers.get(model);
	}

	@Override
	public List<ActivityNode> getNodes(Class<?> model) {
		List<ActivityNode> nodes = this.nodes.get(this.getKey(model));
		return nodes == null ? Collections.<ActivityNode> emptyList() : Collections.unmodifiableList(nodes);
	}

	@Override
	public ActivityNode getNode(Class<?> model, int id) {
		List<ActivityNode> nodes = this.nodes.get(this.getKey(model));
		if (nodes == null || nodes.isEmpty()) {
			return null;
		}
		for (ActivityNode node : nodes) {
			if (node.getId() == id) {
				return node;
			}
		}
		return null;
	}

	@Override
	public ActivityNode getNode(Class<?> model, String code) {
		List<ActivityNode> nodes = this.nodes.get(this.getKey(model));
		if (nodes == null || nodes.isEmpty()) {
			return null;
		}
		for (ActivityNode node : nodes) {
			if (node.getCode().equals(code)) {
				return node;
			}
		}
		return null;
	}

	@Override
	public void deploy(Map<Class<?>, String> processes) {
		RepositoryService repositoryService = this.getRepositoryService();
		Map<String, Class<?>> resources = new HashMap<String, Class<?>>(0);
		for (Entry<Class<?>, String> entry : processes.entrySet()) {
			Class<?> model = entry.getKey();
			String resource = entry.getValue();
			resources.put(resource, model);
			String name = model.getName();
			Long count = repositoryService.createDeploymentQuery().deploymentName(name).count();
			if (count != null && count > 0) {
				continue;
			}
			repositoryService.createDeployment().name(name).addClasspathResource(resource).deploy();
		}

		List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
				.orderByProcessDefinitionVersion().asc().list();
		for (int i = 0; i < definitions.size(); i++) {
			ProcessDefinition definition = definitions.get(i);
			Class<?> model = resources.get(definition.getResourceName());
			this.keys.put(model, definition.getKey());
			this.identifiers.put(model, definition.getId());
			BpmnModel bpmnModel = repositoryService.getBpmnModel(definition.getId());
			if (bpmnModel != null) {
				int index = 1;
				FlowElement startEvent = null, endEvent = null;
				LinkedList<ActivityNode> nodes = new LinkedList<ActivityNode>();
				Collection<FlowElement> elements = bpmnModel.getMainProcess().getFlowElements();
				for (FlowElement element : elements) {
					if (element instanceof StartEvent) {
						startEvent = element;
					} else if (element instanceof EndEvent) {
						endEvent = element;
					} else if (element instanceof UserTask) {
						nodes.add(new ActivityNode(index++, element.getId(), element.getName()));
					}
				}
				if (startEvent == null) {
					throw new RuntimeException("Start element not found");
				}
				if (endEvent == null) {
					throw new RuntimeException("End element not found");
				}
				nodes.addFirst(new ActivityNode(0, startEvent.getId(), startEvent.getName()));
				nodes.add(new ActivityNode(index, endEvent.getId(), endEvent.getName()));
				this.nodes.put(definition.getKey(), nodes);
			}
		}
	}

}
