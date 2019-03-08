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
        private final Context compilationContext;
        public BasicStep(Context compilationContext) { this.compilationContext=compilationContext;}
        @Override public Context getCompilationContext() { return compilationContext;}
    }

    public static class Help extends BasicStep {
        public Help(Context ctx,  String[] args) { super(ctx);}
        @Override public void run(Context ctx) { ctx.getLanguage();}
    }
    public static class Echo extends BasicStep {
        private final String msg;
        public Echo(Context ctx,  String[] args) {
            super(ctx);
            this.msg=String.join(" ",args).substring(args[0].length());
        }
        @Override public void run(Context ctx) { System.out.println(msg);}
    }
}
