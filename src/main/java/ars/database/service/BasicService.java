package ars.database.service;

import ars.database.service.AddService;
import ars.database.service.DeleteService;
import ars.database.service.UpdateService;
import ars.database.service.SearchService;

/**
 * 基础的业务操作接口
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface BasicService<T> extends AddService<T>, DeleteService<T>,
		UpdateService<T>, SearchService<T> {

}
