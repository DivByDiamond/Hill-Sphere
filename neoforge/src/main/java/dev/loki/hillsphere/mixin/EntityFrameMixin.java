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
package dev.loki.hillsphere.mixin;

import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.entity.FrameSwitch;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets living things move as if "down" were along -y while the world is turned around them. Inside a stretch
 * of movement code ({@link #hillsphereInside}) velocity and movement are virtual; {@code move} turns them into
 * the real world for collisions and back afterwards, and works out what "on the ground" means.
 */
@Mixin(Entity.class)
public abstract class EntityFrameMixin implements FrameState {

    private static final double SLACK = 1e-7;

    @Unique private GravityFrame hillsphereFrame = GravityFrame.DOWN;
    @Unique private double hillsphereStrength = 1;
    @Unique private final FrameSwitch hillsphereSwitch = new FrameSwitch();
    @Unique private boolean hillsphereInside;
    @Unique private Vec3 hillsphereFrom = Vec3.ZERO;
    @Unique private Vec3 hillsphereAsked = Vec3.ZERO;

    @Shadow
    protected abstract AABB makeBoundingBox();

    @Override
    public GravityFrame hillsphereFrame() {

        return hillsphereFrame;
    }

    @Override
    public double hillsphereStrength() {

        return hillsphereStrength;
    }

    @Override
    public void hillsphereInside(boolean inside) {

        hillsphereInside = inside;
    }

    @Override
    public void hillsphereRefresh() {

        final Entity self = (Entity) (Object) this;
        final Gravity planet = FieldLookup.planetAt(self);
        hillsphereStrength = Gravity.VANILLA.equals(planet) ? 1 : planet.strength();
        final GravityFrame next = hillsphereSwitch.next(self, hillsphereFrame, planet);
        if (next != hillsphereFrame && hillsphereSwitch.turn(self, hillsphereFrame, next)) {
            hillsphereFrame = next;
        }
    }

    @Inject(method = "makeBoundingBox", at = @At("RETURN"), cancellable = true)
    private void hillsphereTurnBox(CallbackInfoReturnable<AABB> cir) {

        final Entity self = (Entity) (Object) this;
        if (hillsphereFrame != null && !hillsphereFrame.isVanilla()) {
            cir.setReturnValue(LivingGravity.box(hillsphereFrame, self.position(), self.getDimensions(self.getPose())));
        }
    }

    /** The movement arrives virtual; collisions need the real one, and so does the velocity they zero out. */
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 hillsphereMoveInTheWorld(Vec3 movement) {

        if (!hillsphereInside) {
            return movement;
        }
        final Entity self = (Entity) (Object) this;
        hillsphereFrom = self.position();
        hillsphereAsked = movement;
        self.setDeltaMovement(toReal(self.getDeltaMovement()));
        return toReal(movement);
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void hillsphereBackToVirtual(MoverType type, Vec3 movement, CallbackInfo ci) {

        if (hillsphereInside) {
            final Entity self = (Entity) (Object) this;
            self.setDeltaMovement(toVirtual(self.getDeltaMovement()));
        }
    }

    /** Fall damage counts the way down in the virtual frame. */
    @ModifyVariable(method = "checkFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double hillsphereFallenDistance(double y) {

        return hillsphereInside ? moved().y : y;
    }

    /** Standing is touching whatever is below in the virtual frame, which may be a wall or a ceiling; a flying player never lands. */
    @ModifyVariable(method = "checkFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean hillsphereStanding(boolean onGround) {

        if (!hillsphereInside) {
            return onGround;
        }
        final Entity self = (Entity) (Object) this;
        final boolean flying = self instanceof Player player && player.getAbilities().flying;
        final boolean standing = !flying && hillsphereAsked.y < 0 && moved().y > hillsphereAsked.y + SLACK;
        self.setOnGround(standing);
        return standing;
    }

    /** What the last move really did, in the virtual frame. */
    @Unique
    private Vec3 moved() {

        final Entity self = (Entity) (Object) this;
        return toVirtual(self.position().subtract(hillsphereFrom));
    }

    @Unique
    private Vec3 toReal(Vec3 v) {

        final Vec3d r = hillsphereFrame.toReal(new Vec3d(v.x, v.y, v.z));
        return new Vec3(r.x(), r.y(), r.z());
    }

    @Unique
    private Vec3 toVirtual(Vec3 v) {

        final Vec3d r = hillsphereFrame.toVirtual(new Vec3d(v.x, v.y, v.z));
        return new Vec3(r.x(), r.y(), r.z());
    }
}
