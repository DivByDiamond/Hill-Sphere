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
package dev.loki.hillsphere.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.LevelFields;
import dev.loki.hillsphere.world.WorldFields;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** /hillsphere gravity: prints the gravity the server computes where the player stands. */
public final class HillSphereCommand {

    private HillSphereCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("hillsphere")
                .then(Commands.literal("gravity").executes(ctx -> {
                    final CommandSourceStack source = ctx.getSource();
                    final Vec3 pos = source.getPosition();
                    final LevelFields fields = WorldFields.peek(source.getLevel());
                    final int cores = fields == null ? 0 : fields.field().size();
                    final Gravity g = fields == null ? Gravity.VANILLA
                            : fields.field().gravityAt(new Vec3d(pos.x, pos.y, pos.z));
                    source.sendSuccess(() -> Component.literal(String.format(
                            "Hill Sphere: %d active cores here. Gravity %.2fg, down = (%.2f, %.2f, %.2f)",
                            cores, g.strength(), g.direction().x(), g.direction().y(), g.direction().z())), false);
                    return 1;
                })));
    }
}
