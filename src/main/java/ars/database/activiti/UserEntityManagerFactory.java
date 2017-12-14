package ars.database.activiti;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;
import org.activiti.engine.impl.persistence.entity.UserIdentityManager;

/**
 * 用户管理工厂实现
 * 
 * @author yongqiangwu
 * 
 */
public class UserEntityManagerFactory implements SessionFactory {
	private UserEntityManager manager;

	public UserEntityManager getManager() {
		return manager;
	}

	public void setManager(UserEntityManager manager) {
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
