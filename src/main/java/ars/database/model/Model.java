package ars.database.model;

import java.util.Date;
import java.io.Serializable;

/**
 * 数据模型接口
 * 
 * @author yongqiangwu
 * 
 */
public interface Model extends Cloneable, Serializable {
	/**
	 * 获取主键
	 * 
	 * @return 主键
	 */
	public Integer getId();

	/**
	 * 设置主键
	 * 
	 * @param id
	 *            主键
	 */
	public void setId(Integer id);

	/**
	 * 获取排序
	 * 
	 * @return 排序
	 */
	public Double getOrder();

	/**
	 * 设置排序
	 * 
	 * @param order
	 *            排序
	 */
	public void setOrder(Double order);

	/**
	 * 获取数据状态值
	 * 
	 * @return 状态值
	 */
	public Integer getStatus();

	/**
	 * 设置数据状态值
	 * 
	 * @param status
	 *            状态值
	 */
	public void setStatus(Integer status);

	/**
	 * 是否有效
	 * 
	 * @return true/false
	 */
	public Boolean getActive();

	/**
	 * 设置是否有效
	 * 
	 * @param active
	 *            true/false
	 */
	public void setActive(Boolean active);

	/**
	 * 获取备注信息
	 * 
	 * @return 备注信息
	 */
	public String getRemark();

	/**
	 * 设置备注信息
	 * 
	 * @param remark
	 *            备注信息
	 */
	public void setRemark(String remark);

	/**
	 * 获取数据创建者
	 * 
	 * @return 创建者编号
	 */
	public String getCreator();

	/**
	 * 设置数据创建者
	 * 
	 * @param creator
	 *            创建者编号
	 */
	public void setCreator(String creator);

	/**
	 * 获取数据更新者
	 * 
	 * @return 更新者编号
	 */
	public String getUpdater();

	/**
	 * 设置数据更新者
	 * 
	 * @param updater
	 *            更新者编号
	 */
	public void setUpdater(String updater);

	/**
	 * 获取流程标识
	 * 
	 * @return 流程标识
	 */
	public String getProcess();

	/**
	 * 设置流程标识
	 * 
	 * @param process
	 *            流程标识
	 */
	public void setProcess(String process);

	/**
	 * 获取添加数据时间
	 * 
	 * @return 时间
	 */
	public Date getDateJoined();

	/**
	 * 设置添加数据时间
	 * 
	 * @param date
	 *            时间
	 */
	public void setDateJoined(Date date);

	/**
	 * 获取修改数据时间
	 * 
	 * @return 时间
	 */
	public Date getDateUpdate();

	/**
	 * 设置修改数据时间
	 * 
	 * @param date
	 *            时间
	 */
	public void setDateUpdate(Date date);

	/**
	 * 对象克隆
	 * 
	 * @return 对象副本
	 */
	public Model clone();

}
