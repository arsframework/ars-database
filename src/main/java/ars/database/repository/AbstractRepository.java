package ars.database.repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.io.Serializable;

import ars.util.Beans;
import ars.database.model.Model;
import ars.database.model.TreeModel;
import ars.database.repository.Repository;
import ars.database.repository.Repositories;

/**
 * 数据持久操作抽象实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public abstract class AbstractRepository<T> implements Repository<T> {
	private Class<T> model;

	@SuppressWarnings("unchecked")
	public AbstractRepository() {
		Class<?>[] genericTypes = Beans.getGenericTypes(this.getClass());
		if (genericTypes.length == 0) {
			throw new RuntimeException("Generic type not found:" + this.getClass().getName());
		}
		this.model = (Class<T>) genericTypes[0];
	}

	/**
	 * 修改对象实体
	 * 
	 * @param object
	 *            对象实体
	 */
	protected abstract void modify(T object);

	/**
	 * 新增对象实体
	 * 
	 * @param object
	 *            对象实体
	 * @return 主键
	 */
	protected abstract Serializable insert(T object);

	/**
	 * 删除对象实体
	 * 
	 * @param object
	 *            对象实体
	 */
	protected abstract void remove(T object);

	@Override
	public Class<T> getModel() {
		return this.model;
	}

	@Override
	public String getPrimary() {
		return Repositories.DEFAULT_PRIMARY_NAME;
	}

	@Override
	public T get(Object id) {
		return id == null ? null : this.query().eq(this.getPrimary(), id).single();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Serializable save(T object) {
		if (object instanceof Model) {
			Model entity = (Model) object;
			entity.setDateJoined(new Date());
			if (entity instanceof TreeModel) {
				TreeModel<?> tree = (TreeModel<?>) object;
				TreeModel<?> parent = tree.getParent();
				if (parent != null) {
					tree.setLevel(parent.getLevel() + 1);
					if (parent.getLeaf() == Boolean.TRUE) {
						parent.setLeaf(false);
						this.modify((T) parent);
					}
				}
				tree.setKey(UUID.randomUUID().toString());
			}
		}
		Serializable id = this.insert(object);
		if (object instanceof Model) {
			boolean changed = false;
			Model entity = (Model) object;
			if (entity.getOrder() == null) {
				entity.setOrder(((Number) id).doubleValue());
				changed = true;
			}
			if (entity instanceof TreeModel) {
				TreeModel<?> tree = (TreeModel<?>) object;
				tree.setKey(Repositories.buildTreeKey(tree, ((Number) id).intValue()));
				changed = true;
			}
			if (changed) {
				this.modify(object);
			}
		}
		return id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(T object) {
		if (object instanceof Model) {
			Model entity = (Model) object;
			entity.setDateUpdate(new Date());
			if (entity instanceof TreeModel) {
				TreeModel<?> tree = (TreeModel<?>) object;
				String key = tree.getKey();
				int level = tree.getLevel();
				TreeModel<?> parent = tree.getParent();
				String pkey = Repositories.getParentKey(key);
				if (!Beans.isEqual(pkey, parent == null ? null : parent.getKey())) {
					if (parent == null) {
						tree.setLevel(1);
					} else {
						tree.setLevel(parent.getLevel() + 1);
						if (parent.getLeaf() == Boolean.TRUE) {
							parent.setLeaf(false);
							this.modify((T) parent);
						}
					}
					if (pkey != null) {
						TreeModel<?> sparent = (TreeModel<?>) this.query().eq("key", pkey).single();
						if (sparent != null) {
							Boolean leaf = this.query().ne("key", pkey).eq("level", sparent.getLevel() + 1)
									.start("key", pkey).count() == 1;
							if (sparent.getLeaf() != leaf) {
								sparent.setLeaf(leaf);
								this.modify((T) sparent);
							}
						}
					}
					Repositories.refreshTreeKey(tree);
					List<T> relates = this.query().ne("key", key).start("key", key).list();
					for (int i = 0; i < relates.size(); i++) {
						TreeModel<?> relate = (TreeModel<?>) relates.get(i);
						StringBuilder keyBuilder = new StringBuilder(tree.getKey())
								.append(relate.getKey().substring(Repositories.getParentKey(relate.getKey()).length()));
						relate.setKey(keyBuilder.toString());
						relate.setLevel(relate.getLevel() - level + tree.getLevel());
						this.modify((T) relate);
					}
				}
			}
		}
		this.modify(object);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void delete(T object) {
		if (object instanceof TreeModel) {
			TreeModel<?> parent = ((TreeModel<?>) object).getParent();
			if (parent != null) {
				Boolean leaf = this.query().eq("parent", parent).count() == 1;
				if (leaf != parent.getLeaf()) {
					parent.setLeaf(leaf);
					this.modify((T) parent);
				}
			}
		}
		this.remove(object);
	}

}
