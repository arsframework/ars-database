package ars.database.service;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import ars.util.Beans;
import ars.database.model.Model;
import ars.database.model.TreeModel;
import ars.database.repository.Query;
import ars.database.repository.Repository;
import ars.database.repository.Repositories;
import ars.database.service.AbstractService;
import ars.database.service.event.InitEvent;
import ars.database.service.event.SaveEvent;
import ars.database.service.event.QueryEvent;
import ars.database.service.event.UpdateEvent;
import ars.database.service.event.DeleteEvent;
import ars.database.service.event.ServiceEvent;
import ars.database.service.event.ServiceListener;
import ars.invoke.request.Requester;
import ars.invoke.request.ParameterInvalidException;

/**
 * 业务操作接口抽象实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public abstract class AbstractService<T> implements Service<T> {
	private Class<T> model;
	private Repository<T> repository;
	private List<ServiceListener<?>> initListeners = new LinkedList<ServiceListener<?>>();
	private List<ServiceListener<?>> saveListeners = new LinkedList<ServiceListener<?>>();
	private List<ServiceListener<?>> queryListeners = new LinkedList<ServiceListener<?>>();
	private List<ServiceListener<?>> updateListeners = new LinkedList<ServiceListener<?>>();
	private List<ServiceListener<?>> deleteListeners = new LinkedList<ServiceListener<?>>();

	@SuppressWarnings("unchecked")
	public AbstractService() {
		this.model = (Class<T>) Beans.getClassGenericType(this.getClass());
		if (this.model == null) {
			throw new RuntimeException("Generic type not found:" + this.getClass().getName());
		}
	}

	/**
	 * 实体初始化事件监听
	 * 
	 * @param requester
	 *            请求对象
	 * @param entity
	 *            对象实体
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void onInitEvent(Requester requester, T entity) {
		if (!this.initListeners.isEmpty()) {
			InitEvent event = new InitEvent(requester, this, entity);
			for (ServiceListener listener : this.initListeners) {
				listener.onServiceEvent(event);
			}
		}
	}

	/**
	 * 实体保存事件监听
	 * 
	 * @param requester
	 *            请求对象
	 * @param id
	 *            对象实体主键
	 * @param entity
	 *            对象实体
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void onSaveEvent(Requester requester, Serializable id, T entity) {
		if (!this.saveListeners.isEmpty()) {
			SaveEvent event = new SaveEvent(requester, this, id, entity);
			for (ServiceListener listener : this.saveListeners) {
				listener.onServiceEvent(event);
			}
		}
	}

	/**
	 * 实体删除事件监听
	 * 
	 * @param requester
	 *            请求对象
	 * @param entity
	 *            对象实体
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void onDeleteEvent(Requester requester, T entity) {
		if (!this.deleteListeners.isEmpty()) {
			DeleteEvent event = new DeleteEvent(requester, this, entity);
			for (ServiceListener listener : this.deleteListeners) {
				listener.onServiceEvent(event);
			}
		}
	}

	/**
	 * 实体更新事件监听
	 * 
	 * @param requester
	 *            请求对象
	 * @param entity
	 *            对象实体
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void onUpdateEvent(Requester requester, Object entity) {
		if (!this.updateListeners.isEmpty()) {
			UpdateEvent event = new UpdateEvent(requester, this, entity);
			for (ServiceListener listener : this.updateListeners) {
				listener.onServiceEvent(event);
			}
		}
	}

	/**
	 * 实体查询事件监听
	 * 
	 * @param requester
	 *            请求对象
	 * @param query
	 *            数据查询对象
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void onQueryEvent(Requester requester, Query<T> query) {
		if (!this.queryListeners.isEmpty()) {
			QueryEvent event = new QueryEvent(requester, this, query);
			for (ServiceListener listener : this.queryListeners) {
				listener.onServiceEvent(event);
			}
		}
	}

	@Override
	public Class<T> getModel() {
		return this.model;
	}

	@Override
	public Repository<T> getRepository() {
		if (this.repository == null) {
			this.repository = this.getRepository(this.getModel());
		}
		return this.repository;
	}

	@Override
	public <M> Repository<M> getRepository(Class<M> model) {
		return Repositories.getRepository(model);
	}

	@Override
	public <E extends ServiceEvent> void setListeners(Class<E> type, ServiceListener<E>... listeners) {
		this.initListeners.clear();
		this.saveListeners.clear();
		this.queryListeners.clear();
		this.updateListeners.clear();
		this.deleteListeners.clear();
		for (ServiceListener<?> listener : listeners) {
			Class<?> eventType = null;
			for (Method method : listener.getClass().getMethods()) {
				if (method.getName().equals("onServiceEvent")
						&& (eventType == null || eventType == ServiceEvent.class)) {
					eventType = method.getParameterTypes()[0];
				}
			}
			if (eventType == InitEvent.class) {
				this.initListeners.add(listener);
			} else if (eventType == SaveEvent.class) {
				this.saveListeners.add(listener);
			} else if (eventType == QueryEvent.class) {
				this.queryListeners.add(listener);
			} else if (eventType == UpdateEvent.class) {
				this.updateListeners.add(listener);
			} else if (eventType == DeleteEvent.class) {
				this.deleteListeners.add(listener);
			} else {
				this.initListeners.add(listener);
				this.saveListeners.add(listener);
				this.queryListeners.add(listener);
				this.updateListeners.add(listener);
				this.deleteListeners.add(listener);
			}
		}
	}

	@Override
	public Query<T> getQuery(Requester requester) {
		Query<T> query = this.getRepository().query();
		this.onQueryEvent(requester, query);
		return query;
	}

	@Override
	public void initObject(Requester requester, T entity, Map<String, Object> parameters) {
		Class<?> model = this.getRepository().getModel();
		String primary = this.getRepository().getPrimary();
		while (model != Object.class) {
			for (Field field : model.getDeclaredFields()) {
				String property = field.getName();
				if (Modifier.isStatic(field.getModifiers()) || property.equals(primary)) {
					continue;
				}
				if (!parameters.containsKey(property) || (TreeModel.class.isAssignableFrom(model)
						&& (property.equals("key") || property.equals("level") || property.equals("leaf")))) {
					continue;
				}
				Class<?> type = field.getType();
				if (TreeModel.class.isAssignableFrom(type)
						&& (property.equals("parent") || property.equals("children"))) {
					type = this.getRepository().getModel();
				}
				try {
					Object value = parameters.get(property);
					if (Collection.class.isAssignableFrom(type) || !Beans.isMetaClass(type)) {
						Object current = Beans.getValue(entity, field);
						if (Collection.class.isAssignableFrom(type)) {
							Class<?> genericType = Beans.getFieldGenericType(field);
							Repository<?> repository = Repositories.getRepository(genericType);
							String foreignKey = repository.getPrimary();
							Class<?> foreignKeyType = Beans.getField(genericType, foreignKey).getType();
							Object[] values = Beans.toArray(Object.class, value);
							Collection<Object> objects = Set.class.isAssignableFrom(type)
									? new HashSet<Object>(values.length)
									: new ArrayList<Object>(values.length);
							if (values.length > 0) {
								outer: for (Object v : values) {
									if (v == null) {
										continue;
									}
									if (!genericType.isAssignableFrom(v.getClass())) {
										v = Beans.toObject(foreignKeyType, v);
									}
									for (Object o : (Collection<?>) current) {
										if (Beans.isEqual(Beans.getValue(o, foreignKey), v)) {
											objects.add(o);
											continue outer;
										}
									}
									objects.add(repository.get(v));
								}
							}
							value = objects;
						} else if (value != null && !type.isAssignableFrom(value.getClass())) {
							Repository<?> repository = Repositories.getRepository(type);
							String foreignKey = repository.getPrimary();
							Class<?> foreignKeyType = Beans.getField(type, foreignKey).getType();
							value = Beans.toObject(foreignKeyType, value);
							value = Beans.isEqual(Beans.getValue(current, foreignKey), value) ? current
									: repository.get(value);
						}
						Method method = Beans.getSetMethod(model, field);
						try {
							method.invoke(entity, value);
						} catch (IllegalAccessException e) {
							throw new RuntimeException(e);
						} catch (InvocationTargetException e) {
							throw new RuntimeException(e);
						}
						continue;
					}
					Beans.setValue(entity, field, value);
				} catch (IllegalArgumentException e) {
					throw new ParameterInvalidException(property, e.getMessage());
				}
			}
			model = model.getSuperclass();
		}
		this.onInitEvent(requester, entity);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Serializable saveObject(Requester requester, T object) {
		if (object instanceof Model) {
			Model entity = (Model) object;
			entity.setCreator(requester.getUser());
			if (object instanceof TreeModel) {
				TreeModel<?> tree = (TreeModel<?>) object;
				List<?> children = new ArrayList<Object>(tree.getChildren());
				tree.getChildren().clear();
				Serializable id = this.getRepository().save(object);
				this.onSaveEvent(requester, id, object);
				tree.setId((Integer) id);
				for (int i = 0; i < children.size(); i++) {
					TreeModel child = (TreeModel) children.get(i);
					child.setParent(tree);
					this.saveObject(requester, (T) child);
				}
				return id;
			}
		}
		Serializable id = this.getRepository().save(object);
		this.onSaveEvent(requester, id, object);
		return id;
	}

	@Override
	public void updateObject(Requester requester, T object) {
		if (object instanceof Model) {
			Model entity = (Model) object;
			entity.setDateUpdate(new Date());
			entity.setUpdater(requester.getUser());
		}
		this.getRepository().update(object);
		this.onUpdateEvent(requester, object);
	}

	@Override
	public void deleteObject(Requester requester, T object) {
		this.getRepository().delete(object);
		this.onDeleteEvent(requester, object);
	}

}
