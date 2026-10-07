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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.block.FieldState;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.blockentity.HillCoreBlockEntity;
import dev.loki.hillsphere.blockentity.setting.PolarityBehaviour;
import dev.loki.hillsphere.blockentity.setting.StepSettingBehaviour;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.registry.ModBlocks;
import dev.loki.hillsphere.world.WorldFields;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server-side checks that run headless: ./gradlew :neoforge:runGameTestServer. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {

    private static final String EMPTY = "empty";
    private static final BlockPos MOTOR = new BlockPos(1, 2, 3);
    private static final BlockPos CORE = new BlockPos(2, 2, 3);

    private CoreGameTests() {
    }

    /** A motor on the core's shaft, spinning at the given speed; returns the core. */
    static HillCoreBlockEntity powered(GameTestHelper helper, int rpm, Polarity polarity, int level) {

        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        helper.setBlock(CORE, ModBlocks.HILL_CORE.get().defaultBlockState().setValue(HillCoreBlock.AXIS, Axis.X));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(rpm);
        final HillCoreBlockEntity core = (HillCoreBlockEntity) helper.getBlockEntity(CORE);
        core.configure(polarity, level);
        return core;
    }

    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "coreSpinsUpAndMakesAField")
    public static void coreSpinsUpAndMakesAField(GameTestHelper helper) {

        powered(helper, 128, Polarity.ATTRACT, 3);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(HillCoreBlock.FIELD) == FieldState.ATTRACT,
                    "the core should glow as attract once it spins");
            helper.assertTrue(WorldFields.of(helper.getLevel()).field().size() >= 1, "the core should be in the world's fields");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "redstoneSwitchesTheFieldOff")
    public static void redstoneSwitchesTheFieldOff(GameTestHelper helper) {

        powered(helper, 128, Polarity.REPEL, 2);
        helper.setBlock(CORE.above(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(HillCoreBlock.FIELD) == FieldState.OFF,
                    "a redstone signal should switch the field off");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "settingsAreIndependentAndSurviveSaving")
    public static void settingsAreIndependentAndSurviveSaving(GameTestHelper helper) {

        final HillCoreBlockEntity core = powered(helper, 16, Polarity.REPEL, 4);
        helper.assertTrue(core.getBehaviour(PolarityBehaviour.TYPE) != null, "polarity setting must be registered");
        helper.assertTrue(core.getBehaviour(StepSettingBehaviour.TYPE) != null, "level setting must be registered");
        helper.assertTrue(core.polarityValue() == Polarity.REPEL && core.getStrengthLevel() == 4, "settings must not overwrite each other");

        final CompoundTag saved = core.saveWithFullMetadata(helper.getLevel().registryAccess());
        final BlockEntity loaded = BlockEntity.loadStatic(helper.absolutePos(CORE), helper.getBlockState(CORE), saved,
                helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof HillCoreBlockEntity, "the saved core should load back");
        final HillCoreBlockEntity back = (HillCoreBlockEntity) loaded;
        helper.assertTrue(back.polarityValue() == Polarity.REPEL && back.getStrengthLevel() == 4, "settings must survive saving");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "levitationHoldsAnItemUp")
    public static void levitationHoldsAnItemUp(GameTestHelper helper) {

        powered(helper, 128, Polarity.LEVITATE, 3);
        final Vec3 start = new Vec3(2.5, 4.0, 3.5);
        helper.runAfterDelay(100, () -> {
            final ItemEntity item = helper.spawnItem(Items.COBBLESTONE, start);
            final double startY = item.getY();
            final Vec3 abs = helper.absoluteVec(start);
            helper.assertTrue(WorldFields.of(helper.getLevel()).field().liftAt(new Vec3d(abs.x, abs.y, abs.z)) > 0.9,
                    "the field should reach the item before it is checked");
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(item.getY() > startY - 0.6, "an item in full levitation should hardly fall, fell from "
                        + startY + " to " + item.getY());
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 200, batch = "withoutAFieldAnItemFalls")
    public static void withoutAFieldAnItemFalls(GameTestHelper helper) {

        final ItemEntity item = helper.spawnItem(Items.COBBLESTONE, new Vec3(2.5, 4.0, 3.5));
        final double startY = item.getY();
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(item.getY() < startY - 1.0, "control: an item without a field must fall");
            helper.succeed();
        });
    }

    /** The core sits at y 3 here, so something below it is on the far side from vanilla's point of view. */
    static void liftCore(GameTestHelper helper, Polarity polarity) {

        helper.setBlock(MOTOR.above(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        helper.setBlock(CORE.above(), ModBlocks.HILL_CORE.get().defaultBlockState().setValue(HillCoreBlock.AXIS, Axis.X));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR.above())).generatedSpeed.setValue(128);
        ((HillCoreBlockEntity) helper.getBlockEntity(CORE.above())).configure(polarity, 3);
    }

    private static void raise(GameTestHelper helper, Polarity polarity, double itemY, String why) {

        liftCore(helper, polarity);
        helper.runAfterDelay(100, () -> {
            final ItemEntity item = helper.spawnItem(Items.COBBLESTONE, new Vec3(2.5, itemY, 3.5));
            final double startY = item.getY();
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(item.getY() > startY + 0.4, why + ": went from " + startY + " to " + item.getY());
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "anAttractingCoreAboveLiftsAnItem")
    public static void anAttractingCoreAboveLiftsAnItem(GameTestHelper helper) {

        raise(helper, Polarity.ATTRACT, 1.0, "an item below an attracting core should be pulled up toward it");
    }

    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "aRepellingCoreBelowLiftsAnItem")
    public static void aRepellingCoreBelowLiftsAnItem(GameTestHelper helper) {

        raise(helper, Polarity.REPEL, 4.6, "an item above a repelling core should be pushed away, up");
    }
}
