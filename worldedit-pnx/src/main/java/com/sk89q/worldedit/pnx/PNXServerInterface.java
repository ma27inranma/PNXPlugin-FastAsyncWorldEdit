/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.pnx;


import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityIds;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Identifier;
import com.fastasyncworldedit.core.extent.processor.lighting.RelighterFactory;
import com.sk89q.pnx.util.CommandInfo;
import com.sk89q.pnx.util.CommandRegistration;
import com.sk89q.pnx.util.NMSRelighterFactory;
import com.sk89q.pnx.util.PNXCommandInspector;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.MultiUserPlatform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.pnx.registry.PNXRegistries;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.registry.Registries;
import org.apache.logging.log4j.Logger;
import org.enginehub.piston.CommandManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sk89q.worldedit.util.formatting.WorldEditText.reduceToText;

public class PNXServerInterface extends AbstractPlatform implements MultiUserPlatform {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public final Server server;
    public final PNXWorldEditPlugin plugin;
    private final CommandRegistration dynamicCommands;
    //FAWE start
    private RelighterFactory relighterFactory;
    //FAWE end
    private boolean hookingEvents;

    public PNXServerInterface(PNXWorldEditPlugin plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
        this.dynamicCommands = new CommandRegistration(plugin);
    }

    CommandRegistration getDynamicCommands() {
        return dynamicCommands;
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public Registries getRegistries() {
        return PNXRegistries.getInstance();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDataVersion() {
        return 3105;
    }

    @Override
    public DataFixer getDataFixer() {
        return null;
    }

    @Override
    public boolean isValidMobType(String type) {
        if (!type.startsWith("minecraft:")) {
            return false;
        }
        if (EntityIds.IDENTIFIER_2_IDS.containsKey(type)) {
            return Entity.createEntity(new Identifier(type), new Position(0, 0, 0)) instanceof EntityLiving;
        } else {
            return false;
        }
    }

    @Override
    public void reload() {
        plugin.loadConfiguration();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        return Server
                .getInstance()
                .getScheduler()
                .scheduleDelayedRepeatingTask(plugin, task, (int) delay, (int) period)
                .getTaskId();
    }

    @Override
    public Watchdog getWatchdog() {
        return null;
    }

    @Override
    public List<com.sk89q.worldedit.world.World> getWorlds() {
        var worlds = server.getLevels().values();
        List<com.sk89q.worldedit.world.World> ret = new ArrayList<>(worlds.size());
        for (var world : worlds) {
            ret.add(PNXAdapter.adapt(world));
        }
        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof PNXPlayer) {
            return player;
        } else {
            cn.nukkit.Player bukkitPlayer = server.getPlayerExact(player.getName());
            return bukkitPlayer != null ? PNXWorldEditPlugin.getInstance().wrapPlayer(bukkitPlayer) : null;
        }
    }

    @Nullable
    @Override
    public PNXWorld matchWorld(com.sk89q.worldedit.world.World world) {
        if (world instanceof PNXWorld) {
            return (PNXWorld) world;
        } else {
            cn.nukkit.level.Level level = server.getLevelByName(world.getName());
            return level != null ? new PNXWorld(level) : null;
        }
    }

    @Override
    public void registerCommands(CommandManager dispatcher) {
        PNXCommandInspector inspector = new PNXCommandInspector(plugin, dispatcher);
        dynamicCommands.register(dispatcher.getAllCommands()
                .map(command -> {
                    String[] permissionsArray = command.getCondition()
                            .as(PermissionCondition.class)
                            .map(PermissionCondition::getPermissions)
                            .map(s -> s.toArray(new String[0]))
                            .orElseGet(() -> new String[0]);

                    String[] aliases = Stream.concat(
                            Stream.of(command.getName()),
                            command.getAliases().stream()
                    ).toArray(String[]::new);
                    // TODO Handle localisation correctly
                    return new CommandInfo(
                            reduceToText(
                                    command.getUsage(),
                                    WorldEdit.getInstance().getConfiguration().defaultLocale
                            ),
                            reduceToText(command.getDescription(), WorldEdit.getInstance().getConfiguration().defaultLocale),
                            aliases,
                            inspector,
                            permissionsArray
                    );
                }).collect(Collectors.toList()));
    }

    @Override
    public void setGameHooksEnabled(boolean enabled) {
        this.hookingEvents = enabled;
    }

    @Override
    public LocalConfiguration getConfiguration() {
        return plugin.getLocalConfiguration();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String getPlatformName() {
        return "PowerNukkitX-Official";
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getDescription().getVersion();
    }

    //FAWE start
    @Override
    public String getId() {
        return "intellectualsites:pnx";
    }
    //FAWE end

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.NORMAL);
        //capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.PREFERRED);
        capabilities.put(Capability.PERMISSIONS, Preference.PREFERRED);
        capabilities.put(Capability.USER_COMMANDS, Preference.PREFERRED);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFER_OTHERS);
        return capabilities;
    }

    private static final Set<SideEffect> SUPPORTED_SIDE_EFFECTS = Set.of();

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return SUPPORTED_SIDE_EFFECTS;
    }

    public void unregisterCommands() {
        dynamicCommands.unregisterCommands();
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<>();
        for (cn.nukkit.Player player : Server.getInstance().getOnlinePlayers().values()) {
            users.add(PNXWorldEditPlugin.getInstance().wrapPlayer(player));
        }
        return users;
    }

    //FAWE start
    @Override
    @Nonnull
    public RelighterFactory getRelighterFactory() {
        if (this.relighterFactory == null) {
            this.relighterFactory = new NMSRelighterFactory();
            LOGGER.info("Using {} as relighter factory.", this.relighterFactory.getClass().getCanonicalName());
        }
        return this.relighterFactory;
    }

    @Override
    public int versionMinY() {
        return -64;
    }

    @Override
    public int versionMaxY() {
        return 319;
    }
    //FAWE end
}
