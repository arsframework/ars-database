package ars.database.spring;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Collections;
import java.lang.reflect.Method;

import org.activiti.engine.ProcessEngine;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ars.database.service.Service;
import ars.database.service.Services;
import ars.database.service.ServiceFactory;
import ars.database.service.WorkflowService;
import ars.database.service.event.ServiceEvent;
import ars.database.service.event.ServiceListener;
import ars.database.repository.Repository;
import ars.database.repository.Repositories;
import ars.database.repository.RepositoryFactory;
import ars.database.repository.DataConstraintException;
import ars.invoke.convert.ThrowableResolver;

/**
 * 基于Spring数据操作配置
 * 
 * @author yongqiangwu
 * 
 */
public class DatabaseConfiguration
		implements ThrowableResolver, ServiceFactory, RepositoryFactory, ApplicationContextAware {
	/**
	 * 数据关联异常编码
	 */
	public static final int CODE_ERROR_DATA_CONSTRAINT = 52070;

	private Map<Class<?>, Service<?>> services = Collections.emptyMap();
	private Map<Class<?>, Repository<?>> repositories = Collections.emptyMap();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Services.setServiceFactory(this);
		Repositories.setRepositoryFactory(this);

		// 加载数据模型对应的持久化操作对象
		Collection<Repository> repositories = applicationContext.getBeansOfType(Repository.class).values();
		this.repositories = new HashMap<Class<?>, Repository<?>>(repositories.size());
		for (Repository repository : repositories) {
			this.repositories.put(repository.getModel(), repository);
		}

		// 加载数据模型对应业务操作对象
		Collection<Service> services = applicationContext.getBeansOfType(Service.class).values();
		this.services = new HashMap<Class<?>, Service<?>>(services.size());
		for (Service service : services) {
			if (service instanceof WorkflowService) {
				((WorkflowService<?>) service).setProcessEngine(applicationContext.getBean(ProcessEngine.class));
			}
			this.services.put(service.getModel(), service);
		}

		// 初始化业务操作事件监听器
		Map<Class, List<ServiceListener>> listeners = new HashMap<Class, List<ServiceListener>>();
		try {
			for (Entry<String, ServiceListener> entry : applicationContext.getBeansOfType(ServiceListener.class)
					.entrySet()) {
				ServiceListener target = null;
				ServiceListener listener = entry.getValue();
				if (AopUtils.isAopProxy(listener)) {
					target = (ServiceListener) ((Advised) listener).getTargetSource().getTarget();
				}
				Class<?> type = null;
				for (Method method : (target == null ? listener : target).getClass().getMethods()) {
					if (method.getName().equals("onServiceEvent") && (type == null || type == ServiceEvent.class)) {
						type = method.getParameterTypes()[0];
					}
				}
				List<ServiceListener> groups = listeners.get(type);
				if (groups == null) {
					groups = new LinkedList<ServiceListener>();
					listeners.put(type, groups);
				}
				groups.add(listener);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (!listeners.isEmpty()) {
			for (Entry<Class, List<ServiceListener>> entry : listeners.entrySet()) {
				Class type = entry.getKey();
				ServiceListener[] _listeners = entry.getValue().toArray(new ServiceListener[0]);
				for (Service service : services) {
					service.setListeners(type, _listeners);
				}
			}
		}
	}

	@Override
	public int getCode(Throwable throwable) {
		return CODE_ERROR_DATA_CONSTRAINT;
	}

	@Override
	public String getMessage(Throwable throwable) {
		return throwable.getMessage();
	}

	@Override
	public boolean isResolvable(Throwable throwable) {
		return throwable != null && throwable instanceof DataConstraintException;
	}

	@Override
	public Map<Class<?>, Service<?>> getServices() {
		return Collections.unmodifiableMap(this.services);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Service<T> getService(Class<T> model) {
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		Service<T> service = (Service<T>) this.services.get(model);
		if (service == null) {
			synchronized (model) {
				service = (Service<T>) this.services.get(model);
				if (service == null) {
					for (Entry<Class<?>, Service<?>> entry : this.services.entrySet()) {
						if (entry.getKey().isAssignableFrom(model)) {
							service = (Service<T>) entry.getValue();
							this.services.put(model, service);
						}
					}
				}
			}
		}
		if (service == null) {
			throw new RuntimeException("Service not found:" + model);
		}
		return service;
	}

	@Override
	public Map<Class<?>, Repository<?>> getRepositories() {
		return Collections.unmodifiableMap(this.repositories);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Repository<T> getRepository(Class<T> model) {
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		Repository<T> repository = (Repository<T>) this.repositories.get(model);
		if (repository == null) {
			synchronized (model) {
				repository = (Repository<T>) this.repositories.get(model);
				if (repository == null) {
					for (Entry<Class<?>, Repository<?>> entry : this.repositories.entrySet()) {
						if (entry.getKey().isAssignableFrom(model)) {
							repository = (Repository<T>) entry.getValue();
							this.repositories.put(model, repository);
						}
					}
				}
			}
		}
		if (repository == null) {
			throw new RuntimeException("Repository not found:" + model);
		}
		return repository;
	}

}
