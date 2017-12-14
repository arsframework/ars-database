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

	private Type type;
	private transient Service<?> service;

	public ServiceEvent(Requester requester, Type type, Service<?> service) {
		super(requester);
		if (type == null) {
			throw new IllegalArgumentException("Illegal type:" + type);
		}
		if (service == null) {
			throw new IllegalArgumentException("Illegal service:" + service);
		}
		this.type = type;
		this.service = service;
	}

	public Type getType() {
		return type;
	}

	public Service<?> getService() {
		return service;
	}

	/**
	 * 事件类型
	 * 
	 * @author yongqiangwu
	 * 
	 */
	public enum Type {
		/**
		 * 初始类型
		 */
		INIT,

		/**
		 * 保存类型
		 */
		SAVE,

		/**
		 * 查询类型
		 */
		QUERY,

		/**
		 * 修改类型
		 */
		UPDATE,

		/**
		 * 删除类型
		 */
		DELETE;

	}

}
