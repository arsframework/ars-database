package ars.database.hibernate;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Collection;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.hibernate.Session;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;
import org.hibernate.type.CollectionType;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import ars.util.Beans;
import ars.util.Strings;
import ars.database.model.Model;
import ars.database.model.TreeModel;
import ars.database.repository.Query;
import ars.database.hibernate.DetachedCriteriaQuery;

/**
 * Hibernate操作工具类
 * 
 * @author yongqiangwu
 * 
 */
public final class Hibernates {
	private Hibernates() {

	}

	/**
	 * 获取数据模型元对象
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param model
	 *            数据模型
	 * @return 元对象
	 */
	public static ClassMetadata getClassMetadata(SessionFactory sessionFactory, Class<?> model) {
		ClassMetadata metadata = sessionFactory.getClassMetadata(model);
		if (metadata == null) {
			throw new RuntimeException("Class metadata not found:" + model.getName());
		}
		return metadata;
	}

	/**
	 * 获取模型主键名称
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param model
	 *            模型对象
	 * @return 主键名称
	 */
	public static String getPrimary(SessionFactory sessionFactory, Class<?> model) {
		return getClassMetadata(sessionFactory, model).getIdentifierPropertyName();
	}

	/**
	 * 获取主键类型
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param model
	 *            模型对象
	 * @return 主键类型
	 */
	public static Class<?> getPrimaryClass(SessionFactory sessionFactory, Class<?> model) {
		return getClassMetadata(sessionFactory, model).getIdentifierType().getReturnedClass();
	}

	/**
	 * 获取对象主键值
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param object
	 *            对象实例
	 * @return 主键值
	 */
	public static Serializable getIdentifier(SessionFactory sessionFactory, Object object) {
		return object == null ? null
				: object instanceof Model ? ((Model) object).getId()
						: (Serializable) Beans.getValue(object, getPrimary(sessionFactory, object.getClass()));
	}

	/**
	 * 设置对象主键值
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param object
	 *            对象实例
	 * @param id
	 *            主键值
	 */
	public static void setIdentifier(SessionFactory sessionFactory, Object object, Object id) {
		if (object != null && id != null) {
			if (object instanceof Model) {
				((Model) object).setId(Beans.toInteger(int.class, id));
			} else {
				Beans.setValue(object, getPrimary(sessionFactory, object.getClass()), id);
			}
		}
	}

	/**
	 * 获取模型属性
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param model
	 *            数据模型
	 * @return 属性名称数组
	 */
	public static String[] getProperties(SessionFactory sessionFactory, Class<?> model) {
		return getClassMetadata(sessionFactory, model).getPropertyNames();
	}

	/**
	 * 获取类型对象
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param type
	 *            属性映射类型对象
	 * @return 类型对象
	 */
	public static Class<?> getPropertyTypeClass(SessionFactory sessionFactory, Type type) {
		return type.isCollectionType()
				? ((CollectionType) type).getElementType((SessionFactoryImplementor) sessionFactory).getReturnedClass()
				: type.getReturnedClass();
	}

	/**
	 * 获取模型属性类型
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param model
	 *            数据模型
	 * @param property
	 *            属性名称
	 * @return 类型对象
	 */
	public static Type getPropertyType(SessionFactory sessionFactory, Class<?> model, String property) {
		int index = property.indexOf('.');
		if (index > 0) {
			Type type = getPropertyType(sessionFactory, model, property.substring(0, index));
			return getPropertyType(sessionFactory, getPropertyTypeClass(sessionFactory, type),
					property.substring(index + 1));
		}
		ClassMetadata metadata = getClassMetadata(sessionFactory, model);
		if (metadata.hasIdentifierProperty() && metadata.getIdentifierPropertyName().equals(property)) {
			return metadata.getIdentifierType();
		}
		return metadata.getPropertyType(property);
	}

