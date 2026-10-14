package de.lifecircles.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class TissueMechanicsTest {
    private MechanicsLab.Settings settings;
    @BeforeEach void save() { settings = new MechanicsLab.Settings(); }
    @AfterEach void restore() { settings.close(); }
    private TissueMechanicsLab.Result probe(String geometry, int columns, int layers, String load, double force) {
        return TissueMechanicsLab.run(new TissueMechanicsLab.Probe(geometry, columns, layers, load, force, 1.0 / 60));
    }
    private void intact(TissueMechanicsLab.Result r) {
        assertEquals(1, r.minLinks(), r.toString());
        assertEquals(1, r.minComponent(), r.toString());
    }

    @ParameterizedTest @ValueSource(ints = {5, 9})
    void packingLayersIncreaseBendingStiffnessAndRecover(int columns) {
        var chain = probe("bar", columns, 1, "bend", 6);
        var two = probe("bar", columns, 2, "bend", 6);
        var three = probe("bar", columns, 3, "bend", 6);
        intact(chain); intact(two); intact(three);
        assertTrue(chain.peakBend() > 10 * two.peakBend());
        assertTrue(two.peakBend() > 2 * three.peakBend());
        assertTrue(two.residualShape() < .001); assertTrue(three.residualShape() < .001);
        assertTrue(three.residualSpeed() < .001);
    }

    @Test void narrowBridgeBendsWhileThickEndPiecesKeepTheirShape() {
        var neck = probe("neck", 9, 3, "bend", 6);
        var solid = probe("bar", 9, 3, "bend", 6);
        intact(neck);
        assertTrue(neck.peakBend() > 10 * solid.peakBend());
        assertTrue(neck.leftShape() < .01); assertTrue(neck.rightShape() < .01);
        // A flexible bridge has no guaranteed preferred rest angle.
        assertTrue(neck.residualShape() > .1);
    }

    @Test void threeLayersCarryTensileLoadThatBreaksOneAndTwoLayers() {
        for (int layers : new int[]{1, 2}) {
            var broken = probe("bar", 9, layers, "tension", 150);
            assertTrue(broken.minComponent() < .9); assertTrue(broken.minLinks() < .9);
        }
        var strong = probe("bar", 9, 3, "tension", 150);
        intact(strong); assertTrue(strong.residualShape() < .001);
    }

    @ParameterizedTest @ValueSource(doubles = {6, 150})
    void lateralDisturbanceRevealsChainBucklingButPackedBarResists(double force) {
        var chain = probe("bar", 9, 1, "compression", force);
        var packed = probe("bar", 9, 3, "compression", force);
        intact(chain); intact(packed);
        assertTrue(chain.peakBend() > .5); assertTrue(packed.peakBend() < .02);
        assertTrue(packed.residualShape() < .001);
    }

    @ParameterizedTest @ValueSource(ints = {2, 3})
    void prescribedSizeCyclesDoWorkAgainstLoadAndRecover(int layers) {
        var active = probe("bar", 9, layers, "active", 6);
        intact(active);
        for (double stroke : new double[]{active.stroke1(), active.stroke2(), active.stroke3()}) {
            assertTrue(stroke > .15 && stroke < .18);
        }
        assertEquals(active.stroke2(), active.stroke3(), .002);
        assertTrue(active.peakWork() > 100);
        assertTrue(active.residualShape() < .001); assertTrue(Math.abs(active.residualStrain()) < .001);
        assertTrue(active.residualSpeed() < .001);
    }

    @Test void singleActiveRowCannotReplaceLateralSupport() {
        var row = probe("bar", 9, 1, "active", 6);
        intact(row); assertTrue(row.peakBend() > 1); assertTrue(row.residualShape() > .1);
    }

    @ParameterizedTest @ValueSource(strings = {"bend", "active"})
    void smallAndLargeTimeStepsAgreeForIntactPackedBar(String loading) {
        var coarse = probe("bar", 9, 3, loading, 6);
        var fine = TissueMechanicsLab.run(new TissueMechanicsLab.Probe("bar", 9, 3, loading, 6, 1.0 / 120));
        intact(coarse); intact(fine);
        assertEquals(coarse.peakBend(), fine.peakBend(), .001);
        assertEquals(coarse.residualShape(), fine.residualShape(), .001);
        if (loading.equals("active")) assertEquals(coarse.stroke3(), fine.stroke3(), .002);
    }

    @Test void unloadedControlsDistinguishStableBarFromDriftingBridge() {
        var bar = probe("bar", 9, 3, "bend", 0);
        var neck = probe("neck", 9, 3, "bend", 0);
        intact(bar); intact(neck);
        assertTrue(bar.peakShape() < .001, bar.toString());
        // Exposes a present limitation: cohesion alone does not enforce a rest angle.
        assertTrue(neck.peakShape() > .05, neck.toString());
        assertTrue(neck.leftShape() < .01); assertTrue(neck.rightShape() < .01);
    }
}
