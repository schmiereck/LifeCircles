package de.lifecircles.model;

import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategyFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CellSensorVisibilityTest {

    private final double timeStep = 0.1D;

    @Test
    public void testCellsSeeEachOtherWhenClose() {
        // Mini-Environment für den Test erstellen
        Environment environment = new Environment(100, 100);
        SimulationConfig config = SimulationConfig.getInstance();

        // Zellen mit Radius 5, Abstand 5 (sollten sich "sehen")
        //TestTrainStrategyUtils.createAndAddCell(environment, 0, 0, 5, true, false, false);
        //TestTrainStrategyUtils.createAndAddCell(environment, 5, 0, 5, true, false, false);
        createAndAddCell(environment, 10.0D, new CellType(0.1D, 0.2D, 0.3D));
        //createAndAddCell(environment, 15.03D, new CellType(0.4D, 0.5D, 0.6D));
        createAndAddCell(environment, 20.03D, new CellType(0.4D, 0.5D, 0.6D));
        config.setInitialCellCount(2);

        final List<Cell> cellList = environment.getCellList();
        //cellList.get(1).setPosition(new Vector2D(20.03D, 8.0D));

        calcCellList(cellList);

        // Prüfe zusätzlich, ob die Sensoren der Zellen die andere Zelle wahrnehmen
        // (Überprüfung der Brain-Inputs)
        Cell cell1 = cellList.get(0);
        Cell cell2 = cellList.get(1);
        SensorDetectionResult sensorDetection = verifySensorDetection(cell1, cell2);
        assertTrue(sensorDetection.detected, "Die Sensoren von cell1 sollten cell2 detektieren");
    }

    @Test
    public void testCellsDoNotSeeEachOtherWhenFar() {
        // Mini-Environment für den Test erstellen
        Environment environment = new Environment(100, 100);
        SimulationConfig config = SimulationConfig.getInstance();

        // Zellen mit Radius 5, Abstand 20 (sollten sich NICHT sehen)
        //TestTrainStrategyUtils.createAndAddCell(env, 0, 0, 5, true, false, false);
        //TestTrainStrategyUtils.createAndAddCell(env, 20, 0, 5, true, false, false);
        createAndAddCell(environment, 10.0D, new CellType(0.1D, 0.2D, 0.3D)); // Rote Zelle
        createAndAddCell(environment, 25.0D, new CellType(0.4D, 0.5D, 0.6D));
        config.setInitialCellCount(2);

        final List<Cell> cellList = environment.getCellList();

        calcCellList(cellList);

        // Prüfe zusätzlich, ob die Sensoren der Zellen die andere Zelle NICHT wahrnehmen
        Cell cell1 = cellList.get(0);
        Cell cell2 = cellList.get(1);
        SensorDetectionResult sensorDetection = verifySensorDetection(cell1, cell2);
        assertFalse(sensorDetection.detected, "Die Sensoren von cell1 sollten cell2 NICHT detektieren");
    }

    private static void createAndAddCell(final Environment environment, final double x, final CellType cellType) {
        double y = 10.0D;
        double cellRadiusSize = 5.0D;
        Cell cell = CellFactory.createCell(new Vector2D(x, y), cellRadiusSize,
                SimulationConfig.hiddenCountFactorDefault, SimulationConfig.stateHiddenLayerSynapseConnectivityDefault, SimulationConfig.hiddenLayerSynapseConnectivityDefault);
        cell.setType(cellType);
        environment.addCell(cell);
    }

    private void calcCellList(List<Cell> cellList) {
        // Environment aktualisieren, damit die Sensoren verarbeitet werden
        PartitioningStrategy partitioningStrategy = PartitioningStrategyFactory.createStrategy(100, 100, 20);
        // Update all cells with their neighborhood information
        partitioningStrategy.build(cellList);

        //environment.update(timeStep, partitioningStrategy);

        cellList.forEach(cell -> CellBrainService.think(cell));

        // Process repulsive forces
        RepulsionCellCalcService.processRepulsiveForces(cellList, partitioningStrategy);
        // Process sensor/actor interactions
        SensorActorForceCellCalcService.processInteractions(cellList, partitioningStrategy);
        // Process energy transfers between cells
        EnergyTransferCellCalcService.processEnergyTransfers(cellList);

        cellList.forEach(cell -> CellBrainService.think(cell));

        // Update energy and age
        cellList.forEach(cell -> EnergyCellCalcService.decayEnergy(cell, timeStep, true));
        cellList.forEach(cell -> cell.incAge(timeStep));
    }

    /**
     * Ergebnisklasse für die Sensor-Detektions-Prüfung
     */
    private static class SensorDetectionResult {
        public boolean detected;                // True wenn mindestens ein Sensor die Zielzelle detektiert hat
    }

    /**
     * Prüft, ob die Sensoren einer Zelle eine andere Zelle tatsächlich detektieren
     * und ob die Informationen im neuronalen Netzwerk korrekt abgebildet sind.
     *
     * @param observingCell Die beobachtende Zelle
     * @param targetCell Die Zielzelle, die potentiell erkannt wird
     * @return Ergebnis mit den Details der Erkennung
     */
    private SensorDetectionResult verifySensorDetection(Cell observingCell, Cell targetCell) {
        SensorDetectionResult result = new SensorDetectionResult();
        result.detected = false;

        // Jetzt prüfen wir jeden Sensor der beobachtenden Zelle
        List<SensorActor> sensors = observingCell.getSensorActors();

        // 1. Prüfe, ob mindestens ein Sensor die Zielzelle detektiert hat
        for (int sensorIndex = 0; sensorIndex < sensors.size(); sensorIndex++) {
            SensorActor sensor = sensors.get(sensorIndex);
            SensableCell sensedCell = sensor.getSensedCell();

            // Prüfen, ob der Sensor überhaupt etwas detektiert hat
            if (sensedCell != null && sensedCell == targetCell) {
                result.detected = true;

                CellBrainInterface brain = observingCell.getBrain();

                // Hole den Index des Sensors, der die Zelle erkannt hat
                SensableActor sensedActor = sensor.getSensedActor();

                // Berechne die Position der Sensor-spezifischen Inputs im NN-Input-Array
                int sensorInputOffset = GlobalInputFeature.values().length +
                        (sensorIndex * SensorInputFeature.values().length);

                // Prüfe, ob in den Inputs Informationen über die Zielzelle vorhanden sind
                // Wir prüfen, ob mindestens ein Sensor die entsprechenden Inputs hat:

                // Position für die erkannte Zelltyp-Farbe im Input-Array
                int sensedCellRedPos = sensorInputOffset + SensorInputFeature.SENSED_CELL_TYPE_RED.ordinal();
                int sensedCellGreenPos = sensorInputOffset + SensorInputFeature.SENSED_CELL_TYPE_GREEN.ordinal();
                int sensedCellBluePos = sensorInputOffset + SensorInputFeature.SENSED_CELL_TYPE_BLUE.ordinal();

                double sensedCellRed = brain.getInputValue(sensedCellRedPos);
                double sensedCellGreen = brain.getInputValue(sensedCellGreenPos);
                double sensedCellBlue = brain.getInputValue(sensedCellBluePos);

                //double distance = sensor.getCachedPosition().distance(sensedActor.getCachedPosition());
                double distanceFactor = CellBrainService.calcDistanceFactor(sensor, sensedActor);

                // Überprüfen auf nicht-Null-Werte (wenn die Zelle erkannt wurde,
                // sollten hier die Farbwerte des Zelltyps stehen)
                CellType targetCellType = targetCell.getType();

                // Überprüfen, ob die Farbwerte aus dem Zelltyp korrekt ins Brain übertragen wurden
                assertEquals(targetCellType.getRed() * distanceFactor, sensedCellRed, 0.01,
                        "Der Rot-Wert des Zelltyps sollte im NN-Input korrekt abgebildet sein");
                assertEquals(targetCellType.getGreen() * distanceFactor, sensedCellGreen, 0.01,
                        "Der Grün-Wert des Zelltyps sollte im NN-Input korrekt abgebildet sein");
                assertEquals(targetCellType.getBlue() * distanceFactor, sensedCellBlue, 0.01,
                        "Der Blau-Wert des Zelltyps sollte im NN-Input korrekt abgebildet sein");

                // Überprüfe weitere SensorInput-Features

                // SENSED_ACTOR_FORCE_STRENGTH überprüfen
                int sensedActorForceStrengthPos = sensorInputOffset + SensorInputFeature.SENSED_ACTOR_FORCE_STRENGTH.ordinal();
                double sensedActorForceStrength = brain.getInputValue(sensedActorForceStrengthPos);

                // Ermittle den tatsächlichen Wert vom entsprechenden SensorActor der Zielzelle
                if (sensedActor != null) {
                    double expectedForceStrength = sensedActor.getForceStrength();
                    assertEquals(expectedForceStrength, sensedActorForceStrength, 0.01,
                            "Die Kraftstärke des Sensors sollte im NN-Input korrekt abgebildet sein");
                }

                // SENSED_ACTOR_TYPE überprüfen (Farbe des SensorActors)
                int sensedActorTypeRedPos = sensorInputOffset + SensorInputFeature.SENSED_ACTOR_TYPE_RED.ordinal();
                int sensedActorTypeGreenPos = sensorInputOffset + SensorInputFeature.SENSED_ACTOR_TYPE_GREEN.ordinal();
                int sensedActorTypeBluePos = sensorInputOffset + SensorInputFeature.SENSED_ACTOR_TYPE_BLUE.ordinal();

                double sensedActorRed = brain.getInputValue(sensedActorTypeRedPos);
                double sensedActorGreen = brain.getInputValue(sensedActorTypeGreenPos);
                double sensedActorBlue = brain.getInputValue(sensedActorTypeBluePos);

                if (sensedActor != null) {
                    CellType actorType = sensedActor.getType();
                    assertEquals(actorType.getRed(), sensedActorRed, 0.01,
                            "Der Rot-Wert des Sensor-Typs sollte im NN-Input korrekt abgebildet sein");
                    assertEquals(actorType.getGreen(), sensedActorGreen, 0.01,
                            "Der Grün-Wert des Sensor-Typs sollte im NN-Input korrekt abgebildet sein");
                    assertEquals(actorType.getBlue(), sensedActorBlue, 0.01,
                            "Der Blau-Wert des Sensor-Typs sollte im NN-Input korrekt abgebildet sein");
                }

                // Zellzustand prüfen (SENSED_CELL_STATE)
                int sensedCellState0Pos = sensorInputOffset + SensorInputFeature.SENSED_CELL_STATE_0.ordinal();
                int sensedCellState1Pos = sensorInputOffset + SensorInputFeature.SENSED_CELL_STATE_1.ordinal();
                int sensedCellState2Pos = sensorInputOffset + SensorInputFeature.SENSED_CELL_STATE_2.ordinal();

                double sensedCellState0 = brain.getInputValue(sensedCellState0Pos);
                double sensedCellState1 = brain.getInputValue(sensedCellState1Pos);
                double sensedCellState2 = brain.getInputValue(sensedCellState2Pos);

                // Erwartete Zustandswerte aus der Zielzelle holen
                double[] normalizedState = targetCell.getNormalizedCellState();

                assertEquals(normalizedState[0], sensedCellState0, 0.01,
                        "Zellzustand 0 sollte im NN-Input korrekt abgebildet sein");
                assertEquals(normalizedState[1], sensedCellState1, 0.01,
                        "Zellzustand 1 sollte im NN-Input korrekt abgebildet sein");
                assertEquals(normalizedState[2], sensedCellState2, 0.01,
                        "Zellzustand 2 sollte im NN-Input korrekt abgebildet sein");

                // Optional: Auch die Energie- und Alters-Informationen überprüfen
                int sensedCellEnergyPos = sensorInputOffset + SensorInputFeature.SENSED_CELL_ENERGY.ordinal();
                int sensedCellAgePos = sensorInputOffset + SensorInputFeature.SENSED_CELL_AGE.ordinal();

                double sensedCellEnergy = brain.getInputValue(sensedCellEnergyPos);
                double sensedCellAge = brain.getInputValue(sensedCellAgePos);

                // Prüfen, ob Energie und Alter korrekt übertragen wurden
                assertEquals(targetCell.getEnergy(), sensedCellEnergy, 0.01,
                        "Die Energie der Zelle sollte im NN-Input korrekt abgebildet sein.");
                assertEquals(targetCell.getAge(), sensedCellAge + timeStep, 0.01,
                        "Das Alter der Zelle sollte im NN-Input korrekt abgebildet sein.");

                System.out.printf("Sensor %d hat eine Zelle erkannt: %s, %.3f (%s)%n",
                        sensorIndex, targetCell, distanceFactor, sensedActor);
            }
        }

        return result;
    }
}
