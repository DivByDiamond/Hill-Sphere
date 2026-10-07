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
package dev.loki.hillsphere.network;

import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.CoreField;
import dev.loki.hillsphere.world.ClientFields;
import dev.loki.hillsphere.world.LevelFields;
import dev.loki.hillsphere.world.WorldFields;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sends the cores that changed to players, and applies them on the client. */
public final class FieldNetwork {

    private static final String PROTOCOL = "1";

    private FieldNetwork() {
    }

    public static void register(IEventBus modBus) {

        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar(PROTOCOL)
                .playToClient(FieldSyncPayload.TYPE, FieldSyncPayload.CODEC, FieldNetwork::receive));
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                sendChanges(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class,
                e -> sendEverything((ServerPlayer) e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class,
                e -> sendEverything((ServerPlayer) e.getEntity()));
    }

    private static void sendChanges(ServerLevel level) {

        final LevelFields fields = WorldFields.peek(level);
        if (fields == null) {
            return;
        }
        final Map<BlockPos, CoreField> changes = fields.changes().drain();
        if (!changes.isEmpty()) {
            PacketDistributor.sendToPlayersInDimension(level, payload(false, changes));
        }
    }

    /** A player who joins or changes world knows nothing yet, so they get every core of that world. */
    private static void sendEverything(ServerPlayer player) {

        final LevelFields fields = WorldFields.peek(player.serverLevel());
        final Map<BlockPos, CoreField> all = fields == null ? Map.of() : fields.changes().snapshot();
        PacketDistributor.sendToPlayer(player, payload(true, all));
    }

    private static FieldSyncPayload payload(boolean replace, Map<BlockPos, CoreField> cores) {

        final List<FieldSyncPayload.Entry> entries = cores.entrySet().stream()
                .map(e -> new FieldSyncPayload.Entry(e.getKey(), (float) e.getValue().radius(),
                        (float) e.getValue().strength(), e.getValue().polarity()))
                .toList();
        return new FieldSyncPayload(replace, entries);
    }

    private static void receive(FieldSyncPayload payload, IPayloadContext context) {

        final Map<BlockPos, CoreField> cores = new HashMap<>();
        for (final FieldSyncPayload.Entry e : payload.entries()) {
            final Vec3d center = new Vec3d(e.pos().getX() + 0.5, e.pos().getY() + 0.5, e.pos().getZ() + 0.5);
            cores.put(e.pos(), new CoreField(center, e.radius(), e.strength(), e.polarity()));
        }
        context.enqueueWork(() -> ClientFields.apply(payload.replace(), cores));
    }
}
