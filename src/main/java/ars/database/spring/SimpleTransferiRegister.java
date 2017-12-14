package ars.database.spring;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ars.database.repository.Transform;
import ars.database.repository.TransferManager;

/**
 * 本地接口资源注册简单实现
 * 
 * @author yongqiangwu
 * 
 */
public class SimpleTransferiRegister implements ApplicationContextAware {
	private Class<?> model; // 接口地址
	private String property; // 属性名称
	private Transform transform; // 转换对象
	private Map<String, Transform> transforms;

	public SimpleTransferiRegister(Class<?> model,
			Map<String, Transform> transforms) {
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		if (transforms == null || transforms.isEmpty()) {
			throw new IllegalArgumentException("Illegal transforms:"
					+ transforms);
		}
		this.model = model;
		this.transforms = transforms;
	}

	public SimpleTransferiRegister(Class<?> model, String property,
			Transform transform) {
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		if (property == null) {
			throw new IllegalArgumentException("Illegal property:" + property);
		}
		if (transform == null) {
			throw new IllegalArgumentException("Illegal transform:" + transform);
		}
		this.model = model;
		this.property = property;
		this.transform = transform;
	}

	public Class<?> getModel() {
		return model;
	}

	public Map<String, Transform> getTransforms() {
		return transforms;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		TransferManager manager = applicationContext
				.getBean(TransferManager.class);
		if (this.property == null) {
			manager.register(this.model, this.transforms);
		} else {
			manager.register(this.model, this.property, this.transform);
		}
	}

}
