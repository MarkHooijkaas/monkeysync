package org.kisst.monkeysync.sql;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.map.MapRecord;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.script.Config;
import org.kisst.script.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashSet;
import java.util.LinkedHashMap;


public class SqlTable extends MapTable {
    private static final Logger logger= LogManager.getLogger();

    private final Props dbprops;

    public SqlTable(Config cfg, Props props) {
        super(props);
        if (props.getString("db",null)!=null)
            dbprops= cfg.props.getProps(props.getString("db"));
        else
            dbprops=props;
    }

    @Override public void fetch() {
        final boolean trimValues=props.getBoolean("trimValues", true);

        String ignoreColumnsList=props.getString("ignoreColumns",null);
        HashSet<String> ignoreColumns=new HashSet<>();
        if (ignoreColumnsList!=null) {
            for (String col: ignoreColumnsList.split(",")) {
                if (col.trim().length()>0)
                    ignoreColumns.add(col.trim());
            }
        }

        String sql=props.getString("sql",null);
        if (sql==null) {
            try {
                sql = new String(Files.readAllBytes(Paths.get(props.getString("sqlfile"))));
            } catch (IOException e) { throw new RuntimeException(e);}
        }
        try (Connection conn = connect(dbprops)){
            ResultSet rs = conn.createStatement().executeQuery(sql);
            ResultSetMetaData columns = rs.getMetaData();
            int nrofColumns = columns.getColumnCount();
            while (rs.next()) {
                LinkedHashMap<String, String> map= new LinkedHashMap<>();
                //Record rec=new Record();
                for (int i = 1; i <= nrofColumns; i++) {
                    String colname=columns.getColumnName(i);
                    if (ignoreColumns.contains(colname))
                        continue;
                    String value=rs.getString(i);
                    if (trimValues && value!=null)
                        map.put(colname, value.trim());
                    else
                        map.put(colname, value);
                }
                String key=rs.getString(1);
                create(new MapRecord(key, map));
            }
        }
        catch (SQLException e) { throw new RuntimeException(e); }
        autoSave();
        logger.info("fetched {} records",records.size());
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
