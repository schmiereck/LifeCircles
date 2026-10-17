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
}
