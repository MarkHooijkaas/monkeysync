package org.kisst.script;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.PropsLayer;
import org.kisst.monkeysync.StringUtil;

import java.util.HashMap;

public class Context {
    public final static Level VERBOSE = Level.forName("VERBOSE", 350);

    public final Props props;

    private static final Logger logger=LogManager.getLogger();
    private final HashMap<String, Object> vars=new HashMap<>();
    protected final Context parent;
    private Level verbosity=Level.INFO;
    private Level memoryLevel=Level.DEBUG;
    private final Language language;
    private final String name;

    public Context(Language language) {
        this.parent=null;
        this.language=language;
        this.props=new Props();
        this.name=language.getClass().getSimpleName();
    }
    public Context(Context parent, String name) {
        this.parent=parent;
        this.language=parent.language;
        this.props=new PropsLayer(parent.props);
        this.verbosity=parent.verbosity;
        this.name=name;
    }

    public static class SubContext extends Context {
        private SubContext(Context parent, String name) { super(parent, name); }
        @Override public void setVar(String name, Object val) { this.parent.setVar(name, val);}
    }
    public SubContext createSubContext(String name) { return new SubContext(this, name); }

    public String getName() { return name;}
    public String getFullName() {return parent==null? name: parent.getFullName()+"."+name; }
    public Object getVar(String name) { return vars.get(name);}
    public void setVar(String name, Object val) { vars.put(name, val);}

    public void warn(String msg, Object... params) {
        output(Level.WARN, msg,params);
    }
    public void info(String msg, Object... params) { output(Level.INFO, msg, params); }
    public void verbose(String msg, Object... params) { output(VERBOSE,msg, params);    }
    public void debug(String msg, Object... params) { output(Level.DEBUG, msg, params);
    }


    private void output(Level level, String msg, Object... parms) {
        if (true) //verbosity.isMoreSpecificThan(level)) // && memoryLevel<level)
            logger.log(level,msg, parms);
        //logger.printf(level, msg, parms);
        if (memoryLevel.isMoreSpecificThan(level)) {
            StringBuilder buffer=getBuffer(level);
            buffer.append(String.format(msg, parms)+"\n");
        }
    }

    private StringBuilder getBuffer(Level lvl) {
        String levelname=lvl.name()+"_BUFFER";
        StringBuilder result= (StringBuilder) getVar(levelname);
        if (result==null) {
            result=new StringBuilder();
            setVar(levelname, result);
        }
        return result;
    }

    public void setLoglevel(Level level) { this.verbosity=level; }

    public Language getLanguage() { return this.language; }

    public String substitute(String str) {
        return StringUtil.substitute(str, props.props, vars);
    }

    static public void sleep(long msecs) {
        try {
            Thread.sleep(msecs);
        }
        catch (InterruptedException e1) { throw new RuntimeException(e1);}
    }

    static public boolean ask(String s) {
        if (false) {//interactive) {
            System.out.println(s);
            String line=System.console().readLine();
            if (line.trim().toLowerCase().startsWith("n"))
                return false;
        }
        return true;
    }
}