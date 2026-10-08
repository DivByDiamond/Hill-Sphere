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
package dev.loki.hillsphere.client;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.client.camera.GravityTilt;
import dev.loki.hillsphere.client.goggles.GogglesClient;
import dev.loki.hillsphere.registry.ModBlockEntities;
import dev.loki.hillsphere.world.ClientFields;
import dev.loki.hillsphere.registry.ModFluids;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only wiring. Only loaded on the client. */
public final class ClientInit {

    private ClientInit() {
    }

    public static void init(IEventBus modBus) {

        ModPartials.init();
        modBus.addListener(ClientInit::onClientSetup);
        modBus.addListener(ClientInit::registerRenderers);
        modBus.addListener(ClientInit::registerFluidTextures);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ZeroGravityControls::tick);
        GogglesClient.init(modBus);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> ClientFields.clear());
    }

    private static void onClientSetup(FMLClientSetupEvent event) {

        event.enqueueWork(GravityTilt::register);
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(ModBlockEntities.HILL_CORE.get())
                .factory(SingleAxisRotatingVisual.of(ModPartials.ROTOR))
                .skipVanillaRender(be -> true)
                .apply());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerBlockEntityRenderer(ModBlockEntities.HILL_CORE.get(), HillCoreRenderer::new);
    }

    private static void registerFluidTextures(RegisterClientExtensionsEvent event) {

        final ResourceLocation still = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block/molten_gravite_still");
        final ResourceLocation flow = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block/molten_gravite_flow");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {

                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {

                return flow;
            }
        }, ModFluids.TYPE.get());
    }
}
