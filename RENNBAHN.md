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
  - 16 Bahnen à 1600 px nebeneinander (25 600 × 800 px), Boden, an jedem Bahnanfang eine Wand
    über die volle Höhe.
  - Der Gründer startet 400 px hinter seiner Bahnwand, damit ein Haufen nicht von der Wand abstoßen kann.
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
- **Auswahl:**
  - Bester Körper unverändert in Bahn 0.
  - Die übrigen 15 Bahnen erhalten mutierte Gründer, zufällig aus den besten drei.
  - Alle starten frisch als einzelne Zelle.
  - Konsolenzeile `RACE generation=… score=… progress=… cells=… median=…`, Status in der GUI.
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
- Ein von Hand gleichmäßig bewegter Körper wird Elite in Bahn 0 (unverändert, frische Gründer).

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

## Grenzen und offene Hebel

- **Startproblem:** Zufällige Gehirne müssen graue, anziehende Fußaktoren, Lösen durch Abstoßen und
  einen passenden Takt gleichzeitig finden. Anfangs gibt es kaum Anker und praktisch keinen Fortschritt,
  die Auswahl ist dann fast zufällig.
- **Hebel:**
  - Umgesetzt (Nutzerentscheidung 24.10.2026): Geländereibung als reine Physik, `BODENREIBUNG.md`.
  - Nicht umgesetzt: Lehrplan wie HIGH_POSITION3, etwa erst „Anker setzen und lösen“ belohnen.
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
