package ars.database.service.event;

import ars.invoke.request.Requester;
import ars.database.repository.Query;
import ars.database.service.Service;
import ars.database.service.event.ServiceEvent;

/**
 * 模型实体查询事件
 * 
 * @author yongqiangwu
 * 
 */
public class QueryEvent extends ServiceEvent {
	private static final long serialVersionUID = 1L;

	private transient Query<?> query; // 对象查询集合

	public QueryEvent(Requester requester, Service<?> service, Query<?> query) {
		super(requester, service);
		if (query == null) {
			throw new IllegalArgumentException("Illegal query:" + query);
		}
		this.query = query;
	}

	public Query<?> getQuery() {
		return query;
	}

}
