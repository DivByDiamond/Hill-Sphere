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
package dev.loki.hillsphere.mixin.sable;

import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sable finds the eye of a body on a ship as the feet plus the eye height along world +y, and the pick (the
 * crosshair target, where blocks go), the camera offset and the light the body is drawn with all start from it. On
 * a wall or under a ceiling that point is off to the side of the head, so the target is not where the crosshair
 * is and the body is lit from inside a block. The head is along the frame's "up"; the rest is Sable's own.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.ActiveSableCompanion", remap = false)
public abstract class ShipEyeMixin {

    private static final String EYE = "getEyePositionInterpolated(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/world/phys/Vec3;";

    @Inject(method = EYE, at = @At("HEAD"), cancellable = true, remap = false)
    private void hillsphereEyeAlongTheFrame(Entity entity, float partialTicks, CallbackInfoReturnable<Vec3> cir) {

        final GravityFrame frame = LivingGravity.frameOf(entity);
        if (frame.isVanilla() || !(Sable.HELPER.getTrackingOrVehicleSubLevel(entity) instanceof final ClientSubLevel ship)) {
            return;
        }
        final Vec3 head = LivingGravity.toReal(frame, new Vec3(0, entity.getEyeHeight(), 0));
        final Vector3d start = new Vector3d(entity.xo + head.x, entity.yo + head.y, entity.zo + head.z);
        final Vector3d end = new Vector3d(entity.getX() + head.x, entity.getY() + head.y, entity.getZ() + head.z);
        final Pose3dc render = ship.renderPose(partialTicks);
        ship.lastPose().transformPositionInverse(start);
        ship.logicalPose().transformPositionInverse(end);
        start.lerp(end, partialTicks);
        render.transformPosition(start);
        cir.setReturnValue(new Vec3(start.x, start.y, start.z));
    }
}
