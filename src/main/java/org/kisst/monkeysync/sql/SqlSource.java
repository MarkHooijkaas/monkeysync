package org.kisst.monkeysync.sql;

import org.kisst.monkeysync.map.MapRecord;
import org.kisst.monkeysync.map.MapSource;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public class SqlSource extends MapSource {

    public SqlSource(ResultSet rs) {
        try {
            ResultSetMetaData columns = rs.getMetaData();
            int nrofColumns = columns.getColumnCount();
            while (rs.next()) {
                LinkedHashMap<String, String> map= new LinkedHashMap<>();
                //MapRecord rec=new MapRecord();
                for (int i = 1; i < nrofColumns; i++) {
                    String colname=columns.getColumnName(i);
                    String value=rs.getString(i);
                    //System.out.println(colname+i+value);
                    map.put(colname, value);
                }
                String key=rs.getString(1);
                create(new MapRecord(key, map));
            }
        }
        catch (SQLException e) { throw new RuntimeException(e); }
    }
}
