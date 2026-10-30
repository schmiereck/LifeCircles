# Rennbahn (TrainMode RACE)

Status: **experimentell umgesetzt** (24.10.2026). Frage des Nutzers: Können Zellen im Verbund eine
Funktion entwickeln, konkret sich gemeinsam fortbewegen? Viele parallele Bahnen, Gewinner ist,
wer am weitesten von links nach rechts kommt. Die Zellen sollen sich normal teilen, aber nicht
durch schiere Anzahl nach rechts drücken; gewünscht ist ein kleiner, sich aktiv vermehrender Verbund.
Seit 26.10.2026 (Nutzervorgabe: die Richtung ist nicht entscheidend, links nach rechts war ein Fehler in den
Anforderungen) startet jeder Körper in der Bahnmitte, und der Weg in beide Richtungen zählt.

## Physikalischer Befund vorab

- **Kräfte zwischen Zellen heben sich auf.** Aktorfelder wirken paarweise mit Gegenkraft, alle Zellen
  haben für die lineare Bewegung dieselbe Masse (`applyForce` · 0,08), die Viskosität bremst jede Zelle
  gleich. Der Schwerpunkt eines freien Verbands kann sich deshalb aus eigener Kraft nicht seitlich bewegen.
- **Ohne Geländereibung ist der Boden reibungsfrei.** Die Blockerkollision entfernt nur die
  Geschwindigkeit in die Oberfläche hinein; das Bodenfeld (Bodenhalt Stufe A) wirkt senkrecht. Rollen
  erzeugt dann keinen Vortrieb.
- **Vortrieb gibt es nur über Halt am Gelände:** Anker (Bodenhalt Stufe B) oder, seit 24.10.2026, die
  Geländereibung (`BODENREIBUNG.md`): Eine Zelle, die ihre grauen Fußaktoren anziehen lässt, wird vom
  Bodenfeld angedrückt und haftet; eine losgelassene gleitet. Ein Verband kann damit kriechen: eine Zelle
  greift, die andere wird weggeschoben, dann wechseln die Rollen („Raupe“). Mit Reibung kann eine Zelle
  auch rollen, wenn sie sich selbst dreht.

`RaceGaitTest` prüft das mit einem von Hand geregelten Zwei-Zellen-Gang (vier Takte: hinten greifen,
vorn wegschieben, vorn greifen, hinten nachziehen, je 1 s, Abstand über einen gedämpften Regler):

| Gang, 30 s | Weg des Schwerpunkts |
|---|---|
| ohne Anker, ohne Reibung | 0,0 px |
| mit Ankern, ohne Reibung | +10,8 px (vor dem 24.10.2026 +15 px, siehe unten) |
| Reibungsgang, Reibung 0,5, ohne Anker | +21,2 px |

Der Vortrieb über Anker ist möglich, aber langsam: Anker sind weiche Federn innerhalb der kurzen
Aktorfelder (etwa 9 px über den Rand bei Radius 20), der Tonus (0,5 s) verzögert jeden Wechsel, die geringe
Viskosität lässt die Zellen über das Ziel schwingen. Andere Taktzeiten und Hübe brachten 6–13 px.
Die 15 px stammten aus einem Lauf, in dem ein vergessener Diagnoseblock im Test einmal Kräfte zusätzlich
anwandte; ohne ihn und mit dem Abstandsregler über die gemessene Abstandsänderung sind es 10,8 px.
Der Reibungsgang und seine Parameter stehen in `BODENREIBUNG.md`.

## Umsetzung

- `TrainMode.RACE`, `RaceTrainStrategy`.
- **Welt:**
  - 32 Bahnen à 1600 px nebeneinander (51 200 × 800 px; bis 24.10.2026 abends 16 Bahnen), Boden, an jedem
    Bahnanfang eine Wand über die volle Höhe.
  - Der Gründer startet in der Bahnmitte, 800 px (± 40 px zufällig) von beiden Wänden entfernt, damit ein Haufen
    nicht von einer Wand abstoßen kann, und mit zufälliger Drehung. Bis 25.10.2026 startete er 400 px hinter der
    linken Wand, und nur der Weg nach rechts zählte.
