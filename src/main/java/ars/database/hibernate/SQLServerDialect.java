package ars.database.hibernate;

import java.sql.Types;

/**
 * SQLServer数据库方言
 *
 * @author wuyongqiang
 */
public class SQLServerDialect extends org.hibernate.dialect.SQLServerDialect {

    public SQLServerDialect() {
        super();
        registerHibernateType(Types.NVARCHAR, "string");
        registerHibernateType(Types.LONGNVARCHAR, "string");
    }

}
