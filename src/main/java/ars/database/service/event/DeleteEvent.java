package ars.database.service.event;

import ars.invoke.request.Requester;
import ars.database.service.Service;
import ars.database.service.event.ServiceEvent;

/**
 * 模型实体删除事件
 * 
 * @author yongqiangwu
 * 
 */
public class DeleteEvent extends ServiceEvent {
	private static final long serialVersionUID = 1L;

	private transient Object entity; // 对象实体

	public DeleteEvent(Requester requester, Service<?> service, Object entity) {
		super(requester, service);
		if (entity == null) {
			throw new IllegalArgumentException("Illegal entity:" + entity);
		}
		this.entity = entity;
	}

	public Object getEntity() {
		return entity;
	}

}
