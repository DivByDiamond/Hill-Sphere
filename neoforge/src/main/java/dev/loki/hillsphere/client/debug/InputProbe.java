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
package dev.loki.hillsphere.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/** What the local player's controls say, for the debug log; only called on the client. */
public final class InputProbe {

    private InputProbe() {
    }

    public static String describe(Player player) {

        if (!(player instanceof LocalPlayer local)) {
            return "input:remote";
        }
        return String.format(java.util.Locale.ROOT, "input[jump=%s sneak=%s fwd=%.1f side=%.1f autoJump=%s flying=%s mayfly=%s mode=%s]",
                local.input.jumping, local.input.shiftKeyDown, local.input.forwardImpulse, local.input.leftImpulse,
                Minecraft.getInstance().options.autoJump().get(), local.getAbilities().flying, local.getAbilities().mayfly,
                Minecraft.getInstance().gameMode == null ? "?" : Minecraft.getInstance().gameMode.getPlayerMode());
    }
}
