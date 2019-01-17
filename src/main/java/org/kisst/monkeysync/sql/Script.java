package org.kisst.monkeysync.sql;

import org.kisst.monkeysync.Env;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        lines.forEach(line -> {
            //linenr++;
            Env.info("running ",line);
        });
    }

}
