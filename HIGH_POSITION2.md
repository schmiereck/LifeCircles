# HIGH_POSITION2: anhaltende Hoehe bodenverbundener Verbaende

## Was bodenverbunden hier bedeutet

1. Mindestens eine lebende Zelle liegt auf der Oberseite eines Blockers vom Typ
   `GROUND`. Ein Spalt bzw. eine Ueberlappung von maximal 10 % ihres Radius wird
   toleriert. Ihre Mitte muss oberhalb dieser Bodenoberflaeche liegen.
2. Weitere lebende Zellen gehoeren dazu, wenn eine Kette naher Zellkontakte zu
   dieser Basis fuehrt. Zwei Mittelpunkte duerfen hoechstens die Summe ihrer Radien
   plus 10 % des kleineren Radius auseinanderliegen. Beispiel: Bei zweimal Radius
   10 sind maximal 21 Welteinheiten erlaubt, also ein Oberflaechenspalt von 1.
3. Alle beteiligten Zellen liegen in derselben Trainingsbox. Ein Kontakt durch die
   periodische obere/untere Weltgrenze oder zwischen Boxen zaehlt nicht. Seitenwaende
   und Plattformen gelten nicht als Boden.

Das ist ein geometrisches Kriterium. Es fordert weder bestimmte Farben noch
Anziehungsausgaben; auch ein durch Druck tragender Verband soll moeglich sein.
Es erzeugt keine Bindungen und weist nicht nach, welche Zelle welche Last traegt.
Eine kurzzeitige Druckkette kann ebenfalls bodenverbunden sein. Deshalb wird die
erreichte Hoehe zusaetzlich ueber die Zeit bewertet.

## Generationslänge (seit 30.10.2026 einstellbar)

Ein gemeinsamer Regler „Training duration“ im Umweltpanel für alle Trainings (`trainingDurationSeconds`, gespeichert
in `<datei>.cells.settings`). Linkes Ende = 50 s (3.000 Schritte); kürzere Werte hebt HIGH_POSITION2 auf 15 s an.
Bewertet werden weiter die letzten 10 s. Die Länge wird zu Beginn jeder Generation gelesen; Studien mit fester Schrittzahl (`HighPosition2EvolutionStudyTest`)
bleiben davon unberührt.

## Fußzeile

Seit 29.10.2026 zeigt die Fußzeile wie bei RACE und HIGH_POSITION3:
`HIGH_POSITION2 · G<Generation> · Schritt <n>/<Schritte> · <s> s · Wachstum|Bewertung · <lebende Zellen> · letzte Bestwertung <Wert>`.
Die Bestwertung ist der dauerhafte Wert (unteres Quartil, 0–1 der Bodenhöhe) der ersten Siegerzelle.
HIGH_POSITION zeigt Generation, Schritt/10000, Zeit, lebende Zellen, die aktuell höchste Oberkante und die des
letzten Siegers in px über dem Boden. Die Zähler beginnen bei „New…“ neu und stehen nicht in Speicherständen.

## Bewertung

Der Generationswechsel bleibt bei 3.000 Physikschritten, derzeit 50 Simulationssekunden.
In den letzten 600 Schritten werden alle 30 Schritte Stichproben genommen: 20 Werte
waehrend der letzten zehn Sekunden, unabhaengig von der Geschwindigkeit der Darstellung.

Pro zusammenhaengendem, bodenverbundenem Verband wird die mittlere Oberkantenhoehe
seines obersten Viertels berechnet, bei kleinen Verbaenden mindestens einer Zelle.
Die Hoehe wird durch den Abstand seiner Bodenoberflaeche zum oberen Weltrand geteilt.
Die Skala haengt damit nicht mehr vom niedrigsten oder hoechsten Ausreisser der
Population ab. Alle Mitglieder bekommen denselben momentanen Verbandswert.

