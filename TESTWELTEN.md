# Physik und Vererbung: kleine Testwelten

Die Tests laufen ohne JavaFX-Fenster. Physikwelten verwenden konstante Gehirnausgaben,
keine Sonnenstrahlen und keine automatische Wiederbesiedlung. Zufallszahlen fuer
Genetiktests sind fest vorgegeben; globale Einstellungen werden danach wiederhergestellt.

## Ausfuehren

Java 25 und Maven:

```powershell
mvn test '-Dtest=HighPosition3TrainStrategyTest,StructureEpisodeFitnessTest,ControllerStabilityTest,GroundedColonyFitnessTest,StandingStructureTest,CellFileImportTest,BrainInputScalingTest,StructuralMutationTest,NetworkSchedulingTest,ExecutionGradientTest,ModuleMutationCostTest,TrainingSelectionTest,SunIndexRegressionTest,PerformanceEquivalenceTest,TissueMechanicsTest,MechanicalFixtureTest,MechanicsRegressionTest,ForceSignTest,SensorTargetSelectionTest,MutationFactorsTest,PopulationEnergyTest,SunContactTest,TopSensorTest,SimulationSnapshotTest,PeriodicWorldTest,RotationTest,SizeChangeTest,TrainingGenerationTest,NestedInheritanceTest,MultiPortNeuronTest,BackpropagateTest,CoincidentCellCentersTest,PhysicsRegressionTest,InheritanceRegressionTest,CellSensorVisibilityTest,SerialTest#testNeuronSynapseConnectionsAfterDeserialization,Cell_writeObject_is_called_Test'
```

Die Auswahl umfasst 365 Tests. Die evolutionaeren Langzeitexperimente der anderen Testklassen
gehoeren nicht zu diesem gezielten Lauf.

`BrainEnergyLabTest` ergänzt 19 Routineprüfungen für Aufgaben, Gedächtnis,
Energieformel und reproduzierbare Evolution:

```powershell
mvn test '-Dtest=BrainEnergyLabTest'
```

Zusammen wurden 384 Routineprüfungen erfolgreich ausgeführt. Die optionalen
Kalibrierungsserien und ihre Ergebnisse stehen in
[GEHIRN_ERGEBNISSE.md](GEHIRN_ERGEBNISSE.md).

`NoneEnergyControlsTest` ergänzt sieben Routineprüfungen für tatsächliche Gebühren,
Tod, reproduzierbare Geburtsmutation und die Auswahl der Gründer. Zusammen bestanden
391 Prüfungen. Die optionalen NONE-Fortsetzungen und deren Bewertung stehen in
[NONE_ERGEBNISSE.md](NONE_ERGEBNISSE.md).

Die Performance-Äquivalenztests vergleichen die beschleunigte Sonne mit dem Vollscan
und prüfen wiederverwendete Nachbarlisten bei Bewegung, Tod und periodischen Nähten.
Snapshottests sichern vollständige und kompakte Ansichten sowie die optionale
Phasenprofilierung ab. Kurze Laufzeitmessungen und ihre Grenzen: [PERFORMANCE.md](PERFORMANCE.md).

`SunIndexRegressionTest` ergänzt vier Prüfungen für den wiederverwendeten Sonnenindex
mit Tiefenstaffelung: geänderte Population/Geometrie, Dunkelheit und Vollscan-Rückfall,
Tangenten und gleiche Treffer über Gruppengrenzen, Abschattung und Zellpriorität sowie
Strahlstart innerhalb einer Zelle und Treffer genau am Weltende.

`TissueMechanicsTest` ergänzt zwölf Prüfungen für ein- bis dreischichtige Kugelpackungen,
eine bewegliche Brücke und aktive Größenzyklen gegen Gegenlast. Biegesteifigkeit,
Rückstellung, Zugbruch, Einknicken bei gestörtem Druck und Zeitschrittabhängigkeit
werden ohne Fenster gemessen. Die 238 separat ausgeführten Weltläufe und ihre Grenzen
sind in [GEWEBEPROBEN.md](GEWEBEPROBEN.md) dokumentiert. `TissueMechanicsSweepTest`
läuft ausschließlich mit `-Dtissue.sweep=true` und gehört nicht zur Routineauswahl.

HIGH_POSITION3 ergänzt ein Training ganzer klonaler Verbände mit vier Stufen:
Halten, Störungen ausgleichen, Größensteuerung und Aufbau. Die 16 neuen
Strategie-/Bewertungsprüfungen gehören zur Routineauswahl. Kontrolllauf und echte
Zufallsgenerationen sind optional: [HIGH_POSITION3.md](HIGH_POSITION3.md).

