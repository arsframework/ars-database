package ars.database.activiti;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.engine.impl.persistence.entity.GroupIdentityManager;

/**
 * Activiti用户组会话工厂简单实现
 *
 * @author wuyongqiang
 */
public class SimpleGroupSessionFactory implements SessionFactory {
    protected final GroupEntityManager manager;

    public SimpleGroupSessionFactory(GroupEntityManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("GroupEntityManager must not be null");
        }
        this.manager = manager;
    }

    @Override
    public Class<?> getSessionType() {
        return GroupIdentityManager.class;
    }

    @Override
    public Session openSession() {
        return this.manager;
    }

}
