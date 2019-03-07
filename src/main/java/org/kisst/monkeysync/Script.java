package org.kisst.monkeysync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TimerTask;
import java.util.stream.Stream;

public class Script extends TimerTask {
    private final Stream<String> lines;

    public Script(Path path) {
        try {
            lines = Files.lines(path);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public Script(String str) {
        String[] lines=str.split(",");
        this.lines=Stream.of(lines);
    }

    public void run() {
        Env.clear();
        int linenr=0;
        lines.forEach(this::parse);
    }

    public void parse(String line) {
        line = line.trim();
        if (line.length() == 0 || line.startsWith("#"))
            return;
        String[] parts = line.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parseOption(parts, i))
                parts[i] = "";
        }
        Env.info("*** ", line.trim());
        // optons might be removed from the parts so reassemble the parts
        line = String.join(" ", parts);
        parts = line.trim().split("\\s+");
        tryCommand(parts);
    }

    private void tryCommand(String[] parts) {
        String cmd=parts[0].trim();
        int tries=Env.props.getInt("tries",Env.props.getInt(cmd+".tries",1));
        if (tries==1) {
            parseCommand(parts);
            return;
        }
        int tryCount=0;

        long retryInterval=Env.props.getInt("retryInterval",Env.props.getInt(cmd+".retryInterval",0));
        boolean succeeded=false;
        while (tryCount<tries && ! succeeded) {
            try {
                parseCommand(parts);
                succeeded = true;
            } catch (RuntimeException e) {
                tryCount++;
                if (tryCount>=tries)
                    throw e;
                Env.warn("Retrying "+cmd+" because it failed with error "+e.getMessage());
                if (retryInterval>0) {
                    try {
                        Env.info("sleeping before retry","");
                        Thread.sleep(retryInterval);
                    }
                    catch (InterruptedException e1) { throw new RuntimeException(e1);}
                }
            }
        }
    }

    private boolean parseOption(String[] parts, int i) {
        String option=parts[i];
        if ("-h".equals(option) || "--help".equals(option))
            showHelp();
        else if ("-V".equals(option) || "--version".equals(option))
            showVersion();
        else if ("-v".equals(option) || "--verbose".equals(option))
            Env.verbosity=2;
        else if ("-q".equals(option) || "--quiet".equals(option))
            Env.verbosity=0;
        else if ("-d".equals(option) || "--debug".equals(option))
            Env.verbosity=3;
        else if ("-a".equals(option) || "--ask".equals(option))
            Env.interactive=true;
        else if ("-r".equals(option) || "--dry-run".equals(option))
            Env.props.parseLine("sync.dryRun=true");
        else if ("-w".equals(option) || "--wet-run".equals(option))
            Env.props.parseLine("sync.dryRun=false");
        else if ("-c".equals(option) || "--config".equals(option)) {
            if (! "-".equals(parts[i+1]))
                Env.loadProps(Paths.get(parts[i+1]));
            parts[i+1]="";
        }
        else if ("-n".equals(option) || "--null".equals(option) || "--no".equals(option) ) {
            Env.props.clearProp((parts[i+1]));
            parts[i+1]="";
        }
        else if (option.startsWith("--") && option.indexOf("=")>0)
            Env.props.parseLine(option.substring(2));
        else
            return false;
        return true;
    }


    private void parseCommand(String[] parts) {
        for (int i=0; i<parts.length; i++)
            parts[i]=Env.substitute(parts[i]);
        String cmd=parts[0].trim();
        if ("save".equals(cmd)) {
            if (parts.length>2)
                Env.getTable(parts[1]).save(parts[2]);
            else
                Env.getTable(parts[1]).save();
        }
        else if ("load".equals(cmd)) {
            {
                if (parts.length>2)
                    Env.loadTable(parts[1],parts[2]);
                else
                    Env.loadTable(parts[1]);
            }
        }
        else if ("fetch".equals(cmd))
            Env.fetchTable(parts[1]);
        else if ("sync".equals(cmd))
            new Syncer().syncAll(Env.getTable(parts[1]),Env.getTable(parts[2]));
        else if ("syncCreate".equals(cmd))
            new Syncer().createNewRecords(Env.getTable(parts[1]),Env.getTable(parts[2]));
        else if ("syncUpdate".equals(cmd))
            new Syncer().updateRecords(Env.getTable(parts[1]),Env.getTable(parts[2]));
        else if ("syncDelete".equals(cmd))
            new Syncer().deleteInactiveRecords(Env.getTable(parts[1]),Env.getTable(parts[2]));
        else if ("syncDeleteMissing".equals(cmd))
            new Syncer().deleteMissingRecords(Env.getTable(parts[1]),Env.getTable(parts[2]));
        else if ("send".equals(cmd))
            Mail.send(Env.props.getProps(parts[1]));
        else if ("run".equals(cmd))
            new Script(Paths.get(parts[1])).run();
        else if ("echo".equals(cmd)) {
            parts[0]="";
            String line=String.join(" ",parts).trim();
            System.out.println(line);
        }
        else
            throw new RuntimeException("Unknown command "+cmd);
    }

    private void showVersion() {
        System.out.println("monkeysync version 1.0");
        System.exit(0);

    }
    private void showHelp() {
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
        System.exit(0);
    }

}
