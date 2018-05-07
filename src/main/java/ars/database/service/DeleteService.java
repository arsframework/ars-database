package ars.database.service;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;

/**
 * 数据删除外部调用接口
 *
 * @param <T> 数据模型
 * @author wuyongqiang
 */
public interface DeleteService<T> extends Service<T> {
    /**
     * 删除对象
     *
     * @param requester 请求对象
     */
    @Api("delete")
    public void delete(Requester requester);

}
