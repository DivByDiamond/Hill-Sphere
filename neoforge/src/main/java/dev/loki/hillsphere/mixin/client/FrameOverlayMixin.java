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
package dev.loki.hillsphere.mixin.client;

import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The "inside a block" screen overlay probes eight points around the player's x and z and the eye's y,
 * all against world axes: on a wall the feet's coordinate along the wall's normal lies on the face of the
 * block below, so the probe lands in the wall and the screen goes dark with planks. In a turned frame the
 * probe is taken around the real eye instead: the feet plus the eye height along the frame's "up", since
 * {@code getEyePosition} itself still adds the eye height along world Y and so lands in the wall or ceiling.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class FrameOverlayMixin {

    /** Half the span of the probe around the eye, in blocks. */
    private static final double SPAN = 0.1;

    @Inject(method = "getOverlayBlock", at = @At("HEAD"), cancellable = true)
    private static void hillsphereProbeAtEye(Player player, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {

        final GravityFrame frame = LivingGravity.frameOf(player);
        if (frame.isVanilla()) {
            return;
        }
        final Vec3d up = frame.toReal(new Vec3d(0, player.getEyeHeight(), 0));
        final Vec3 eye = player.position().add(up.x(), up.y(), up.z());
        Pair<BlockState, BlockPos> found = null;
        for (int i = 0; i < 8 && found == null; i++) {
            final BlockPos pos = BlockPos.containing(eye.x + ((i & 1) - 0.5) * SPAN, eye.y + (((i >> 1) & 1) - 0.5) * SPAN,
                    eye.z + (((i >> 2) & 1) - 0.5) * SPAN);
            final BlockState state = player.level().getBlockState(pos);
            if (state.getRenderShape() != net.minecraft.world.level.block.RenderShape.INVISIBLE
                    && state.isViewBlocking(player.level(), pos)) {
                found = Pair.of(state, pos);
            }
        }
        cir.setReturnValue(found);
    }
}
