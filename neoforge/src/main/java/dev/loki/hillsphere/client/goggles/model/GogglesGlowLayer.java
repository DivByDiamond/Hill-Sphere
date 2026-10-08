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
package dev.loki.hillsphere.client.goggles.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.client.goggles.GogglesState;
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.config.HillSphereConfig;
import dev.loki.hillsphere.field.view.FieldProbe;
import dev.loki.hillsphere.world.ClientFields;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Makes the lenses glow in the colour of the field the wearer stands in; dark outside fields. Drawn additively
 * at full brightness, so the glow shows at night too. Fades with how deep in the field the wearer is.
 */
public final class GogglesGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static final RenderType GLOW = RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/armor/hill_goggles_layer_1_glow.png"));
    private static final double PULSE_SPEED = 0.15;

    public GogglesGlowLayer(RenderLayerParent<T, M> parent) {

        super(parent);
    }

    /** Adds the glow to every player skin. */
    public static void addTo(EntityRenderersEvent.AddLayers event) {

        for (final PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new GogglesGlowLayer<>(renderer));
            }
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (entity.isInvisible() || !GogglesState.isWorn(entity)) {
            return;
        }
        final FieldProbe probe = FieldProbe.at(HillSphereConfig.tuning(), ClientFields.field().cores(),
                GogglesState.of(entity.getEyePosition(partialTick)));
        if (!probe.inField()) {
            return;
        }
        final double pulse = 0.8 + 0.2 * Math.sin(ageInTicks * PULSE_SPEED);
        final int colour = Palette.scale(Palette.of(probe.polarity())[Palette.MAIN], probe.influence() * pulse);
        final ModelPart head = GogglesModel.head();
        head.copyFrom(getParentModel().head);
        head.render(pose, buffers.getBuffer(GLOW), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
    }
}
