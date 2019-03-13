package org.kisst.script;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.PropsLayer;
import org.kisst.monkeysync.StringUtil;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Stack;

public class Config {
    private static final Logger logger=LogManager.getLogger();

    public final Props props;
    protected final Config parent;
    private Level verbosity=Level.INFO;
    public Level bufferLevel=Level.DEBUG;
    private final Language language;
    private final String name;

    public Config(Language language) {
        this.parent=null;
        this.language=language;
        this.props=new Props();
        this.name=language.getClass().getSimpleName();
    }
    public Config(Config parent, String name) {
        this.parent=parent;
        this.language=parent.language;
        this.props=new PropsLayer(parent.props);
        for (String key: parent.props.keySet()) {
            if (key.startsWith(name+".")) {
                String subkey = key.substring(name.length() + 1);
                String value = parent.props.get(key);
                this.props.put(subkey, value);
                logger.debug("Setting subkey {} for command {} to {} ",subkey,name, value);
            }
        }
        this.verbosity=parent.verbosity;
        this.name=name;
    }


    public String getName() { return name;}
    public String getFullName() {return parent==null? name: parent.getFullName()+"."+name; }
    @Override public String toString() {return getFullName(); }

    public Level getLoglevel() { return this.verbosity; }
    public void setLoglevel(Level level) { this.verbosity=level; }
    public Language getLanguage() { return this.language; }

    public boolean parseOption(String[] parts, int i) {
        String option=parts[i];
        if ("-v".equals(option) || "--verbose".equals(option))
            setLoglevel(Context.VERBOSE);
        else if ("-q".equals(option) || "--quiet".equals(option))
            setLoglevel(Level.WARN);
        else if ("-d".equals(option) || "--debug".equals(option))
            setLoglevel(Level.DEBUG);
        else if ("-w".equals(option) || "--wet-run".equals(option))
            props.parseLine("dryRun=false");
        else if ("-r".equals(option) || "--dry-run".equals(option))
            props.parseLine("dryRun=true");
        else if ("-c".equals(option) || "--config".equals(option)) {
            if (! "-".equals(parts[i+1]))
                props.loadProps(Paths.get(parts[i+1]));
            parts[i+1]="";
        }
        else if ("-n".equals(option) || "--null".equals(option) || "--no".equals(option) ) {
            props.clearProp((parts[i+1]));
            parts[i+1]="";
        }
        else if (option.startsWith("--") && option.indexOf("=")>0)
            props.parseLine(option.substring(2));
        else
            return false;
        return true;
    }

    static public boolean ask(String s) {
        if (false) {//interactive) {
            System.out.println(s);
            String line=System.console().readLine();
            if (line.trim().toLowerCase().startsWith("n"))
                return false;
        }
        return true;
    }
}