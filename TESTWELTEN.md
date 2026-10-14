# Physik und Vererbung: kleine Testwelten

Die Tests laufen ohne JavaFX-Fenster. Physikwelten verwenden konstante Gehirnausgaben,
keine Sonnenstrahlen und keine automatische Wiederbesiedlung. Zufallszahlen fuer
Genetiktests sind fest vorgegeben; globale Einstellungen werden danach wiederhergestellt.

## Ausfuehren

Java 25 und Maven:

```powershell
mvn test '-Dtest=CoincidentCellCentersTest,PhysicsRegressionTest,InheritanceRegressionTest,CellSensorVisibilityTest,SerialTest#testNeuronSynapseConnectionsAfterDeserialization,Cell_writeObject_is_called_Test'
```

Die Auswahl umfasst 44 Tests. Die langen Lernexperimente der anderen Testklassen
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