- **Schalter beim Anlegen:** Geländereibung, Haftung, Bindungen, Geburtsbindung, Bodenfeld, Anker,
  „Anker nur am Boden“ (die Wände halten nicht). Die Policy folgt dem Hauptschalter Haftung wie die offenen
  Welten (nicht `trainingAdhesion`), denn ohne Haftung drückt kein Fuß an und es gibt keinen Vortrieb.
- **Körper:**
  - Ein Gründer je Bahn, der sich klonal teilt (`reproduceClonally`): Ein Verband trägt ein Genom, die
    Auswahl wirkt auf das Genom des Gründers.
  - Rollen über `cellState` bleiben möglich; ob und wohin eine Zelle sich teilt, entscheidet das Gehirn
    (normale Teilungsregeln).
- **Teilung begrenzt durch eine Zellobergrenze, nicht durch Energie:** höchstens 6 Zellen je Bahn,
  und nur in der Wachstumsphase (0–30 s jeder Episode).
  Die Energie ist gesichert (Primärenergie 0,75 in jedem Schritt), die Gehirne sind kostenlos, keine
  Sonne, keine Umwandlung, kein Tod; wie HIGH_POSITION3. Bewegung kostet damit nichts.
- **Bewertung „Eigenfortschritt“:**
  - In jedem Schritt zählt der mittlere waagrechte Schritt der Zellen, die schon im vorigen Schritt da
    waren; das wird aufsummiert.
  - Bei gleichen Massen heben sich alle Kräfte innerhalb des Verbands in diesem Mittel auf. Übrig bleibt,
    was von außen kommt, also Halt am Boden.
  - Ein Kind zählt ab seiner Geburtsposition: Die Platzierung vor dem Elternteil zählt nicht, sein
    erster Stoß gegen den Elternteil hebt sich aber wie jede innere Kraft auf.
  - Geburt ist impulsneutral: Eltern und Kind teilen sich die Geschwindigkeit des Elternteils (je die
    Hälfte). Bis 28.10.2026 nur in der Rennbahn (`onBirth`), seitdem allgemein bei jeder Teilung
    (`TEILUNG.md`).
  - Der Fortschritt zählt erst ab 35 s (Wachstum 0–30 s, Ruhe 30–35 s, Lauf 35–60 s). Mit Reibung schiebt
    eine Geburt den Verband an (Überlappung, die hinteren Zellen haften): 1,95 px in 20 s, wenn er mitzählt.
    Ohne die Phasen würde die Auswahl erst den Geburtsschub optimieren.
  - Episodenlänge einstellbar (30.10.2026, Nutzerwunsch: die Körper brauchen mehr Zeit zum Bewegen):
    `raceEpisodeSeconds`, GUI „RACE episode length (s)“ im Umweltpanel, 45–300 s, Standard 60 s, gespeichert in
    `<datei>.cells.settings`. Wachstum und Ruhe bleiben bei 30 bzw. 5 s, nur die Laufphase wird länger. Die
    Länge wird zu Beginn jeder Episode gelesen; eine Änderung gilt ab der nächsten Startlage. Längere Läufe
    ergeben höhere Werte (der Fortschritt wird aufsummiert), Rekorde und Bestwertungen verschiedener Längen sind
    also nicht vergleichbar, auch nicht innerhalb eines Laufs, in dem die Länge geändert wurde.
  - Der Wert ist der Betrag des Mittelwerts dieses Fortschritts über die letzten 10 s der 60-s-Episode (alle
    0,5 s), belohnt also anhaltende Bewegung, keinen kurzen Ausschlag. Links und rechts zählen gleich; jede
    Startlage wird für sich gewertet, ein Körper darf also aus verschiedenen Lagen in verschiedene Richtungen
    laufen. Wer nur hin und her wackelt, kommt im Mittel nicht weg und bekommt wenig.
  - Verlässt eine Zelle ihre Bahn, bekommt der Körper 0.
