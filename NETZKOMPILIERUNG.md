# Kompilierte Vorwärtsrechnung der Gehirne

Umgesetzt am 24.10.2026. Vorgänger: [DNA-erhaltender Ausführungsplan](NETZAUSFUEHRUNG.md).

## Idee

Die Objektausführung (`Neuron.activate`) läuft pro Synapse über verstreute
`Synapse`- und `Neuron`-Objekte und liest jeden Wert über die `NeuronValueFunction`.
Bei ganzen Populationen passt das nicht in den Cache: rund 52 ns pro Synapse,
speicherlatenzgebunden. `CompiledForward` legt für ein Netz Quell-IDs, Gewichte,
Bias und Aktivierungsfunktion in flache Arrays und rechnet in **derselben
Reihenfolge** wie `Neuron.activate`. Werte, Eingangssummen, Gedächtnis, Layer-Takt und
die abgerechnete Synapsenarbeit bleiben bitgenau gleich.

- Gilt nur für Netze aus skalaren `Neuron`en (ein Ein-/Ausgang, nur eigene Kanten auf
  Port 0). Netze mit Modul-Neuronen oder Mehrport-Verdrahtung bleiben auf dem
  Objektpfad mit Ausführungsplan.
- Trainingsaufzeichnung (Backprop, nur Tests) nutzt immer den Objektpfad.
- Gültigkeit: Gewichts-, Bias-, Aktivierungs- und Verdrahtungsänderungen über
  Methoden erhöhen einen Gen-Stempel des Neurons; ersetzte Arrays (Eingänge, Ausgänge,
  Layer, Layer-Neuronen, Synapsen) werden per Referenz erkannt. Dann wird neu kompiliert.
  Es wird nichts übersprungen, daher braucht eine Umverdrahtung keinen Gedächtnisreset.
- **Regel (seit 24.10.2026):** Arrays, die Netz, Layer und Neuronen über Getter herausgeben,
  werden nicht an Ort und Stelle beschrieben, sondern über die Methoden ersetzt. Der
  Produktivcode hält das ein (geprüft: alle Schreibzugriffe gehen in frisch angelegte Arrays).
  Vorher verglich die Prüfung in jedem Schritt die komplette Verdrahtung mit Kopien; das war
  etwa 60 % des kompilierten Durchlaufs. `-Dlife.verifyCompiled=true` bzw.
  `CompiledForward.setVerifyContents(true)` schaltet diesen Inhaltsvergleich wieder ein; die
  Tests, die absichtlich direkt schreiben, laufen damit. Gilt ebenso für den
  Finanzierungszensus (`NeuralNet.getMaximumProcessedSynapses`/`getStoredSynapseCount`).
- Ausgänge liest der kompilierte Pfad direkt aus dem Wertearray (gleiche Werte).
- Kompilierte Netze verzichten auf den Ausführungsplan: Seine Prüfung kostete mehr,
  als das Überspringen unerreichbarer Neuronen sparte.

## Messung

**Population einzeln** (`BrainPopulationBenchmarkTest`, gespeicherte WORLD2-Population
`prof-world2.cells`: 1117 Gehirne, je 1249 Synapsen, 4 versteckte Layer, keine Module;
ein Thread, 200 gemessene Runden, wechselnde Reihenfolge):

| Variante | ms/Runde |
| --- | ---: |
| Objektpfad | 64–73 |
| Objektpfad + Ausführungsplan | 68–78 |
| kompiliert | 30,5 |
| kompiliert + Plan | 45,7 |

Alle Varianten bitgleich in Ausgaben und Arbeit über 150 geprüfte Runden.

**WORLD2-Lauf** (`TrainModeStudyTest`, Seed 4711, 300 s Simulationszeit, `-Dlife.profile=true`,
parallele Gehirne, unbelasteter Rechner; alter Stand 8450ca4 in eigenem Worktree):

| Phase (ms/Schritt) | vorher | nachher |
| --- | ---: | ---: |
| BRAINS | 9,56 | 4,49 |
| INTERACTIONS | 8,95 | 9,12 |
| TRANSFER | 3,93 | 4,34 |
| INTEGRATION | 3,62 | 3,25 |
| BRAIN_FUNDING | 1,49 | 1,72 |
| Summe aller Phasen | 29,0 | 24,4 |

Grenzen: Die Studie ist trotz Seed nicht deterministisch (parallele Ausführung);
am Ende lebten 1014 (vorher) bzw. 1163 (nachher) Zellen. Die Gehirnphase ist trotz
größerer Population um den Faktor 2,1 schneller. Eine frühere Messung
(45,5 ms/Schritt) lief parallel zu einer GUI-Simulation und ist nicht vergleichbar.
Größter verbleibender Block: Zell-Wechselwirkungen (INTERACTIONS).

**Ohne Inhaltsvergleich** (24.10.2026, gleicher Benchmark, Population `prof-world2-funding.cells`
mit 1099 Gehirnen, 200 gemessene Runden):

| Variante | ms/Runde |
| --- | ---: |
| Objektpfad | 40 |
| kompiliert mit Inhaltsvergleich (`-Dlife.verifyCompiled=true`) | 15,6 |
| kompiliert, Referenz- und Stempelprüfung | 8,0 |

## Wiederholen

```powershell
mvn test '-Dtest=CompiledForwardTest,NetworkExecutionPlanTest,ExecutionPlanWorldTest,PerformanceEquivalenceTest'
mvn test '-Dtest=BrainPopulationBenchmarkTest' '-Dbrain.bench.cells=target/trainmode-study/prof-world2.cells'
mvn surefire:test '-Dtest=TrainModeStudyTest' '-Dtrainmode.study.mode=WORLD2' '-Dtrainmode.study.seconds=300' '-Dtrainmode.study.seed=4711' '-Dtrainmode.study.name=prof-world2' '-Dlife.profile=true'
```

`CompiledForwardTest` vergleicht kompiliert gegen Objektpfad bitgenau unter
Gewichts-, Bias- und Aktivierungsänderungen, Kantentausch im Array, neuen Kanten,
Strukturmutation, Layer-Steuerung, Modulen und Neuronentausch im Layer-Array.
`ExecutionPlanWorldTest` vergleicht eine ganze Welt (Bahnen, Energie, Arbeit) gegen
den reinen Objektpfad.
