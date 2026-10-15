"""Keep compact review data in docs and full genomes outside Git, surviving mvn clean."""
import argparse
import gzip
import shutil
from pathlib import Path

p=argparse.ArgumentParser();p.add_argument("folders",nargs="+",type=Path)
p.add_argument("--docs",type=Path,required=True);p.add_argument("--models",type=Path,required=True)
a=p.parse_args()
for source in a.folders:
    name=source.name.removeprefix("brain-energy-")
    docs=a.docs/name;models=a.models/name
    docs.mkdir(parents=True,exist_ok=True)
    shutil.copytree(source,models,dirs_exist_ok=True)
    for filename in ["results.csv","settings.txt"]:shutil.copy2(source/filename,docs/filename)
    (docs/"trajectory.csv.gz").write_bytes(gzip.compress((source/"trajectory.csv").read_bytes(),mtime=0))
print(f"Archived {len(a.folders)} datasets; genomes in {a.models}")