- **Auswahl: Inselmodell mit Katastrophen** (Nutzervorgabe 24.10.2026: echte Evolution mit den
  Standardwerkzeugen gegen lokale Optima, kein Lehrplan):
  - **Inseln:** 4 Inseln zu je 8 Bahnen. Jede wählt für sich aus, nur Migranten wandern.
  - **Elite mit Neubewertung:** Der Beste jeder Insel startet unverändert in ihrer ersten Bahn. Wegen der
    zufälligen Startdrehung und -lage wird er dabei neu bewertet: Er bleibt nur, wenn er sich wieder bewegt.
    Vorher wurde die Elite deterministisch wiederholt, ein einzelner Ruck (0,85 px) hielt die Auswahl 99
    Generationen lang fest.
  - **Turnier:** Die übrigen Bahnen erhalten mutierte Gründer, jeweils der Beste aus 3 zufällig gezogenen
    Körpern der eigenen Insel (statt immer aus den besten drei: weniger Gleichschritt).
  - **Migration:** Alle 10 Generationen tritt der Beste jeder Insel unverändert in der letzten Bahn der
    nächsten Insel an (Ring) und muss sich dort gegen die Einheimischen durchsetzen.
  - **Katastrophe:** Hat eine Insel ihren Rekord 30 Generationen lang nicht um mehr als 10 % + 0,1 px
    verbessert (der Rand trennt Fortschritt vom Rauschen des Zufallsstarts), wird sie vollständig mit neuen
    Zufallsgenomen gefüllt und ihr Rekord gelöscht. Höchstens eine Insel je Generation, die am längsten
    stillstehende; ihre besten Gene leben über frühere Migranten auf den Nachbarinseln weiter.
  - **Rekord als gleitendes Mittel, Schutz der führenden Insel** (seit 26.10.2026, Nutzerentscheidung nach der
    600er-Studie): Der Rekord einer Insel ist ihr bestes Mittel der Bestwerte der letzten 10 Generationen, nicht
    mehr eine einzelne Spitze. Eine Glücksgeneration hebt ihn nur um ein Zehntel ihres Ausschlags. Die Insel mit
    dem höchsten aktuellen Mittel wird nie gelöscht.
  - **Mehrere Startlagen** (seit 25.10.2026, Nutzerentscheidung): Eine Generation besteht aus 3 Episoden.
    Dieselben Gründer (unveränderte Kopien) laufen aus 3 zufälligen Startlagen; gewertet wird der Mittelwert.
    Ein Ruck aus einer einzigen Lage zählt damit nur noch ein Drittel, auch für den Rekord der Katastrophe.
    Eine Generation dauert dreimal so lange (3 Minuten Simulationszeit).
  - Alle starten frisch als einzelne Zelle.
  - Konsolenzeile `RACE generation=… score=… lastProgress=… cells=… median=… islands … catastrophes=…`
    (Mittelwerte, Bestwert je Insel) und `RACE catastrophe: island …`; Status in der GUI mit Start 1–3 und Zahl
    der Katastrophen.
- **Import:** Ein Genom je Bahn aus den geladenen Zellen, danach Neustart der Episoden.

## Warum Schieben durch Masse nicht zählt

`RaceTrainStrategyTest` lässt jede Zelle sich nach rechts teilen (Aktor 0):

| | Ergebnis |
|---|---|
| Zellen nach 20 s | 6 je Bahn (Obergrenze) |
| Vorderste Zelle | +77 px (mit Reibung; ohne +123 px) |
| Eigenfortschritt | 0,000 px (vor der impulsneutralen Geburt −0,4 px) |
| Danach 10 s Lauf | 0,000 px |
| Geburten mitgezählt (nur Prüfung) | 1,95 px: Geburtsschub über Reibung |

Weitere Prüfungen:
- Bahnen und Wände, Schalter.
- Ein Neuling weit vorn zählt nicht, danach zählt er wie alle; Verlassen der Bahn ergibt 0.
- Ein von Hand gleichmäßig bewegter Körper wird Elite in der ersten Bahn seiner Insel (unverändert,
  frische Gründer).
- Ein Körper, der in allen drei Startlagen gleichmäßig 4 px/s läuft (80 px), schlägt einen, der nur in einer
  Lage 10 px/s läuft (200 px, im Mittel 67 px).
