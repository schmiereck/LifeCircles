package de.lifecircles.view;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellDetailFormatTest {
    @Test void smallReservesStayVisible() {
        assertEquals("0",CellDetailView.formatAmount(0));
        assertEquals(String.format("%.3f",.25),CellDetailView.formatAmount(.25));
        assertEquals(String.format("%.2e",.00042),CellDetailView.formatAmount(.00042));
        assertNotEquals(CellDetailView.formatAmount(0),CellDetailView.formatAmount(1e-9));
    }

    /** The brain picture is copied under the lock and drawn later; it holds every placed synapse and neuron value. */
    @Test void brainPictureCopiesLayersValuesAndSynapses() {
        var cell = de.lifecircles.model.CellFactory.createCell(new de.lifecircles.model.Vector2D(10, 10), 10);
        var picture = CellDetailView.captureBrain(cell);
        var net = ((de.lifecircles.model.neural.CellBrain) cell.getBrain()).getNeuralNetwork();
        assertNull(picture.message());
        assertArrayEquals(net.getLayerSizes(), picture.layers());
        assertEquals(net.getSynapseCount(), picture.weights().length);
        for (double[] column : picture.activation()) for (double value : column) assertTrue(value >= 0 && value <= 1);
        cell.becomeRemains();
        assertNotNull(CellDetailView.captureBrain(cell).message());
    }
}
