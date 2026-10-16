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
package dev.loki.hillsphere.gametest.side;

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.client.camera.FrameEyeMath;
import dev.loki.hillsphere.gametest.ship.ShipRig;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** The camera's eye of a body on the side of a ship: where Sable puts it, and where the clear-air check pulls it. */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SideEyeGameTests {

    /** The mixin that turns the camera's eye with the frame. */
    private static final String OURS = "dev/loki/hillsphere/mixin/client/FrameCameraMixin.class";

    /** Sable's mixin that replaces the camera's eye of a body standing on a ship. */
    private static final String SABLES = "dev/ryanhcode/sable/mixin/entity/entity_sublevel_collision/CameraMixin.class";

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

    /** A mixin without a priority has this one. */
    private static final int DEFAULT_PRIORITY = 1000;

    private SideEyeGameTests() {
    }

    /**
     * Both wrap the camera's {@code setPosition}; the mixin applied later, the one of higher priority, is the outer one.
     * Sable's must be outer: it drops the position it is handed, so as the inner one it threw the turned eye away and
     * put an upright one in its place, in the face of the ship the body stands on.
     */
    @GameTest(template = "empty", batch = "sableCarriesTheCameraBeforeTheFrameTurnsIt")
    public static void sableCarriesTheCameraBeforeTheFrameTurnsIt(GameTestHelper helper) {

        if (!ModList.get().isLoaded("sable")) {
            helper.succeed();
            return;
        }
        final int ours = priority(OURS);
        final int sables = priority(SABLES);
        helper.assertTrue(ours < sables, "the frame's camera wrap must be inside Sable's: priority " + ours + " vs " + sables);
        helper.succeed();
    }

    /** A head pulled back out of a ship's wall stays by the body: the ship's hit is measured where the ship is. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "theClearEyeOfAShipWallStaysByTheBody")
    public static void theClearEyeOfAShipWallStaysByTheBody(GameTestHelper helper) {

        if (!ModList.get().isLoaded("sable")) {
            helper.succeed();
            return;
        }
        final List<BlockPos> wall = new ArrayList<>();
        for (int y = 1; y <= 5; y++) {
            for (int z = 1; z <= 5; z++) {
                wall.add(0, new BlockPos(3, y, z));
                helper.setBlock(wall.get(0), Blocks.STONE);
            }
        }
        final ShipRig rig = ShipRig.assemble(helper, wall.toArray(BlockPos[]::new));
        helper.runAfterDelay(2, () -> {
            try {
                final Vec3 middle = helper.absoluteVec(new Vec3(4.9, 3.5, 3.5));
                final Vec3 target = helper.absoluteVec(new Vec3(2.5, 3.5, 3.5));
                final Vec3 eye = FrameEyeMath.clearEye(helper.getLevel(), null, middle, target);
                final String where = "eye " + eye + " middle " + middle + " wanted " + target;
                helper.assertTrue(eye.distanceTo(middle) <= target.distanceTo(middle), "the eye must stay by the body, " + where);
                helper.assertTrue(!rig.solidAt(eye), "the eye must not be in the ship's wall, " + where);
            } finally {
                rig.remove();
            }
            helper.succeed();
        });
    }

    /** The priority a mixin class declares, read from its bytes: a mixin class itself must never be loaded. */
    private static int priority(String resource) {

        try (InputStream in = SideEyeGameTests.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("no class file " + resource);
            }
            final ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
            final List<AnnotationNode> annotations = new ArrayList<>();
            if (node.invisibleAnnotations != null) {
                annotations.addAll(node.invisibleAnnotations);
            }
            for (final AnnotationNode annotation : annotations) {
                for (int i = 0; MIXIN.equals(annotation.desc) && annotation.values != null && i < annotation.values.size(); i += 2) {
                    if ("priority".equals(annotation.values.get(i))) {
                        return (Integer) annotation.values.get(i + 1);
                    }
                }
            }
            return DEFAULT_PRIORITY;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
