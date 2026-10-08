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
package dev.loki.hillsphere.gametest;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.item.HillGogglesItem;
import dev.loki.hillsphere.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The goggles are a helmet that protects nothing and never wears out. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GogglesGameTests {

    private GogglesGameTests() {
    }

    @GameTest(template = "empty", batch = "gogglesGoOnTheHead")
    public static void gogglesGoOnTheHead(GameTestHelper helper) {

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack goggles = new ItemStack(ModItems.HILL_GOGGLES.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, goggles);
        goggles.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        final ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
        helper.assertTrue(worn.getItem() instanceof HillGogglesItem, "using the goggles should put them on, head has " + worn);
        helper.assertTrue(player.getArmorValue() == 0, "goggles should give no armor, give " + player.getArmorValue());
        helper.assertFalse(worn.isDamageableItem(), "goggles should not wear out");
        helper.assertTrue(worn.getMaxStackSize() == 1, "goggles should not stack");
        helper.succeed();
    }
}