	/**
	 * 获取模型属性类型
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param model
	 *            数据模型
	 * @param property
	 *            属性名称
	 * @return 属性类型
	 */
	public static Class<?> getPropertyClass(SessionFactory sessionFactory, Class<?> model, String property) {
		Type type = getPropertyType(sessionFactory, model, property);
		return getPropertyTypeClass(sessionFactory, type);
	}

	/**
	 * 获取对象属性值
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param object
	 *            对象实例
	 * @param property
	 *            属性名称
	 * @return 属性值
	 */
	public static Object getValue(SessionFactory sessionFactory, Object object, String property) {
		if (object == null) {
			return null;
		}
		ClassMetadata metadata = getClassMetadata(sessionFactory, object.getClass());
		Object value = metadata.getPropertyValue(object, property);
		if (value != null && !Hibernate.isInitialized(value)) {
			Type type = metadata.getPropertyType(property);
			return type.isCollectionType() ? new ArrayList<Object>(0) : null;
		}
		return value;
	}

	/**
	 * 获取对象多个属性值
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param object
	 *            对象实例
	 * @param properties
	 *            属性名称数组
	 * @return 属性名称/值键值对
	 */
	public static Map<String, Object> getValues(SessionFactory sessionFactory, Object object, String... properties) {
		if (object == null) {
			return new HashMap<String, Object>(0);
		}
		if (properties == null || properties.length == 0) {
			properties = getProperties(sessionFactory, object.getClass());
		}
		Map<String, Object> values = new HashMap<String, Object>(properties.length);
		for (String property : properties) {
			values.put(property, getValue(sessionFactory, object, property));
		}
		return values;
	}

	/**
	 * 设置对象属性值
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param object
	 *            对象实例
	 * @param property
	 *            属性名称
	 * @param value
	 *            属性值
	 */
	public static void setValue(SessionFactory sessionFactory, Object object, String property, Object value) {
		if (object == null) {
			return;
		}
		ClassMetadata metadata = getClassMetadata(sessionFactory, object.getClass());
		Type type = metadata.getPropertyType(property);
		Class<?> meta = getPropertyTypeClass(sessionFactory, type);
		if (type.isEntityType()) { // 多对一
			if (value != null && !meta.isAssignableFrom(value.getClass())) {
				Serializable id = (Serializable) Beans.toObject(getPrimaryClass(sessionFactory, meta), value);
				Session session = sessionFactory.openSession();
				try {
					value = session.get(meta, id);
				} finally {
					session.close();
				}
			}
			metadata.setPropertyValue(object, property, value);
		} else if (type.isCollectionType()) { // 多对多
			Object[] values = Beans.toArray(Object.class, value);
			Collection<Object> objects = Set.class.isAssignableFrom(type.getReturnedClass())
					? new HashSet<Object>(values.length)
					: new ArrayList<Object>(values.length);
			if (values.length > 0) {
				Session session = null;
				Class<?> foreignPrimaryClass = getPrimaryClass(sessionFactory, meta);
				try {
					for (Object v : values) {
						if (v != null && !meta.isAssignableFrom(v.getClass())) {
							if (session == null) {
								session = sessionFactory.openSession();
							}
							Serializable id = (Serializable) Beans.toObject(foreignPrimaryClass, v);
							v = session.get(meta, id);
						}
						if (v != null) {
							objects.add(v);
						}
					}
				} finally {
					if (session != null) {
						session.close();
					}
				}
			}
			metadata.setPropertyValue(object, property, objects);
		}
		metadata.setPropertyValue(object, property, Beans.toObject(meta, value));
	}

