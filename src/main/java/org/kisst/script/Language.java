package org.kisst.script;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

public class Language {
    private static final Logger logger= LogManager.getLogger();

    public interface Command {
        Script.Step parse(Context ctx, String[] args);
        String getName();
        String getHelp();
    }

    private final Language parent;
    private final LinkedHashMap<String, Command>  commands=new LinkedHashMap<>();

    public Language(Command ... commands) { this(null, commands); }
    public Language(Language parent, Command ... commands) {
        this.parent = parent;
        for (Command cmd: commands)
            this.commands.put(cmd.getName(), cmd);
    }


    public Script compile(Context parent, Stream<String> lines){
        Context ctx;
        if (parent==null)
            ctx=new Context(this);
        else
            ctx=new Context(parent,"compile");
        Context.pushContext(ctx);
        try {
            ArrayList<Script.Step> steps = new ArrayList<>();
            lines.forEach((line) -> {
                Script.Step step = parse(ctx, line);
                if (step != null)
                    steps.add(step);
            });
            return new Script(ctx, steps);
        }
        finally { Context.popContext();}
     }

    public Script compile(Context parent, Path path) {
        try {
            return compile(parent, Files.lines(path));
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public Script compile(Context ctx, String line) {
        String[] lines=line.split(",");
        return compile(ctx, Stream.of(lines));
    }

    public Script.Step parse(Context parent, String line) {
        Context ctx=parent.createSubContext(line.trim());
        Context.pushContext(ctx);
        try {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#"))
                return null;
            logger.info("compiling {}", line);
            String[] parts = line.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if (parseOption(ctx, parts, i))
                    parts[i] = "";
            }
            Command cmd = commands.get(parts[0]);
            if (cmd == null)
                throw new UnsupportedOperationException(parts[0]);
            parts = String.join(" ", parts).split("\\s+");
            return cmd.parse(ctx, parts);
        }
        finally { Context.popContext(); }
    }

    private boolean parseOption(Context ctx, String[] parts, int i) {
        if (ctx.parseOption(parts,i))
            return true;
        String option=parts[i];
        if ("-h".equals(option) || "--help".equals(option))
            showHelp();
        //else if ("-V".equals(option) || "--version".equals(option))
        //    showVersion();
        else
            return false;
        return true;
    }


    public void showHelp() {
        for (Command cmd: commands.values())
            System.out.println(cmd.getHelp());
    }

    /**
     *     Helper method to extend languages using super constructor
     *     The newCommands are of the Child class
     *     The thisCommands are in the super class, but must be last parameter
     *
     */
    public static Command[] join(Command[] newCommands, Command... thisCommands) {
        Command[] result=new Command[thisCommands.length + newCommands.length];
        int i=0;
        for (Command cmd : thisCommands)
            result[i++]=cmd;
        for (Command cmd : newCommands)
            result[i++]=cmd;
        return result;
    }

}
