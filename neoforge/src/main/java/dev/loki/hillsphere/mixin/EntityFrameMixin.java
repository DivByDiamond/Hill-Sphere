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
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.FieldLookup;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
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

    /** In the air a frame is kept while the pull is within 60 degrees of its "down". */
    private static final double KEEP_IN_AIR = 0.5;

    /** On the ground it is kept until the pull points well above the horizon, so a platform stays a floor beside the core. */
    private static final double KEEP_ON_GROUND = -0.4;

    /** Ticks a new direction must hold before the frame follows it. */
    private static final int DWELL_TICKS = 4;

    @Unique private GravityFrame hillsphereFrame = GravityFrame.DOWN;
    @Unique private double hillsphereStrength = 1;
    @Unique private GravityFrame hillsphereCandidate = GravityFrame.DOWN;
    @Unique private int hillsphereDwell;
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
        final boolean vanilla = Gravity.VANILLA.equals(planet);
        hillsphereStrength = vanilla ? 1 : planet.strength();
        if (vanilla) {
            turnTo(self, GravityFrame.DOWN);
            return;
        }
        final Vec3d pull = planet.direction();
        final GravityFrame wanted = GravityFrame.of(pull);
        if (wanted == hillsphereFrame || pull.dot(hillsphereFrame.toReal(Vec3d.DOWN)) > (self.onGround() ? KEEP_ON_GROUND : KEEP_IN_AIR)) {
            hillsphereDwell = 0;
            return;
        }
        hillsphereDwell = wanted == hillsphereCandidate ? hillsphereDwell + 1 : 1;
        hillsphereCandidate = wanted;
        if (hillsphereDwell >= DWELL_TICKS) {
            turnTo(self, wanted);
        }
    }

    /** Switches frame around the middle of the body, and only if the turned body has room; else waits. */
    @Unique
    private void turnTo(Entity self, GravityFrame frame) {

        if (frame == hillsphereFrame) {
            return;
        }
        final Vec3 center = self.getBoundingBox().getCenter();
        final EntityDimensions size = self.getDimensions(self.getPose());
        final Vec3d off = frame.toReal(new Vec3d(0, -size.height() / 2, 0));
        final Vec3 feet = new Vec3(center.x + off.x(), center.y + off.y(), center.z + off.z());
        if (!self.level().noCollision(self, LivingGravity.box(frame, feet, size))) {
            return;
        }
        dropSpeedAlongTheOldDown(self);
        hillsphereFrame = frame;
        hillsphereDwell = 0;
        self.setPos(feet);
    }

    /** What was fallen so far must not carry on as sideways flight once "down" has turned. */
    @Unique
    private void dropSpeedAlongTheOldDown(Entity self) {

        final Vec3d down = hillsphereFrame.toReal(Vec3d.DOWN);
        final Vec3 v = self.getDeltaMovement();
        final double along = v.x * down.x() + v.y * down.y() + v.z * down.z();
        self.setDeltaMovement(v.x - along * down.x(), v.y - along * down.y(), v.z - along * down.z());
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
