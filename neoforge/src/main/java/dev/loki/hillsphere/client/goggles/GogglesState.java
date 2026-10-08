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
package dev.loki.hillsphere.client.goggles;

import dev.loki.hillsphere.client.goggles.GogglesMode.Layer;
import dev.loki.hillsphere.config.GogglesConfig;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.FieldTuning;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.field.view.FieldProbe;
import dev.loki.hillsphere.item.HillGogglesItem;
import dev.loki.hillsphere.world.ClientFields;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * What the goggles know this tick: whether the local player wears them, the cores whose fields come near, and
 * a clock for animations. Refreshed once a client tick, so layers never search all cores while drawing.
 */
public final class GogglesState {

    /** Cores this far beyond the drawing range still count, so a field edge is seen before it is reached. */
    private static final double LOOK_AHEAD = 24;
    /** Ticks the mode name stays large after switching. */
    private static final int MODE_BANNER_TICKS = 40;

    private boolean worn;
    private List<CoreField> near = List.of();
    private Vec3d eye = Vec3d.ZERO;
    private long ticks;
    private long modeChangedAt = -MODE_BANNER_TICKS;

    public void tick() {

        final Player player = Minecraft.getInstance().player;
        worn = player != null && isWorn(player);
        if (!worn) {
            near = List.of();
            return;
        }
        ticks++;
        eye = of(player.getEyePosition());
        final double reach = GogglesConfig.RANGE.get() + LOOK_AHEAD;
        final Vec3d from = eye;
        near = ClientFields.field().cores().stream()
                .filter(core -> core.center().sub(from).length() - core.radius() < reach)
                .sorted(Comparator.comparingDouble(core -> core.center().sub(from).length()))
                .toList();
    }

    public static boolean isWorn(LivingEntity entity) {

        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HillGogglesItem;
    }

    public boolean worn() {

        return worn;
    }

    public boolean shows(Layer layer) {

        return worn && mode().shows(layer);
    }

    public GogglesMode mode() {

        return GogglesConfig.MODE.get();
    }

    /** Switches to the next mode and remembers it in the client config. */
    public void cycleMode() {

        GogglesConfig.MODE.set(mode().next());
        GogglesConfig.MODE.save();
        modeChangedAt = ticks;
    }

    /** 1 right after a mode switch, falling to 0 when the banner should be gone. */
    public double modeBanner(float partialTick) {

        return Math.max(0, 1 - (ticks + partialTick - modeChangedAt) / MODE_BANNER_TICKS);
    }

    /** Animation clock in ticks. */
    public double time(float partialTick) {

        return ticks + partialTick;
    }

    public long ticks() {

        return ticks;
    }

    /** Cores whose fields reach near the wearer, nearest first. */
    public List<CoreField> near() {

        return near;
    }

    public Vec3d eye() {

        return eye;
    }

    public FieldTuning tuning() {

        return HillSphereConfig.tuning();
    }

    public FieldProbe probe(Vec3d point) {

        return FieldProbe.at(tuning(), near, point);
    }

    public static Vec3d of(Vec3 v) {

        return new Vec3d(v.x, v.y, v.z);
    }

    public static Vec3 of(Vec3d v) {

        return new Vec3(v.x(), v.y(), v.z());
    }
}
