package org.kisst.script;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.PropsLayer;
import org.kisst.monkeysync.StringUtil;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Stack;

public class Context {
    public final static Level VERBOSE = Level.forName("VERBOSE", 450);

    public final Props props;

    private static final Logger logger=LogManager.getLogger();
    private final HashMap<String, Object> vars;
    protected final Context parent;
    private Level verbosity=Level.INFO;
    public Level bufferLevel=Level.DEBUG;
    public boolean dryRun=true;
    private final Language language;
    private final String name;

    public Context(Language language) {
        this.parent=null;
        this.language=language;
        this.props=new Props();
        this.name=language.getClass().getSimpleName();
        this.vars=new HashMap<>();
    }
    public Context(Context parent, String name) {
        this(parent, new HashMap<>(),name);
    }

    public Context(Context parent, HashMap<String, Object> vars, String name) {
        this.parent=parent;
        this.language=parent.language;
        this.props=new PropsLayer(parent.props);
        this.verbosity=parent.verbosity;
        this.vars=vars;
        this.name=name;
        this.dryRun=parent.dryRun;
    }


    public static class SubContext extends Context {
        private SubContext(Context parent, String name) { super(parent, name); }
        @Override public void setVar(String name, Object val) { this.parent.setVar(name, val);}
    }
    public SubContext createSubContext(String name) { return new SubContext(this, name); }
    public Context createRunContext(Context runtime) {
        return new Context(this, runtime.vars, "run("+name+")");
    }

    public String getName() { return name;}
    public String getFullName() {return parent==null? name: parent.getFullName()+"."+name; }
    public Object getVar(String name) {
        Object result=vars.get(name);
        if (result==null && parent!=null)
            result=parent.getVar(name);
        return result;
    }
    public void setVar(String name, Object val) { vars.put(name, val);}

    public StringBuilder getBuffer(Level lvl) {
        String levelname=lvl.name()+"_LOG";
        StringBuilder result= (StringBuilder) getVar(levelname);
        if (result==null) {
            result=new StringBuilder();
            setVar(levelname, result);
        }
        return result;
    }

    public Level getLoglevel() { return this.verbosity; }
    public void setLoglevel(Level level) { this.verbosity=level; }
    public Language getLanguage() { return this.language; }
    public String substitute(String str) {
        return StringUtil.substitute(str, props.props, vars);
    }

    public boolean parseOption(String[] parts, int i) {
        String option=parts[i];
        if ("-v".equals(option) || "--verbose".equals(option))
            setLoglevel(Context.VERBOSE);
        else if ("-q".equals(option) || "--quiet".equals(option))
            setLoglevel(Level.WARN);
        else if ("-d".equals(option) || "--debug".equals(option))
            setLoglevel(Level.DEBUG);
        else if ("-c".equals(option) || "--config".equals(option)) {
            if (! "-".equals(parts[i+1]))
                props.loadProps(Paths.get(parts[i+1]));
            parts[i+1]="";
        }
        else if ("-n".equals(option) || "--null".equals(option) || "--no".equals(option) ) {
            props.clearProp((parts[i+1]));
            parts[i+1]="";
        }
        else if (option.startsWith("--") && option.indexOf("=")>0)
            props.parseLine(option.substring(2));
        else
            return false;
        return true;
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

    private static ThreadLocal<Stack<Context>> contextStack = new ThreadLocal<>();
    public static void pushContext(Context ctx) {
        Stack<Context> stack=contextStack.get();
        if (stack==null) {
            stack = new Stack<>();
            contextStack.set(stack);
        }
        stack.push(ctx);
    }
    public static Context getThreadContext() {
        Stack<Context> stack=contextStack.get();
        if (stack==null)
            return null;
        return stack.peek();
    }
    public static Context popContext() {
        Stack<Context> stack=contextStack.get();
        if (stack==null)
            return null;
        Context result = stack.peek();
        if (stack.empty())
            contextStack.remove();
        return result;
    }
}