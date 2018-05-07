package ars.database.spring;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;

import ars.util.Beans;
import ars.database.repository.Query;
import ars.database.repository.Repositories;

/**
 * 对象实体工厂类
 *
 * @author wuyongqiang
 */
public class EntityFactoryBean implements FactoryBean<Object> {
    private Object entity; // 模型实体（设置表示默认值）
    private boolean loaded; // 实体是否已加载
    private Class<?> model; // 对象模型
    private boolean multiple; // 是否获取多个
    private Map<String, Object> attributes; // 实体属性

    public Class<?> getModel() {
        return model;
    }

    public void setModel(Class<?> model) {
        this.model = model;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Object getObject() throws Exception {
        if (!this.loaded) {
            if (this.model == null) {
                throw new IllegalStateException("Model not initialized");
            }
            if (this.attributes == null || this.attributes.isEmpty()) {
                throw new IllegalStateException("Attributes not initialized");
            }
            this.loaded = true;
            Query<?> query = Repositories.query(this.model).custom(this.attributes);
            if (this.multiple) {
                List<?> objects = query.list();
                if (Beans.isEmpty(this.entity) || !objects.isEmpty()) {
                    this.entity = objects;
                }
            } else {
                Object object = query.single();
                if (Beans.isEmpty(this.entity) || object != null) {
                    this.entity = object;
                }
            }
        }
        return this.entity;
    }

    @Override
    public Class<?> getObjectType() {
        return this.multiple ? List.class : this.model;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
