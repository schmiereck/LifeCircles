# HIGH_POSITION3: Halten lernen, dann aufbauen

Start in VS Code mit **Main HIGH_POSITION3**. Alternativ dem bisherigen Java-Start
`--trainMode=HIGH_POSITION3` hinzufügen. Die Statusleiste zeigt Stufe, Generation,
Episodenzeit, letzten Bestwert und aufeinanderfolgende Erfolge.

## Aufgabe und Bedingungen

Ein Kandidat ist ein **Gründergenom für einen ganzen Verband**. Acht Kandidaten
werden gleichzeitig geprüft. Ihre genetisch gleichen Körperzellen haben
unabhängige neuronale Zustände und eigene Sensorwerte. Es werden keine
Klebeverbindungen, Verankerungen oder neuen Kräfte eingeführt.

Die Welt ist 11200 × 1200 groß, mit acht Versuchsbereichen von je 1400 Breite,
ohne Seitenwände, mit normalem Boden. Verlässt ein Verband seinen Bereich samt
Sicherheitsabstand, bekommt er für die Episode null Punkte. Bei Eindringen in
einen anderen Bereich wird auch dessen Versuch ungültig, damit gegenseitige
physikalische Hilfe nicht belohnt wird.

Dies ist ein **Physiktraining mit gesicherter Energie**:

- Vor jedem Schritt wird jede Zelle auf Energie 0,75 aufgefüllt; neue Kinder
  werden nach der Teilung ebenfalls aufgefüllt.
- Sonnenstrahlen und Energieübertragung sind in diesem Modus ausgesetzt.
- Alter und Energiemangel lassen hier keine Zellen sterben.
- Mutation erfolgt zwischen Kandidatengenerationen. Biologische Teilungen sind
  klonal: DNA, Körperfarbe und Mutationsfaktoren bleiben erhalten. Die
  Ausgangsbits für den Zustand der Kinder sind in der Aufbaustufe frei.

Die globalen Sonnen-, Teilungs- und Mutationsparameter werden dafür nicht
umgeschrieben. Diese Bedingungen gelten nur in HIGH_POSITION3 und sind keine
Aussage über die spätere Überlebensfähigkeit eines Organismus in NONE.

## Vier Stufen

Eine Episode dauert **30 Simulationssekunden**; alle 0,5 Sekunden wird bewertet.
Physik und Gehirne laufen weiterhin in jedem Schritt. Darstellungsgeschwindigkeit
und Schrittweite ändern diese Zeiten nicht.

| Stufe | Startkörper | Freie Steuerung | Aufgabe |
|---|---|---|---|
| HOLD – Halten | 60 Zellen, fünf Spalten × zwölf versetzte Reihen, Radius 20 | Kräfte | Höhe und Form über die ganze Episode erhalten |
| RECOVER – Störung ausgleichen | Gleicher Körper | Kräfte | Bei 10 und 20 Sekunden erhalten die fünf obersten Zellen einmal +20 bzw. -20 seitliche Geschwindigkeit |
| SIZE – Größe mitsteuern | Gleicher Körper | Kräfte, Größe, Aktorfarben | Dieselben Störungen ausgleichen, obwohl Größe und Farben nicht mehr vorgegeben sind |
| BUILD – Aufbauen | Fünf Zellen in einer Bodenreihe | Zusätzlich Teilung und Kinderzustände | Früh bodenverbundene Höhe aufbauen und anschließend halten; maximal 60 Zellen je Kandidat |

In den ersten beiden Stufen bleibt der Radius tatsächlich 20: Der Größenwunsch
wird nach dem Denken und vor der Größenintegration begrenzt. Die Kraftausgänge
bleiben in jeder Stufe aktiv. Gesperrte Teilungsausgänge werden vor der
Reproduktion begrenzt. Die Gehirne werden weder eingefroren noch ersetzt.

In BUILD gelten die normalen Bedingungen für Teilungsalter, Teilungswunsch,
Energieaufteilung, Platzierung und Wachstum. Das Zelllimit wird nach jeder Geburt
aktualisiert, sodass gleichzeitig teilungsbereite Eltern es nicht überschreiten.
Eine größere Population allein gewinnt keine zusätzlichen Punkte.
BUILD ist die abschließende Trainingsstufe.

