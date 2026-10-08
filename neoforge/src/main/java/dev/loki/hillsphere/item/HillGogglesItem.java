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
package dev.loki.hillsphere.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Goggles that show gravity fields while worn. A helmet so the game draws them on the head; they give no
 * armor and never wear out (no durability is set, so the stack cannot be damaged).
 */
public class HillGogglesItem extends ArmorItem {

    public HillGogglesItem(Properties properties) {

        super(GogglesArmor.MATERIAL, Type.HELMET, properties.stacksTo(1));
    }

    /** No "+0 Armor" line in the tooltip. */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {

        return ItemAttributeModifiers.EMPTY;
    }
}
