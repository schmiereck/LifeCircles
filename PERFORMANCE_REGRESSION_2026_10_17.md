# Regression durch serielle Gehirnfinanzierung

## Befund und Korrektur

Die in `daf8fb0` eingeführte Vorabfinanzierung durchläuft die gespeicherten Gene
und die mögliche Rechenarbeit jedes Gehirns. Anders als die eigentliche Ausführung
lief diese neue Arbeit für die gesamte Population seriell. Große Populationen
bekamen dadurch einen zusätzlichen Engpass auf einem einzelnen Thread.

Finanzierung und Eingabeerzeugung laufen jetzt jeweils parallel über unabhängige
Zellen. Die Phasengrenzen bleiben erhalten: zuerst **alle** Speichergebühren und
Pausenbefehle, danach **alle** Sensorwerte, danach die Entscheidungen und deren
tatsächliche Arbeitsgebühren. Die Reihenfolge der Eingabevektoren bleibt erhalten.
Unbezahlter Speicher verhindert ohnehin das Denken; dessen Arbeitsobergrenze wird
nicht mehr zusätzlich berechnet. Bei Speicherpreis null entfällt die Genzählung.
Der Profiler erfasst `BRAIN_FUNDING` nun getrennt von `INPUTS`.

## Kurzer Vergleich am 17.10.2026

Lokales Archiv:
`experiments/none-energy-2026-10-16/seed-2-s1.0-w1.0/end.cells`.
SHA-256: `2fe8fcdeced26a72d071dce1cc2b33fa955d2da0e01614fc5a968a41929a6d0e`.
2000 lebende Zellen, 1.501.957 gespeicherte Synapsen. NONE-Geometrie/Standardpreise,
normale Sonne, Umwandlung, Teilung und Mutation, feste Zufallsstarts für Sonne (713),
Gehirnmutation (421) und Geburtsmerkmale (835). Original aus `daf8fb0` und Fix
nacheinander in separaten JVMs gemessen; jeweils 120 Schritte Warmlauf, 60 Messschritte.

| Rechenphase | Original, ms/Schritt | Fix, ms/Schritt |
|---|---:|---:|
| Finanzierung + Eingaben | 32,266 | 6,413 |
| Gehirnausführung | 15,461 | 16,518 |
| Raster | 0,602 | 0,604 |
| Interaktionen | 3,844 | 3,918 |
| Energieaustausch | 4,298 | 4,912 |
| Gesamte erfasste Schrittphasen | 58,996 | 34,935 |

Rund 41 % weniger Rechenzeit bzw. 1,69-fache Geschwindigkeit in dieser kurzen
Stichprobe. Mutation ist bereits in Reproduktion enthalten und wurde nicht doppelt
summiert. GUI, Dateiladen und Diagnose-Prüfsummen sind nicht in der Summe enthalten.
Kein FPS-Versprechen für andere Populationen oder Hardware.

Über alle 180 Schritte stimmen die Prüfsummen der erfassten Zellzustände bitgenau
überein: Position, Geschwindigkeit, Rotation, Radius, Alter, Zustand, Pause/Tod,
alle acht Energien, verarbeitete Synapsen und Kraft-/Austausch-/Teilungsbefehle.
Endstand jeweils 2035 Zellen, davon ein pausiertes Gehirn.
Gemeinsame SHA-256: `9e85edbb90d1d5fa2fc31b0802577fc3f775a0b8dc584d6d90dbacd130cd8e4b`.

Rohprotokolle, lokal unter `target/`:
`performance-regression-reference-seeded.log`, `performance-regression-fixed-seeded.log`.
Opt-in-Diagnose für weitere Archive:

```powershell
& C:/Users/thomas/.maven/maven-3.10.0/bin/mvn.cmd -q `
  '-Dtest=PerformanceRegressionProbeTest' `
  '-Dperformance.file=PFAD_ZUR_DATEI.cells' `
  '-Dperformance.warm=120' '-Dperformance.steps=60' test
```

Die Diagnose lädt nur die Population, keine damaligen GUI-Reglereinstellungen oder
Weltgeometrie. Sie setzt diese in eine NONE-Welt. Quelldateien werden nicht verändert.

## Verifikation und Grenzen

139 gezielte Tests erfolgreich, darunter Finanzierung/Pause/Wiederanlauf,
Energieaustausch, Sensoren, verschachtelte Netze, Physik und HIGH_POSITION3.
`ParallelBrainFundingTest` prüft beide Phasenbarrieren ausdrücklich an 64 Zellen
mit unterschiedlichen Speichergebühren und einem Anteil pausierender Gehirne.

Der gemeldete Rückgang von 20–25 auf 5 Calc FPS bei rund 2500 Zellen ist damit
noch nicht quantitativ vollständig reproduziert. Die zum Messzeitpunkt laufende
GUI enthielt nur rund 330 Zellobjekte. Das Öffnen des zuletzt gefundenen großen
OneDrive-Archivs wartete im Dateizugriff; dieser eigene Diagnoseprozess wurde
beendet und durch den lokalen Vergleich ersetzt. Die GUI blieb unverändert laufen.
Die Wirkung auf die konkrete Nutzerpopulation muss nach Neustart gemessen werden.
