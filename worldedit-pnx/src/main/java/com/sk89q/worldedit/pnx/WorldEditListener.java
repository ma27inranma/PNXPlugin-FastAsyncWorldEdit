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

// $Id$

package com.sk89q.worldedit.pnx;

import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerGameModeChangeEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.ServerCommandEvent;
import com.fastasyncworldedit.core.util.UpdateNotification;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.SessionIdleEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;

import java.util.Optional;

/**
 * Handles all events thrown in relation to a Player.
 */
public class WorldEditListener implements Listener {

    private final PNXWorldEditPlugin plugin;

    /**
     * Construct the object.
     *
     * @param plugin the plugin
     */
    public WorldEditListener(PNXWorldEditPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemode(PlayerGameModeChangeEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        //FAWE start - correctly handle refreshing LocalSession
        Player player = plugin.wrapPlayer(event.getPlayer());
        LocalSession session;
        if ((session = WorldEdit.getInstance().getSessionManager().getIfPresent(player)) == null) {
            session = WorldEdit.getInstance().getSessionManager().get(player);
        }
        session.loadDefaults(player, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        PNXPlayer player = plugin.wrapPlayer(event.getPlayer());
        //If plugins do silly things like teleport, deop (anything that requires a perm-recheck) (anything that ultimately
        // requires a BukkitPlayer at some point) then the retention of metadata by the server (as it's stored based on a
        // string value indescriminate of player a player relogging) means that a BukkitPlayer caching an old player object
        // will be kept, cached and retrieved by FAWE. Adding a simple memory-based equality check when the player rejoins,
        // and then "invaliding" (redoing) the cache if the players are not equal, fixes this.
        if (player.getPlayer() != event.getPlayer()) {
            player = plugin.reCachePlayer(event.getPlayer());
        }
        LocalSession session;
        if ((session = WorldEdit.getInstance().getSessionManager().getIfPresent(player)) != null) {
            session.loadDefaults(player, true);
        }
        UpdateNotification.doUpdateNotification(player);
    }
    //FAWE end

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerCommandSend(ServerCommandEvent event) {
        if (event.getSender().isPlayer()) {
            InjectedValueStore store = MapBackedValueStore.create();
            store.injectValue(Key.of(Actor.class), context ->
                    Optional.of(plugin.wrapCommandSender(event.getSender())));
            CommandManager commandManager = plugin
                    .getWorldEdit()
                    .getPlatformManager()
                    .getPlatformCommandManager()
                    .getCommandManager();
        }

    }

    /**
     * Called when a player interacts.
     *
     * @param event Relevant event details
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getInternalPlatform().isHookingEvents()) {
            return;
        }

        final Player player = plugin.wrapPlayer(event.getPlayer());
        final World world = player.getWorld();
        final WorldEdit we = plugin.getWorldEdit();
        final Direction direction = PNXAdapter.adapt(event.getFace());

        PlayerInteractEvent.Action action = event.getAction();
        if (action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            final Block clickedBlock = event.getBlock();
            final Location pos = new Location(world, clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockLeftClick(player, pos, direction)) {
                event.setCancelled(true);
            }

            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }

        } else if (action == PlayerInteractEvent.Action.LEFT_CLICK_AIR) {

            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }

        } else if (action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            final Block clickedBlock = event.getBlock();
            final Location pos = new Location(world, clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockRightClick(player, pos, direction)) {
                event.setCancelled(true);
            }

            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        } else if (action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getWorldEdit().getEventBus().post(new SessionIdleEvent(new PNXPlayer.SessionKeyImpl(event.getPlayer())));
    }

}