- Inseln: Elite je Insel, Turniere nur innerhalb der Insel, Migration im Ring; eine Insel ohne Rekord über
  den Rauschrand hinaus wird nach 30 Generationen neu gewürfelt, immer nur eine je Generation;
  Gründer starten mit verschiedener Drehung.

## Erste Studien (24.10.2026, Seed 4711, Zufallsgründer)

`RaceTrainStrategyTest#study`, Dauer etwa 2,5 s je Generation.

- **Erster Lauf, 20 Generationen, mit zwei Lücken in der Bewertung:**
  - Generation 1–13: ohne einen Anker ein kleiner, stetig wachsender Fortschritt (0,65 → 0,85 px),
    vom Median sofort übernommen.
  - Ursache 1: Ein Kind zählte erst ab seinem zweiten Schritt. Sein erster Stoß gegen den Elternteil
    zählte nur einseitig, ein hinten abgelegtes Kind schob den Elternteil gratis nach vorn.
  - Ursache 2: Ein Kind erbt die Geschwindigkeit des Elternteils, das ist zusätzlicher Impuls.
  - Ab Generation 14 entstanden Anker (bis 37 je Stichprobe), der Bestwert stieg auf 35 px. Ob das
    echtes Kriechen war oder die Lücken weiter wirkten, ist offen.
  - Beides ist behoben; die Lücken zeigen, wie schnell die Auswahl solche Schlupflöcher findet.
- **Zweiter Lauf, 30 Generationen, Bewertung dicht:**
  - Fortschritt in allen Generationen 0,000 px, im Median ebenso.
  - Anker nur in 4 Generationen (1,5–3,5 je Stichprobe).
  - Die Auswahl ist damit rein zufällig: **Startproblem bestätigt.**

## Studie mit Geländereibung (24.10.2026, Seed 4711, Zufallsgründer, Modellstand `f0ac137`)

Reibung 0,5, Rollwiderstand 0,5, Anker weiter an, Wachstum/Ruhe/Lauf wie oben, 100 Generationen.

- Generation 2 findet einen Körper mit 0,85 px. Er bleibt bis Generation 100 unverändert Elite in Bahn 0;
  keiner der etwa 1500 Mutanten übertrifft ihn.
- Sein Verlauf im Lauf: 35 s 0,00 · 40 s 0,01 · 45 s 0,02 · 50 s 0,86 · 55 s 0,85 px. Das ist ein einzelner
  Ruck zwischen 45 und 50 s, kein Gang.
- Median 0,000–0,020 px, kein einziger Anker in 100 Generationen.
- **Befund:** Die Reibung macht Vortrieb physikalisch leicht möglich (Reibungsgang 21 px in 30 s, doppelt
  so viel wie mit Ankern). Aus Zufallsgehirnen entsteht er in 100 Generationen trotzdem nicht. Das
  Startproblem liegt also nicht mehr in der Physik, sondern in der Steuerung: graue, anziehende Fußaktoren
  im Wechsel mit Loslassen und Schieben in einem Takt.

## Studie mit Inselmodell (24./25.10.2026, Seed 4711, Zufallsgründer, Modellstand `ab64ff7`)

Geplant waren 1000 Generationen. Der Lauf wurde nach 587 Generationen abgebrochen, weil der Rechner wenig
Speicher hatte (Claude Code beendet dann Hintergrundprozesse); der Abbruch lag nicht an der Simulation.

- 58 Katastrophen in 587 Generationen, also etwa alle 10 Generationen eine. Jede Insel wurde im Schnitt
  alle 40 Generationen neu gewürfelt.
- Bestwert je Generation: 46 von 587 über 0,1 px, 10 über 0,5 px, 7 über 1 px. Spitzen: 5,27 px (G33),
  4,07 px (G362), 5,47 px (G418), dazu 2,28, 2,23, 1,31, 1,20 px.
- **Keine Spitze hat sich wiederholt.** In der folgenden Generation startet derselbe Körper unverändert,
  nur mit anderer Startdrehung und -lage, und kommt auf 0,00–0,02 px (G34 nach 5,27 px, G363 nach 4,07 px,
  G419 nach 5,47 px). Auch die mutierten Nachkommen bauen nicht darauf auf.
