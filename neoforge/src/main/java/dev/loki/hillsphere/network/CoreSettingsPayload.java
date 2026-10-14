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

import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;
import dev.loki.hillsphere.field.shape.Shape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** What a player set on the core screen, sent from the client to the server. */
public record CoreSettingsPayload(BlockPos pos, Polarity polarity, double level, RedstoneMode mode, Shape shape)
        implements CustomPacketPayload {

    public static final Type<CoreSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "core_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoreSettingsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CoreSettingsPayload::pos,
            ByteBufCodecs.idMapper(i -> Polarity.values()[i], Polarity::ordinal), CoreSettingsPayload::polarity,
            ByteBufCodecs.DOUBLE, CoreSettingsPayload::level,
            ByteBufCodecs.idMapper(i -> RedstoneMode.values()[i], RedstoneMode::ordinal), CoreSettingsPayload::mode,
            ByteBufCodecs.idMapper(i -> Shape.values()[i], Shape::ordinal), CoreSettingsPayload::shape,
            CoreSettingsPayload::new);

    @Override
    public Type<CoreSettingsPayload> type() {

        return TYPE;
    }
}
