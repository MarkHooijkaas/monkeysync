package org.kisst.script;


import java.util.List;

public class Script {
    public interface Step {
        public void run(Context ctx);
    }

    private final Context compileContext;
    private final Step[] steps;

    public Script(Context compileContext, List<Step> lst) {
        this.compileContext = compileContext;
        this.steps=new Step[lst.size()];
        int i=0;
        for (Step s: lst)
            steps[i++]=s;
    }
    public Script(Context compileContext, Step... steps) {
        this.compileContext=compileContext;
        this.steps = steps;
    }

    public void run() { run(new Context(compileContext)); }
    public void run(Context ctx) {
        for (Step s: steps) {
            s.run(ctx);
        }
    }

}
