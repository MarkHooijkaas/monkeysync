package org.kisst.script;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

public class Language {
    public interface Command {
        Script.Step compile(Context ctx, String[] args);
        String getName();
        //String getHelp();
    }

    private final Language parent;
    private final HashMap<String, Command>  commands=new HashMap<>();

    public Language(Command ... commands) { this(null, commands); }
    public Language(Language parent, Command ... commands) {
        this.parent = parent;
        for (Command cmd: commands)
            this.commands.put(cmd.getName(), cmd);
    }


    public Script compile(Stream<String> lines){
        Context ctx=new Context();
        ArrayList<Script.Step> steps=new ArrayList<>();
        lines.forEach((line) -> {
            Script.Step step = parse(ctx, line);
            if (step!=null)
                steps.add(step);
        });
        return new Script(ctx, steps);
     }

    public Script compile(Path path) {
        try {
            return compile(Files.lines(path));
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public Script compile(String line) {
        String[] lines=line.split(",");
        return compile(Stream.of(lines));
    }

    public Script.Step parse(Context ctx, String line) {
        ctx.info("*** ", line.trim());
        line = line.trim();
        if (line.length() == 0 || line.startsWith("#"))
            return null;
        String[] parts = line.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parseOption(ctx, parts, i))
                parts[i] = "";
        }
        Command cmd=commands.get(parts[0]);
        if (cmd==null)
            throw new UnsupportedOperationException(parts[0]);
        return cmd.compile(ctx, parts);
    }

    private boolean parseOption(Context ctx, String[] parts, int i) {
        String option=parts[i];
        if ("-h".equals(option) || "--help".equals(option))
            showHelp();
        //else if ("-V".equals(option) || "--version".equals(option))
        //    showVersion();
        else if ("-v".equals(option) || "--verbose".equals(option))
            ctx.setLoglevel(Context.VERBOSE);
        else if ("-q".equals(option) || "--quiet".equals(option))
            ctx.setLoglevel(Level.WARN);
        else if ("-d".equals(option) || "--debug".equals(option))
            ctx.setLoglevel(Level.DEBUG);
        else if ("-c".equals(option) || "--config".equals(option)) {
            if (! "-".equals(parts[i+1]))
                ctx.props.loadProps(Paths.get(parts[i+1]));
            parts[i+1]="";
        }
        else if ("-n".equals(option) || "--null".equals(option) || "--no".equals(option) ) {
            ctx.props.clearProp((parts[i+1]));
            parts[i+1]="";
        }
        else if (option.startsWith("--") && option.indexOf("=")>0)
            ctx.props.parseLine(option.substring(2));
        else
            return false;
        return true;
    }


    private void showHelp() {
    }
}
