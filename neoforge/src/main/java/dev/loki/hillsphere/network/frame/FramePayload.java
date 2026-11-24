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

/** Server to client: which frame an entity is in now. Also the server's correction of the player's own frame. */
public record FramePayload(int entity, GravityFrame frame) implements CustomPacketPayload {

    public static final Type<FramePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "frame"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FramePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FramePayload::entity,
            ByteBufCodecs.idMapper(i -> GravityFrame.values()[i], GravityFrame::ordinal), FramePayload::frame,
            FramePayload::new);

    @Override
    public Type<FramePayload> type() {

        return TYPE;
    }
}
