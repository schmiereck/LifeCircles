package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.NeuralNetwork;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Ports computed once per step must give bit-identical transfers to the former per-form computation. */
class EnergyTransferLinkRegressionTest {
    static final double DELIVERY_PER_SECOND = 0.005 * SimulationConfig.FPS, ABSORPTION_PER_SECOND = 0.01 * SimulationConfig.FPS, RESERVE = 0.1;

    @AfterEach void reset() { NeuralNetwork.setRandom(new Random()); }

    @Test void randomBrainsInACrowdTransferExactlyAsBefore() {
        NeuralNetwork.setRandom(new Random(7));
        Random random = new Random(8);
        Environment world = new Environment(600, 400, new Random(9));
        for (int i = 0; i < 80; i++) {
            Cell cell = CellFactory.createCell(new Vector2D(200 + random.nextDouble() * 200, 150 + random.nextDouble() * 150), 8 + random.nextDouble() * 10);
            for (EnergyType type : EnergyType.ALL) cell.setEnergy(type, random.nextDouble() * .5);
            if (i % 9 == 0) cell.becomeRemains();
            world.addCell(cell);
        }
        SimulationStepPolicy policy = new SimulationStepPolicy() {
            @Override public boolean useSunlight() { return false; }
            @Override public boolean allowReproduction(Cell cell) { return false; }
        };
        var grid = new SpatialGridPartitioningStrategy(600, 400, 80);
        int changed = 0;
        for (int step = 1; step <= 120; step++) {
            grid.build(world.getCellList()); world.update(1.0 / 60, grid, policy);
            if (step % 10 != 0) continue;
            List<Cell> cells = world.getCellList();
            double[][] before = snapshot(cells);
            EnergyTransferCellCalcService.processEnergyTransfers(cells, world, 1.0 / 60);
            double[][] actual = snapshot(cells);
            restore(cells, before);
            reference(cells, world, 1.0 / 60);
            double[][] expected = snapshot(cells);
            restore(cells, before);
            for (int i = 0; i < cells.size(); i++) {
                assertArrayEquals(expected[i], actual[i], 0, "cell " + i + " step " + step);
                if (!Arrays.equals(before[i], actual[i])) changed++;
            }
        }
        assertTrue(changed > 50, "transfers happened: " + changed);
    }

    static double[][] snapshot(List<Cell> cells) {
        double[][] e = new double[cells.size()][EnergyType.ALL.size()];
        for (int i = 0; i < cells.size(); i++) for (EnergyType type : EnergyType.ALL) e[i][type.getCode()] = cells.get(i).getEnergy(type);
        return e;
    }
    static void restore(List<Cell> cells, double[][] e) {
        for (int i = 0; i < cells.size(); i++) for (EnergyType type : EnergyType.ALL) cells.get(i).setEnergy(type, e[i][type.getCode()]);
    }

    // The former implementation (before ports were shared between forms), kept verbatim as reference.
    static final class Budget { final Cell cell; final double energy; double delivery, outgoing, incoming, delta;
        Budget(Cell cell, EnergyType type) { this.cell = cell; energy = cell.getEnergy(type); } }
    static final class Transfer { final Budget donor, receiver; final boolean delivery; double amount;
        Transfer(Budget donor, Budget receiver, double amount, boolean delivery) {
            this.donor = donor; this.receiver = receiver; this.amount = amount; this.delivery = delivery;
            if (delivery) donor.delivery += amount; } }

    static void reference(List<Cell> cells, Environment environment, double deltaTime) {
        processType(cells, environment, deltaTime, EnergyType.PRIMARY);
        int present = 0;
        for (Cell cell : cells) present |= cell.getEnergyPresenceMask() & ~1;
        for (EnergyType type : EnergyType.ALL) if ((present & (1 << type.getCode())) != 0) processType(cells, environment, deltaTime, type);
    }
    static void processType(List<Cell> cells, Environment environment, double deltaTime, EnergyType energyType) {
        var budgets = new IdentityHashMap<Cell, Budget>(cells.size());
        for (Cell cell : cells) budgets.put(cell, new Budget(cell, energyType));
        List<Transfer> transfers = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.isRemains()) continue;
            Budget own = budgets.get(cell);
            for (SensorActor sensor : cell.getSensorActors()) {
                if (!(sensor.getSensedCell() instanceof Cell target) || target == cell
                        || !(sensor.getSensedActor() instanceof SensorActor actor) || actor.getParentCell() != target) continue;
                Budget other = budgets.get(target);
                if (other == null) continue;
                double delivery = sensor.getEnergyDelivery(), absorption = sensor.getEnergyAbsorption();
                if (!(delivery > 0) && !(absorption > 0)) continue;
                if (sensor.getCachedPosition() == null || actor.getCachedPosition() == null) continue;
                double distance = sensor.getCachedPosition().getShortestDirectionWithWrapAround(
                        actor.getCachedPosition(), environment.getWidth(), environment.getHeight()).length();
                double radius = SensorActorForceCellCalcService.calcSensorRadius(target.getRadiusSize(), target.getSensorActors().size());
                double normDistance = radius > 0 ? Math.min(distance / radius, 1) : 0;
                double strength = cell.getRadiusSize() / SimulationConfig.getInstance().getCellMaxRadiusSize();
                double deliveryFactor = 1 - (1 - SimulationConfig.CELL_ENERGY_DELIVERY_FACTOR_FAR) * normDistance * strength;
                double absorptionFactor = 1 - (1 - SimulationConfig.CELL_ENERGY_ABSORPTION_FACTOR_FAR) * normDistance * strength;
                request(transfers, own, other, DELIVERY_PER_SECOND * delivery * deliveryFactor * deltaTime, true, energyType);
                request(transfers, other, own, ABSORPTION_PER_SECOND * absorption * absorptionFactor * deltaTime, false, energyType);
            }
        }
        for (Transfer t : transfers) {
            if (t.delivery) t.amount *= scale(Math.max(0, t.donor.energy - RESERVE), t.donor.delivery);
            t.donor.outgoing += t.amount;
        }
        for (Transfer t : transfers) { t.amount *= scale(t.donor.energy, t.donor.outgoing); t.receiver.incoming += t.amount; }
        for (Transfer t : transfers) {
            double amount = t.amount * scale(Math.max(0, t.receiver.cell.getMaxEnergy(energyType) - t.receiver.energy), t.receiver.incoming);
            t.donor.delta -= amount; t.receiver.delta += amount;
        }
        for (Budget b : budgets.values()) b.cell.setEnergy(energyType, b.energy + b.delta);
    }
    static double scale(double available, double requested) { return requested > 0 ? Math.min(1, available / requested) : 0; }
    static void request(List<Transfer> transfers, Budget donor, Budget receiver, double amount, boolean delivery, EnergyType energyType) {
        if (donor.energy <= (delivery ? RESERVE : 0) || receiver.cell.isRemains()
                || receiver.energy >= receiver.cell.getMaxEnergy(energyType)) return;
        if (amount > 0 && Double.isFinite(amount)) transfers.add(new Transfer(donor, receiver, amount, delivery));
    }
}
