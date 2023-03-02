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

package com.sk89q.worldedit.pnx.registry;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.blockentity.BlockEntityContainer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.pnx.PNXAdapter;
import com.sk89q.worldedit.pnx.PNXWorldEditPlugin;
import com.sk89q.worldedit.pnx.data.FileRegistries;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BundledBlockRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PNXBlockRegistry extends BundledBlockRegistry {

    private PNXBlockMaterial[] materialMap;

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        if (blockType == null) {
            return null;
        }
        Block mat = Block.get(0);
        if (!isRemain(blockType.getId())) {
            mat = PNXAdapter.adapt(blockType).getBlock();
        }
        if (materialMap == null) {
            materialMap = new PNXBlockMaterial[Block.MAX_BLOCK_ID];
        }
        PNXBlockMaterial result = materialMap[mat.getId()];
        if (result == null) {
            result = new PNXBlockMaterial(mat);
            materialMap[mat.getId()] = result;
        }
        return result;
    }

    //FAWE start
    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockState state) {
        if (state == null) {
            return null;
        }
        cn.nukkit.blockstate.BlockState nkBlockState;
        if ((nkBlockState = PNXAdapter.adapt(state)) != null) {
            PNXBlockMaterial result = materialMap[nkBlockState.getBlockId()];
            Block mat = nkBlockState.getBlock();
            if (result == null) {
                result = new PNXBlockMaterial(mat);
                materialMap[mat.getId()] = result;
            }
            return result;
        } else {
            return null;
        }
    }
    //FAWE end

    public static class PNXBlockMaterial implements BlockMaterial {

        private final Block material;

        public PNXBlockMaterial(Block block) {
            this.material = block;
            if (block.level == null) {
                block.setLevel(PNXWorldEditPlugin.getInstance().getServer().getDefaultLevel());
            }
        }

        @Override
        public boolean isAir() {
            return material.getId() == BlockID.AIR;
        }

        @Override
        public boolean isFullCube() {
            return this.material.isFullBlock();
        }

        @Override
        public boolean isOpaque() {
            return !this.material.isTransparent();
        }

        @Override
        public boolean isPowerSource() {
            return this.material.isPowerSource();
        }

        @Override
        public boolean isLiquid() {
            return this.material instanceof BlockLiquid;
        }

        @Override
        public boolean isSolid() {
            return material.isSolid();
        }

        @Override
        public float getHardness() {
            return (float) this.material.getHardness();
        }

        @Override
        public float getResistance() {
            return (float) this.material.getResistance();
        }

        @Override
        public float getSlipperiness() {
            return (float) this.material.getFrictionFactor();
        }

        @Override
        public int getLightValue() {
            return this.material.getLightLevel();
        }

        @Override
        public boolean isFragileWhenPushed() {
            return this.material.breaksWhenMoved();
        }

        @Override
        public boolean isUnpushable() {
            return !this.material.canBePushed();
        }

        @Override
        public boolean isTicksRandomly() {
            return this.material.canRandomTick();
        }

        @Override
        public boolean isMovementBlocker() {
            return !this.material.canPassThrough();
        }

        @Override
        public boolean isBurnable() {
            return this.material.getBurnAbility() > 0;
        }

        @Override
        public boolean isToolRequired() {
            return this.material.getToolType() != 0;
        }

        @Override
        public boolean isReplacedDuringPlacement() {
            return this.material.canBeReplaced();
        }

        @Override
        public boolean isTranslucent() {
            return material.isTransparent();
        }

        @Override
        public boolean hasContainer() {
            return this.material instanceof BlockEntityContainer;
        }

        @Override
        public int getLightOpacity() {
            return this.material.getLightFilter();
        }

        @Override
        public boolean isTile() {
            return this.material.getLevelBlockEntity() != null;
        }

        @org.jetbrains.annotations.Nullable
        @Override
        public CompoundTag getDefaultTile() {
            return null;
        }

        @Override
        public int getMapColor() {
            return this.material.getColor().getRGB();
        }

    }

    private final Set<String> remain = Set.of(
            "minecraft:__reserved__",
            "minecraft:air",
            "minecraft:cave_air",
            "minecraft:void_air"
    );

    @Override
    public Collection<String> values() {
        var list = new ArrayList<String>();
        for (Map.Entry<String, FileRegistries.BlockManifest> manifestEntry : PNXWorldEditPlugin
                .getInstance()
                .getFileRegistries()
                .getDataFile().blocks.entrySet()) {
            list.add(manifestEntry.getValue().defaultstate);
        }
        return list;
    }

    private boolean isRemain(String id) {
        return remain.contains(id);
    }

    @Nullable
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        if (isRemain(blockType.getId())) {
            return Map.of();
        }
        final FileRegistries.BlockManifest blockManifest = PNXWorldEditPlugin
                .getInstance()
                .getFileRegistries()
                .getDataFile().blocks.get(blockType.getId());
        if (blockManifest != null) {
            Map<String, FileRegistries.BlockProperty> properties = blockManifest.properties;
            Maps.EntryTransformer<String, FileRegistries.BlockProperty, Property<?>> entryTransform = (key, value) -> createProperty(
                    value.type,
                    key,
                    value.values
            );
            return ImmutableMap.copyOf(Maps.transformEntries(properties, entryTransform));
        } else {
            return null;
        }
    }

    private Property<?> createProperty(String type, String key, List<String> values) {
        switch (type) {
            case "int" -> {
                List<Integer> fixedValues = values.stream().map(Integer::parseInt).collect(Collectors.toList());
                return new IntegerProperty(key, fixedValues);
            }
            case "bool" -> {
                List<Boolean> fixedValues = values.stream().map(Boolean::parseBoolean).collect(Collectors.toList());
                return new BooleanProperty(key, fixedValues);
            }
            case "enum" -> {
                return new EnumProperty(key, values);
            }
            case "direction" -> {
                List<Direction> fixedValues = values
                        .stream()
                        .map(String::toUpperCase)
                        .map(Direction::valueOf)
                        .collect(Collectors.toList());
                return new DirectionalProperty(key, fixedValues);
            }
            default -> throw new RuntimeException("Failed to create property");
        }
    }
    //FAWE end
}
