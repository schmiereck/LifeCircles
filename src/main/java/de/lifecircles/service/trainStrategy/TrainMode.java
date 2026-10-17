package de.lifecircles.service.trainStrategy;

/**
 * Enumeration of training modes for the simulation.
 */
public enum TrainMode {
    NONE,
    CARRION_RAIN,
    EVOLUTIONARY,
    HIGH_ENERGY,
    HIGH_POSITION,
    HIGH_POSITION2,
    HIGH_POSITION3,

    TEST_A,
    TEST_B,
    TEST_C;

    public String description() {
        return switch(this) {
            case NONE, EVOLUTIONARY -> "Offene Evolution ohne Trainingsauswahl. Sonnen- und Zellregler gelten wie eingestellt.";
            case CARRION_RAIN -> "Aasregen: acht Versuchskammern, keine Sonne, keine Teilung. Überleben und Energieaufnahme aus Überresten werden selektiert. Episoden: 120 Simulationssekunden. Gehirnkosten und Lebensdauer gelten wie eingestellt.";
            case HIGH_ENERGY -> "Selektion nach Primärenergie. Dieses ältere Szenario setzt die Energie pro Sonnenstrahl auf 0.02.";
            case HIGH_POSITION -> "Älteres Höhentraining (HIGH_POSITION).";
            case HIGH_POSITION2 -> "Älteres Höhentraining mit getrennten Trainingsbereichen (HIGH_POSITION2).";
            case HIGH_POSITION3 -> "Physiktraining: Halten, Störungen, Größensteuerung, Aufbau. Keine Sonne; Energie gesichert.";
            case TEST_A -> "Fest verdrahteter mechanischer Testaufbau A (Schneemann).";
            case TEST_B -> "Fest verdrahteter Testaufbau B.";
            case TEST_C -> "Fest verdrahteter Testaufbau C.";
        };
    }
}
