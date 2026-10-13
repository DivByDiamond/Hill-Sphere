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
package dev.loki.hillsphere.debug;

import dev.loki.hillsphere.client.debug.InputProbe;
import dev.loki.hillsphere.compat.Ships;
import dev.loki.hillsphere.entity.LivingGravity;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.resolve.Gravity;
import dev.loki.hillsphere.world.FieldLookup;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Describes a player who is inside a field or in a turned frame: a line every half second, and at once
 * whenever the frame, the ground contact or the seat changes. A bounce shows as a run of ground flips
 * with the speed, the ship under the feet and the collision flags beside them.
 */
public final class PlayerWatch {

    private static final int PERIOD = 10;

    private record Seen(GravityFrame frame, boolean onGround, boolean seated) {
    }

    private static final Map<String, Seen> SEEN = new HashMap<>();

    private PlayerWatch() {
    }

    public static void register(IEventBus bus) {

        bus.addListener(PlayerTickEvent.Post.class, e -> tick(e.getEntity()));
        bus.addListener(LivingJumpEvent.class, e -> {
            if (e.getEntity() instanceof Player player) {
                jumped(player);
            }
        });
    }

    private static void tick(Player player) {

        if (!Debug.on()) {
            return;
        }
        final Gravity planet = FieldLookup.planetAt(player);
        final double lift = FieldLookup.liftAt(player);
        final GravityFrame frame = LivingGravity.frameOf(player);
        final String key = (player.level().isClientSide ? "C " : "S ") + player.getName().getString();
        final Seen now = new Seen(frame, player.onGround(), player.isPassenger());
        final Seen before = SEEN.put(key, now);
        if (frame.isVanilla() && Gravity.VANILLA.equals(planet) && lift == 0) {
            return;
        }
        final long time = player.level().getGameTime();
        if (before != null && !before.equals(now)) {
            Debug.log("{} CHANGE t={} frame {}->{} ground {}->{} seated {}->{}", key, time, before.frame(), now.frame(),
                    before.onGround(), now.onGround(), before.seated(), now.seated());
            describe(key, player, planet, lift);
        } else if (time % PERIOD == 0) {
            describe(key, player, planet, lift);
        }
    }

    /** Every jump, with the controls that asked for it: auto-jump or a held key would show here. */
    private static void jumped(Player player) {

        final String key = (player.level().isClientSide ? "C " : "S ") + player.getName().getString();
        Debug.log("{} JUMP t={} pos={} vel={} ground={} frame={} {}", key, player.level().getGameTime(), player.position(),
                player.getDeltaMovement(), player.onGround(), LivingGravity.frameOf(player),
                player.level().isClientSide ? InputProbe.describe(player) : "");
    }

    private static void describe(String key, Player player, Gravity planet, double lift) {

        final Vec3 p = player.position();
        final Vec3 v = player.getDeltaMovement();
        Debug.log("{} t={} pos=({},{},{}) vel=({},{},{}) ground={} vColl={} vBelow={} hColl={} fall={} frame={} pull=({} x{}) lift={} {}",
                key, player.level().getGameTime(), f(p.x), f(p.y), f(p.z), f(v.x), f(v.y), f(v.z), player.onGround(),
                player.verticalCollision, player.verticalCollisionBelow, player.horizontalCollision, f(player.fallDistance),
                LivingGravity.frameOf(player), planet.direction(), f(planet.strength()), f(lift), Ships.describe(player)
                + (player.level().isClientSide ? " " + InputProbe.describe(player) : ""));
    }

    private static String f(double v) {

        return String.format(java.util.Locale.ROOT, "%.3f", v);
    }
}
