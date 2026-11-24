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

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.field.frame.GravityFrame;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the player's own client has turned into this frame (players move on their client, so it decides). */
public record FrameClaimPayload(GravityFrame frame) implements CustomPacketPayload {

    public static final Type<FrameClaimPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "frame_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FrameClaimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> GravityFrame.values()[i], GravityFrame::ordinal), FrameClaimPayload::frame,
            FrameClaimPayload::new);

    @Override
    public Type<FrameClaimPayload> type() {

        return TYPE;
    }
}
