package org.kisst.monkeysync;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;

public class Props {
    protected final LinkedHashMap<String,String> props=new LinkedHashMap<>();

    public String get(String key) {  return props.get(key);}

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
            result.add(values.trim());
        return result;
    }

    public Set<String> keySet() { return props.keySet(); }

    public Props getProps(String prefix) {
        if (! prefix.endsWith("."))
            prefix+=".";
        Props result=new Props();
        for (String key: keySet()) {
            if (key.startsWith(prefix))
                result.props.put(key.substring(prefix.length()), get(key));
        }
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (String key:props.keySet())
            result.append(key+" = "+props.get(key)+"\n");
        return result.toString();
    }

    public void loadProps(Path path) {
        try {
            Properties p = new Properties();
            p.load(new FileReader(path.toFile()));
            for (Object key: p.keySet())
                this.props.put((String) key, substitute(p.getProperty((String)key)));
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }

    public void parseLine(String line) {
        int pos=line.indexOf("=");
        if (pos>0) {
            String key=line.substring(0,pos);
            String value=line.substring(pos+1);
            props.put(key,value);
        }
    }
    public void clearProp(String name) { props.remove(name); }

    public String substitute(String str) {
        StringBuilder result = new StringBuilder();
        int prevpos=0;
        int pos=str.indexOf("${");
        while (pos>=0) {
            int pos2=str.indexOf("}", pos);
            if (pos2<0)
                throw new RuntimeException("Unbounded ${ starting with "+str.substring(pos,pos+10));
            String key=str.substring(pos+2,pos2);
            result.append(str.substring(prevpos,pos));
            String value=this.getString(key,null);
            if (value==null && key.equals("dollar"))
                value="$";
            if (value==null && key.startsWith("env."))
                value=System.getenv(key.substring(4));
            if (value==null)
                throw new RuntimeException("Unknown variable ${"+key+"}");
            result.append(value.toString());
            prevpos=pos2+1;
            pos=str.indexOf("${",prevpos);
        }
        result.append(str.substring(prevpos));
        return result.toString();
    }
}
