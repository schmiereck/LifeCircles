"""Archive the DNA/fee-preserving execution-plan measurements separately from structural pruning."""
import argparse, csv, hashlib, json, platform, shutil, statistics as st
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

p=argparse.ArgumentParser()
p.add_argument("folder",type=Path);p.add_argument("--models",type=Path,required=True);p.add_argument("--output",type=Path,required=True)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
def read(name):
    with (a.folder/name).open() as f:return list(csv.DictReader(f))
counts,eq,timing=read("counts.csv"),read("equivalence.csv"),read("timing.csv")
assert len(counts)==18 and {int(r['model']) for r in counts}==set(range(18))
for r in counts:
    for field in ('root_neurons','root_synapses','stored_synapses','copy_bytes'):
        assert r[field+'_before']==r[field+'_after']
assert len(eq)==144 and {(int(r['model']),int(r['birth_state'])) for r in eq}=={(m,s) for m in range(18) for s in range(8)}
assert all(int(r['steps'])==2200 and int(r['output_comparisons'])==2200*133 and r['executed_before']==r['executed_after'] for r in eq)
assert len(timing)==252 and {(int(r['model']),int(r['repeat']),r['mode']) for r in timing}=={(m,i,mode) for m in range(18) for i in range(7) for mode in ('original','planned')}
models=[]
for m in range(18):
    ratios=[]
    for i in range(7):
        pair={r['mode']:r for r in timing if int(r['model'])==m and int(r['repeat'])==i}
        x,y=pair['original'],pair['planned']
        assert x['checksum']==y['checksum'] and x['executed_synapses']==y['executed_synapses']
        assert int(x['steps'])==int(y['steps'])==2000 and int(x['nanos'])>0 and int(y['nanos'])>0
        ratios.append(int(x['nanos'])/int(y['nanos']))
    models.append({'model':m,'median_speedup':st.median(ratios),'paired_speedups':ratios})
summary={'models':18,'sequences':144,'bit_exact_output_comparisons':sum(int(r['output_comparisons']) for r in eq),
         'billed_synapses_original':sum(int(r['executed_before']) for r in eq),'billed_synapses_planned':sum(int(r['executed_after']) for r in eq),
         'median_model_speedup':st.median(r['median_speedup'] for r in models),'min_model_speedup':min(r['median_speedup'] for r in models),
         'max_model_speedup':max(r['median_speedup'] for r in models),'note':'Same DNA, outputs, scheduling and logical work bill; isolated forward execution, not world performance'}
files=[]
for m in range(18):
    path=a.models/f'model-{m}.cells';files.append({'file':path.name,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
known=json.loads(Path('docs/function-comparison/stable/model-files.json').read_text())
assert {r['file']:r['sha256'] for r in files}=={r['file']:r['sha256'] for r in known}
for name in ('counts.csv','equivalence.csv','timing.csv','settings.txt'):shutil.copy2(a.folder/name,a.output/name)
for name,data in (('summary.json',summary),('models.json',models),('source-models.json',files)):
    (a.output/name).write_text(json.dumps(data,indent=2)+'\n')
sources=['src/main/java/de/lifecircles/model/neural/'+name+'.java' for name in ('NeuralNet','NetworkExecutionPlan','NeuronNetwork')]
sources.append('src/test/java/de/lifecircles/model/neural/NetworkExecutionPlanStudyTest.java')
(a.output/'provenance.json').write_text(json.dumps({'implementation_commit':'28a526e','platform':platform.platform(),
    'source_files':[{'path':s,'sha256':hashlib.sha256(Path(s).read_bytes()).hexdigest()} for s in sources]},indent=2)+'\n')
fig,ax=plt.subplots(figsize=(10,4.8))
for r in models:
    c='#4079b8' if r['model']%2==0 else '#df852d'
    ax.scatter([r['model']]*7,r['paired_speedups'],color=c,alpha=.25,s=15)
    ax.scatter(r['model'],r['median_speedup'],color=c,s=40)
ax.axhline(1,color='black',linewidth=.8);ax.set_xticks(range(18));ax.grid(axis='y',alpha=.2)
ax.set_xlabel('Modell-ID; blau: früh, orange: spät');ax.set_ylabel('Originalzeit / Zeit mit Ausführungsplan')
ax.set_title('DNA und Energieabrechnung erhalten: Einzelpaare und Median je Genom')
fig.tight_layout();fig.savefig(a.output/'execution-plan.png',dpi=150)
print(json.dumps(summary,indent=2))