## Bewertung und Stufenwechsel

`H` ist die bodenverbundene Höhe relativ zum Zielkörper, auf 1 begrenzt. Wie in
HIGH_POSITION2 zählt die mittlere obere Kante des höchsten Viertels eines
bodenverbundenen Kontaktverbands. Die Zielhöhe beträgt 386,41. Ein abgelöstes
hochfliegendes Fragment bekommt keinen Höhenbonus.

`S` ist der Anteil bodenverbundener Körperzellen. `C` ist der Anteil erhaltener
ursprünglicher Nachbarpaare; Kontakt gilt bis R1+R2+0,1×min(R1,R2).
`D` ist der RMS-Formfehler der Zellmittelpunkte, geteilt durch den Anfangsradius
20, nach optimaler gemeinsamer Translation und Rotation. Schrumpfen, Scheren
und Zerfließen zählen als Formänderung; eine gemeinsame Bewegung nicht.

In HOLD, RECOVER und SIZE lautet der momentane Wert:

```text
Q = H × S × C / (1 + D²)
Episodenwert = Mittelwert aller 60 Messungen
```

Für den Aufstieg muss der beste Kandidat zusätzlich folgende Bedingungen erfüllen:

- Episodenwert mindestens 0,8.
- In den letzten fünf Sekunden im Mittel mindestens 90 % Zielhöhe,
  98 % bodenverbundene Zellen und 95 % erhaltene ursprüngliche Nachbarpaare.
- Mittlerer Formfehler im gleichen Fenster höchstens 0,5 Radien.
- **Drei Generationen in Folge** müssen erfolgreich sein. Ein Fehlschlag setzt
  den Zähler zurück. Der Wechsel gilt erst für die nächste frische Episode.

In BUILD gibt es keine vorgegebene Endform mit fest zugeordneten 60 Zellen.
Während des Aufbaus zählt `H × S`. In den letzten fünf Sekunden wird zusätzlich
Kontakt- und Formtreue gegenüber der jeweils vorherigen Messung belohnt;
neue Zellen zählen dabei nicht als verschobene alte Zellen. Der Endwert besteht
zu 75 % aus dem Episodenmittel und zu 25 % aus dem Mittel des Schlussfensters.
Damit bringt früher Aufbau mehr Punkte als derselbe späte Endzustand.
Die kurze Formreferenz erlaubt Entwicklung; sie garantiert keine dauerhafte
Formtreue über beliebig lange Zeiträume.

Eine pauschale Geschwindigkeitsstrafe gibt es nicht. Gemeinsame Bewegung und
spätere Gelenkigkeit sollen möglich bleiben.

## Episodenlänge (seit 30.10.2026 einstellbar)

Ein gemeinsamer Regler „Training duration“ im Umweltpanel für alle Trainings (`trainingDurationSeconds`, gespeichert
in `<datei>.cells.settings`). Linkes Ende = 30 s; kürzere Werte hebt HIGH_POSITION3 auf 25 s an. Die Stöße bleiben
bei 10 und 20 s; das Schlussfenster (Form- und Kontakterhalt beim Aufbauen, berichtete Werte) sind immer die letzten 5 s der Episode. Die Länge wird zu Beginn
jeder Episode gelesen. Längere Episoden mitteln über mehr Proben; Werte verschiedener Längen sind nur bedingt
vergleichbar.

## Vererbung und Auswahl

Ausgewählt werden die **Gründergenome der besten ganzen Körper**. Neue Episoden
beginnen mit frischen Gehirnzuständen, Alter null, ruhenden Geschwindigkeiten
und gleicher Startgeometrie.

Ein Elitegenom bleibt unverändert. Die sieben anderen Kandidaten wählen einen
Elternteil aus den besten drei Verbänden und mutieren einmal mit den globalen
Mutationsparametern und vererbten Mutationsfaktoren. Erst anschließend werden
die Körperzellen geklont. Mutation innerhalb eines Körpers verändert dessen
Bewertung während der Episode also nicht.

## Laden und Speichern

