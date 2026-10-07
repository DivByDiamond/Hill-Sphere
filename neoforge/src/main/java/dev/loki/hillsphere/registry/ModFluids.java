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

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Molten gravite: the liquid metal made in a mixer and poured onto an emerald to cast an ingot. */
public final class ModFluids {

    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Constants.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Constants.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register("molten_gravite",
            () -> new FluidType(FluidType.Properties.create().density(3000).viscosity(6000).temperature(1300)
                    .lightLevel(8).sound(SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA)
                    .sound(SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOURCE =
            FLUIDS.register("molten_gravite", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING =
            FLUIDS.register("flowing_molten_gravite", () -> new BaseFlowingFluid.Flowing(properties()));
    public static final DeferredBlock<LiquidBlock> BLOCK = BLOCKS.registerBlock("molten_gravite",
            p -> new LiquidBlock(SOURCE.get(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)
                    .mapColor(MapColor.COLOR_PURPLE).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BucketItem> BUCKET = ITEMS.registerItem("molten_gravite_bucket",
            p -> new BucketItem(SOURCE.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));

    private ModFluids() {
    }

    private static BaseFlowingFluid.Properties properties() {

        return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING).block(BLOCK).bucket(BUCKET)
                .slopeFindDistance(2).levelDecreasePerBlock(2);
    }

    public static void register(IEventBus bus) {

        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    /** The bucket as an item, for creative tabs. */
    public static Item bucket() {

        return BUCKET.get();
    }
}
