"""Collect the opt-in JVM measurements and render standalone scientific figures.

python tools/plot_mechanics.py --collect   # target/mechanics-* -> docs/mechanics
python tools/plot_mechanics.py             # redraw from the committed measurement tables
Requires matplotlib; no simulation or plotting package is required by the Java application.
"""
import argparse
import csv
import math
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Circle

ROOT = Path(__file__).resolve().parents[1]
plt.rcParams.update({"svg.fonttype": "none", "svg.hashsalt": "LifeCircles-mechanics", "font.size": 9,
                     "axes.spines.top": False, "axes.spines.right": False})
COLORS = {"legacy": "#b84a47", "soft-only": "#db9b26", "balanced": "#157f83"}
NAMES = {"legacy": "Bisherige Mechanik", "soft-only": "Kontakt geglättet", "balanced": "Ausgewogen"}


def read(path):
    with path.open(encoding="utf-8", newline="") as source:
        return list(csv.DictReader(source))


def collect(output):
    results = []
    for stage in ["legacy", "soft-grid", "refine", "hz60", "hz120", "hz240"]:
        directory = ROOT / "target" / ("mechanics-" + stage)
        rows = read(directory / "results.csv")
        # Early coarse sweeps predate the extra residual-stroke column. Derive
        # the same last release-window sample (t=7) from their recorded positions.
        if "stroke_residual_R" not in rows[0]:
            points = {}
            for row in read(directory / "trajectories.csv"):
                if row["scenario"] == "actuator" and float(row["time"]) in (3, 7):
                    points.setdefault((row["profile"], float(row["time"])), []).append((float(row["x"]), float(row["y"])))
            for row in rows:
                row["stroke_residual_R"] = "0"
                if row["scenario"] == "actuator":
                    before = math.dist(*points[(row["profile"], 3)])
                    after = math.dist(*points[(row["profile"], 7)])
                    row["stroke_residual_R"] = f"{abs(after - before) / 12:.9f}"
        results.extend({"stage": stage, **row} for row in rows)
    with (output / "results.csv").open("w", newline="", encoding="utf-8") as destination:
        writer = csv.DictWriter(destination, fieldnames=list(results[0]))
        writer.writeheader()
        writer.writerows(results)
    trajectories = read(ROOT / "target/mechanics-hz60/trajectories.csv")
    with (output / "trajectories.csv").open("w", newline="", encoding="utf-8") as destination:
        writer = csv.DictWriter(destination, fieldnames=list(trajectories[0]))
        writer.writeheader()
        writer.writerows(trajectories)


def save(fig, output, name):
    fig.savefig(output / (name + ".svg"), bbox_inches="tight", metadata={"Date": None})
    svg = output / (name + ".svg")
    svg.write_text("\n".join(line.rstrip() for line in svg.read_text(encoding="utf-8").splitlines()) + "\n", encoding="utf-8")
    fig.savefig(output / (name + ".png"), dpi=160, bbox_inches="tight")
    plt.close(fig)