- Median durchgehend 0,000 px. In 278 von 587 Generationen setzte in der ganzen Welt keine Zelle einen Anker.
- **Befund:**
  - Die Neubewertung tut, wofür sie gedacht war: Ein Glückstreffer hält die Auswahl nicht mehr fest.
  - Die Spitzen sind aber Glückstreffer einer bestimmten Startlage, keine Gänge.
  - Die Katastrophen kommen so regelmäßig, weil der Rekord einer Insel meist von einem solchen Treffer
    stammt, den keine spätere Generation übertrifft. Bei einem Median von 0 wird die Auswahl damit im
    Wesentlichen zu einer Zufallssuche mit regelmäßigem Neustart.
- **Mögliche nächste Schritte (nicht umgesetzt):**
  - Jedes Genom aus mehreren Startlagen bewerten und Mittel oder Minimum werten, wie es bei verrauschter
    Fitness üblich ist. Das kostet entsprechend mehr Rechenzeit je Generation.
  - Den Rekord für die Katastrophe aus diesen wiederholten Bewertungen nehmen statt aus einer Spitze.

## Taktgeber: Können die Gehirne überhaupt schwingen? (25.10.2026)

Beobachtung des Nutzers nach etwa 240 Generationen in der GUI: Ein Gang braucht Aktoren oder Größe, die sich
zyklisch ändern; das war nie zu sehen. Die Studie zählt das jetzt (`oscillating`: Gründer, deren Aktor im Lauf
mindestens viermal zwischen Ziehen und Drücken wechselt; `pulsing`: Radius schwankt um mehr als 2 px).
Erste zwei Generationen: 0 von 32 und 0 von 32.

Befund aus dem Code (`NeuralNet`, `CellBrain`):
- Zufallsgehirne sind beim Anlegen reine Vorwärtsnetze. Rückwärtskanten entstehen erst durch die Mutation
  `addRandomSynapse` (die Quellschicht darf hinter der Zielschicht liegen).
- Alle neuen Synapsen haben Gewichte von höchstens ± 0,001. Die Ausgänge eines Zufallsgehirns hängen damit
  praktisch nur von den Biaswerten ab und sind konstant; die Eingänge wirken kaum.
- Es gibt keinen Takteingang. Das Alter (`MY_CELL_AGE`, 1 − 60/(60 + Alter)) steigt nur langsam und
  gleichmäßig an.
- Ein Takt kann also nur entstehen, wenn die Mutation eine Rückkopplung mit großen Gewichten aufbaut oder
  wenn ein Regelkreis über die Sensoren läuft (Bewegung → Sensor → Aktor), und beides erst aus großen
  Gewichten. Das erklärt, warum sich nichts zyklisch ändert.
- Umgesetzt (Nutzerentscheidung 25.10.2026): sechs vererbte Taktgeber als Sinus/Kosinus des Zellalters,
  `TAKTGEBER.md`. Zufallsgehirne spüren sie wegen der kleinen Anfangsgewichte (± 0,001) noch kaum; in den
  ersten 4 Generationen schwang kein Körper.

## Studie mit Taktgebern (25.10.2026, Seed 4711, Zufallsgründer, Modellstand `a8c7fa5`, Schwerkraft 10 %)

300 Generationen, jede aus 3 Startlagen (Mittelwert), Schwerkraft auf 10 % des Standards wie in der GUI des
Nutzers (`-Drace.study.gravity=0.1`). Wegen der anderen Schwerkraft und der Mittelung nicht direkt mit den
Studien oben vergleichbar.

- **Kein Körper hat geschwungen oder pulsiert:** `oscillating 0` und `pulsing 0` in allen 300 Generationen.
- Bestwert (Mittel aus 3 Starts): über 1 px in 267, über 2 px in 85, über 3 px in 13 Generationen; Spitze
  4,56 px (G12), 4,25 px (G132), 4,07 px (G295).
