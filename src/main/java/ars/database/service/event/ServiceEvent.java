package ars.database.service.event;

import ars.invoke.event.InvokeEvent;
import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 业务操作事件模型
 * 
 * @author yongqiangwu
 * 
 */
public abstract class ServiceEvent extends InvokeEvent {
	private static final long serialVersionUID = 1L;

	private transient Service<?> service;

	public ServiceEvent(Requester requester, Service<?> service) {
		super(requester);
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		this.service = service;
	}

	public Service<?> getService() {
		return service;
	}

}
