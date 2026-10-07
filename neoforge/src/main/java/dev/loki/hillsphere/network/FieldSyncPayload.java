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

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Cores that changed, sent from the server to clients. A core with no radius has been removed;
 * a replacing sync tells the client to forget everything it knew first.
 */
public record FieldSyncPayload(boolean replace, List<Entry> entries) implements CustomPacketPayload {

    public static final Type<FieldSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "field_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Entry::pos,
            ByteBufCodecs.FLOAT, Entry::radius,
            ByteBufCodecs.FLOAT, Entry::strength,
            ByteBufCodecs.idMapper(i -> Polarity.values()[i], Polarity::ordinal), Entry::polarity,
            Entry::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, FieldSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FieldSyncPayload::replace,
            ENTRY_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)), FieldSyncPayload::entries,
            FieldSyncPayload::new);

    @Override
    public Type<FieldSyncPayload> type() {

        return TYPE;
    }

    /** One core: its block, size, pull and kind. */
    public record Entry(BlockPos pos, float radius, float strength, Polarity polarity) {
    }
}
