package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.EnergyType;
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
        Budget(Cell cell, EnergyType energyType) { this.cell = cell; energy = cell.getEnergy(energyType); }
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
        // Ports and their distance factors do not depend on the energy form: compute them once per step.
        List<Link> links = links(cells, environment);
        // Existing single-energy populations do not pay for a second transfer pass.
        int present = 0;
        for (Cell cell : cells) present |= cell.getEnergyPresenceMask() & ~1;
        List<EnergyType> types = new ArrayList<>(List.of(EnergyType.PRIMARY));
        for (EnergyType type : EnergyType.ALL) if ((present & (1 << type.getCode())) != 0) types.add(type);
        // Each form reads and changes only its own stores, so the forms are computed in parallel; writing stays
        // sequential in form order (a cell's store array for further forms is created on first write).
        List<Budget[]> results = types.size() == 1 ? List.<Budget[]>of(processEnergyType(cells, links, deltaTime, types.getFirst()))
                : types.parallelStream().map(type -> processEnergyType(cells, links, deltaTime, type)).toList();
        for (int t = 0; t < types.size(); t++)
            for (Budget b : results.get(t)) b.cell.setEnergy(types.get(t), b.energy + b.delta);
    }

    /** One active port from a living cell to an actor of another listed cell, by list index. */
    private record Link(int cell, int target, double delivery, double absorption, double deliveryFactor, double absorptionFactor) {}

    private static final Link[] NO_LINKS = {};

    private static List<Link> links(List<Cell> cells, Environment environment) {
        var index = new IdentityHashMap<Cell, Integer>(cells.size());
        for (int i = 0; i < cells.size(); i++) index.putIfAbsent(cells.get(i), i);
        // Cells independently in parallel (the index is only read), joined in list order.
        Link[][] perCell = new Link[cells.size()][];
        java.util.stream.IntStream.range(0, cells.size()).parallel().forEach(i -> perCell[i] = links(cells, environment, index, i));
        List<Link> links = new ArrayList<>();
        for (Link[] own : perCell) for (Link link : own) links.add(link);
        return links;
    }

    private static Link[] links(List<Cell> cells, Environment environment, IdentityHashMap<Cell, Integer> index, int i) {
        Cell cell = cells.get(i);
        if (cell.isRemains()) return NO_LINKS;
        List<Link> links = null;
        for (SensorActor sensor : cell.getSensorActors()) {
            if (!(sensor.getSensedCell() instanceof Cell target) || target == cell
                    || !(sensor.getSensedActor() instanceof SensorActor actor) || actor.getParentCell() != target) continue;
            Integer other = index.get(target);
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
            if (links == null) links = new ArrayList<>(4);
            links.add(new Link(index.get(cell), other, delivery, absorption, deliveryFactor, absorptionFactor));
        }
        return links == null ? NO_LINKS : links.toArray(NO_LINKS);
    }

    /** New stores of one form, not yet written: the touched budgets with their deltas. */
    private static Budget[] processEnergyType(List<Cell> cells, List<Link> links, double deltaTime, EnergyType energyType) {
        // Budgets only for cells with a port: stores of all other cells stay as they are (already bounded).
        // Links use the first list index of a cell, so a cell listed twice still shares one budget.
        var budgets = new Budget[cells.size()];
        List<Budget> touched = new ArrayList<>();
        List<Transfer> transfers = new ArrayList<>();
        for (Link link : links) {
            Budget own = budget(budgets, touched, cells, link.cell(), energyType), other = budget(budgets, touched, cells, link.target(), energyType);
            request(transfers, own, other, DELIVERY_PER_SECOND * link.delivery() * link.deliveryFactor() * deltaTime, true, energyType);
            request(transfers, other, own, ABSORPTION_PER_SECOND * link.absorption() * link.absorptionFactor() * deltaTime, false, energyType);
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
            double amount = t.amount * scale(Math.max(0, t.receiver.cell.getMaxEnergy(energyType) - t.receiver.energy), t.receiver.incoming);
            t.donor.delta -= amount;
            t.receiver.delta += amount;
        }
        return touched.toArray(new Budget[0]);
    }

    private static Budget budget(Budget[] budgets, List<Budget> touched, List<Cell> cells, int index, EnergyType energyType) {
        Budget budget = budgets[index];
        if (budget == null) { budget = budgets[index] = new Budget(cells.get(index), energyType); touched.add(budget); }
        return budget;
    }

    private static double scale(double available, double requested) {
        return requested > 0 ? Math.min(1, available / requested) : 0;
    }
    private static void request(List<Transfer> transfers, Budget donor, Budget receiver, double amount, boolean delivery, EnergyType energyType) {
        if (donor.energy <= (delivery ? DELIVERY_RESERVE : 0)
                || receiver.cell.isRemains()
                || receiver.energy >= receiver.cell.getMaxEnergy(energyType)) return;
        if (amount > 0 && Double.isFinite(amount)) transfers.add(new Transfer(donor, receiver, amount, delivery));
    }
}
