package org.kisst.monkeysync;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.script.Config;
import org.kisst.script.Context;
import org.kisst.script.Script;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SyncCli {
    private static Logger logger= LogManager.getLogger();
    private static MonkeyLanguage lang=new MonkeyLanguage();

    public static void main(String... args) {
        Config cfg=new Config(lang);

        // dirty hack to see if default configuration is needed
        boolean configFound=false;
        for (int i=0; i<args.length; i++) {
            if (args[i].equals("-c")||args[i].equals("--config"))
                configFound=true;
            if (args[i].indexOf(",")>=0)
                break;
            if (! args[i].trim().startsWith("-"))
                break;
            if (cfg.parseOption(args, i))
                args[i]="";
        }
        String str=String.join(" ", args).trim();
        // load default config if not specified otherwise
        if (! configFound)
            cfg.props.loadProps(Paths.get("config/monkeysync.props"));

        if (str.length()==0)
            str=cfg.props.getString("script.cmd");
        Script script =lang.compile(cfg, str);
        logger.info("compiled to {}",script.toString());

        String schedule=cfg.props.getString("script.schedule",null);
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
        logger.info("Starting script at {} with interval {} seconds", start, period);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { script.run(); }
        }, start, period * 1000);
    }
}