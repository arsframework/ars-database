package ars.database.repository;

import ars.invoke.request.RequestHandleException;

/**
 * 数据约束异常
 * 
 * @author yongqiangwu
 * 
 */
public class DataConstraintException extends RequestHandleException {
	private static final long serialVersionUID = 1L;

	public DataConstraintException(String message) {
		super(message);
	}

}
