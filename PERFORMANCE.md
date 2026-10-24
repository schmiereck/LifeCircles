# Performance: kurze Messungen vor langen Evolutionsläufen

Die Gesamtmessungen unten stammen vom 14. Oktober 2026. Die weitere Sonnenoptimierung
vom 15. Oktober ist im Nachtrag am Ende dokumentiert.

## Ergebnis

Die einfachen Verbesserungen sind umgesetzt. Die Simulation entscheidet weiterhin
in jedem Physikschritt. Bei den gemessenen dichten Kolonien mit 4096 Zellen sinkt die
Schrittzeit von rund 59,8 auf 46,5 ms, also um etwa **22 %**. Das entspricht ungefähr
29 % mehr Schritten pro Sekunde. Diese Zahlen stammen aus einem kontrollierten Lauf
mit vier Common-Pool-Workern und umfassen vollständige Snapshots bei jedem zweiten Schritt.

| Welt | Vorher, ms/Schritt | Nachher, ms/Schritt |
| --- | ---: | ---: |
| Dünn, 1024 Zellen | 9,92 | 8,82 |
| Dünn, 4096 Zellen | 36,75 | 36,44 |
| Dicht, 1024 Zellen | 13,52 | 11,56 |
| Dicht, 4096 Zellen | 59,79 | 46,50 |

Medians aus jeweils drei Wiederholungen. Bei dünnen großen Welten ist der Gewinn
klein: Dort dominieren bereits die Gehirne. Schwankungen durch JIT, GC und andere
Prozesse sind sichtbar; die Werte sind keine garantierten Grenzwerte.

## Welche Kandidaten haben sich bestätigt?

### 1. Nachbarschaft und Aktorvergleiche

Der günstige Mittelpunkt-Abstandstest existierte bereits, wurde aber für jeden der
zwölf eigenen Aktoren wiederholt. Er läuft jetzt einmal pro gerichtetem Zellpaar.
Reichweite und Größenskalierung werden dabei ebenfalls einmal berechnet. Anschließend
verwenden alle eigenen Aktoren dieselbe Kandidatenliste. Periodische Geometrie,
Nachbarreihenfolge, Sensorprioritäten und Reihenfolge der Kraftsummierung bleiben erhalten.

Die Bucket-Listen wurden ebenfalls bereits wiederverwendet; `build()` leerte sie
auch nicht mehr doppelt. Neu ist die Wiederverwendung der **Nachbarlisten pro Bucket**:
Zellen im selben Bucket haben dieselben Rasterkandidaten. Diese Liste wird einmal
zusammengestellt und bis zum nächsten Rasteraufbau gemeinsam gelesen. Beim nächsten
Aufbau werden zuvor verwendete Listen geleert, damit tote oder weggezogene Zellen
nicht über alte Listen festgehalten werden.

| Phase, dichte Welt mit 4096 Zellen | Vorher, ms/Schritt | Nachher, ms/Schritt |
| --- | ---: | ---: |
| Raster | 2,91 | 1,10 |
| Sensoren/Aktoren | 19,10 | 9,07 |
| Gehirne | 24,73 | 23,60 |

Die kleine Differenz bei Gehirnen ist kein Nachweis einer Gehirnoptimierung; deren
Berechnung wurde nicht optimiert. Messrauschen, Cache und GC beeinflussen Phasen gemeinsam.
Auch die optimierte dichte Welt muss alle tatsächlich nahen Aktorpaare prüfen.

### 2. Sonne: Streifen quer zur Strahlrichtung (erste Variante, 14. Oktober)

Alle Strahlen eines Schritts haben dieselbe Richtung. Für sie ist die Koordinate
`u = x * dirY - y * dirX` entlang des Strahls konstant. Kreise und Rechtecke werden
über ihre projizierten Intervalle in der ersten Variante in 64 Einheiten breite Streifen einsortiert. Ein
Strahl prüft nur die Objekte in seinem Streifen, weiterhin mit den bisherigen exakten
Kreis- und Rechteckschnittberechnungen. Die ursprüngliche Objekt-Reihenfolge bleibt
auch bei identischen Treffern erhalten. Die konservative Intervallauflösung berücksichtigt
Rundung an Streifengrenzen.

