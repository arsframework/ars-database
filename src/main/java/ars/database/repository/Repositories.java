package ars.database.repository;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.io.Serializable;

import ars.util.Beans;
import ars.util.Strings;
import ars.util.Formable;
import ars.util.SimpleTree;
import ars.database.model.TreeModel;
import ars.database.repository.Query;
import ars.database.repository.EmptyQuery;
import ars.database.repository.Repository;
import ars.database.repository.RepositoryFactory;

/**
 * 数据持久化操作工具类
 * 
 * @author yongqiangwu
 * 
 */
public final class Repositories {
	/**
	 * 树对象唯一标识字符串分隔符
	 */
	public static final char TREE_KEY_SEPARATOR = 'x';

	private static RepositoryFactory repositoryFactory;

	private Repositories() {

	}

	public static RepositoryFactory getRepositoryFactory() {
		if (repositoryFactory == null) {
			throw new RuntimeException("Repository factory has not been initialize");
		}
		return repositoryFactory;
	}

	public static void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		if (Repositories.repositoryFactory != null) {
			throw new RuntimeException("Repository factory is already initialized");
		}
		Repositories.repositoryFactory = repositoryFactory;
	}

	/**
	 * 获取系统数据模型列表
	 * 
	 * @return 数据模型列表
	 */
	public static List<Class<?>> getModels() {
		List<Class<?>> models = new ArrayList<Class<?>>(getRepositoryFactory().getRepositories().keySet());
		Collections.sort(models, new Comparator<Class<?>>() {

			@Override
			public int compare(Class<?> o1, Class<?> o2) {
				return o1.getName().compareTo(o2.getName());
			}

		});
		return models;
	}

	/**
	 * 获取数据持久化对象
	 * 
	 * @param <M>
	 *            数据类型
	 * @param model
	 *            数据模型
	 * @return 数据持久化对象
	 */
	public static <M> Repository<M> getRepository(Class<M> model) {
		return getRepositoryFactory().getRepository(model);
	}

	/**
	 * 获取父节点标识
	 * 
	 * @param key
	 *            树标识
	 * @return 树标识
	 */
	public static String getParentKey(String key) {
		int count = 0, index1 = -1, index2 = -1;
		for (int i = key.length() - 1; i > -1; i--) {
			if (key.charAt(i) == TREE_KEY_SEPARATOR) {
				index1 = index2;
				index2 = i;
				if (++count > 2) {
					break;
				}
			}
		}
		return count < 3 ? null : key.substring(0, index1 + 1);
	}

	/**
	 * 根据树标识获取所有父节点标识
	 * 
	 * @param key
	 *            树标识
	 * @return 父节点标识数组
	 */
	public static String[] getParentKeys(String key) {
		if (key == null || key.isEmpty()) {
			return Strings.EMPTY_ARRAY;
		}
		int offset = 0;
		List<String> keys = new LinkedList<String>();
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (i > 0 && c == TREE_KEY_SEPARATOR) {
				keys.add(key.substring(offset, i + 1));
				offset = i;
			}
		}
		return keys.subList(0, keys.size() - 1).toArray(Strings.EMPTY_ARRAY);
	}

	/**
	 * 构建树对象实体标识
	 * 
	 * @param <M>
	 *            数据类型
	 * @param tree
	 *            树对象实体
	 * @return 树标识
	 */
	@SuppressWarnings("rawtypes")
	public static <M extends TreeModel> String buildKey(M tree) {
		if (tree == null) {
			throw new IllegalArgumentException("Illegal tree:" + tree);
		}
		return buildKey(tree, tree.getId());
	}

	/**
	 * 构建树对象实体标识
	 * 
	 * @param <M>
	 *            数据类型
	 * @param tree
	 *            树对象实体
	 * @param sequence
	 *            序号（不能小于0）
	 * @return 树标识
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <M extends TreeModel> String buildKey(M tree, int sequence) {
		if (tree == null) {
			throw new IllegalArgumentException("Illegal tree:" + tree);
		}
		if (sequence < 0) {
			throw new IllegalArgumentException("Illegal sequence:" + sequence);
		}
		M parent = (M) tree.getParent();
		return new StringBuilder().append(parent == null ? TREE_KEY_SEPARATOR : parent.getKey()).append(sequence)
				.append(TREE_KEY_SEPARATOR).toString();
	}

	/**
	 * 刷新树对象实体标识
	 * 
	 * @param <M>
	 *            数据类型
	 * @param tree
	 *            树对象实体
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <M extends TreeModel> void refreshKey(M tree) {
		if (tree == null) {
			throw new IllegalArgumentException("Illegal tree:" + tree);
		}
		String key = tree.getKey();
		if (key == null) {
			tree.setKey(buildKey(tree));
		} else {
			M parent = (M) tree.getParent();
			String pkey = getParentKey(key);
			if (parent == null && pkey != null) {
				tree.setKey(key.substring(pkey.length() - 1));
			} else if (pkey == null && parent != null) {
				tree.setKey(new StringBuilder(parent.getKey()).append(key.substring(1)).toString());
			} else if (parent != null && pkey != null && !Beans.isEqual(parent.getKey(), pkey)) {
				tree.setKey(new StringBuilder(parent.getKey()).append(key.substring(pkey.length())).toString());
			}
		}
	}

	/**
	 * 合并树对象并返回根节点列表
	 * 
	 * @param <M>
	 *            数据类型
	 * @param trees
	 *            树对象数组
	 * @return 合并后的树对象根节点列表
	 */
	@SuppressWarnings("rawtypes")
	public static <M extends TreeModel> List<M> mergeTrees(M[] trees) {
		if (trees == null) {
			throw new IllegalArgumentException("Illegal trees:" + trees);
		}
		return trees.length == 0 ? new ArrayList<M>(0) : mergeTrees(Arrays.asList(trees));
	}

	/**
	 * 合并树对象并返回根节点副本（根据key值组装树结构）
	 * 
	 * @param <M>
	 *            数据类型
	 * @param trees
	 *            树对象集合
	 * @return 根节点列表
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <M extends TreeModel> List<M> mergeTrees(Collection<M> trees) {
		if (trees == null) {
			throw new IllegalArgumentException("Illegal trees:" + trees);
		}
		if (trees.isEmpty()) {
			return new ArrayList<M>(0);
		}
		Map<String, M> temp = new LinkedHashMap<String, M>(trees.size());
		Map<String, List<M>> groups = new HashMap<String, List<M>>();
		for (M tree : trees) {
			String key = tree.getKey();
			if (temp.containsKey(key)) {
				continue;
			}
			temp.put(key, tree);
			if (!groups.containsKey(key)) {
				groups.put(key, new LinkedList<M>());
			}
			String parentKey = getParentKey(key);
			if (parentKey != null) {
				List<M> group = groups.get(parentKey);
				if (group == null) {
					group = new LinkedList<M>();
					groups.put(parentKey, group);
				}
				group.add(tree);
			}
		}
		LinkedList<M> roots = new LinkedList<M>();
		for (Entry<String, M> entry : temp.entrySet()) {
			M tree = entry.getValue();
			tree.setChildren(groups.get(entry.getKey()));
			String parentKey = getParentKey(tree.getKey());
			if (parentKey == null || !temp.containsKey(parentKey)) {
				roots.add(tree);
			}
		}
		return roots;
	}

	/**
	 * 级联保存树对象
	 * 
	 * @param <M>
	 *            数据类型
	 * @param repository
	 *            持久化操作对象
	 * @param object
	 *            树对象
	 * @return 保存后的树对象
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <M> M saveTree(Repository<M> repository, M object) {
		if (repository == null) {
			throw new IllegalArgumentException("Illegal repository:" + repository);
		}
		if (object == null) {
			throw new IllegalArgumentException("Illegal object:" + object);
		}
		TreeModel tree = (TreeModel) object;
		List<M> children = new ArrayList<M>(tree.getChildren());
		tree.getChildren().clear();
		Serializable id = repository.save((M) tree);
		tree.setId((Integer) id);
		for (int i = 0; i < children.size(); i++) {
			TreeModel child = (TreeModel) children.get(i);
			child.setParent(tree);
			saveTree(repository, (M) child);
		}
		return object;
	}

	/**
	 * 级联保存树对象
	 * 
	 * @param <M>
	 *            数据类型
	 * @param repository
	 *            持久化操作对象
	 * @param objects
	 *            树对象集合
	 * @return 保存后的树对象列表
	 */
	public static <M> List<M> saveTrees(Repository<M> repository, Collection<M> objects) {
		if (repository == null) {
			throw new IllegalArgumentException("Illegal repository:" + repository);
		}
		if (objects.isEmpty()) {
			return new ArrayList<M>(0);
		}
		List<M> saved = new ArrayList<M>(objects.size());
		for (M tree : objects) {
			saved.add(saveTree(repository, tree));
		}
		return saved;
	}

	/**
	 * 获取对象实体的树形对象
	 * 
	 * @param object
	 *            对象实体
	 * @return 树形对象
	 */
	public static SimpleTree getSimpleTree(Object object) {
		if (object == null) {
			throw new IllegalArgumentException("Illegal object:" + object);
		}
		String id = new StringBuilder(object.getClass().getSimpleName()).append('_').append(object.hashCode())
				.toString();
		Map<String, Object> values = object instanceof Formable ? ((Formable) object).format()
				: Beans.getValues(object);
		return new SimpleTree(id, object.toString(), values);
	}

	/**
	 * 获取对象实体的树形对象
	 * 
	 * @param objects
	 *            对象实体集合
	 * @return 树形对象列表
	 */
	public static List<SimpleTree> getSimpleTrees(Collection<?> objects) {
		return getSimpleTrees(objects, null);
	}

	/**
	 * 获取对象实体的树形对象
	 * 
	 * @param <M>
	 *            数据类型
	 * @param objects
	 *            对象实体集合
	 * @param mappings
	 *            原始对象实例/树对象实例映射
	 * @return 树形对象列表
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <M> List<SimpleTree> getSimpleTrees(Collection<M> objects, Map<M, SimpleTree> mappings) {
		if (objects == null) {
			throw new IllegalArgumentException("Illegal objects:" + objects);
		}
		if (objects.isEmpty()) {
			return new ArrayList<SimpleTree>(0);
		}
		List<SimpleTree> simples = new ArrayList<SimpleTree>(objects.size());
		for (M object : objects) {
			SimpleTree tree = getSimpleTree(object);
			if (mappings != null) {
				mappings.put(object, tree);
			}
			if (object instanceof TreeModel) {
				List<M> children = ((TreeModel) object).getChildren();
				if (!children.isEmpty()) {
					List<SimpleTree> schildren = getSimpleTrees(children, mappings);
					tree.setChildren(schildren);
					for (int i = 0; i < schildren.size(); i++) {
						schildren.get(i).setParent(tree);
					}
				}
			}
			simples.add(tree);
		}
		return simples;
	}

	/**
	 * 获取数据空查询集合
	 * 
	 * @param <M>
	 *            数据类型
	 * @return 数据空查询集合
	 */
	public static <M> Query<M> emptyQuery() {
		return EmptyQuery.instance();
	}

}
