package org.kisst.monkeysync;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Properties;

public class Props {
    private final LinkedHashMap<String,String> props=new LinkedHashMap<>();

    public String getString(String key, String defaultValue) {
        String result = props.get(key);
        if (result==null)
            return defaultValue;
        return result;
    }
    public Boolean getBoolean(String key, boolean defaultValue) {
        String result = props.get(key);
        if (result==null)
            return defaultValue;
        return Boolean.parseBoolean(result);
    }
    public int getInt(String key, int defaultValue) {
        String result = props.get(key);
        if (result==null)
            return defaultValue;
        return Integer.parseInt(result);
    }

    public String getString(String key) {
        String result = props.get(key);
        if (result==null)
            throw new RuntimeException("Could not find string property "+key);
        return result;
    }
    public Boolean getBoolean(String key) {
        String result = props.get(key);
        if (result==null)
            throw new RuntimeException("Could not find boolean property "+key);
        return Boolean.parseBoolean(result);
    }
    public int getInt(String key) {
        String result = props.get(key);
        if (result==null)
            throw new RuntimeException("Could not find int property "+key);
        return Integer.parseInt(result);
    }

    public Props  getProps(String prefix) {
        if (! prefix.endsWith("."))
            prefix+=".";
        Props result = new Props();
        for (String key:props.keySet()) {
            if (key.startsWith(prefix))
                result.props.put(key.substring(prefix.length()), props.get(key));
        }
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (String key:props.keySet())
            result.append(key+" = "+props.get(key)+"\n");
        return result.toString();
    }

    public void loadProps(File path) {
        try {
            Properties props = new Properties();
            props.load(new FileReader(path));
            for (Object key: props.keySet())
                this.props.put((String) key, props.getProperty((String)key));
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }
}