In dieser ersten Variante blieb es für weniger als acht Strahlen oder weniger als insgesamt 256 Objekte beim
direkten Scan. So entsteht bei kleinen Welten und schwacher Sonne kein zusätzlicher Index.
Der Index wird bei Bedarf pro Schritt neu erzeugt; Bewegungen, Größenänderungen und
änderbare Blocker benötigen dadurch keine Invalidierungslogik. Die bestehende
Sonnengeometrie wird beibehalten, einschließlich außerhalb der Welt startender Strahlen.

Separate Sonnenprobe, Welt 6400 × 6400, 100 Blocker, Strahlrate 120, Abstand 20:

| Zellen | Vollscan, ms/Schritt | Index einschließlich Aufbau, ms/Schritt | Faktor |
| --- | ---: | ---: | ---: |
| 1024 | 1,59 | 0,276 | 5,7 |
| 4096 | 3,97 | 0,343 | 11,6 |
| 16384 | 18,96 | 1,339 | 14,2 |

Bei der üblichen Strahlrate 10 und Abstand 60 kostet die Sonne in der 4096er-Dichteprobe
schon vorher nur etwa 0,33 ms. Der große Sonnenfaktor lässt sich deshalb nicht auf die
gesamte Simulation übertragen. Er wird bei hoher Strahldichte und größeren Populationen relevant.

### 3. Snapshots und Darstellung

Snapshots waren bereits an Bedarf und Zustandsversion gekoppelt. Neu ist eine kompakte
Variante ohne Aktordaten. Die Übersicht fordert sie an, wenn der Zoom höchstens 0,5
beträgt oder weder Aktoren noch Kraftfelder eingeschaltet sind. Beim Wechsel auf Details
wird auch ohne neuen Simulationsschritt sofort ein vollständiger Snapshot erfasst.
`getLatestState()` liefert weiterhin vollständige Daten; die kompakte Variante wird
explizit mit `getLatestState(false)` angefordert. Beide bleiben vom Modell getrennt
und unveränderlich. Der Cache berücksichtigt Zustandsversion und Detailmodus.

| Zellen | Vollständiger Snapshot, ms | Übersicht, ms |
| --- | ---: | ---: |
| 1024 | 0,278 | 0,044 |
| 4096 | 1,507 | 0,235 |
| 16384 | 8,347 | 1,545 |

Diese Werte messen nur die Erstellung, keine JavaFX-Zeichenzeit und keine Wartezeit
auf den Simulationslock. Die vollständigen Snapshots enthalten zwölf Aktor-Datensätze
pro Zelle, die kompakten keinen.

Der Renderer überspringt Zellen außerhalb des Ausschnitts. Kraftfelder berücksichtigen
ihre zusätzliche Reichweite. Aktoren, Kraftfelder und Balken werden erst ab acht Pixeln
Zellradius, Beschriftungen ab zehn Pixeln gezeichnet, jeweils zusätzlich zur Zoomgrenze.
Die bereits vorhandene Begrenzung auf 30 Renderbilder/s bleibt bestehen. Neuzeichnen
bei Pause bleibt möglich, damit Kamera- und Anzeigeänderungen sichtbar werden.
Die JavaFX-Darstellung wurde kompiliert, aber in diesem Schritt nicht visuell geprüft.

## Messaufbau und Grenzen

- Windows, Java 25, Host mit zwölf logischen Prozessoren.
- Hauptvergleich: Common-Pool-Parallelität explizit vier. Identische Befehle und
  Weltlayouts vor und nach den Kandidatenoptimierungen.
- 1024 oder 4096 Zellen in quadratischen Welten von 3200 beziehungsweise 6400 Einheiten
  Seitenlänge; Abstand der Zellmittelpunkte 96 oder 24, Radius 12.
- Jede Zelle rechnet ein echtes, mit Seed 731 erzeugtes Netz: acht versteckte Neuronen,
  15 % Verbindungshäufigkeit, reguläre Ein- und Ausgabegrößen. Die Ergebnisse werden
  durch feste Testausgaben ersetzt: Radius 12, neutrale Kräfte, keine Teilung und kein
  Energietransfer. Das hält die Last während des kurzen Vergleichs stabil. Die Sonne
  im Hauptvergleich besitzt weiterhin einen zufälligen Start; die separaten Sonnenproben
  verwenden identische Seeds für beide Verfahren.
