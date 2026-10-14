# Physik und Vererbung: kleine Testwelten

Die Tests laufen ohne JavaFX-Fenster. Physikwelten verwenden konstante Gehirnausgaben,
keine Sonnenstrahlen und keine automatische Wiederbesiedlung. Zufallszahlen fuer
Genetiktests sind fest vorgegeben; globale Einstellungen werden danach wiederhergestellt.

## Ausfuehren

Java 25 und Maven:

```powershell
mvn test '-Dtest=TopSensorTest,SimulationSnapshotTest,PeriodicWorldTest,RotationTest,SizeChangeTest,TrainingGenerationTest,NestedInheritanceTest,MultiPortNeuronTest,BackpropagateTest,CoincidentCellCentersTest,PhysicsRegressionTest,InheritanceRegressionTest,CellSensorVisibilityTest,SerialTest#testNeuronSynapseConnectionsAfterDeserialization,Cell_writeObject_is_called_Test'
```

Die Auswahl umfasst 196 Tests. Die evolutionaeren Langzeitexperimente der anderen Testklassen
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

## Groessensteuerung im Denk- und Weltablauf

Quelle: `src/test/java/de/lifecircles/service/SizeChangeTest.java`.

21 Tests pruefen die Gehirnausgaben ueber `CellBrainService` und `CellCalcService`.
Ein analytischer Vergleich verwendet `Ziel + (Start - Ziel) * exp(-Zeit / 0.1)`.
1, 2, 6, 60 und 600 Denk-/Physikschritte ueber insgesamt 100 ms muessen denselben
Radius liefern. Wiederholte identische Ausgaben verschieben keine Groessenaenderung.
Wechselnde Ziele setzen am aktuellen Radius an; Wachsen und Schrumpfen bleiben
innerhalb der Grenzen. Sehr kleine und sehr grosse Schritte werden ebenfalls geprueft.

Reines Denken bewegt weder Radius noch Wachstumsalter. Ohne Simulationszeit aendert
sich der adulte Radius auch bei deaktivierter Glaettung nicht (dieser Randfall schlug vor
der Korrektur fuer `sizeChangeDelay = 0` und `-1` fehl). Speichern/Laden setzt die
Annaeherung am gespeicherten Radius fort. Eine Einzelzellwelt ueber 60 echte
Weltupdates prueft zusaetzlich die gecachten Aktorpositionen auf dem aktuellen Zellrand.

Die bestehende Neugeborenenregel bleibt erhalten: Start bei 0.5, lineares Wachstum
ueber eine Simulationssekunde zur jeweils aktuellen Zielgroesse. Geprueft werden
1, 4 und 64 Schritte sowie wechselnde Ziele und der Uebergang zur adulten Glaettung.
Ein Zielwechsel waehrend dieser Phase kann weiterhin einen Groessensprung verursachen,
weil die lineare Interpolation das aktuelle Ziel verwendet. Er startet die Wachstumszeit
aber nicht neu. Eine Glaettung auch dieser Sonderphase waere eine weitere Modellregel.

Bei `sizeChangeDelay = 100` sind nach 100 ms etwa 63%, nach 300 ms etwa 95% und
nach 500 ms etwa 99.3% der angeforderten Aenderung erreicht. Es gibt keine Wartefrist
vor Beginn und keinen exakten Endzeitpunkt; die Zielgroesse wird asymptotisch angenaehert.
Bei Werten <= 0 wird sie im naechsten positiven Simulationsschritt direkt uebernommen.

## Rotation und Daempfung

Quelle: `src/test/java/de/lifecircles/service/RotationTest.java`.

25 Tests pruefen freie Rotation, Drehmomentimpulse und eine echte Einzelzellwelt.
Die unabhaengige analytische Referenz lautet fuer eine konstante Daempfungsrate k:

```text
omega(t) = omega(0) * exp(-k*t)
theta(t) = theta(0) + omega(0)/k * (1 - exp(-k*t))
```

