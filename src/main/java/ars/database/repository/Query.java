package ars.database.repository;

import java.util.Map;
import java.util.List;

import ars.util.Conditions.Logic;

/**
 * 数据查询集合
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public interface Query<T> extends Iterable<T> {
	/**
	 * 高级查询分割符号
	 */
	public static final String DELIMITER = "__";

	/**
	 * 等于
	 */
	public static final String EQ = "eq";

	/**
	 * 大于或等于
	 */
	public static final String GE = "ge";

	/**
	 * 大于
	 */
	public static final String GT = "gt";

	/**
	 * 小于或等于
	 */
	public static final String LE = "le";

	/**
	 * 小于
	 */
	public static final String LT = "lt";

	/**
	 * 不等于
	 */
	public static final String NE = "ne";

	/**
	 * 包含
	 */
	public static final String IN = "in";

	/**
	 * 多条件或
	 */
	public static final String OR = "or";

	/**
	 * 不包含
	 */
	public static final String NOT = "not";

	/**
	 * 属性等于
	 */
	public static final String PEQ = "peq";

	/**
	 * 属性不等于
	 */
	public static final String PNE = "pne";

	/**
	 * 属性小于
	 */
	public static final String PLT = "plt";

	/**
	 * 属性小于或等于
	 */
	public static final String PLE = "ple";

	/**
	 * 属性大于
	 */
	public static final String PGT = "pgt";

	/**
	 * 属性大于或等于
	 */
	public static final String PGE = "pge";

	/**
	 * 空
	 */
	public static final String EMPTY = "empty";

	/**
	 * 非空
	 */
	public static final String NOT_EMPTY = "nempty";

	/**
	 * 匹配开始位置
	 */
	public static final String START = "start";

	/**
	 * 非匹配开始位置
	 */
	public static final String NOT_START = "nstart";

	/**
	 * 匹配结束位置
	 */
	public static final String END = "end";

	/**
	 * 非匹配结束位置
	 */
	public static final String NOT_END = "nend";

	/**
	 * 匹配任意位置
	 */
	public static final String LIKE = "like";

	/**
	 * 非匹配任意位置
	 */
	public static final String NOT_LIKE = "nlike";

	/**
	 * 最小
	 */
	public static final String MIN = "__min";

	/**
	 * 最大
	 */
	public static final String MAX = "__max";

	/**
	 * 平均
	 */
	public static final String AVG = "__avg";

	/**
	 * 和
	 */
	public static final String SUM = "__sum";

	/**
	 * 分组
	 */
	public static final String GROUP = "__group";

	/**
	 * 数量
	 */
	public static final String NUMBER = "__number";

	/**
	 * 属性
	 */
	public static final String PROPERTY = "__property";

	/**
	 * 页码
	 */
	public static final String PAGE = "__page";

	/**
	 * 分页大小
	 */
	public static final String SIZE = "__size";

	/**
	 * 排序
	 */
	public static final String ORDER = "__order";

	/**
	 * 条件
	 */
	public static final String CONDITION = "__condition";

	/**
	 * 获取数据模型
	 * 
	 * @return 数据模型
	 */
	public Class<T> getModel();

	/**
	 * 获取数据是否已加载
	 * 
	 * @return true/false
	 */
	public boolean isLoaded();

	/**
	 * 设置是否过滤重复数据
	 * 
	 * @param distinct
	 *            true/false
	 * @return 数据集合
	 */
	public Query<T> setDistinct(boolean distinct);

	/**
	 * 设置是否使用缓存
	 * 
	 * @param cacheable
	 *            true/false
	 * @return 数据集合
	 */
	public Query<T> setCacheable(boolean cacheable);

	/**
	 * 等于空
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> empty(String... properties);

	/**
	 * 非空
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> nonempty(String... properties);

	/**
	 * 等于
	 * 
	 * @param property
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> eq(String property, Object value);

	/**
	 * 不等于
	 * 
	 * @param property
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> ne(String property, Object value);

	/**
	 * 大于
	 * 
	 * @param property
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> gt(String property, Object value);

	/**
	 * 大于或等于
	 * 
	 * @param property
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> ge(String property, Object value);

	/**
	 * 小于
	 * 
	 * @param property
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> lt(String property, Object value);

	/**
	 * 小于或等于
	 * 
	 * @param property
	 *            属性名
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> le(String property, Object value);

	/**
	 * 属性值在两个值之间
	 * 
	 * @param property
	 *            属性名
	 * @param low
	 *            低值
	 * @param high
	 *            高值
	 * @return 数据集合
	 */
	public Query<T> between(String property, Object low, Object high);

	/**
	 * 以指定字符串为开始
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值数组
	 * @return 数据集合
	 */
	public Query<T> start(String property, String... values);

	/**
	 * 排除以指定字符串为开始
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值数组
	 * @return 数据集合
	 */
	public Query<T> nstart(String property, String... values);

	/**
	 * 以指定字符串为结束
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值数组
	 * @return 数据集合
	 */
	public Query<T> end(String property, String... values);

	/**
	 * 排除以指定字符串为结束
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值数组
	 * @return 数据集合
	 */
	public Query<T> nend(String property, String... values);

	/**
	 * 包含指定字符串
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值数组
	 * @return 数据集合
	 */
	public Query<T> like(String property, String... values);

	/**
	 * 排除包含指定字符串
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值数组
	 * @return 数据集合
	 */
	public Query<T> nlike(String property, String... values);

	/**
	 * 属性值在指定数据范围内
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值集合
	 * @return 数据集合
	 */
	public Query<T> in(String property, Object[] values);

	/**
	 * 或
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值集合
	 * @return 数据集合
	 */
	public Query<T> or(String property, Object[] values);

	/**
	 * 非
	 * 
	 * @param property
	 *            属性名
	 * @param values
	 *            属性值集合
	 * @return 数据集合
	 */
	public Query<T> not(String property, Object[] values);

	/**
	 * 根据属性名称特性进行定制查询
	 * 
	 * @param property
	 *            自定义属性名称
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> custom(String property, Object value);

	/**
	 * 根据属性名称特性进行定制查询
	 * 
	 * @param parameters
	 *            参数键/值表
	 * @return 数据集合
	 */
	public Query<T> custom(Map<String, Object> parameters);

	/**
	 * 自定义查询条件
	 * 
	 * @param logic
	 *            条件逻辑
	 * @return 数据集合
	 */
	public Query<T> condition(Logic logic);

	/**
	 * 自定义查询条件
	 * 
	 * @param property
	 *            自定义属性名称
	 * @param value
	 *            属性值
	 * @return 数据集合
	 */
	public Query<T> condition(String property, Object value);

	/**
	 * 自定义查询条件
	 * 
	 * @param parameters
	 *            参数键/值表
	 * @return 数据集合
	 */
	public Query<T> condition(Map<String, Object> parameters);

	/**
	 * 一个属性值等于另一个属性值 property=other
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 数据集合
	 */
	public Query<T> eqProperty(String property, String other);

	/**
	 * 一个属性值不等于另一个属性值
	 * 
	 * @param property
	 *            属性名称
	 * @param other
	 *            属性名称
	 * @return 数据集合
	 */
	public Query<T> neProperty(String property, String other);

	/**
	 * 一个属性值小于另一个属性值
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 数据集合
	 */
	public Query<T> ltProperty(String property, String other);

	/**
	 * 一个属性值小于或等于另一个属性值
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 数据集合
	 */
	public Query<T> leProperty(String property, String other);

	/**
	 * 一个属性值大于另一个属性值
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 数据集合
	 */
	public Query<T> gtProperty(String property, String other);

	/**
	 * 一个属性值大于或等于另一个属性值
	 * 
	 * @param property
	 *            属性名
	 * @param other
	 *            属性名
	 * @return 数据集合
	 */
	public Query<T> geProperty(String property, String other);

	/**
	 * 多个属性升序排序
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> asc(String... properties);

	/**
	 * 多个属性降序排序
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> desc(String... properties);

	/**
	 * 数据集合分页
	 * 
	 * @param page
	 *            页码（从1开始）
	 * @param size
	 *            每页数据量
	 * @return 数据集合
	 */
	public Query<T> paging(int page, int size);

	/**
	 * 最小值统计
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> min(String... properties);

	/**
	 * 最大值统计
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> max(String... properties);

	/**
	 * 平均值统计
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> avg(String... properties);

	/**
	 * 总和统计
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> sum(String... properties);

	/**
	 * 数据量统计
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> number(String... properties);

	/**
	 * 分组
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> group(String... properties);

	/**
	 * 查询指定属性
	 * 
	 * @param properties
	 *            属性名数组
	 * @return 数据集合
	 */
	public Query<T> property(String... properties);

	/**
	 * 获取数量
	 * 
	 * @return 数量
	 */
	public int count();

	/**
	 * 获取一个数据对象
	 * 
	 * 当查询结果数量大于1时将抛出异常，当没有查询到数据时返回null
	 * 
	 * @return 数据对象
	 */
	public T single();

	/**
	 * 将数据集合对象转换成List对象
	 * 
	 * @return 列表对象
	 */
	public List<T> list();

	/**
	 * 统计数据
	 * 
	 * @return 数据列表
	 */
	public List<?> stats();

}
