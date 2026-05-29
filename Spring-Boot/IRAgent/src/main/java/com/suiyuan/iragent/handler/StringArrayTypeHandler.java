package com.suiyuan.iragent.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(String[].class)
@MappedJdbcTypes(JdbcType.ARRAY)
public class StringArrayTypeHandler extends BaseTypeHandler<String[]> {

    private static final String PG_TEXT_ARRAY = "text";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String[] parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf(PG_TEXT_ARRAY, parameter);
        ps.setArray(i, array);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        return toArray(array);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        return toArray(array);
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return toArray(array);
    }

    private String[] toArray(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        Object raw = array.getArray();
        if (raw instanceof String[]) {
            return (String[]) raw;
        }
        if (raw instanceof Object[]) {
            Object[] objArr = (Object[]) raw;
            String[] result = new String[objArr.length];
            for (int i = 0; i < objArr.length; i++) {
                result[i] = objArr[i] != null ? objArr[i].toString() : null;
            }
            return result;
        }
        return null;
    }
}
