package com.wiqer.redis.netty.channel.nio;

import io.netty.channel.*;
import io.netty.channel.nio.NioTask;
import io.netty.util.IntSupplier;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReflectionUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class NioSingleEventLoop extends SingleThreadEventLoop {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioSingleEventLoop.class);

    private static final int CLEANUP_INTERVAL = 256; // XXX Hard-coded value, but won't need customization.

    private static final boolean DISABLE_KEY_SET_OPTIMIZATION =
            SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);

    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;

    private final IntSupplier selectNowSupplier = this::selectNow;

    // Workaround for JDK NIO bug.
    //
    // See:
    // - https://bugs.java.com/view_bug.do?bug_id=6427854
    // - https://github.com/netty/netty/issues/203
    static {
        final String key = "sun.nio.ch.bugLevel";
        final String bugLevel = SystemPropertyUtil.get(key);
        if (bugLevel == null) {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        System.setProperty(key, "");
                        return null;
                    }
                });
            } catch (final SecurityException e) {
                log.debug("Unable to get/set System Property: " + key, e);
            }
        }

        int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
        if (selectorAutoRebuildThreshold < MIN_PREMATURE_SELECTOR_RETURNS) {
            selectorAutoRebuildThreshold = 0;
        }

        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;

        if (log.isDebugEnabled()) {
            log.debug("-Dio.netty.noKeySetOptimization: {}", DISABLE_KEY_SET_OPTIMIZATION);
            log.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", SELECTOR_AUTO_REBUILD_THRESHOLD);
        }
    }

    /**
     * The NIO {@link Selector}.
     */
    private Selector selector;
    private Selector unwrappedSelector;
    private SingleSelectedSelectionKeySet selectedKeys;

    private final SelectorProvider provider;

    private static final long AWAKE = -1L;
    private static final long NONE = Long.MAX_VALUE;

    // nextWakeupNanos is:
    //    AWAKE            when EL is awake
    //    NONE             when EL is waiting with no wakeup scheduled
    //    other value T    when EL is waiting with wakeup scheduled at time T
    private final AtomicLong nextWakeupNanos = new AtomicLong(AWAKE);

    private final SelectStrategy selectStrategy;

    private volatile int ioRatio = 50;
    private int cancelledKeys;
    private boolean needsToSelectAgain;

    NioSingleEventLoop(NioSingleEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler,
                 EventLoopTaskQueueFactory taskQueueFactory, EventLoopTaskQueueFactory tailTaskQueueFactory) {
        super(parent, executor, false, newTaskQueue(taskQueueFactory), newTaskQueue(tailTaskQueueFactory),
                rejectedExecutionHandler);
        this.provider = ObjectUtil.checkNotNull(selectorProvider, "selectorProvider");
        this.selectStrategy = ObjectUtil.checkNotNull(strategy, "selectStrategy");
        final SelectorTuple selectorTuple = openSelector();
        this.selector = selectorTuple.selector;
        this.unwrappedSelector = selectorTuple.unwrappedSelector;
    }

    private static Queue<Runnable> newTaskQueue(
            EventLoopTaskQueueFactory queueFactory) {
        if (queueFactory == null) {
            return newTaskQueue0(DEFAULT_MAX_PENDING_TASKS);
        }
        return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
    }

    private static final class SelectorTuple {
        final Selector unwrappedSelector;
        final Selector selector;

        SelectorTuple(Selector unwrappedSelector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = unwrappedSelector;
        }

        SelectorTuple(Selector unwrappedSelector, Selector selector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = selector;
        }
    }

    //初始化 开启Selector
    private SelectorTuple openSelector() {
        final Selector unwrappedSelector;
        try {
            unwrappedSelector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }

        if (DISABLE_KEY_SET_OPTIMIZATION) {
            return new SelectorTuple(unwrappedSelector);
        }

        Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    return Class.forName(
                            "sun.nio.ch.SelectorImpl",
                            false,
                            PlatformDependent.getSystemClassLoader());
                } catch (Throwable cause) {
                    return cause;
                }
            }
        });

        if (!(maybeSelectorImplClass instanceof Class) ||
                // ensure the current selector implementation is what we can instrument.
                !((Class<?>) maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass())) {
            if (maybeSelectorImplClass instanceof Throwable) {
                Throwable t = (Throwable) maybeSelectorImplClass;
                log.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, t);
            }
            return new SelectorTuple(unwrappedSelector);
        }

        final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;
        final SingleSelectedSelectionKeySet selectedKeySet = new SingleSelectedSelectionKeySet();

        Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
                    Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");

                    if (PlatformDependent.javaVersion() >= 9 && PlatformDependent.hasUnsafe()) {
                        // Let us try to use sun.misc.Unsafe to replace the SelectionKeySet.
                        // This allows us to also do this in Java9+ without any extra flags.
                        long selectedKeysFieldOffset = PlatformDependent.objectFieldOffset(selectedKeysField);
                        long publicSelectedKeysFieldOffset =
                                PlatformDependent.objectFieldOffset(publicSelectedKeysField);

                        if (selectedKeysFieldOffset != -1 && publicSelectedKeysFieldOffset != -1) {
                            PlatformDependent.putObject(
                                    unwrappedSelector, selectedKeysFieldOffset, selectedKeySet);
                            PlatformDependent.putObject(
                                    unwrappedSelector, publicSelectedKeysFieldOffset, selectedKeySet);
                            return null;
                        }
                        // We could not retrieve the offset, lets try reflection as last-resort.
                    }

                    Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }
                    cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }

                    selectedKeysField.set(unwrappedSelector, selectedKeySet);
                    publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
                    return null;
                } catch (NoSuchFieldException e) {
                    return e;
                } catch (IllegalAccessException e) {
                    return e;
                }
            }
        });

        if (maybeException instanceof Exception) {
            selectedKeys = null;
            Exception e = (Exception) maybeException;
            log.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, e);
            return new SelectorTuple(unwrappedSelector);
        }
        selectedKeys = selectedKeySet;
        log.trace("instrumented a special java.util.Set into: {}", unwrappedSelector);
        return new SelectorTuple(unwrappedSelector,
                new SingleSelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
    }

    /**
     * Returns the {@link SelectorProvider} used by this {@link NioSingleEventLoop} to obtain the {@link Selector}.
     */
    public SelectorProvider selectorProvider() {
        return provider;
    }

    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return newTaskQueue0(maxPendingTasks);
    }

    private static Queue<Runnable> newTaskQueue0(int maxPendingTasks) {
        // This event loop never calls takeTask()
        return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.<Runnable>newMpscQueue()
                : PlatformDependent.<Runnable>newMpscQueue(maxPendingTasks);
    }

    /**
     * Registers an arbitrary {@link SelectableChannel}, not necessarily created by Netty, to the {@link Selector}
     * of this event loop.  Once the specified {@link SelectableChannel} is registered, the specified {@code task} will
     * be executed by this event loop when the {@link SelectableChannel} is ready.
     */
    public void register(final SelectableChannel ch, final int interestOps, final NioTask<?> task) {
        ObjectUtil.checkNotNull(ch, "ch");
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ch.validOps()) != 0) {
            throw new IllegalArgumentException(
                    "invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
        ObjectUtil.checkNotNull(task, "task");

        if (isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }

        if (inEventLoop()) {
            register0(ch, interestOps, task);
        } else {
            try {
                // Offload to the EventLoop as otherwise java.nio.channels.spi.AbstractSelectableChannel.register
                // may block for a long time while trying to obtain an internal lock that may be hold while selecting.
                submit(new Runnable() {
                    @Override
                    public void run() {
                        register0(ch, interestOps, task);
                    }
                }).sync();
            } catch (InterruptedException ignore) {
                // Even if interrupted we did schedule it so just mark the Thread as interrupted.
                Thread.currentThread().interrupt();
            }
        }
    }

    private void register0(SelectableChannel ch, int interestOps, NioTask<?> task) {
        try {
            ch.register(unwrappedSelector, interestOps, task);
        } catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    /**
     * Returns the percentage of the desired amount of time spent for I/O in the event loop.
     */
    public int getIoRatio() {
        return ioRatio;
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the event loop. Value range from 1-100.
     * The default value is {@code 50}, which means the event loop will try to spend the same amount of time for I/O
     * as for non-I/O tasks. The lower the number the more time can be spent on non-I/O tasks. If value set to
     * {@code 100}, this feature will be disabled and event loop will not attempt to balance I/O and non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }

    /**
     * Replaces the current {@link Selector} of this event loop with newly created {@link Selector}s to work
     * around the infamous epoll 100% CPU bug.
     */
    public void rebuildSelector() {
        if (!inEventLoop()) {
            execute(new Runnable() {
                @Override
                public void run() {
                    rebuildSelector0();
                }
            });
            return;
        }
        rebuildSelector0();
    }

    @Override
    public int registeredChannels() {
        return selector.keys().size() - cancelledKeys;
    }

    private void rebuildSelector0() {
        final Selector oldSelector = selector;
        final SelectorTuple newSelectorTuple;

        if (oldSelector == null) {
            return;
        }

        try {
            newSelectorTuple = openSelector();
        } catch (Exception e) {
            log.warn("Failed to create a new Selector.", e);
            return;
        }

        // Register all channels to the new Selector.
        int nChannels = 0;
        for (SelectionKey key: oldSelector.keys()) {
            Object a = key.attachment();
            try {
                if (!key.isValid() || key.channel().keyFor(newSelectorTuple.unwrappedSelector) != null) {
                    continue;
                }

                int interestOps = key.interestOps();
                key.cancel();
                SelectionKey newKey = key.channel().register(newSelectorTuple.unwrappedSelector, interestOps, a);
                if (a instanceof AbstractSingleNioChannel) {
                    // Update SelectionKey
                    ((AbstractSingleNioChannel) a).selectionKey = newKey;
                }
                nChannels ++;
            } catch (Exception e) {
                log.warn("Failed to re-register a Channel to the new Selector.", e);
                if (a instanceof AbstractSingleNioChannel) {
                    AbstractSingleNioChannel ch = (AbstractSingleNioChannel) a;
                    ch.unsafe().close(ch.unsafe().voidPromise());
                } else {
                    @SuppressWarnings("unchecked")
                    NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                    invokeChannelUnregistered(task, key, e);
                }
            }
        }

        selector = newSelectorTuple.selector;
        unwrappedSelector = newSelectorTuple.unwrappedSelector;

        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to close the old Selector.", t);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Migrated " + nChannels + " channel(s) to the new Selector.");
        }
    }

    @Override
    protected void run() {
        int selectCnt = 0;
        for (;;) {
            try {
                int strategy;
                try {
                    strategy = selectStrategy.calculateStrategy(selectNowSupplier, hasTasks());
                    switch (strategy) {
                        case SelectStrategy.CONTINUE:
                            continue;

                        case SelectStrategy.BUSY_WAIT:
                            // fall-through to SELECT since the busy-wait is not supported with NIO

                        case SelectStrategy.SELECT:
                            long curDeadlineNanos = nextScheduledTaskDeadlineNanos();
                            if (curDeadlineNanos == -1L) {
                                curDeadlineNanos = NONE; // nothing on the calendar
                            }
                            nextWakeupNanos.set(curDeadlineNanos);
                            try {
                                //没有任务吗？
                                if (!hasTasks()) {
                                    //給selectedKeys添加任务
                                    strategy = select(curDeadlineNanos);
                                }
                            } finally {
                                // This update is just to help block unnecessary selector wakeups
                                // so use of lazySet is ok (no race condition)
                                nextWakeupNanos.lazySet(AWAKE);
                            }
                            // fall through
                        default:
                    }
                } catch (IOException e) {
                    // If we receive an IOException here its because the Selector is messed up. Let's rebuild
                    // the selector and retry. https://github.com/netty/netty/issues/8566
                    rebuildSelector0();
                    selectCnt = 0;
                    handleLoopException(e);
                    continue;
                }

                selectCnt++;
                cancelledKeys = 0;
                needsToSelectAgain = false;
                final int ioRatio = this.ioRatio;
                boolean ranTasks;
                //单线程就不需要这段代码了
//                if (ioRatio == 100) {
//                    try {
//                        if (strategy > 0) {
//                            processSelectedKeys();
//                        }
//                    } finally {
//                        // Ensure we always run tasks.
//                        ranTasks = runAllTasks();
//                    }
//                    //活动管道数
//                } else
                    if (strategy > 0) {
                    final long ioStartTime = System.nanoTime();
                    try {
                        //正常业务在这里处理字节流
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        final long ioTime = System.nanoTime() - ioStartTime;
                        ranTasks = runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                } else {
                    ranTasks = runAllTasks(0); // This will run the minimum number of tasks
                }

                if (ranTasks || strategy > 0) {
                    if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS && log.isDebugEnabled()) {
                        log.debug("Selector.select() returned prematurely {} times in a row for Selector {}.",
                                selectCnt - 1, selector);
                    }
                    selectCnt = 0;
                } else if (unexpectedSelectorWakeup(selectCnt)) { // Unexpected wakeup (unusual case)
                    selectCnt = 0;
                }
            } catch (CancelledKeyException e) {
                // Harmless exception - log anyway
                if (log.isDebugEnabled()) {
                    log.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                            selector, e);
                }
            } catch (Error e) {
                throw (Error) e;
            } catch (Throwable t) {
                handleLoopException(t);
            } finally {
                // Always handle shutdown even if the loop processing threw an exception.
                try {
                    if (isShuttingDown()) {
                        closeAll();
                        if (confirmShutdown()) {
                            return;
                        }
                    }
                } catch (Error e) {
                    throw (Error) e;
                } catch (Throwable t) {
                    handleLoopException(t);
                }
            }
        }
    }

    // returns true if selectCnt should be reset
    private boolean unexpectedSelectorWakeup(int selectCnt) {
        if (Thread.interrupted()) {
            // Thread was interrupted so reset selected keys and break so we not run into a busy loop.
            // As this is most likely a bug in the handler of the user or it's client library we will
            // also log it.
            //
            // See https://github.com/netty/netty/issues/2426
            if (log.isDebugEnabled()) {
                log.debug("Selector.select() returned prematurely because " +
                        "Thread.currentThread().interrupt() was called. Use " +
                        "NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
            }
            return true;
        }
        if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
            // The selector returned prematurely many times in a row.
            // Rebuild the selector to work around the problem.
            log.warn("Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                    selectCnt, selector);
            rebuildSelector();
            return true;
        }
        return false;
    }

    private static void handleLoopException(Throwable t) {
        log.warn("Unexpected exception in the selector loop.", t);

        // Prevent possible consecutive immediate failures that lead to
        // excessive CPU consumption.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }

    private void processSelectedKeys() {
        if (selectedKeys != null) {
            processSelectedKeysOptimized();
        } else {
            processSelectedKeysPlain(selector.selectedKeys());
        }
    }

    @Override
    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            log.warn("Failed to close a selector.", e);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        cancelledKeys ++;
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            needsToSelectAgain = true;
        }
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }

        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();

            if (a instanceof AbstractSingleNioChannel) {
                processSelectedKey(k, (AbstractSingleNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (!i.hasNext()) {
                break;
            }

            if (needsToSelectAgain) {
                selectAgain();
                selectedKeys = selector.selectedKeys();

                // Create the iterator again to avoid ConcurrentModificationException
                if (selectedKeys.isEmpty()) {
                    break;
                } else {
                    i = selectedKeys.iterator();
                }
            }
        }
    }

    //处理数据
    private void processSelectedKeysOptimized() {
        for (int i = 0; i < selectedKeys.size; ++i) {
            final SelectionKey k = selectedKeys.keys[i];
            // null out entry in the array to allow to have it GC'ed once the Channel close
            // See https://github.com/netty/netty/issues/2363
            selectedKeys.keys[i] = null;

            //获取附件-》channel a -》[id: 0x12baa2bd, L:/0:0:0:0:0:0:0:0:1921]
            final Object a = k.attachment();

            if (a instanceof AbstractSingleNioChannel) {
                //启动后进入这个AbstractSingleNioChannel
                processSelectedKey(k, (AbstractSingleNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (needsToSelectAgain) {
                // null out entries in the array to allow to have it GC'ed once the Channel close
                // See https://github.com/netty/netty/issues/2363
                selectedKeys.reset(i + 1);

                selectAgain();
                i = -1;
            }
        }
    }

    private void processSelectedKey(SelectionKey k, AbstractSingleNioChannel ch) {
        final AbstractSingleNioChannel.NioUnsafe unsafe = ch.unsafe();
        //非有效的
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                // If the channel implementation throws an exception because there is no event loop, we ignore this
                // because we are only trying to determine if ch is registered to this event loop and thus has authority
                // to close ch.
                return;
            }
            // Only close ch if ch is still registered to this EventLoop. ch could have deregistered from the event loop
            // and thus the SelectionKey could be cancelled as part of the deregistration process, but the channel is
            // still healthy and should not be closed.
            // See https://github.com/netty/netty/issues/5125
            if (eventLoop == this) {
                // close the channel if the key is not valid anymore
                unsafe.close(unsafe.voidPromise());
            }
            return;
        }

        try {
            int readyOps = k.readyOps();
            // We first need to call finishConnect() before try to trigger a read(...) or write(...) as otherwise
            // the NIO JDK channel implementation may throw a NotYetConnectedException.
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
                // See https://github.com/netty/netty/issues/924
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);

                unsafe.finishConnect();
            }

            // Process OP_WRITE first as we may be able to write some queued buffers and so free memory.
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
                ch.unsafe().forceFlush();
            }

            // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
            // to a spin loop
            // 读事件或者连接事件/关闭事件
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }

    private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
        int state = 0;
        try {
            task.channelReady(k.channel(), k);
            state = 1;
        } catch (Exception e) {
            k.cancel();
            invokeChannelUnregistered(task, k, e);
            state = 2;
        } finally {
            switch (state) {
                case 0:
                    k.cancel();
                    invokeChannelUnregistered(task, k, null);
                    break;
                case 1:
                    if (!k.isValid()) { // Cancelled by channelReady()
                        invokeChannelUnregistered(task, k, null);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void closeAll() {
        selectAgain();
        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractSingleNioChannel> channels = new ArrayList<AbstractSingleNioChannel>(keys.size());
        for (SelectionKey k: keys) {
            Object a = k.attachment();
            if (a instanceof AbstractSingleNioChannel) {
                channels.add((AbstractSingleNioChannel) a);
            } else {
                k.cancel();
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                invokeChannelUnregistered(task, k, null);
            }
        }

        for (AbstractSingleNioChannel ch: channels) {
            ch.unsafe().close(ch.unsafe().voidPromise());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k, Throwable cause) {
        try {
            task.channelUnregistered(k.channel(), cause);
        } catch (Exception e) {
            log.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && nextWakeupNanos.getAndSet(AWAKE) != AWAKE) {
            selector.wakeup();
        }
    }

    @Override
    protected boolean beforeScheduledTaskSubmitted(long deadlineNanos) {
        // Note this is also correct for the nextWakeupNanos == -1 (AWAKE) case
        return deadlineNanos < nextWakeupNanos.get();
    }

    @Override
    protected boolean afterScheduledTaskSubmitted(long deadlineNanos) {
        // Note this is also correct for the nextWakeupNanos == -1 (AWAKE) case
        return deadlineNanos < nextWakeupNanos.get();
    }

    Selector unwrappedSelector() {
        return unwrappedSelector;
    }

    int selectNow() throws IOException {
        return selector.selectNow();
    }

    private int select(long deadlineNanos) throws IOException {
        if (deadlineNanos == NONE) {
            return selector.select();
        }
        // Timeout will only be 0 if deadline is within 5 microsecs
        long timeoutMillis = deadlineToDelayNanos(deadlineNanos + 995000L) / 1000000L;
        return timeoutMillis <= 0 ? selector.selectNow() : selector.select(timeoutMillis);
    }

    private void selectAgain() {
        needsToSelectAgain = false;
        try {
            selector.selectNow();
        } catch (Throwable t) {
            log.warn("Failed to update SelectionKeys.", t);
        }
    }
}
