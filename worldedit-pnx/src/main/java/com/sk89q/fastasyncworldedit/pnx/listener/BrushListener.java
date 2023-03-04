package com.sk89q.fastasyncworldedit.pnx.listener;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.Plugin;
import com.fastasyncworldedit.core.command.tool.ResettableTool;
import com.fastasyncworldedit.core.command.tool.scroll.ScrollTool;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.pnx.PNXAdapter;
import com.sk89q.worldedit.pnx.PNXPlayer;

public class BrushListener implements Listener {

    public BrushListener(Plugin plugin) {
        Server.getInstance().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHoldEvent(final PlayerItemHeldEvent event) {
        final Player pnxPlayer = event.getPlayer();
        if (pnxPlayer.isSneaking()) {
            return;
        }
        PNXPlayer player = PNXAdapter.adapt(pnxPlayer);
        LocalSession session = player.getSession();
        Tool tool = session.getTool(player);
        if (tool instanceof ScrollTool scrollable) {
            final int slot = event.getSlot();
            final int oldSlot = event.getSlot();
            final int ri;
            if ((((slot - oldSlot) <= 4) && ((slot - oldSlot) > 0)) || (((slot - oldSlot) < -4))) {
                ri = 1;
            } else {
                ri = -1;
            }
            if (scrollable.increment(player, ri)) {
                final PlayerInventory inv = pnxPlayer.getInventory();
                final Item item = inv.getItem(slot);
                final Item newItem = inv.getItem(oldSlot);
                inv.setItem(slot, newItem);
                inv.setItem(oldSlot, item);
                inv.sendContents(pnxPlayer);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player pnxPlayer = event.getPlayer();
        if (pnxPlayer.isSneaking()) {
            if (event.getAction() == PlayerInteractEvent.Action.PHYSICAL) {
                return;
            }
            com.sk89q.worldedit.entity.Player player = PNXAdapter.adapt(pnxPlayer);
            LocalSession session = player.getSession();
            Tool tool = session.getTool(player);
            if (tool instanceof ResettableTool) {
                if (((ResettableTool) tool).reset()) {
                    event.setCancelled(true);
                }
            }
        }
    }

}
