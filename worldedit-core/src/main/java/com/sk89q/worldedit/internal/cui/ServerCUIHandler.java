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

package com.sk89q.worldedit.internal.cui;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;

/**
 * Handles creation of server-side CUI systems.
 */
public class ServerCUIHandler {

    private ServerCUIHandler() {
    }

    public static int getMaxServerCuiSize() {
        return 1000;
    }

    /**
     * Creates a structure block that shows the region.
     *
     * <p>
     * Null symbolises removal of the CUI.
     * </p>
     *
     * @param player The player to create the structure block for.
     * @return The structure block, or null
     */
    @Nullable
    public static BaseBlock createStructureBlock(Player player) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
        RegionSelector regionSelector = session.getRegionSelector(player.getWorld());

        int startX;
        int startY;
        int startZ;
        int endX;
        int endY;
        int endZ;

        if (regionSelector instanceof CuboidRegionSelector) {
            if (regionSelector.isDefined()) {
                try {
                    CuboidRegion region = ((CuboidRegionSelector) regionSelector).getRegion();

                    startX = region.getMinimumPoint().getBlockX();
                    startY = region.getMinimumPoint().getBlockY();
                    startZ = region.getMinimumPoint().getBlockZ();
                    endX = region.getMaximumPoint().getBlockX();
                    endY = region.getMaximumPoint().getBlockY();
                    endZ = region.getMaximumPoint().getBlockZ();

                } catch (IncompleteRegionException e) {
                    // This will never happen.
                    e.printStackTrace();
                    return null;
                }
            } else {
                CuboidRegion region = ((CuboidRegionSelector) regionSelector).getIncompleteRegion();
                BlockVector3 point;
                if (region.getPos1() != null) {
                    point = region.getPos1();
                } else if (region.getPos2() != null) {
                    point = region.getPos2();
                } else {
                    // No more selection
                    return null;
                }

                // Just select the point.
                startX = point.getBlockX();
                startY = point.getBlockY();
                startZ = point.getBlockZ();
                endX = point.getBlockX();
                endY = point.getBlockY();
                endZ = point.getBlockZ();
            }
        } else {
            // We only support cuboid regions right now.
            return null;
        }

        //FAWE start - CBT > Map<String, Tag>
        CompoundBinaryTag.Builder structureTag = CompoundBinaryTag.builder();

        //FAWE start - see comment of CBT
        structureTag.putString("name", "worldedit:" + player.getName());
        structureTag.putString("author", player.getName());
        structureTag.putString("metadata", "");
        structureTag.putInt("x", startX);
        structureTag.putInt("y", startY);
        structureTag.putInt("z", startZ);
        structureTag.putInt("posX", endX);
        structureTag.putInt("posY", endY);
        structureTag.putInt("posZ", endZ);
        structureTag.putInt("sizeX", endX);
        structureTag.putInt("sizeY", endY);
        structureTag.putInt("sizeZ", endZ);
        structureTag.putString("rotation", "NONE");
        structureTag.putString("mirror", "NONE");
        structureTag.putString("mode", "SAVE");
        structureTag.putByte("ignoreEntities", (byte) 1);
        structureTag.putByte("showboundingbox", (byte) 1);
        structureTag.putString("id", BlockTypes.STRUCTURE_BLOCK.getId());

        return BlockTypes.STRUCTURE_BLOCK.getDefaultState().toBaseBlock(structureTag.build());
        //FAWE end
    }

}
