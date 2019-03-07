package org.kisst.monkeysync;

import java.util.Set;

public class PropsLayer extends Props {
    private static final String NULL=new String("@NULL@");
    private final Props parent;

    public PropsLayer(Props parent) {
        assert (parent!=null);
        this.parent=parent;
    }

    @Override public String get(String key) {
        String result=props.get(key);
        if (result==NULL) // Hack to make it possible to clear a property set in the parent
            return null;
        if (result==null)
            return parent.get(key);
        return result;
    }

    @Override public Set<String> keySet() {
        Set<String> result=parent.keySet();
        for (String key : props.keySet()) {
            if (props.get(key) == NULL)
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
                result.props.put(key.substring(prefix.length()), get(key));
        }
        return result;
    }

    @Override public void clearProp(String name) {
        if (parent.get(name)!=null)
            props.put(name, NULL); // Override the parent property
        else
            props.remove(name);
    }
}
