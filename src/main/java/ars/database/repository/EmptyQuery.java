package ars.database.repository;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import ars.util.Conditions.Logic;
import ars.database.repository.Query;

/**
 * 数据查询集合空实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public final class EmptyQuery<T> implements Query<T> {
	private static EmptyQuery<?> instance;

	private EmptyQuery() {

	}

	/**
	 * 获取数据查询集合实例
	 * 
	 * @param <M>
	 *            数据类型
	 * @return 数据查询集合实例
	 */
	@SuppressWarnings("unchecked")
	public static <M> EmptyQuery<M> instance() {
		if (instance == null) {
			synchronized (EmptyQuery.class) {
				if (instance == null) {
					instance = new EmptyQuery<M>();
				}
			}
		}
		return (EmptyQuery<M>) instance;
	}

	@Override
	public Class<T> getModel() {
		return null;
	}

	@Override
	public boolean isLoaded() {
		return true;
	}

	@Override
	public Iterator<T> iterator() {
		return this.list().iterator();
	}

	@Override
	public Query<T> setDistinct(boolean distinct) {
		return this;
	}

	@Override
	public Query<T> setCacheable(boolean cacheable) {
		return this;
	}

	@Override
	public Query<T> empty(String... properties) {
		return this;
	}

	@Override
	public Query<T> nonempty(String... properties) {
		return this;
	}

	@Override
	public Query<T> eq(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> ne(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> gt(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> ge(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> lt(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> le(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> between(String property, Object low, Object high) {
		return this;
	}

	@Override
	public Query<T> start(String property, String... values) {
		return this;
	}

	@Override
	public Query<T> nstart(String property, String... values) {
		return this;
	}

	@Override
	public Query<T> end(String property, String... values) {
		return this;
	}

	@Override
	public Query<T> nend(String property, String... values) {
		return this;
	}

	@Override
	public Query<T> like(String property, String... values) {
		return this;
	}

	@Override
	public Query<T> nlike(String property, String... values) {
		return this;
	}

	@Override
	public Query<T> in(String property, Object[] values) {
		return this;
	}

	@Override
	public Query<T> or(String property, Object[] values) {
		return this;
	}

	@Override
	public Query<T> not(String property, Object[] values) {
		return this;
	}

	@Override
	public Query<T> custom(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> custom(Map<String, Object> parameters) {
		return this;
	}

	@Override
	public Query<T> condition(Logic logic) {
		return this;
	}

	@Override
	public Query<T> condition(String property, Object value) {
		return this;
	}

	@Override
	public Query<T> condition(Map<String, Object> parameters) {
		return this;
	}

	@Override
	public Query<T> eqProperty(String property, String other) {
		return this;
	}

	@Override
	public Query<T> neProperty(String property, String other) {
		return this;
	}

	@Override
	public Query<T> ltProperty(String property, String other) {
		return this;
	}

	@Override
	public Query<T> leProperty(String property, String other) {
		return this;
	}

	@Override
	public Query<T> gtProperty(String property, String other) {
		return this;
	}

	@Override
	public Query<T> geProperty(String property, String other) {
		return this;
	}

	@Override
	public Query<T> asc(String... properties) {
		return this;
	}

	@Override
	public Query<T> desc(String... properties) {
		return this;
	}

	@Override
	public Query<T> paging(int page, int size) {
		return this;
	}

	@Override
	public Query<T> min(String... properties) {
		return this;
	}

	@Override
	public Query<T> max(String... properties) {
		return this;
	}

	@Override
	public Query<T> avg(String... properties) {
		return this;
	}

	@Override
	public Query<T> sum(String... properties) {
		return this;
	}

	@Override
	public Query<T> number(String... properties) {
		return this;
	}

	@Override
	public Query<T> group(String... properties) {
		return this;
	}

	@Override
	public Query<T> property(String... properties) {
		return this;
	}

	@Override
	public int count() {
		return 0;
	}

	@Override
	public T single() {
		return null;
	}

	@Override
	public List<T> list() {
		return Collections.emptyList();
	}

	@Override
	public List<?> stats() {
		return Collections.emptyList();
	}

}
