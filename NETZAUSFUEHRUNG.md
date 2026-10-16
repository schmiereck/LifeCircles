# DNA-erhaltender Ausführungsplan und einstellbare Gehirnkosten

Stand: 16.10.2026, Java 25.0.3. Implementierung auf `master`, in der Simulation
standardmäßig aktiv. Die GUI muss für diesen Stand neu gestartet werden.

## Was wird schneller?

Die DNA wird nicht gekürzt. Ein vorbereiteter Ausführungsplan markiert diejenigen
skalaren versteckten Neuronen, die weder Ausgänge noch Taktgeber noch ein auszuführendes
Modul beeinflussen können. Nur deren Berechnung entfällt. Rückkopplungen und indirekte
Abhängigkeiten werden rückwärts bis zum vollständigen Abschluss verfolgt.

Alle dynamischen Taktgeber, alle Ausgänge und alle Module bleiben Wurzeln der Analyse.
Ein Modul wird weiter ausgeführt, auch wenn seine Ausgänge niemand verwendet: Seine
innere Taktsteuerung kann die bisherige Synapsenabrechnung eingabeabhängig verändern.
Innerhalb eines Moduls dürfen wiederum sichere skalare Berechnungen entfallen.
Die Ausführungszustände verschiedener Modulinstanzen bleiben getrennt.

Die bisherige logische Synapsenarbeit wird bei übersprungenen skalaren Neuronen weiterhin
mitgezählt, genau dann, wenn der ursprüngliche Layer sie ausgeführt hätte. Darum bleiben
bei Kostenfaktor 1 nicht nur Ausgaben, sondern auch Speichergebühren und Rechengebühren
gleich. `getProccessedSynapses()` ist somit die bisherige abrechenbare Arbeit, nicht
mehr zwingend die Zahl tatsächlich durchgeführter Multiplikationen.

Training mit Gradientenaufzeichnung verwendet weiterhin die vollständige Ausführung.
Pläne und Kontrollzustände sind transient. Sie werden beim Laden neu aufgebaut.
Mutation kopiert die vollständige DNA in ein frisches Netz; latente Neuronen und
Verbindungen bleiben als Mutationsziele erhalten.

## Strukturänderungen und Gedächtnis

Öffentliche Layer-/Neuron- und Synapsenarrays existieren weiterhin. Deshalb vergleicht
der Plan vor jedem Aufruf die Neuronenbelegung und die relevanten eingehenden Kanten
mit seinen Snapshots, einschließlich Änderungen innerhalb eines bestehenden Arrays.
Gewichte und Biases dürfen sich ändern: Die Analyse entfernt keine kleinen oder
Nullgewichte und bleibt deshalb von ihren Werten unabhängig.

Ändert sich eine relevante Topologie nach bereits übersprungener Berechnung, wird
der Aufruf mit einer eindeutigen Ausnahme abgebrochen. Ein stiller Neuaufbau wäre
falsch: Bisher übersprungene latente Neuronen haben nicht ihr vollständiges historisches
Gedächtnis berechnet. Für solche manuellen Strukturänderungen muss deshalb zuerst
eine frische Genotypkopie erstellt oder `rnnClearPreviousState()` aufgerufen werden.
Normale Zellteilung und Mutation verwenden bereits unabhängige Kopien mit leerem
Gedächtnis. Die Strukturprüfung verhindert somit einen still veralteten Plan; sie
verspricht keine beliebige Umverdrahtung eines laufenden Netzes unter Erhalt seiner
unberechneten Vergangenheit. Auch nach dem Laden eines Zustands sollte für manuelle
Umverdrahtung eine frische Kopie oder ein expliziter Reset verwendet werden.

Wenn ein Netz überhaupt keine skalaren Neuronen überspringen kann, entfällt die
wiederholte Planprüfung für dieses Netz. Seine Module können trotzdem eigene Pläne
nutzen. Ein Zustandsreset erlaubt die Neubewertung. Weitere strukturelle Änderungen
an einem solchen vollständig ausgeführten Netz führen höchstens zu verpassten
Optimierungen bis zum Reset, nicht zu ausgelassenen neuen Abhängigkeiten.

## Messung der produktiven Variante

Die 18 Genome des vorherigen Funktionsvergleichs wurden jeweils mit allen acht
Zellzuständen geprüft: 2200 Aufrufe pro Folge, Zustandswechsel bei Aufruf 1100,
reproduzierbare Eingaben in [-2,2] und zusätzlich größere Werte in [-100,100].
Die Referenz schaltet die Optimierung ausdrücklich aus, auch für innere Module.

Ergebnis:

- 144 Folgen mit 316.800 Netzschritten.
- **42.134.400 rohe Ausgabewerte bitgenau gleich**, ebenso Layerflags und Taktzähler.
- **188.418.940 abrechenbare Synapsenausführungen auf beiden Seiten**, schrittweise geprüft.
- Gleiche Neuronenzahl, Synapsenzahl, gespeicherte Synapsenzahl und Größe frischer
  serialisierter Genotypkopien.
- Median der Beschleunigung pro Genom, anschließend Median über 18 Genome:
  **1,200×**, entsprechend etwa **16,7 % weniger Vorwärtsrechenzeit**.
  Genom-Mediane reichen von 1,088× bis 1,332×.

