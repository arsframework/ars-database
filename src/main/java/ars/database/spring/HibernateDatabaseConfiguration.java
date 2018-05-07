package ars.database.spring;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Collections;
import java.lang.reflect.Modifier;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.event.spi.EvictEventListener;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.springframework.beans.BeansException;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import ars.util.Beans;
import ars.util.Trees;
import ars.util.Strings;
import ars.util.ObjectAdapter;
import ars.util.Conditions.Or;
import ars.util.Conditions.Match;
import ars.database.model.TreeModel;
import ars.database.hibernate.Hibernates;
import ars.database.hibernate.HibernateSimpleRepository;
import ars.database.repository.Repository;
import ars.database.repository.Repositories;
import ars.database.repository.DataConstraintException;
import ars.database.service.event.DeleteEvent;
import ars.database.service.event.InitEvent;
import ars.database.service.event.ServiceEvent;
import ars.database.service.event.ServiceListener;
import ars.database.spring.DatabaseConfiguration;
import ars.invoke.request.ParameterInvalidException;

/**
 * 基于Hibernate数据操作配置
 *
 * @author wuyongqiang
 */
public class HibernateDatabaseConfiguration extends DatabaseConfiguration implements ObjectAdapter,
    ServiceListener<ServiceEvent> {
    private static Map<Class<?>, List<Property>> VALIDATE_MODEL_PROPERTY_MAPPING = new HashMap<Class<?>, List<Property>>(); // 数据模型/属性映射
    private static Map<Class<?>, Map<Class<?>, List<Property>>> VALIDATE_MODEL_RELATE_MAPPING = new HashMap<Class<?>, Map<Class<?>, List<Property>>>(); // 数据模型关联属性映射

    private String validate; // 数据验证模型

    public String getValidate() {
        return validate;
    }

    public void setValidate(String validate) {
        this.validate = validate;
    }

    /**
     * 对象实体属性值有效性验证
     *
     * @param event 初始化事件对象
     */
    protected void validation(InitEvent event) {
        Object entity = event.getEntity();
        Class<?> model = event.getService().getModel();
        while (true) {
            Class<?> superclass = model.getSuperclass();
            if (superclass != Object.class && !Modifier.isAbstract(superclass.getModifiers())
                && VALIDATE_MODEL_PROPERTY_MAPPING.containsKey(superclass)) {
                model = superclass;
                continue;
            }
            break;
        }
        Repository<?> repository = Repositories.getRepository(model);
        List<Property> properties = VALIDATE_MODEL_PROPERTY_MAPPING.get(model);
        if (properties == null) {
            throw new RuntimeException("Unknown model:" + model.getName());
        }

        List<Match> uniques = new LinkedList<Match>();
        for (Property property : properties) {
            String name = property.getName();
            Object value = Beans.getValue(entity, name);
            Iterator<?> columnIterator = property.getColumnIterator();
            if (columnIterator.hasNext()) {
                Column column = (Column) columnIterator.next();
                if (!column.isNullable() && Beans.isEmpty(value)) {
                    throw new ParameterInvalidException(name, "required");
                } else if (column.isUnique() && !Beans.isEmpty(value)) {
                    uniques.add(new Match(name, value));
                }
            }
            if (entity instanceof TreeModel && name.equals("parent") && Trees.isLoop((TreeModel<?>) entity)) {
                throw new ParameterInvalidException(name, "dead cycle");
            }
        }
        if (!uniques.isEmpty()) {
            Object id = Beans.getValue(entity, repository.getPrimary());
            Object exist = repository.query().ne(repository.getPrimary(), id)
                .condition(new Or(uniques.toArray(new Match[0]))).single();
            if (exist != null) {
                for (Match match : uniques) {
                    if (Beans.isEqual(Beans.getValue(exist, match.getKey()), match.getValue())) {
                        throw new ParameterInvalidException(match.getKey(), "exist");
                    }
                }
            }
        }
    }

    /**
     * 对象实体删除验证
     *
     * @param event 删除事件对象
     */
    protected void validation(DeleteEvent event) {
        Object entity = event.getEntity();
        Class<?> model = event.getService().getModel();
        Map<Class<?>, List<Property>> relates = VALIDATE_MODEL_RELATE_MAPPING.get(model);
        if (relates != null && !relates.isEmpty()) {
            for (Entry<Class<?>, List<Property>> entry : relates.entrySet()) {
                Class<?> foreignKeyClass = entry.getKey();
                Repository<?> repository = Repositories.getRepository(foreignKeyClass);
                for (Property property : entry.getValue()) {
                    if (TreeModel.class.isAssignableFrom(model) && model == foreignKeyClass
                        && property.getName().equals("parent")) {
                        continue;
                    }
                    Object relate = repository.query().eq(property.getName(), entity).paging(1, 1).single();
                    if (relate != null) {
                        String message = new StringBuilder(event.getSource().format(foreignKeyClass.getName()))
                            .append('[').append(relate.toString()).append(']').toString();
                        throw new DataConstraintException(message);
                    }
                }
            }
        }
    }

    /**
     * 绑定持久化会话工厂实例
     *
     * @param applicationContext Spring上下文对象
     */
    @SuppressWarnings("rawtypes")
    protected void bindSessionFactory(ApplicationContext applicationContext) {
        Collection<SessionFactory> sessionFactories = applicationContext.getBeansOfType(SessionFactory.class).values();
        Map<Class<?>, SessionFactory> modelSessionFactoryMappings = new HashMap<Class<?>, SessionFactory>();
        for (SessionFactory sessionFactory : sessionFactories) {
            Collection<ClassMetadata> classMetadatas = sessionFactory.getAllClassMetadata().values();
            for (ClassMetadata classMetadata : classMetadatas) {
                modelSessionFactoryMappings.put(classMetadata.getMappedClass(), sessionFactory);
            }
        }

        // 绑定数据持久化对象会话工厂实例
        Map<String, Repository> repositories = applicationContext.getBeansOfType(Repository.class);
        for (Entry<String, Repository> entry : repositories.entrySet()) {
            Repository<?> repository = entry.getValue();
            if (AopUtils.isAopProxy(repository)) {
                try {
                    repository = (Repository<?>) ((Advised) repository).getTargetSource().getTarget();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (!(repository instanceof HibernateSimpleRepository)) {
                continue;
            }
            Class<?> model = repository.getModel();
            SessionFactory sessionFactory = modelSessionFactoryMappings.get(model);
            if (sessionFactory == null) {
                throw new IllegalStateException("No matching session factory:" + model);
            }
            ((HibernateSimpleRepository<?>) repository).setSessionFactory(sessionFactory);
        }
    }

    /**
     * 绑定数据验证属性
     *
     * @param applicationContext Spring上下文对象
     */
    protected void bindValidateProperty(ApplicationContext applicationContext) {
        Collection<LocalSessionFactoryBean> sessionFactoryBeans = applicationContext.getBeansOfType(
            LocalSessionFactoryBean.class).values();
        for (LocalSessionFactoryBean bean : sessionFactoryBeans) {
            SessionFactory sessionFactory = bean.getObject();
            Configuration configuration = bean.getConfiguration();
            Iterator<PersistentClass> persistentIterator = configuration.getClassMappings();
            while (persistentIterator.hasNext()) {
                PersistentClass persistent = persistentIterator.next();
                Class<?> mappedClass = persistent.getMappedClass();
                if (this.validate != null && !Strings.matches(mappedClass.getName(), this.validate)) {
                    continue;
                }
                List<Property> mappedProperties = VALIDATE_MODEL_PROPERTY_MAPPING.get(mappedClass);
                if (mappedProperties == null) {
                    mappedProperties = new LinkedList<Property>();
                    VALIDATE_MODEL_PROPERTY_MAPPING.put(mappedClass, mappedProperties);
                }
                Iterator<?> propertyIterator = persistent.getPropertyIterator();
                while (propertyIterator.hasNext()) {
                    Property property = (Property) propertyIterator.next();
                    String name = property.getName();
                    if (TreeModel.class.isAssignableFrom(mappedClass)
                        && (name.equals("key") || name.equals("level") || name.equals("leaf")
                        || name.equals("parent") || name.equals("children"))) {
                        continue;
                    }
                    mappedProperties.add(property);
                    Type type = property.getType();
                    Class<?> foreignKeyClass = Hibernates.getPropertyTypeClass(sessionFactory, type);
                    if (this.validate != null && !Strings.matches(foreignKeyClass.getName(), this.validate)) {
                        continue;
                    }
                    if (type.isEntityType() || type.isCollectionType()) {
                        Map<Class<?>, List<Property>> relates = VALIDATE_MODEL_RELATE_MAPPING.get(foreignKeyClass);
                        if (relates == null) {
                            relates = new HashMap<Class<?>, List<Property>>();
                            VALIDATE_MODEL_RELATE_MAPPING.put(foreignKeyClass, relates);
                        }
                        List<Property> properties = relates.get(mappedClass);
                        if (properties == null) {
                            properties = new LinkedList<Property>();
                            relates.put(mappedClass, properties);
                        }
                        properties.add(property);
                    }
                }
            }

            persistentIterator = configuration.getClassMappings();
            while (persistentIterator.hasNext()) {
                PersistentClass persistent = persistentIterator.next();
                Class<?> mappedClass = persistent.getMappedClass();
                Map<Class<?>, List<Property>> relates = VALIDATE_MODEL_RELATE_MAPPING.get(mappedClass);
                if (relates == null) {
                    continue;
                }
                Class<?> superclass = mappedClass.getSuperclass();
                while (superclass != Object.class && !Modifier.isAbstract(superclass.getModifiers())) {
                    Map<Class<?>, List<Property>> superrelates = VALIDATE_MODEL_RELATE_MAPPING.get(superclass);
                    if (superrelates == null) {
                        break;
                    }
                    for (Entry<Class<?>, List<Property>> entry : relates.entrySet()) {
                        Class<?> key = entry.getKey();
                        List<Property> properties = superrelates.get(key);
                        if (properties == null) {
                            properties = new LinkedList<Property>();
                            superrelates.put(key, properties);
                        }
                        for (Property property : entry.getValue()) {
                            if (!properties.contains(property)) {
                                properties.add(property);
                            }
                        }
                    }
                    for (Entry<Class<?>, List<Property>> entry : superrelates.entrySet()) {
                        Class<?> key = entry.getKey();
                        List<Property> properties = relates.get(key);
                        if (properties == null) {
                            properties = new LinkedList<Property>();
                            relates.put(key, properties);
                        }
                        for (Property property : entry.getValue()) {
                            if (!properties.contains(property)) {
                                properties.add(property);
                            }
                        }
                    }
                    superclass = superclass.getSuperclass();
                }
            }
        }
    }

    /**
     * 注册事件监听器
     *
     * @param applicationContext Spring上下文对象
     */
    protected void registerEventListener(ApplicationContext applicationContext) {
        Collection<SessionFactory> sessionFactories = applicationContext.getBeansOfType(SessionFactory.class).values();
        for (SessionFactory sessionFactory : sessionFactories) {
            EventListenerRegistry registry = ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService(
                EventListenerRegistry.class);

            Map<String, LoadEventListener> loadEventListeners = applicationContext
                .getBeansOfType(LoadEventListener.class);
            if (!loadEventListeners.isEmpty()) {
                registry.appendListeners(EventType.LOAD, loadEventListeners.values().toArray(new LoadEventListener[0]));
            }

            Map<String, LockEventListener> lockEventListeners = applicationContext
                .getBeansOfType(LockEventListener.class);
            if (!lockEventListeners.isEmpty()) {
                registry.appendListeners(EventType.LOCK, lockEventListeners.values().toArray(new LockEventListener[0]));
            }

            Map<String, MergeEventListener> mergeEventListeners = applicationContext
                .getBeansOfType(MergeEventListener.class);
            if (!mergeEventListeners.isEmpty()) {
                registry.appendListeners(EventType.MERGE,
                    mergeEventListeners.values().toArray(new MergeEventListener[0]));
            }

            Map<String, EvictEventListener> evictEventListeners = applicationContext
                .getBeansOfType(EvictEventListener.class);
            if (!evictEventListeners.isEmpty()) {
                registry.appendListeners(EventType.EVICT,
                    evictEventListeners.values().toArray(new EvictEventListener[0]));
            }

            Map<String, FlushEventListener> flushEventListeners = applicationContext
                .getBeansOfType(FlushEventListener.class);
            if (!flushEventListeners.isEmpty()) {
                registry.appendListeners(EventType.FLUSH,
                    flushEventListeners.values().toArray(new FlushEventListener[0]));
            }

            Map<String, DeleteEventListener> deleteEventListeners = applicationContext
                .getBeansOfType(DeleteEventListener.class);
            if (!deleteEventListeners.isEmpty()) {
                registry.appendListeners(EventType.DELETE,
                    deleteEventListeners.values().toArray(new DeleteEventListener[0]));
            }

            Map<String, PersistEventListener> persistEventListeners = applicationContext
                .getBeansOfType(PersistEventListener.class);
            if (!persistEventListeners.isEmpty()) {
                registry.appendListeners(EventType.PERSIST,
                    persistEventListeners.values().toArray(new PersistEventListener[0]));
            }

            Map<String, RefreshEventListener> refreshEventListeners = applicationContext
                .getBeansOfType(RefreshEventListener.class);
            if (!refreshEventListeners.isEmpty()) {
                registry.appendListeners(EventType.REFRESH,
                    refreshEventListeners.values().toArray(new RefreshEventListener[0]));
            }

            Map<String, PreLoadEventListener> preLoadEventListeners = applicationContext
                .getBeansOfType(PreLoadEventListener.class);
            if (!preLoadEventListeners.isEmpty()) {
                registry.appendListeners(EventType.PRE_LOAD,
                    preLoadEventListeners.values().toArray(new PreLoadEventListener[0]));
            }

            Map<String, PostLoadEventListener> postLoadEventListeners = applicationContext
                .getBeansOfType(PostLoadEventListener.class);
            if (!postLoadEventListeners.isEmpty()) {
                registry.appendListeners(EventType.POST_LOAD,
                    postLoadEventListeners.values().toArray(new PostLoadEventListener[0]));
            }

            Map<String, AutoFlushEventListener> autoFlushEventListeners = applicationContext
                .getBeansOfType(AutoFlushEventListener.class);
            if (!autoFlushEventListeners.isEmpty()) {
                registry.appendListeners(EventType.AUTO_FLUSH,
                    autoFlushEventListeners.values().toArray(new AutoFlushEventListener[0]));
            }

            Map<String, PreUpdateEventListener> preUpdateEventListeners = applicationContext
                .getBeansOfType(PreUpdateEventListener.class);
            if (!preUpdateEventListeners.isEmpty()) {
                registry.appendListeners(EventType.PRE_UPDATE,
                    preUpdateEventListeners.values().toArray(new PreUpdateEventListener[0]));
            }

            Map<String, PreDeleteEventListener> preDeleteEventListeners = applicationContext
                .getBeansOfType(PreDeleteEventListener.class);
            if (!preDeleteEventListeners.isEmpty()) {
                registry.appendListeners(EventType.PRE_DELETE,
                    preDeleteEventListeners.values().toArray(new PreDeleteEventListener[0]));
            }

            Map<String, PreInsertEventListener> preInsertEventListeners = applicationContext
                .getBeansOfType(PreInsertEventListener.class);
            if (!preInsertEventListeners.isEmpty()) {
                registry.appendListeners(EventType.PRE_INSERT,
                    preInsertEventListeners.values().toArray(new PreInsertEventListener[0]));
            }

            Map<String, ReplicateEventListener> replicateEventListeners = applicationContext
                .getBeansOfType(ReplicateEventListener.class);
            if (!replicateEventListeners.isEmpty()) {
                registry.appendListeners(EventType.REPLICATE,
                    replicateEventListeners.values().toArray(new ReplicateEventListener[0]));
            }

            Map<String, DirtyCheckEventListener> dirtyCheckEventListeners = applicationContext
                .getBeansOfType(DirtyCheckEventListener.class);
            if (!dirtyCheckEventListeners.isEmpty()) {
                registry.appendListeners(EventType.DIRTY_CHECK,
                    dirtyCheckEventListeners.values().toArray(new DirtyCheckEventListener[0]));
            }

            Map<String, PostUpdateEventListener> postUpdateEventListeners = applicationContext
                .getBeansOfType(PostUpdateEventListener.class);
            if (!postUpdateEventListeners.isEmpty()) {
                registry.appendListeners(EventType.POST_UPDATE,
                    postUpdateEventListeners.values().toArray(new PostUpdateEventListener[0]));
            }

            Map<String, PostDeleteEventListener> postDeleteEventListeners = applicationContext
                .getBeansOfType(PostDeleteEventListener.class);
            if (!postDeleteEventListeners.isEmpty()) {
                registry.appendListeners(EventType.POST_DELETE,
                    postDeleteEventListeners.values().toArray(new PostDeleteEventListener[0]));
            }

            Map<String, PostInsertEventListener> postInsertEventListeners = applicationContext
                .getBeansOfType(PostInsertEventListener.class);
            if (!postInsertEventListeners.isEmpty()) {
                registry.appendListeners(EventType.POST_INSERT,
                    postInsertEventListeners.values().toArray(new PostInsertEventListener[0]));
            }

            Map<String, FlushEntityEventListener> flushEntityEventListeners = applicationContext
                .getBeansOfType(FlushEntityEventListener.class);
            if (!flushEntityEventListeners.isEmpty()) {
                registry.appendListeners(EventType.FLUSH_ENTITY,
                    flushEntityEventListeners.values().toArray(new FlushEntityEventListener[0]));
            }

            Map<String, SaveOrUpdateEventListener> saveOrUpdateEventListeners = applicationContext
                .getBeansOfType(SaveOrUpdateEventListener.class);
            if (!saveOrUpdateEventListeners.isEmpty()) {
                registry.appendListeners(EventType.SAVE_UPDATE,
                    saveOrUpdateEventListeners.values().toArray(new SaveOrUpdateEventListener[0]));
            }

            Map<String, InitializeCollectionEventListener> initializeCollectionEventListeners = applicationContext
                .getBeansOfType(InitializeCollectionEventListener.class);
            if (!initializeCollectionEventListeners.isEmpty()) {
                registry.appendListeners(EventType.INIT_COLLECTION, initializeCollectionEventListeners.values()
                    .toArray(new InitializeCollectionEventListener[0]));
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        this.bindSessionFactory(applicationContext);
        this.bindValidateProperty(applicationContext);
        this.registerEventListener(applicationContext);
    }

    @Override
    public Object adaption(Object object) {
        if (Hibernate.isInitialized(object)) {
            return object;
        } else if (object instanceof Map) {
            return Collections.emptyMap();
        } else if (object instanceof Set) {
            return Collections.emptySet();
        } else if (object instanceof List) {
            return Collections.emptyList();
        }
        return null;
    }

    @Override
    public void onServiceEvent(ServiceEvent event) {
        Class<?> model = event.getService().getModel();
        if (this.validate == null || Strings.matches(model.getName(), this.validate)) {
            if (event instanceof InitEvent) {
                this.validation((InitEvent) event);
            } else if (event instanceof DeleteEvent) {
                this.validation((DeleteEvent) event);
            }
        }
    }

}
