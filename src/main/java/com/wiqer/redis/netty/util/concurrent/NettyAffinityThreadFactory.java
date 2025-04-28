package com.wiqer.redis.netty.util.concurrent;

import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import net.openhft.affinity.AffinityStrategy;
import net.openhft.affinity.AffinityThreadFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyAffinityThreadFactory extends AffinityThreadFactory {
    private static final AtomicInteger poolId = new AtomicInteger();
    private AtomicInteger nextId;
    private String prefix;
    private boolean daemon;
    private int priority;
    protected ThreadGroup threadGroup;

    public NettyAffinityThreadFactory(String name, AffinityStrategy... strategies) {
        super(name, strategies);
    }

    public NettyAffinityThreadFactory(String name, boolean daemon, @NotNull AffinityStrategy... strategies) {
        super(name, daemon, strategies);
    }

    public NettyAffinityThreadFactory(Class<?> poolType) {
        this((Class) poolType, false, 5);
    }

    public NettyAffinityThreadFactory(String poolName) {
        this((String) poolName, false, 5);
    }

    public NettyAffinityThreadFactory(Class<?> poolType, boolean daemon) {
        this((Class) poolType, daemon, 5);
    }

    public NettyAffinityThreadFactory(String poolName, boolean daemon) {
        this((String) poolName, daemon, 5);
    }

    public NettyAffinityThreadFactory(Class<?> poolType, int priority) {
        this(poolType, false, priority);
    }

    public NettyAffinityThreadFactory(String poolName, int priority) {
        this(poolName, false, priority);
    }

    public NettyAffinityThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        this(toPoolName(poolType), daemon, priority);
    }

    public static String toPoolName(Class<?> poolType) {
        ObjectUtil.checkNotNull(poolType, "poolType");
        String poolName = StringUtil.simpleClassName(poolType);
        switch (poolName.length()) {
            case 0:
                return "unknown";
            case 1:
                return poolName.toLowerCase(Locale.US);
            default:
                return Character.isUpperCase(poolName.charAt(0)) && Character.isLowerCase(poolName.charAt(1)) ? Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1) : poolName;
        }
    }

    public NettyAffinityThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        super(poolName, daemon);
        this.nextId = new AtomicInteger();
        ObjectUtil.checkNotNull(poolName, "poolName");
        if (priority >= 1 && priority <= 10) {
            this.prefix = poolName + '-' + poolId.incrementAndGet() + '-';
            this.daemon = daemon;
            this.priority = priority;
            this.threadGroup = threadGroup;
        } else {
            throw new IllegalArgumentException("priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }
    }

    public NettyAffinityThreadFactory(String poolName, boolean daemon, int priority) {
        this(poolName, daemon, priority, (ThreadGroup) null);
    }

    public Thread newThread(Runnable r) {
        Thread t = this.newThread(SingleFastThreadLocalRunnable.wrap(r), this.prefix + this.nextId.incrementAndGet());

        try {
            if (t.isDaemon() != this.daemon) {
                t.setDaemon(this.daemon);
            }

            if (t.getPriority() != this.priority) {
                t.setPriority(this.priority);
            }
        } catch (Exception var4) {
        }

        return t;
    }

    protected Thread newThread(Runnable r, String name) {
        return new FastThreadLocalThread(this.threadGroup, r, name);
    }

}
