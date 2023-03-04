package com.sk89q.fastasyncworldedit.pnx.util;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.scheduler.AsyncTask;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.pnx.PNXWorldEditPlugin;

import javax.annotation.Nonnull;

public class PNXTaskManager extends TaskManager {

    private final Plugin plugin;

    public PNXTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleRepeatingTask(this.plugin, runnable, interval, false).getTaskId();
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        return this.plugin.getServer().getScheduler().scheduleRepeatingTask(this.plugin, runnable, interval, true).getTaskId();
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        this.plugin.getServer().getScheduler().scheduleAsyncTask(this.plugin, new AsyncTask() {
            @Override
            public void onRun() {
                runnable.run();
            }
        }).getTaskId();
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        this.plugin.getServer().getScheduler().scheduleTask(this.plugin, runnable).getTaskId();
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler().scheduleDelayedTask(this.plugin, runnable, delay).getTaskId();
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        this.plugin.getServer().getScheduler().scheduleDelayedTask(this.plugin, runnable, delay, true);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            PNXWorldEditPlugin.getInstance().getServer().getScheduler().cancelTask(task);
        }
    }

}
