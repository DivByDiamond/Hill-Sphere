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

import com.mojang.blaze3d.platform.InputConstants;
import dev.loki.hillsphere.client.goggles.GogglesMode.Layer;
import dev.loki.hillsphere.client.goggles.flow.FlowLayer;
import dev.loki.hillsphere.client.goggles.hud.HudOverlay;
import dev.loki.hillsphere.client.goggles.model.GogglesExtensions;
import dev.loki.hillsphere.client.goggles.model.GogglesGlowLayer;
import dev.loki.hillsphere.client.goggles.render.WorldLayer;
import dev.loki.hillsphere.client.goggles.render.WorldOverlay;
import dev.loki.hillsphere.client.goggles.shell.ShellLayer;
import dev.loki.hillsphere.client.goggles.surface.SurfaceLayer;
import dev.loki.hillsphere.registry.ModItems;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Client wiring of the goggles: the model on the head, the mode key, the world layers and the screen layers. */
public final class GogglesClient {

    private static final KeyMapping CYCLE = new KeyMapping("key.hillsphere.goggles_mode", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H, "key.categories.hillsphere");
    private static final GogglesState STATE = new GogglesState();
    private static final WorldOverlay WORLD = new WorldOverlay(STATE, layers());
    private static final HudOverlay HUD = new HudOverlay(STATE);

    private GogglesClient() {
    }

    public static void init(IEventBus modBus) {

        modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(CYCLE));
        modBus.addListener(RegisterClientExtensionsEvent.class, e -> e.registerItem(new GogglesExtensions(), ModItems.HILL_GOGGLES.get()));
        modBus.addListener(EntityRenderersEvent.AddLayers.class, GogglesGlowLayer::addTo);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, WORLD::onRender);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class,
                e -> HUD.render(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false)));
    }

    private static void tick() {

        STATE.tick();
        while (CYCLE.consumeClick()) {
            if (STATE.worn()) {
                STATE.cycleMode();
            }
        }
        WORLD.tick();
    }

    private static Map<Layer, WorldLayer> layers() {

        final Map<Layer, WorldLayer> layers = new EnumMap<>(Layer.class);
        layers.put(Layer.SURFACES, new SurfaceLayer());
        layers.put(Layer.BOUNDS, new ShellLayer());
        layers.put(Layer.FLOW, new FlowLayer());
        return layers;
    }
}
