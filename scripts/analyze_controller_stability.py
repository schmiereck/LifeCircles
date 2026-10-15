"""Plot the opt-in controller assay (standard library + matplotlib)."""
import argparse
import csv
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

parser = argparse.ArgumentParser()
parser.add_argument("folder", type=Path)
args = parser.parse_args()
with (args.folder / "summary.csv").open() as f:
    rows = list(csv.DictReader(f))
programs = list(dict.fromkeys(row["program"] for row in rows))
labels = ["Fest", "Größe", "Kraft", "Größe + Kraft", "Farbe", "Größe + Farbe", "Kraft + Farbe", "Alle"]
fig, axes = plt.subplots(2, 2, figsize=(13, 8), sharey=True)
for ax, program in zip(axes.flat, programs):
    for frozen, offset, color in [("true", -.19, "#6580ae"), ("false", .19, "#d9803e")]:
        data = [r for r in rows if r["program"] == program and r["frozen"] == frozen and float(r["dt"]) > .01]
        ax.bar([int(r["mask"]) + offset for r in data], [float(r["height_ratio"])*100 for r in data],
               width=.36, color=color, label="Ausgänge eingefroren" if frozen == "true" else "Laufende Steuerung")
    ax.set_title(program)
    ax.set_xticks(range(8), labels, rotation=35, ha="right")
    ax.set_ylim(0, 105)
    ax.set_ylabel("Höhe nach 30 s / Anfangshöhe (%)")
    ax.grid(axis="y", alpha=.2)
axes[0, 0].legend(fontsize=9)
fig.suptitle("Gleiche Packung, 60 Zellen, volle Schwerkraft: Welche Ausgänge destabilisieren?", fontsize=13)
fig.tight_layout()
fig.savefig(args.folder / "channels.png", dpi=160)
plt.close(fig)
with (args.folder / "uniform-attraction.csv").open() as f:
    sweep = list(csv.DictReader(f))
x = [-float(r["mean_signed_force"]) for r in sweep]
fig, axes = plt.subplots(1, 2, figsize=(10, 4))
axes[0].plot(x, [float(r["height_ratio"])*100 for r in sweep], "o-")
axes[0].set_ylabel("Höhe nach 30 s / Anfangshöhe (%)")
axes[1].plot(x, [float(r["original_contacts"])*100 for r in sweep], "o-", color="#6580ae")
axes[1].set_ylabel("Erhaltene ursprüngliche Kontakte (%)")
for ax in axes:
    ax.set_xlabel("Einheitliche Anziehungsstärke je Aktor (Betrag)")
    ax.grid(alpha=.2)
fig.suptitle("Diese Packung bleibt bei Stärke 45–90 stehen; 30 reicht hier nicht")
fig.tight_layout()
fig.savefig(args.folder / "attraction.png", dpi=160)
