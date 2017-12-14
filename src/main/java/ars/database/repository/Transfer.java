package ars.database.repository;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Inherited;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 持久化数据转换注解
 * 
 * @author yongqiangwu
 * 
 */
@Inherited
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transfer {
	/**
	 * 获取当前对象类型唯一标识
	 * 
	 * @return 当前对象唯一标识
	 */
	public String key();

	/**
	 * 转换目标属性名称
	 * 
	 * @return 目标属性名称
	 */
	public String target();

	/**
	 * 获取转换资源地址（列表地址）
	 * 
	 * @return 资源地址
	 */
	public String resource();

	/**
	 * 是否延迟加载
	 * 
	 * @return true/false
	 */
	public boolean lazy() default true;

}
