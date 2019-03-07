package org.kisst.script;


public class BasicLanguage extends Language {
    public BasicLanguage(Command ... commands) {
        super( join(commands,
                new GenericCommand(Help.class),
                new GenericCommand(Echo.class)
                )
        );
    }

    public static class Help implements Script.Step {
        public Help(Context ctx,  String[] args) {}
        @Override public void run(Context ctx) { ctx.getLanguage();}
    }
    public static class Echo implements Script.Step {
        private final String msg;
        public Echo(Context ctx,  String[] args) {
            this.msg=String.join(" ",args).substring(args[0].length());
        }
        @Override public void run(Context ctx) { System.out.println(msg);}
    }
}
