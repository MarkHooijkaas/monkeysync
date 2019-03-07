package org.kisst.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class GenericCommand<T extends Script.Step>  implements Language.Command {

    private final Class<T> clz;
    private final String name;

    public GenericCommand(Class<T> clz) {
        this.name=clz.getSimpleName().toLowerCase();
        this.clz=clz;
    }
    public GenericCommand(Class<T> clz, String name) {
        this.name=name;
        this.clz=clz;
    }

    @Override public String getName() { return name;}

    @Override public String getHelp() {
        return name+" ... TODO";
    }

    @Override public Script.Step parse(Context ctx, String[] args) {
        try {
            Constructor<T> cons= clz.getConstructor(Context.class, String[].class);
            return cons.newInstance(ctx, args);
        }
        catch (NoSuchMethodException e) { throw new RuntimeException(e); }
        catch (IllegalAccessException e) { throw new RuntimeException(e); }
        catch (InstantiationException e) { throw new RuntimeException(e); }
        catch (InvocationTargetException e) { throw new RuntimeException(e); }
    }

}
