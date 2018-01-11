package ars.database.service;

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.lang.reflect.Modifier;

import ars.util.Beans;
import ars.util.Strings;
import ars.database.model.TreeModel;
import ars.database.service.ServiceFactory;

/**
 * 业务操作工具类
 * 
 * @author yongqiangwu
 * 
 */
public final class Services {
	private static ServiceFactory serviceFactory;

	private Services() {

	}

	public static ServiceFactory getServiceFactory() {
		if (serviceFactory == null) {
			throw new RuntimeException("Service factory has not been initialize");
		}
		return serviceFactory;
	}

	public static void setServiceFactory(ServiceFactory serviceFactory) {
		if (Services.serviceFactory != null) {
			throw new RuntimeException("Service factory is already initialized");
		}
		Services.serviceFactory = serviceFactory;
	}

	/**
	 * 获取业务操作对象
	 * 
	 * @param <M>
	 *            数据类型
	 * @param model
	 *            数据模型
	 * @return 业务操作对象
	 */
	public static <M> Service<M> getService(Class<M> model) {
		return getServiceFactory().getService(model);
	}

	/**
	 * 创建数据业务层代码
	 * 
	 * @param packages
	 *            数据模型所在包
	 */
	public static void createServiceResource(String... packages) {
		for (String pack : packages) {
			createServiceResource(Beans.getClasses(pack).toArray(new Class<?>[0]));
		}
	}

	/**
	 * 创建数据业务层代码
	 * 
	 * @param models
	 *            数据模型数组
	 */
	public static void createServiceResource(Class<?>... models) {
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
			File serviceInterfacePath = new File(new File(resourcePath, Strings.replace(basePack, '.', '/')),
					"service");
			if (!serviceInterfacePath.exists()) {
				serviceInterfacePath.mkdirs();
			}
			String serviceInterfaceName = model.getSimpleName() + "Service";
			File serviceInterface = new File(serviceInterfacePath, serviceInterfaceName + ".java");
			if (!serviceInterface.exists()) {
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(serviceInterface), "utf-8"));
					writer.write("package " + basePack + ".service;");
					writer.newLine();
					writer.newLine();
					writer.write("import ars.invoke.local.Api;");
					writer.newLine();
					if (TreeModel.class.isAssignableFrom(model)) {
						writer.write("import ars.database.service.TreeService;");
						writer.newLine();
					}
					writer.write("import ars.database.service.BasicService;");
					writer.newLine();
					writer.newLine();
					writer.write("import " + model.getName() + ";");
					writer.newLine();
					writer.newLine();
					writer.write("/**");
					writer.newLine();
					writer.write(" * " + model.getSimpleName() + " service interface");
					writer.newLine();
					writer.write(" *");
					writer.newLine();
					writer.write(" * @author " + System.getenv().get("USERNAME"));
					writer.newLine();
					writer.write(" *");
					writer.newLine();
					writer.write(" */");
					writer.newLine();
					writer.write("@Api(\"" + (Strings.replace(pack.substring(0, pack.lastIndexOf('.')), '.', '/') + "/"
							+ model.getSimpleName()).toLowerCase() + "\")");
					writer.newLine();
					if (TreeModel.class.isAssignableFrom(model)) {
						writer.write("public interface " + serviceInterfaceName + " extends BasicService<"
								+ model.getSimpleName() + ">, TreeService<" + model.getSimpleName() + "> {");
					} else {
						writer.write("public interface " + serviceInterfaceName + " extends BasicService<"
								+ model.getSimpleName() + "> {");
					}
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

			File serviceImplementPath = new File(serviceInterfacePath, "impl");
			if (!serviceImplementPath.exists()) {
				serviceImplementPath.mkdirs();
			}
			File serviceImplement = new File(serviceImplementPath, serviceInterfaceName + "Impl.java");
			if (!serviceImplement.exists()) {
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(serviceImplement), "utf-8"));
					writer.write("package " + basePack + ".service.impl;");
					writer.newLine();
					writer.newLine();
					writer.write("import org.springframework.stereotype.Service;");
					writer.newLine();
					writer.newLine();
					writer.write("import ars.database.service.StandardGeneralService;");
					writer.newLine();
					writer.newLine();
					writer.write("import " + model.getName() + ";");
					writer.newLine();
					writer.write("import " + basePack + ".service." + serviceInterfaceName + ";");
					writer.newLine();
					writer.newLine();
					writer.write("@Service");
					writer.newLine();
					writer.write("public class " + serviceInterfaceName + "Impl extends StandardGeneralService<"
							+ model.getSimpleName() + "> implements " + serviceInterfaceName + " {");
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

}
