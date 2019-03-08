package org.kisst.script;


public class BasicLanguage extends Language {
    public BasicLanguage(Command ... commands) {
        super( join(commands,
                new GenericCommand(Help.class),
                new GenericCommand(Echo.class)
                )
        );
    }

    public abstract static class BasicStep implements Script.Step {
        private final Config config;
        public BasicStep(Config cfg) { this.config=cfg;}
        @Override public Config getConfig() { return config;}
    }

    public static class Help extends BasicStep {
        public Help(Config cfg,  String[] args) { super(cfg);}
        @Override public void run(Context ctx) { getConfig().getLanguage().showHelp();}
    }
    public static class Echo extends BasicStep {
        private final String msg;
        public Echo(Config cfg,  String[] args) {
            super(cfg);
            this.msg=String.join(" ",args).substring(args[0].length());
        }
        @Override public void run(Context ctx) { System.out.println(msg);}
    }
}