- Je 15 Aufwärmschritte, 30 gemessene Schritte bei 1/60 s Simulationszeit, drei Wiederholungen.
  Die erste Wiederholung kann noch JIT-Effekte enthalten. Vollständige Snapshots bei jedem
  zweiten Schritt; unmittelbare Wiederholungsanfragen müssen denselben Snapshot liefern.
- Anschließend acht separate Mutationsproben ohne Einsetzen der Kinder. Diese Zeiten
  gehören nicht zur zuvor gemessenen Gesamtschrittzeit. Geburtshäufigkeiten eines
  Evolutionslaufs lassen sich daraus nicht ableiten.
- Die Dichteprobe prüft hohe lokale Kandidatenzahlen, aber keine aktiven Muskelkräfte,
  große verschachtelte Gehirne oder laufende Fortpflanzung. Lange Evolutionsläufe wurden
  nicht gestartet.

Ein zusätzlicher Lauf mit der normalen, nicht eingeschränkten Common-Pool-Einstellung
erreicht nach den Optimierungen bei 4096 Zellen etwa **26,75 ms dünn / 32,87 ms dicht**.
Vier Worker sind hier also kein empfohlenes Produktionslimit. Ein entsprechender
Vorher-Lauf mit Standard-Pool liegt nicht vor; diese Werte belegen keinen zusätzlichen
Optimierungsfaktor. Rohdaten: [Standard-Pool](docs/performance/default-pool.csv).

## Phasenprofilierung im laufenden Programm

Die neue `SimulationProfiler`-Instanz jeder Welt misst Raster, Sonne, Abstoßung,
Sensor-/Aktorinteraktionen, Energietransfer, Eingaben, Gehirne, Integration,
Fortpflanzung, Mutation, Trainingsauswahl, Statistik und Snapshots getrennt.
Standardmäßig ist sie aus. Einschalten über die JVM-Option `-Dlife.profile=true`
oder `world.getProfiler().setEnabled(true)`, auslesen über `snapshot()`.

Gemessen wird verstrichene Phasenzeit inklusive paralleler Arbeit, keine Summe der
CPU-Zeiten aller Worker. Integration hat zwei Messabschnitte pro Schritt, deren Zeiten
addiert werden. Mutation liegt innerhalb von Fortpflanzung beziehungsweise Auswahl;
diese verschachtelten Werte dürfen nicht noch einmal zur Gesamtzeit addiert werden.
Snapshotzeiten entstehen nur bei tatsächlicher Neuerfassung. `reset()` wird zwischen
Schritten aufgerufen. Ohne Profilierung werden keine Uhren pro Zelle oder Neuron gelesen.
Für die getrennte Gehirnphase werden zunächst alle eingefrorenen Eingaben verarbeitet,
danach folgen Bewegung, Wachstum und Stoffwechsel; direkte `CellCalcService.updateCell()`-
Aufrufe denken und integrieren weiterhin gemeinsam.

## Nächster größerer Ansatz: Gehirne

Bei 4096 Zellen sind im kontrollierten Vier-Worker-Lauf etwa 24 ms für Gehirne und
weitere 3–5 ms für Eingaben nötig. Hier liegt das nächste große Potential. Eine niedrigere
Denkfrequenz wurde **nicht** als transparente Performancekorrektur eingebaut:

- Rekurrenter Speicher und Layer-Aktivierungszähler entwickeln sich derzeit pro Aufruf,
  nicht pro verstrichener Simulationssekunde.
- Die Kosten verarbeiteter Synapsen werden pro Ausführung berechnet; ohne Anpassung
  könnten bei übersprungenen Entscheidungen alte Zähler mehrfach abgerechnet werden.
- Neugeborene benötigen initiale Ausgaben, Größenänderungen müssen trotzdem jeden
  Physikschritt weiterlaufen, und Sensorereignisse zwischen Entscheidungen brauchen
  eine klar festgelegte Behandlung.

Das ist ein eigenes Modell-Experiment. Vorher lohnen sich Speicher- und CPU-Profile
an repräsentativen entwickelten Gehirnen, insbesondere für Eingabearrays, temporäre
Ausgaben und verschachtelte Netzwerke. Die Messung liefert dafür jetzt eine Grundlage.

## Reproduzieren und Prüfen

