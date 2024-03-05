package com.sk89q.pnx.util.mappings;


import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.pnx.util.mappings.loader.BiomeRegistryLoader;
import com.sk89q.pnx.util.mappings.populator.BlockRegistryPopulator;
import com.sk89q.pnx.util.mappings.populator.ItemRegistryPopulator;
import com.sk89q.pnx.util.mappings.type.BlockMappings;
import com.sk89q.pnx.util.mappings.type.ItemMappings;
import com.sk89q.worldedit.world.biome.BiomeType;

public class MappingRegistries {

    public static final Gson JSON_MAPPER = new GsonBuilder().create();

    /**
     * A mapped registry which stores Java biome identifiers and their Bedrock biome identifier.
     */
    public static final SimpleMappingRegistry<HashBiMap<Integer, BiomeType>> BIOME = SimpleMappingRegistry.create(
            "mappings/biomes.json",
            BiomeRegistryLoader::new
    );

    /**
     * A versioned registry which holds {@link BlockMappings} for each version. These block mappings contain
     * primarily Bedrock version-specific data.
     */
    public static final BlockMappings BLOCKS = BlockRegistryPopulator.registerBlockMappings();
    public static final ItemMappings ITEM = ItemRegistryPopulator.load();

}
