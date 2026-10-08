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

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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

    private ModItems() {
    }

    public static void register(IEventBus bus) {

        GogglesArmor.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModItems::fillTabs);
    }

    private static void fillTabs(BuildCreativeModeTabContentsEvent event) {

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(HILL_CORE);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(DEEPSLATE_GRAVITE_ORE);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(GRAVITE_BLOCK);
            event.accept(HILL_PLATE);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_GRAVITE);
            event.accept(CRUSHED_GRAVITE);
            event.accept(HAUNTED_GRAVITE);
            event.accept(GRAVITE_INGOT);
            event.accept(GRAVITE_SHEET);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(HILL_GOGGLES);
            event.accept(ModFluids.bucket());
        }
    }
}
