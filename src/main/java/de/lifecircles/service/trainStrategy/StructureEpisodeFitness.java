package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import java.util.*;

/** Whole-colony score; rigid motion is not a deformation. No forces are imposed here. */
final class StructureEpisodeFitness {
    static final double RADIUS = 20;
    static final double TARGET_HEIGHT = 40 + 10 * Math.sqrt(3) * RADIUS; // upper three of twelve rows
    record Link(Cell a, Cell b) {}
    record Measurement(double score, double height, double support, double contacts, double form) {}
    record Summary(double score, double height, double support, double contacts, double form) {
        boolean mastered() { return score >= .8 && height >= .9 && support >= .98 && contacts >= .95 && form <= .5; }
    }

    static final class Body {
        final int slot;
        final Cell genome;
        final List<Cell> cells = new ArrayList<>();
        final Map<Cell, Vector2D> reference = new IdentityHashMap<>();
        Map<Cell, Vector2D> previous;
        final Set<Link> originalLinks;
        Set<Link> previousLinks;
        boolean invalid;
        int samples, lateSamples;
        double scoreSum, lateScore, lateHeight, lateSupport, lateContacts, lateForm;

        Body(int slot, Cell genome, List<Cell> cells) {
            this.slot = slot; this.genome = genome; this.cells.addAll(cells);
            this.cells.forEach(c -> reference.put(c, copy(c.getPosition())));
            previous = new IdentityHashMap<>(reference);
            originalLinks = links(cells); previousLinks = originalLinks;
        }
        void add(Measurement m, double seconds) {
            samples++; scoreSum += m.score;
            if (seconds >= 25 - 1e-9) {
                lateSamples++; lateScore += m.score; lateHeight += m.height; lateSupport += m.support;
                lateContacts += m.contacts; lateForm += m.form;
            }
        }
        Summary summary(boolean building) {
            double n = Math.max(1, lateSamples);
            double mean = scoreSum / Math.max(1, samples);
            return new Summary(invalid ? 0 : building ? .75 * mean + .25 * lateScore / n : mean,
                    lateHeight / n, lateSupport / n, lateContacts / n, lateForm / n);
        }
    }

    private final GroundedColonyFitness grounded;
    StructureEpisodeFitness(int sections) { grounded = new GroundedColonyFitness(sections); }

    void sample(Environment world, List<Body> bodies, double seconds, boolean building) {
        Map<Cell, GroundedColonyFitness.Rating> ratings = grounded.current(world);
        Set<Cell> alive = Collections.newSetFromMap(new IdentityHashMap<>()); alive.addAll(world.getCellList());
        double floor = world.getHeight() - Environment.GroundBlockerHeight;
        for (Body body : bodies) {
            boolean valid = !body.invalid && body.cells.stream().allMatch(alive::contains);
            double height = body.cells.stream().filter(ratings::containsKey)
                    .mapToDouble(c -> ratings.get(c).score() * floor / TARGET_HEIGHT).max().orElse(0);
            height = Math.min(1, height);
            double support = body.cells.stream().filter(ratings::containsKey).count() / (double)body.cells.size();
            Set<Link> currentLinks = links(body.cells);
            Set<Link> expectedLinks = building ? body.previousLinks : body.originalLinks;
            double contacts = expectedLinks.isEmpty() ? 1 : expectedLinks.stream().filter(currentLinks::contains).count() / (double)expectedLinks.size();
            double form = shapeError(building ? body.previous : body.reference, body.cells);
            double score = height * support * (building ? 1 : contacts / (1 + form * form));
            // During construction only the last five seconds reward local form/contact retention.
            if (building && seconds >= 25 - 1e-9) score *= contacts / (1 + form * form);
            if (!valid || !Double.isFinite(score)) {
                body.invalid = true; score = height = support = contacts = 0; form = 100;
            }
            body.add(new Measurement(score, height, support, contacts, form), seconds);
            body.previous = new IdentityHashMap<>(); body.cells.forEach(c -> body.previous.put(c,copy(c.getPosition())));
            body.previousLinks = currentLinks;
        }
    }

    static Set<Link> links(List<Cell> cells) {
        Set<Link> result = new HashSet<>();
        for (int i=0; i<cells.size(); i++) for (int j=i+1; j<cells.size(); j++) {
            Cell a=cells.get(i), b=cells.get(j);
            double reach=a.getRadiusSize()+b.getRadiusSize()+.1*Math.min(a.getRadiusSize(),b.getRadiusSize());
            if (a.getPosition().distance(b.getPosition()) <= reach) result.add(new Link(a,b));
        }
        return result;
    }

    private static Vector2D copy(Vector2D p) { return new Vector2D(p.getX(),p.getY()); }

    static double shapeError(Map<Cell, Vector2D> reference, List<Cell> cells) {
        List<Cell> common=cells.stream().filter(reference::containsKey).toList();
        if (common.isEmpty()) return 100;
        double rx=0,ry=0,px=0,py=0;
        for (Cell cell:common) { var r=reference.get(cell); var p=cell.getPosition(); rx+=r.getX();ry+=r.getY();px+=p.getX();py+=p.getY(); }
        int n=common.size(); rx/=n;ry/=n;px/=n;py/=n;
        double dot=0,cross=0,sum=0;
        for (Cell cell:common) {
            double x=reference.get(cell).getX()-rx,y=reference.get(cell).getY()-ry;
            double u=cell.getPosition().getX()-px,v=cell.getPosition().getY()-py;
            dot+=x*u+y*v;cross+=x*v-y*u;sum+=x*x+y*y+u*u+v*v;
        }
        return Math.sqrt(Math.max(0,(sum-2*Math.hypot(dot,cross))/n))/RADIUS;
    }
}
