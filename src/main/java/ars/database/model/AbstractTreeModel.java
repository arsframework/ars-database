package ars.database.model;

import java.util.List;
import java.util.ArrayList;

import ars.util.Trees;

/**
 * 树形数据模型抽象实现
 *
 * @param <T> 树对象类型
 * @author wuyongqiang
 */
public abstract class AbstractTreeModel<T extends AbstractTreeModel<T>> extends
    AbstractModel implements TreeModel<T> {
    private static final long serialVersionUID = 1L;

    private String key; // 树标识
    private Integer level = 1; // 所在树中的层级（从1开始）
    private Boolean leaf = true; // 是否是叶节点
    private T parent; // 父节点
    private List<T> children = new ArrayList<T>(0); // 子节点

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public Integer getLevel() {
        return level;
    }

    @Override
    public void setLevel(Integer level) {
        this.level = level;
    }

    @Override
    public Boolean getLeaf() {
        return leaf;
    }

    @Override
    public void setLeaf(Boolean leaf) {
        this.leaf = leaf;
    }

    @Override
    public T getParent() {
        return parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> getParents() {
        return (List<T>) Trees.getParents(this);
    }

    @Override
    public void setParent(T parent) {
        this.parent = parent;
    }

    @Override
    public List<T> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<T> children) {
        this.children = children;
    }

}