**Load** verwendet geladene neuronale Zellgenome als Kandidaten und startet
frisch in HOLD. Pro räumlichem Bereich wird zunächst ein Repräsentant genommen;
wenige Quellen werden auf acht Kandidaten verteilt. **Load & Merge** gibt
importierten Repräsentanten Vorrang und ergänzt aus dem bisherigen Bestand.
Auch dabei beginnt eine frische HOLD-Episode. Eine leere Ersetzungsdatei startet
eine neue Zufallspopulation.

Die bestehende `.cells`-Speicherung enthält Zellgenome und Zellzustände, aber
keinen vollständigen Trainingscheckpoint mit Stufe, Erfolgszähler und
Bewertungsgeschichte. Zum Aufbewahren aller Kandidaten **Save** verwenden.
`Save Best` verwendet weiterhin die räumliche Energieauswahl und ist keine
Auswahl nach dem neuen Verbandswert.

## Durchgeführte Prüfungen am 15.10.2026

**Kontrolllauf über den Anwendungspfad:** Ein unveränderliches neuronales
Kontrollprogramm mit fast maximaler einheitlicher Anziehung, Radiusziel fast 20
und Teilung nach oben erreicht RECOVER nach Generation 3, SIZE nach Generation 6
und BUILD nach Generation 9. Es hält rund 95,77 % der Zielhöhe, alle ursprünglichen
Kontakte und einen Formfehler von 0,282 Radien. In BUILD findet reale Teilung
statt; das Limit von insgesamt 480 Zellen für acht Kandidaten hält.
Der Kontrolllauf verwendet Teilungsalter 6 Sekunden, Mutation 0 und 60 Hz.
Der normale Modus behält das konfigurierte Teilungsalter (standardmäßig 8 Sekunden).

Das vorgegebene Programm baut in BUILD nur **49,82 % der Zielhöhe** auf.
Das ist eine Kontrolle der Aufgaben und Übergänge, kein Evolutionserfolg.

**Zufallsstart mit Mutation:** Fünf echte Generationen bei 60 Hz, Mutationsrate
0,1 und Stärke 0,2, Netzinitialisierung mit Seed 574. Alle Positionen bleiben
endlich; die Episoden bleiben in HOLD. Der Bestwert steigt von 0,049999 auf
0,053749. Die späte Höhe liegt bei etwa 34–37 % des Ziels, der Formfehler bei
etwa neun Radien. Das zeigt funktionsfähige Mutation und Auswahl, noch keine
gelernte Stabilität. Elternauswahl und Meta-Mutation sind nicht vollständig
deterministisch; Konvergenz braucht längere Läufe und unabhängige Wiederholungen.

Rohdaten: [Kontrolllauf](docs/high-position3/control-60.csv),
[fünf Zufallsgenerationen](docs/high-position3/random-5-generations.csv).
Start- und Folgegenome liegen lokal unter `experiments/high-position3-2026-10-15/`
und bleiben von `mvn clean` erhalten.

**Prüfsumme:** 365 Routineprüfungen plus ein Kontrolllauf, insgesamt 366
erfolgreich. Darin enthalten: elf neue Strategie- und fünf Bewertungsprüfungen,
Importablehnung und die bisherigen Physik-, Netzwerk-, Vererbungs-, Lade- und
Snapshotregressionen. Der Zufallslauf wurde separat geprüft.

```powershell
# Neue Routineprüfungen, ohne langen Kontroll-/Evolutionslauf
mvn test '-Dtest=HighPosition3TrainStrategyTest,StructureEpisodeFitnessTest'

# Vollständiger Kontrolllauf über alle Stufen
mvn test '-Dtest=HighPosition3IntegrationStudyTest#knownStableProgramPassesCurriculumAndBuildRemainsBounded' '-Dhigh3.study=true' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'

# Zufallsstart, standardmäßig fünf Generationen
mvn test '-Dtest=HighPosition3IntegrationStudyTest#randomNetworksExecuteSelectionAndMutation' '-Dhigh3.study=true' '-Dhigh3.generations=5' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```

Die optionalen Versuche laufen nicht mit den Routineprüfungen.
Grundlage: [isolierte Steuerungsproben](STEUERUNGSPROBEN.md).
