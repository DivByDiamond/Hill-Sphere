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
package dev.loki.hillsphere;

import dev.loki.hillsphere.client.ClientInit;
import dev.loki.hillsphere.command.HillSphereCommand;
import dev.loki.hillsphere.config.GogglesConfig;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.network.FieldNetwork;
import dev.loki.hillsphere.registry.ModBlockEntities;
import dev.loki.hillsphere.registry.ModBlocks;
import dev.loki.hillsphere.registry.ModFluids;
import dev.loki.hillsphere.registry.ModItems;
import dev.loki.hillsphere.world.WorldFields;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod(Constants.MOD_ID)
public class HillSphereNeoForge {

    public HillSphereNeoForge(IEventBus modBus, ModContainer container) {

        container.registerConfig(ModConfig.Type.SERVER, HillSphereConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, GogglesConfig.SPEC);
        modBus.addListener(ModConfigEvent.Loading.class, HillSphereConfig::onConfig);
        modBus.addListener(ModConfigEvent.Reloading.class, HillSphereConfig::onConfig);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModFluids.register(modBus);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, e -> WorldFields.forget(e.getLevel()));
        FieldNetwork.register(modBus);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> HillSphereCommand.register(e.getDispatcher()));
        if (FMLEnvironment.dist.isClient()) {
            ClientInit.init(modBus);
        }
        HillSphere.init();
    }
}
