package org.kisst.script;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class Script {
    private static final Logger logger= LogManager.getLogger();

    public interface Step {
        public Config getConfig();
        public void run(Context ctx);
    }

    private final Config config;
    private final Step[] steps;

    public Script(Config config,List<Step> lst) {
        this.config=config;
        this.steps=new Step[lst.size()];
        int i=0;
        for (Step s: lst)
            steps[i++]=s;
    }
    public Script(Config config,Step... steps) {
        this.config=config;
        this.steps = steps;
    }

    public void run() {
        Context ctx=new Context();
        Context.pushContext(ctx);
        try {
            for (Step s : steps) {

                ctx.startStep(s);
                logger.info("*** {}", s);
                s.run(ctx);
            }
        }
        finally { Context.popContext();}
    }

    @Override public String toString() {
        return Arrays.toString(steps);
    }

    private void tryStep(Context ctx, Step step) {
        int tries= step.getConfig().props.getInt("tries",1);
        if (tries==1) {
            step.run(ctx);
            return;
        }
        int tryCount=0;

        long retryInterval=step.getConfig().props.getInt("retryInterval",0);
        boolean succeeded=false;
        while (tryCount<tries && ! succeeded) {
            try {
                step.run(ctx);
                succeeded = true;
            } catch (RuntimeException e) {
                tryCount++;
                if (tryCount>=tries)
                    throw e;
                logger.warn("Retrying {} because it failed with error {} ",step,e);
                if (retryInterval>0) {
                    logger.info("sleeping {} second before retry", retryInterval/1000);
                    ctx.sleep(retryInterval);
                }
            }
        }
    }
}
