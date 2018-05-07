package ars.database.service;

import java.util.List;
import java.io.Serializable;

import ars.util.Beans;
import ars.invoke.request.Requester;
import ars.database.model.TreeModel;
import ars.database.repository.Repository;
import ars.database.repository.Repositories;

/**
 * 通用业务操作接口抽象实现
 *
 * @param <T> 数据模型
 * @author wuyongqiang
 */
public abstract class StandardGeneralService<T> extends AbstractService<T> {
    /**
     * 新增对象实体
     *
     * @param requester 请求对象
     * @return 新增对象实体主键
     */
    public Serializable add(Requester requester) {
        T entity = Beans.getInstance(this.getModel());
        this.initObject(requester, entity);
        return this.saveObject(requester, entity);
    }

    /**
     * 删除对象
     *
     * @param requester 请求对象
     */
    public void delete(Requester requester) {
        List<T> entities = this.getQuery(requester, true).custom(requester.getParameters()).list();
        for (int i = 0; i < entities.size(); i++) {
            this.deleteObject(requester, entities.get(i));
        }
    }

    /**
     * 修改对象实体
     *
     * @param requester   请求对象
     * @param identifiers 对象主键数组
     */
    @SuppressWarnings("unchecked")
    public void update(Requester requester, Object[] identifiers) {
        if (identifiers.length > 0) {
            Repository<T> repository = this.getRepository();
            String primary = repository.getPrimary();
            List<T> entities = this.getQuery(requester).or(primary, identifiers).list();
            for (int i = 0; i < entities.size(); i++) {
                T entity = entities.get(i);
                Boolean active = entity instanceof TreeModel ? ((TreeModel<?>) entity).getActive() : null;
                this.initObject(requester, entity);
                this.updateObject(requester, entity);
                if (active != null && active != ((TreeModel<?>) entity).getActive()) {
                    TreeModel<?> tree = (TreeModel<?>) entity;
                    if (tree.getActive() == Boolean.TRUE) {
                        TreeModel<?> parent = (TreeModel<?>) tree.getParent();
                        while (parent != null) {
                            if (parent.getActive() != Boolean.TRUE) {
                                parent.setActive(true);
                                ((Repository<TreeModel<?>>) repository).update(parent);
                            }
                            parent = (TreeModel<?>) parent.getParent();
                        }
                    } else if (tree.getActive() == Boolean.FALSE) {
                        List<T> children = repository.query().ne(primary, tree.getId()).eq("active", true)
                            .start("key", tree.getKey()).list();
                        for (int j = 0; j < children.size(); j++) {
                            TreeModel<?> child = (TreeModel<?>) children.get(j);
                            if (child.getActive() != Boolean.FALSE) {
                                child.setActive(false);
                                ((Repository<TreeModel<?>>) repository).update(child);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 统计数量
     *
     * @param requester 请求对象
     * @return 数量
     */
    public int count(Requester requester) {
        return this.getQuery(requester).custom(requester.getParameters()).count();
    }

    /**
     * 数据统计
     *
     * @param requester 请求对象
     * @return 统计数据列表
     */
    public List<?> stats(Requester requester) {
        return this.getQuery(requester).custom(requester.getParameters()).stats();
    }

    /**
     * 获取单个对象
     *
     * @param requester 请求对象
     * @return 对象实例
     */
    public T object(Requester requester) {
        return this.getQuery(requester, true).custom(requester.getParameters()).single();
    }

    /**
     * 获取对象列表
     *
     * @param requester 请求对象
     * @return 对象实例列表
     */
    public List<T> objects(Requester requester) {
        return this.getQuery(requester).custom(requester.getParameters()).list();
    }

    /**
     * 获取树对象列表
     *
     * @param requester 请求对象
     * @return 树对象实例列表
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<T> trees(Requester requester) {
        List<T> objects = this.getQuery(requester).custom(requester.getParameters()).list();
        return (List<T>) Repositories.mergeTrees((List<TreeModel>) objects);
    }

}
