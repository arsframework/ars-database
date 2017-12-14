package ars.database.activiti;

import java.util.Map;
import java.util.List;

import org.activiti.engine.EngineServices;

/**
 * 流程配置接口
 * 
 * @author yongqiangwu
 * 
 */
public interface ProcessConfiguration extends EngineServices {
	/**
	 * 获取对象流程标识
	 * 
	 * @param model
	 *            对象模型
	 * @return 流程标识
	 */
	public String getKey(Class<?> model);

	/**
	 * 获取对象流程标识（唯一）
	 * 
	 * @param model
	 *            对象模型
	 * @return 流程标识
	 */
	public String getIdentifier(Class<?> model);

	/**
	 * 获取流程节点列表
	 * 
	 * @param model
	 *            对象模型
	 * @return 流程节点列表
	 */
	public List<ActivityNode> getNodes(Class<?> model);

	/**
	 * 根据节点主键获取节点对象
	 * 
	 * @param model
	 *            数据模型
	 * @param id
	 *            节点主键
	 * @return 节点对象
	 */
	public ActivityNode getNode(Class<?> model, int id);

	/**
	 * 根据节点标识获取节点对象
	 * 
	 * @param model
	 *            数据模型
	 * @param code
	 *            节点标识
	 * @return 节点对象
	 */
	public ActivityNode getNode(Class<?> model, String code);

	/**
	 * 流程发布
	 * 
	 * @param processes
	 *            数据模型/流程映射表
	 */
	public void deploy(Map<Class<?>, String> processes);

}
