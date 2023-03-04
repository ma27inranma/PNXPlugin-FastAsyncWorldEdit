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

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.particle.PunchBlockParticle;
import cn.nukkit.math.BlockFace;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.google.common.collect.ImmutableSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.pnx.util.WorldUnloadedException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class PNXWorld extends AbstractWorld {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private WeakReference<Level> worldRef;
    //FAWE start
    private final String worldNameRef;
    //FAWE end

    /**
     * Construct the object.
     *
     * @param world the world
     */
    public PNXWorld(Level world) {
        this.worldRef = new WeakReference<>(world);
        //FAWE start
        this.worldNameRef = world.getName();
        //FAWE end
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities(Region region) {
        Level world = getLevel();
        cn.nukkit.entity.Entity[] ents = world.getEntities();
        List<com.sk89q.worldedit.entity.Entity> entities = new ArrayList<>();
        for (cn.nukkit.entity.Entity ent : ents) {
            if (region.contains(PNXAdapter.asBlockVector(ent.getLocation()))) {
                entities.add(new PNXEntity(ent));
            }
        }
        return entities;
    }

    /**
     * Get the world handle.
     *
     * @return the world
     */
    public Level getLevel() {
        return checkNotNull(worldRef.get(), "The world was unloaded and the reference is unavailable");
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities() {
        Level world = getLevel();
        cn.nukkit.entity.Entity[] ents = world.getEntities();
        List<com.sk89q.worldedit.entity.Entity> entities = new ArrayList<>();
        for (cn.nukkit.entity.Entity ent : ents) {
            entities.add(PNXAdapter.adapt(ent));
        }
        return entities;
    }

    //FAWE: createEntity was moved to IChunkExtent to prevent issues with Async Entity Add.

    /**
     * Get the world handle.
     *
     * @return the world
     */
    public Level getWorld() {
        //FAWE start
        Level tmp = worldRef.get();
        if (tmp == null) {
            tmp = PNXWorldEditPlugin.getInstance().getServer().getLevelByName(worldNameRef);
            if (tmp != null) {
                worldRef = new WeakReference<>(tmp);
            }
        }
        //FAWE end
        return checkNotNull(tmp, "The world was unloaded and the reference is unavailable");
    }

    //FAWE start

    /**
     * Get the world handle.
     *
     * @return the world
     */
    protected Level getWorldChecked() throws WorldEditException {
        Level tmp = worldRef.get();
        if (tmp == null) {
            tmp = PNXWorldEditPlugin.getInstance().getServer().getLevelByName(worldNameRef);
            if (tmp != null) {
                worldRef = new WeakReference<>(tmp);
            }
        }
        if (tmp == null) {
            throw new WorldUnloadedException(worldNameRef);
        }
        return tmp;
    }
    //FAWE end

    @Override
    public String getName() {
        //FAWE start - Throw WorldUnloadedException rather than NPE when world unloaded and attempted to be accessed
        return getWorldChecked().getName();
        //FAWE end
    }

    //FAWE start - allow history to read an unloaded world's name
    @Override
    public String getNameUnsafe() {
        return worldNameRef;
    }
    //FAWE end

    @Override
    public String getId() {
        return getWorld().getName().replace(" ", "_").toLowerCase(Locale.ROOT);
    }

    @Override
    public Path getStoragePath() {
        Path worldFolder = Path.of(getLevel().getProvider().getPath());
        switch (getWorld().getDimension()) {
            case 1:
                return worldFolder.resolve("nether");
            case 2:
                return worldFolder.resolve("the_end");
            default:
                return worldFolder.resolve("world");
        }
    }

    @Override
    public int getBlockLightLevel(BlockVector3 pt) {
        return getWorld().getBlockLightAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
    }

    @Override
    public boolean regenerate(Region region, Extent extent, RegenOptions options) {
        var maxY = region.getMaximumY();
        var minY = region.getMinimumY();
        BaseBlock[] history = new BaseBlock[16 * 16 * maxY];

        for (BlockVector2 chunk : region.getChunks()) {
            BlockVector3 min = BlockVector3.at(chunk.getBlockX() << 4, minY, chunk.getBlockZ() << 4);

            // First save all the blocks inside
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < maxY; ++y) {
                    for (int z = 0; z < 16; ++z) {
                        BlockVector3 pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;
                        history[index] = extent.getBlock(pt).toBaseBlock();
                    }
                }
            }

            try {
                getLevel().regenerateChunk(chunk.getBlockX(), chunk.getBlockZ());
            } catch (Throwable t) {
                PNXWorldEditPlugin.getInstance().getLogger().warning(
                        "Chunk generation via Nukkit raised an error",
                        t
                );
            }

            // Then restore
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < maxY; ++y) {
                    for (int z = 0; z < 16; ++z) {
                        BlockVector3 pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;
//todo 弄明白怎么写
//                        // We have to restore the block if it was outside
//                        if (!region.contains(pt)) {
//                            extent.setBlock(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ(), history[index]);
//                        } else { // Otherwise fool with history
//                            extent.enableHistory(new BlockBagChangeSet())
//                            editSession.rememberChange(pt, history[index],
//                                    editSession.rawGetBlock(pt)
//                            );
//                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 pt) {
        BlockEntity block = getLevel().getBlockEntity(new cn.nukkit.math.BlockVector3(pt.getBlockX(), pt.getBlockY(),
                pt.getBlockZ()
        ));
        if (block == null) {
            return false;
        }
        if (block instanceof InventoryHolder) {
            if (block instanceof BlockEntityChest) {
                ((BlockEntityChest) block).getRealInventory().clearAll();
            } else {
                ((InventoryHolder) block).getInventory().clearAll();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 pt) {
        //FAWE start - allow tree commands to be undone and obey region restrictions
//        return WorldEditPlugin.getInstance().getBukkitImplAdapter().generateTree(type, editSession, pt, getWorld());
        //FAWE end
        return false;
    }

    @Override
    public void dropItem(Vector3 pt, BaseItemStack item) {
        Level world = getWorld();
        world.dropItem(PNXAdapter.adapt(world, pt), PNXAdapter.adapt(item));
    }

    @Override
    public void checkLoadedChunk(BlockVector3 pt) {
        Level world = getLevel();

        if (!world.isChunkLoaded(pt.getBlockX() >> 4, pt.getBlockZ() >> 4)) {
            world.loadChunk(pt.getBlockX() >> 4, pt.getBlockZ() >> 4);
        }
    }

    @Override
    public boolean equals(Object other) {
        final Level ref = worldRef.get();
        if (ref == null) {
            return false;
        } else if (other == null) {
            return false;
        } else if ((other instanceof PNXWorld)) {
            Level otherWorld = ((PNXWorld) other).worldRef.get();
            if (otherWorld == null) {
                return false;
            }
            return ref.getName().equals(otherWorld.getName());
        } else if (other instanceof com.sk89q.worldedit.world.World) {
            return ((com.sk89q.worldedit.world.World) other).getName().equals(ref.getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getWorld().hashCode();
    }

    @Override
    public int getMaxY() {
        return getWorld().getMaxHeight();
    }

    @Override
    public int getMinY() {
        return getWorld().getMinHeight() + 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void fixAfterFastMode(Iterable<BlockVector2> chunks) {
    }

    @Override
    public boolean playEffect(Vector3 position, int type, int data) {
        return false;
    }

    //FAWE start - allow block break effect of non-legacy blocks
    @Override
    public boolean playBlockBreakEffect(Vector3 position, BlockType type) {
        Level world = getWorld();
        world.addParticle(new PunchBlockParticle(new cn.nukkit.math.Vector3(position.getX(), position.getY(), position.getZ()),
                PNXAdapter.adapt(type).getBlock(), BlockFace.UP
        ));
        return true;
    }
    //FAWE end

    @Override
    public WeatherType getWeather() {
        if (getWorld().isThundering()) {
            return WeatherTypes.THUNDER_STORM;
        } else if (getWorld().isRaining()) {
            return WeatherTypes.RAIN;
        }
        return WeatherTypes.CLEAR;
    }

    @Override
    public long getRemainingWeatherDuration() {
        if (getWorld().isThundering()) {
            return getWorld().getThunderTime();
        } else if (getWorld().isRaining()) {
            return getWorld().getRainTime();
        }
        return 0;
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            getWorld().setThundering(true);
        } else if (weatherType == WeatherTypes.RAIN) {
            getWorld().setRaining(true);
        } else {
            getWorld().setRaining(false);
            getWorld().setThundering(false);
        }
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        // Who named these methods...
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            getWorld().setThundering(true);
            getWorld().setThunderTime((int) duration);
        } else if (weatherType == WeatherTypes.RAIN) {
            getWorld().setRaining(true);
            getWorld().setRainTime((int) duration);
        } else {
            getWorld().setRaining(false);
            getWorld().setThundering(false);
            getWorld().setRainTime((int) duration);
        }
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return PNXAdapter.asBlockVector(getWorld().getSpawnLocation().getLocation());
    }

    @Override
    public void simulateBlockMine(BlockVector3 pt) {
        getWorld().useBreakOn(new cn.nukkit.math.Vector3(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()));
    }

    //FAWE start
    @Override
    public Collection<BaseItemStack> getBlockDrops(BlockVector3 position) {
        return Arrays
                .stream(getWorld()
                        .getBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ())
                        .getDrops(Item.get(0)))
                .map(PNXAdapter::adapt).collect(Collectors.toList());
    }
    //FAWE end

    @Override
    public boolean canPlaceAt(BlockVector3 position, com.sk89q.worldedit.world.block.BlockState blockState) {
        if (PNXAdapter.adapt(blockState).getBlock().canBePlaced() &&
                getWorld().getBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ()).canBeReplaced()) {
            return true;
        }
        return false;
    }

    @Override
    public com.sk89q.worldedit.world.block.BlockState getBlock(BlockVector3 position) {
        return PNXAdapter.adapt(getWorld().getBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ()));
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) {
        getWorld().setBlockStateAt(
                position.getBlockX(), position.getBlockY(), position.getBlockZ(),
                PNXAdapter.adapt(block.getBlockType())
        );
        return true;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getBlock(position).toBaseBlock();
    }

    @Override
    public Set<SideEffect> applySideEffects(
            BlockVector3 position, com.sk89q.worldedit.world.block.BlockState previousType,
            SideEffectSet sideEffectSet
    ) {
        return ImmutableSet.of();
    }

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        getWorld().useBreakOn(
                new cn.nukkit.math.Vector3(position.getBlockX(), position.getBlockY(), position.getBlockZ()),
                PNXAdapter.adapt(item.getType())
        );
        return true;
    }

    @Override
    public boolean fullySupports3DBiomes() {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return PNXAdapter.adapt(Biome.getBiome(getWorld().getBiomeId(position.getBlockX(), position.getBlockY(),
                position.getBlockZ()
        )));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        getWorld().setBiomeId(position.getBlockX(), position.getBlockY(), position.getBlockZ(),
                (byte) PNXAdapter.adapt(biome).getId()
        );
        return true;
    }

    @Override
    public void flush() {

    }

    //FAWE start

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        return setBlock(BlockVector3.at(x, y, z), block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return setBiome(BlockVector3.at(x, y, z), biome);
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        Collection<cn.nukkit.Player> players = PNXWorldEditPlugin.getInstance().getServer().getOnlinePlayers().values();
        int view = PNXWorldEditPlugin.getInstance().getServer().getViewDistance();
        for (cn.nukkit.Player player : players) {
            Position pos = player.getPosition();
            int pcx = pos.getFloorX() >> 4;
            int pcz = pos.getFloorZ() >> 4;
            if (Math.abs(pcx - chunkX) > view || Math.abs(pcz - chunkZ) > view) {
                continue;
            }
            getWorld().requestChunk(chunkX, chunkZ, player);
        }
    }

    @Override
    public IChunkGet get(final int x, final int z) {
        return new PNXGetBlocks(getWorld(), x, z);
    }

    @Override
    public void sendFakeChunk(@Nullable final Player player, final ChunkPacket packet) {

    }
    //FAWE end
}
