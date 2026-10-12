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
package dev.loki.hillsphere.mixin.frame;

import java.util.Optional;

import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The block under the feet is measured along the frame's "down", not along -y: friction, jump and speed
 * factors stand on a wall or a ceiling too. NeoForge's own {@code getOnPos} logic, turned to the frame.
 */
@Mixin(Entity.class)
public abstract class FrameFootMixin {

    @Shadow
    public Optional<BlockPos> mainSupportingBlockPos;

    @Shadow
    private boolean onGroundNoBlocks;

    /** The feet's block: NeoForge's {@code getOnPos(float)} with every step measured along "down". */
    @Inject(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void hillsphereFrameOnPos(float offset, CallbackInfoReturnable<BlockPos> cir) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return;
        }
        final Entity self = (Entity) (Object) this;
        final Vec3d down = frame.toReal(new Vec3d(0, -1, 0));
        if (mainSupportingBlockPos.isPresent()) {
            final BlockPos supporting = mainSupportingBlockPos.get();
            if (!(offset > 1.0E-5F)) {
                cir.setReturnValue(supporting);
                return;
            }
            final BlockState state = self.level().getBlockState(supporting);
            final boolean holds = offset <= 0.5F && state.collisionExtendsVertically(self.level(), supporting, self);
            if (holds) {
                cir.setReturnValue(supporting);
                return;
            }
            cir.setReturnValue(hillsphereReplaceDown(supporting, down, hillspherePoint(self, down, offset)));
            return;
        }
        final Vec3d there = hillspherePoint(self, down, offset);
        cir.setReturnValue(new BlockPos(Mth.floor(there.x()), Mth.floor(there.y()), Mth.floor(there.z())));
    }

    /** Which block holds the feet, looked for along "down" as vanilla looks along -y. */
    @Inject(method = "checkSupportingBlock(ZLnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void hillsphereFrameSupport(boolean onGround, Vec3 movement, CallbackInfo ci) {

        final GravityFrame frame = frame();
        if (frame == null || frame.isVanilla()) {
            return;
        }
        final Entity self = (Entity) (Object) this;
        final Vec3d down = frame.toReal(new Vec3d(0, -1, 0));
        if (onGround) {
            final AABB sliver = hillsphereSliver(self.getBoundingBox(), down, 1.0E-6);
            Optional<BlockPos> found = self.level().findSupportingBlock(self, sliver);
            if (found.isPresent() || onGroundNoBlocks) {
                mainSupportingBlockPos = found;
            } else if (movement != null) {
                final double along = movement.x * down.x() + movement.y * down.y() + movement.z * down.z();
                final Vec3 flat = movement.subtract(down.x() * along, down.y() * along, down.z() * along);
                found = self.level().findSupportingBlock(self, sliver.move(-flat.x, -flat.y, -flat.z));
                mainSupportingBlockPos = found;
            }
            onGroundNoBlocks = found.isEmpty();
        } else {
            onGroundNoBlocks = false;
            if (mainSupportingBlockPos.isPresent()) {
                mainSupportingBlockPos = Optional.empty();
            }
        }
        ci.cancel();
    }

    /** The point an offset below the feet becomes in the real world. */
    @Unique
    private static Vec3d hillspherePoint(Entity self, Vec3d down, double offset) {

        final Vec3 at = self.position();
        return new Vec3d(at.x + down.x() * offset, at.y + down.y() * offset, at.z + down.z() * offset);
    }

    /** The given supporting pos with its coordinate along "down" replaced, as vanilla replaces its y. */
    @Unique
    private static BlockPos hillsphereReplaceDown(BlockPos pos, Vec3d down, Vec3d there) {

        final int x = down.x() != 0 ? Mth.floor(there.x()) : pos.getX();
        final int y = down.y() != 0 ? Mth.floor(there.y()) : pos.getY();
        final int z = down.z() != 0 ? Mth.floor(there.z()) : pos.getZ();
        return new BlockPos(x, y, z);
    }

    /** A hair-thick slice on the face of the box that "down" presses into. */
    @Unique
    private static AABB hillsphereSliver(AABB box, Vec3d down, double thin) {

        final double x0 = down.x() > 0 ? box.maxX : down.x() < 0 ? box.minX - thin : box.minX;
        final double x1 = down.x() > 0 ? box.maxX + thin : down.x() < 0 ? box.minX : box.maxX;
        final double y0 = down.y() > 0 ? box.maxY : down.y() < 0 ? box.minY - thin : box.minY;
        final double y1 = down.y() > 0 ? box.maxY + thin : down.y() < 0 ? box.minY : box.maxY;
        final double z0 = down.z() > 0 ? box.maxZ : down.z() < 0 ? box.minZ - thin : box.minZ;
        final double z1 = down.z() > 0 ? box.maxZ + thin : down.z() < 0 ? box.minZ : box.maxZ;
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    @Unique
    private GravityFrame frame() {

        return ((FrameState) this).hillsphereFrame();
    }
}
