# Physik und Vererbung: kleine Testwelten

Die Tests laufen ohne JavaFX-Fenster. Physikwelten verwenden konstante Gehirnausgaben,
keine Sonnenstrahlen und keine automatische Wiederbesiedlung. Zufallszahlen fuer
Genetiktests sind fest vorgegeben; globale Einstellungen werden danach wiederhergestellt.

## Ausfuehren

Java 25 und Maven:

```powershell
mvn test '-Dtest=TrainingGenerationTest,NestedInheritanceTest,MultiPortNeuronTest,BackpropagateTest,CoincidentCellCentersTest,PhysicsRegressionTest,InheritanceRegressionTest,CellSensorVisibilityTest,SerialTest#testNeuronSynapseConnectionsAfterDeserialization,Cell_writeObject_is_called_Test'
```

Die Auswahl umfasst 86 Tests. Die evolutionaeren Langzeitexperimente der anderen Testklassen
gehoeren nicht zu diesem gezielten Lauf.

## Physikwelten

Quelle: `src/test/java/de/lifecircles/service/PhysicsRegressionTest.java`.

| Welt | Geprueftes Verhalten |
| --- | --- |
| Zwei Zellen am selben Mittelpunkt, 60 Schritte | Zellen trennen sich, Positionen und Geschwindigkeiten bleiben endlich. |
| Zwei Zellen innen und an der periodischen Naht | Gleiche Abstoßung, Feldkraefte, Drehmomente, Sensorentfernungen und Energieuebertragung. |
| Rasterwelten mit Breiten 15, 25, 45, 105 und 300 | Alle nahen Paare werden gefunden, jeder Nachbar erscheint nur einmal. |
| Ein sendender Aktor | Negative Feldstaerke zieht an, positive stoesst ab; Gesamtimpuls und Gesamtdrehmoment bleiben erhalten. |
| 36 dicht stehende Zellen, elf Wiederholungen | Parallele Berechnung liefert identische Kraefte und Drehmomente. |
| Einzelzellen mit verschiedenen Zeitschritten | Kraftimpulse skalieren mit der Zeit; Rotationsdaempfung und Groessenannaeherung funktionieren ohne Echtzeituhr. |
| Zellmittelpunkt im Hindernis und Zelle an einer Ecke | Kreisgeometrie wird beruecksichtigt; nur die in das Hindernis gerichtete Geschwindigkeit wird entfernt. |
| Fallende Zelle ueber Boden, 240 Schritte | Der Kreis bleibt oberhalb des Bodens. |
| Aktor beruehrt Blocker neben einer anderen Zelle | Blockerkontakt behaelt Vorrang vor weiterer Sensorerkennung. |
| 24 Zellen mit protokollierten Eingaben | Alle Gehirne lesen den gleichen abgeschlossenen Simulationsschritt. |
| Einzelzelle ohne Sonnenstrahlen | Oberer Sensor hat den kleinsten Y-Wert; Sonnenkontakt gilt nur fuer den aktuellen Schritt. |

`CoincidentCellCentersTest` prueft zusaetzlich alle drei oeffentlichen Abstoßungsvarianten
(linear, bisherige experimentelle lineare Variante, nichtlinear). Die zwoelf parametrisierten
Tests decken exakte Ueberlagerung, sehr kleine Abstaende bis `Double.MIN_VALUE`,
periodisch identische Positionen und fuenf deckungsgleiche Zellen ueber 60 Schritte ab.
Kraefte muessen endlich, entgegengesetzt und ohne doppelte Paarzaehlung sein.

## Vererbungspruefungen

Quelle: `src/test/java/de/lifecircles/model/neural/InheritanceRegressionTest.java`.

