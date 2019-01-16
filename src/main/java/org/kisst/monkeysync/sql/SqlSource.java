package org.kisst.monkeysync.sql;

import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.RecordSource;
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
                for (int i = 0; i < nrofColumns; i++)
                    map.put(columns.getColumnName(i), rs.getString(i));
            }

            ResultSetMetaData rsmd = rs.getMetaData();
            String name = rsmd.getColumnName(1);
        }
        catch (SQLException e) { throw new RuntimeException(e); }
    }
}
