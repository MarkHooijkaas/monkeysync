package org.kisst.monkeysync.sql;

import org.kisst.monkeysync.Env;
import org.kisst.monkeysync.Syncer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Script {
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
        int linenr=0;
        lines.forEach(this::parse);
    }

    public void parse(String line) {
        line=line.trim();
        if (line.length()==0 || line.startsWith("#"))
            return;
        String[] parts = line.split("\\s+");
        for (int i=0; i<parts.length; i++) {
            if (parseOption(parts,i))
                parts[i] = "";
        }
        Env.info("*** ",line.trim());
       // optons might be removed from the parts so reassemble the parts
        line=String.join(" ",parts);
        parts=line.trim().split("\\s+");
        parseCommands(parts);
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
        else if ("-a".equals(option) || "--ask".equals(option))
            Env.interactive=true;
        else if ("-c".equals(option) || "--config".equals(option)) {
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


    private void parseCommands(String[] parts) {
        for (int i=0; i<parts.length; i++)
            parts[i]=substitute(parts[i]);
        String cmd=parts[0].trim();
        if ("save".equals(cmd))
            Env.getTable(parts[1]).save(parts[2]);
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
        else if ("run".equals(cmd))
            new Script(Paths.get(parts[1])).run();
        else if ("echo".equals(cmd)) {
            parts[0]="";
            System.out.println(Env.props.substitute(String.join(" ",parts)));
        }
        else
            throw new RuntimeException("Unknown command "+cmd);

    }
    private String substitute(String str) {return Env.props.substitute(str);}

    private void showVersion() {
        System.out.println("monkeysync version 0.1");
        System.exit(0);

    }
    private void showHelp() {
        System.out.println(
                "Usage: monkeysync <command> [<arg>|<option>]*\n" +
                "  -c, --config <file>  load a config file (multiple files are allowed)\n" +
                "  -h, --help           show this help message and exit\n" +
                "  -a, --ask            if set will ask before each update\n" +
                "  -v, --verbose        if set, will output details\n" +
                "  -q, --quiet          if set, no output will be printed\n" +
                "  -V, --version        print version information and exit\n" +
                "  -n, --no --null      clear a property set in the configuration\n" +
                "  --<prop>=<value>     set/override any property from the loaded configuration\n" +
                "  echo ....            echoes text to the console\n" +
                "  run <file>           run a script from a file\n" +
                "  load <name> <file>   load a table from <file> and name it <name>\n" +
                "  save <name> <file>   save a table with <name> to <file> \n" +
                "  sync <name1> <name2> sync table <name1> to table <name2>\n" +
                "  syncCreate ...       sync only new records\n" +
                "  syncUpdate ...       sync only existing records\n" +
                "  syncDelete ...       sync only records that are marked as deleted in src\n" +
                "  syncDeleteMissing ...sync only records that are missing in src\n");
        System.exit(0);
    }

}
