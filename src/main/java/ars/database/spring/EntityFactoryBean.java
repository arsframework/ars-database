package ars.database.spring;

import java.util.Map;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.FactoryBean;

import ars.util.Beans;
import ars.database.repository.Query;
import ars.database.repository.RepositoryFactory;

/**
 * 对象实体工厂类
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            对象类型
 */
public class EntityFactoryBean implements FactoryBean<Object> {
	private Object entity; // 模型实体（设置表示默认值）
	private boolean loaded; // 实体是否已加载
	private Class<?> model; // 对象模型
	private boolean multiple; // 是否获取多个
	private Map<String, Object> attributes; // 实体属性

	@Resource
	private RepositoryFactory repositoryFactory;

	public Class<?> getModel() {
		return model;
	}

	public void setModel(Class<?> model) {
		this.model = model;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public Object getObject() throws Exception {
		if (!this.loaded) {
			if (this.model == null) {
				throw new RuntimeException("Model has not been initialize");
			}
			if (this.attributes == null || this.attributes.isEmpty()) {
				throw new RuntimeException("Attributes has not been initialize");
			}
			this.loaded = true;
			Query<?> query = this.repositoryFactory.getRepository(this.model)
					.query().custom(this.attributes);
			if (this.multiple) {
				List<?> objects = query.list();
				if (Beans.isEmpty(this.entity) || !objects.isEmpty()) {
					this.entity = objects;
				}
			} else {
				Object object = query.single();
				if (Beans.isEmpty(this.entity) || object != null) {
					this.entity = object;
				}
			}
		}
		return this.entity;
	}

	@Override
	public Class<?> getObjectType() {
		return this.multiple ? List.class : this.model;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
