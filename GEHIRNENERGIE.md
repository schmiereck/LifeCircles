# Gehirnenergie: Messung und Versuchsplan

## Ausgangspunkt: gespeicherter HIGH_POSITION3-Lauf

Datei: `test-highPos3-2026-10-15-a01.cells`, 480 Zellen, 31.312.116 Bytes.
Beobachtung des Nutzers: G128 etwa 0,239, G463 etwa 0,255 bei rund 840.000 Schritten.
Diese beiden Werte belegen keine Lernkurve des ganzen Laufs; weitere Generationen
oder unabhängige Wiederholungen wurden nicht nachträglich rekonstruiert.

Die Datei wurde ausschließlich gelesen. `BrainEnergyAuditTest` setzt den geladenen
Stand lokal für 1200 Schritte bei 60 Hz fort: gleiche Weltgröße, HOLD-Bedingungen,
Energie 0,75, keine Teilung, Sterblichkeit, Sonne, Auswahl oder Mutation.
Ausführungszähler sind transient und werden deshalb neu gemessen. Die Fortsetzung
ist keine exakte Wiederholung des ursprünglichen Laufs.

| Größe | Ergebnis |
|---|---:|
| Gespeicherte Synapsen pro Zelle, einschließlich Moduldefinitionen | Mittel 753,5; Bereich 750–762 |
| Ausgeführte Synapsen pro Denkaufruf | Mittel etwa 431,16 |
| Neuronen im äußeren Netz, einschließlich Ein-/Ausgaben | Mittel 643,5; ohne Modulinneres |
| Layer im äußeren Netz | 5 |
| Gehirnverbrauch bei 60 Denkaufrufen/s | etwa 0,000518 Energie/s |
| Grundverbrauch | 0,003 Energie/s |

Die aktuelle Formel in `EnergyCellCalcService` lautet:

```text
Kosten je Schritt = S × 3e-10 × dt + A × 2e-8
S = gespeicherte Synapsen, A = in diesem Denkaufruf ausgeführte Synapsen
```

Damit kostet die Ausführung hier etwa **17,25 % des Grundverbrauchs**, die
Speicherung nur **0,0075 %**. Inaktive Struktur ist fast kostenlos. Die
Zählung enthält innere Modulverbindungen, aber keine eigenen Kosten für
Neuronenaktivierung, Portverwaltung, Nullsetzen inaktiver Layer oder
Modultopologieprüfungen. Synapsenzahl allein ist deshalb kein vollständiger
CPU-Kostenmesser. Aus dieser einen Datei folgt auch nicht, dass die Netze im
langen Lauf gewachsen sind; dazu fehlen vergleichbare ältere Stände.

Nach zwei Sekunden Aufwärmphase: Denkphase einschließlich Ausgabeanwendung und
Trainingsbegrenzung rund 5,59 ms je Update, insgesamt
7,63 ms in den erfassten Phasen, also etwa **73 %**. Rasteraufbau, Prüfbuchhaltung,
Darstellung, Snapshots und Generationswechsel sind dabei nicht erfasst.
Das ist eine kurze Einzelmessung ohne Wiederholungsserie oder belastbare
Beschleunigungsprognose. Rohdaten: [Zellen](docs/brain-energy/cells.csv),
[Phasenzeiten](docs/brain-energy/timings.csv).

## Zwei wichtige Grenzen

**HIGH_POSITION3 belohnt bisher keine Energieeffizienz.** Die Energie wird vor
jedem Schritt aufgefüllt, Sterblichkeit ist ausgesetzt, und die Auswahl bewertet
Strukturleistung. Größere Kostenkonstanten allein würden dort keinen verlässlichen
Selektionsdruck erzeugen. Für dieses Training muss Energieaufwand ausdrücklich
in die Auswahl eingehen; ein normaler Ressourcenversuch braucht eine endliche
Energieversorgung.

**Ein statisches Programm kann für die Halteaufgabe sinnvoll sein.** Der bereits
geprüfte konstante Anziehungscontroller trägt den Verband. Eine allgemeine
Strafe für konstante Ausgänge oder eine Mindestnetzgröße würde unnötige Arbeit
erzwingen. Zusätzliche Rechenleistung soll dort gewinnen, wo sensorabhängiges
Handeln oder Gedächtnis einen echten Vorteil liefert.

## Vorhandene Aufgabenexperimente

`NeuralNetworkTest`, `NeuralNetworkValuesTest` und `NeuralNetworkNeuronTest`
enthalten OR, AND, NAND, XOR und Bitcount. Sie bewerten Ausgabefehler, teilweise
mit eigenen Mutations-/Kreuzungsroutinen, bis zu 1000 Kandidaten und 500.000
Generationen. Layer-Deaktivierung wird in diesen Versuchen abgeschaltet.
`MemoryNeuralNetworkTest` trainiert Symbolfolgen; sein einfacher Einstieg
verwendet zusätzlich Backpropagation. Das sind nützliche Aufgabenquellen, aber
kein gemeinsamer Test der heutigen Zellmutation und eines Energiehaushalts.

## Vorgeschlagene Untersuchung

### 1. Erst Leistungsfähigkeit ohne Kostendruck feststellen

Ein kleiner, begrenzter Versuch mit derselben `NeuralNetwork.mutate()`-Logik wie
bei Zellen. Kein Backpropagation oder separates Crossover im Evolutionsvergleich.
Ein Backpropagation-Vergleich kann separat die Darstellbarkeit einer Aufgabe
prüfen, erklärt aber nicht, ob Mutation sie entdeckt.

