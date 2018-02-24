package ars.database.service.event;

import ars.invoke.request.Requester;
import ars.util.Beans;
import ars.database.service.Service;
import ars.database.service.event.ServiceEvent;
import ars.database.repository.Repository;

/**
 * 模型实体修改事件
 * 
 * @author yongqiangwu
 * 
 */
public class UpdateEvent extends ServiceEvent {
	private static final long serialVersionUID = 1L;

	private transient Object entity; // 当前对象实体
	private transient Object original; // 原始对象实体

	public UpdateEvent(Requester requester, Service<?> service, Object entity) {
		super(requester, service);
		if (entity == null) {
			throw new IllegalArgumentException("Illegal entity:" + entity);
		}
		this.entity = entity;
	}

	public Object getEntity() {
		return entity;
	}

	public Object getOriginal() {
		if (this.original == null) {
			Repository<?> repository = this.getService().getRepository();
			this.original = repository.get(Beans.getValue(this.entity, repository.getPrimary()));
		}
		return original;
	}

}
