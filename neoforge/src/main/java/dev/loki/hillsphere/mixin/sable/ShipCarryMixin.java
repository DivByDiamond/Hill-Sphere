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

import dev.loki.hillsphere.debug.Debug;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sable carries an entity along with the ship it stands on by placing its position at "box centre minus
 * half the box height", which is where the feet are of an upright body. In a turned frame the position sits
 * at the other end of the box, so that placement is off by a constant, and the difference Sable then applies
 * to the entity as "motion the ship gave it" is that constant too: a player under a ceiling was thrown a
 * whole body height away and fell back, over and over. Sable measures the carried motion as the new anchor minus the old, so it is exactly minus that constant, and adding it back leaves only the ship's own motion.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public abstract class ShipCarryMixin {

    @Inject(method = "collide(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;"
            + "Ldev/ryanhcode/sable/api/math/LevelReusedVectors;)"
            + "Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision$CollisionInfo;",
            at = @At("RETURN"), remap = false)
    private static void hillsphereTakeOffTheAnchorError(Entity entity, Vec3 motion, Vec3 velocity,
            dev.ryanhcode.sable.api.math.LevelReusedVectors sink, CallbackInfoReturnable<SubLevelEntityCollision.CollisionInfo> cir) {

        final SubLevelEntityCollision.CollisionInfo info = cir.getReturnValue();
        if (info == null || info.inheritedMotion == null || LivingGravity.frameOf(entity).isVanilla()) {
            return;
        }
        final AABB box = entity.getBoundingBox();
        final Vec3 centre = box.getCenter();
        final Vec3 pos = entity.position();
        final Vec3 error = new Vec3(pos.x - centre.x, pos.y - centre.y + box.getYsize() / 2.0, pos.z - centre.z);
        final Vec3 fixed = info.inheritedMotion.add(error);
        Debug.every("carry" + entity.getId() + entity.level().isClientSide, entity.level().getGameTime(), 5,
                "{} SHIP CARRY {} inherited={} anchorError={} fixed={}", entity.level().isClientSide ? "C" : "S",
                entity.getName().getString(), info.inheritedMotion, error, fixed);
        info.inheritedMotion = fixed.lengthSqr() < 1.0E-8 ? null : fixed;
    }
}
