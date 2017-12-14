package ars.database.activiti;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.engine.impl.persistence.entity.GroupIdentityManager;

/**
 * 用户组管理工厂实现
 * 
 * @author yongqiangwu
 * 
 */
public class GroupEntityManagerFactory implements SessionFactory {
	private GroupEntityManager manager;

	public GroupEntityManager getManager() {
		return manager;
	}

	public void setManager(GroupEntityManager manager) {
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
