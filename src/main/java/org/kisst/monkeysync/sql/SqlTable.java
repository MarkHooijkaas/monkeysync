package org.kisst.monkeysync.sql;

import org.kisst.monkeysync.map.MapRecord;
import org.kisst.monkeysync.map.MapTable;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Properties;

public class SqlTable extends MapTable {

    public SqlTable(Properties props, String sql) {
        try (Connection conn = connect(props)){
            ResultSet rs = conn.createStatement().executeQuery(sql);
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

    private Connection connect(Properties props) {
        try {
            Class.forName(props.getProperty("class"));

            String user=props.getProperty("user");
            String password=props.getProperty("password");
            String url = props.getProperty("url");
            return DriverManager.getConnection(url, user, password);
        }
        catch (ClassNotFoundException e) { throw new RuntimeException(e);}
        catch (SQLException e) { throw new RuntimeException(e);}
    }

}