Jede Zelle besitzt fuer das Bewertungsfenster eine eigene kurze Historie. Fehlende
Werte, fehlender Bodenkontakt oder eine noch nicht geborene Zelle zaehlen als null.
Als Fitness dient der fuenftkleinste der 20 Werte. Eine Hoehe muss damit in mindestens
16 Stichproben erreicht sein, um diesen Wert zu liefern. Ein einzelner hoher Wert
oder wenige spitze Ausschlaege erhoehen ihn nicht. Gestorbene Zellen werden entfernt;
die Historien und der Rasterinhalt werden nach jeder Selektionsrunde freigegeben.

Bei der endgueltigen Auswahl kommen ausschliesslich aktuell bodenverbundene Zellen
infrage, sofern mindestens ein solcher Verband vorhanden ist. Bei gleicher zeitlicher
Fitness werden Basiszellen, dann aeltere Mitglieder, dann zentralere Zellen bevorzugt.
Damit koennen auch die Gene der Basis vererbt werden. Die Auswahl bleibt genetisch
eine Auswahl einzelner Zellen, keine gemeinsame Vererbung einer Mischung verschiedener
Genome.

Existiert noch kein bodenverbundener Verband, erfolgt eine Startauswahl ohne
Hoehenbonus: Alter, dann Abstand zur Boxmitte. Dadurch bleibt eine Trainingsgeneration
moeglich, aber eine schwebende Spitze erhaelt keinen Hoehenvorteil. Es bleiben maximal
zwei Gewinner je Box, maximal 16 Elternkandidaten, drei unveraenderte Elitekopien und
13 Mutationskandidaten. Alle beginnen weiterhin als frische Entwicklungsversuche.

Der Kontaktraster wird zwischen Stichproben wiederverwendet. Seine Aufloesung ist
begrenzt, damit eine Population winziger Neugeborener kein riesiges Raster erzeugt.
Die zusaetzliche Auswertung laeuft nur im Schlussfenster, nicht bei jedem Physikschritt.

## Nachweise und Grenzen

`GroundedColonyFitnessTest` prueft 16 Faelle: Kontaktketten, abgeloeste Spitzen,
kurzzeitig hohe aber noch verbundene Strukturen, Verlust der Basis, Kontaktabstaende,
Bodentoleranz, andere Bodengeometrie, Wand/Plattform, Box- und Weltgrenzen,
unveraenderte Skala bei Ausreissern, spaet geborene Zellen, Startauswahl, Reset und
tote Basiszellen. Eine 30-s-Physikwelt mit 68 Zellen vergleicht die tragende grosse
Packung mit der zusammenfallenden schmalen Kette.

`TrainingSelectionTest` prueft ausserdem den echten Generationswechsel: Gene eines
bodenverbundenen Turms werden statt denen einer abgeloesten hohen Zelle vererbt.
Zusammen mit Trainings-, Stand-, Mechanik- und Mutationsfaktorregressionen bestanden
69 gezielte Tests.

Das ist ein gezielter Selektionsdruck, kein Nachweis, dass dadurch bereits interessante
Verbaende evolvieren. Langsame Umordnung bei gleicher Hoehe, Kontakte ohne Kraftuebertragung,
mechanische Zusammenarbeit verschiedener Genome und langfristige Stabilitaet nach
dem Bewertungsfenster bleiben Grenzen. Sonnenenergie, Physik und Mutationsparameter
wurden fuer diese Aenderung nicht angepasst.

```powershell
mvn test '-Dtest=GroundedColonyFitnessTest,TrainingSelectionTest,TrainingGenerationTest,StandingStructureTest,MechanicsRegressionTest,MutationFactorsTest' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```


## Ausgefuehrte Evolutionslaeufe

90 Trainingsgenerationen und vier Nachversuche sind in [EVOLUTIONSLAUF.md](EVOLUTIONSLAUF.md)
ausgewertet. Die Ergebnisse zeigen hoehere Matten und eine erhebliche Beruhigung
nach dem Unterbinden weiterer Teilung; schlanke Tuerme sind noch nicht entstanden.
