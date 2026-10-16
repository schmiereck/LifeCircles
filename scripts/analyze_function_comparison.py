"""Paired live/frozen clonal assays; keep early/late, fees and source histories distinct."""
import argparse,csv,gzip,json,statistics as st
from collections import defaultdict
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

p=argparse.ArgumentParser();p.add_argument("folder",type=Path);p.add_argument("--output",type=Path,required=True)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
def read(name):
    path=a.folder/name
    if path.exists():
        with path.open() as f:return list(csv.DictReader(f))
    with gzip.open(str(path)+".gz","rt") as f:return list(csv.DictReader(f))
rows=read("results.csv");profiles=read("profiles.csv")
assert len(rows)==144 and len({(r["model"],r["fee"],r["mode"],r["sun_seed"]) for r in rows})==144
assert {int(r["model"]) for r in rows}==set(range(18))
assert all(r["status"] in ("completed","extinct","cap_stop") for r in rows)
assert all(int(r["steps"])==7200 for r in rows if r["status"]=="completed")
models=defaultdict(list)
for r in rows:models[(int(r["model"]),r["fee"])].append(r)
profile_values=defaultdict(lambda:defaultdict(dict));counts=defaultdict(dict);variations=defaultdict(dict)
for r in profiles:
    model,state,j=int(r["model"]),int(r["state"]),int(r["output"])
    profile_values[model][state][j]=float(r["value"])
    counts[model][state]=int(r["samples"]);variations[model][state]=float(r["rms_variation"])
paired=[];per_model=[]
for (model,fee),group in sorted(models.items()):
    base=group[0];live={r["sun_seed"]:r for r in group if r["mode"]=="live"}
    frozen={r["sun_seed"]:r for r in group if r["mode"]=="frozen"}
    assert len(live)==2 and live.keys()==frozen.keys()
    total_samples=sum(counts[model].values())
    rms=(sum(counts[model][s]*variations[model][s]**2 for s in range(8) if counts[model][s]>0)/max(1,total_samples))**.5
    # Frozen reproduction has one fixed winning actor per state. Follow the reachable state graph.
    reachable=set();state=int(base["source_state"])
    while state not in reachable:
        reachable.add(state);values=profile_values[model][state]
        desires=[values[1+actor*11+6] for actor in range(12)]
        actor=max(range(12),key=lambda actor:desires[actor])
        if desires[actor]<.5:break
        state=sum((1<<bit) for bit in range(3) if values[1+actor*11+8+bit]>=.5)
    fallback=sorted(s for s in reachable if counts[model][s]==0)
    for seed,l in live.items():
        f=frozen[seed]
        paired.append({"model":model,"fee":fee,"sun_seed":int(seed),"early":int(base["snapshot_step"])==60000,
                       "censored":l["status"]=="cap_stop" or f["status"]=="cap_stop",
                       "cell_seconds_gain":float(l["cell_seconds"])/max(1e-12,float(f["cell_seconds"]))-1,
                       "births_delta":int(l["births"])-int(f["births"]),
                       "energy_seconds_gain":float(l["energy_seconds"])/max(1e-12,float(f["energy_seconds"]))-1})
    valid=all(r["status"]!="cap_stop" for r in group)
    lsum=sum(float(r["cell_seconds"]) for r in live.values());fsum=sum(float(r["cell_seconds"]) for r in frozen.values())
    per_model.append({"model":model,"fee":fee,"origin_seed":int(base["origin_seed"]),"storage":float(base["storage"]),"work":float(base["work"]),
                      "snapshot_step":int(base["snapshot_step"]),"stored":int(base["stored"]),"valid":valid,
                      "cell_seconds_gain":lsum/max(1e-12,fsum)-1,
                      "births_delta":st.mean(int(r["births"]) for r in live.values())-st.mean(int(r["births"]) for r in frozen.values()),
                      "live_cell_seconds":lsum/2,"frozen_cell_seconds":fsum/2,
                      "calibration_rms":rms,"reachable_frozen_states":sorted(reachable),"reachable_fallback_states":fallback,
                      "shadow_rms":st.mean(float(r["command_deviation"]) for r in frozen.values())})
summary=[]
for step in (60000,600000):
    for fee in ("source","zero"):
        group=[r for r in per_model if r["snapshot_step"]==step and r["fee"]==fee and r["valid"]]
        gain=[r["cell_seconds_gain"] for r in group]
        pair_group=[r for r in paired if r["fee"]==fee and r["early"]==(step==60000) and not r["censored"]]
        summary.append({"snapshot_step":step,"fee":fee,"valid_models":len(group),
                        "mean_gain":st.mean(gain),"median_gain":st.median(gain),"min_gain":min(gain),"max_gain":max(gain),
                        "positive_models":sum(g>0 for g in gain),"positive_seed_pairs":sum(r["cell_seconds_gain"]>0 for r in pair_group),
                        "seed_pairs":len(pair_group),"mean_births_delta":st.mean(r["births_delta"] for r in group),
                        "reachable_fallback_models":sum(bool(r["reachable_fallback_states"]) for r in group)})
for name,data in (("paired",paired),("models",per_model),("summary",summary)):
    (a.output/(name+".json")).write_text(json.dumps(data,indent=2)+"\n")

fig,axes=plt.subplots(1,2,figsize=(13,5))
for ax,fee in zip(axes,("source","zero")):
    for step,color,label in ((60000,"#4079b8","Früh: 1000 s"),(600000,"#df852d","Spät: 10000 s")):
        group=[r for r in per_model if r["fee"]==fee and r["snapshot_step"]==step and r["valid"]]
        for i,r in enumerate(group):ax.scatter(r["model"]//2+(0 if step==60000 else .18),100*r["cell_seconds_gain"],color=color,marker="x" if r["reachable_fallback_states"] else "o",label=label if i==0 else None)
    labels={r["model"]//2:f"{r['storage']:g}/{r['work']:g}\ns{r['origin_seed']}" for r in per_model}
    ax.set_xticks([i+.09 for i in sorted(labels)],[labels[i] for i in sorted(labels)],fontsize=8)
    ax.axhline(0,color="black",linewidth=.8);ax.grid(alpha=.2)
    ax.set_title("Herkunftspreise" if fee=="source" else "Kostenfreie Gehirne")
    ax.set_xlabel("Struktur-/Rechenpreis und Evolutionsseed; ×: erreichbares Ersatzmuster");ax.set_ylabel("Zellsekunden: normal gegenüber fest (%)");ax.legend()
fig.suptitle("Funktionsvergleich: je Punkt ein Genom, Verhältnis über zwei Sonnenfolgen")
fig.tight_layout();fig.savefig(a.output/"function-gains.png",dpi=140)
print(json.dumps({"trials":len(rows),"status_counts":{s:sum(r["status"]==s for r in rows) for s in sorted({r["status"] for r in rows})},"summary":summary},indent=2))