- Werte, Deltas und Eingangssummen mehrerer Modulausgaenge bleiben getrennt.
- Synapsen lesen den angegebenen Modulausgang.
- Kindmodule teilen den Kindtyp untereinander, aber kein veraenderbares Typnetz mit dem Elternteil.
- Auch vom echten strukturellen Mutationsalgorithmus erzeugte Module werden korrekt kopiert.
- Kopieren ohne Mutation erhaelt das Verhalten bei gleichen Eingaben.
- Gewichte und Biases des Elternteils bleiben ueber 20 Mutationsgenerationen unveraendert.
- Aktivierungen und rekurrentes Gedaechtnis starten bei genetischen Kopien leer.
- Wiederverwendete Neuronen-IDs enthalten keine Werte des entfernten Neurons.
- Durch Zellzustand deaktivierte Layer bleiben vollstaendig deaktiviert.
- Zellteilung setzt den Kindzustand vor dem ersten Denken, teilt Energie und uebernimmt Bewegung und Generation.
- Vererbte Mutationsfaktoren beeinflussen die tatsaechlich verwendete Mutationsrate und -staerke.
- Kinder werden an den Weltrand zurueckgefuehrt; gespeicherte Modulnetze behalten ihre Portverbindungen beim Laden.

## Mehrere Ein- und Ausgangsports

Quelle: `src/test/java/de/lifecircles/model/neural/MultiPortNeuronTest.java`.

Zehn Tests pruefen getrennte Werte, Deltas und Eingangssummen fuer sieben Ausgaenge
auch nach Wachstum des Speichers. Beim Lernen wird jede Synapse genau einmal anhand
ihres Quellausgangs und Zieleingangs aktualisiert. Modul-Eingangsgradienten werden
getrennt von den Fehlern der Modulausgaenge berechnet; die Ableitung der inneren
Ausgabeneuronen wird dabei genau einmal angewendet.

Numerische Ableitungen der Verlustfunktion dienen als unabhaengiger Vergleich fuer
alle Gewichte und Biases der kleinen Testnetze. Abgedeckt sind vorgeschaltete skalare
Neuronen, ungleiche Portzahlen (1/3, 3/1, 2/3), gekreuzte Verbindungen und ein
verschachteltes Modul. Ein weiterer Test prueft, dass ein nicht am Fehler beteiligter
Modulausgang durch das Lernen am anderen Ausgang unveraendert bleibt.

Die drei vorhandenen `BackpropagateTest`-Versuche fuer XOR, AND und Bitzaehlung
sind ebenfalls Teil des gezielten Regressionslaufs.

## Verschachtelte Vererbung und Instanzzustand

Quelle: `src/test/java/de/lifecircles/model/neural/NestedInheritanceTest.java`.

Zwoelf Tests pruefen die Trennung bis zu drei Verschachtelungsebenen:

- Eltern, Kinder und Geschwister teilen keine veraenderbaren Genotypobjekte,
  einschliesslich Typmetadaten, Neuronen, Layern und Synapsen.
- Eine mehrfach verwendete Typdefinition wird im gesamten Kindgraphen genau einmal
  kopiert, auch wenn sie auf verschiedenen Ebenen vorkommt.
- Jede Ausfuehrungsinstanz hat eigene Werte, Eingangsgradienten und Layer-Zaehler.
  Gemeinsame Typdefinitionen koppeln damit nicht das Gedaechtnis ihrer Instanzen.
- Tiefe Mutation und Lernen am Kind lassen Eltern und Geschwister unveraendert.
  Auch aggressive Mutation verwendet ausschliesslich den Wertespeicher des Kindes.
- Numerische Gradienten pruefen Lernen bei mehrfach verwendeten verschachtelten Typen.
- Acht Kindnetze lernen parallel genauso wie acht getrennt ausgefuehrte Vergleichsnetze.
- Strukturaenderungen eines Kindtyps erneuern dessen Ausfuehrungsansicht. Das Loeschen
  des Gedaechtnisses setzt auch die verschachtelten Instanzen zurueck.
- Speichern und Laden erhaelt die getrennten Instanzen; die Netze koennen identisch weiterlernen.

