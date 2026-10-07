/*
 * Copyright (C) 2026 loki
 *
 * This file is part of Hill Sphere.
 *
 * Hill Sphere is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hill Sphere is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hill Sphere. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.loki.hillsphere.registry;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.block.HillCoreBlock;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);

    public static final DeferredBlock<HillCoreBlock> HILL_CORE = BLOCKS.registerBlock("hill_core", HillCoreBlock::new,
            Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion());

    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_GRAVITE_ORE = BLOCKS.registerBlock(
            "deepslate_gravite_ore", p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE));
    public static final DeferredBlock<Block> GRAVITE_BLOCK = BLOCKS.registerSimpleBlock("gravite_block",
            Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> HILL_PLATE = BLOCKS.registerSimpleBlock("hill_plate",
            Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PURPLE));

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {

        BLOCKS.register(bus);
    }
}
