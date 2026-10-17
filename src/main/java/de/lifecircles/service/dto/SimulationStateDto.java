package de.lifecircles.service.dto;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.EnergyType;
import de.lifecircles.model.SunRay;
import de.lifecircles.model.Vector2D;
import javafx.scene.paint.Color;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Detached, immutable values for one complete simulation state.
 * Capture must be protected from concurrent writes to the source model.
 * Used to transfer state between calculation and visualization threads.
 */
public class SimulationStateDto {
    private final List<CellStateDto> cells;
    private final List<BlockerStateDto> blockers;
    private final List<SunRayStateDto> sunRays;
    private final double width;
    private final double height;

    public SimulationStateDto(List<CellStateDto> cells, List<Blocker> blockers, List<SunRay> sunRays, double width, double height) {
        this.cells = List.copyOf(cells);
        this.blockers = blockers.stream()
            .map(BlockerStateDto::new)
            .collect(Collectors.toList());
        this.sunRays = sunRays.stream().filter(Objects::nonNull).map(SunRayStateDto::new).collect(Collectors.toList());
        this.width = width;
        this.height = height;
    }

    public List<CellStateDto> getCells() {
        return Collections.unmodifiableList(cells);
    }

    public List<BlockerStateDto> getBlockers() {
        return Collections.unmodifiableList(blockers);
    }

    public List<SunRayStateDto> getSunRays() {
        return Collections.unmodifiableList(sunRays);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public static class CellStateDto {
        private final Vector2D position;
        private final double rotation;
        private final double radiusSize;
        private final double[] typeRGB;
        private final List<ActorStateDto> actors;
        private final double energy;
        private final double secondaryEnergy;
        private final double[] additionalEnergies;
        private final boolean remains;
        private final double age;
        private final int cellState; // Zell-Zustand

        public CellStateDto(Vector2D position, double rotation, double radiusSize,
                            double[] typeRGB, List<ActorStateDto> actors, double energy, double age, int cellState) {
            this(position, rotation, radiusSize, typeRGB, actors, energy, 0, age, cellState);
        }

        public CellStateDto(Vector2D position, double rotation, double radiusSize,
                            double[] typeRGB, List<ActorStateDto> actors, double energy, double secondaryEnergy,
                            double age, int cellState) {
            this(position, rotation, radiusSize, typeRGB, actors, energy, secondaryEnergy,
                    new double[EnergyType.ALL.size() - 2], age, cellState);
        }

        public CellStateDto(Vector2D position, double rotation, double radiusSize,
                            double[] typeRGB, List<ActorStateDto> actors, double energy, double secondaryEnergy,
                            double[] additionalEnergies, double age, int cellState) {
            this(position, rotation, radiusSize, typeRGB, actors, energy, secondaryEnergy, additionalEnergies, age, cellState, false);
        }

        public CellStateDto(Vector2D position, double rotation, double radiusSize,
                            double[] typeRGB, List<ActorStateDto> actors, double energy, double secondaryEnergy,
                            double[] additionalEnergies, double age, int cellState, boolean remains) {

            this.position = new Vector2D(position.getX(), position.getY());
            this.rotation = rotation;
            this.radiusSize = radiusSize;
            this.typeRGB = typeRGB.clone();
            this.actors = List.copyOf(actors);
            this.energy = energy;
            this.secondaryEnergy = secondaryEnergy;
            this.additionalEnergies = additionalEnergies.clone();
            this.remains = remains;
            this.age = age;
            this.cellState = cellState; // Setze Zell-Zustand
        }

        public Vector2D getPosition() {
            return new Vector2D(position.getX(), position.getY());
        }

        public double getRotation() {
            return rotation;
        }

        public double getRadiusSize() {
            return radiusSize;
        }

        public double[] getTypeRGB() {
            return typeRGB.clone();
        }

        public List<ActorStateDto> getActors() {
            return Collections.unmodifiableList(actors);
        }

        public double getAge() {
            return age;
        }

        public double getEnergy() {
            return energy;
        }

        public double getEnergy(EnergyType energyType) {
            return switch (energyType) {
                case PRIMARY -> energy;
                case SECONDARY -> secondaryEnergy;
                default -> additionalEnergies[energyType.getCode() - 2];
            };
        }
        public boolean isRemains() { return remains; }

        public int getCellState() {
            return cellState;
        }
    }

    public static class ActorStateDto {
        private final Vector2D position;
        private final double[] typeRGB;
        private final double forceStrength;

        public ActorStateDto(Vector2D position, double[] typeRGB, double forceStrength) {
            this.position = new Vector2D(position.getX(), position.getY());
            this.typeRGB = typeRGB.clone();
            this.forceStrength = forceStrength;
        }

        public Vector2D getPosition() {
            return new Vector2D(position.getX(), position.getY());
        }

        public double[] getTypeRGB() {
            return typeRGB.clone();
        }

        public double getForceStrength() {
            return forceStrength;
        }
    }

    public static class BlockerStateDto {
        private final double x;
        private final double y;
        private final double width;
        private final double height;
        private final Color color;
        private final Blocker.BlockerType type;

        public double getHeight() {
            return height;
        }

        public BlockerStateDto(Blocker blocker) {
            Vector2D pos = blocker.getPosition();
            this.x = pos.getX();
            this.y = pos.getY();
            this.width = blocker.getWidth();
            this.height = blocker.getHeight();
            this.color = blocker.getColor();
            this.type = blocker.getBlockerType();
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public Color getColor() { return color; }
        public Blocker.BlockerType getType() { return type; }
    }

    public static class SunRayStateDto {
        private final double startX, startY, endX, endY;
        private final EnergyType energyType;

        public SunRayStateDto(SunRay ray) {
            this.energyType = ray.getEnergyType();
            this.startX = ray.getStart().getX();
            this.startY = ray.getStart().getY();
            this.endX = ray.getEnd().getX();
            this.endY = ray.getEnd().getY();
        }

        public double getStartX() { return startX; }
        public double getStartY() { return startY; }
        public double getEndX() { return endX; }
        public double getEndY() { return endY; }
        public EnergyType getEnergyType() { return energyType; }
    }
}
