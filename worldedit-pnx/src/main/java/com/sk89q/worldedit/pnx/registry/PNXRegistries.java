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

import com.sk89q.worldedit.world.registry.BiomeRegistry;
import com.sk89q.worldedit.world.registry.BlockCategoryRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BundledRegistries;
import com.sk89q.worldedit.world.registry.ItemCategoryRegistry;

/**
 * World data for the Bukkit platform.
 */
public class PNXRegistries extends BundledRegistries {

    private static final PNXRegistries INSTANCE = new PNXRegistries();
    private final BlockRegistry blockRegistry = new PNXBlockRegistry();
    private final BiomeRegistry biomeRegistry = new PNXBiomeRegistry();
    private final BlockCategoryRegistry blockCategoryRegistry = new PNXBlockCategoryRegistry();
    private final ItemCategoryRegistry itemCategoryRegistry = new PNXItemCategoryRegistry();

    /**
     * Create a new instance.
     */
    PNXRegistries() {
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    @Override
    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

    @Override
    public BlockCategoryRegistry getBlockCategoryRegistry() {
        return blockCategoryRegistry;
    }

    @Override
    public ItemCategoryRegistry getItemCategoryRegistry() {
        return itemCategoryRegistry;
    }

    /**
     * Get a static instance.
     *
     * @return an instance
     */
    public static PNXRegistries getInstance() {
        return INSTANCE;
    }

}
