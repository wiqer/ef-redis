package com.wiqer.redis.netty.util.concurrent;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectUtil;

class SingleFastThreadLocalRunnable implements Runnable {
    private final Runnable runnable;

    private SingleFastThreadLocalRunnable(Runnable runnable) {
        this.runnable = (Runnable) ObjectUtil.checkNotNull(runnable, "runnable");
    }

    public void run() {
        try {
            this.runnable.run();
        } finally {
            FastThreadLocal.removeAll();
        }

    }

    static Runnable wrap(Runnable runnable) {
        return (Runnable)(runnable instanceof SingleFastThreadLocalRunnable ? runnable : new SingleFastThreadLocalRunnable(runnable));
    }
}