- **Kein Aufwärtstrend:** Mittel des Bestwerts je 50 Generationen 1,60 · 1,56 · 1,82 · 1,68 · 1,72 · 1,78 px.
- Spitzen halten sich nicht: Die Elite der Spitzengeneration fällt in der nächsten auf 2,0 bzw. 1,4 px
  (Regression zur Mitte).
- Median fast immer 0 (einmal über 0,01 px). Anker im Mittel 4,1 je Messpunkt; in 45 von 300 Generationen keiner.
- 23 Katastrophen, also seltener als im Inselmodell ohne Mittelung (58 in 587 Generationen).
- **Befund:** Die gemittelten 1–2 px in 25 s sind gleichmäßiges Abdriften (bei 10 % Schwerkraft rutscht und
  rollt ein Körper leichter), kein Gang. Die Taktgeber haben in 300 Generationen keinen schwingenden Aktor
  hervorgebracht.
- **Ursache (geprüft mit `ActorRhythmTest`, `TAKTGEBER.md`):** Der Signalweg samt Tonus lässt Takte ab 1 s als
  Vorzeichenwechsel durch. Aber die Zufallsgehirne reichen den Takt über mehrere Schichten mit kleinen Gewichten
  kaum weiter: Die Aktorkraft schwankt durch die Takte im Median um 0,002 von 240. Auch 300 Mutationen ohne
  Auswahl ändern daran nichts, und ein schwaches Wackeln bringt der Auswahl keinen Vorteil.
- **Mögliche Hebel (Entscheidung Nutzer):** größere Anfangsgewichte für die Taktsynapsen; prüfen, ob der Tonus
  der Aktorkraft schnelle Wechsel überhaupt durchlässt; Schwingen als Zwischenziel wäre Training und ist verworfen.

## Studie mit direkten Taktsynapsen (25.10.2026, Seed 4711, Zufallsgründer, Modellstand `580c9a2`, Schwerkraft 10 %)

Gleiche Einstellungen wie die Studie mit Taktgebern oben (300 Generationen, 3 Startlagen, 10 % Schwerkraft); neu
sind nur die direkten Taktsynapsen neuer Gehirne (`TAKTGEBER.md`).

- **Erstmals ein stetiger Fortschritt.** Mittel des Bestwerts je 50 Generationen (Mittel aus 3 Starts):
  5,7 · 7,6 · 9,3 · 11,4 · 12,9 · 12,6 px (vorher 1,6–1,8 px ohne Trend). Bestwert über 10 px in 152 von 300
  Generationen, Spitze 19,9 px (G287); nie über 20 px.
- **Die ganze Population läuft:** Der Median aller Bahnen steigt von 0,5 auf etwa 4,5 px je Block (vorher 0); in
  259 von 300 Generationen über 0,5 px, nur einmal unter −0,5 px.
- Alle 32 Körper schwingen und pulsieren in jeder Generation.
- **Die Elite wiederholt ihre Leistung meist:** In der ersten Startlage legt sie in den letzten Generationen
  typischerweise 6–24 px in 25 s gleichmäßig zurück (bis etwa 1 px/s), aus manchen Lagen aber auch rückwärts
  (bis −10 px). Die Richtung hängt also noch teils von der Startdrehung ab.
- **Ohne Anker:** Anker je Messpunkt sanken von 1,2 auf 0–0,6; in den späten Generationen meist keiner. Der
  Vortrieb kommt aus Formänderung (Pulsieren, Aktorwechsel) und Geländereibung, nicht aus Haften am Boden.
- 18 Katastrophen (vorher 23). Ab etwa Generation 200 flacht der Anstieg ab (12–13 px).
- **Befund:** Die direkten Taktsynapsen haben die Sperre gelöst. Aus schwingenden Zufallskörpern wählt die
  Evolution solche mit Netto-Vortrieb aus und verbessert sie über Generationen. Die Geschwindigkeit ist noch
  gering (etwa 0,5 px/s im Mittel der Bestwerte), und die Laufrichtung ist nicht in allen Startlagen stabil.

## Studie beidseitig, 600 Generationen (25./26.10.2026, Seed 4711, Modellstand `b8d2ba5`, Schwerkraft 10 %)