RACE (Rennbahn) wählt kleine klonale Verbände nach Eigenfortschritt nach rechts aus; Teilung nach
vorn zählt nicht. `RaceGaitTest` zeigt, dass nur Anker Vortrieb geben, `RaceTrainStrategyTest`
prüft Bahnen, Obergrenze, Bewertung und Auswahl: [RENNBAHN.md](RENNBAHN.md).

## Physikwelten

Die Netzkorrekturen stehen in [NETZPRUEFUNG.md](NETZPRUEFUNG.md). Ihre
Korrektheitsregressionen sind Teil der oben genannten 365 Tests. Nur die einzelne
Mutationsverteilungsprobe bleibt optional:

```powershell
mvn test '-Dtest=NetworkMutationStudyTest' '-Dnetwork.audit=true'
```

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

## Sonnenkontakt pro Schritt

Quelle: `src/test/java/de/lifecircles/service/SunContactTest.java`.

Fuenf Tests pruefen tatsaechliche Strahlentreffer mit fest vorgegebener Strahlposition:
Treffer, mehrere dunkle Schritte und erneute Treffer; Abschattung durch ein Hindernis;
getroffene und verfehlte Zellen im selben Schritt; sowie einen Strahlenakkumulator,
der noch keinen ganzen Strahl erzeugt. Kontakt und Gehirneingabe muessen den aktuellen
Schritt beschreiben. Ohne Treffer wird keine Sonnenenergie hinzugefuegt.

Der Reset war seit dem ersten Physik-Fix im Weltupdate vorhanden. Er liegt jetzt
direkt am Anfang von `EnergySunCalcService.calculateSunEnergy()`, damit auch direkte
Aufrufer keinen alten Kontakt behalten. Alle Zellflags werden zuerst geloescht und
nur bei einem tatsaechlichen Treffer erneut gesetzt. Ein weiterer Test prueft im
vollstaendigen Weltupdate, dass das Gehirn schon im selben Schritt null erhaelt.

## Population ohne automatische Energiesubvention

Quelle: `src/test/java/de/lifecircles/service/PopulationEnergyTest.java`.

13 Tests pruefen kleine Populationen mit `initialCellCount = 20` und ausgeschaltetem
Sonnenlicht, also deutlich unterhalb der frueheren Wiederbesiedlungsschwelle.
Die automatische Wiederbesiedlung ist entfernt: keine Auffuellung von Kindenergie
auf 1.0, keine erzwungenen Teilungen unter Umgehung von Alter/Energieschwelle und
kein Neustart aus der zuletzt gestorbenen Zelle oder aus zufaellig erzeugten Zellen.
`initialCellCount` steuert nur die explizite Initialisierung bzw. den Reset.

Die Population waechst ausschliesslich durch regulaere Teilung lebender Zellen.
Das Kind erhaelt den gewaehlten Anteil der verfuegbaren Elternenergie; die Summe
bleibt gleich. Getestet sind Anteile 0.1, 0.25 und 0.5 sowie die Energiebilanz nach
Stoffwechselkosten. Wiederholte Teilung mit Anteil 0.1 reduziert einen Elternteil
mit Energie 0.8 nach sieben Geburten unter die Schwelle 0.4; die Gesamtenergie
bleibt 0.8 und es wird nicht weiter auf die konfigurierte Anfangszahl aufgefuellt.

Zu junge, erschoepfte oder nicht teilungswillige Zellen werden nicht zur Teilung
gezwungen. Ein blockierter Geburtsort entzieht keine Energie. Die Todespruefung
liegt vor der Teilung: Zellen am Hoechstalter koennen keine letzten Kinder mehr
erzeugen. Ausgestorbene und anfangs leere Welten bleiben leer, auch ueber weitere
Updates. Ein Neustart erfolgt bewusst ueber die bestehende Reset-/Initialisierungsfunktion.

Das selektierende Training ist weiterhin ein explizites Experiment mit vorgegebenen
Startbedingungen: Gewinner und Trainingsnachkommen erhalten dort volle Startenergie.
Die vorhandenen Trainingsregressionen pruefen diese Regel weiterhin getrennt von der
Energiebilanz der biologischen Welt. Fuer die entfernte automatische Wiederbesiedlung
gibt es keine Kompatibilitaetsoption und keine Teilungsbelohnung.

