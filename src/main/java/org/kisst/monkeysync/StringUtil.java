package org.kisst.monkeysync;

import java.util.Map;

public class StringUtil {
    static public String substitute(String str, Map<String,?>... maps) {
        StringBuilder result = new StringBuilder();
        int prevpos=0;
        int pos=str.indexOf("${");
        while (pos>=0) {
            int pos2=str.indexOf("}", pos);
            if (pos2<0)
                throw new RuntimeException("Unbounded ${ starting with "+str.substring(pos,pos+10));
            String key=str.substring(pos+2,pos2);
            String defaultValue=null;
            int posColon=key.indexOf(':');
            if (posColon>0) {
                defaultValue=key.substring(posColon+1);
                key=key.substring(0,posColon);
            }
            result.append(str.substring(prevpos,pos));
            String value=null;
            for (Map<String,?> m: maps) {
                Object o = m.get(key);
                if (o!=null) {
                    value = o.toString();
                    continue;
                }
            }
            if (value==null && key.equals("dollar"))
                value="$";
            if (value==null && key.startsWith("env."))
                value=System.getenv(key.substring(4));
            if (value==null)
                value=defaultValue;
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
