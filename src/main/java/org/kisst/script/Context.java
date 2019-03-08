package org.kisst.script;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.monkeysync.StringUtil;

import java.util.HashMap;
import java.util.Stack;

public class Context {
    public final static Level VERBOSE = Level.forName("VERBOSE", 450);
    private static final Logger logger=LogManager.getLogger();

    private Script.Step currentStep;
    private final HashMap<String, Object> vars=new HashMap<>();

    public Object getVar(String name) { return vars.get(name); }
    public void setVar(String name, Object val) { vars.put(name, val);}

    public void startStep(Script.Step step) { this.currentStep=step;}
    public Script.Step getCurrentStep() { return currentStep;}
    public Config getCurrentConfig() {
        if (currentStep==null)
            return null; // TODO
        return currentStep.getConfig();
    }

    public StringBuilder getBuffer(Level lvl) {
        String levelname=lvl.name()+"_LOG";
        StringBuilder result= (StringBuilder) getVar(levelname);
        if (result==null) {
            result=new StringBuilder();
            setVar(levelname, result);
        }
        return result;
    }

    public String substitute(String str) {
        if (currentStep!=null)
            return StringUtil.substitute(str, currentStep.getConfig().props.props, vars);
        return StringUtil.substitute(str, vars);
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