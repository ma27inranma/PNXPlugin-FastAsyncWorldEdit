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
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.customblock.CustomBlock;
import cn.nukkit.command.CommandSender;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.level.Position;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.Identifier;
import com.sk89q.pnx.util.mappings.MappingRegistries;
import com.sk89q.pnx.util.mappings.type.ItemMappings;
import com.sk89q.worldedit.NotABlockException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlayerProxy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.pnx.data.FileRegistries;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.item.ItemType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public final class PNXAdapter {

    private PNXAdapter() {
    }

    private static final ParserContext TO_BLOCK_CONTEXT = new ParserContext();

    static {
        TO_BLOCK_CONTEXT.setRestricted(false);
    }

    /**
     * Checks equality between a WorldEdit BlockType and a PNX Material.
     *
     * @param blockType The WorldEdit BlockType
     * @param type      The PNX Material
     * @return If they are equal
     */
    public static boolean equals(BlockType blockType, Item type) {
        final BlockType blockType2 = asItemType(type).getBlockType();
        if (blockType2 != null) {
            return blockType == blockType2;
        } else {
            return false;
        }
    }

    /**
     * Convert any WorldEdit world into an equivalent wrapped PNX world.
     *
     * <p>If a matching world cannot be found, a {@link RuntimeException}
     * will be thrown.</p>
     *
     * @param world the world
     * @return a wrapped PNX world
     */
    public static PNXWorld asPNXWorld(World world) {
        if (world instanceof PNXWorld) {
            return (PNXWorld) world;
        } else {
            PNXWorld PNXWorld = PNXWorldEditPlugin.getInstance().getInternalPlatform().matchWorld(world);
            if (PNXWorld == null) {
                throw new RuntimeException("World '" + world.getName() + "' has no matching version in PNX");
            }
            return PNXWorld;
        }
    }

    /**
     * Create a WorldEdit world from a PNX world.
     *
     * @param world the PNX world
     * @return a WorldEdit world
     */
    public static World adapt(cn.nukkit.level.Level world) {
        checkNotNull(world);
        return new PNXWorld(world);
    }

    /**
     * Create a WorldEdit Actor from a PNX CommandSender.
     *
     * @param sender The PNX CommandSender
     * @return The WorldEdit Actor
     */
    public static Actor adapt(CommandSender sender) {
        return PNXWorldEditPlugin.getInstance().wrapCommandSender(sender);
    }

    /**
     * Create a WorldEdit Player from a PNX Player.
     *
     * @param player The PNX player
     * @return The WorldEdit player
     */
    public static PNXPlayer adapt(Player player) {
        return PNXWorldEditPlugin.getInstance().wrapPlayer(player);
    }

    /**
     * Create a PNX CommandSender from a WorldEdit Actor.
     *
     * @param actor The WorldEdit actor
     * @return The PNX command sender
     */
    public static CommandSender adapt(Actor actor) {
        if (actor instanceof com.sk89q.worldedit.entity.Player) {
            return adapt((com.sk89q.worldedit.entity.Player) actor);
        } else if (actor instanceof PNXBlockCommandSender) {
            return ((PNXBlockCommandSender) actor).getSender();
        }
        return ((PNXCommandSender) actor).getSender();
    }

    /**
     * Create a PNX Player from a WorldEdit Player.
     *
     * @param player The WorldEdit player
     * @return The PNX player
     */
    public static Player adapt(com.sk89q.worldedit.entity.Player player) {
        //FAWE start - Get player from PlayerProxy instead of PNXPlayer if null
        player = PlayerProxy.unwrap(player);
        return player == null ? null : ((PNXPlayer) player).getPlayer();
        //FAWE end
    }

    /**
     * Create a WorldEdit Direction from a PNX BlockFace.
     *
     * @param face the PNX BlockFace
     * @return a WorldEdit direction
     */
    public static Direction adapt(@Nullable BlockFace face) {
        if (face == null) {
            return null;
        }
        switch (face) {
            case NORTH:
                return Direction.NORTH;
            case SOUTH:
                return Direction.SOUTH;
            case WEST:
                return Direction.WEST;
            case EAST:
                return Direction.EAST;
            case DOWN:
                return Direction.DOWN;
            case UP:
            default:
                return Direction.UP;
        }
    }

    /**
     * Create a PNX world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a PNX world
     */
    public static cn.nukkit.level.Level adapt(World world) {
        checkNotNull(world);
        if (world instanceof PNXWorld) {
            return ((PNXWorld) world).getWorld();
        } else {
            cn.nukkit.level.Level match = Server.getInstance().getLevelByName(world.getName());
            if (match != null) {
                return match;
            } else {
                throw new IllegalArgumentException("Can't find a Bukkit world for " + world);
            }
        }
    }

    /**
     * Create a WorldEdit location from a PNX location.
     *
     * @param location the PNX location
     * @return a WorldEdit location
     */
    public static com.sk89q.worldedit.util.Location adapt(cn.nukkit.level.Location location) {
        checkNotNull(location);
        Vector3 position = asVector(location);
        return new com.sk89q.worldedit.util.Location(
                adapt(location.getLevel()),
                position,
                (float) location.getYaw(),
                (float) location.getPitch()
        );
    }

    /**
     * Create a PNX location from a WorldEdit location.
     *
     * @param location the WorldEdit location
     * @return a PNX location
     */
    public static cn.nukkit.level.Location adapt(Location location) {
        checkNotNull(location);
        Vector3 position = location;
        return new cn.nukkit.level.Location(
                position.getX(), position.getY(), position.getZ(),
                location.getYaw(),
                location.getPitch(),
                adapt((World) location.getExtent())
        );
    }

    /**
     * Create a PNX location from a WorldEdit position with a PNX world.
     *
     * @param world    the PNX world
     * @param position the WorldEdit position
     * @return a PNX location
     */
    public static cn.nukkit.level.Location adapt(cn.nukkit.level.Level world, Vector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return new cn.nukkit.level.Location(
                position.getX(), position.getY(), position.getZ(), world
        );
    }

    /**
     * Create a PNX location from a WorldEdit position with a PNX world.
     *
     * @param world    the PNX world
     * @param position the WorldEdit position
     * @return a PNX location
     */
    public static cn.nukkit.level.Location adapt(cn.nukkit.level.Level world, BlockVector3 position) {
        checkNotNull(world);
        checkNotNull(position);
        return new cn.nukkit.level.Location(
                position.getX(), position.getY(), position.getZ(), world
        );
    }

    /**
     * Create a PNX location from a WorldEdit location with a PNX world.
     *
     * @param world    the PNX world
     * @param location the WorldEdit location
     * @return a PNX location
     */
    public static cn.nukkit.level.Location adapt(cn.nukkit.level.Level world, Location location) {
        checkNotNull(world);
        checkNotNull(location);
        return new cn.nukkit.level.Location(
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(),
                location.getPitch(),
                world
        );
    }

    /**
     * Create a WorldEdit Vector from a PNX location.
     *
     * @param location The PNX location
     * @return a WorldEdit vector
     */
    public static Vector3 asVector(cn.nukkit.level.Location location) {
        checkNotNull(location);
        return Vector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit BlockVector from a PNX location.
     *
     * @param location The PNX location
     * @return a WorldEdit vector
     */
    public static BlockVector3 asBlockVector(cn.nukkit.level.Location location) {
        checkNotNull(location);
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Create a WorldEdit entity from a PNX entity.
     *
     * @param entity the PNX entity
     * @return a WorldEdit entity
     */
    public static Entity adapt(cn.nukkit.entity.Entity entity) {
        checkNotNull(entity);
        return new PNXEntity(entity);
    }

    /**
     * Create a WorldEdit entity from a PNX entity.
     *
     * @param entity the PNX entity
     * @return a WorldEdit entity
     */
    public static Entity adapt(cn.nukkit.blockentity.BlockEntity entity) {
        checkNotNull(entity);
        return new PNXEntity(entity);
    }

    /**
     * Create a PNX Material form a WorldEdit ItemType.
     *
     * @param itemType The WorldEdit ItemType
     * @return The PNX Material
     */
    public static Item adapt(ItemType itemType) {
        return MappingRegistries.ITEM.getMapping().inverse().get(itemType).getItem();
    }

    /**
     * Create a PNX BlockState form a WorldEdit BlockType.
     *
     * @param blockType The WorldEdit BlockType
     * @return The PNX BlockState
     */
    @NotNull
    public static cn.nukkit.block.BlockState adapt(BlockType blockType) {
        final FileRegistries.BlockManifest blockManifest = PNXWorldEditPlugin
                .getInstance()
                .getFileRegistries()
                .getDataFile().blocks.get(blockType.getId());
        if (blockManifest != null) {
            return MappingRegistries.BLOCKS.getPNXBlock(blockManifest.defaultstate);
        } else {
            return BlockAir.STATE;
        }
    }

    /**
     * Create a WorldEdit BiomeType from a PNX one.
     *
     * @param biome PNX Biome
     * @return WorldEdit BiomeType
     */
    public static BiomeType adapt(int biome) {
        return MappingRegistries.BIOME.get().get(biome);
    }

    public static int adapt(BiomeType biomeType) {
        if (!biomeType.getId().startsWith("minecraft:")) {
            throw new IllegalArgumentException("PNX only supports vanilla biomes");
        }
        return MappingRegistries.BIOME.get().inverse().get(biomeType);
    }

    /**
     * Create a WorldEdit EntityType from a PNX one.
     *
     * @param entityType PNX EntityType
     * @return WorldEdit EntityType
     */
    public static EntityType adaptEntityType(cn.nukkit.entity.Entity entityType) {
        var id = entityType.getIdentifier();
        return EntityTypes.get(id.toLowerCase(Locale.ROOT));
    }

    public static cn.nukkit.entity.Entity adaptEntityType(EntityType entityType) {
        if (!entityType.getId().startsWith("minecraft:")) {
            throw new IllegalArgumentException("PNX only supports vanilla entities");
        }
        return cn.nukkit.entity.Entity.createEntity(new Identifier(entityType.getId()), new Position(0, 0, 0));
    }

    /**
     * Converts a Material to a BlockType.
     *
     * @param material The material
     * @return The blocktype
     */
    @Nullable
    public static BlockType asBlockType(Block material) {
        checkNotNull(material);
        //FAWE start - logic moved to IPNXAdapter
        return BlockTypes.get(material.getId());
        //FAWE end
    }

    /**
     * Converts a Material to a ItemType.
     *
     * @param material The material
     * @return The itemtype
     */
    @Nullable
    public static ItemType asItemType(Item material) {
        //FAWE start - logic moved to IPNXAdapter
        var result = MappingRegistries.ITEM.getMapping().get(ItemMappings.HashItem.of(material));
        if (result == null) {
            material = material.clone();
            material.setDamage(MappingRegistries.ITEM.getItemDamageMapping().get(material.getId()).intValue());
            return MappingRegistries.ITEM.getMapping().get(ItemMappings.HashItem.of(material));
        } else {
            return result;
        }
        //FAWE end
    }

    /**
     * Create a WorldEdit BlockState from a PNX BlockData.
     *
     * @param blockData The PNX BlockData
     * @return The WorldEdit BlockState
     */
    public static BlockState adapt(@Nonnull cn.nukkit.block.BlockState blockData) {
        return MappingRegistries.BLOCKS.getFAWEBlock(blockData);
    }

    /**
     * Create a PNX BlockState from a WorldEdit BlockState.
     *
     * @param block The WorldEdit BlockState
     * @return The PNX BlockState
     */
    public static cn.nukkit.block.BlockState adapt(@Nonnull BlockState block) {
        return MappingRegistries.BLOCKS.getPNXBlock(block.getAsString());
    }

    /**
     * Create a WorldEdit BlockState from a PNX Block.
     *
     * @param block the PNX Block
     * @return The WorldEdit BlockState
     * @throws WorldEditException the world edit exception
     */
    public static BlockState adapt(Block block) throws WorldEditException {
        checkNotNull(block);
        if (block instanceof CustomBlock) {
            BlockState.get("minecraft:air");
        }
        return adapt(block.getBlockState());
    }

    /**
     * Create a WorldEdit BlockState from a PNX Item
     *
     * @param itemStack The PNX Item
     * @return The WorldEdit BlockState
     */
    public static BlockState asBlockState(Item itemStack) throws WorldEditException {
        checkNotNull(itemStack);
        if (itemStack instanceof ItemBlock itemBlock) {
            return adapt(itemBlock.getBlock());
        } else {
            throw new NotABlockException();
        }
    }

    /**
     * Create a WorldEdit BaseItemStack from a PNX Item.
     *
     * @param item The PNX Item
     * @return The WorldEdit BaseItemStack
     */
    public static BaseItemStack adapt(Item item) {
        checkNotNull(item);
        if (!new Identifier(item.getId()).getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            BlockState.get("minecraft:air");
        }
        return new PNXItemStack(item);
    }

    /**
     * Create a PNX Item from a WorldEdit BaseItemStack.
     *
     * @param item The WorldEdit BaseItemStack
     * @return The PNX Item
     */
    public static Item adapt(BaseItemStack item) {
        checkNotNull(item);
        if (item instanceof PNXItemStack) {
            return ((PNXItemStack) item).getPNXItem();
        }
        var i = adapt(item.getType()).clone();
        i.setCount(item.getAmount());
        return i;
    }

}