Bei k = 0 gilt ungedaempfte Rotation. Winkel werden auf `[0, 2*pi)` normalisiert.
Geprueft werden beide Drehrichtungen, Stillstand, mehrere volle Umdrehungen sowie
1, 2, 10, 60 und 600 Schritte pro Simulationssekunde. Winkel und Geschwindigkeit
muessen bei jedem Zwischenschritt der analytischen Loesung entsprechen. Zuvor war
nur die Geschwindigkeit exakt gedaempft; der Winkel verwendete noch die ungedaempfte
Geschwindigkeit fuer den ganzen Schritt. Dadurch haengte die Drehung von der Schrittweite ab.

Weitere Grenzfaelle: keine und sehr kleine Daempfung, starke Daempfung ohne
Richtungsumkehr, Nullzeitschritt und winzige negative Winkel. Ein negativer Rest
konnte nach Addition von `2*pi` auf genau `2*pi` runden; dieser Fall wird auf null
abgebildet. Negative, NaN- und unendliche Daempfungsraten werden am Config-Setter
abgewiesen, bevor sie die Dynamik beschaedigen.

Die Default-Rate 0.8 hat die Einheit `1/s`: Nach einer Simulationssekunde bleiben
etwa 44.9% der Winkelgeschwindigkeit uebrig; die Halbwertszeit betraegt etwa 0.866 s.
Es gibt keine kuenstliche Obergrenze fuer die Winkelgeschwindigkeit.

Drehmomenttests pruefen Kraefte im Mittelpunkt und entlang des Radius (kein Drehmoment),
gleich grosse Gegenimpulse, Skalierung mit der Simulationszeit, das Leeren des
Kraftakkumulators und die kleinste Neugeborenengroesse. Weltupdates pruefen auch die
gecachten Aktorpositionen nach Rotation. Die exakte Schrittweitenunabhaengigkeit gilt
fuer freie Rotation bei konstantem k. Wechselwirkende Kraefte werden weiterhin als
diskrete Impulse vor der Rotation angewendet; deren Diskretisierungsfehler sind damit
nicht beseitigt. Das bestehende angenaeherte Traegheitsmodell proportional zu Radius^2
bleibt erhalten.

## Periodische Zellgeometrie

Quelle: `src/test/java/de/lifecircles/service/PeriodicWorldTest.java`.

31 Tests vergleichen Situationen im Inneren mit Verschiebungen ueber den horizontalen
Rand, den vertikalen Rand und beide gleichzeitig. Feldkraefte, Drehmomente,
Sensorentfernungen und Energieuebertragung muessen gleich bleiben. Ganze Welten mit
Abstossung, Schwerkraft, Reibung und Rotation werden ueber 60 Schritte verglichen.
Die gleichen Vergleiche laufen mit Raster und Quadtree.

Nachbarsuchen werden in Welten mit Breiten 15, 25, 105 und 300 gegen alle Zellpaare
geprueft, auch bei Suchradien groesser als die Welt und bei nichtkanonischen Positionen
mehrere Milliarden Weltbreiten entfernt. Das Raster darf zusaetzliche Kandidaten
liefern; der Quadtree liefert genau die Treffer innerhalb des Radius. Beide duerfen
keine Nachbarn uebersehen oder doppelt liefern. Der Quadtree fragt passende periodische
Abbilder ab, ohne Zellen zu verschieben; 500 deckungsgleiche Zellen pruefen seine
begrenzte raeumliche Unterteilung.

`Vector2D.wrapped()` fuehrt Positionen einheitlich nach `[0, Breite) x [0, Hoehe)`
zurueck. Bereits kanonische Koordinaten bleiben unveraendert, auch sehr nahe am Rand.
Beim exakt halben Weltabstand wird die gleich lange Richtung anhand kanonischer
Koordinaten gewaehlt: ganze Weltverschiebungen aendern sie nicht, die Gegenrichtung
bleibt entgegengesetzt. Weltlaengen und Suchradien muessen positiv und endlich sein;
die Partitionierer weisen ungueltige Abmessungen ab.