def plots(output):
    results = read(output / "results.csv")
    frames = read(output / "trajectories.csv")
    scenarios = ["snowman", "snowman-drop", "chain", "strip", "actuator", "sizes"]
    labels = ["Schneemann", "+ fallende Zellen", "Kette", "Dreiecksstreifen", "Aktuator", "3 Radien"]
    fig, ax = plt.subplots(figsize=(10, 4))
    for offset, profile in enumerate(COLORS):
        values = [float(next(r for r in results if r["stage"] == "hz60" and r["profile"] == profile and r["scenario"] == scenario)["late_relative_speed"]) for scenario in scenarios]
        locations = [i + (offset - 1) * .24 for i in range(6)]
        ax.bar(locations, [max(1e-5, value) for value in values], width=.23, label=NAMES[profile], color=COLORS[profile])
        for location, value in zip(locations, values):
            if value < 1e-5: ax.text(location, 1.4e-5, "<10⁻⁵", rotation=90, ha="center", va="bottom", fontsize=8)
    ax.set_yscale("log"); ax.set_ylim(1e-5, 200)
    ax.set_xticks(range(6), labels); ax.set_ylabel("Restbewegung relativ zum Schwerpunkt [Welteinheiten/s]")
    ax.set_title("Ruhiger Kontakt ohne starre Fixierung – kontrollierte Testwelten")
    ax.legend(loc="upper right"); ax.grid(axis="y", alpha=.2)
    fig.text(.12, -.02, "Messfenster: 18–19 s beim Schneemann, sonst 6–7 s; kein Sonnenlicht, keine Evolution.")
    save(fig, output, "rest-motion")

    fig, axes = plt.subplots(2, 3, figsize=(8, 8), sharex=True, sharey=True)
    for row, profile in enumerate(["legacy", "balanced"]):
        for col, time in enumerate([0, 8, 20]):
            ax = axes[row, col]
            for point in frames:
                if point["profile"] != profile or point["scenario"] != "snowman-drop" or float(point["time"]) != time:
                    continue
                color = "#157f83" if int(point["cell"]) < 15 else "#db9b26"
                ax.add_patch(Circle((float(point["x"]), float(point["y"])), float(point["radius"]), fc=color, ec="white", lw=.4, alpha=.8))
            ax.axhline(750, color="#555555", lw=2)
            ax.set_xlim(20, 300); ax.set_ylim(770, 150); ax.set_aspect("equal")
            ax.set_title(f"{NAMES[profile]}, t={time} s")
            if col == 0: ax.set_ylabel("Welt-Y (nach unten)")
    fig.suptitle("TEST_A: erste 15 Körperzellen und 6 fallende Zellen (orange)")
    fig.tight_layout()
    save(fig, output, "snowman")

    def points(profile, scenario, time):
        rows = sorted((r for r in frames if r["profile"] == profile and r["scenario"] == scenario and float(r["time"]) == time), key=lambda r: int(r["cell"]))
        return [(float(r["x"]), float(r["y"]), float(r["radius"])) for r in rows]

    def bend(points):
        a, m, b = points[0], points[2], points[4]
        dx, dy = b[0] - a[0], b[1] - a[1]
        return (dx * (m[1] - a[1]) - dy * (m[0] - a[0])) / math.hypot(dx, dy) / 12

    fig, axes = plt.subplots(2, 2, figsize=(9, 7))
    for ax, scenario, title in zip(axes[0], ["chain", "strip"], ["Einreihige Kette: gelenkig", "Triangulierter Streifen: steifer"]):
        for time, color in [(3, "#999999"), (4, "#157f83")]:
            shape = points("balanced", scenario, time)
            cx = sum(p[0] for p in shape) / len(shape); cy = sum(p[1] for p in shape) / len(shape)
            for x, y, radius in shape:
                ax.add_patch(Circle((x - cx, y - cy), radius, fill=False, ec=color, lw=1.5))
        ax.set_xlim(-70, 70); ax.set_ylim(45, -45); ax.set_aspect("equal"); ax.set_title(title)
        ax.set_xlabel("X relativ zum Schwerpunkt"); ax.set_ylabel("Y relativ zum Schwerpunkt")
    times = sorted({float(r["time"]) for r in frames if r["profile"] == "balanced" and r["scenario"] == "chain"})
    for scenario, label in [("chain", "Kette"), ("strip", "Streifen")]:
        base = bend(points("balanced", scenario, 3))
        axes[1, 0].plot(times, [abs(bend(points("balanced", scenario, t)) - base) if t >= 3 else 0 for t in times], label=label)
    axes[1, 0].set_ylabel("Biegung gegenüber t=3 s [Zellradien]"); axes[1, 0].legend()
    pair_base = math.dist(points("balanced", "actuator", 3)[0][:2], points("balanced", "actuator", 3)[1][:2])
    axes[1, 1].plot(times, [(math.dist(points("balanced", "actuator", t)[0][:2], points("balanced", "actuator", t)[1][:2]) - pair_base) / 12 for t in times], color="#157f83")
    axes[1, 1].set_ylabel("Abstandsänderung des Aktuatorpaars [Zellradien]")
    for ax in axes[1]:
        ax.axvspan(3, 4, color="#db9b26", alpha=.2); ax.set_xlabel("Simulationszeit [s]"); ax.grid(alpha=.2)
    fig.suptitle("Gleiche Mechanik, unterschiedliche Geometrie; grau vor Last, türkis unter Last")
    fig.tight_layout()
    save(fig, output, "flexibility")

    fig, axes = plt.subplots(1, 2, figsize=(9, 3.5))
    for profile in COLORS:
        pair = [next(r for r in results if r["stage"] == f"hz{hz}" and r["profile"] == profile and r["scenario"] == "actuator") for hz in [60, 120, 240]]
        strip = [next(r for r in results if r["stage"] == f"hz{hz}" and r["profile"] == profile and r["scenario"] == "strip") for hz in [60, 120, 240]]
        axes[0].plot([60, 120, 240], [float(r["late_relative_speed"]) for r in pair], "o-", color=COLORS[profile], label=NAMES[profile])
        axes[1].plot([60, 120, 240], [float(r["peak_bend_R"]) for r in strip], "o-", color=COLORS[profile])
    axes[0].set_yscale("log"); axes[0].set_ylabel("Restbewegung Aktuator [Welteinheiten/s]"); axes[0].legend()
    axes[1].set_ylabel("Maximale Streifenbiegung [Zellradien]")
    for ax in axes: ax.set_xlabel("Schritte pro Simulationssekunde [Hz]"); ax.set_xticks([60, 120, 240]); ax.grid(alpha=.2)
    fig.suptitle("Zeitschrittprüfung: geglättete Mechanik bleibt bei kleinerem Schritt vergleichbar")
    fig.tight_layout()
    save(fig, output, "convergence")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--collect", action="store_true")
    parser.add_argument("--output", type=Path, default=ROOT / "docs/mechanics")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    if args.collect:
        collect(args.output)
    plots(args.output)
