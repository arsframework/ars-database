package ars.database.service.event;

import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 对象实例初始化事件
 *
 * @author wuyongqiang
 */
public class InitEvent extends ServiceEvent {
    private static final long serialVersionUID = 1L;

    private transient Object entity; // 对象实体

    public InitEvent(Requester requester, Service<?> service, Object entity) {
        super(requester, service);
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

}
