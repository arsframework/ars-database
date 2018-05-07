package ars.database.activiti;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;
import org.activiti.engine.impl.persistence.entity.UserIdentityManager;

/**
 * Activiti用户会话工厂简单实现
 *
 * @author wuyongqiang
 */
public class SimpleUserSessionFactory implements SessionFactory {
    protected final UserEntityManager manager;

    public SimpleUserSessionFactory(UserEntityManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("UserEntityManager must not be null");
        }
        this.manager = manager;
    }

    @Override
    public Class<?> getSessionType() {
        return UserIdentityManager.class;
    }

    @Override
    public Session openSession() {
        return this.manager;
    }

}
