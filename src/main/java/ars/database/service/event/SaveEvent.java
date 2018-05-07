package ars.database.service.event;

import ars.invoke.request.Requester;
import ars.database.service.Service;

/**
 * 模型实体保存事件
 *
 * @author wuyongqiang
 */
public class SaveEvent extends ServiceEvent {
    private static final long serialVersionUID = 1L;

    private transient Object entity; // 对象实体

    public SaveEvent(Requester requester, Service<?> service, Object entity) {
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
