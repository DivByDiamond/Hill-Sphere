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
package dev.loki.hillsphere.network.frame;

import dev.loki.hillsphere.debug.Debug;
import dev.loki.hillsphere.entity.FrameState;
import dev.loki.hillsphere.field.frame.GravityFrame;
import dev.loki.hillsphere.field.math.Vec3d;
import dev.loki.hillsphere.field.resolve.Gravity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Keeps "down" the same on the server and every client. The server decides for mobs and tells the clients. A
 * player moves on their own client, so that client decides for its player, turns at once and tells the server,
 * which checks that the field allows the frame and passes it on (or corrects the client). The frame is sent again
 * whenever a client starts seeing an entity, joins, or changes dimension.
 */
public final class FrameNetwork {

    private static final String PROTOCOL = "1";
    /** How far (in dot) above the horizon the pull may point in a claimed frame: a little more than the client keeps a frame for. */
    private static final double CLAIM_SLACK = -0.5;

    private FrameNetwork() {
    }

    public static void register(IEventBus modBus) {

        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar(PROTOCOL)
                .playToClient(FramePayload.TYPE, FramePayload.CODEC, FrameNetwork::onFrame)
                .playToServer(FrameClaimPayload.TYPE, FrameClaimPayload.CODEC, FrameNetwork::onClaim));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, e -> {
            if (e.getTarget() instanceof LivingEntity target && e.getEntity() instanceof ServerPlayer tracker) {
                PacketDistributor.sendToPlayer(tracker, payload(target));
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class,
                e -> PacketDistributor.sendToPlayer((ServerPlayer) e.getEntity(), payload(e.getEntity())));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            ((FrameState) e.getEntity()).hillsphereApply(GravityFrame.DOWN);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(e.getEntity(), payload(e.getEntity()));
        });
    }

    /** Whether this side picks the entity's frame itself; otherwise the frame arrives from the other side. */
    public static boolean decides(Entity self) {

        if (self.level().isClientSide) {
            return self instanceof Player player && player.isLocalPlayer();
        }
        return !(self instanceof ServerPlayer player) || !predicts(player);
    }

    /** The side that decided has turned the entity: tell the other side. */
    public static void turned(Entity self, GravityFrame frame) {

        if (self.level().isClientSide) {
            PacketDistributor.sendToServer(new FrameClaimPayload(frame));
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(self, new FramePayload(self.getId(), frame));
        }
    }

    /** A real client with this mod predicts its player; test players and clients without the channel do not. */
    private static boolean predicts(ServerPlayer player) {

        return player.connection != null && player.connection.hasChannel(FrameClaimPayload.TYPE);
    }

    private static FramePayload payload(Entity entity) {

        return new FramePayload(entity.getId(), ((FrameState) entity).hillsphereFrame());
    }

    private static void onFrame(FramePayload payload, IPayloadContext context) {

        context.enqueueWork(() -> {
            final Entity entity = context.player().level().getEntity(payload.entity());
            if (entity instanceof LivingEntity) {
                ((FrameState) entity).hillsphereApply(payload.frame());
            }
        });
    }

    private static void onClaim(FrameClaimPayload payload, IPayloadContext context) {

        context.enqueueWork(() -> {
            final ServerPlayer player = (ServerPlayer) context.player();
            final FrameState state = (FrameState) player;
            if (allowed(payload.frame(), state.hillspherePull())) {
                state.hillsphereApply(payload.frame());
                PacketDistributor.sendToPlayersTrackingEntity(player, new FramePayload(player.getId(), payload.frame()));
            } else {
                Debug.log("S {} frame claim {} refused, pull={}, keeps {}", player.getName().getString(), payload.frame(),
                        state.hillspherePull(), state.hillsphereFrame());
                PacketDistributor.sendToPlayer(player, payload(player));
            }
        });
    }

    /** Ordinary down is always fine; a turned frame only while the field pulls roughly that way. */
    private static boolean allowed(GravityFrame frame, Gravity pull) {

        if (frame.isVanilla()) {
            return true;
        }
        return !Gravity.VANILLA.equals(pull) && pull.direction().dot(frame.toReal(Vec3d.DOWN)) > CLAIM_SLACK;
    }
}
