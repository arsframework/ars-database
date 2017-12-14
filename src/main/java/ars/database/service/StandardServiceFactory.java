package ars.database.service;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import ars.database.service.Service;
import ars.database.service.ServiceFactory;

/**
 * 业务操作工厂标准实现
 * 
 * @author yongqiangwu
 * 
 */
public class StandardServiceFactory implements ServiceFactory {
	private Map<Class<?>, Service<?>> services = Collections.emptyMap();

	public StandardServiceFactory(Service<?>... services) {
		this.services = new HashMap<Class<?>, Service<?>>(services.length);
		for (Service<?> service : services) {
			this.services.put(service.getModel(), service);
		}
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
			throw new RuntimeException("Service not found:" + model);
		}
		return service;
	}

}
