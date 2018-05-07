package ars.database.service.event;

import ars.invoke.request.Requester;
import ars.database.service.Service;
import ars.database.repository.Query;

/**
 * 模型实体查询事件
 *
 * @author wuyongqiang
 */
public class QueryEvent extends ServiceEvent {
    private static final long serialVersionUID = 1L;

    private transient Query<?> query; // 对象查询集合

    public QueryEvent(Requester requester, Service<?> service, Query<?> query) {
        super(requester, service);
        if (query == null) {
            throw new IllegalArgumentException("Query must not be null");
        }
        this.query = query;
    }

    public Query<?> getQuery() {
        return query;
    }

}
