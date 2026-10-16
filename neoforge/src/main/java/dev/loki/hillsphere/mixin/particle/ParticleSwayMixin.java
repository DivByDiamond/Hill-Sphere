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

import dev.loki.hillsphere.client.particle.Leanable;
import dev.loki.hillsphere.debug.Debug;
import dev.loki.hillsphere.client.particle.ParticleSway;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particles fall along the world's -y. Inside a field, a falling particle gets its gravity turned toward
 * the field's "down" every tick, and a launch speed (smoke, flames, bubbles that rise by their own speed)
 * is turned toward the field's "up" once, when the particle is added.
 */
@Mixin(Particle.class)
public abstract class ParticleSwayMixin implements Leanable {

    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;
    @Shadow
    protected double xd;
    @Shadow
    protected double yd;
    @Shadow
    protected double zd;
    @Shadow
    protected float gravity;

    @Inject(method = "tick", at = @At("HEAD"))
    private void hillsphereFall(CallbackInfo ci) {

        if (gravity == 0) {
            return;
        }
        final Vec3d push = ParticleSway.fall(x, y, z, gravity);
        if (Debug.on()) {
            ParticleSway.trace(push);
        }
        if (push != null) {
            xd += push.x();
            yd += push.y();
            zd += push.z();
        }
    }

    @Override
    public void hillsphereLean() {

        final Vec3d turned = ParticleSway.lean(x, y, z, new Vec3d(xd, yd, zd));
        if (turned != null) {
            xd = turned.x();
            yd = turned.y();
            zd = turned.z();
        }
    }
}