```powershell
mvn test '-Dtest=PerformanceStudyTest' '-Dperformance.study=true' '-Dperformance.output=target/performance' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
mvn test '-Dtest=SunPerformanceStudyTest,SnapshotPerformanceStudyTest' '-Dperformance.study=true' '-Dperformance.output=target/performance' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```

Optional: `-Dperformance.counts=4096`, `-Dperformance.steps=30`. Die Study-Klassen
für Performance und Mechanik laufen nur mit ihren expliziten Schaltern; die kurzen
Äquivalenztests sind normale Regressionen. Die Performance-Studien besitzen keine
Laufzeit-Pass/Fail-Schwellen.

Der gemeinsame gezielte Regressionslauf bestand mit **278 Tests ohne Fehler**,
einschließlich mechanischer Gewebeproben und vier neuer Prüfungen für Sonnenindex,
Bucket-Wiederverwendung, Snapshotmodus und Profilierung. Der Sonnenvergleich kontrolliert
Strahlendpunkte, Energie und Sonnenkontakt exakt über mehrere Winkel, außerhalb liegende
Objekte, überlappende Zellen und Blocker hinweg. Die Sonnen-Laststudie kontrolliert
zusätzlich identische Zellenergien zwischen beiden Verfahren.

- [Vorher](docs/performance/baseline.csv), [Nachher](docs/performance/optimized.csv).
- [Sonne](docs/performance/sun.csv), [Snapshots](docs/performance/snapshots.csv).
- [Messaufbau](src/test/java/de/lifecircles/service/PerformanceStudyTest.java),
  [Äquivalenzprüfungen](src/test/java/de/lifecircles/service/PerformanceEquivalenceTest.java).

## Nachtrag: Sonne am 15. Oktober 2026

Der bereits vorhandene Sonnenindex wurde gezielt weiter beschleunigt:

- Wiederverwendbare Arrays ersetzen die pro Schritt erzeugten HashMaps, Schlüsselobjekte
  und Bucket-Listen. Die Objektverweise werden auch in dunklen Schritten und beim
  Wechsel zum Vollscan freigegeben; die Arraykapazitäten bleiben erhalten.
- Die Querstreifen sind in normalen Welten 24 Einheiten breit. Eine zusätzliche
  Tiefenstaffelung gruppiert Objekte nach ihrem frühestmöglichen Treffer, bei normalen
  Welthöhen in 128 Einheiten hohen Gruppen. Jedes Objekt liegt in genau einer Tiefengruppe
  und gegebenenfalls mehreren Querstreifen. Die Kopfarrays sind auf insgesamt 65536
  Buckets begrenzt; für größere Welten werden die Gruppen gröber.
- Leere Streifen werden direkt erkannt. Innerhalb eines belegten Streifens beginnt
  die Suche bei der ersten belegten Tiefengruppe. Sobald deren untere Grenze hinter
  dem nächsten bekannten Treffer liegt, endet die Suche.
- Blocker werden zuerst geprüft; der erste Schattentreffer begrenzt anschließend die
  Zellsuche. Die bisherigen exakten Schnittgleichungen und die Zellpriorität bei
  gleichzeitigem Zell-/Blockertreffer bleiben erhalten.
- Tiefengruppen verändern die Suchreihenfolge. Deshalb tragen Kandidaten ihren ursprünglichen
  Listenindex: Bei exakt gleichem Zelltreffer gewinnt weiterhin die zuerst gelistete Zelle,
  auch wenn sie erst in einer späteren Gruppe geprüft wird. Tangenten und Treffern an
  Gruppengrenzen wird durch konservative Rundungspuffer Rechnung getragen.
- Der Index wird ab 16 Strahlen und insgesamt mindestens 256 Objekten verwendet.
  Die zusätzliche Mindeststrahlzahl vermeidet Aufbaukosten bei schwacher Sonne.
  Bei null Strahlen entfallen auch die Winkel- und Geometrieberechnungen; Sonnenkontakt,
  Tageszeit und Strahlakkumulator werden weiterhin für jeden Schritt aktualisiert.

Frische Vergleichsmessung auf demselben Rechner, weiterhin Welt 6400 × 6400,
Strahlrate 120, Abstand 20, 100 Blocker, jeweils drei Wiederholungen:

