package ars.database.service;

import java.util.Map;

/**
 * 业务操作工厂接口
 *
 * @author wuyongqiang
 */
public interface ServiceFactory {
    /**
     * 获取所有业务操作对象
     *
     * @return 数据模型/业务操作对象映射
     */
    public Map<Class<?>, Service<?>> getServices();

    /**
     * 根据数据模型获取业务操作对象
     *
     * @param <T>   数据类型
     * @param model 数据模型
     * @return 业务操作对象
     */
    public <T> Service<T> getService(Class<T> model);

}
