package xyz.jpenilla.squaremap.addon.common.scheduler;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.WrappedTask;

import java.util.concurrent.TimeUnit;

/**
 * Wrapped runnable.
 */
public abstract class WrappedRunnable implements Runnable {

    private WrappedTask wrappedTask;

    public synchronized boolean isCancelled() throws IllegalStateException {
        checkScheduled();
        return wrappedTask.isCancelled();
    }

    public WrappedTask runTaskTimer(FoliaLib scheduler, long delay, long period, TimeUnit unit) {
        checkNotYetScheduled();
        return setupTask(scheduler.getImpl().runTimer(this, delay, period, unit));
    }

    public WrappedTask runTaskTimerAsynchronously(FoliaLib scheduler, long delay, long period, TimeUnit unit) {
        checkNotYetScheduled();
        return setupTask(scheduler.getImpl().runTimerAsync(this, delay, period, unit));
    }

    public WrappedTask runTask(FoliaLib scheduler) {
        checkScheduled();
        scheduler.getImpl().runNextTick(this);
        return setupTask(null);
    }

    public WrappedTask runTaskAsynchronously(FoliaLib scheduler) {
        checkScheduled();
        scheduler.getImpl().runAsync(this);
        return setupTask(null);
    }

    public WrappedTask runTaskLater(FoliaLib scheduler, long delay, TimeUnit unit) {
        checkScheduled();
        return setupTask(scheduler.getImpl().runLater(this, delay, unit));
    }


    public synchronized void cancel() throws IllegalStateException {
        wrappedTask.cancel();
    }

    private void checkScheduled() {
        if (wrappedTask == null) {
            throw new IllegalStateException("Not scheduled yet");
        }
    }

    private void checkNotYetScheduled() {
        if (wrappedTask != null) {
            throw new IllegalStateException("Task is already scheduled!");
        }
    }

    private WrappedTask setupTask(final WrappedTask wrappedTask) {
        this.wrappedTask = wrappedTask;
        return wrappedTask;
    }
}
