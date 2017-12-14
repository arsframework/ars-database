package ars.database.activiti;

import java.io.Serializable;

import ars.util.Strings;

/**
 * Activiti活动节点
 * 
 * @author yongqiangwu
 * 
 */
public class ActivityNode implements Serializable {
	private static final long serialVersionUID = 1L;

	private int id; // 状态标识
	private String code; // 状态编号
	private String name; // 状态名称

	public ActivityNode(int id, String code, String name) {
		if (Strings.isEmpty(code)) {
			throw new IllegalArgumentException("Illegal code:" + code);
		}
		if (Strings.isEmpty(name)) {
			throw new IllegalArgumentException("Illegal name:" + name);
		}
		this.id = id;
		this.code = code;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return 31 + this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ActivityNode)) {
			return false;
		}
		return this.code.equals(((ActivityNode) obj).getCode());
	}

	@Override
	public String toString() {
		return this.name;
	}

}
