package org.kisst.monkeysync;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;

public class Props {
    public final LinkedHashMap<String,String> map =new LinkedHashMap<>();

    public String get(String key) {  return map.get(key);}

    public String getString(String key, String defaultValue) {
        String result = get(key);
        if (result==null)
            return defaultValue;
        return result;
    }
    public Boolean getBoolean(String key, boolean defaultValue) {
        String result = get(key);
        if (result==null)
            return defaultValue;
        return Boolean.parseBoolean(result);
    }
    public int getInt(String key, int defaultValue) {
        String result = get(key);
        if (result==null)
            return defaultValue;
        return Integer.parseInt(result);
    }

    public String getString(String key) {
        String result = get(key);
        if (result==null)
            throw new RuntimeException("Could not find string property "+key);
        return result;
    }
    public Boolean getBoolean(String key) {
        String result = get(key);
        if (result==null)
            throw new RuntimeException("Could not find boolean property "+key);
        return Boolean.parseBoolean(result);
    }
    public int getInt(String key) {
        String result = get(key);
        if (result==null)
            throw new RuntimeException("Could not find int property "+key);
        return Integer.parseInt(result);
    }


    public Set<String> getStringSet(String name) {
        HashSet<String> result=new HashSet<>();
        String values=getString(name, null);
        if (values==null)
            return result;
        String[] list=values.split(",");
        for (String value : list)
            result.add(value.trim());
        return result;
    }

    public String[] getStrings(String name) {
        String values=getString(name, null);
        if (values==null)
            throw new IllegalArgumentException("no property with name "+name);
        String[] list=values.split(",");
        for (int i=0; i<list.length; i++)
            list[i]=list[i].trim();
        return list;
    }
    public Set<String> keySet() { return map.keySet(); }

    public Props getProps(String prefix) {
        if (! prefix.endsWith("."))
            prefix+=".";
        Props result=new Props();
        for (String key: keySet()) {
            if (key.startsWith(prefix))
                result.map.put(key.substring(prefix.length()), get(key));
        }
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (String key: map.keySet())
            result.append(key+" = "+ map.get(key)+"\n");
        return result.toString();
    }

    public void loadProps(Path path) {
        try {
            Properties p = new Properties();
            p.load(new FileReader(path.toFile()));
            for (Object key: p.keySet())
                this.map.put((String) key, StringUtil.substitute(p.getProperty((String)key), map));
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }

    public void parseLine(String line) {
        int pos=line.indexOf("=");
        if (pos>0) {
            String key=line.substring(0,pos);
            String value=line.substring(pos+1);
            map.put(key,value);
        }
    }
    public void clearProp(String name) { map.remove(name); }

}
