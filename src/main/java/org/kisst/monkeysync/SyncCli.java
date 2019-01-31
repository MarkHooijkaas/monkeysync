package org.kisst.monkeysync;

import java.nio.file.Paths;

public class SyncCli {

    public static void main(String... args) {
        // dirty hack to see if default configuration is needed
        boolean configFound=false;
        for (String arg: args) {
            if (arg.equals("-c")||arg.equals("--config"))
                configFound=true;
            if (arg.indexOf(",")>=0)
                break;
        }
        // load default config if not specified otherwise
        if (!configFound)
            Env.loadProps(Paths.get("monkeysync.props"));
        Script script = new Script(String.join(" ", args));
        script.run();
    }
}