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
package dev.loki.hillsphere.field.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.loki.hillsphere.field.Polarity;

import org.junit.jupiter.api.Test;

class RedstoneModeTest {

    private static final double EPS = 1e-9;

    @Test
    void manualIgnoresTheSignal() {

        assertEquals(new Control(Polarity.ATTRACT, 4), RedstoneMode.MANUAL.apply(Polarity.ATTRACT, 4, 15));
        assertEquals(new Control(Polarity.ATTRACT, 4), RedstoneMode.MANUAL.apply(Polarity.ATTRACT, 4, 0));
    }

    @Test
    void aSignalSwitchesTheCoreOffOrOn() {

        assertEquals(0, RedstoneMode.OFF_ON_SIGNAL.apply(Polarity.REPEL, 3, 1).level(), EPS);
        assertEquals(3, RedstoneMode.OFF_ON_SIGNAL.apply(Polarity.REPEL, 3, 0).level(), EPS);
        assertEquals(3, RedstoneMode.ON_WITH_SIGNAL.apply(Polarity.REPEL, 3, 1).level(), EPS);
        assertEquals(0, RedstoneMode.ON_WITH_SIGNAL.apply(Polarity.REPEL, 3, 0).level(), EPS);
    }

    @Test
    void analogScalesThePanelLevelBySignalOutOfFifteen() {

        assertEquals(0, RedstoneMode.ANALOG.apply(Polarity.ATTRACT, 6, 0).level(), EPS);
        assertEquals(6 * 4 / 15.0, RedstoneMode.ANALOG.apply(Polarity.ATTRACT, 6, 4).level(), EPS);
        assertEquals(6, RedstoneMode.ANALOG.apply(Polarity.ATTRACT, 6, 15).level(), EPS);
        assertEquals(3, RedstoneMode.ANALOG.apply(Polarity.ATTRACT, 3, 15).level(), EPS);
    }

    @Test
    void aSignalAboveFifteenDoesNotOvershoot() {

        assertEquals(6, RedstoneMode.ANALOG.apply(Polarity.ATTRACT, 6, 99).level(), EPS);
    }

    @Test
    void reverseFlipsAttractionButNotLevitation() {

        assertEquals(Polarity.REPEL, RedstoneMode.REVERSE_ON_SIGNAL.apply(Polarity.ATTRACT, 2, 7).polarity());
        assertEquals(Polarity.ATTRACT, RedstoneMode.REVERSE_ON_SIGNAL.apply(Polarity.REPEL, 2, 7).polarity());
        assertEquals(Polarity.LEVITATE, RedstoneMode.REVERSE_ON_SIGNAL.apply(Polarity.LEVITATE, 2, 7).polarity());
        assertEquals(Polarity.ATTRACT, RedstoneMode.REVERSE_ON_SIGNAL.apply(Polarity.ATTRACT, 2, 0).polarity());
    }
}
