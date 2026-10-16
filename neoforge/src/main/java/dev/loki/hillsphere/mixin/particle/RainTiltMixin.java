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
package dev.loki.hillsphere.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.hillsphere.client.particle.ParticleSway;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rain and snow are drawn as vertical quads around the camera. Each corner is moved sideways in proportion to its
 * height above the player's eyes, by the lean the field gives there, so the strips run along the field's "down"
 * and drops fall toward a core instead of straight down. The eyes are where the strip stays put.
 */
@Mixin(LevelRenderer.class)
public abstract class RainTiltMixin {

    @WrapOperation(method = "renderSnowAndRain", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer hillsphereLeanTheRain(BufferBuilder builder, float x, float y, float z, Operation<VertexConsumer> original) {

        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final Vec3d slope = ParticleSway.rainSlope(eye.x + x, eye.y + y, eye.z + z);
        return slope == null ? original.call(builder, x, y, z)
                : original.call(builder, (float) (x + slope.x() * y), y, (float) (z + slope.z() * y));
    }
}
