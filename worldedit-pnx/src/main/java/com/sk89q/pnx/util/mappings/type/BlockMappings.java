package com.sk89q.pnx.util.mappings.type;


import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BlockMappings {

    Object2ObjectOpenHashMap<String, cn.nukkit.block.BlockState> mapping1;
    Object2ObjectOpenHashMap<cn.nukkit.block.BlockState, com.sk89q.worldedit.world.block.BlockState> mapping2;

    public cn.nukkit.block.BlockState getPNXBlock(String faweBlockState) {
        final BlockState blockState = mapping1.get(faweBlockState);
        if (blockState == null) {
            return BlockAir.STATE;
        } else {
            return blockState;
        }
    }

    public com.sk89q.worldedit.world.block.BlockState getFAWEBlock(cn.nukkit.block.BlockState pnxBlockState) {
        final com.sk89q.worldedit.world.block.BlockState faweBlockState = mapping2.get(pnxBlockState);
        if (faweBlockState == null) {
            return com.sk89q.worldedit.world.block.BlockState.get("minecraft:air");
        } else {
            return faweBlockState;
        }
    }

}