| Gleichmäßig verteilte Zellen | Vorhandener Streifenindex, ms/Schritt | Neuer Index, ms/Schritt | Faktor |
| --- | ---: | ---: | ---: |
| 1024 | 0,2713 | 0,0697 | 3,9 |
| 4096 | 0,3636 | 0,1753 | 2,1 |
| 16384 | 1,3180 | 0,6941 | 1,9 |

Die Tabelle vergleicht die Weiterentwicklung mit dem **bereits beschleunigten Index**.
Sie beschreibt ausschließlich die Sonnenphase. Die Gesamtschrittzeit wurde für diesen
Nachtrag nicht erneut gemessen.

Zusätzliche Lastfälle verwenden gleich große Zellen im Abstand 24, angeordnet als
quadratische Kolonie ab (2000, 1000). Die abgeschattete Variante ergänzt einen 8000
Einheiten breiten Blocker bei Y = 500. Bei 16384 Zellen kosten diese Fälle mit dem neuen
Index etwa **0,481 ms dicht / 0,488 ms abgeschattet**. Für diese beiden Anordnungen wurde
kein Vorher-Lauf des alten Index erfasst; daraus wird kein zusätzlicher Vergleichsfaktor abgeleitet.

Die normale Strahlrate 10 bei Abstand 60 wurde separat geprüft. In ihrem kurzen
Beobachtungsfenster entstehen im Mittel etwa sieben Strahlen pro Schritt; deshalb
benutzt auch der eingeschaltete Beschleuniger dort den direkten Scan. Dieser Lauf
dient der Kontrolle des Rückfalls, nicht dem Nachweis eines Sonnen-Speedups bei
der üblichen Strahldichte. Wie zuvor: 15 Aufwärm- und 30 Messschritte, Mediane aus
drei Wiederholungen, keine Laufzeit-Pass/Fail-Schwellen. JIT- und GC-Schwankungen bleiben sichtbar.

Die vier neuen `SunIndexRegressionTest`-Prüfungen vergleichen zusätzlich exakte
Strahlendpunkte, Energie und Sonnenkontakt bei Bewegung, Größenänderung, entfernten
Zellen, Dunkelheit, Umschaltung zum Vollscan, geänderten Weltdimensionen, überbreiten
Blockern, Tangenten, gleichen Treffern in verschiedenen Tiefengruppen, Strahlstart
innerhalb einer Zelle und Treffern genau am Weltende. Auch ein Blocker am Strahlstart
und identische Zell-/Blockertreffzeiten werden geprüft. Der gemeinsame gezielte Lauf
umfasst jetzt **282 Tests**.

```powershell
mvn test '-Dtest=SunIndexRegressionTest,PerformanceEquivalenceTest,SunContactTest'
mvn test '-Dtest=SunPerformanceStudyTest' '-Dperformance.study=true' '-Dsun.layouts=uniform,dense,shade' '-Dperformance.output=target/sun-followup' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
# Normale Strahldichte:
mvn test '-Dtest=SunPerformanceStudyTest' '-Dperformance.study=true' '-Dsun.rate=10' '-Dsun.spacing=60' '-Dperformance.output=target/sun-normal' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```

- [Vorheriger Index, frische Baseline](docs/performance/sun-followup-before.csv),
  [neuer Index, drei Anordnungen](docs/performance/sun-followup-after.csv),
  [normale Strahldichte](docs/performance/sun-followup-normal.csv).
- [Index](src/main/java/de/lifecircles/service/ProjectedSunIndex.java),
  [Regressionen](src/test/java/de/lifecircles/service/SunIndexRegressionTest.java).


## Netzkorrekturen und Netzperformance (15. Oktober 2026)

Die separate [Netzmessung](NETZPERFORMANCE.md) dokumentiert Aufbau, Vorwaertsrechnung,
Mutation und Kostenzaehlung nach den Logikkorrekturen. Der Hauptgewinn ist weniger
temporaerer Speicher; fuer die laufende Vorwaertsrechnung ist noch kein belastbarer
CPU-Gewinn nachgewiesen.


## WORLD2: Gehirne und Sensor-/Aktor-Wechselwirkungen (24. Oktober 2026)

WORLD2-Studie (`TrainModeStudyTest`, Seed 4711, 300 s Simulationszeit, `-Dlife.profile=true`,
unbelasteter Rechner). „Vorher“ ist Stand 8450ca4 in einem eigenen Worktree. Die Läufe
sind trotz Seed nicht deterministisch (parallele Ausführung); am Ende lebten 1014 / 1163 / 1152 Zellen.