	/**
	 * 设置对象属性值
	 * 
	 * @param sessionFactory
	 *            会话工厂
	 * @param object
	 *            对象实例
	 * @param parameters
	 *            属性名称/值键值对
	 */
	public static void setValues(SessionFactory sessionFactory, Object object, Map<String, Object> parameters) {
		if (object != null && parameters != null && !parameters.isEmpty()) {
			for (Entry<String, Object> entry : parameters.entrySet()) {
				setValue(sessionFactory, object, entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * 获取数据查询对象
	 * 
	 * @param <T>
	 *            数据类型
	 * @param sessionFactory
	 *            会话工厂
	 * @param model
	 *            数据模型
	 * @return 数据查询对象
	 */
	public static <T> Query<T> query(SessionFactory sessionFactory, Class<T> model) {
		if (sessionFactory == null) {
			throw new IllegalArgumentException("Illegal sessionFactory:" + sessionFactory);
		}
		if (model == null) {
			throw new IllegalArgumentException("Illegal model:" + model);
		}
		return new DetachedCriteriaQuery<T>(sessionFactory, model);
	}

	/**
	 * 创建数据持久层代码
	 * 
	 * @param packages
	 *            数据模型所在包
	 */
	public static void createRepositoryResource(String... packages) {
		for (String pack : packages) {
			createRepositoryResource(Beans.getClasses(pack).toArray(new Class<?>[0]));
		}
	}

	/**
	 * 创建数据持久层代码
	 * 
	 * @param models
	 *            数据模型数组
	 */
	public static void createRepositoryResource(Class<?>... models) {
		for (Class<?> model : models) {
			if (Beans.isMetaClass(model) || Modifier.isAbstract(model.getModifiers())) {
				continue;
			}
			File resourcePath;
			try {
				resourcePath = new File(new File(URLDecoder.decode(System.getProperty("user.dir"), "utf-8")), "src");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			String pack = model.getPackage().getName();
			String basePack = pack.substring(0, pack.lastIndexOf("."));
			File repositoryInterfacePath = new File(new File(resourcePath, Strings.replace(basePack, '.', '/')),
					"repository");
			if (!repositoryInterfacePath.exists()) {
				repositoryInterfacePath.mkdirs();
			}
			String repositoryInterfaceName = model.getSimpleName() + "Repository";
			File repositoryInterface = new File(repositoryInterfacePath, repositoryInterfaceName + ".java");
			if (!repositoryInterface.exists()) {
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(repositoryInterface), "utf-8"));
					writer.write("package " + basePack + ".repository;");
					writer.newLine();
					writer.newLine();
					writer.write("import ars.database.repository.Repository;");
					writer.newLine();
					writer.newLine();
					writer.write("import " + model.getName() + ";");
					writer.newLine();
					writer.newLine();
					writer.write("/**");
					writer.newLine();
					writer.write(" * " + model.getSimpleName() + " repository interface");
					writer.newLine();
					writer.write(" *");
					writer.newLine();
					writer.write(" * @author " + System.getenv().get("USERNAME"));
					writer.newLine();
					writer.write(" *");
					writer.newLine();
					writer.write(" */");
					writer.newLine();
					writer.write("public interface " + repositoryInterfaceName + " extends Repository<"
							+ model.getSimpleName() + "> {");
					writer.newLine();
					writer.newLine();
					writer.write("}");
					writer.newLine();
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

			File repositoryImplementPath = new File(repositoryInterfacePath, "impl");
			if (!repositoryImplementPath.exists()) {
				repositoryImplementPath.mkdirs();
			}
			File repositoryImplement = new File(repositoryImplementPath, repositoryInterfaceName + "Impl.java");
			if (!repositoryImplement.exists()) {
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(repositoryImplement), "utf-8"));
					writer.write("package " + basePack + ".repository.impl;");
					writer.newLine();
					writer.newLine();
					writer.write("import org.springframework.stereotype.Repository;");
					writer.newLine();
					writer.newLine();
					writer.write("import ars.database.hibernate.HibernateSimpleRepository;");
					writer.newLine();
					writer.newLine();
					writer.write("import " + model.getName() + ";");
					writer.newLine();
					writer.write("import " + basePack + ".repository." + repositoryInterfaceName + ";");
					writer.newLine();
					writer.newLine();
					writer.write("@Repository");
					writer.newLine();
					writer.write("public class " + repositoryInterfaceName + "Impl extends HibernateSimpleRepository<"
							+ model.getSimpleName() + "> implements " + repositoryInterfaceName + " {");
					writer.newLine();
					writer.newLine();
					writer.write("}");
					writer.newLine();
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * 创建实体映射配置文件
	 * 
	 * @param packages
	 *            数据模型所在包
	 */
	public static void createEntityConfigure(String... packages) {
		for (String pack : packages) {
			createEntityConfigure(Beans.getClasses(pack).toArray(new Class<?>[0]));
		}
	}

	/**
	 * 创建实体映射配置文件
	 * 
	 * @param models
	 *            数据模型数组
	 */
	public static void createEntityConfigure(Class<?>... models) {
		for (Class<?> model : models) {
			if (Beans.isMetaClass(model) || Modifier.isAbstract(model.getModifiers())) {
				continue;
			}
			File resourcePath;
			try {
				resourcePath = new File(new File(URLDecoder.decode(System.getProperty("user.dir"), "utf-8")), "src");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			String pack = model.getPackage().getName();
			File configurePath = new File(new File(resourcePath, Strings.replace(pack, '.', '/')), "hbm");
			if (!configurePath.exists()) {
				configurePath.mkdirs();
			}
			File configure = new File(configurePath, model.getSimpleName() + ".hbm.xml");
			if (!configure.exists()) {
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configure), "utf-8"));
					writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					writer.newLine();
					writer.write("<!DOCTYPE hibernate-mapping PUBLIC");
					writer.newLine();
					writer.write("\t\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"");
					writer.newLine();
					writer.write("\t\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">");
					writer.newLine();
					writer.write("<hibernate-mapping>");
					writer.newLine();
					writer.write("\t<class name=\"" + model.getName() + "\" table=\""
							+ Strings.splitHumpString(model.getSimpleName(), true) + "\">");
					writer.newLine();
					writer.write("\t\t<id name=\"id\" column=\"ID_\" type=\"int\">");
					writer.newLine();
					writer.write("\t\t\t<generator class=\"native\"/>");
					writer.newLine();
					writer.write("\t\t</id>");

					Class<?> _model = model;
					while (_model != Object.class) {
						Field[] fields = _model.getDeclaredFields();
						for (Field field : fields) {
							if (!Modifier.isStatic(field.getModifiers())) {
								String name = field.getName();
								Class<?> type = field.getType();
								if (type == Object.class || name.equals("id")) {
									continue;
								}
								String column = Strings.splitHumpString(name, true);
								if (type == byte.class || type == Byte.class) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
											+ "_\" type=\"byte\"/>");
								} else if (type == byte[].class || type == Byte[].class) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
											+ "_\" type=\"binary\"/>");
								} else if (type == int.class || type == Integer.class) {
									writer.newLine();
									if ((Model.class.isAssignableFrom(model) && name.equals("status"))
											|| (TreeModel.class.isAssignableFrom(model) && name.equals("level"))) {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"int\" not-null=\"true\"/>");
									} else {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"int\"/>");
									}
								} else if (type == short.class || type == Short.class) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
											+ "_\" type=\"short\"/>");
								} else if (type == float.class || type == Float.class) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
											+ "_\" type=\"float\"/>");
								} else if (type == double.class || type == Double.class) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
											+ "_\" type=\"double\"/>");
								} else if (type == long.class || type == Long.class) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
											+ "_\" type=\"long\"/>");
								} else if (type == boolean.class || type == Boolean.class) {
									writer.newLine();
									if ((Model.class.isAssignableFrom(model) && name.equals("active"))
											|| (TreeModel.class.isAssignableFrom(model) && (name.equals("leaf")))) {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"boolean\" not-null=\"true\"/>");
									} else {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"boolean\"/>");
									}
								} else if (type == char.class || type == Character.class) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
											+ "_\" type=\"character\"/>");
								} else if (type == String.class) {
									writer.newLine();
									if (TreeModel.class.isAssignableFrom(model) && name.equals("key")) {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"string\" length=\"50\" not-null=\"true\" unique=\"true\"/>");
									} else {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"string\" length=\"50\"/>");
									}
								} else if (Date.class.isAssignableFrom(type)) {
									writer.newLine();
									if (Model.class.isAssignableFrom(model) && name.equals("dateJoined")) {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"timestamp\" not-null=\"true\"/>");
									} else {
										writer.write("\t\t<property name=\"" + name + "\" column=\"" + column
												+ "_\" type=\"timestamp\"/>");
									}
								} else if (Enum.class.isAssignableFrom(type)) {
									writer.newLine();
									writer.write("\t\t<property name=\"" + name + "\" column=\"" + column + "_\">");
									writer.newLine();
									writer.write("\t\t\t<type name=\"org.hibernate.type.EnumType\">");
									writer.newLine();
									writer.write("\t\t\t\t<param name=\"enumClass\">" + type.getName() + "</param>");
									writer.newLine();
									writer.write("\t\t\t</type>");
									writer.newLine();
									writer.write("\t\t</property>");
								} else if (Collection.class.isAssignableFrom(type)) {
									Class<?> foreign = Beans.getFieldGenericType(field);
									if (foreign == null || foreign == Object.class
											|| Modifier.isAbstract(foreign.getModifiers())) {
										continue;
									}
									writer.newLine();
									if (Set.class.isAssignableFrom(type)) {
										writer.write("\t\t<set name=\"" + name + "\">");
									} else {
										writer.write("\t\t<list name=\"" + name + "\">");
									}
									writer.newLine();
									writer.write("\t\t\t<key column=\""
											+ Strings.splitHumpString(model.getSimpleName(), true)
											+ "_ID_\" not-null=\"true\"/>");
									if (List.class.isAssignableFrom(type)) {
										writer.newLine();
										writer.write("\t\t\t<index column=\"ORDER_\"/>");
									}
									writer.newLine();
									if (Beans.isMetaClass(foreign)) {
										writer.write("\t\t\t<element type=\"" + foreign.getName() + "\" column=\""
												+ name.toUpperCase() + "_\"/>");
									} else {
										writer.write("\t\t\t<many-to-many class=\"" + foreign.getName() + "\" column=\""
												+ Strings.splitHumpString(foreign.getSimpleName(), true) + "_ID_\"/>");
									}
									writer.newLine();
									if (Set.class.isAssignableFrom(type)) {
										writer.write("\t\t</set>");
									} else {
										writer.write("\t\t</list>");
									}
								} else if (!Modifier.isAbstract(type.getModifiers())) {
									writer.newLine();
									writer.write("\t\t<many-to-one name=\"" + name + "\" column=\"" + column
											+ "_ID_\" class=\"" + type.getName() + "\"/>");
								}
							}
						}
						_model = _model.getSuperclass();
					}
					if (TreeModel.class.isAssignableFrom(model)) {
						writer.newLine();
						writer.write("\t\t<many-to-one name=\"parent\" column=\"PARENT_ID_\" class=\"" + model.getName()
								+ "\"/>");
						writer.newLine();
						writer.write("\t\t<list name=\"children\" inverse=\"true\">");
						writer.newLine();
						writer.write("\t\t\t<key column=\"PARENT_ID_\"/>");
						writer.newLine();
						writer.write("\t\t\t<index column=\"ORDER_\"/>");
						writer.newLine();
						writer.write("\t\t\t<one-to-many class=\"" + model.getName() + "\"/>");
						writer.newLine();
						writer.write("\t\t</list>");
					}
					writer.newLine();
					writer.write("\t</class>");
					writer.newLine();
					writer.write("</hibernate-mapping>");
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

}
