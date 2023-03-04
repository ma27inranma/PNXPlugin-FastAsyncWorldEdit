package com.sk89q.fastasyncworldedit.pnx;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.Plugin;
import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.IFawe;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.queue.implementation.preloader.Preloader;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.image.ImageViewer;
import com.sk89q.fastasyncworldedit.pnx.listener.BrushListener;
import com.sk89q.fastasyncworldedit.pnx.util.PNXQueueHandler;
import com.sk89q.fastasyncworldedit.pnx.util.PNXTaskManager;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.pnx.PNXAdapter;
import com.sk89q.worldedit.pnx.PNXPlayer;
import com.sk89q.worldedit.pnx.PNXWorldEditPlugin;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class FawePNX implements IFawe, Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private final Plugin plugin;

    public FawePNX(Plugin plugin) {
        this.plugin = plugin;
        try {
            Fawe.set(this);
            Fawe.setupInjector();
            try {
                new BrushListener(plugin);
            } catch (Throwable e) {
                LOGGER.error("Brush Listener Failed", e);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            Server.getInstance().shutdown();
        }

//        platformAdapter = new NMSAdapter();

        //PlotSquared support is limited to Spigot/Paper as of 02/20/2020
        TaskManager.taskManager().later(this::setupPlotSquared, 0);

        // Registered delayed Event Listeners
        TaskManager.taskManager().task(() -> {
            // This class
            PNXWorldEditPlugin.getInstance().getServer().getPluginManager().registerEvents(FawePNX.this, FawePNX.this.plugin);
        });
    }

    @Override
    public QueueHandler getQueueHandler() {
        return new PNXQueueHandler();
    }

    @Override
    public synchronized ImageViewer getImageViewer(com.sk89q.worldedit.entity.Player player) {
        return null;
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public String getDebugInfo() {
        StringBuilder msg = new StringBuilder();

        msg.append("# FastAsyncWorldEdit Information\n");
        msg.append(Fawe.instance().getVersion()).append("\n\n");

        List<Plugin> plugins = new ArrayList<>(PNXWorldEditPlugin
                .getInstance()
                .getServer()
                .getPluginManager()
                .getPlugins()
                .values());
        plugins.sort(Comparator.comparing(Plugin::getName));

        msg.append("Server Version: ").append(PNXWorldEditPlugin.getInstance().getServer().getVersion()).append("\n");
        msg.append("Plugins (").append(plugins.size()).append("):\n");
        for (Plugin p : plugins) {
            msg.append(" - ").append(p.getName()).append(":").append("\n")
                    .append("  • Version: ").append(p.getDescription().getVersion()).append("\n")
                    .append("  • Enabled: ").append(p.isEnabled()).append("\n")
                    .append("  • Main: ").append(p.getDescription().getMain()).append("\n")
                    .append("  • Authors: ").append(p.getDescription().getAuthors()).append("\n")
                    .append("  • Load Before: ").append(p.getDescription().getLoadBefore()).append("\n")
                    .append("  • Dependencies: ").append(p.getDescription().getDepend()).append("\n")
                    .append("  • Soft Dependencies: ").append(p.getDescription().getSoftDepend()).append("\n");
        }
        int dataVersion = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getDataVersion();
        return msg.toString();
    }

    /**
     * The task manager handles sync/async tasks.
     */
    @Override
    public TaskManager getTaskManager() {
        return new PNXTaskManager(plugin);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * A mask manager handles region restrictions e.g., PlotSquared plots / WorldGuard regions
     */
    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();

        /*final Plugin worldguardPlugin =
                PNXWorldEditPlugin.getInstance().getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldguardPlugin != null && worldguardPlugin.isEnabled()) {
            try {
                managers.add(new WorldGuardFeature(worldguardPlugin));
                LOGGER.info("Attempting to use plugin 'WorldGuard'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin townyPlugin = PNXWorldEditPlugin.getInstance().getServer().getPluginManager().getPlugin("Towny");
        if (townyPlugin != null && townyPlugin.isEnabled()) {
            try {
                managers.add(new TownyFeature(townyPlugin));
                LOGGER.info("Attempting to use plugin 'Towny'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin residencePlugin = PNXWorldEditPlugin.getInstance().getServer().getPluginManager().getPlugin("Residence");
        if (residencePlugin != null && residencePlugin.isEnabled()) {
            try {
                managers.add(new ResidenceFeature(residencePlugin, this));
                LOGGER.info("Attempting to use plugin 'Residence'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin griefpreventionPlugin =
                PNXWorldEditPlugin.getInstance().getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefpreventionPlugin != null && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin));
                LOGGER.info("Attempting to use plugin 'GriefPrevention'");
            } catch (Throwable ignored) {
            }
        }
        final Plugin griefdefenderPlugin =
                PNXWorldEditPlugin.getInstance().getServer().getPluginManager().getPlugin("GriefDefender");
        if (griefdefenderPlugin != null && griefdefenderPlugin.isEnabled()) {
            try {
                managers.add(new GriefDefenderFeature(griefdefenderPlugin));
                LOGGER.info("Attempting to use plugin 'GriefDefender'");
            } catch (Throwable ignored) {
            }
        }*/

        return managers;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        cn.nukkit.Player player = event.getPlayer();
        PNXPlayer wePlayer = PNXAdapter.adapt(player);
        wePlayer.unregister();
    }

    @Override
    public String getPlatform() {
        return "PowerNukkitX";
    }

    @Override
    public UUID getUUID(String name) {
        return PNXWorldEditPlugin.getInstance().getServer().getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public String getName(UUID uuid) {
        return PNXWorldEditPlugin.getInstance().getServer().getOfflinePlayer(uuid).getName();
    }

    @Override
    public Preloader getPreloader(boolean initialise) {
        return null;
    }

    @Override
    public boolean isChunksStretched() {
        return true;
    }

    @Override
    public FAWEPlatformAdapterImpl getPlatformAdapter() {
        return PNXPlatformAdapter.INSTANCE;
    }

    private void setupPlotSquared() {
        Plugin plotSquared = this.plugin.getServer().getPluginManager().getPlugin("PlotSquared");
        if (plotSquared == null) {
            return;
        }
//        if (PlotSquared.get().getVersion().version[0] == 6) {
//            WEManager.weManager().addManager(new com.fastasyncworldedit.bukkit.regions.plotsquared.PlotSquaredFeature());
//            LOGGER.info("Plugin 'PlotSquared' v6 found. Using it now.");
//        } else {
//            LOGGER.error("Incompatible version of PlotSquared found. Please use PlotSquared v6.");
//            LOGGER.info("https://www.spigotmc.org/resources/77506/");
//        }
    }

}
