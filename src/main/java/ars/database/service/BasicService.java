package ars.database.service;

/**
 * 基础的业务操作接口
 *
 * @param <T> 数据模型
 * @author wuyongqiang
 */
public interface BasicService<T> extends AddService<T>, DeleteService<T>,
    UpdateService<T>, SearchService<T> {

}