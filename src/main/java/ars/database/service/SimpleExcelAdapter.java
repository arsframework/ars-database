package ars.database.service;

import java.sql.Date;
import java.lang.reflect.Field;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;

import ars.util.Beans;
import ars.file.office.Excels;
import ars.invoke.Invokes;
import ars.invoke.request.Requester;
import ars.database.service.Service;
import ars.database.service.ExcelAdapter;

/**
 * Excel数据对象适配简单实现
 * 
 * @author yongqiangwu
 * 
 * @param <T>
 *            数据模型
 */
public class SimpleExcelAdapter<T> implements ExcelAdapter<T> {
	private String[] dateFormats = new String[] { "yyyy-MM-dd", "yyyy/MM/dd" };

	public String[] getDateFormats() {
		return dateFormats;
	}

	public void setDateFormats(String... dateFormats) {
		this.dateFormats = dateFormats;
	}

	@Override
	public void begin(Requester requester, Service<T> service) {

	}

	@Override
	public String[] getTitles(Requester requester, Service<T> service) {
		return Invokes.getPropertyMessages(requester, service.getModel());
	}

	@Override
	public T read(Requester requester, Service<T> service, Row row) {
		int count = 0; // 设置属性个数
		T entity = Beans.getInstance(service.getModel());
		Field[] fields = Beans.getFields(service.getModel());
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			Cell cell = row.getCell(i);
			Class<?> type = field.getType();
			Object value = Date.class.isAssignableFrom(type) ? Excels.getDate(cell, this.dateFormats)
					: Excels.getValue(cell, type);
			if (value == null) {
				continue;
			}
			Beans.setValue(entity, field, value);
			count++;
		}
		return count == 0 ? null : entity;
	}

	@Override
	public void write(Requester requester, Service<T> service, T entity, Row row) {
		Excels.setObject(row, entity);
	}

	@Override
	public void complete(Requester requester, Service<T> service) {

	}

}
