package ars.database.service.event;

import ars.invoke.request.Requester;
import ars.database.service.Service;
import ars.database.service.event.ServiceEvent;

/**
 * 模型实体修改事件
 * 
 * @author yongqiangwu
 * 
 */
public class UpdateEvent extends ServiceEvent {
	private static final long serialVersionUID = 1L;

	private transient Object entity; // 对象实体

	public UpdateEvent(Requester requester, Service<?> service, Object entity) {
		super(requester, Type.UPDATE, service);
		if (entity == null) {
			throw new IllegalArgumentException("Illegal entity:" + entity);
		}
		this.entity = entity;
	}

	public Object getEntity() {
		return entity;
	}

}
