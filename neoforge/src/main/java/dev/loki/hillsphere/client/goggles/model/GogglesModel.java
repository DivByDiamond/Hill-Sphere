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

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * The goggles on the head, in pixels of the head model: a strap hugging the head (vanilla head layout of the
 * 64x32 armor texture) and two hollow rectangular brass frames 2 pixels deep in front of the eyes. Each frame is
 * 4x3 outside with a 2x1 hole right over a skin's eye, so the eyes show through; the bars are shared (one top, one
 * bottom, one bridge) so no two faces lie on each other. Short temples join the frames to the strap. The bar
 * regions sit in the unused hat area at x 32-62 (see docs/tools/goggles_textures.py). Only the head carries cubes.
 */
public final class GogglesModel {

    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 32;
    /** Just above the skin's hat layer (0.5), so the strap is not hidden in it. */
    private static final CubeDeformation STRAP = new CubeDeformation(0.55f);
    /** Front of the frames: 2 pixels deep, their back just clear of the strap (head face -4, strap 0.55 out). */
    private static final float FRONT = -6.6f;
    private static final String[] EMPTY_PARTS = {"hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    private static HumanoidModel<LivingEntity> model;

    private GogglesModel() {
    }

    /** The baked model, made on first use (models can only be baked on the render thread after startup). */
    public static HumanoidModel<LivingEntity> get() {

        if (model == null) {
            model = new HumanoidModel<>(layer().bakeRoot());
        }
        return model;
    }

    /** The head part alone, for the glow pass. */
    public static ModelPart head() {

        return get().head;
    }

    private static LayerDefinition layer() {

        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -8f, -4f, 8, 8, 8, STRAP)
                .texOffs(32, 0).addBox(-4f, -5f, FRONT, 8, 1, 2)
                .texOffs(32, 4).addBox(-4f, -3f, FRONT, 8, 1, 2)
                .texOffs(32, 8).addBox(-4f, -4f, FRONT, 1, 1, 2)
                .texOffs(40, 8).addBox(3f, -4f, FRONT, 1, 1, 2)
                .texOffs(48, 8).addBox(-1f, -4f, FRONT, 2, 1, 2)
                .texOffs(56, 8).addBox(-5f, -4f, FRONT + 0.2f, 1, 1, 2)
                .texOffs(56, 8).addBox(4f, -4f, FRONT + 0.2f, 1, 1, 2), PartPose.ZERO);
        for (final String name : EMPTY_PARTS) {
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
