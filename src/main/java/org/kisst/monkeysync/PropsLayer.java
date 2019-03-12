package org.kisst.monkeysync;

import java.util.HashSet;
import java.util.Set;

public class PropsLayer extends Props {
    private static final String NULL_STRING =new String("@NULL_STRING@");
    private final Props parent;

    public PropsLayer(Props parent) {
        assert (parent!=null);
        this.parent=parent;
    }

    @Override public String get(String key) {
        String result= map.get(key);
        if (result== NULL_STRING) // Hack to make it possible to clear a property set in the parent
            return null;
        if (result==null)
            return parent.get(key);
        return result;
    }

    @Override public Set<String> keySet() {
        Set<String> result=new HashSet<>(parent.keySet());
        for (String key : map.keySet()) {
            if (map.get(key) == NULL_STRING)
                result.remove(key);
            else
                result.add(key);
        }
        return result;
    }

    @Override public Props getProps(String prefix) {
        if (! prefix.endsWith("."))
            prefix+=".";
        Props result;
        result=parent.getProps(prefix);
        for (String key: keySet()) {
            if (key.startsWith(prefix))
                result.map.put(key.substring(prefix.length()), get(key));
        }
        return result;
    }

    @Override public void clearProp(String name) {
        if (parent.get(name)!=null)
            map.put(name, NULL_STRING); // Override the parent property
        else
            map.remove(name);
    }
}
