package de.lifecircles.service.dto;

import de.lifecircles.model.Blocker;
import de.lifecircles.model.SunRay;
import de.lifecircles.model.Vector2D;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thread-safe Data Transfer Object for simulation state.
 * Used to transfer state between calculation and visualization threads.
 */
public class SimulationStateDto {
    private final List<CellState> cells;
    private final List<BlockerState> blockers;
    private final List<SunRayState> sunRays;
    private final double width;
    private final double height;

    public SimulationStateDto(List<CellState> cells, List<Blocker> blockers, List<SunRay> sunRays, double width, double height) {
        this.cells = new ArrayList<>(cells);
        this.blockers = blockers.stream()
            .map(BlockerState::new)
            .collect(Collectors.toList());
        this.sunRays = sunRays.stream().map(SunRayState::new).collect(Collectors.toList());
        this.width = width;
        this.height = height;
    }

    public List<CellState> getCells() {
        return Collections.unmodifiableList(cells);
    }

    public List<BlockerState> getBlockers() {
        return Collections.unmodifiableList(blockers);
    }

    public List<SunRayState> getSunRays() {
        return Collections.unmodifiableList(sunRays);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public static class CellState {
        private final Vector2D position;
        private final double rotation;
        private final double size;
        private final double[] typeRGB;
        private final List<ActorState> actors;
        private final double energy;
        private final double age;

        public CellState(Vector2D position, double rotation, double size, 
                        double[] typeRGB, List<ActorState> actors, double energy, double age) {

            this.position = position;
            this.rotation = rotation;
            this.size = size;
            this.typeRGB = typeRGB.clone();
            this.actors = new ArrayList<>(actors);
            this.energy = energy;
            this.age = age;
        }

        public Vector2D getPosition() {
            return new Vector2D(position.getX(), position.getY());
        }

        public double getRotation() {
            return rotation;
        }

        public double getSize() {
            return size;
        }

        public double[] getTypeRGB() {
            return typeRGB.clone();
        }

        public List<ActorState> getActors() {
            return Collections.unmodifiableList(actors);
        }

        public double getAge() {
            return age;
        }

        public double getEnergy() {
            return energy;
        }
    }

    public static class ActorState {
        private final Vector2D position;
        private final double[] typeRGB;
        private final double forceStrength;

        public ActorState(Vector2D position, double[] typeRGB, double forceStrength) {
            this.position = position;
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

    public static class BlockerState {
        private final double x;
        private final double y;
        private final double width;
        private final double height;
        private final Color color;
        private final Blocker.BlockerType type;

        public double getHeight() {
            return height;
        }

        public BlockerState(Blocker blocker) {
            Vector2D pos = blocker.getPosition();
            this.x = pos.getX();
            this.y = pos.getY();
            this.width = blocker.getWidth();
            this.height = blocker.getHeight();
            this.color = blocker.getColor();
            this.type = blocker.getType();
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public Color getColor() { return color; }
        public Blocker.BlockerType getType() { return type; }
    }

    public static class SunRayState {
        private final double startX, startY, endX, endY;

        public SunRayState(SunRay ray) {
            this.startX = ray.getStart().getX();
            this.startY = ray.getStart().getY();
            this.endX = ray.getEnd().getX();
            this.endY = ray.getEnd().getY();
        }

        public double getStartX() { return startX; }
        public double getStartY() { return startY; }
        public double getEndX() { return endX; }
        public double getEndY() { return endY; }
    }
}
