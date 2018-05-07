package ars.database.hibernate;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ars.database.repository.Query;
import ars.database.repository.AbstractRepository;

/**
 * 基于Hibernate数据持久化操作简单实现
 *
 * @param <T> 数据模型
 * @author wuyongqiang
 */
public class HibernateSimpleRepository<T> extends AbstractRepository<T> {
    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * 获取Session
     *
     * @return Session对象
     */
    protected Session getSession() {
        if (this.sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not initialize");
        }
        Session session = this.sessionFactory.getCurrentSession();
        session.clear();
        return session;
    }

    @Override
    protected void modify(T object) {
        if (object != null) {
            this.getSession().update(object);
        }
    }

    @Override
    protected Serializable insert(T object) {
        return object == null ? null : this.getSession().save(object);
    }

    @Override
    protected void remove(T object) {
        if (object != null) {
            this.getSession().delete(object);
        }
    }

    @Override
    public Query<T> query() {
        return new DetachedCriteriaQuery<T>(this.sessionFactory, this.getModel());
    }

}
