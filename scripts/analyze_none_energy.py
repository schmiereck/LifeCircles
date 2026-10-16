"""Summarize paired NONE continuations; no pooling of censored endpoints."""
import argparse
import csv
import json
import statistics as st
from collections import defaultdict
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

p = argparse.ArgumentParser()
p.add_argument("folder", type=Path)
p.add_argument("--output", type=Path, required=True)
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=True)
def read(name):
    with (a.folder / name).open() as stream:
        return list(csv.DictReader(stream))

results, trajectory = read("results.csv"), read("trajectory.csv")
settings = dict(line.split("=", 1) for line in (a.folder / "settings.txt").read_text().splitlines() if "=" in line)
expected = int(settings["seeds"])
groups = defaultdict(list)
for r in results:
    r["mean_brain_cost_per_cell_second"] = float(r["brain_cost_requested"]) / max(1,float(r["decisions"])) * 60
    groups[r["storage"], r["work"]].append(r)
assert len(groups) == 3 and all(len(g) == expected for g in groups.values()), "Study incomplete"
assert len({(r["seed"],r["storage"],r["work"]) for r in results}) == len(results)
starts = [r for r in trajectory if r["step"] == "0"]
assert len(starts) == len(results) and len({r["state_hash"] for r in starts}) == 1, "Different start states"
summary = []
fields = ["cells", "births", "deaths", "founder_lineages", "total_energy", "mean_energy", "mean_stored",
          "mean_executed", "mean_root_neurons", "mean_generation", "max_generation", "contact_fraction", "step_wall_ms", "mean_brain_cost_per_cell_second"]
common_steps = []
for step in sorted({int(r["step"]) for r in trajectory}):
    rows = [r for r in trajectory if int(r["step"]) == step]
    if len(rows) == len(results): common_steps.append(step)
common_step = max(common_steps)
common = [r for r in trajectory if int(r["step"]) == common_step]
(a.output / "common-horizon.json").write_text(json.dumps({"step":common_step,"seconds":common_step/60,"rows":common},indent=2)+"\n")
for price, rows in sorted(groups.items()):
    item = {"storage":float(price[0]), "work":float(price[1]), "trials":len(rows),
            "status_counts":{status:sum(r["status"] == status for r in rows) for status in sorted({r["status"] for r in rows})}}
    item["observed_stop_seconds"] = [float(r["seconds"]) for r in rows]
    # Endpoints only describe common-duration trials. Report censorship separately.
    full = [r for r in rows if r["status"] == "completed" and int(r["step"]) == int(settings["steps"])]
    item["full_duration_trials"] = len(full)
    for field in fields:
        if full:
            values = [float(r[field]) for r in full]
            item[field] = {"mean":st.mean(values), "min":min(values), "max":max(values)}
    summary.append(item)
(a.output / "summary.json").write_text(json.dumps(summary, indent=2)+"\n")

paired = []
baseline = {r["seed"]:r for r in groups["1.0","1.0"]}
for price, rows in sorted(groups.items()):
    if price == ("1.0","1.0"): continue
    for r in rows:
        base = baseline[r["seed"]]
        if r["step"] != base["step"] or r["status"] != "completed" or base["status"] != "completed": continue
        paired.append({"seed":int(r["seed"]), "storage":float(price[0]), "work":float(price[1]),
                       **{f+"_delta":float(r[f])-float(base[f]) for f in fields}})
(a.output / "paired.json").write_text(json.dumps(paired, indent=2)+"\n")

if common_step == 0:
    (a.output / "trajectories.png").unlink(missing_ok=True)
    print("No shared follow-up checkpoint: report stop times, not pooled trajectories.")
    print(json.dumps(summary,indent=2))
    raise SystemExit(0)

fig, axes = plt.subplots(2,3,figsize=(15,8))
plots = [("cells","Lebende Zellen"),("mean_energy","Mittlere Energie"),("mean_stored","Gespeicherte Synapsen je Zelle"),
         ("mean_executed","Ausgeführte Synapsen je Entscheidung am Messpunkt"),("births","Geburten insgesamt"),("founder_lineages","Erhaltene Gründerlinien")]
colors = ["#4079b8","#df852d","#339764"]
for ax, (field,title) in zip(axes.flat,plots):
    for color, price in zip(colors,sorted(groups)):
        by_step = defaultdict(list)
        for r in trajectory:
            if int(r["step"]) > common_step: continue
            if field == "mean_executed" and r["step"] == "0": continue  # transient counters after loading are not measured decisions
            if (r["storage"],r["work"]) == price: by_step[int(r["step"])].append(float(r[field]))
        points = sorted((step,values) for step,values in by_step.items() if len(values) == expected)
        x = [step/60 for step,v in points]
        ax.plot(x,[st.mean(v) for step,v in points],color=color,label=f"S={price[0]}, W={price[1]}")
        ax.fill_between(x,[min(v) for step,v in points],[max(v) for step,v in points],color=color,alpha=.15)
    ax.set_title(title,fontsize=10);ax.set_xlabel("Simulationssekunden");ax.grid(alpha=.2)
axes.flat[0].legend(fontsize=8)
fig.suptitle(f"NONE: {expected} Zufallsfolgen je Preis; Mittelwert und Min/Max, nur gemeinsame Messpunkte")
fig.tight_layout()
fig.savefig(a.output / "trajectories.png",dpi=140)
print(json.dumps(summary,indent=2))
