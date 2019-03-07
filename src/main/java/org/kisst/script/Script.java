package org.kisst.script;

import java.util.Arrays;
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

    public void run() { run(new Context(compileContext,"run")); }
    public void run(Context parent) {
        for (Step s: steps) {
            Context ctx=parent.createSubContext(s.toString());
            BufferAppender.pushContext(ctx);
            try {
                ctx.info("*** {}", s);
                s.run(ctx);
            }
            finally { BufferAppender.popContext();}
        }
    }

    @Override public String toString() {
        return Arrays.toString(steps);
    }

    private void tryStep(Context ctx, Step step) {
        int tries= ctx.props.getInt("tries",1);
        if (tries==1) {
            step.run(ctx);
            return;
        }
        int tryCount=0;

        long retryInterval=ctx.props.getInt("retryInterval",0);
        boolean succeeded=false;
        while (tryCount<tries && ! succeeded) {
            try {
                step.run(ctx);
                succeeded = true;
            } catch (RuntimeException e) {
                tryCount++;
                if (tryCount>=tries)
                    throw e;
                ctx.warn("Retrying {} because it failed with error {} ",step,e);
                if (retryInterval>0) {
                    ctx.info("sleeping {} second before retry", retryInterval/1000);
                    ctx.sleep(retryInterval);
                }
            }
        }
    }
}
