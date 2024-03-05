package com.sk89q.worldedit.pnx;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.metadata.FixedMetadataValue;
import cn.nukkit.metadata.MetadataValue;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import com.fastasyncworldedit.core.Fawe;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.sk89q.fastasyncworldedit.pnx.FawePNX;
import com.sk89q.pnx.util.mappings.MappingRegistries;
import com.sk89q.pnx.util.mappings.populator.BlockRegistryPopulator;
import com.sk89q.pnx.util.mappings.type.BlockMappings;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.event.platform.PlatformUnreadyEvent;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.pnx.data.FileRegistries;
import com.sk89q.worldedit.world.block.BlockCategory;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.gamemode.GameModes;
import com.sk89q.worldedit.world.item.ItemCategory;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class PNXWorldEditPlugin extends PluginBase {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    public static final String CUI_PLUGIN_CHANNEL = "worldedit:cui";
    private static PNXWorldEditPlugin INSTANCE;
    //private static final int BSTATS_ID = 1403;

    //private final SimpleLifecycled<BukkitImplAdapter> adapter = SimpleLifecycled.invalid();
    private PNXServerInterface platform;
    private PNXConfiguration config;
    private FileRegistries fileRegistries;

    @Override
    public void onLoad() {

        //FAWE start
        // This is already covered by Spigot, however, a more pesky warning with a proper explanation over "Ambiguous plugin name..." can't hurt.
        var plugins = Server.getInstance().getPluginManager().getPlugins().values();
        for (Plugin p : plugins) {
            if (p.getName().equals("WorldEdit")) {
                LOGGER.warn(
                        "You installed WorldEdit alongside FastAsyncWorldEdit. That is unneeded and will cause unforeseen issues, " +
                                "because FastAsyncWorldEdit already provides WorldEdit. " +
                                "Stop your server and delete the 'worldedit-bukkit' jar from your plugins folder.");
            }
        }
        //FAWE end

        INSTANCE = this;

        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();

        WorldEdit worldEdit = WorldEdit.getInstance();

        // Setup platform
        platform = new PNXServerInterface(this, getServer());
        worldEdit.getPlatformManager().register(platform);

        //FAWE start - Migrate from config-legacy to worldedit-config
        migrateLegacyConfig();
        //FAWE end

        //FAWE start - Modify WorldEdit config name
        config = new PNXConfiguration(new YAMLProcessor(new File(getDataFolder(), "worldedit-config.yml"), true), this);
        //FAWE end

        //Path delChunks = Paths.get(getDataFolder().getPath(), DELCHUNKS_FILE_NAME);
        //if (Files.exists(delChunks)) {
        //    ChunkDeleter.runFromFile(delChunks, true);
        //}

        //FAWE start - Delete obsolete DummyFawe from pre 1.14 days
        if (Objects.requireNonNull(this.getDataFolder().getParentFile().listFiles(file -> {
            if (file.getName().equals("DummyFawe.jar")) {
                file.delete();
                return true;
            }
            return false;
        })).length > 0) {
            LOGGER.warn("DummyFawe detected and automatically deleted! This file is no longer necessary.");
        }
        //FAWE end
    }

    /**
     * Called on plugin enable.
     */
    @Override
    public void onEnable() {
        //FAWE start
        new FawePNX(this);
        //FAWE end

        config.load(); // Load config before we say we've loaded platforms as it is used in listeners of the event

        WorldEdit.getInstance().getEventBus().post(new PlatformsRegisteredEvent());

        this.fileRegistries = new FileRegistries(this);
        this.fileRegistries.loadDataFiles();
        WorldEdit.getInstance().loadMappings();
        this.setupRegistries();

        PermissionsResolverManager.initialize(this); // Setup permission resolver

        // Register CUI
//        getServer().getMessenger().registerIncomingPluginChannel(this, CUI_PLUGIN_CHANNEL, new CUIChannelListener(this));
//        getServer().getMessenger().registerOutgoingPluginChannel(this, CUI_PLUGIN_CHANNEL);

        // Now we can register events
        getServer().getPluginManager().registerEvents(new WorldEditListener(this), this);
        // register async tab complete, if available
        WorldEdit.getInstance().getEventBus().post(new PlatformReadyEvent(platform));
        // Check if we are in a safe environment
//        ServerLib.checkUnsafeForks();
        // Check if a new build is available
//        UpdateNotification.doUpdateCheck();
    }

    /**
     * Called on plugin disable.
     */
    @Override
    public void onDisable() {
        Fawe.instance().onDisable();
        WorldEdit worldEdit = WorldEdit.getInstance();
        worldEdit.getSessionManager().unload();
        if (platform != null) {
            worldEdit.getEventBus().post(new PlatformUnreadyEvent(platform));
            worldEdit.getPlatformManager().unregister(platform);
            platform.unregisterCommands();
        }
        if (config != null) {
            config.unload();
        }
        this.getServer().getScheduler().cancelTask(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // Add the command to the array because the underlying command handling
        // code of WorldEdit expects it
        String[] split = new String[args.length + 1];
        System.arraycopy(args, 0, split, 1, args.length);
        split[0] = commandLabel.startsWith("fastasyncworldedit:") ? commandLabel.replace("fastasyncworldedit:", "") :
                commandLabel;

        CommandEvent event = new CommandEvent(wrapCommandSender(sender), Joiner.on(" ").join(split));
        getWorldEdit().getEventBus().post(event);

        return true;
    }

    /**
     * Gets the session for the player.
     *
     * @param player a player
     * @return a session
     */
    public LocalSession getSession(Player player) {
        return WorldEdit.getInstance().getSessionManager().get(wrapPlayer(player));
    }

    /**
     * Gets the session for the player.
     *
     * @param player a player
     * @return a session
     */
    public EditSession createEditSession(Player player) {
        com.sk89q.worldedit.entity.Player wePlayer = wrapPlayer(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        BlockBag blockBag = session.getBlockBag(wePlayer);

        EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .locatableActor(wePlayer)
                .maxBlocks(session.getBlockChangeLimit())
                .blockBag(blockBag)
                .build();
        editSession.enableStandardMode();

        return editSession;
    }

    public void setupRegistries() {
        // Blocks
        final BlockMappings blockMappings = MappingRegistries.BLOCKS;
        BlockTypesCache.getAllProperties();
        BlockRegistryPopulator.initMapping2(blockMappings);

        // Entities
        for (String name : fileRegistries.getDataFile().entities) {
            if (EntityType.REGISTRY.get(name) == null) {
                EntityType.REGISTRY.register(name, new EntityType(name));
            }
        }
        // Tags
        for (String name : fileRegistries.getDataFile().blocktags.keySet()) {
            if (BlockCategory.REGISTRY.get(name) == null) {
                BlockCategory.REGISTRY.register(name, new BlockCategory(name));
            }
        }
        for (String name : fileRegistries.getDataFile().itemtags.keySet()) {
            if (ItemCategory.REGISTRY.get(name) == null) {
                ItemCategory.REGISTRY.register(name, new ItemCategory(name));
            }
        }
        GameModes.get("");
        WeatherTypes.get("");
    }

    public FileRegistries getFileRegistries() {
        return this.fileRegistries;
    }

    /**
     * Remember an edit session.
     *
     * @param player      a player
     * @param editSession an edit session
     */
    public void remember(Player player, EditSession editSession) {
        com.sk89q.worldedit.entity.Player wePlayer = wrapPlayer(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);

        session.remember(editSession);
        editSession.close();

        WorldEdit.getInstance().flushBlockBag(wePlayer, editSession);
    }

    /**
     * Gets the instance of this plugin.
     *
     * @return an instance of the plugin
     * @throws NullPointerException if the plugin hasn't been enabled
     */
    public static PNXWorldEditPlugin getInstance() {
        return Preconditions.checkNotNull(INSTANCE);
    }

    public PNXServerInterface getInternalPlatform() {
        return platform;
    }

    /**
     * Returns the configuration used by WorldEdit.
     *
     * @return the configuration
     */
    public PNXConfiguration getLocalConfiguration() {
        return config;
    }

    /**
     * Get WorldEdit.
     *
     * @return an instance
     */
    public WorldEdit getWorldEdit() {
        return WorldEdit.getInstance();
    }

    public Actor wrapCommandSender(CommandSender sender) {
        if (sender instanceof Player) {
            return wrapPlayer((Player) sender);
        }/* else if (config.commandBlockSupport && sender instanceof ICommandBlock) {
            return new BukkitBlockCommandSender(this, (BlockCommandSender) sender);
        }*/

        return new PNXCommandSender(this, sender);
    }

    /**
     * Used to wrap a Bukkit Player as a WorldEdit Player.
     *
     * @param player a player
     * @return a wrapped player
     */
    public PNXPlayer wrapPlayer(Player player) {
        //FAWE start - Use cache over returning a direct BukkitPlayer
        PNXPlayer wePlayer = getCachedPlayer(player);
        if (wePlayer == null) {
            synchronized (player) {
                wePlayer = getCachedPlayer(player);
                if (wePlayer == null) {
                    wePlayer = new PNXPlayer(this, player);
                    player.setMetadata("WE", new FixedMetadataValue(this, wePlayer));
                    return wePlayer;
                }
            }
        }
        return wePlayer;
        //FAWE end
    }

    /**
     * Get the permissions resolver in use.
     *
     * @return the permissions resolver
     */
    public PermissionsResolverManager getPermissionsResolver() {
        return PermissionsResolverManager.getInstance();
    }

    PNXPlayer reCachePlayer(Player player) {
        synchronized (player) {
            PNXPlayer wePlayer = new PNXPlayer(this, player);
            player.setMetadata("WE", new FixedMetadataValue(this, wePlayer));
            return wePlayer;
        }
    }

    PNXPlayer getCachedPlayer(Player player) {
        List<MetadataValue> meta = player.getMetadata("WE");
        if (meta.isEmpty()) {
            return null;
        }
        return (PNXPlayer) meta.get(0).value();
    }

    private void migrateLegacyConfig() {
        File legacy = new File(getDataFolder(), "config-legacy.yml");
        if (legacy.exists()) {
            try {
                legacy.renameTo(new File(getDataFolder(), "worldedit-config.yml"));
                LOGGER.info("Migrated config-legacy.yml to worldedit-config.yml");
            } catch (Exception e) {
                LOGGER.error("Unable to rename legacy config file", e);
            }
        }
        createDefaultConfiguration("worldedit-config.yml");
    }

    /**
     * Create a default configuration file from the .jar.
     *
     * @param name the filename
     */
    protected void createDefaultConfiguration(String name) {
        File actual = new File(getDataFolder(), name);
        if (!actual.exists()) {
            try (InputStream stream = getResource("defaults/" + name)) {
                if (stream == null) {
                    throw new FileNotFoundException();
                }
                copyDefaultConfig(stream, actual, name);
            } catch (IOException e) {
                LOGGER.error("Unable to read default configuration: " + name);
            }
        }
    }

    private void copyDefaultConfig(InputStream input, File actual, String name) {
        try (FileOutputStream output = new FileOutputStream(actual)) {
            byte[] buf = new byte[8192];
            int length;
            while ((length = input.read(buf)) > 0) {
                output.write(buf, 0, length);
            }

            LOGGER.info("Default configuration file written: " + name);
        } catch (IOException e) {
            LOGGER.warn("Failed to write default config file", e);
        }
    }

    /**
     * Loads and reloads all configuration.
     */
    protected void loadConfiguration() {
        config.unload();
        config.load();
        getPermissionsResolver().load();
    }

}
