package org.kisst.monkeysync;

import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;
import org.kisst.script.*;

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
                new GenericCommand(Mailer.class, "send")
        ));
    }

    public abstract static class TableStep extends BasicStep {
        protected final String tblname;
        private final String line;
        public TableStep(Config cfg, String[] args) {
            super(cfg);
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
        public Save(Config cfg, String[] args) {
            super(cfg, args);
            if (args.length>2)
                this.file=args[2];
            else
                this.file=cfg.props.getString("file",null);
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
        public Load(Config cfg, String[] args) {
            super(cfg, args);
            if (args.length > 2)
                this.file = args[2];
            else
                this.file = cfg.props.getString("file", null);
        }
        @Override public void run(Context ctx) {
            Table tbl = createTable(ctx);
            if (file == null)
                tbl.load();
            else
                tbl.load(file);
            ctx.setVar(tblname, tbl);
        }
    }
    public static class Fetch extends TableStep {
        public Fetch(Config cfg, String[] args) { super(cfg, args);}
        @Override public void run(Context ctx) {
            Table tbl=createTable(ctx);
            tbl.fetch();
            ctx.setVar(tblname, tbl);
        }
    }

    public abstract static class SyncStep extends TableStep {
        private final String dest;
        public SyncStep(Config cfg, String[] args) {
            super(cfg, args);
            if (args.length!=3)
                throw new IllegalArgumentException(args[0]+" should have exactly two parameters <src> <dest>");
            this.dest=args[2];
        }
        protected Table getDest(Context ctx){ return (Table) ctx.getVar(dest);}
    }

    public static class Sync extends SyncStep {
        public Sync(Config cfg, String[] args) { super(cfg, args);}
        @Override public void run(Context ctx) {
            new Syncer(getConfig()).syncAll(getTable(ctx), getDest(ctx));
        }
    }

    public static class SyncUpdates extends SyncStep {
        public SyncUpdates(Config cfg, String[] args) { super(cfg, args);}
        @Override public void run(Context ctx) { new Syncer(getConfig()).updateRecords(getTable(ctx), getDest(ctx));
        }
    }
    public static class SyncCreate extends SyncStep {
        public SyncCreate(Config cfg, String[] args) { super(cfg, args);}
        @Override public void run(Context ctx) { new Syncer(getConfig()).createNewRecords(getTable(ctx), getDest(ctx)); }
    }
    public static class SyncDeletes extends SyncStep {
        public SyncDeletes(Config cfg, String[] args) { super(cfg, args);}
        @Override public void run(Context ctx) {
            new Syncer(getConfig()).deleteInactiveRecords(getTable(ctx), getDest(ctx));
        }
    }

    static public Table createTable(Context ctx, String name) {
        Props tblprops=ctx.getCurrentConfig().props.getProps(name);
        String type=tblprops.getString("type");
        Table result;
        if ("SqlTable".equals(type))
            result=new SqlTable(ctx.getCurrentConfig(),tblprops);
        else if ("MailchimpTable".equals(type))
            result=new MailchimpTable(tblprops);
        else if ("MapTable".equals(type))
            result=new MapTable(tblprops);
        else
            throw new RuntimeException("Unknown table type "+type);
        ctx.setVar(name, result);
        return result;
    }

    @Override public void showHelp() {
        System.out.println(
                "Usage: monkeysync <command> [<arg>|<option>]*\n" +
                "  -c, --config <file>  load a config file (multiple files are allowed, - can skip default configfile)\n" +
                "  -h, --help           show this help message and exit\n" +
                "  -v, --verbose        if set, will output details\n" +
                "  -d, --debug          if set, will output extra details\n" +
                "  -q, --quiet          if set, no output will be printed\n" +
                "  -V, --version        print version information and exit\n" +
                "  -n, --no --null      clear a property set in the configuration\n" +
                "  -r, --dry-run        do a dry(rehearsal) run without modifying anything\n" +
                "  -w, --wet-run        do a wet(write) run that modifies the destination table\n" +
                "  -a, --ask            if set will ask before each update\n" +
                "  --once, --now        run the script immediately, and ignore a script.schedule\n" +
                "  --<prop>=<value>     set/override any property from the loaded configuration\n" +
                "  echo ....            echoes text to the console\n" +
                "  run <file>           run a script from a file\n" +
                "  fetch <name>         fetch the data for a table with <name> from it's source (e.g. SQL or Mailchimp)\n" +
                "  load <name> [<file>] load a named table from <file>, or the default file\n" +
                "  save <name> [<file>] save a table with <name> to <file> or the default file\n" +
                "  sync <name1> <name2> sync table <name1> to table <name2>\n" +
                "  syncCreate ...       sync only new records\n" +
                "  syncUpdate ...       sync only existing records\n" +
                "  syncDelete ...       sync only records that are marked as deleted in src\n" +
                "  syncDeleteMissing ...sync only records that are missing in src\n" +
                "  send <name>          send email with settings from <name>\n");
        //super.showHelp();
    }
}
