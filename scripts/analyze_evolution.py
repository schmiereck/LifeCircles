"""Plot CSV results from HighPosition2EvolutionStudyTest. Requires matplotlib."""
import argparse
import csv
import json
import math
import statistics
from pathlib import Path
import numpy as np

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Circle, Rectangle

parser = argparse.ArgumentParser()
parser.add_argument("input", type=Path)
parser.add_argument("output", type=Path)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
labels = {"standard": "Standardsonne", "more_sun": "Doppelte Strahlrate",
          "clonal_more_sun": "Doppelte Strahlrate, klonale Teilung"}
colors = {"standard": "#277da1", "more_sun": "#f8961e", "clonal_more_sun": "#43aa8b"}
datasets = {}
settings = dict(line.split("=", 1) for line in (args.input / "settings.txt").read_text().splitlines() if "=" in line)

def offered_energy(generation, rate):
    begin, end = (generation - 1) * 50, generation * 50
    integral = 0
    while begin < end:
        phase = begin % 120
        length = min(end - begin, 120 - phase)
        integral += .4 * length + .6 * 120 / math.pi * (
            math.cos(phase * math.pi / 120) - math.cos((phase + length) * math.pi / 120))
        begin += length
    return integral * float(settings["width"]) / float(settings["ray_spacing"]) * rate * float(settings["energy_per_ray"])
for path in sorted(args.input.glob("*/generations.csv")):
    with path.open() as stream:
        datasets[path.parent.name] = [dict(row, **{key: float(value) for key, value in row.items()
                                                  if key != "case"}) for row in csv.DictReader(stream)]

fig, axes = plt.subplots(3, 2, figsize=(12, 10), constrained_layout=True)
panels = [("sustained_height", "Gehaltene Höhe [Welteinheiten]"),
          ("diameters", "Gehaltene Höhe / mittlerer Durchmesser"),
          ("cells", "Zellen vor dem Generationswechsel"), ("mean_speed", "Mittlere Geschwindigkeit [Einheiten/s]"),
          ("mean_energy", "Mittlere Zellenergie"), ("mean_genes", "Gespeicherte Verbindungen je Zelle")]
summary = {}
for name, rows in datasets.items():
    if not rows:
        continue
    for row in rows:
        row["diameters"] = row["sustained_height"] / (2 * row["mean_radius"])
    for axis, (key, title) in zip(axes.flat, panels):
        axis.plot([row["generation"] for row in rows], [row[key] for row in rows],
                  label=labels[name], color=colors[name], marker=".", linewidth=1.3)
        axis.set_title(title, fontsize=11)
        axis.set_xlabel("Trainingsgeneration")
        axis.grid(alpha=.2)
        axis.set_ylim(bottom=0)
    last = rows[-5:]
    best = max(rows, key=lambda row: row["sustained_height"])
    summary[name] = {"generations": len(rows), "simulation_seconds": rows[-1]["seconds"],
                     "median_last5_height": statistics.median(row["sustained_height"] for row in last),
                     "median_last5_diameters": statistics.median(row["diameters"] for row in last),
                     "median_last5_cells": statistics.median(row["cells"] for row in last),
                     "median_last5_speed": statistics.median(row["mean_speed"] for row in last),
                     "best_height": best["sustained_height"], "best_generation": best["generation"],
                     "births": sum(row["births"] for row in rows), "deaths": sum(row["deaths"] for row in rows)}
    offered = [offered_energy(row["generation"], 10 if name == "standard" else 20) for row in rows]
    if len(rows) > 1:
        summary[name]["offered_sun_population_correlation"] = float(np.corrcoef(offered, [row["cells"] for row in rows])[0, 1])
    if len(rows) >= 24:
        # Twelve generations cover every alignment of the 50-s evaluation and 120-s sun cycle.
        for period, block in [("first12", rows[:12]), ("last12", rows[-12:])]:
            for key in ["sustained_height", "diameters", "cells", "mean_speed", "mean_energy", "mean_genes"]:
                summary[name][f"median_{period}_{key}"] = statistics.median(row[key] for row in block)
axes[0, 0].legend(fontsize=8)
fig.suptitle("HIGH_POSITION2: unabhängige Läufe ab derselben Startpopulation", fontsize=13)
fig.savefig(args.output / "progress.png", dpi=160)
plt.close(fig)
(args.output / "summary.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")

snapshots = []
for name in datasets:
    path = args.input / name / "best.cells.csv"
    if not path.exists():
        path = args.input / name / f"generation-{len(datasets[name])}.cells.csv"
    if path.exists():
        with path.open() as stream:
            snapshots.append((name, path, [{key: float(value) for key, value in row.items() if key != "base"}
                                           for row in csv.DictReader(stream)]))
