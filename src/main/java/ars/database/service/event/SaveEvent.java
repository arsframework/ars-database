package ars.database.service.event;

import java.io.Serializable;

import ars.invoke.request.Requester;
import ars.database.service.Service;
import ars.database.service.event.ServiceEvent;

/**
 * 模型实体保存事件
 * 
 * @author yongqiangwu
 * 
 */
public class SaveEvent extends ServiceEvent {
	private static final long serialVersionUID = 1L;

	private Serializable id; // 对象主键
	private transient Object entity; // 对象实体

	public SaveEvent(Requester requester, Service<?> service, Serializable id, Object entity) {
		super(requester, service);
		if (id == null) {
			throw new IllegalArgumentException("Illegal id:" + id);
		}
		if (entity == null) {
			throw new IllegalArgumentException("Illegal entity:" + entity);
		}
		this.id = id;
		this.entity = entity;
	}

	public Serializable getId() {
		return id;
	}

	public Object getEntity() {
		return entity;
	}

}
