package ars.database.spring;

import java.util.List;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;

import org.springframework.context.event.ContextRefreshedEvent;

import ars.util.Beans;
import ars.util.Strings;
import ars.database.model.TreeModel;
import ars.database.repository.Repository;
import ars.database.repository.Repositories;
import ars.database.repository.RepositoryFactory;
import ars.spring.context.ApplicationInitializer;

/**
 * 对象实体同步更新
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            对象类型
 */
public class EntitySynchronUpgrader<T> extends ApplicationInitializer {
	private Repository<T> repository;
	private Comparator<T> comparator;
	private List<T> entities = Collections.emptyList(); // 对象实体列表
	private String[] comparators = Strings.EMPTY_ARRAY; // 比较属性
	private List<String> includes = new LinkedList<String>();

	public List<T> getEntities() {
		return entities;
	}

	public void setEntities(List<T> entities) {
		this.entities = entities;
	}

	public String[] getComparators() {
		return comparators;
	}

	public void setComparators(String... comparators) {
		this.comparators = comparators;
	}

	/**
	 * 对象实体同步（只是新增或修改，不包含删除操作）
	 * 
	 * @param sources
	 *            源对象实体集合
	 * @param targets
	 *            目标对象实体集合
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void synchron(Collection<T> sources, Collection<T> targets) {
		outer: for (T target : targets) {
			for (T source : sources) {
				if (this.comparator.compare(source, target) == 0) {
					if (source instanceof TreeModel) {
						List<?> children = ((TreeModel<?>) target).getChildren();
						if (!children.isEmpty()) {
							for (Object child : children) {
								((TreeModel) child).setParent((TreeModel) source);
							}
							this.synchron((Collection<T>) ((TreeModel<?>) source).getChildren(),
									(Collection<T>) children);
						}
					}
					if (!includes.isEmpty()) {
						boolean different = false;
						for (String property : this.includes) {
							Object sourceValue = Beans.getValue(source, property);
							Object targetValue = Beans.getValue(target, property);
							if (!Beans.isEqual(sourceValue, targetValue)) {
								Beans.setValue(source, property, targetValue);
								different = true;
							}
						}
						if (different) {
							this.repository.update(source);
						}
					}
					continue outer;
				}
			}
			if (target instanceof TreeModel) {
				Repositories.saveTree(this.repository, target);
			} else {
				this.repository.save(target);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void execute(ContextRefreshedEvent event) {
		if (this.entities.isEmpty()) {
			throw new RuntimeException("Target entities has not been initialize");
		}
		Class<T> model = (Class<T>) this.entities.get(0).getClass();
		this.repository = event.getApplicationContext().getBean(RepositoryFactory.class).getRepository(model);
		String primary = this.repository.getPrimary();
		List<String> excludes = new LinkedList<String>();
		final List<String> conditions = new LinkedList<String>();
		for (String property : this.comparators) {
			if (property.equals(primary)) {
				continue;
			} else if (property.charAt(0) == '+') {
				this.includes.add(property.substring(1));
			} else if (property.charAt(0) == '-') {
				excludes.add(property.substring(1));
			} else {
				conditions.add(property);
			}
		}
		if (this.includes.isEmpty() && !excludes.isEmpty()) {
			for (String property : Beans.getProperties(model)) {
				if (!property.equals(primary) && !excludes.contains(property)) {
					this.includes.add(property);
				}
			}
		}

		this.comparator = new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				if (conditions.isEmpty()) {
					return o1.equals(o2) ? 0 : -1;
				}
				for (String property : conditions) {
					if (!Beans.isEqual(Beans.getValue(o1, property), Beans.getValue(o2, property))) {
						return -1;
					}
				}
				return 0;
			}

		};

		List<T> sources = this.repository.query().list();
		if (TreeModel.class.isAssignableFrom(model)) {
			sources = (List<T>) Repositories.mergeTrees((List<TreeModel>) sources);
		}
		this.synchron(sources, this.entities);
	}

}