Start in der Bahnmitte, Wertung des Betrags (beide Richtungen), sonst wie die Studie mit direkten Taktsynapsen.
Werte deshalb etwas höher als dort (Rückwärtsläufer zählen jetzt).

| Generationen | Bestwert (Mittel) | Maximum | Median |
|---|---|---|---|
| 1–50 | 14,5 | 28,6 | 4,6 |
| 51–100 | 21,6 | 35,2 | 7,9 |
| 101–150 | 26,3 | 40,3 | 10,2 |
| 151–200 | 29,8 | 44,8 | 11,4 |
| 201–250 | 21,3 | 29,4 | 7,1 |
| 251–600 (je 50) | 19,8–25,8 | 28–38 | 7,7–12,0 |

- **Anstieg bis Generation 200, danach Plateau** bei 20–26 px (etwa 1 px/s), nie über 45 px. Alle 32 Körper
  schwingen; Anker selten (0,2–3 je Messpunkt).
- Die Elite läuft in der ersten Startlage am Ende im Mittel 11 px weit, in 39 von 100 Generationen nach links.
  Beide Richtungen werden also genutzt.
- **40 Katastrophen**, etwa alle 15 Generationen eine, jede Insel rund alle 60 Generationen.
- **Befund, Ursache des Plateaus (wahrscheinlich):** Der Inselrekord stammt aus einer einzelnen Spitzengeneration
  (der Mittelwert aus 3 Starts streut stark, etwa 25–45 px für dieselbe Linie). Ein solcher Rekord wird danach
  kaum um 10 % übertroffen, und nach 30 Generationen wird die Insel gelöscht, auch wenn sie die beste ist.
  Beispiel: Insel 2 setzte in G171 den Rekord 44,8 px, lief danach stabil bei 23–34 px und wurde in G202 gelöscht;
  der Bestwert der Welt fiel von etwa 30 auf 20 px. Die Katastrophe räumt damit gerade die erfolgreichen Linien ab.
- **Abhilfe (umgesetzt 26.10.2026, Nutzerentscheidung):** Rekord aus dem gleitenden Mittel über 10 Generationen,
  die führende Insel wird nie gelöscht (oben, Auswahl). Nicht umgesetzt: Elite der gelöschten Insel als Migrant.

## Studie mit gleitendem Rekord, 600 Generationen (26.10.2026, Seed 4711, Modellstand `d0f5037`, Schwerkraft 10 %)

Wie die beidseitige Studie oben, aber Rekord als gleitendes Mittel über 10 Generationen und führende Insel geschützt.

| Generationen | Bestwert (Mittel) | Maximum | Median | vorher (Spitzenrekord): Bestwert |
|---|---|---|---|---|
| 1–50 | 15,6 | 27,2 | 4,8 | 14,5 |
| 51–100 | 19,5 | 32,0 | 6,4 | 21,6 |
| 101–150 | 21,3 | 33,2 | 6,0 | 26,3 |
| 151–200 | 20,3 | 31,2 | 7,5 | 29,8 |
| 201–250 | 21,7 | 32,9 | 8,0 | 21,3 |
| 251–300 | 18,9 | 31,1 | 5,8 | 24,3 |
| 301–350 | 21,7 | 31,8 | 8,1 | 23,7 |
| 351–400 | 22,0 | 32,8 | 7,4 | 25,8 |
| 401–450 | 26,1 | 35,7 | 9,4 | 25,1 |
| 451–500 | 27,5 | 42,1 | 9,9 | 19,8 |
| 501–550 | 35,2 | 54,9 | 12,6 | 25,0 |
| 551–600 | 33,9 | 44,8 | 12,0 | 21,9 |

- **Kein festes Plateau mehr:** Nach langsamerem Beginn steigt der Bestwert ab etwa Generation 400 weiter, auf
  34–35 px im Mittel der letzten 100 Generationen (vorher 22–25 px) und bis 54,9 px (G524, vorher höchstens
  44,8 px). Median zuletzt 12 px (vorher 8,6–10,5 px). Bestwert über 40 px in 21 Generationen, über 50 px in 2.
