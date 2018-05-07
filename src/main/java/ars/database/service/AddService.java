package ars.database.service;

import java.io.Serializable;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;

/**
 * 数据新增外部调用接口
 *
 * @param <T> 数据模型
 * @author wuyongqiang
 */
public interface AddService<T> extends Service<T> {
    /**
     * 新增对象实体
     *
     * @param requester 请求对象
     * @return 新增对象实体主键
     */
    @Api("add")
    public Serializable add(Requester requester);

}
