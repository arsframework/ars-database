package ars.database.repository;

import ars.util.Strings;

/**
 * 数据转换对象
 * 
 * @author yongqiangwu
 * 
 */
public class Transform {
	private String key; // 当前对象标识
	private String target; // 目标对象标识
	private String resource; // 对象查询资源接口地址
	private boolean lazy; // 是否延迟加载

	public Transform(String key, String target, String resource) {
		this(key, target, resource, true);
	}

	public Transform(String key, String target, String resource, boolean lazy) {
		if (Strings.isEmpty(key) || (key = key.trim()).isEmpty()) {
			throw new IllegalArgumentException("Illegal key:" + key);
		}
		if (Strings.isEmpty(target) || (target = target.trim()).isEmpty()) {
			throw new IllegalArgumentException("Illegal target:" + target);
		}
		if (Strings.isEmpty(resource) || (resource = resource.trim()).isEmpty()) {
			throw new IllegalArgumentException("Illegal resource:" + resource);
		}
		this.key = key;
		this.target = target;
		this.resource = resource;
		this.lazy = lazy;
	}

	public String getKey() {
		return key;
	}

	public String getTarget() {
		return target;
	}

	public String getResource() {
		return resource;
	}

	public boolean isLazy() {
		return lazy;
	}

}
