package org.kisst.monkeysync;

import org.kisst.script.Context;
import org.kisst.script.Script;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SyncCli {
    private static MonkeyLanguage lang=new MonkeyLanguage();

    public static void main(String... args) {
        Context ctx=new Context(lang);

        // dirty hack to see if default configuration is needed
        boolean configFound=false;
        for (String arg: args) {
            if (arg.equals("-c")||arg.equals("--config"))
                configFound=true;
            if (arg.equals("-q")||arg.equals("--quiet"))
                Env.verbosity=0; // turn off logging to prevent showing the loading config message
            if (arg.indexOf(",")>=0)
                break;
        }
        String str=String.join(" ", args);
        // load default config if not specified otherwise
        if (!configFound)
            ctx.props.loadProps(Paths.get("config/monkeysync.props"));


        if (str.trim().length()==0)
            str=ctx.props.getString("script.cmd");
        Script script =lang.compile(ctx, str);
        ctx.info("compiled to {}",script.toString());

        String schedule=Env.props.getString("script.schedule",null);
        if (schedule==null)
            script.run();
        else
            schedule(script, schedule);
    }

    private static void schedule(Script script, String schedule) {
        Timer timer=new Timer("timer");
        LocalDateTime now= LocalDateTime.now();
        LocalDateTime ldt=now;
        ldt=ldt.minusSeconds(ldt.getSecond());
        long period=60*60;
        if (schedule.startsWith("daily:")) {
            period *=24;
            String[] time=schedule.substring(6).trim().split(":");
            int hour=Integer.parseInt(time[0]);
            int minutes=Integer.parseInt(time[1]);
            ldt=ldt.plusHours(hour-ldt.getHour());
            ldt=ldt.plusMinutes(minutes-ldt.getMinute());
        }
        if (schedule.startsWith("hourly:")) {
            int minutes=Integer.parseInt(schedule.substring(7).trim());
            ldt=ldt.plusMinutes(minutes-ldt.getMinute());
        }
        while (ldt.isBefore(now))
            ldt=ldt.plusSeconds(period);
        Date start = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        Env.info("Starting script at", start, "with interval", period, "seconds");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { script.run(); }
        }, start, period * 1000);
    }
}