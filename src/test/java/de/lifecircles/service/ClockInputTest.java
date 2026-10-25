package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import java.io.*;
import java.util.Random;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** TAKTGEBER.md: six inherited clocks on the cell's age, read as sine and cosine. */
class ClockInputTest {
    final SimulationConfig config = SimulationConfig.getInstance();

    static Cell cell() { return CellFactory.createCell(new Vector2D(100, 100), 10, .5, .01, .025); }

    @Test void clocksAreSineAndCosineOfTheAgeAfterAllOtherInputs() {
        Cell cell = cell();
        Environment world = new Environment(300, 300);
        world.addCell(cell);
        assertArrayEquals(Cell.DEFAULT_CLOCK_PERIODS, cell.getClockPeriods());
        cell.setClockPeriods(new double[]{.5, 1, 2, 4, 8, 3});
        cell.setAge(1.25);
        double[] inputs = CellBrainService.generateInputs(cell, world);
        assertEquals(EnergyInputLayout.inputCount(12), inputs.length);
        assertEquals(EnergyInputLayout.remainsInputCount(12), EnergyInputLayout.clockSine(12, 0), "appended after every older channel");
        assertEquals(0, inputs[EnergyInputLayout.clockSine(12, 0)], 1e-12);          // 2.5 turns
        assertEquals(-1, inputs[EnergyInputLayout.clockCosine(12, 0)], 1e-12);
        assertEquals(1, inputs[EnergyInputLayout.clockSine(12, 1)], 1e-12);          // 1.25 turns
        assertEquals(0, inputs[EnergyInputLayout.clockCosine(12, 1)], 1e-12);
        double angle = 2 * Math.PI * 1.25 / 3;
        assertEquals(Math.sin(angle), inputs[EnergyInputLayout.clockSine(12, 5)], 1e-12);
        assertEquals(Math.cos(angle), inputs[EnergyInputLayout.clockCosine(12, 5)], 1e-12);
    }

    @Test void periodsAreClampedAndMutateByAFactor() {
        Cell cell = cell();
        cell.setClockPeriods(new double[]{.01, 1, 2, 4, 8, 1000});
        assertEquals(Cell.MIN_CLOCK_PERIOD, cell.getClockPeriod(0));
        assertEquals(Cell.MAX_CLOCK_PERIOD, cell.getClockPeriod(5));
        assertThrows(IllegalArgumentException.class, () -> cell.setClockPeriods(new double[]{1, 2}));
        cell.setClockPeriods(Cell.DEFAULT_CLOCK_PERIODS);
        cell.mutateClockPeriods(0, .2, new Random(1));
        assertArrayEquals(Cell.DEFAULT_CLOCK_PERIODS, cell.getClockPeriods(), "rate 0 keeps them");
        cell.mutateClockPeriods(1, .2, new Random(1));
        for (int i = 0; i < Cell.DEFAULT_CLOCK_PERIODS.length; i++) {
            double factor = cell.getClockPeriod(i) / Cell.DEFAULT_CLOCK_PERIODS[i];
            assertNotEquals(1, factor);
            assertTrue(factor >= Math.exp(-.2) && factor <= Math.exp(.2), "factor " + factor);
        }
    }

    @Test void birthsAndTrainingFoundersInheritTheClocks() {
        double rate = config.getMutationRate(), strength = config.getMutationStrength();
        try {
            Environment world = new Environment(1000, 1000);
            Cell parent = cell();
            parent.setPosition(new Vector2D(500, 500));
            double[] periods = {.7, 1.3, 2.9, 5, 11, 40};
            parent.setClockPeriods(periods);
            config.setMutationRate(0);
            Cell copy = ReproductionManagerService.createTrainingFounder(config, world, parent, new Vector2D(200, 200), 0, true);
            assertArrayEquals(periods, copy.getClockPeriods(), "no mutation, same clocks");
            for (SensorActor actor : parent.getSensorActors()) actor.setReproductionDesire(1);
            Cell child = ReproductionManagerService.reproduceClonally(config, world, parent);
            assertArrayEquals(periods, child.getClockPeriods());
            config.setMutationRate(1); config.setMutationStrength(.2);
            Cell mutant = ReproductionManagerService.createTrainingFounder(config, world, parent, new Vector2D(200, 200), 0, true);
            assertNotEquals(periods[0], mutant.getClockPeriod(0));
            Cell elite = ReproductionManagerService.createTrainingFounder(config, world, parent, new Vector2D(200, 200), 0, false);
            assertArrayEquals(periods, elite.getClockPeriods(), "elite unchanged");
        } finally { config.setMutationRate(rate); config.setMutationStrength(strength); }
    }

    @Test void cellsSavedBeforeTheClocksLoadWithTheDefaultOctaves() throws Exception {
        Cell old = cell();
        old.setClockPeriods(new double[]{1, 1, 1, 1, 1, 1});
        var field = Cell.class.getDeclaredField("clockPeriods");
        field.setAccessible(true);
        field.set(old, null); // what an older save contains
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) { out.writeObject(old); }
        Cell loaded;
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { loaded = (Cell) in.readObject(); }
        assertArrayEquals(Cell.DEFAULT_CLOCK_PERIODS, loaded.getClockPeriods());
        assertEquals(EnergyInputLayout.inputCount(12), loaded.getBrain().getInputCount());
    }
}
