package org.kisst.monkeysync;

import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "monkeysync",  mixinStandardHelpOptions = true, version = "0.1")
public class SyncCli implements Runnable{

    @CommandLine.Option(names = {"-c", "--config"}, defaultValue = "monkeysync.props", description = "The config file.")
    String configFile;

    @CommandLine.Option(names = {"-s", "--src"}, defaultValue = "src", description = "The source.")
    String src;

    @CommandLine.Option(names = {"-d", "--dest"}, defaultValue = "dest", description = "The destination.")
    String dest;

    @CommandLine.Option(names = {"-w", "--write-dest-file"}, required=false, description = "The filename where to write destination.")
    String writeDestFile;

    @CommandLine.Option(names = {"-v", "--verbose"}, required=false, description = "if set, will output details.")
    boolean verbose;

    @CommandLine.Option(names = {"-i", "--interactive"}, required=false, description = "if set will ask before each update.")
    boolean interactive;

    public void run() {
        Props props=new Props();
        props.loadProps(new File(configFile));
        Syncer syncer=new Syncer();
        if (verbose)
            syncer.setVerbose(true);
        if (interactive)
            syncer.setInteractive(true);

        syncer.verbose("Loading source ",src);
        Table srcdb=syncer.getTable(props.getProps(src));
        syncer.info("Loaded "+srcdb.size()+" records from ", src);

        syncer.verbose("Loading destination ", dest);
        Table destdb=syncer.getTable(props.getProps(dest));
        syncer.info("Loaded "+destdb.size()+" records from ", dest);

        syncer.syncAll(srcdb,destdb);
    }

    public static void main(String... args) {
        CommandLine.run(new SyncCli(), System.err, args);
    }

}