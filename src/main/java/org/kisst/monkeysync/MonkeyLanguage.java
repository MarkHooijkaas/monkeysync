package org.kisst.monkeysync;

import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;
import org.kisst.script.Context;
import org.kisst.script.GenericCommand;
import org.kisst.script.BasicLanguage;
import org.kisst.script.Script;

public class MonkeyLanguage extends BasicLanguage {
    public MonkeyLanguage(Command ... cmds) {
        super( join(cmds,
                new GenericCommand(Save.class),
                new GenericCommand(Load.class),
                new GenericCommand(Fetch.class),
                new GenericCommand(Sync.class),
                new GenericCommand(SyncCreate.class),
                new GenericCommand(SyncUpdates.class),
                new GenericCommand(SyncDeletes.class),
                new GenericCommand(Mail.class)
        ));
    }

    public abstract static class TableStep implements Script.Step {
        private final String tblname;
        private final String line;
        public TableStep(Context ctx,  String[] args) {
            if (args.length < 2)
                throw new IllegalArgumentException(args[0]+" should have at least 1 parameter <table> [<file>]");
            this.tblname = args[1];
            this.line=String.join(" ",args);
        }
        protected Table getTable(Context ctx){ return (Table) ctx.getVar(tblname);}
        protected Table createTable(Context ctx){ return MonkeyLanguage.createTable(ctx,tblname);}
        @Override public String toString() { return line;}
    }

    public static class Save extends TableStep {
        private final String file;
        public Save(Context ctx, String[] args) {
            super(ctx, args);
            if (args.length>2)
                this.file=args[2];
            else
                this.file=ctx.props.getString("file",null);
        }
        @Override public void run(Context ctx) {
            Table tbl = getTable(ctx);
            if (file==null)
                tbl.save();
            else
                tbl.save(file);
        }
    }

    public static class Load extends TableStep  {
        private final String file;
        public Load(Context ctx, String[] args) {
            super(ctx, args);
            if (args.length > 2)
                this.file = args[2];
            else
                this.file = ctx.props.getString("file", null);
        }

        @Override public void run(Context ctx) {
            Table tbl = createTable(ctx);
            if (file == null)
                tbl.load();
            else
                tbl.load(file);
        }
    }
    public static class Fetch extends TableStep {
        public Fetch(Context ctx, String[] args) { super(ctx, args);}
        @Override public void run(Context ctx) {
            createTable(ctx).fetch(ctx);
        }
    }

    public abstract static class SyncStep extends TableStep {
        private final String dest;
        public SyncStep(Context ctx, String[] args) {
            super(ctx, args);
            if (args.length!=3)
                throw new IllegalArgumentException(args[0]+" should have exactly two parameters <src> <dest>");
            this.dest=args[2];
        }
        protected Table getDest(Context ctx){ return (Table) ctx.getVar(dest);}
    }

    public static class Sync extends SyncStep {
        public Sync(Context ctx, String[] args) { super(ctx, args);}
        @Override public void run(Context ctx) {
            new Syncer().syncAll(getTable(ctx), getDest(ctx));
        }
    }

    public static class SyncUpdates extends SyncStep {
        public SyncUpdates(Context ctx, String[] args) { super(ctx, args);}
        @Override public void run(Context ctx) {
            new Syncer().updateRecords(getTable(ctx), getDest(ctx));
        }
    }
    public static class SyncCreate extends SyncStep {
        public SyncCreate(Context ctx, String[] args) { super(ctx, args);}
        @Override public void run(Context ctx) {
            new Syncer().createNewRecords(getTable(ctx), getDest(ctx));
        }
    }
    public static class SyncDeletes extends SyncStep {
        public SyncDeletes(Context ctx, String[] args) { super(ctx, args);}
        @Override public void run(Context ctx) {
            new Syncer().deleteInactiveRecords(getTable(ctx), getDest(ctx));
        }
    }

    public static class Mail implements Script.Step {
        private final String name;
        public Mail(Context ctx,  String[] args) {
            if (args.length != 2)
                throw new IllegalArgumentException(args[0]+" should have 1 parameter <mailcfg>");
            this.name = args[1];
        }
        @Override public void run(Context ctx) {
            Mailer.send(ctx, ctx.props.getProps(name));
        }
    }

    static public Table createTable(Context ctx, String name) {
        Props tblprops=ctx.props.getProps(name);
        String type=tblprops.getString("type");
        Table result;
        if ("SqlTable".equals(type))
            result=new SqlTable(ctx, tblprops);
        else if ("MailchimpTable".equals(type))
            result=new MailchimpTable(tblprops);
        else if ("MapTable".equals(type))
            result=new MapTable(tblprops);
        else
            throw new RuntimeException("Unknown table type "+type);
        ctx.setVar(name, result);
        return result;
    }
}
