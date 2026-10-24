# Rennbahn (TrainMode RACE)

Status: **experimentell umgesetzt** (24.10.2026). Frage des Nutzers: Können Zellen im Verbund eine
Funktion entwickeln, konkret sich gemeinsam fortbewegen? Viele parallele Bahnen, Gewinner ist,
wer am weitesten von links nach rechts kommt. Die Zellen sollen sich normal teilen, aber nicht
durch schiere Anzahl nach rechts drücken; gewünscht ist ein kleiner, sich aktiv vermehrender Verbund.

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
  - Der Gründer startet 400 px (± 40 px zufällig) hinter seiner Bahnwand, damit ein Haufen nicht von der Wand
    abstoßen kann, und mit zufälliger Drehung.
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
    Hälfte). Im allgemeinen Modell erbt das Kind die volle Geschwindigkeit, das schafft Impuls aus dem
    Nichts. Nur die Rennbahn ändert das, in `onBirth`.
  - Der Fortschritt zählt erst ab 35 s (Wachstum 0–30 s, Ruhe 30–35 s, Lauf 35–60 s). Mit Reibung schiebt
    eine Geburt den Verband an (Überlappung, die hinteren Zellen haften): 1,95 px in 20 s, wenn er mitzählt.
    Ohne die Phasen würde die Auswahl erst den Geburtsschub optimieren.
  - Der Wert ist der Mittelwert dieses Fortschritts über die letzten 10 s der 60-s-Episode (alle 0,5 s),
    belohnt also anhaltende Bewegung, keinen kurzen Ausschlag.
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
  - Alle starten frisch als einzelne Zelle.
  - Konsolenzeile `RACE generation=… score=… progress=… cells=… median=… islands … catastrophes=…` (Bestwert
    je Insel) und `RACE catastrophe: island …`; Status in der GUI mit Zahl der Katastrophen.
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
