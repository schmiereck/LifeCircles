package de.lifecircles.service;

import de.lifecircles.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Study snapshot (ZELLVERBINDUNGEN.md): cells in body colour, remains dark, terrain grey, bonds as
 * yellow lines between the bonded actors and ground anchors as orange diamonds (BODENHALT.md). Periodic geometry: everything is placed by its shortest offset
 * from the view centre, so a view may cross the world border.
 */
final class BondSnapshotImage {
    static final Color BACKGROUND = new Color(20, 20, 30), TERRAIN = new Color(105, 105, 105),
            BOND = new Color(255, 220, 0), ANCHOR = new Color(255, 120, 0), HIGHLIGHT = Color.WHITE, TEXT = new Color(230, 230, 230);

    private BondSnapshotImage() {}

    /** Renders a view of viewWidth x viewHeight world px around centre; highlighted cells get a white outline. */
    static BufferedImage render(Environment world, Vector2D centre, double viewWidth, double viewHeight, double scale,
                                Collection<Cell> highlight, String label) {
        int w = (int) Math.ceil(viewWidth * scale), h = (int) Math.ceil(viewHeight * scale);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(BACKGROUND); g.fillRect(0, 0, w, h);
            View view = new View(world, centre, viewWidth, viewHeight, scale);
            g.setColor(TERRAIN);
            for (Blocker b : world.getBlockerList()) {
                double[] p = view.screen(b.getPosition());
                g.fillRect((int) Math.round(p[0] - b.getWidth() * scale / 2), (int) Math.round(p[1] - b.getHeight() * scale / 2),
                        (int) Math.ceil(b.getWidth() * scale), (int) Math.ceil(b.getHeight() * scale));
            }
            Set<Cell> marked = Collections.newSetFromMap(new IdentityHashMap<>());
            marked.addAll(highlight);
            for (Cell c : world.getCellList()) {
                double[] p = view.screen(c.getPosition());
                double r = c.getRadiusSize() * scale;
                if (!view.visible(p, r)) continue;
                CellType t = c.getType();
                float dim = c.isRemains() ? .35f : 1f;
                g.setColor(new Color((float) t.getRed() * dim, (float) t.getGreen() * dim, (float) t.getBlue() * dim));
                Shape disc = new Ellipse2D.Double(p[0] - r, p[1] - r, 2 * r, 2 * r);
                g.fill(disc);
                g.setStroke(new BasicStroke(marked.contains(c) ? 2f : 1f));
                g.setColor(marked.contains(c) ? HIGHLIGHT : c.isRemains() ? Color.DARK_GRAY : Color.GRAY);
                g.draw(disc);
            }
            Set<Cell> present = Collections.newSetFromMap(new IdentityHashMap<>());
            present.addAll(world.getCellList());
            g.setColor(BOND);
            g.setStroke(new BasicStroke((float) Math.max(1.5, 2.5 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Cell c : world.getCellList()) for (SensorActor a : c.getSensorActors()) {
                SensorActor partner = a.getBondPartner();
                // Partners not in the world (e.g. remains left out of a saved population) are not drawn.
                if (partner == null || !present.contains(partner.getParentCell())) continue;
                Vector2D from = a.calcPosition();
                double[] p = view.screen(from);
                Vector2D step = from.getShortestDirectionWithWrapAround(partner.calcPosition(), world.getWidth(), world.getHeight());
                double qx = p[0] + step.getX() * scale, qy = p[1] + step.getY() * scale;
                if (view.visible(p, 0) || view.visible(new double[]{qx, qy}, 0)) g.draw(new Line2D.Double(p[0], p[1], qx, qy));
            }
            // Ground anchors (BODENHALT.md): a line to the anchor point and a small diamond on it.
            for (Cell c : world.getCellList()) for (SensorActor a : c.getSensorActors()) {
                if (a.getAnchor() == null) continue;
                Vector2D from = a.calcPosition();
                double[] p = view.screen(from);
                Vector2D step = from.getShortestDirectionWithWrapAround(a.getAnchor(), world.getWidth(), world.getHeight());
                double qx = p[0] + step.getX() * scale, qy = p[1] + step.getY() * scale, k = Math.max(2, 3 * scale);
                if (!view.visible(new double[]{qx, qy}, k)) continue;
                g.setColor(BOND);
                g.draw(new Line2D.Double(p[0], p[1], qx, qy));
                g.setColor(ANCHOR);
                g.fill(new java.awt.geom.Path2D.Double() {{ moveTo(qx, qy - k); lineTo(qx + k, qy); lineTo(qx, qy + k); lineTo(qx - k, qy); closePath(); }});
            }
            if (label != null) {
                g.setColor(TEXT); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                g.drawString(label, 6, 16);
            }
        } finally { g.dispose(); }
        return image;
    }

    /** Whole world, scaled down. */
    static void writeOverview(Environment world, Path file, double scale, String label) throws IOException {
        ImageIO.write(render(world, new Vector2D(world.getWidth() / 2, world.getHeight() / 2), world.getWidth(), world.getHeight(),
                scale, List.of(), label), "png", file.toFile());
    }

    /** Crop of viewSize x viewSize world px around a body's centre, body outlined; kept inside the world vertically. */
    static void writeBody(Environment world, List<Cell> body, Path file, double viewSize, double scale, String label) throws IOException {
        Vector2D c = centre(world, body);
        double half = Math.min(viewSize, world.getHeight()) / 2;
        c = new Vector2D(c.getX(), Math.max(half, Math.min(world.getHeight() - half, c.getY())));
        ImageIO.write(render(world, c, viewSize, viewSize, scale, body, label), "png", file.toFile());
    }

    /** Mean position of the cells, wrap-aware. */
    static Vector2D centre(Environment world, List<Cell> cells) {
        Vector2D anchor = cells.get(0).getPosition();
        double x = 0, y = 0;
        for (Cell c : cells) {
            Vector2D d = anchor.getShortestDirectionWithWrapAround(c.getPosition(), world.getWidth(), world.getHeight());
            x += d.getX(); y += d.getY();
        }
        return new Vector2D(anchor.getX() + x / cells.size(), anchor.getY() + y / cells.size());
    }

    private record View(Environment world, Vector2D centre, double width, double height, double scale) {
        double[] screen(Vector2D p) {
            Vector2D d = centre.getShortestDirectionWithWrapAround(p, world.getWidth(), world.getHeight());
            return new double[]{(d.getX() + width / 2) * scale, (d.getY() + height / 2) * scale};
        }
        boolean visible(double[] p, double r) {
            return p[0] + r >= 0 && p[1] + r >= 0 && p[0] - r <= width * scale && p[1] - r <= height * scale;
        }
    }
}