if snapshots:
    fig, axes = plt.subplots(1, len(snapshots), figsize=(max(10, 5 * len(snapshots)), 5), squeeze=False,
                             constrained_layout=True)
    selected = []
    for name, path, cells in snapshots:
        best = max(cells, key=lambda cell: cell["supported_height"])
        section = int(best["x"] // 400)
        selected.append([cell for cell in cells if section * 400 <= cell["x"] < (section + 1) * 400])
    y_min = min(750, min(cell["y"] - cell["radius"] - 20 for cells in selected for cell in cells))
    for axis, (name, path, cells) in zip(axes.flat, snapshots):
        # Same physical window for all cases; choose the section with greatest supported height.
        best = max(cells, key=lambda cell: cell["supported_height"])
        section = int(best["x"] // 400)
        for cell in cells:
            if section * 400 <= cell["x"] < (section + 1) * 400:
                axis.add_patch(Circle((cell["x"] - section * 400, cell["y"]), cell["radius"],
                                      facecolor=plt.cm.viridis(cell["energy"]), edgecolor="black", linewidth=.2))
        axis.add_patch(Rectangle((0, 1150), 400, 20, color="#555555"))
        axis.add_patch(Rectangle((0, 1110), 25, 40, color="#999999"))
        axis.set(xlim=(0, 400), ylim=(1170, y_min), aspect="equal",
                 xlabel="Position in Box [Welteinheiten]", ylabel="Y [Welteinheiten]")
        axis.set_title(f"{labels[name]}\nBox {section+1}, gespeicherter Stand", fontsize=10)
    fig.suptitle("Ausschnitt der jeweils höchsten bodenverbundenen Kolonie; Farbe = Energie", fontsize=11)
    fig.colorbar(plt.cm.ScalarMappable(norm=plt.Normalize(0, 1), cmap="viridis"),
                 ax=list(axes.flat), shrink=.7, label="Zellenergie")
    fig.savefig(args.output / "colonies.png", dpi=160)
    plt.close(fig)

followups = list(args.input.glob("*/followup-no_division.csv"))
if followups:
    fig, axes = plt.subplots(1, 2, figsize=(11, 4), constrained_layout=True)
    followup_summary = {}
    for folder in sorted({path.parent for path in followups}):
        for mode, linestyle in [("no_division", "-"), ("frozen_mechanics", "--")]:
            path = folder / f"followup-{mode}.csv"
            if not path.exists():
                continue
            with path.open() as stream:
                rows = [{key: float(value) for key, value in row.items()} for row in csv.DictReader(stream)]
            section = int((folder / f"followup-{mode}-settings.txt").read_text().splitlines()[0].split("=")[1])
            with (folder / f"followup-{mode}-initial-positions.csv").open() as stream:
                initial = {int(row["id"]): row for row in csv.DictReader(stream) if int(float(row["x"]) // 400) == section}
            with (folder / f"followup-{mode}-positions.csv").open() as stream:
                final = {int(row["id"]): row for row in csv.DictReader(stream)}
            surviving = sorted(initial.keys() & final.keys())
            reference = np.array([[float(initial[key]["x"]), float(initial[key]["y"])] for key in surviving])
            actual = np.array([[float(final[key]["x"]), float(final[key]["y"])] for key in surviving])
            reference -= reference.mean(axis=0)
            actual -= actual.mean(axis=0)
            u, _, vt = np.linalg.svd(reference.T @ actual)
            correction = np.diag([1, np.linalg.det(u @ vt)])
            rotation = u @ correction @ vt
            radius = statistics.mean(float(initial[key]["radius"]) for key in surviving)
            shape_error = np.sqrt(np.mean(np.sum((reference @ rotation - actual) ** 2, axis=1))) / radius
            followup_summary[f"{folder.name}/{mode}"] = {
                "tracked_section": section + 1,
                "height_retained": rows[-1]["tracked_supported_height"] / rows[0]["tracked_supported_height"],
                "cells_retained_world": rows[-1]["cells"] / rows[0]["cells"],
                "cells_retained_initial_section": len(surviving) / len(initial),
                "shape_rms_in_initial_radii": float(shape_error),
                "final_mean_speed": rows[-1]["mean_speed"]}
            label = labels[folder.name] + (", lebende Steuerung" if mode == "no_division" else ", mechanische Kontrolle")
            for axis, key in zip(axes, ["tracked_supported_height", "cells"]):
                axis.plot([row["seconds"] for row in rows],
                          [row[key] / rows[0][key] for row in rows], color=colors[folder.name], linestyle=linestyle,
                          label=label)
    for axis, title in zip(axes, ["Verbleibende gestützte Höhe", "Verbleibende Zellen"]):
        axis.set(title=title, xlabel="Sekunden ohne weitere Teilung", ylabel="Anteil des Anfangswerts")
        axis.grid(alpha=.2)
    axes[0].legend(fontsize=7)
    fig.suptitle("Ohne weitere Teilung: laufende Steuerung und mechanische Kontrolle\n"
                 "Kontrolle: Energie anfangs 1, feste Ausgänge/Größen, keine Sonne und kein Energietransfer", fontsize=10)
    fig.savefig(args.output / "followups.png", dpi=160)
    plt.close(fig)
    (args.output / "followup-summary.json").write_text(json.dumps(followup_summary, indent=2), encoding="utf-8")
print(json.dumps(summary, indent=2, ensure_ascii=False))
