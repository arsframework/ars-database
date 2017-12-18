package ars.database.repository;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

import ars.util.Conditions.Logic;
import ars.database.repository.Query;

/**
 * 数据包装查询实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据类型
 */
public class WrapQuery<T> implements Query<T> {
	private Query<T> query; // 查询对象

	public WrapQuery(Query<T> query) {
		if (query == null) {
			throw new IllegalArgumentException("Illegal query:" + query);
		}
		this.query = query;
	}

	public Query<T> getQuery() {
		return query;
	}

	@Override
	public Iterator<T> iterator() {
		return this.query.iterator();
	}

	@Override
	public Class<T> getModel() {
		return this.query.getModel();
	}

	@Override
	public Query<T> empty(String... properties) {
		return this.query.empty(properties);
	}

	@Override
	public Query<T> nonempty(String... properties) {
		return this.query.nonempty(properties);
	}

	@Override
	public Query<T> eq(String property, Object value) {
		return this.query.eq(property, value);
	}

	@Override
	public Query<T> ne(String property, Object value) {
		return this.query.ne(property, value);
	}

	@Override
	public Query<T> gt(String property, Object value) {
		return this.query.gt(property, value);
	}

	@Override
	public Query<T> ge(String property, Object value) {
		return this.query.ge(property, value);
	}

	@Override
	public Query<T> lt(String property, Object value) {
		return this.query.lt(property, value);
	}

	@Override
	public Query<T> le(String property, Object value) {
		return this.query.le(property, value);
	}

	@Override
	public Query<T> between(String property, Object low, Object high) {
		return this.query.between(property, low, high);
	}

	@Override
	public Query<T> start(String property, String... values) {
		return this.query.start(property, values);
	}

	@Override
	public Query<T> nstart(String property, String... values) {
		return this.query.nstart(property, values);
	}

	@Override
	public Query<T> end(String property, String... values) {
		return this.query.end(property, values);
	}

	@Override
	public Query<T> nend(String property, String... values) {
		return this.query.nend(property, values);
	}

	@Override
	public Query<T> like(String property, String... values) {
		return this.query.like(property, values);
	}

	@Override
	public Query<T> nlike(String property, String... values) {
		return this.query.nlike(property, values);
	}

	@Override
	public Query<T> in(String property, Object[] values) {
		return this.query.in(property, values);
	}

	@Override
	public Query<T> or(String property, Object[] values) {
		return this.query.or(property, values);
	}

	@Override
	public Query<T> not(String property, Object[] values) {
		return this.query.not(property, values);
	}

	@Override
	public Query<T> custom(String property, Object value) {
		return this.query.custom(property, value);
	}

	@Override
	public Query<T> custom(Map<String, Object> parameters) {
		return this.query.custom(parameters);
	}

	@Override
	public Query<T> condition(Logic logic) {
		return this.query.condition(logic);
	}

	@Override
	public Query<T> condition(String property, Object value) {
		return this.query.condition(property, value);
	}

	@Override
	public Query<T> condition(Map<String, Object> parameters) {
		return this.query.condition(parameters);
	}

	@Override
	public Query<T> eqProperty(String property, String other) {
		return this.query.eqProperty(property, other);
	}

	@Override
	public Query<T> neProperty(String property, String other) {
		return this.query.neProperty(property, other);
	}

	@Override
	public Query<T> ltProperty(String property, String other) {
		return this.query.ltProperty(property, other);
	}

	@Override
	public Query<T> leProperty(String property, String other) {
		return this.query.leProperty(property, other);
	}

	@Override
	public Query<T> gtProperty(String property, String other) {
		return this.query.gtProperty(property, other);
	}

	@Override
	public Query<T> geProperty(String property, String other) {
		return this.query.geProperty(property, other);
	}

	@Override
	public Query<T> asc(String... properties) {
		return this.query.asc(properties);
	}

	@Override
	public Query<T> desc(String... properties) {
		return this.query.desc(properties);
	}

	@Override
	public Query<T> paging(int page, int size) {
		return this.query.paging(page, size);
	}

	@Override
	public Query<T> min(String... properties) {
		return this.query.min(properties);
	}

	@Override
	public Query<T> max(String... properties) {
		return this.query.max(properties);
	}

	@Override
	public Query<T> avg(String... properties) {
		return this.query.avg(properties);
	}

	@Override
	public Query<T> sum(String... properties) {
		return this.query.sum(properties);
	}

	@Override
	public Query<T> number(String... properties) {
		return this.query.number(properties);
	}

	@Override
	public Query<T> group(String... properties) {
		return this.query.group(properties);
	}

	@Override
	public Query<T> property(String... properties) {
		return this.query.property(properties);
	}

	@Override
	public int count() {
		return this.query.count();
	}

	@Override
	public T single() {
		return this.query.single();
	}

	@Override
	public List<T> list() {
		return this.query.list();
	}

	@Override
	public List<?> stats() {
		return this.query.stats();
	}

}