Gemessen wurde auf einem Thread ohne Trainingsspur, Zustand 7, sieben Wiederholungen
mit wechselnder Reihenfolge Original/Plan, je 500 Aufwärm- und 2000 Messaufrufe.
Einzelmessungen streuen. Die Genome sind frühe/späte Verwandte aus neun Evolutionsgeschichten;
das ist keine unabhängige Stichprobe aller möglichen Netze. Die Messung enthält die
wiederkehrenden Strukturprüfungen, aber keine Planvorbereitung für laufend neu geborene
Zellen und keine gesamte Weltberechnung. Für größere Populationen sind Physik,
Sensorvergleiche, Sonnenstrahlen und Geburten weiterhin weitere Kostenstellen.

Die früher gemessenen **1,62×** gehörten zur strukturell gekürzten Forschungs-Kopie
aus [NETZVEREINFACHUNG.md](NETZVEREINFACHUNG.md). Sie sind nicht der Geschwindigkeitsgewinn
dieser DNA- und gebührenerhaltenden Umsetzung. Entwicklungsproben der Laufzeitvariante
lagen um 1,25×; der hier archivierte Abschlusslauf liegt bei 1,20×.

89 gezielte Kontrollen bestanden. Dazu gehören ein 1000-Schritte-Vergleich zweier
vollständiger kleiner Welten mit identischer Energie, Position, Geschwindigkeit und
Rotation, Speichern/Laden, Mutation der vollständigen DNA, Module, öffentliche
Arrayänderungen, Kostenregler und JavaFX-Layout. Die neue lange Netzprobe bestand
zusätzlich als eigener explizit aktivierter Studientest.

Messarchiv: [Zusammenfassung](docs/network-execution-plan/2026-10-16/summary.json),
[Zeitmessungen](docs/network-execution-plan/2026-10-16/timing.csv),
[Grafik](docs/network-execution-plan/2026-10-16/execution-plan.png).
Modellprüfsummen stimmen mit dem abgeschlossenen Funktionsvergleich überein.

## GUI: Cell → Brain energy

Die Cell-Box enthält zusätzlich **Max. lifespan**: logarithmisch 10–10.000
Simulationssekunden, Startwert weiterhin 10.000. Die Grenze gilt auch für bestehende
Zellen; bereits ältere Zellen sterben bei der nächsten Altersprüfung. Der Regler
ändert weder ihr bisheriges Alter noch die Energie-Todesregel. Strategien mit eigener
Sterberegel (etwa HIGH_POSITION3 mit fester Mitgliedschaft) behalten diese Regel.
Ideen zur Wiederverwertung: [Überreste und Ressourcenkreisläufe](RESSOURCENKREISLAUF.md).

Neben Environment befindet sich unten die neue Box **Cell**. Ihr Regler skaliert
die bisherigen Speicher- und Rechengebühren gemeinsam:

| Einstellung | Wirkung |
|---|---|
| 0× | Keine Gehirngebühren |
| 1× | Bisheriger Standard; Startwert |
| 10× / 100× / 1000× | Entsprechend höhere Gehirngebühren |

Die Skala verläuft nahe null linear, danach logarithmisch, damit sowohl kleine als
auch große Faktoren erreichbar sind. Der Grundumsatz einer Zelle bleibt unverändert.
Die Simulation übernimmt pro Schritt einen unveränderlichen Gebührenstand für alle
Zellen. Ein während des Schritts bewegter Regler beeinflusst erst den nächsten Schritt.
Explizite Kostenvarianten einer Teststrategie haben Vorrang vor dem GUI-Regler.
Einstellungen gelten für die laufende Anwendung; sie werden nicht mit `.cells` gespeichert.

Für den gewünschten Versuch lassen sich somit Sonnenangebot und Gehirngebühren
unabhängig erhöhen. Das garantiert keine bestimmte Populationsgröße: Kleine Gehirne
oder günstige statische Steuerungen können weiterhin hohe Dichten ermöglichen.
Die Laufzeitoptimierung spart CPU, ohne automatisch das energetische Wachstum zu
fördern; der zusätzliche Selektionsdruck entsteht erst durch den bewusst geänderten
Kostenregler.

## Wiederholen

```powershell
mvn test '-Dtest=NetworkExecutionPlanTest,ExecutionPlanWorldTest,LiveBrainEnergyTest,MainControllerLayoutTest'
mvn test '-Dtest=NetworkExecutionPlanStudyTest' '-Dexecution.plan.study.folder=experiments/function-comparison-stable-2026-10-16' '-Dexecution.plan.study.output=experiments/network-execution-plan-new' '-DargLine=-Xmx2g'
python scripts/analyze_network_execution_plan.py experiments/network-execution-plan-new --models experiments/function-comparison-stable-2026-10-16 --output docs/network-execution-plan/new
```

Neue Ausgabeordner verwenden. Die lokalen Modell-Dateien bleiben im Experimentordner;
das Archiv enthält die Modellprüfsummen und alle Messwerte. `planned` bezeichnet in
den Zeitmessungen die unveränderte DNA mit Ausführungsplan.