Genetische Kopien starten mit frischen Aktivierungen und dynamischen Layer-Zaehlern;
Zellzustand-Layer behalten ihre Aktivierung bis zur expliziten Festlegung des Kindzustands.
Ausfuehrungsansichten (`makeCopy=false` bzw. Bindung an ein vorhandenes `NeuralNet`)
teilen bewusst die Gene, besitzen aber getrennte Laufzeitzustaende. Die Standardkopie
und `mutate()` kopieren dagegen den gesamten Genotyp. Rekursive Typzyklen werden beim
Kopieren mit einer klaren Fehlermeldung abgelehnt, da sie nicht endlich ausfuehrbar sind.

## Begrenzte Trainingswechsel

Quelle: `src/test/java/de/lifecircles/service/trainStrategy/TrainingGenerationTest.java`.

17 Tests pruefen alle drei selektierenden Strategien: `HIGH_ENERGY`, `HIGH_POSITION`
und `HIGH_POSITION2`. Trainingsnachkommen werden direkt aus den Gewinnern mutiert;
Teilungsbereitschaft, Alter, Energie und gecachte Aktorpositionen sind dafuer keine
Voraussetzung. Die biologische Zellteilung verwendet weiterhin ihre normalen
Entscheidungen und teilt die Elternenergie. Training stattet Nachkommen explizit
mit voller Energie aus und entzieht den Gewinnern dafuer keine Energie.

Zuerst werden alle Positionen gegen Blocker geprueft. Pro Platz sind hoechstens
64 Versuche erlaubt, beginnend mit der bevorzugten Position. Ersatzpositionen werden
bei den Hoehenstrategien innerhalb des jeweiligen Abschnitts gesucht, bei der
Energiestrategie innerhalb der gesamten Welt. Die Suche ist begrenzt und garantiert
keine vollstaendige Untersuchung jeder moeglichen freien Position.

Kann ein Platz nicht gefunden werden, wird der Generationenwechsel mit einer
protokollierten Meldung uebersprungen. Die bisherige Population einschliesslich
Reihenfolge, Positionen, Zustaenden und Energien bleibt dabei erhalten. Erst nach
erfolgreicher Planung werden die Nachkommen erzeugt und die Gewinner zurueckgesetzt.

Abgedeckt sind Gewinner ohne jede Teilungsbereitschaft, vollstaendig blockierte
Welten ueber wiederholte Auswahlrunden, blockierte bevorzugte Positionen mit freiem
Ausweichraum, leere Populationen und leere Abschnitts-Gewinnerlisten. Ein spaeter
Fehler beim zweiten Platz darf weder Gewinner veraendern noch bereits Nachkommen
mutieren. Weitere Tests pruefen den ehemals fehlenden 16. Platz bei `HIGH_POSITION`
und die Begrenzung von `HIGH_ENERGY` auf 20 Zellen bei uebergrossen Populationen.

## Veraenderte Modellregeln

- Feldstaerke: negativ = anziehend, positiv = abstossend. Die Farbdarstellung folgt derselben Regel.
- Feldwirkung nimmt linear zur Reichweitengrenze auf null ab. Bisher stieg sie mit dem Abstand an.
- Gegenkraefte greifen am Aktor der jeweiligen Zelle an. Jeder parallele Worker schreibt nur seine eigene Zelle.
- Kraefte bleiben fuer den bisherigen 60-Hz-Schritt gleich skaliert; andere Schrittweiten skalieren den Impuls proportional.
- Rotationsdaempfung ist exponentiell in der Simulationszeit. Alte Populationen koennen dadurch deutlich anders rotieren.
- `sizeChangeDelay` ist die Zeitkonstante der exponentiellen Groessenannaeherung in Millisekunden.
- Blocker werden als feste, unelastische Hindernisse behandelt. Ueberlappungen werden nach der Bewegung korrigiert;
  die bisherige `blockerRepulsionStrength` steuert diese geometrische Kontaktaufloesung nicht mehr.

Die Blockerpruefung ist weiterhin diskret: Sehr schnelle Zellen koennen ein duennes
Hindernis in einem Schritt vollstaendig ueberspringen. Dafuer waere eine kontinuierliche
Kollisionspruefung oder eine Unterteilung des Physikschritts notwendig.
