package ars.database.model;

import java.util.Date;

import ars.database.model.Model;

/**
 * 基础数据模型抽象实现
 * 
 * @author yongqiangwu
 * 
 */
public abstract class AbstractModel implements Model {
	private static final long serialVersionUID = 1L;

	private Integer id; // 主键
	private Double order; // 排序
	private Integer status = 0; // 当前状态
	private Boolean active = true; // 是否有效
	private String remark; // 备注
	private String creator; // 创建用户
	private String updater; // 更新用户
	private String process; // 流程标识
	private Date dateJoined = new Date(); // 创建时间
	private Date dateUpdate; // 更新时间

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public Double getOrder() {
		return order;
	}

	@Override
	public void setOrder(Double order) {
		this.order = order;
	}

	@Override
	public Integer getStatus() {
		return status;
	}

	@Override
	public void setStatus(Integer status) {
		this.status = status;
	}

	@Override
	public Boolean getActive() {
		return active;
	}

	@Override
	public void setActive(Boolean active) {
		this.active = active;
	}

	@Override
	public String getRemark() {
		return remark;
	}

	@Override
	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Override
	public String getCreator() {
		return creator;
	}

	@Override
	public void setCreator(String creator) {
		this.creator = creator;
	}

	@Override
	public String getUpdater() {
		return updater;
	}

	@Override
	public void setUpdater(String updater) {
		this.updater = updater;
	}

	@Override
	public String getProcess() {
		return process;
	}

	@Override
	public void setProcess(String process) {
		this.process = process;
	}

	@Override
	public Date getDateJoined() {
		return dateJoined;
	}

	@Override
	public void setDateJoined(Date dateJoined) {
		this.dateJoined = dateJoined;
	}

	@Override
	public Date getDateUpdate() {
		return dateUpdate;
	}

	@Override
	public void setDateUpdate(Date dateUpdate) {
		this.dateUpdate = dateUpdate;
	}

	@Override
	public Model clone() {
		try {
			return (Model) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		return this.id == null ? super.hashCode() : 31 + this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.id == null) {
			return super.equals(obj);
		} else if (this == obj) {
			return true;
		}
		return obj != null && obj instanceof AbstractModel && this.id.equals(((AbstractModel) obj).getId());
	}

	@Override
	public String toString() {
		return this.id == null ? super.toString()
				: new StringBuilder(this.getClass().getName()).append('#').append(this.id).toString();
	}

}
