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


import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.fake.FakeStructBlock;
import cn.nukkit.blockstate.BlockStateRegistry;
import cn.nukkit.event.player.PlayerDropItemEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.network.protocol.UpdateBlockPacket;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.util.TaskManager;
import com.google.common.collect.Sets;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.component.TextUtils;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.serializer.plain.PlainComponentSerializer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;
import com.sk89q.worldedit.world.gamemode.GameModes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PNXPlayer extends AbstractPlayerActor {

    private final Player player;
    private final PNXWorldEditPlugin plugin;
    //FAWE start

    /**
     * This constructs a new {@link PNXPlayer} for the given {@link Player}.
     *
     * @param player The corresponding {@link Player} or null if you need a null WorldEdit player for some reason.
     * @deprecated Players are cached by the plugin. Should use {@link PNXWorldEditPlugin#wrapPlayer(Player)}
     */
    @Deprecated
    public PNXPlayer(@Nullable Player player) {
        super(player != null ? getExistingMap(PNXWorldEditPlugin.getInstance(), player) : new ConcurrentHashMap<>());
        this.plugin = PNXWorldEditPlugin.getInstance();
        this.player = player;
    }
    //FAWE end

    /**
     * This constructs a new {@link PNXPlayer} for the given {@link Player}.
     *
     * @param plugin The running instance of {@link PNXWorldEditPlugin}
     * @param player The corresponding {@link Player} or null if you need a null WorldEdit player for some reason.
     * @deprecated Players are cached by the plugin. Should use {@link PNXWorldEditPlugin#wrapPlayer(Player)}
     */
    @Deprecated
    public PNXPlayer(@Nonnull PNXWorldEditPlugin plugin, @Nullable Player player) {
        this.plugin = plugin;
        this.player = player;
        //FAWE start
        if (player != null && Settings.settings().CLIPBOARD.USE_DISK) {
            PNXPlayer cached = PNXWorldEditPlugin.getInstance().getCachedPlayer(player);
            if (cached == null) {
                loadClipboardFromDisk();
            }
        }
        //FAWE end
    }

    //FAWE start
    private static Map<String, Object> getExistingMap(PNXWorldEditPlugin plugin, Player player) {
        PNXPlayer cached = plugin.getCachedPlayer(player);
        if (cached != null) {
            return cached.getRawMeta();
        }
        return new ConcurrentHashMap<>();
    }
    //FAWE end

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        Item item = handSide == HandSide.MAIN_HAND
                ? player.getInventory().getItemInHand()
                : player.getOffhandInventory().getItem(0);
        return PNXAdapter.adapt(item);
    }

    @Override
    public BaseBlock getBlockInHand(HandSide handSide) throws WorldEditException {
        Item item = handSide == HandSide.MAIN_HAND
                ? player.getInventory().getItemInHand()
                : player.getOffhandInventory().getItem(0);
        return PNXAdapter.asBlockState(item).toBaseBlock();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getDisplayName() {
        return player.getDisplayName();
    }

    //FAWE start
    @Override
    public void giveItem(BaseItemStack itemStack) {
        final PlayerInventory inv = player.getInventory();
        Item newItem = PNXAdapter.adapt(itemStack);
        TaskManager.taskManager().sync(() -> {
            if (itemStack.getType().getId().equalsIgnoreCase(WorldEdit.getInstance().getConfiguration().wandItem)) {
                inv.remove(newItem);
            }
            final Item item = player.getInventory().getItemInHand();
            player.getInventory().setItemInHand(newItem);
            if (inv.canAddItem(item)) {
                inv.addItem(item);
            } else {
                PlayerDropItemEvent event = new PlayerDropItemEvent(player, newItem);
                Server.getInstance().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return null;
                }
                player.getLevel().dropItem(player.getLocation(), newItem);
            }
            var sets = Sets.newHashSet(player.getViewers().values());
            sets.add(player);
            inv.sendContents(sets);
            return null;
        });
    }
    //FAWE end

    @Deprecated
    @Override
    public void printRaw(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage(part);
        }
    }

    @Deprecated
    @Override
    public void print(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§d" + part);
        }
    }

    @Deprecated
    @Override
    public void printDebug(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§7" + part);
        }
    }

    @Deprecated
    @Override
    public void printError(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("§c" + part);
        }
    }

    @Override
    public void print(Component component) {
        print(PlainComponentSerializer.INSTANCE.serialize(WorldEditText.format(component, getLocale())));
    }

    @Override
    public boolean trySetPosition(Vector3 pos, float pitch, float yaw) {
        //FAWE start
        cn.nukkit.level.Level world = player.getLevel();
        if (pos instanceof com.sk89q.worldedit.util.Location) {
            com.sk89q.worldedit.util.Location loc = (com.sk89q.worldedit.util.Location) pos;
            Extent extent = loc.getExtent();
            if (extent instanceof World) {
                world = Server.getInstance().getLevelByName(((World) extent).getName());
            }
        }
        cn.nukkit.level.Level finalWorld = world;
        //FAWE end
        return TaskManager.taskManager().sync(() -> player.teleport(new cn.nukkit.level.Location(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                yaw,
                pitch,
                finalWorld
        )));
    }

    @Override
    public String[] getGroups() {
        return plugin.getPermissionsResolver().getGroups(player.getName());
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return new PNXPlayerBlockBag(player);
    }

    @Override
    public GameMode getGameMode() {
        var modeStr = switch (player.getGamemode()) {
            case 1 -> "creative";
            case 2 -> "adventure";
            case 3 -> "spectator";
            default -> "survival";
        };
        return GameModes.get(modeStr);
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        var mode = switch (gameMode.getId().toLowerCase(Locale.ROOT)) {
            case "creative" -> 1;
            case "adventure" -> 2;
            case "spectator" -> 3;
            default -> 0;
        };
        player.setGamemode(mode);
    }

    @Override
    public boolean hasPermission(String perm) {
        return (!plugin.getLocalConfiguration().noOpPermissions && player.isOp())
                || this.player.hasPermission(perm);
    }

    //FAWE start
    @Override
    public void setPermission(String permission, boolean value) {
        this.player.addAttachment(PNXWorldEditPlugin.getInstance(), permission, value);
    }
    //FAWE end

    @Override
    public World getWorld() {
        return PNXAdapter.adapt(player.getLevel());
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        String[] params = event.getParameters();
        String send = event.getTypeId();
        if (params.length > 0) {
            send = send + "|" + StringUtil.joinString(params, "|");
        }
        player.sendMessage(send);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isAllowedToFly() {
        return player.getAllowFlight();
    }

    @Override
    public void setFlying(boolean flying) {
        player.setAllowFlight(flying);
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public com.sk89q.worldedit.util.Location getLocation() {
        Location nativeLocation = player.getLocation();
        Vector3 position = PNXAdapter.asVector(nativeLocation);
        return new com.sk89q.worldedit.util.Location(
                getWorld(),
                position,
                (float) nativeLocation.getYaw(),
                (float) nativeLocation.getPitch()
        );
    }

    @Override
    public boolean setLocation(com.sk89q.worldedit.util.Location location) {
        return player.teleport(PNXAdapter.adapt(location));
    }

    @Override
    public Locale getLocale() {
        return TextUtils.getLocaleByMinecraftTag(player.getLanguageCode().name());
    }

    @Override
    public void sendAnnouncements() {
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(this.player);
    }

    static class SessionKeyImpl implements SessionKey {
        // If not static, this will leak a reference

        private final UUID uuid;
        private final String name;

        SessionKeyImpl(Player player) {
            this.uuid = player.getUniqueId();
            this.name = player.getName();
        }

        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isActive() {
            // This is a thread safe call on CraftBukkit because it uses a
            // CopyOnWrite list for the list of players, but the Bukkit
            // specification doesn't require thread safety (though the
            // spec is extremely incomplete)
            return PNXWorldEditPlugin.getInstance().getServer().getPlayer(name) != null;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

    @Override
    public <B extends BlockStateHolder<B>> void sendFakeBlock(BlockVector3 pos, B block) {
        Location loc = new Location(pos.getX(), pos.getY(), pos.getZ(), player.getLevel());
        if (block == null) {
            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.blockRuntimeId = BlockStateRegistry.getRuntimeId(loc.getLevelBlock().getId());
            updateBlockPacket.flags = UpdateBlockPacket.FLAG_NETWORK;
            updateBlockPacket.x = loc.getFloorX();
            updateBlockPacket.y = loc.getFloorY();
            updateBlockPacket.z = loc.getFloorZ();
            player.dataPacket(updateBlockPacket);
        } else {
            if (block.getBlockType() == BlockTypes.STRUCTURE_BLOCK && block instanceof BaseBlock) {
                final FakeStructBlock fakeStructBlock = new FakeStructBlock();
                fakeStructBlock.create(loc.asBlockVector3(), loc.asBlockVector3(), this.player);
            } else {
                UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
                updateBlockPacket.blockRuntimeId = BlockStateRegistry.getRuntimeId(PNXAdapter.adapt(block.getBlockType()));
                updateBlockPacket.flags = UpdateBlockPacket.FLAG_NETWORK;
                updateBlockPacket.x = loc.getFloorX();
                updateBlockPacket.y = loc.getFloorY();
                updateBlockPacket.z = loc.getFloorZ();
                player.dataPacket(updateBlockPacket);
            }
        }
    }

    //FAWE start
    @Override
    public void sendTitle(Component title, Component sub) {
        String titleStr = WorldEditText.reduceToText(title, getLocale());
        String subStr = WorldEditText.reduceToText(sub, getLocale());
        player.sendTitle(titleStr, subStr, 0, 70, 20);
    }

    @Override
    public void unregister() {
        player.removeMetadata("WE", PNXWorldEditPlugin.getInstance());
        super.unregister();
    }
    //FAWE end
}
