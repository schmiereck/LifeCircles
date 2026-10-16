"""Analyze complete long NONE trials, including cell-time-weighted late windows."""
import argparse
import csv
import gzip
import json
import statistics as st
from collections import defaultdict
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

p=argparse.ArgumentParser()
p.add_argument("folder",type=Path)
p.add_argument("--output",type=Path,required=True)
p.add_argument("--short",type=Path)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
def read(root,name):
    path=root/name
    if path.exists():
        with path.open(encoding="utf-8-sig") as f:return list(csv.DictReader(f))
    with gzip.open(str(path)+".gz","rt",encoding="utf-8-sig") as f:return list(csv.DictReader(f))
rows=read(a.folder,"trajectory.csv");results=read(a.folder,"results.csv")
trials=defaultdict(list)
for r in rows:trials[(r["seed"],r["storage"],r["work"])].append(r)
assert len(trials)==9 and len(results)==9
assert all(r["status"]=="completed" and int(r["step"])==600000 for r in results)
assert len({r["state_hash"] for r in rows if r["step"]=="0"})==1
for trial in trials.values():
    trial.sort(key=lambda r:int(r["step"]))
    assert [int(r["step"]) for r in trial]==list(range(0,600001,600))
replay_checks=0
if a.short:
    early={(r["seed"],r["storage"],r["work"],r["step"]):r for r in read(a.short,"trajectory.csv")}
    for r in rows:
        if int(r["step"])<=36000:
            assert r["state_hash"]==early[(r["seed"],r["storage"],r["work"],r["step"])]["state_hash"],"Early trajectory changed"
            replay_checks+=1

def window(trial,start,end):
    part=[r for r in trial if start<=float(r["seconds"])<=end]
    assert float(part[0]["seconds"])==start and float(part[-1]["seconds"])==end
    # Trapezoidal quadrature of equally sampled population observables. Fees and births use exact cumulative differences.
    integral=defaultdict(float)
    execution_celltime=0
    for left,right in zip(part,part[1:]):
        dt=float(right["seconds"])-float(left["seconds"])
        for r in (left,right):
            n=float(r["cells"])
            integral["cells"]+=dt/2*n
            integral["total_root_neurons"]+=dt/2*n*float(r["mean_root_neurons"])
            for f in ("mean_stored","mean_executed","mean_root_neurons","mean_energy","mean_generation"):
                if f=="mean_executed" and float(left["seconds"])==0:continue
                integral[f]+=dt/2*n*float(r[f])
            if float(left["seconds"])>0:execution_celltime+=dt/2*n
    celltime=integral["cells"]
    first,last=part[0],part[-1]
    decisions=float(last["decisions"])-float(first["decisions"])
    return {"seed":int(first["seed"]),"storage":float(first["storage"]),"work":float(first["work"]),
            "start":start,"end":end,"cells":celltime/(end-start),
            "total_root_neurons":integral["total_root_neurons"]/(end-start),
            **{f:integral[f]/celltime for f in ("mean_stored","mean_root_neurons","mean_energy","mean_generation")},
            "mean_executed":integral["mean_executed"]/execution_celltime,
            "births_per_second":(float(last["births"])-float(first["births"] ))/(end-start),
            "deaths_per_second":(float(last["deaths"])-float(first["deaths"] ))/(end-start),
            "brain_cost_per_cell_second":(float(last["brain_cost_requested"])-float(first["brain_cost_requested"]))/decisions*60}
windows=[window(t,start,start+1000) for t in trials.values() for start in range(0,10000,1000)]
late=[r for r in windows if r["start"]==9000]
fields=["cells","mean_energy","mean_stored","mean_executed","mean_root_neurons","total_root_neurons","births_per_second","deaths_per_second","brain_cost_per_cell_second","mean_generation"]
def aggregate(data):
    groups=defaultdict(list)
    for r in data:groups[(r["storage"],r["work"])].append(r)
    return [{"storage":key[0],"work":key[1],"trials":len(group),
             **{f:{"mean":st.mean(r[f] for r in group),"min":min(r[f] for r in group),"max":max(r[f] for r in group)} for f in fields}}
            for key,group in sorted(groups.items())]
summary=aggregate(late)
(a.output/"windows.json").write_text(json.dumps(windows,indent=2)+"\n")
(a.output/"late-summary.json").write_text(json.dumps(summary,indent=2)+"\n")
with (a.output/"windows.csv").open("w",newline="") as f:
    writer=csv.DictWriter(f,fieldnames=list(windows[0]));writer.writeheader();writer.writerows(windows)
baseline={r["seed"]:r for r in late if r["storage"]==1 and r["work"]==1}
paired=[{"seed":r["seed"],"storage":r["storage"],"work":r["work"],
         **{f+"_delta":r[f]-baseline[r["seed"]][f] for f in fields}} for r in late if r["work"]!=1]
(a.output/"late-paired.json").write_text(json.dumps(paired,indent=2)+"\n")
audit={"trials":9,"steps":5400000,"trajectory_rows":len(rows),"early_replay_checks":replay_checks,
       "checkpoint_populations":len(list(a.folder.glob("*/step-*.cells"))),
       "lineage_files":len(list(a.folder.glob("*/step-*-lineages.csv")))}
assert audit["checkpoint_populations"]==90 and audit["lineage_files"]==90
for path in a.folder.glob("*/step-*.cells"):
    with path.with_name(path.stem+"-lineages.csv").open() as f:
        ancestry=list(csv.DictReader(f))
    step=path.stem.removeprefix("step-")
    seed,storage,work=path.parent.name.removeprefix("seed-").replace("-s",",").replace("-w",",").split(",")
    r=next(r for r in trials[(seed,storage,work)] if r["step"]==step)
    assert len(ancestry)==int(r["cells"])
    assert len({r["founder_lineage"] for r in ancestry})==int(r["founder_lineages"])
(a.output/"audit.json").write_text(json.dumps(audit,indent=2)+"\n")

fig,axes=plt.subplots(2,3,figsize=(15,8))
plots=[("cells","Lebende Zellen"),("mean_energy","Energie je Zelle"),("mean_stored","Gespeicherte Synapsen je Zelle"),
       ("mean_executed","Ausgeführte Synapsen je Entscheidung"),("births_per_second","Geburten je Simulationssekunde"),("total_root_neurons","Gesamte äußere Neuronenzahl")]
prices=[(1,1),(1,.1),(100,.1)]
for ax,(field,title) in zip(axes.flat,plots):
    for price,color in zip(prices,["#df852d","#4079b8","#339764"]):
        data=[r for r in windows if (r["storage"],r["work"])==price]
        bins=defaultdict(list)
        for r in data:bins[r["start"]+500].append(r[field])
        x=sorted(bins)
        ax.plot(x,[st.mean(bins[t]) for t in x],color=color,label=f"S={price[0]}, W={price[1]}",marker=".")
        ax.fill_between(x,[min(bins[t]) for t in x],[max(bins[t]) for t in x],color=color,alpha=.15)
    ax.set_title(title);ax.set_xlabel("Simulationssekunden (Fenstermitte)");ax.grid(alpha=.2)
axes.flat[0].legend(fontsize=8)
fig.suptitle("NONE Langzeit: 1000-Sekunden-Fenster, Mittelwert und Min/Max über drei Seeds")
fig.tight_layout();fig.savefig(a.output/"window-trajectories.png",dpi=140)
print(json.dumps({"audit":audit,"late_summary":summary},indent=2))