Hinderniskontakte, Hindernissensoren und die Freiplatzpruefung fuer biologische und
Trainingsnachkommen beruecksichtigen periodische Abbilder. Kontakt liefert ein volles
Sensorsignal statt einer Abschwaechung anhand des entfernten Rechteckmittelpunkts.
Der Kollisionsimpuls verwendet die lokale Kontaktnormale; nur die hinein gerichtete
Geschwindigkeit wird entfernt. Danach werden Zellposition und Aktorpositionen erneuert.

Ein Hindernis, das eine ganze Achse abdeckt, hat auf dieser Achse keinen seitlichen
Ausgang. Der weltbreite Boden setzt sich dadurch ohne kuenstliche Seitenkante ueber
den horizontalen Rand fort. Weil auch die Y-Achse periodisch ist, ist seine Unterseite
am oberen Weltrand erreichbar: Eine Zelle bei y=5 mit Radius 10 beruehrt den Boden
bei y=300 von der anderen Seite. Das wird jetzt auch bei Teilungspositionen erkannt.
Eine vollstaendig blockierte Welt bietet keinen gueltigen Ausweg; Nachkommen werden
abgewiesen, vorhandene Zellen werden nicht auf unendliche Koordinaten projiziert.

Weltupdates reichen die eigene Umgebung explizit an Kraefte, Sensoren, Energie und
Gehirneingaben weiter. Eine spaeter erzeugte Welt kann damit nicht versehentlich die
Periodenlaengen oder Hindernisse einer bereits bestehenden Welt ersetzen. Die alten
Service-Einstiegspunkte bleiben fuer bestehende Aufrufer erhalten; sie verwenden
weiterhin die globale Umgebung, die neuen Ueberladungen erlauben explizite Welten.

Die Sonne bleibt die bestehende externe Lichtquelle mit Strahlen von oben im endlichen
Darstellungsfenster. Diese Tests pruefen die periodische Zell- und Kontaktphysik;
sie definieren kein umlaufendes periodisches Strahlenmodell. Die diskrete
Hindernisprojektion verhindert weiterhin kein Durchtunneln bei sehr grossen Schritten.

## Konsistente Darstellungszustaende

Quelle: `src/test/java/de/lifecircles/service/SimulationSnapshotTest.java`.

Neun Tests pruefen die Uebergabe zwischen Simulation und Darstellung ohne JavaFX-Fenster.
Der gleiche Lock schuetzt jetzt den gesamten Physik-/Trainingsschritt, Initialisierung,
Reset und die vollstaendige Snapshot-Erfassung. Zuvor wurde nur die fertige DTO-Zuweisung
geschuetzt; die Kopie der Zellliste enthielt weiterhin parallel veraenderte Zellen.

Ein Snapshot wird bei Bedarf nach dem letzten abgeschlossenen Schritt erstellt.
Wiederholte Abfragen ohne Zustandsaenderung liefern dasselbe Objekt. Die Hauptansicht
zeichnet anschliessend ausserhalb des Locks aus vollstaendig abgetrennten DTOs.
Positionen und Farbarrays werden bei Uebernahme kopiert; Getter geben ebenfalls
Kopien zurueck. Listen sind von den Quelldaten getrennt und nicht veraenderbar.
Blocker- und Strahlendaten enthalten nur feste Werte. Alte Snapshots bleiben auch
nach weiteren Schritten oder Reset unveraendert.

Die Detailansicht liest das lebende Zell-/Neuronennetz unter demselben Lock.
Sie berechnet Aktorpositionen lokal und schreibt nicht mehr in Simulations-Caches.
Klicksuche verwendet ebenfalls den Lock und beruecksichtigt periodische Zellabbilder.
Snapshot-Erfassung und Detailzeichnung koennen einen Simulationsschritt kurzzeitig
aufhalten; es wird dafuer kein komplettes Neuronennetz je Weltframe kopiert.

