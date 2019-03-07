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
    private int verbosity=Level.INFO.intLevel();
    private Level memoryLevel=Level.DEBUG;
    private final Language language;

    public Context(Language language) {
        this.language=language;
        this.props=new Props();
    }
    public Context(Context parent) {
        this.language=parent.language;
        this.props=new PropsLayer(parent.props);
        this.verbosity=parent.verbosity;
    }

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
        if (verbosity<level.intLevel()) // && memoryLevel<level)
            return;
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

    public void setLoglevel(Level level) { this.verbosity=level.intLevel(); }

    public Language getLanguage() { return this.language; }

    public String substitute(String str) {
        return StringUtil.substitute(str, props.props, vars);
    }

}