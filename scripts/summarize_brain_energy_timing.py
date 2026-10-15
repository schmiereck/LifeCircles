"""Pair final models by seed and task, comparing only preserved successful functions."""
import argparse
import csv
import json
import statistics as st
from collections import defaultdict
from pathlib import Path

p=argparse.ArgumentParser();p.add_argument("input",type=Path);p.add_argument("output",type=Path);a=p.parse_args()
with a.input.open() as stream: rows=list(csv.DictReader(stream))
groups=defaultdict(list)
for r in rows:
    groups[(r["dataset"],r["phase"],r["task"],float(r["storage"]),float(r["work"]),int(r["seed"]))].append(r)
models={key:dict(ns=st.median(float(r["ns_per_decision"]) for r in data),solved=data[0]["solved"]=="true") for key,data in groups.items()}
paired=defaultdict(list)
for (dataset,phase,task,s,w,seed),model in models.items():
    reference_dataset="brain-energy-acquire" if phase=="acquire" else dataset
    base=models.get((reference_dataset,phase,task,0.,0.,seed))
    if not base or not base["solved"] or not model["solved"]:continue
    paired[(dataset,phase,task,s,w)].append(base["ns"]/model["ns"])
result=[dict(dataset=key[0],phase=key[1],task=key[2],storage=key[3],work=key[4],n=len(values),
             median_speedup=st.median(values),min_speedup=min(values),max_speedup=max(values)) for key,values in sorted(paired.items())]
a.output.mkdir(parents=True,exist_ok=True)
(a.output/"timing-paired.json").write_text(json.dumps(result,indent=2),encoding="utf-8")
with (a.output/"timing-paired.csv").open("w",newline="") as stream:
    writer=csv.DictWriter(stream,fieldnames=list(result[0]));writer.writeheader();writer.writerows(result)
print(f"{len(models)} models, {sum(int(r['decisions']) for r in rows)} measured decisions; successful paired speedups in {a.output}")
