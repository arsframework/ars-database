package ars.database.model;

import ars.util.Tree;
import ars.database.model.Model;

/**
 * 树形结构数据模型
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            树对象类型
 */
public interface TreeModel<T extends TreeModel<T>> extends Model, Tree<T> {
	/**
	 * 获取树标识
	 * 
	 * @return 树标识
	 */
	public String getKey();

	/**
	 * 设置树标识
	 * 
	 * @param key
	 *            树标识
	 */
	public void setKey(String key);

}
