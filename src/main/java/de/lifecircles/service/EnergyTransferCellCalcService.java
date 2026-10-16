package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.SensorActor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** Conservative transfers computed from one shared pre-transfer energy snapshot. */
public class EnergyTransferCellCalcService {
    // Preserve the former full-output throughput at 60 physics steps per second.
    private static final double DELIVERY_PER_SECOND = 0.005 * SimulationConfig.FPS;
    private static final double ABSORPTION_PER_SECOND = 0.01 * SimulationConfig.FPS;
    private static final double DELIVERY_RESERVE = 0.1;

    private static final class Budget {
        final Cell cell;
        final double energy;
        double delivery, outgoing, incoming, delta;
        Budget(Cell cell) { this.cell = cell; energy = cell.getEnergy(); }
    }
    private static final class Transfer {
        final Budget donor, receiver;
        final boolean delivery;
        double amount;
        Transfer(Budget donor, Budget receiver, double amount, boolean delivery) {
            this.donor = donor; this.receiver = receiver; this.amount = amount; this.delivery = delivery;
            if (delivery) donor.delivery += amount;
        }
    }

    /** Convenience for callers using the standard 60 Hz physics step. */
    public static void processEnergyTransfers(List<Cell> cells) {
        processEnergyTransfers(cells, Environment.getInstance());
    }
    public static void processEnergyTransfers(List<Cell> cells, Environment environment) {
        processEnergyTransfers(cells, environment, 1.0 / SimulationConfig.FPS);
    }

    /**
     * Delivery shares energy above 0.1; absorption can take the donor's last energy.
     * Commands are linear in output [0,1], distance attenuation and simulation time.
     * Scarce donor budgets and receiver space are shared proportionally between requests.
     * Received energy becomes available for onward transfer on the next step.
     */
    public static void processEnergyTransfers(List<Cell> cells, Environment environment, double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime < 0) throw new IllegalArgumentException("Invalid transfer time step");
        if (deltaTime == 0 || cells.isEmpty()) return;
        var budgets = new IdentityHashMap<Cell, Budget>(cells.size());
        for (Cell cell : cells) budgets.put(cell, new Budget(cell));
        List<Transfer> transfers = new ArrayList<>();
        for (Cell cell : cells) {
            Budget own = budgets.get(cell);
            for (SensorActor sensor : cell.getSensorActors()) {
                if (!(sensor.getSensedCell() instanceof Cell target) || target == cell
                        || !(sensor.getSensedActor() instanceof SensorActor actor) || actor.getParentCell() != target) continue;
                Budget other = budgets.get(target);
                if (other == null) continue;
                double delivery = sensor.getEnergyDelivery(), absorption = sensor.getEnergyAbsorption();
                if (!(delivery > 0) && !(absorption > 0)) continue;
                // Missing geometry must not act as a zero-distance connection.
                if (sensor.getCachedPosition() == null || actor.getCachedPosition() == null) continue;
                double distance = sensor.getCachedPosition().getShortestDirectionWithWrapAround(
                        actor.getCachedPosition(), environment.getWidth(), environment.getHeight()).length();
                double radius = SensorActorForceCellCalcService.calcSensorRadius(target.getRadiusSize(), target.getSensorActors().size());
                double normDistance = radius > 0 ? Math.min(distance / radius, 1) : 0;
                double strength = cell.getRadiusSize() / SimulationConfig.getInstance().getCellMaxRadiusSize();
                double deliveryFactor = 1 - (1 - SimulationConfig.CELL_ENERGY_DELIVERY_FACTOR_FAR) * normDistance * strength;
                double absorptionFactor = 1 - (1 - SimulationConfig.CELL_ENERGY_ABSORPTION_FACTOR_FAR) * normDistance * strength;
                request(transfers, own, other, DELIVERY_PER_SECOND * delivery * deliveryFactor * deltaTime, true);
                request(transfers, other, own, ABSORPTION_PER_SECOND * absorption * absorptionFactor * deltaTime, false);
            }
        }
        // First enforce the voluntary delivery reserve across ALL ports.
        for (Transfer t : transfers) {
            if (t.delivery) t.amount *= scale(Math.max(0, t.donor.energy - DELIVERY_RESERVE), t.donor.delivery);
            t.donor.outgoing += t.amount;
        }
        // Absorption and delivery compete for the same finite donor energy.
        for (Transfer t : transfers) {
            t.amount *= scale(t.donor.energy, t.donor.outgoing);
            t.receiver.incoming += t.amount;
        }
        // Receiver room uses the same snapshot: no order-dependent same-step relaying.
        for (Transfer t : transfers) {
            double amount = t.amount * scale(Math.max(0, t.receiver.cell.getMaxEnergy() - t.receiver.energy), t.receiver.incoming);
            t.donor.delta -= amount;
            t.receiver.delta += amount;
        }
        for (Budget b : budgets.values()) b.cell.setEnergy(b.energy + b.delta);
    }

    private static double scale(double available, double requested) {
        return requested > 0 ? Math.min(1, available / requested) : 0;
    }
    private static void request(List<Transfer> transfers, Budget donor, Budget receiver, double amount, boolean delivery) {
        if (donor.energy <= (delivery ? DELIVERY_RESERVE : 0)
                || receiver.energy >= receiver.cell.getMaxEnergy()) return;
        if (amount > 0 && Double.isFinite(amount)) transfers.add(new Transfer(donor, receiver, amount, delivery));
    }
}
