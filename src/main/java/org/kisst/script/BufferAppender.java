package org.kisst.script;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name="BufferAppender", category="Core", elementType="appender", printObject=true)
public final class BufferAppender extends AbstractAppender {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    protected BufferAppender(String name,
                             Filter filter,
                             Layout<? extends Serializable> layout,
                             final boolean ignoreExceptions,
                             final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }


    @Override public void append(LogEvent event) {
        readLock.lock();
        try {
            final byte[] bytes = getLayout().toByteArray(event);
            Context ctx=Context.getThreadContext();
            Level level=event.getLevel();
            if (ctx!=null &&  ctx.bufferLevel.intLevel()>=level.intLevel())
                ctx.getBuffer(level).append(bytes);
            if (ctx!=null &&  ctx.getLoglevel().intLevel()<level.intLevel())
                return;
            //if (ctx!=null) System.out.print(ctx.getName()+": ");
            System.out.write(bytes);
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        } finally {
            readLock.unlock();
        }
    }

    public static BufferAppender instance=null;
    @PluginFactory
    public static BufferAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("Properties") final Property[] properties) {
            //@PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for BufferAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        instance=new BufferAppender(name, filter, layout, true, properties);
        return instance;
    }
}