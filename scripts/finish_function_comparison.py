"""Archive and summarize a completed function comparison; called by the background watcher."""
import argparse,csv,gzip,hashlib,json,shutil,subprocess,sys
from pathlib import Path

p=argparse.ArgumentParser();p.add_argument("folder",type=Path);p.add_argument("--output",type=Path,required=True)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
script=Path(__file__).resolve().parent
subprocess.run([sys.executable,str(script/"analyze_function_comparison.py"),str(a.folder),"--output",str(a.output)],check=True)
for name in ("results.csv","settings.txt","signals.csv"):
    shutil.copy2(a.folder/name,a.output/name)
if (a.folder/"launch.json").exists():shutil.copy2(a.folder/"launch.json",a.output/"launch.json")
for name in ("profiles.csv","progress.csv"):
    (a.output/(name+".gz")).write_bytes(gzip.compress((a.folder/name).read_bytes(),mtime=0))
records=[]
for path in sorted(a.folder.glob("model-*.cells")):
    with path.open("rb") as stream:sha=hashlib.file_digest(stream,"sha256").hexdigest()
    records.append({"file":path.name,"bytes":path.stat().st_size,"sha256":sha})
assert len(records)==18
(a.output/"model-files.json").write_text(json.dumps(records,indent=2)+"\n")
with (a.folder/"signals.csv").open() as stream:signals=list(csv.DictReader(stream))
assert len(signals)==18*8*20
signal_summary=[]
for model in range(18):
    rows=[r for r in signals if int(r["model"])==model]
    signal_summary.append({"model":model,"probes":len(rows),
        "max_raw_delta":max(float(r["max_raw_delta"]) for r in rows),
        "max_applied_delta":max(float(r["max_applied_delta"]) for r in rows),
        "raw_changed_probes":sum(float(r["max_raw_delta"])>1e-9 for r in rows),
        "applied_changed_probes":sum(float(r["max_applied_delta"])>1e-9 for r in rows),
        "winner_changed_probes":sum(r["winner_changed"]=="true" for r in rows),
        "child_state_changed_probes":sum(r["child_state_changed"]=="true" for r in rows),
        "changed_features":sorted({r["max_applied_feature"] for r in rows if float(r["max_applied_delta"])>1e-9})})
(a.output/"signal-summary.json").write_text(json.dumps(signal_summary,indent=2)+"\n")
summary=json.loads((a.output/"summary.json").read_text())
text=["# Funktionsvergleich: automatische Messzusammenfassung","",
      "144 Vergleichsläufe für 18 Genome, zwei Sonnenfolgen, normale und feste Steuerung.",
      "Kalibrierung: unterschiedliche Gründerwinkel, stabile Online-Mittelwerte und Varianzen.","",
      "| Speicherstand | Gebühren | Modelle | Median Zellsekunden-Vorteil normal | Min / Max | Positive Paare |",
      "|---|---|---:|---:|---:|---:|"]
for r in summary:
    text.append(f"| {'Früh (1000 s)' if r['snapshot_step']==60000 else 'Spät (10000 s)'} | {r['fee']} | {r['valid_models']} | {100*r['median_gain']:.3f} % | {100*r['min_gain']:.3f} / {100*r['max_gain']:.3f} % | {r['positive_seed_pairs']}/{r['seed_pairs']} |")
text.extend(["","Die festen Muster sind keine optimierten statischen Gegenspieler. Die zwei Sonnenfolgen",
             "und Herkunftsgenome bilden eine explorative Stichprobe. Rohe Aktivität und veränderte",
             "angewendete Ausgaben beweisen allein keinen biologischen Nutzen. Die synthetischen",
             "Signalproben testen Einfluss auf Ausgaben, keine Fitness. `signal-summary.json` und",
             "`signals.csv` trennen rohe Ausgaben, angewendete Ausgaben und angeforderte Teilungswahl.",
             "`max_applied_feature` bezeichnet pro Probe nur die größte Änderung; weitere können vorhanden sein.",
             "","Der erste Marker wird oberhalb einer numerischen Schwelle von 1e-9 gezählt;",
             "damit ist noch keine biologische Relevanzschwelle festgelegt.","",
             "Methodik: [FUNKTIONSVERGLEICH.md](../../../FUNKTIONSVERGLEICH.md).",
             "Vorversuche werden separat aufbewahrt und nicht in die Zusammenfassung eingemischt.",""])
(a.output/"README.md").write_text("\n".join(text),encoding="utf-8")
print("Function comparison archived to",a.output)
