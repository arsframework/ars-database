package ars.database.service.event;

import java.util.EventListener;

import ars.database.service.event.ServiceEvent;

/**
 * 业务操作事件监听器，监听器在真正业务操作执行之前执行
 * 
 * @author yongqiangwu
 * 
 * @param <E>
 *            事件类型
 */
public interface ServiceListener<E extends ServiceEvent> extends EventListener {
	/**
	 * 事件监听
	 * 
	 * @param event
	 *            事件对象
	 */
	public void onServiceEvent(E event);

}
