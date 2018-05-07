package ars.database.service;

import java.util.List;

import ars.invoke.local.Api;
import ars.invoke.request.Requester;

/**
 * 树形数据业务操作接口
 *
 * @param <T> 数据模型
 * @author wuyongqiang
 */
public interface TreeService<T> extends Service<T> {
    /**
     * 获取树对象列表
     *
     * @param requester 请求对象
     * @return 树对象实例列表
     */
    @Api("trees")
    public List<T> trees(Requester requester);

}
