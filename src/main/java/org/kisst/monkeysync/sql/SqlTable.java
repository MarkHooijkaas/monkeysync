package org.kisst.monkeysync.sql;

import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.map.MapTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.LinkedHashMap;


public class SqlTable extends MapTable {

    public SqlTable(Props props) {
        String sql=props.getString("sql");
        if (sql==null) {
            try {
                sql = new String(Files.readAllBytes(Paths.get(props.getString("sqlfile"))));
            } catch (IOException e) { throw new RuntimeException(e);}
        }
        try (Connection conn = connect(props)){
            ResultSet rs = conn.createStatement().executeQuery(sql);
            ResultSetMetaData columns = rs.getMetaData();
            int nrofColumns = columns.getColumnCount();
            while (rs.next()) {
                LinkedHashMap<String, String> map= new LinkedHashMap<>();
                //Record rec=new Record();
                for (int i = 1; i < nrofColumns; i++) {
                    String colname=columns.getColumnName(i);
                    String value=rs.getString(i);
                    //System.out.println(colname+i+value);
                    map.put(colname, value);
                }
                String key=rs.getString(1);
                create(new Record(key, map));
            }
        }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Connection connect(Props props) {
        try {
            Class.forName(props.getString("class"));

            String user=props.getString("user");
            String password=props.getString("password");
            String url = props.getString("url");
            return DriverManager.getConnection(url, user, password);
        }
        catch (ClassNotFoundException e) { throw new RuntimeException(e);}
        catch (SQLException e) { throw new RuntimeException(e);}
    }

}