Nebenlaeufigkeitstests verwenden Latches, um den Schreiber gezielt mitten im
Trainingswechsel anzuhalten. Snapshot und Reset muessen auf dessen Abschluss warten;
umgekehrt darf ein Schritt nicht waehrend eines geschuetzten Detailzugriffs beginnen.
Ein paralleler Lauf mit 300 Updates und 500 Snapshot-Abfragen prueft gleiche Zellalter
sowie die zu Radius und Rotation passenden Aktorpositionen. Weitere Tests manipulieren
Quellvektoren, Quelllisten und zurueckgegebene Farbarrays, ohne den Snapshot zu veraendern.

Ein absichtlich fehlschlagender Schritt darf keinen teilweise veraenderten Zustand
als neue Ansicht liefern: Bis zum naechsten erfolgreichen Schritt oder Reset bleibt
der zuletzt erfasste Snapshot sichtbar; Live-Detailzugriffe werden unterdrueckt.
Dies setzt den Simulationszustand selbst nicht zurueck und ist keine Fehlerbehebung
fuer die Ursache des abgebrochenen Schritts. JavaFX-Zeichenoperationen werden durch
diese Headless-Tests nicht visuell geprueft.

## Oberer Sensor

Quelle: `src/test/java/de/lifecircles/service/TopSensorTest.java`.

24 Tests pruefen `getTopSensorIndex()` und das Gehirneingabesignal
`MY_ACTOR_TOP_POSITION`. Positive Welt-Y-Richtung zeigt nach unten; oben ist daher
die kleinste vertikale Komponente. Bei Rotation null ist Sensor 9 oben, Sensor 3 unten.
Die Umkehr von maximalem zu minimalem Y war bereits im ersten Physik-Fix enthalten.
Die Auswahl wird jetzt zusaetzlich von Positions-Caches unabhaengig gemacht:
Sie verwendet die rotierte Einheitsrichtung des jeweiligen Sensors.

Geprueft werden frische Zellen ohne Cache, alle zwoelf Ausrichtungen mit veralteten
und absichtlich falschen Caches, Neugeborene und grosse Zellen sowie Positionen an
beiden Seiten des periodischen Randes und bei sehr grossen Koordinaten. Auch negative
Rotationen und mehrere volle Umdrehungen liefern dieselbe obere Richtung. Es wird
genau ein Gehirneingang markiert. Die Auswahl schreibt keine Aktor-Caches.

Bei gleicher Hoehe gewinnt der kleinere Index; Differenzen bis `1e-14` in der
vertikalen Einheitskomponente werden als numerischer Gleichstand behandelt.
Beide Seiten der Umschaltgrenze zwischen zwei Sensoren sind separat geprueft.
Vor der Korrektur scheiterten 22 der 24 neuen Pruefungen. Netze, die das bisherige
falsche Signal bei fehlenden oder veralteten Caches ausgenutzt haben, erhalten jetzt
die tatsaechliche obere Orientierung; eine Kompatibilitaetsumschaltung gibt es nicht.

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
- Rotationsdaempfung ist exponentiell in der Simulationszeit; auch der Drehwinkel integriert die abklingende Geschwindigkeit.
  Die Daempfungsrate ist endlich und nichtnegativ in `1/s`. Alte Populationen koennen dadurch deutlich anders rotieren.
- `sizeChangeDelay` ist die Zeitkonstante der exponentiellen Groessenannaeherung in Millisekunden.
- Blocker werden als feste, unelastische Hindernisse behandelt. Ueberlappungen werden nach der Bewegung korrigiert;
  die bisherige `blockerRepulsionStrength` steuert diese geometrische Kontaktaufloesung nicht mehr.

Die Blockerpruefung ist weiterhin diskret: Sehr schnelle Zellen koennen ein duennes
Hindernis in einem Schritt vollstaendig ueberspringen. Dafuer waere eine kontinuierliche
Kollisionspruefung oder eine Unterteilung des Physikschritts notwendig.