## Stabile Sensorzielauswahl

Quelle: `src/test/java/de/lifecircles/service/SensorTargetSelectionTest.java`.

Zehn Tests pruefen eine feste Rangfolge fuer die einzelne Zielreferenz eines Sensors:

1. Ein beruehrter Blocker hat Vorrang vor Zellaktoren.
2. Unter Zellaktoren innerhalb ihrer Senderreichweite gewinnt der kleinste periodische Abstand.
3. Bei exakt gleichem Abstand gewinnt die Zelle mit der kleineren laufzeitstabilen ID.
4. Innerhalb derselben Zelle entscheidet der kleinere feste Aktorwinkel.

Bei mehreren beruehrten Blockern entscheidet deren kleinere laufzeitstabile ID.
Der naechste Zellaktor und Blockervorrang waren bereits im ersten Physik-Fix
enthalten; hinzugekommen ist die vollstaendige Aufloesung der Gleichstaende.
Es wird ohne Abstandstoleranz verglichen, damit die Rangfolge transitiv bleibt.
Winzige echte Abstandsveraenderungen duerfen daher weiterhin das naechste Ziel wechseln;
eine zeitliche Hysterese ist nicht Bestandteil dieser Auswahlregel.

Die IDs werden bei der Objekterzeugung threadsicher vergeben und nur zur Zielauswahl
bei Gleichstand verwendet. Zell-IDs sind transient und werden beim Laden neu vergeben,
damit auch mehrfach geladene Kopien desselben Objekts unterscheidbar bleiben.
Listenumsortierung aendert keine ID. Bei exakten Gleichstaenden kann sich die Auswahl
nach erneutem Laden aendern; eine ueber Speicherzyklen persistente Objektidentitaet
wird hier nicht eingefuehrt. Innerhalb einer laufenden Welt ist die Auswahl stabil.

Geprueft werden unterschiedliche Abstaende, 100 zufaellige Umordnungen von Welt- und
Nachbarlisten, ein exakter Aktor-Gleichstand, ueberlappende Blocker und deren Entfernung.
Raster und Quadtree muessen am periodischen Rand dasselbe Ziel liefern. Energie wird
an denselben Gewinner uebertragen; nicht mehr sichtbare Ziele werden im naechsten
Durchlauf geloescht. Wiederholtes Laden liefert verschiedene Laufzeit-IDs.

Die Zielentscheidung beeinflusst nur Sensorinformationen und zielgerichtete
Energieuebertragung. Feldkraefte werden weiterhin von allen relevanten Aktorpaaren
berechnet, auch wenn ein Blocker das Sensorziel ist. Ein eigener Test prueft dabei
die entgegengesetzten Kraefte und den erhaltenen Gesamtimpuls.

## Vorzeichen der Aktorfelder

Quelle: `src/test/java/de/lifecircles/service/ForceSignTest.java`.

Die Konvention gilt durchgaengig fuer Gehirnausgabe, gespeicherte Feldstaerke,
Feldkraft, Kommentare sowie Haupt- und Detaildarstellung:

| Gehirnausgabe FORCE | Feldstaerke | Wirkung | Darstellung |
| --- | --- | --- | --- |
| 0 | -maximale Anziehungsstaerke | Anziehung | rot |
| 0.5 | 0 | neutral | transparent |
| 1 | +maximale Abstossungsstaerke | Abstossung | gruen |

Zwischenwerte skalieren auf der jeweiligen Seite linear. Die Maximalbetraege sind
getrennt konfigurierbar (Default: Anziehung 90, Abstossung 150). Werte ausserhalb
des vorgesehenen Ausgabebereichs werden am Aktor auf die jeweilige Grenze begrenzt.

Der periodische Richtungsvektor `towardOtherActor` zeigt vom eigenen Aktor ZUM
anderen Aktor. Die tatsaechliche Kraft verwendet dessen negatives Vielfaches:

```text
F = -effektiveFeldstaerke * Groessenskalierung * towardOtherActor / Abstand
```

Deshalb wirkt eine negative Feldstaerke in Richtung des anderen Aktors, eine
positive dagegen entgegengesetzt. Die effektive Feldstaerke enthaelt das empfangene
Feld und die Reaktion auf das eigene Feld, jeweils mit linearem Reichweitenabfall.
Mit der neuen Kontaktglaettung wird jeder Beitrag zusaetzlich mit
`min(1, Abstand / (Senderreichweite * actorFieldSofteningFraction))` skaliert;
bei Glaettungsanteil null bleibt die fruehere Kraftform erhalten.
Der fehlende Vorzeichenwechsel der urspruenglichen Implementierung wurde bereits
im ersten Physik-Fix korrigiert. Die aktuelle Pruefung aendert diese Physik nicht.
Die frueheren Renderer-Kommentare zu Gradienten wurden durch die tatsaechliche
Farbcodierung ersetzt; die Felder werden als gefuellte Kreise dargestellt.