| Aufgabe | Geprüfte Funktion | Kontrolle gegen Abkürzungen |
|---|---|---|
| Konstanter Zielwert | Billige statische Steuerung darf gewinnen | Erwarteter Kontrollfall für geringe Komplexität |
| AND / OR / XOR | Abhängigkeit von aktuellen Eingaben | Vollständige Wahrheitstabellen, zufällige Reihenfolge; Gedächtnis vor unabhängigen Fällen löschen |
| Verzögerte Kopie | Ausgabe der vorherigen Eingabe | Zufällige, ausgewogene Folgen; keine Eingabe des letzten Zielwerts |
| COUNTER mit RESET und TICK | Ereignisse zählen, pausieren und zurücksetzen | Zufällige Ticks/Pausen/Resets; kein Alters-, Schritt- oder Phaseneingang |

Statische und gedächtnislose Referenzen mitmessen. Bei AND/OR erreicht ein
konstanter Klassifikator bereits 75 % Genauigkeit auf der vollständigen Tabelle;
das darf nicht als gelöste Aufgabe gelten. Trainings- und Prüfsequenzen trennen.
Gedächtnis zwischen Episoden zurücksetzen, innerhalb einer Folge erhalten.
Wird eine Aufgabe schon ohne Kosten nicht entdeckt, kann ihre Ausfallrate keine
zu hohen Energiekosten belegen.

### 2. Kosten gegen den funktionellen Gewinn kalibrieren

Die Belohnung wird kontinuierlich aus dem Fehler abgeleitet und in Energie pro
Simulationssekunde ausgedrückt. Belohnung und Speicherunterhalt skalieren mit
Zeit, ausgeführte Arbeit mit tatsächlichen Entscheidungen. Eine längere
Prüfsequenz darf allein kein größeres Energiebudget erzeugen.

Speicher- und Ausführungskosten getrennt variieren. Als grobes erstes Raster
bieten sich Speicherfaktoren **0, 1, 100, 1000** und Ausführungsfaktoren
**0, 0,25, 1, 4** gegenüber den aktuellen Konstanten an. Faktor 1000 beim
Speicherunterhalt entspräche für den gespeicherten Stand erst etwa 7,5 % des
Grundverbrauchs; die große Zahl allein bedeutet also keine extreme Belastung.
Diese Werte sind Suchpunkte, keine empfohlenen neuen Standardwerte.

Gleiche Startpopulationen und Aufgabenfolgen für die Varianten; Zufallsquellen
einschließlich Auswahl und Meta-Mutation kontrollieren. Erst grob suchen, dann
vielversprechende Bereiche mit mindestens zehn unabhängigen Seeds vergleichen.
Die großen bestehenden Testschleifen nicht einfach vervielfachen: Population,
Generationszahl und Laufzeit begrenzen und auch gescheiterte Läufe auswerten.

Zwei Fragestellungen getrennt beantworten:

- **Erlernen:** Entsteht eine Lösung aus einer frischen Population trotz Kosten?
- **Verdichten:** Kann eine bereits gefundene Lösung günstiger werden und ihre
  Leistung auf unbekannten Folgen erhalten?

Messen: Erfolgsquote, Zeit bis zur Lösung, Prüfleistung, Speichergröße,
ausgeführte Synapsen, aktive Neuronen/Ports/Modulaufrufe und Energieaufwand.
Laufzeiten nach Aufwärmen zusätzlich messen; Hardwarezeit nicht direkt zur
evolutionären Energiewährung machen. Ergebnisse als Kurve von Leistung gegen
Aufwand darstellen. Gesucht ist ein Bereich, der einfache Netze verkleinert
und die Erfolgsrate bei anspruchsvolleren Aufgaben weitgehend erhält.

### 3. In die Zellwelt übertragen

Kleine Logiknetze haben andere Ein-/Ausgabemengen als Zellgehirne. Deshalb eine
zweite Probe im Zellformat und anschließend echte Ressourcenwelten durchführen:
Veränderliche Licht-/Nahrungssituation, günstige und ungünstige Handlungsoptionen,
gleiche äußere Energiezufuhr zwischen Kostenvarianten. Gemessen werden zusätzlicher
Energiegewinn, Rechenverbrauch, Reproduktion und Überleben. Mehr Denkleistung muss
sich durch einen besseren Nettoertrag bezahlen können.

Für HIGH_POSITION3 eignet sich zunächst eine Leistung-/Aufwand-Auswahl mit
geschütztem Leistungseliten oder einer Pareto-Auswahl. Die Kriterien für den
Stufenaufstieg bleiben auf der unveränderten physischen Leistung. Ein günstigeres
Netz darf einen schlechteren Körper nicht zum erfolgreichen Stufenwechsel machen.
Das wäre ein gesonderter Effizienzversuch, keine Umstellung der regulären
Energieversorgung.

Wenn Synapsenzahlen die gemessene Laufzeit schlecht erklären, eine deterministische
Kostenkomponente für zusätzliche Neuronen/Ports/Modulaufrufe ergänzen und erneut
kalibrieren. Vorher die Konstanten nicht pauschal verschärfen.

## Reproduktion der Bestandsmessung

```powershell
mvn test '-Dtest=BrainEnergyAuditTest' '-Dbrain.audit.file=C:\Pfad\stand.cells' '-Dbrain.audit.output=target/brain-energy-audit' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```

Optional `-Dbrain.audit.steps=1200`. Der Test ist ausdrücklich optional und nimmt
eine HIGH_POSITION3-HOLD-Welt an. Die Produktionslogik und Energiekonstanten wurden
für diese Untersuchung nicht geändert. Der Audit-Test bestand mit dem genannten
Zellstand. Die anschließenden Kalibrierungsserien sind abgeschlossen:
[Ergebnisse der Kalibrierung](GEHIRN_ERGEBNISSE.md).
