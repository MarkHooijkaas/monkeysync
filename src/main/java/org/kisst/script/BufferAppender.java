package org.kisst.script;

import java.io.Serializable;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name="BufferAppender", category="Core", elementType="appender", printObject=true)
public final class BufferAppender extends AbstractAppender {
    private static ThreadLocal<Stack<Context>> contextStack = new ThreadLocal<>();
    public static void pushContext(Context ctx) {
        Stack<Context> stack=contextStack.get();
        if (stack==null) {
            stack = new Stack<>();
            contextStack.set(stack);
        }
        stack.push(ctx);
    }
    public static Context getContext() {
        Stack<Context> stack=contextStack.get();
        if (stack==null)
            return null;
        return stack.peek();
    }
    public static Context popContext() {
        Stack<Context> stack=contextStack.get();
        if (stack==null)
            return null;
        Context result = stack.peek();
        if (stack.empty())
            contextStack.remove();
        return result;
    }

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    protected BufferAppender(String name, Filter filter,
                             Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }


    @Override public void append(LogEvent event) {
        readLock.lock();
        try {
            final byte[] bytes = getLayout().toByteArray(event);
            Context ctx=getContext();
            if (ctx!=null)
                System.out.print(ctx.getName()+": ");
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
            @PluginElement("Filter") final Filter filter) {
            //@PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for BufferAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        instance=new BufferAppender(name, filter, layout, true);
        return instance;
    }
}