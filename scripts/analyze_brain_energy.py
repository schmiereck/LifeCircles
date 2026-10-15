"""Summarize paired-seed brain-energy experiments. Requires matplotlib only for figures."""
import argparse
import csv
import json
import statistics as st
from collections import defaultdict
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

parser = argparse.ArgumentParser()
parser.add_argument("folders", nargs="+", type=Path)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
rows = []
budgets = {}
seen = {}
for folder in args.folders:
    settings = dict(line.split("=",1) for line in (folder / "settings.txt").read_text().splitlines() if "=" in line)
    with (folder / "results.csv").open() as stream:
        for row in csv.DictReader(stream):
            kind=(row["phase"],row["task"],row["reward_scale"])
            budget=(row["generations"],settings.get("population","32"),settings.get("train_length","24"),settings.get("train_episodes","2"))
            if kind in budgets and budgets[kind]!=budget:
                raise ValueError("Analyze different generation/training budgets separately")
            budgets[kind]=budget
            identity=kind+(row["storage_factor"],row["work_factor"],row["seed"])
            values=tuple(row[field] for field in ("loss","accuracy","stored","executed","solved"))
            if identity in seen:
                if seen[identity]!=values: raise ValueError("Conflicting repeated trial")
                continue
            seen[identity]=values;rows.append(row)
groups = defaultdict(list)
for row in rows:
    groups[(row["phase"], row["task"], float(row["storage_factor"]), float(row["work_factor"]), float(row["reward_scale"]))].append(row)

def median(values):
    return st.median(values) if values else None

summary = []
for (phase, task, storage, work, reward), data in sorted(groups.items()):
    solved = [r for r in data if r["solved"] == "true"]
    summary.append(dict(phase=phase, task=task, storage=storage, work=work, reward=reward, n=len(data),
        successes=len(solved), median_loss=median([float(r["loss"]) for r in data]),
        median_accuracy=median([float(r["accuracy"]) for r in data]),
        median_stored=median([int(r["stored"]) for r in data]),
        median_executed=median([float(r["executed"]) for r in data]),
        median_neurons=median([int(r["root_neurons"]) for r in data]),
        solved_median_stored=median([int(r["stored"]) for r in solved]),
        solved_median_executed=median([float(r["executed"]) for r in solved]),
        median_first_solved=median([int(r["first_solved"]) for r in data if int(r["first_solved"])>=0]),
        lost_after_discovery=sum(int(r["first_solved"])>=0 and r["solved"]!="true" for r in data),
        rejected=sum(int(r["rejected_evaluations"]) for r in data)))
(args.output / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
with (args.output / "summary.csv").open("w", newline="") as stream:
    writer=csv.DictWriter(stream, fieldnames=list(summary[0]));writer.writeheader();writer.writerows(summary)

tasks = ["CONSTANT", "AND", "OR", "XOR", "DELAY", "COUNTER"]
for reward in sorted({s["reward"] for s in summary}):
    phases = sorted({s["phase"] for s in summary if s["reward"]==reward})
    fig, axes = plt.subplots(len(phases), 1, figsize=(14, 4*len(phases)), squeeze=False)
    for ax, phase in zip(axes.flat, phases):
        filtered = [s for s in summary if s["phase"]==phase and s["reward"]==reward]
        prices = sorted({(s["storage"], s["work"]) for s in filtered})
        lookup = {(s["task"],s["storage"],s["work"]):s for s in filtered}
        matrix=[[lookup.get((task,*price),{}).get("successes",float("nan")) for price in prices] for task in tasks]
        picture=ax.imshow(matrix, vmin=0, vmax=10, cmap="YlGnBu", aspect="auto")
        for y,task in enumerate(tasks):
            for x,price in enumerate(prices):
                value=lookup.get((task,*price))
                if value:
                    ax.text(x,y,f'{value["successes"]}/{value["n"]}',ha="center",va="center",color="white" if value["successes"]>6 else "black",fontsize=9)
                else:
                    ax.text(x,y,"—",ha="center",va="center",color="#666666",fontsize=9)
        ax.set_yticks(range(len(tasks)),tasks)
        ax.set_xticks(range(len(prices)),[f"{s:g} / {w:g}" for s,w in prices],rotation=35,ha="right")
        ax.set_xlabel("Speicherfaktor / Ausführungsfaktor (—: nicht durchgeführt)")
        title={"acquire":"Erlernen aus Zufallsnetzen","compress":"Fähigkeit nach Verdichtung erhalten","compete":"Wettbewerb mit statischen Netzen"}[phase]
        ax.set_title(title+f"; Belohnungsskala {reward:g} Energie/s")
    fig.tight_layout();fig.savefig(args.output / f"success-{reward:g}.png",dpi=160);plt.close(fig)

# Paired comparisons use only equal task, seed, reward and generation budget.
index={(r["phase"],r["task"],r["storage_factor"],r["work_factor"],r["reward_scale"],r["seed"]):r for r in rows}
pairs=[]
for row in rows:
    key=(row["phase"],row["task"],"0","0",row["reward_scale"],row["seed"])
    base=index.get(key)
    if not base or base["generations"]!=row["generations"]:continue
    pairs.append(dict(phase=row["phase"],task=row["task"],seed=int(row["seed"]),storage=float(row["storage_factor"]),
        work=float(row["work_factor"]),reward=float(row["reward_scale"]),baseline_solved=base["solved"]=="true",solved=row["solved"]=="true",
        delta_loss=float(row["loss"])-float(base["loss"]),delta_stored=int(row["stored"])-int(base["stored"]),
        delta_executed=float(row["executed"])-float(base["executed"])))
(args.output / "paired.json").write_text(json.dumps(pairs,indent=2),encoding="utf-8")
print(f"{len(rows)} runs, {len(summary)} task/price groups; summaries in {args.output}")
