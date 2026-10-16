"""Validate and archive the exact network simplification study, including paired timings."""
import argparse
import csv
import hashlib
import json
import platform
import shutil
import statistics as stats
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

parser = argparse.ArgumentParser()
parser.add_argument("folder", type=Path)
parser.add_argument("--models", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)

def read(name):
    with (args.folder / name).open() as stream:
        return list(csv.DictReader(stream))

counts, equivalence, timings = read("counts.csv"), read("equivalence.csv"), read("timing.csv")
assert len(counts) == 18 and {int(r["model"]) for r in counts} == set(range(18))
assert len(equivalence) == 144
assert {(int(r["model"]), int(r["birth_state"])) for r in equivalence} == {(m, s) for m in range(18) for s in range(8)}
assert all(int(r["steps"]) == 2200 and int(r["output_comparisons"]) == 2200 * 133 for r in equivalence)
assert len(timings) == 18 * 7 * 2
assert {(int(r["model"]), int(r["repeat"]), r["mode"]) for r in timings} == {(m, r, mode) for m in range(18) for r in range(7) for mode in ("original", "compact")}
assert all(int(r["steps"]) == 2000 and int(r["nanos"]) > 0 for r in timings)

per_model = []
for model in range(18):
    row = next(r for r in counts if int(r["model"]) == model)
    ratios = []
    for repeat in range(7):
        pair = {r["mode"]: r for r in timings if int(r["model"]) == model and int(r["repeat"]) == repeat}
        assert pair["original"]["checksum"] == pair["compact"]["checksum"]
        ratios.append(int(pair["original"]["nanos"]) / int(pair["compact"]["nanos"]))
    per_model.append({"model": model, "median_speedup": stats.median(ratios), "paired_speedups": ratios,
                      "root_neurons_removed": int(row["root_neurons_before"]) - int(row["root_neurons_after"]),
                      "stored_synapses_removed": int(row["stored_synapses_before"]) - int(row["stored_synapses_after"])})

summary = {"models": 18, "sequences": 144, "network_steps_compared": sum(int(r["steps"]) for r in equivalence),
           "bit_exact_output_comparisons": sum(int(r["output_comparisons"]) for r in equivalence),
           "median_model_speedup": stats.median(r["median_speedup"] for r in per_model),
           "min_model_speedup": min(r["median_speedup"] for r in per_model),
           "max_model_speedup": max(r["median_speedup"] for r in per_model),
           "timing_note": "Median of seven paired ratios per model, then median across 18 related genomes; isolated forward execution, no whole-world inference"}
for name, data, before, after in (
        ("root_neurons", counts, "root_neurons_before", "root_neurons_after"),
        ("root_synapses", counts, "root_synapses_before", "root_synapses_after"),
        ("stored_synapses", counts, "stored_synapses_before", "stored_synapses_after"),
        ("serialized_bytes", counts, "copy_bytes_before", "copy_bytes_after"),
        ("executed_synapses", equivalence, "executed_before", "executed_after")):
    old, new = sum(int(r[before]) for r in data), sum(int(r[after]) for r in data)
    assert 0 <= new <= old
    summary[name] = {"before": old, "after": new, "reduction": 1 - new / old}

files = []
for model in range(18):
    path = args.models / f"model-{model}.cells"
    files.append({"file": path.name, "bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
known = json.loads(Path("docs/function-comparison/stable/model-files.json").read_text())
assert {r["file"]: r["sha256"] for r in files} == {r["file"]: r["sha256"] for r in known}
for name in ("counts.csv", "equivalence.csv", "timing.csv", "settings.txt"):
    shutil.copy2(args.folder / name, args.output / name)
for name, value in (("summary.json", summary), ("models.json", per_model), ("source-models.json", files)):
    (args.output / name).write_text(json.dumps(value, indent=2) + "\n")
sources = ["src/main/java/de/lifecircles/model/neural/NeuralNetwork.java",
           "src/main/java/de/lifecircles/model/neural/NeuralNet.java",
           "src/test/java/de/lifecircles/model/neural/NetworkSimplificationStudyTest.java"]
(args.output / "provenance.json").write_text(json.dumps({"simplifier_commit": "5100edb", "platform": platform.platform(),
    "source_files": [{"path": p, "sha256": hashlib.sha256(Path(p).read_bytes()).hexdigest()} for p in sources]}, indent=2) + "\n")

fig, axes = plt.subplots(1, 2, figsize=(12, 4.8))
names = ["root_neurons", "stored_synapses", "executed_synapses"]
axes[0].bar(["Neuronen\nWurzelnetz", "Gespeicherte\nSynapsen", "Ausgeführte\nSynapsen"],
            [100 * summary[k]["reduction"] for k in names], color=["#4079b8", "#df852d", "#548b55"])
axes[0].set_ylabel("Entfernt / eingespart (%)")
axes[0].set_ylim(0, 50)
axes[0].set_title("Summen über alle 18 Genome")
for model in per_model:
    color = "#4079b8" if model["model"] % 2 == 0 else "#df852d"
    axes[1].scatter([model["model"]] * 7, model["paired_speedups"], color=color, alpha=.25, s=14)
    axes[1].scatter(model["model"], model["median_speedup"], color=color, s=34)
axes[1].axhline(1, color="black", linewidth=.8)
axes[1].set_xlabel("Modell-ID; blau: früh, orange: spät")
axes[1].set_ylabel("Beschleunigung (Originalzeit / kompakte Zeit)")
axes[1].set_title("Einzelpaare und Median pro Genom")
axes[1].set_xticks(range(18))
for ax in axes:
    ax.grid(axis="y", alpha=.2)
fig.suptitle("Konservative Vereinfachung: 42,1 Mio. Ausgaben bitgenau gleich")
fig.tight_layout()
fig.savefig(args.output / "simplification.png", dpi=150)
print(json.dumps(summary, indent=2))
