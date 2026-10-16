# Funktionsvergleich der NONE-Genome

## Fragestellung

Bringt die laufende situationsabhängige Steuerung einen Vorteil gegenüber festen
Ausgaben, wenn beide Varianten dieselbe DNA und denselben Zellzustand verwenden?
Der Vergleich untersucht einen Beitrag zur Energieversorgung, Entwicklung und
Fortpflanzung. Er ist keine Messung abstrakter Aufgabenleistung wie XOR.

## Auswahl und Aufbau

- Aus jedem der neun langen Läufe werden die Speicherstände bei 1000 und 10.000
  Simulationssekunden untersucht: 18 Modelle. Pro Speicherstand wird deterministisch
  die mittlere Zelle nach gespeicherter Synapsenzahl gewählt, Dateireihenfolge als
  Gleichstandsregel. Die Auswahl verwendet keine gemessene spätere Leistung.
- Je Modell acht neue klonale Gründer in einer 600 × 400 großen periodischen Welt
  mit Boden, Energie 0,75, Alter 9 und Radius 10. Ererbte Zellfarbe und anfänglicher
  Zellzustand stammen von der ausgewählten Zelle. Laufende Netzaktivierungen und
  Gedächtnis beginnen leer. Die Modelle werden separat gespeichert.
- Natürliche Teilung, Sonne, Tod und physikalische Wechselwirkungen sind aktiv.
  Geburten sind klonal: keine Genom-, Farb- oder Meta-Mutation im Versuchszeitraum.
  Kein Training, kein Nachfüllen, keine Wiederbesiedlung.

## Feste Ausgaben

Jedes Modell läuft zuerst 120 Simulationssekunden mit seinen ursprünglichen
Gehirnpreisen in einer separaten Kalibrierung. Die mittleren rohen Ausgaben
werden je Zellzustand (0–7) bestimmt. Es gibt damit acht mögliche feste
Ausgabemuster; unterschiedliche Zelltypen bei Geburt bleiben möglich.

Für nicht beobachtete Zustände werden 64 kanonische Entscheidungen eines neuen
Gründers mit diesem Zustand verwendet. Deren Befehle sind eine Ersatzvorgabe,
keine im Kalibrierlauf beobachteten Mittelwerte. Samplezahlen und Streuungen
werden in `profiles.csv` dokumentiert.

Im Vergleich laufen die Gehirne in beiden Varianten normal weiter. In `frozen`
werden direkt nach dem Denken sämtliche Ausgaben durch das feste Muster des
aktuellen Zellzustands ersetzt. Das gilt auch für Kinder vor ihrer Aufnahme in
die Welt. Größe, Farbe, Kraft, Energieübertragung, Teilungswunsch, Energieanteil
und Kinderzustand sind damit festgelegt. Der neuronale Schattenlauf verbraucht
weiterhin Rechenzeit und wird bei aktiven Gehirngebühren regulär abgerechnet.

## Vergleichsläufe

Pro Genom:

- `live` und `frozen` mit den Gebühren seines Herkunftslaufs;
- beide Varianten zusätzlich mit kostenlosen Gehirnen;
- zwei Sonnen-Zufallsfolgen je Variante, unabhängig von der Kalibrierung;
- je 120 Simulationssekunden (ein vollständiger Tag-/Nachtzyklus).

Das ergibt **144 Vergleichsläufe plus 18 Kalibrierungen**. Die Reihenfolge von
`live` und `frozen` rotiert. Sonne und Anfangsbedingungen sind innerhalb eines
Paares identisch. Physikalische Verläufe dürfen infolge der Steuerung abweichen.
Kostenfreie Paare untersuchen den Steuerungsbeitrag ohne Unterschiede in den
angeforderten Gehirngebühren. Paare mit Herkunftspreisen prüfen zusätzlich
den Nettoeffekt im jeweiligen Gebührenmodell.

Primäre Messgrößen sind integrierte lebende Zellzahl (Zellsekunden) und Geburten
über dieselbe Dauer. Hinzu kommen integrierte Gesamtenergie, Todesfälle,
Endzustände, Ausführungszahlen und Abweichung der berechneten Schattenausgaben
vom festen Muster. Bei Aussterben bleibt der Rest des geplanten Zeitfensters
ohne lebende Zellen. Bei 2000 Zellen wird ein Lauf als zensiert beendet.
Endzellzahl oder reine Geburtenzahl allein sind kein umfassender Fitnesswert.

## Prüfungen und Grenzen

28 gezielte Kontrollen bestanden: drei neue Funktionskontrollen sowie bestehende
Steuerungs-, Gebühren- und Populationsprüfungen. Sie sichern gleiche Energie und
unabhängige Netzzustände der Gründer, Zustandsmuster für Kinder, weiterlaufende
Neuronenausführung und eine negative Kontrolle mit tatsächlich konstantem Netz ab.

Die festen Muster sind Beispiele statischer Steuerung. Sie wurden nicht auf
maximale Leistung optimiert; ein Vorteil des normalen Netzes beweist keinen
Vorteil gegenüber jeder möglichen statischen Steuerung. Kalibrierung und
kostenfreie Kontrolle verwenden unterschiedliche Gebühren; im kostenfreien
Versuch wird das zuvor gelernte Muster beibehalten. Eine Reaktion auf andere
Ressourcenverteilungen, längere Lebenszyklen oder fremde Genotypen wird nicht
vollständig erfasst. Ein Vertreter je Speicherstand bildet die genetische
Vielfalt innerhalb einer Gründerlinie nicht vollständig ab.

Nebenbei laufende GUI-Prozesse können CPU-Zeiten verändern. Die biologischen
Vergleiche verwenden feste Simulationsschritte und separate JVMs. Aus diesen
Laufzeiten wird kein Performance-Ranking abgeleitet.

## Ausführen

```powershell
mvn test '-Dtest=FunctionComparisonStudyTest' '-Dfunction.study.folder=experiments/none-energy-long-2026-10-16' '-Dfunction.study.output=experiments/function-comparison' '-DargLine=-Xmx2g -Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```

Eigener neuer Ausgabeordner; die CSV-Dateien werden bei erneutem Start überschrieben.
Parameter für kurze Proben: `function.study.steps`, `function.study.calibration`,
`function.study.models`, `function.study.repeats`. Ergebnisse: `results.csv`,
Verlauf: `progress.csv`, feste Muster: `profiles.csv`, ausgewählte Modelle:
`model-N.cells`. Modelldateien enthalten jeweils eine Zelle in einer Liste.

## GUI parallel beobachten

In VS Code **Main NONE** auswählen. Dieses Profil übergibt `--trainMode=NONE`
und verwendet vier Common-Pool-Worker. Die aktuelle lokale Konfigurationsvorgabe
HIGH_POSITION3 bleibt erhalten; Main DEFAULT ohne Argument verwendet diese Vorgabe.

Die implementierten Physik-, Vererbungs-, Netz- und Performancekorrekturen gelten
auch in der GUI. Standard-Gehirnpreise bleiben 1/1, Sonnenenergie bleibt 0,01 je
Strahl, normale Teilung mutiert mit Multiplikator 1. Die optionale
Eingabenormalisierung bleibt aus. Denken erfolgt weiter in jedem 60-Hz-Physikschritt.
Die Gebührenvarianten und das Einfrieren dieses Tests ändern keine GUI-Parameter.
NONE startet mit zufälligen Gründern; ein importierter Zellstand verändert den
genetischen Ausgangspunkt und damit die Vergleichbarkeit.
