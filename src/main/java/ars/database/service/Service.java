package ars.database.service;

import java.io.Serializable;

import ars.invoke.request.Requester;
import ars.database.repository.Query;
import ars.database.repository.Repository;
import ars.database.service.event.ServiceEvent;
import ars.database.service.event.ServiceListener;

/**
 * 业务操作接口
 *
 * @param <T> 数据模型
 * @author wuyongqiang
 */
public interface Service<T> {
    /**
     * 获取数据模型
     *
     * @return 数据模型
     */
    public Class<T> getModel();

    /**
     * 获取数据持久化操作对象
     *
     * @return 数据持久化操作对象
     */
    public Repository<T> getRepository();

    /**
     * 获取数据查询对象
     *
     * @param requester 请求对象
     * @return 数据查询对象
     */
    public Query<T> getQuery(Requester requester);

    /**
     * 获取数据查询对象
     *
     * @param requester 请求对象
     * @param accurate  是否精确查询（排除无效参数）
     * @return 数据查询对象
     */
    public Query<T> getQuery(Requester requester, boolean accurate);

    /**
     * 对象初始化
     *
     * @param requester 请求对象
     * @param entity    对象实体
     */
    public void initObject(Requester requester, T entity);

    /**
     * 将对象持久化
     *
     * @param requester 请求对象
     * @param object    数据对象
     * @return 主键标识
     */
    public Serializable saveObject(Requester requester, T object);

    /**
     * 修改对象
     *
     * @param requester 请求对象
     * @param object    数据对象
     */
    public void updateObject(Requester requester, T object);

    /**
     * 删除数据
     *
     * @param requester 请求对象
     * @param object    数据对象
     */
    public void deleteObject(Requester requester, T object);

    /**
     * 设置对象实体业务操作监听器
     *
     * @param <E>       事件类型
     * @param type      事件类型对象
     * @param listeners 监听器数组
     */
    public <E extends ServiceEvent> void setListeners(Class<E> type, ServiceListener<E>... listeners);

}