- 32 Katastrophen (vorher 40), jetzt etwa gleichmäßig verteilt; die führende Insel blieb verschont.
- Alle 32 Körper schwingen; Anker fast keine (0–1,7 je Messpunkt).
- **Grenzen:** je ein Lauf mit einem Seed, daher keine gesicherte Aussage über den Beginn (langsamer als vorher
  ist vermutlich Zufall des Verlaufs). Ob der Anstieg am Ende weitergeht, zeigt erst ein längerer Lauf.

## Langlauf 2000 Generationen, abgebrochen nach 681 (26./27.10.2026, Seed 4711, Modellstand `d0f5037`, 10 %)

Gleiche Einstellungen wie die Studie mit gleitendem Rekord. Claude Code hat den Lauf nach 681 Generationen wegen
Speichermangels des Rechners beendet (nicht wegen der Simulation).

| Generationen | Bestwert (Mittel) | Maximum | Median |
|---|---|---|---|
| 1–100 | 16,3 | 34,3 | 5,2 |
| 101–200 | 33,8 | 55,0 | 12,9 |
| 201–300 | 47,7 | 72,1 | 20,8 |
| 301–400 | 46,2 | 58,4 | 22,2 |
| 401–500 | 45,6 | 58,9 | 22,0 |
| 501–600 | 50,9 | 61,4 | 24,8 |
| 601–681 | 50,7 | 64,7 | 21,8 |

- Trotz gleichem Seed ein anderer Verlauf als die 600er-Studie: Die Simulation ist nicht bitgleich wiederholbar
  (parallele Schritte), schon kleine Abweichungen führen zu anderen Linien. Die beiden Läufe sind damit zwei
  unabhängige Stichproben derselben Einstellung.
- Hier schneller Anstieg bis etwa Generation 250 (Bestwert ~47 px, Spitze 72,1 px in G230), danach Plateau mit
  leichtem Anstieg auf ~51 px; Median 20–25 px. 39 Katastrophen bis G681.
- Zusammen mit der 600er-Studie (Ende 34–35 px): Die Rennbahn erreicht mit dieser Einstellung 35–50 px im
  Mittel der Bestwerte (etwa 2 px/s); ob es danach weiter steigt, ist offen.

## Grenzen und offene Hebel

- **Startproblem:** Zufällige Gehirne müssen graue, anziehende Fußaktoren, Lösen durch Abstoßen und
  einen passenden Takt gleichzeitig finden. Anfangs gibt es kaum Anker und praktisch keinen Fortschritt,
  die Auswahl ist dann fast zufällig. Mit Reibung bleibt das so (Studie oben).
- **Auswahl:** Bis `ab64ff7` wurde die Elite unverändert wiederholt und ergab deterministisch denselben Wert;
  ein einzelner Glückstreffer hielt die Auswahl fest. Seitdem Inselmodell mit Neubewertung und Katastrophen
  (oben).
- **Hebel:**
  - Umgesetzt (Nutzerentscheidung 24.10.2026): Geländereibung als reine Physik, `BODENREIBUNG.md`.
  - Umgesetzt (Nutzerentscheidung 24.10.2026): Inseln, Turnier, Zufallsstart, Migration, Katastrophe.
  - Verworfen (Nutzer: nur echte Evolution, kein Training): Lehrplan, etwa erst „Anker setzen und lösen“ belohnen.
  - Nicht umgesetzt: Aktorfarbe für Fußaktoren vorgeben.
- Die Anker sind weich und kurz; schnelle Läufer sind mit der jetzigen Physik nicht zu erwarten.
  Entscheidend ist das Vorzeichen der Bewegung über viele Generationen.
- Gleiche Massen sind Voraussetzung der Bewertung. Würde die Masse später größenabhängig, müsste
  der Fortschritt massengewichtet gerechnet werden.

## Tests

```powershell
mvn test '-Dtest=BlockerFrictionTest,RaceGaitTest,RaceTrainStrategyTest,TrainingAdhesionTest'
# Opt-in: echte Generationen mit Zufallsgründern
mvn test '-Dtest=RaceTrainStrategyTest#study' '-Drace.study.generations=20'
```
