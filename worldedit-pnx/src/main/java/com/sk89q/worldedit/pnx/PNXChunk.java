package com.sk89q.worldedit.pnx;

import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.format.FullChunk;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class PNXChunk implements IChunkGet {

    private final FullChunk chunk;

    public PNXChunk(FullChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public boolean hasSection(final int layer) {
        return true;
    }

    @Override
    public char[] load(final int layer) {
        return new char[0];
    }

    @Nullable
    @Override
    public char[] loadIfPresent(final int layer) {
        return new char[0];
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        var map = new HashMap<BlockVector3, CompoundTag>();
        this.chunk.getBlockEntities().values()
                .forEach(entity -> map.put(
                        BlockVector3.at(entity.getFloorX(), entity.getFloorY(), entity.getFloorZ()),
                        new CompoundTag(NBTConverter.fromNative(entity.namedTag))
                ));
        return map;
    }

    @Override
    public CompoundTag getTile(final int x, final int y, final int z) {
        return new CompoundTag(NBTConverter.fromNative(this.chunk.getTile(x & 15, y, z & 15).namedTag));
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return this.chunk.getEntities().values().stream()
                .map(entity -> new CompoundTag(NBTConverter.fromNative(entity.namedTag)))
                .collect(Collectors.toSet());
    }

    @Override
    public void removeSectionLighting(final int layer, final boolean sky) {

    }

    @Override
    public boolean trim(final boolean aggressive, final int layer) {
        return false;
    }

    @Override
    public IBlocks reset() {
        return null;
    }

    @Override
    public int getSectionCount() {
        return this.chunk.getChunkSectionCount();
    }

    @Override
    public int getMaxSectionPosition() {
        if (this.chunk.isOverWorld()) {
            return 20;
        }
        return 16;
    }

    @Override
    public int getMinSectionPosition() {
        if (this.chunk.isOverWorld()) {
            return -4;
        }
        return 0;
    }

    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        return this.getBlock(x, y, z).toBaseBlock();
    }

    @Override
    public BiomeType getBiomeType(final int x, final int y, final int z) {
        return PNXAdapter.adapt(Biome.getBiome(this.chunk.getBiomeId(x & 15, y, z & 15)));
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        return PNXAdapter.adapt(this.chunk.getBlockState(x & 15, y, z & 15));
    }

    @Override
    public int getSkyLight(final int x, final int y, final int z) {
        return this.chunk.getBlockSkyLight(x & 15, y, z & 15);
    }

    @Override
    public int getEmittedLight(final int x, final int y, final int z) {
        return 0;
    }

    @Override
    public int[] getHeightMap(final HeightMapType type) {
        var map = new int[16];
        for (int i = 0; i < 16; ++i) {
            map[i] = this.chunk.getHeightMap(i, i);
        }
        return map;
    }

    @Override
    public <T extends Future<T>> T call(final IChunkSet set, final Runnable finalize) {
        return null;
    }

    @Override
    public CompoundTag getEntity(final UUID uuid) {
        return new CompoundTag(this.chunk.getEntities().values().stream()
                .filter(entity -> entity.getUniqueId().equals(uuid))
                .map(entity -> NBTConverter.fromNative(entity.namedTag))
                .toList().get(0));
    }

    @Override
    public boolean isCreateCopy() {
        return false;
    }

    @Override
    public void setCreateCopy(final boolean createCopy) {

    }

    @Override
    public void setLightingToGet(final char[][] lighting, final int startSectionIndex, final int endSectionIndex) {

    }

    @Override
    public void setSkyLightingToGet(final char[][] lighting, final int startSectionIndex, final int endSectionIndex) {
        System.out.println(lighting.length);
        System.out.println(lighting[0].length);
    }

    @Override
    public void setHeightmapToGet(final HeightMapType type, final int[] data) {
        if (data.length == 16) {
            for (int i = 0; i < 16; ++i) {
                this.chunk.setHeightMap(i, i, data[i]);
            }
        }
    }

    @Override
    public int getMaxY() {
        if (this.chunk.isOverWorld()) {
            return 384;
        }
        return 256;
    }

    @Override
    public int getMinY() {
        if (this.chunk.isOverWorld()) {
            return -63;
        }
        return 0;
    }

    @Override
    public boolean trim(final boolean aggressive) {
        return false;
    }

}