| Phase, ms/Schritt | vorher | kompilierte Gehirne | + Wechselwirkungen |
| --- | ---: | ---: | ---: |
| INTERACTIONS | 8,95 | 9,12 | 1,72 |
| BRAINS | 9,56 | 4,49 | 4,18 |
| TRANSFER | 3,93 | 4,34 | 3,56 |
| INTEGRATION | 3,62 | 3,25 | 3,08 |
| BRAIN_FUNDING | 1,49 | 1,72 | 1,55 |
| Summe aller Phasen | 29,0 | 24,4 | 15,4 |

Gehirne: siehe [NETZKOMPILIERUNG.md](NETZKOMPILIERUNG.md). Wechselwirkungen, alle bitgleich
zum bisherigen Algorithmus (Kräfte, Drehmomente, erkannte Ziele):

- **Berührte Blocker** werden über den x-Index (`TerrainIndex`) statt über alle Blocker
  gesucht; es gewinnt weiter die kleinste `sensingId`, die Reihenfolge ist egal.
  Vorher etwa ein Drittel der Phase (221 Blocker in WORLD2, für jeden Aktor).
- **Nachbarzellen pro Aktor ausgeschlossen**: Aktoren sitzen auf dem Zellrand. Liegt ein
  Aktor weiter als Radius plus größere Reichweite (+1e-6 px) vom anderen Mittelpunkt
  entfernt, erreicht ihn keiner der dortigen Aktoren (Dreiecksungleichung, auch periodisch).
- **Affinität (`Math.pow`) nur in Reichweite**: außerhalb beider Reichweiten ist die Kraft 0.

Isoliert (`InteractionBenchmarkTest`, gespeicherte Population mit 1177 Zellen, parallel):
6,5 → 1,6–1,8 ms pro Aufruf. Verworfen: eine Abkürzung der periodischen Richtung in
`Vector2D` ohne Division (bitgleich, aber ohne Gewinn, im Blockerpfad langsamer).

Prüfung: `InteractionEquivalenceTest` vergleicht mit der Referenzkopie
`SensorActorForceReference` in kleinen und großen periodischen Welten mit Bindungen,
Ankern, Bodenfeld und Erkennung; eine absichtlich zu knappe Schranke fällt dort auf.

```powershell
mvn test '-Dtest=InteractionEquivalenceTest,ForceSignTest,CellAdhesionTest,CellBondTest,GroundHoldTest,MechanicsRegressionTest,PeriodicWorldTest,PhysicsRegressionTest,SensorTargetSelectionTest'
mvn test '-Dtest=InteractionBenchmarkTest' '-Dinteraction.bench.cells=target/trainmode-study/prof-world2.cells'
```

### Energietransfer (24. Oktober 2026)

Bitgleich zum bisherigen Algorithmus (`EnergyTransferLinkRegressionTest`, Referenzkopie):

- Die bis zu acht Energieformen werden **parallel berechnet**: Jede Form liest und ändert nur
  ihre eigenen Vorräte. Geschrieben wird weiter seriell in Formreihenfolge, weil das Array
  für weitere Formen beim ersten Schreiben angelegt wird.
- Aktive Ports werden **pro Zelle parallel** gesammelt und in Listenreihenfolge zusammengefügt.

Isoliert (`TransferBenchmarkTest`, 1158 Zellen, alle Formen vorhanden): 8,8–10 → 1,95 ms pro Aufruf.
WORLD2-Lauf wie oben: TRANSFER 3,56 → 1,59 ms/Schritt, obwohl am Ende 1246 statt 1152 Zellen
lebten (die übrigen Phasen stiegen dadurch um etwa 15 %; Summe aller Phasen 15,7 ms).

```powershell
mvn test '-Dtest=EnergyTransferLinkRegressionTest,EnergyTransferTest,MultiEnergyTransferTest'
mvn test '-Dtest=TransferBenchmarkTest' '-Dtransfer.bench.cells=target/trainmode-study/prof-world2.cells'
```

Größte verbleibende Blöcke: Gehirne (~4,5), Integration (~3,3), Wechselwirkungen (~1,9),
Gehirnfinanzierung (~1,7 ms/Schritt).
