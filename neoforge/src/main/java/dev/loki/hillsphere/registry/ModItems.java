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
import dev.loki.hillsphere.item.GogglesArmor;
import dev.loki.hillsphere.item.HillGogglesItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final DeferredItem<BlockItem> HILL_CORE = ITEMS.registerSimpleBlockItem(ModBlocks.HILL_CORE);

    public static final DeferredItem<BlockItem> DEEPSLATE_GRAVITE_ORE =
            ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_GRAVITE_ORE);
    public static final DeferredItem<BlockItem> GRAVITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVITE_BLOCK);
    public static final DeferredItem<BlockItem> HILL_PLATE = ITEMS.registerSimpleBlockItem(ModBlocks.HILL_PLATE);
    public static final DeferredItem<Item> RAW_GRAVITE = ITEMS.registerSimpleItem("raw_gravite");
    public static final DeferredItem<Item> CRUSHED_GRAVITE = ITEMS.registerSimpleItem("crushed_gravite");
    public static final DeferredItem<Item> HAUNTED_GRAVITE = ITEMS.registerSimpleItem("haunted_gravite");
    public static final DeferredItem<Item> GRAVITE_INGOT = ITEMS.registerSimpleItem("gravite_ingot");
    public static final DeferredItem<Item> GRAVITE_SHEET = ITEMS.registerSimpleItem("gravite_sheet");
    public static final DeferredItem<HillGogglesItem> HILL_GOGGLES =
            ITEMS.registerItem("hill_goggles", HillGogglesItem::new);

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Constants.MOD_ID))
                    .icon(() -> HILL_CORE.get().getDefaultInstance())
                    .displayItems(ModItems::fillTab)
                    .build());

    private ModItems() {
    }

    public static void register(IEventBus bus) {

        GogglesArmor.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static void fillTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output out) {

        out.accept(HILL_CORE);
        out.accept(HILL_GOGGLES);
        out.accept(DEEPSLATE_GRAVITE_ORE);
        out.accept(GRAVITE_BLOCK);
        out.accept(HILL_PLATE);
        out.accept(RAW_GRAVITE);
        out.accept(CRUSHED_GRAVITE);
        out.accept(HAUNTED_GRAVITE);
        out.accept(GRAVITE_INGOT);
        out.accept(GRAVITE_SHEET);
        out.accept(ModFluids.bucket());
    }
}