Sieben parametrisierte Tests pruefen Ausgaben -0.5, 0, 0.25, 0.5, 0.75, 1 und 1.5
ueber die gesamte Kette `think()` -> Output-Abbildung -> Feldkraft -> Geschwindigkeit.
Jede Ausgabe wird auf beiden Achsen, im Inneren und ueber die periodische Naht sowie
mit beiden moeglichen Sendern geprueft (56 kleine Versuche). Ein isoliertes Aktorpaar
erlaubt den analytischen Vergleich des Impulsbetrags inklusive Reichweitenabfall und
Groessenskalierung. Die Reaktionen bleiben gleich gross und entgegengesetzt.

## Mechanisches Labor und Kontaktglaettung

Details, Messdaten, Abbildungen und Reproduktionsbefehle: [MECHANIK.md](MECHANIK.md).

Sieben `MechanicalFixtureTest`-Pruefungen sichern Radien, initiale Kraftausgaben,
aktuelle Gehirneingaben und TEST_A-Positionen anhand der echten Welthoehe ab.
Zwoelf `MechanicsRegressionTest`-Pruefungen sichern stetige Aktorkraefte nahe Kontakt,
unveraenderte Kraefte ausserhalb der Kontaktzone, Parametergrenzen, den Schneemann
mit und ohne fallende Zellen, Kette, Dreiecksstreifen, reversiblen Aktuatorhub,
Beweglichkeit, Wiederholbarkeit und Zeitschrittkonvergenz ab.

Der Default `actorFieldSofteningFraction = 0.25` gibt Aktorfeldern eine lineare
Kernzone. Dort geht der Kraftbetrag kontinuierlich gegen null. Das Vorzeichen
bleibt unveraendert; ausserhalb gilt die vorherige Kraftform.
`MechanicsPreset.BALANCED` kombiniert dies mit Rotationsdaempfung 3 und wird von
TEST_A beim Initialisieren der Umgebung angewendet. `LEGACY` erlaubt Vergleichslaeufe.

`MechanicsSweepTest` laeuft nur mit `-Dmechanics.sweep=true`; die 318 dokumentierten
Weltlaeufe sind nicht Bestandteil der 262 normalen Regressionstests. Die Studie
verwendet feste Gehirnausgaben und beweist keine selbstentstehende Gewebedifferenzierung.

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

## Wirksame Mutationsfaktoren

Quelle: `src/test/java/de/lifecircles/service/MutationFactorsTest.java`.

Die Beruecksichtigung der Elternfaktoren wurde bereits im ersten Vererbungs-Fix
eingebaut. Zwoelf zusaetzliche Pruefungen sichern biologische Teilung und
Trainingsnachkommen ueber denselben Kind-Erzeugungspfad ab:

```text
effektive Rate    = min(1, globale Rate * Eltern-Ratenfaktor)
effektive Staerke = globale Staerke * Eltern-Staerkefaktor
```

Die Tests erfassen die Parameter am Gehirn und pruefen zusaetzlich echte Kindgene.
Ein festgelegter Zufallswert 0.75 fuehrt bei effektiver Rate 0.25 zu keiner Mutation,
bei Rate 1 dagegen zur Mutation. Mit demselben Mutationszug aendert sich ein Bias
bei Staerkefaktor 0.5 um 0.05 und bei Faktor 2 um 0.2 (globale Staerke 0.2).
Dabei bleiben Eltern-Biases unveraendert. Weitere Pruefungen decken die Begrenzung
der Rate, die Faktorgrenzen 0.1 bis 2 und exakte Faktorvererbung bei globaler Rate 0 ab.

Die Faktoren des Elternteils bestimmen die aktuelle Netzmutation. Anschliessend
erbt das Kind die Faktoren; deren eigene Mutation verwendet die globalen Einstellungen.
Veraenderte Kindfaktoren wirken erst auf seine spaeteren Nachkommen. Diese Trennung
verhindert, dass die aktuelle Netzmutation von bereits veraenderten Kindfaktoren abhaengt.

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
erfolgreicher Planung werden die Nachkommen erzeugt und frische Elitekopien erzeugt.

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


