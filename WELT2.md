# WORLD2: breite Welt mit der neuen Physik

Status: **umgesetzt** (20.10.2026), `TrainMode.WORLD2`, `World2TrainStrategy`. Offene Evolution
ohne Selektion, wie NONE, aber viermal so breit und mit allen neuen Mechaniken eingeschaltet.

## Wiederholte Spektralzonen

- Neuer Wert `sunZoneWidth` (px) in `SimulationConfig`; GUI-Regler „Spectral zone width“
  im Umweltpanel (0–3200 px, Schritte von 100 px).
- `0` (Standard) bedeutet wie bisher: ein Zyklus 000 | 001 | 010 | 100 über die ganze Welt,
  jede Zone ein Viertel der Breite. Alte Welten und Läufe bleiben unverändert.
- Ein fester Wert wiederholt den Zyklus alle `4 × sunZoneWidth` px. Die Mischstreifen
  (15 % einer Zonenbreite) liegen an jeder Zonengrenze, auch zwischen zwei Zyklen (100 → 000).
- Die Weltbreite sollte ein Vielfaches eines Zyklus sein; sonst endet der letzte Zyklus
  abgeschnitten an x = 0. Die Darstellung zeichnet die Zonen wiederholt.
- Studien zählen Zellen und Überreste nach Zonenform, nicht nach Weltviertel
  (`ZoneSeasonStudyTest.zone`).

Tests: `SunSpectrumTest` (Wiederholung, Grenzen zwischen Zyklen, Summe der Anteile, Standard
gleich altem Verhalten, Sonne nutzt die eingestellte Breite), `MainControllerLayoutTest`.

## Die Welt

| | NONE | WORLD2 |
|---|---|---|
| Breite × Höhe | 6400 × 1200 | 25 600 × 1200 |
| Zonenbreite | ein Zyklus je Welt (1600) | 1600, viermal wiederholt |
| Gründer | `initialCellCount` (30) | 4 × `initialCellCount` (120), über die ganze Breite |
| Gelände | NONE-Nischen | NONE-Nischen viermal, je um eine Zone versetzt |
| Kamera | Maßstab 6,5 | Maßstab 6,5 (etwa ein Abschnitt sichtbar; herauszoomen bis 0,02) |

- **Nischen:** flache Pferche, ein niedriges Schattendach, eine Reihe hoher Wände mit
  schmalen Spalten, Einzelwände verschiedener Höhe, eine Reihe kleiner Plattformen oben
  (Fleckenschatten) und ein zweites Pferchfeld. Abschnitt `k` verschiebt alle Nischen um
  `k × 1600` px innerhalb des Abschnitts. So liegt jede Nische einmal in jeder Lichtform; das
  Schattendach etwa unter 000, 001, 010 und 100.
- **Physik:** Beim Anlegen schaltet WORLD2 Spektralzonen, Zellhaftung, Bindungen,
  Geburtsbindung, Bodenfeld und Anker ein (Policy `OPEN_WORLD`). Das Umweltpanel zeigt
  die Werte nach „New…“ an (`EnvironmentPanel.syncFromConfig`). Die Schalter bleiben danach
  frei änderbar und gelten auch für später angelegte NONE-Welten, bis man sie zurückstellt.
- **Nicht verändert:** Sonnenrate, Energie pro Strahl, Jahreszeiten (aus), Lebensdauer,
  Gehirnkosten. Die Strahlen pro Sekunde wachsen mit der Breite; die Lichtdichte je px ist
  dieselbe wie in NONE.

Test: `World2TrainStrategyTest` (Breite, Zonenbreite, Schalter, Policy, 1 + 4 × 55 Blocker
innerhalb der Welt, Schattendach in allen vier Lichtformen, Gründer in allen Abschnitten).

## Leistung (20.10.2026)

`TrainModeStudyTest`, `-Dtrainmode.study.mode=WORLD2`, Seed 4711, 600 s, ohne GUI, eine JVM
(1,5 GB), dieser Rechner. Die Welt ist nach etwa 2 min mit 1200–1700 lebenden Zellen und
350–550 Überresten besiedelt, in mehreren Kolonien, überwiegend in den 000-Zonen.

| Modellstand | Simulationssekunden je Wandsekunde | lebend Ø | Überreste Ø |
|---|---|---|---|
| erster Lauf (`a7cfd9b`, bis 419 s) | 0,22 | 1375 | 467 |
| + Gelände-Vorfilter und x-Index, Transferports je Schritt (`9395573`) | 0,43 | 1353 | 412 |
| + Transferbudgets nur für Zellen mit Port (`f3a3b68`) | **0,49** | 1223 | 445 |

Engpässe des ersten Laufs (Stapelproben): Ankersuche und Bodenfeld prüften jeden Aktor
gegen alle 221 Blocker der Welt (in NONE 56); der Energietransfer berechnete die Geometrie
aller Ports für jede vorhandene Energieform neu und legte je Form Budgets für alle Zellen an.
Alle drei Änderungen lassen die Ergebnisse unverändert:
- `TerrainIndex` liefert alle Blocker in Reichweite in Listenreihenfolge (`TerrainIndexTest`);
  `GroundHoldTest` und `CellBondTest` geben dieselben Zahlen wie vorher.
- `EnergyTransferLinkRegressionTest` vergleicht den Transfer bitgenau mit dem alten Algorithmus.

Jetzt verteilt sich die Zeit auf Aktorfelder zwischen Zellen, Gehirne und Sensoreingaben;
das ist Arbeit je Zelle und wächst mit der Population. Eine Stunde Simulation braucht
etwa zwei Stunden. Die GUI kostet zusätzlich Zeichenzeit; beim Herauszoomen auf die
ganze Welt mehr.

Parameter: Lichtdichte, Lebensdauer und Gehirnkosten sind wie in NONE gelassen. Wird es zu
langsam, sind die nächsten Hebel weniger Licht je px (Sonnenregler: weniger Zellen je
Fläche) oder eine kürzere Halbwertszeit der Überreste (weniger Überreste).
