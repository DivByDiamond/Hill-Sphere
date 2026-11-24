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
package dev.loki.hillsphere.client.screen;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.loki.hillsphere.Constants;
import dev.loki.hillsphere.block.HillCoreBlock;
import dev.loki.hillsphere.blockentity.setting.CoreSettings;
import dev.loki.hillsphere.client.goggles.Palette;
import dev.loki.hillsphere.field.Polarity;
import dev.loki.hillsphere.field.control.RedstoneMode;
import dev.loki.hillsphere.field.shape.Shape;
import dev.loki.hillsphere.network.CoreSettingsPayload;
import dev.loki.hillsphere.registry.ModItems;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The screen of a core, built like Create's own (the sequenced gearshift's): a brass title strip, dark wood, and
 * a grey field per setting that changes when scrolled over, with a tooltip listing the choices. Polarity, redstone
 * mode, shape and strength level; the level field also shows the six level cells in the polarity's colour. The
 * settings go to the server when the screen closes (the tick button or Escape).
 */
public final class CoreSettingsScreen extends AbstractSimiScreen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/core_settings.png");
    /** Size of the background drawn by docs/tools/textures.py (gui_core): keep the two in step. */
    private static final int WIDTH = 220;
    private static final int HEIGHT = 140;
    private static final int MARGIN = 8;
    private static final int FIELD_H = 18;
    private static final String[] CAPTIONS = {"polarity", "mode", "shape", "level"};
    private static final int ROW_Y = 22;
    private static final int ROW_STEP = 23;
    /** Level steps per scroll: a quarter normally, the finest step with Shift. */
    private static final int COARSE = 5;
    private static final int TITLE = 0x592424;
    private static final int CAPTION = 0xE4B763;

    private final BlockPos pos;
    private Polarity polarity;
    private RedstoneMode mode;
    private Shape shape;
    private double level;
    /** Where the fields start and how wide they are: after the widest caption in the current language. */
    private int fieldX;
    private int fieldW;

    private CoreSettingsScreen(CoreSettings settings) {

        super(Component.translatable("hillsphere.core.settings"));
        pos = settings.blockEntity.getBlockPos();
        polarity = settings.polarity();
        mode = settings.mode();
        shape = settings.shape();
        level = settings.level();
    }

    public static void open(CoreSettings settings) {

        Minecraft.getInstance().setScreen(new CoreSettingsScreen(settings));
    }

    @Override
    protected void init() {

        setWindowSize(WIDTH, HEIGHT);
        super.init();
        int widest = 0;
        for (final String caption : CAPTIONS) {
            widest = Math.max(widest, font.width(caption(caption)));
        }
        fieldX = MARGIN + widest + 10;
        fieldW = WIDTH - fieldX - MARGIN;
        addRenderableWidget(choice(0, Polarity.values(), "hillsphere.core.polarity", polarity.ordinal(), field(0),
                i -> polarity = Polarity.values()[i]));
        addRenderableWidget(choice(1, RedstoneMode.values(), "hillsphere.core.mode", mode.ordinal(), field(1),
                i -> mode = RedstoneMode.values()[i]));
        addRenderableWidget(choice(2, Shape.values(), "hillsphere.core.shape", shape.ordinal(), field(2),
                i -> shape = Shape.values()[i]));
        final Label levelLabel = field(3);
        addRenderableWidget(new ScrollInput(guiLeft + fieldX, rowTop(3), fieldW, FIELD_H)
                .withRange(0, (int) Math.round(HillCoreBlock.MAX_LEVEL / CoreSettings.LEVEL_STEP) + 1)
                .withStepFunction(c -> c.shift ? 1 : COARSE)
                .titled(Component.translatable("hillsphere.core.level"))
                .format(i -> Component.literal(String.format(Locale.ROOT, "%.2f", i * CoreSettings.LEVEL_STEP)))
                .writingTo(levelLabel)
                .calling(i -> level = i * CoreSettings.LEVEL_STEP)
                .setState((int) Math.round(level / CoreSettings.LEVEL_STEP)));
        final IconButton confirm = new IconButton(guiLeft + WIDTH - 25, guiTop + HEIGHT - 21, AllIcons.I_CONFIRM);
        confirm.withCallback(this::onClose);
        addRenderableWidget(confirm);
    }

    private Label field(int row) {

        final Label label = new Label(guiLeft + fieldX + 5, rowTop(row) + 5, Component.empty()).withShadow();
        addRenderableWidget(label);
        return label;
    }

    private <E extends Enum<E>> ScrollInput choice(int row, E[] values, String key, int state, Label label,
            IntConsumer onPick) {

        final List<Component> options = Arrays.stream(values)
                .map(v -> (Component) Component.translatable(key + "." + v.name().toLowerCase(Locale.ROOT))).toList();
        return new SelectionScrollInput(guiLeft + fieldX, rowTop(row), fieldW, FIELD_H).forOptions(options)
                .titled(Component.translatable(key))
                .writingTo(label)
                .calling(onPick::accept)
                .setState(state);
    }

    private int rowTop(int row) {

        return guiTop + ROW_Y + row * ROW_STEP;
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        graphics.blit(BACKGROUND, guiLeft, guiTop, 0, 0, WIDTH, HEIGHT);
        graphics.drawString(font, title, guiLeft + (WIDTH - font.width(title)) / 2, guiTop + 4, TITLE, false);
        for (int row = 0; row < CAPTIONS.length; row++) {
            graphics.drawString(font, caption(CAPTIONS[row]), guiLeft + MARGIN, rowTop(row) + 5, CAPTION, true);
            InputField.draw(graphics, guiLeft + fieldX, rowTop(row), fieldW, FIELD_H);
        }
        cells(graphics);
        GuiGameElement.of(new ItemStack(ModItems.HILL_CORE.get())).<GuiGameElement.GuiRenderBuilder>at(guiLeft + WIDTH + 6,
                guiTop + HEIGHT - 60, -200).scale(4).render(graphics);
    }

    private static Component caption(String key) {

        return Component.translatable("hillsphere.core." + key);
    }

    /** Six cells at the right of the level field, lit up to the level in the polarity's colour (fractions too). */
    private void cells(GuiGraphics graphics) {

        final int[] ramp = Palette.of(polarity);
        final int y = rowTop(3) + 6;
        for (int i = 0; i < HillCoreBlock.MAX_LEVEL; i++) {
            final int x = guiLeft + fieldX + fieldW - 64 + i * 10;
            graphics.fill(x, y, x + 8, y + 6, 0xFF373737);
            final int lit = (int) Math.round(8 * Math.max(0, Math.min(1, level - i)));
            graphics.fill(x, y, x + lit, y + 6, 0xFF000000 | ramp[Palette.MAIN]);
            graphics.fill(x, y, x + lit, y + 1, 0xFF000000 | ramp[Palette.LIGHT]);
        }
    }

    @Override
    public void removed() {

        PacketDistributor.sendToServer(new CoreSettingsPayload(pos, polarity, level, mode, shape));
        super.removed();
    }
}