## Korrekturen aus der Netzpruefung

Die 18 ehemaligen Beobachtungen wurden in regulaere Struktur-, Steuerungs-,
Gradienten-, Modul- und Trainingsregressionen ueberfuehrt. Eine einzelne optionale
Mutationsverteilungsprobe bleibt als `NetworkMutationStudyTest` erhalten.
Der breite Abschlusslauf bestand mit 312 regulaeren Regressionen plus dieser Probe.
Details und gezielte Aufrufe: [Netzpruefung](NETZPRUEFUNG.md).
Die kurze Leistungsprobe und Rohdaten stehen in [Netzperformance](NETZPERFORMANCE.md).

Trainingsgenerationen enthalten hoechstens 20 % unveraenderte Elitegenome bei den
normalen Zielgroessen 16/20; der Rest sind Mutationskandidaten. Beide beginnen als
frische Zellen mit gleicher Wachstumsphase, Energie und genau einem initialen
Denkschritt. Auch Elitekopien teilen keine Laufzeitzustaende mit ihren Eltern.


## Laden und Zusammenfuehren ohne veraltete Anzeige

`CellFileImportTest` ergaenzt zehn Regressionen: Laden/Ersetzen und Zusammenfuehren
vor Start und pausiert, jeweils mit vollstaendigem und kompaktem Snapshot, defekte
Dateien sowie Import waehrend eines noch laufenden Simulationsschritts.
Dateilesen erfolgt ausserhalb der Simulationssperre; die gesamte Population wird
zwischen zwei Schritten uebernommen und der Darstellungszustand sofort aktualisiert.
`FileService` veraendert die Welt nicht mehr direkt. Beide Ladebuttons verwenden
`CalculationService.loadCellsFromFile()`.

Die alte Population `default-2025-07-29-a01.cells` wurde separat unveraendert geladen:
1.047 Zellen, direkt nach Import 1.047 Zellen im Snapshot. Ein anschliessender
Physik-/Denkschritt in einer Welt 6.400 x 1.200 lieferte endliche Positionen,
Groessen und Energien. Das belegt Laden und kurzfristige Ausfuehrbarkeit;
langfristiges Verhalten unter den korrigierten Modellregeln wurde nicht geprueft.
Der gezielte Abschlusslauf bestand mit 36 Tests inklusive dieser realen Datei.

Die echte Datei gehoert nicht zum Repository. Der optionale `LegacyCellFileTest`
laeuft nur mit angegebenem Dateipfad und schreibt die Quelldatei nicht:

```powershell
mvn test '-Dtest=CellFileImportTest,SimulationSnapshotTest'
mvn test '-Dtest=LegacyCellFileTest' '-Dlegacy.cells=C:\Pfad\population.cells' '-DargLine=-Xmx3g -Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```


## Standproben unter voller Schwerkraft

`StandingStructureTest` ergaenzt vier Routinepruefungen: Eine grosse 5 x 12-Packung
bleibt aufrecht, eine schmale kleine 1 x 8-Packung verliert trotz Zusammenhalt ihre
Hoehe. Beide werden bei 60 und 120 Hz geprueft. 108 separate Weltlaeufe,
Energiebilanz und Bewertung von HIGH_POSITION2: [Standproben](STANDPROBEN.md).
`StandingStructureStudyTest` laeuft nur mit `-Dstanding.study=true`.


## HIGH_POSITION2 bewertet anhaltende Hoehe

`GroundedColonyFitnessTest` ergaenzt 16 Pruefungen fuer die geometrische Bodenbindung
und die zeitliche Bewertung ganzer Verbaende. `TrainingSelectionTest` besitzt jetzt
sechs Pruefungen, einschliesslich echter Vererbung des Turmprogramms statt einer
abgeloesten Spitzenzelle. Details: [HIGH_POSITION2](HIGH_POSITION2.md).


## Optionale echte Evolutionslaeufe

`HighPosition2EvolutionStudyTest` startet nur mit `-Devolution.study=true` echte
mehrere Generationen lange Versuche. `EvolutionArtifactExportTest` exportiert die
gespeicherten Staende und fuehrt auf Wunsch deklarierte Nachversuche aus.
Parameter, Rohdaten, ladbare Populationen und Auswertung: [EVOLUTIONSLAUF.md](EVOLUTIONSLAUF.md).
Diese Laeufe gehoeren nicht zur regulaeren Regressionenauswahl.
