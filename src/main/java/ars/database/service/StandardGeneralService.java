package ars.database.service;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map.Entry;
import java.lang.reflect.Field;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import ars.util.Beans;
import ars.util.Dates;
import ars.util.Files;
import ars.util.Nfile;
import ars.util.Strings;
import ars.file.office.Excels;
import ars.invoke.Invokes;
import ars.invoke.request.Requester;
import ars.database.model.TreeModel;
import ars.database.repository.Query;
import ars.database.repository.Repositories;
import ars.database.service.ExcelAdapter;
import ars.database.service.ImportService;
import ars.database.service.AbstractService;

/**
 * 通用业务操作接口抽象实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public abstract class StandardGeneralService<T> extends AbstractService<T> {
	private String directory = Strings.TEMP_PATH;
	private Class<? extends ExcelAdapter<T>> excelAdapterClass = null;

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public Class<? extends ExcelAdapter<T>> getExcelAdapterClass() {
		return excelAdapterClass;
	}

	public void setExcelAdapterClass(Class<? extends ExcelAdapter<T>> excelAdapterClass) {
		this.excelAdapterClass = excelAdapterClass;
	}

	/**
	 * 获取Excel数据对象适配器
	 * 
	 * @return Excel数据对象适配器
	 */
	protected ExcelAdapter<T> getExcelAdapter() {
		if (this.excelAdapterClass == null) {
			final Field[] fields = Beans.getFields(this.getModel());
			return new ExcelAdapter<T>() {

				@Override
				public String[] getTitles(Requester requester, Service<T> service) {
					return Invokes.getPropertyMessages(requester, getModel());
				}

				@Override
				public T read(Requester requester, Service<T> service, Row row, int count) {
					return Excels.getObject(row, getModel(), fields);
				}

				@Override
				public void write(Requester requester, Service<T> service, T entity, Row row, int count) {
					Excels.setObject(row, entity, fields);
				}

			};
		}
		return Beans.getInstance(this.excelAdapterClass);
	}

	/**
	 * 获取查询有效参数（排除参数值为空的参数）
	 * 
	 * @param parameters
	 *            参数键/值对
	 * @return 有效参数键/值对
	 */
	protected Map<String, Object> getEffectiveParameters(Map<String, Object> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return new HashMap<String, Object>(0);
		}
		String emptySuffix = new StringBuilder(Query.DELIMITER).append(Query.EMPTY).toString();
		String nonemptySuffix = new StringBuilder(Query.DELIMITER).append(Query.NOT_EMPTY).toString();
		Map<String, Object> effectives = new HashMap<String, Object>(parameters.size());
		for (Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (Beans.isEmpty(value) && !key.endsWith(emptySuffix) && !key.endsWith(nonemptySuffix)) {
				continue;
			}
			effectives.put(key, value);
		}
		return effectives;
	}

	/**
	 * 获取数据查询对象
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            数据过滤参数
	 * @return 数据查询对象
	 */
	protected Query<T> getQuery(Requester requester, Map<String, Object> parameters) {
		return this.getQuery(requester).custom(parameters);
	}

	/**
	 * 数据批量导入
	 * 
	 * @param requester
	 *            请求对象
	 * @param file
	 *            文件对象
	 * @param start
	 *            开始数据行（从0开始）
	 * @param adapter
	 *            Excel文件适配对象
	 * @param parameters
	 *            请求参数
	 * @return 数据导入结果对象
	 * @throws Exception
	 *             操作异常
	 */
	protected ImportService.Result import_(final Requester requester, final Nfile file, final Integer start,
			final ExcelAdapter<T> adapter, Map<String, Object> parameters) throws Exception {
		long timestamp = System.currentTimeMillis();
		final ImportService.Result result = new ImportService.Result();
		final String[] titles = start > 0 ? Excels.getTitles(file, start - 1) : Strings.EMPTY_ARRAY;
		final Workbook failed = new SXSSFWorkbook(100);
		try {
			int count = Excels.iteration(file, start, new Excels.Reader<T>() {

				@Override
				public T read(Row row, int count) {
					try {
						T entity = adapter.read(requester, StandardGeneralService.this, row, count);
						if (entity != null) {
							saveObject(requester, entity);
						}
					} catch (Exception e) {
						Row target = null;
						if (result.getFailed() % 50000 == 0) {
							Sheet sheet = failed.createSheet();
							if (titles.length > 0) {
								Excels.setTitles(sheet.createRow(0), titles);
							}
							target = sheet.createRow(start);
						} else {
							Sheet sheet = failed.getSheetAt(failed.getNumberOfSheets() - 1);
							target = sheet.createRow(sheet.getLastRowNum() + 1);
						}
						Excels.copy(row, target);
						int columns = titles.length > 0 ? titles.length : row.getLastCellNum();
						Excels.setValue(target.createCell(columns), e.getMessage());
						result.setFailed(result.getFailed() + 1);
					}
					return null;
				}
			});
			result.setTotal(count);
			if (result.getFailed() > 0) {
				String name = new StringBuilder(UUID.randomUUID().toString()).append(".xlsx").toString();
				File attachment = new File(this.directory, name);
				Excels.write(failed, attachment);
				result.setFile(name);
				result.setSize(Files.getUnitSize(attachment.length()));
			}
		} finally {
			failed.close();
		}
		result.setSpend(Dates.getUnitTime(System.currentTimeMillis() - timestamp));
		return result;
	}

	/**
	 * 数据批量导出
	 * 
	 * @param requester
	 *            请求对象
	 * @param adapter
	 *            Excel文件适配对象
	 * @param parameters
	 *            请求参数
	 * @return 导出结果
	 * @throws Exception
	 *             操作异常
	 */
	public ExportService.Result export(Requester requester, ExcelAdapter<T> adapter, Map<String, Object> parameters)
			throws Exception {
		parameters = requester.getParameters();
		Integer page = Beans.toInteger(Integer.class, parameters.remove(Query.PAGE));
		Integer size = Beans.toInteger(Integer.class, parameters.remove(Query.SIZE));

		long timestamp = System.currentTimeMillis();
		String[] titles = adapter.getTitles(requester, this);
		final ExportService.Result result = new ExportService.Result();
		Workbook workbook = new SXSSFWorkbook(100);
		try {
			if (page == null || size == null) {
				Sheet sheet = null;
				int r = 1, count = 0, length = 200;
				int total = this.getQuery(requester, parameters).count();
				for (int i = 1, pages = (int) Math.ceil((double) total / length); i <= pages; i++) {
					List<T> objects = this.getQuery(requester, parameters).paging(i, length).list();
					for (int n = 0; n < objects.size(); n++) {
						if (++count % 50000 == 1) {
							r = 1;
							sheet = workbook.createSheet();
							Excels.setTitles(sheet.createRow(0), titles);
						}
						adapter.write(requester, this, objects.get(n), sheet.createRow(r++), count);
					}
				}
				result.setTotal(count);
			} else {
				Sheet sheet = workbook.createSheet();
				Excels.setTitles(sheet.createRow(0), titles);
				List<T> objects = this.getQuery(requester, parameters).paging(page, size).list();
				for (int i = 0; i < objects.size(); i++) {
					adapter.write(requester, this, objects.get(i), sheet.createRow(i + 1), i + 1);
				}
				result.setTotal(objects.size());
			}
			String name = new StringBuilder(UUID.randomUUID().toString()).append(".xlsx").toString();
			File attachment = new File(this.directory, name);
			Excels.write(workbook, attachment);
			result.setFile(name);
			result.setSize(Files.getUnitSize(attachment.length()));
			result.setSpend(Dates.getUnitTime(System.currentTimeMillis() - timestamp));
		} finally {
			workbook.close();
		}
		return result;
	}

	/**
	 * 新增对象实体
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            对象实体参数
	 * @return 新增对象实体主键
	 */
	public Serializable add(Requester requester, Map<String, Object> parameters) {
		T entity = Beans.getInstance(this.getModel());
		this.initObject(requester, entity, parameters);
		return this.saveObject(requester, entity);
	}

	/**
	 * 删除对象
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            数据过滤参数
	 */
	public void delete(Requester requester, Map<String, Object> parameters) {
		Map<String, Object> effectives = this.getEffectiveParameters(parameters);
		if (!effectives.isEmpty()) {
			List<T> entities = this.getQuery(requester, effectives).list();
			for (int i = 0; i < entities.size(); i++) {
				this.deleteObject(requester, entities.get(i));
			}
		}
	}

	/**
	 * 修改对象实体
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            对象实体参数
	 */
	public void update(Requester requester, Map<String, Object> parameters) {
		String primary = this.getRepository().getPrimary();
		Object[] identifiers = Beans.toArray(Object.class, parameters.get(primary));
		if (identifiers.length > 0) {
			List<T> entities = this.getQuery(requester).or(primary, identifiers).list();
			for (int i = 0; i < entities.size(); i++) {
				T entity = entities.get(i);
				this.initObject(requester, entity, parameters);
				this.updateObject(requester, entity);
			}
		}
	}

	/**
	 * 统计数量
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 数量
	 */
	public int count(Requester requester, Map<String, Object> parameters) {
		Map<String, Object> effectives = this.getEffectiveParameters(parameters);
		effectives.remove(Query.PAGE);
		effectives.remove(Query.SIZE);
		return this.getQuery(requester, effectives).count();
	}

	/**
	 * 数据统计
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 统计数据列表
	 */
	public List<?> stats(Requester requester, Map<String, Object> parameters) {
		return this.getQuery(requester, parameters).stats();
	}

	/**
	 * 获取单个对象
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例
	 */
	public T object(Requester requester, Map<String, Object> parameters) {
		Map<String, Object> effectives = this.getEffectiveParameters(parameters);
		if (effectives.isEmpty()) {
			return null;
		}
		return this.getQuery(requester, effectives).single();
	}

	/**
	 * 获取对象列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 对象实例列表
	 */
	public List<T> objects(Requester requester, Map<String, Object> parameters) {
		return this.getQuery(requester, parameters).list();
	}

	/**
	 * 获取树对象列表
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            过滤参数
	 * @return 树对象实例列表
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<T> trees(Requester requester, Map<String, Object> parameters) {
		List<T> objects = this.getQuery(requester, parameters).list();
		return (List<T>) Repositories.mergeTrees((List<TreeModel>) objects);
	}

	/**
	 * 数据批量导入
	 * 
	 * @param requester
	 *            请求对象
	 * @param file
	 *            文件对象
	 * @param start
	 *            开始数据行（从0开始）
	 * @param parameters
	 *            请求参数
	 * @return 数据导入结果对象
	 * @throws Exception
	 *             操作异常
	 */
	public ImportService.Result import_(Requester requester, Nfile file, Integer start, Map<String, Object> parameters)
			throws Exception {
		return this.import_(requester, file, start, this.getExcelAdapter(), parameters);
	}

	/**
	 * 数据批量导出
	 * 
	 * @param requester
	 *            请求对象
	 * @param parameters
	 *            请求参数
	 * @return 导出结果
	 * @throws Exception
	 *             操作异常
	 */
	public ExportService.Result export(Requester requester, Map<String, Object> parameters) throws Exception {
		return this.export(requester, this.getExcelAdapter(), parameters);
	}

	/**
	 * 下载批量导入（失败）/导出文件
	 * 
	 * @param requester
	 *            请求对象
	 * @param name
	 *            文件名称
	 * @param parameters
	 *            请求参数
	 * @return 文件对象
	 */
	public Nfile file(Requester requester, String name, Map<String, Object> parameters) {
		return new Nfile(new File(this.directory, name));
	}

}
