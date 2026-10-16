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
package dev.loki.hillsphere.compat;

import dev.loki.hillsphere.compat.sable.SableShips;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.shape.Deck;
import java.util.List;
import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

/**
 * Ships of an optional ship mod, so nothing else touches that mod. Sable support is installed only
 * when Sable is loaded; without it its classes are never loaded and ships simply do not exist.
 */
public final class Ships {

    private static ShipAccess access = ShipAccess.NONE;

    private Ships() {
    }

    public static void init() {

        if (ModList.get().isLoaded("sable")) {
            access = SableShips.install();
        }
    }

    /** @see ShipAccess#toWorld */
    public static Vec3d toWorld(Level level, Vec3d point) {

        return access.toWorld(level, point);
    }

    /** @see ShipAccess#describe */
    public static String describe(Entity entity) {

        return access.describe(entity);
    }

    /** @see ShipAccess#turnsCamera */
    public static boolean turnsCamera(Entity entity) {

        return access.turnsCamera(entity);
    }

    /** @see ShipAccess#collides */
    public static boolean collides(Level level, AABB box) {

        return access.collides(level, box);
    }

    /** @see ShipAccess#shipsNear */
    public static List<ShipPlot> shipsNear(Level level, Vec3d point, double range) {

        return access.shipsNear(level, point, range);
    }

    /** @see ShipAccess#hullOf */
    public static Deck hullOf(Level level, BlockPos pos) {

        return access.hullOf(level, pos);
    }
}
