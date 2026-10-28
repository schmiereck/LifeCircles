# Reibung am Gelände

Status: **umgesetzt** (24.10.2026), Schalter `blockerFriction`, **Standard an seit 28.10.2026**
(Nutzerentscheidung, damit Kriechen am Boden in allen Welten möglich ist); RACE schaltet ihn beim Anlegen ein.
Gespeicherte Einstellungen mit `blockerFriction=false` bleiben aus. Mechanik-Benchmarks (`MechanicsLab`) und
Ankertests (`GroundHoldTest`) laufen weiter auf reibungsfreiem Gelände, gegen das sie kalibriert wurden. Nutzervorgabe: Reibung an den Blockern soll als reine Physik wirken, ohne
ausdrückliche Anker. Anker (`BODENHALT.md`) bleiben unverändert und können zusätzlich wirken.

## Ausgangslage

- Bisher entfernte die Blockerkollision nur die Geschwindigkeit in die Oberfläche hinein; das Gelände war
  reibungsfrei. Kräfte zwischen Zellen heben sich auf, ein Verband konnte sich deshalb nur über Anker
  fortbewegen (`RENNBAHN.md`). Anker zu finden ist für zufällige Gehirne sehr schwer (Startproblem).

## Modell

`BlockerCellCalcService.handleBlockerCollisions(cell, blockers, world, friction, rolling, dt)`:

- **Normalimpuls:** die Geschwindigkeit in die Oberfläche hinein, die die Kollision ohnehin entfernt
  (Schwerkraft, Andrücken durch das Bodenfeld anziehender Aktoren, Druck von Nachbarzellen).
  Ohne Andrücken gibt es keine Reibung, auch nicht bei Berührung.
- **Gleitreibung (Coulomb):** Der Kontaktpunkt (Rand gegenüber der Normalen) bewegt sich mit Zelle und
  Drehung. Ein tangentialer Impuls bremst ihn bis zum Stillstand, höchstens
  `Reibungskoeffizient × Normalimpuls` an linearer Geschwindigkeit. Er ändert Translation und Drehung
  gemäß `Cell.applyForce` (gleiche lineare Masse 0,08, Drehträgheit über `CELL_ANGULAR_VELOCITY_DIFF · r²`).
  Drehung wird so zu Rollen und Rollen zu Drehung.
- **Rollwiderstand:** Eine Zelle ist kein starres Rad, sondern liegt mit einer abgeplatteten
  Kontaktfläche auf. Deren halbe Breite relativ zum Radius begrenzt ein Gegendrehmoment auf
  `Rollwiderstand × Normalkraft × r`. Ohne ihn rollt eine mittig gezogene Zelle bei jeder Kraft weg
  (Rad), statt zu haften.
- **Gemeinsame Lösung:** Gleiten und Rollen werden zusammen gelöst (projiziertes Gauß-Seidel, Fehler
  schrumpft je Durchgang auf < 0,4). Reichen beide Grenzen, haftet die Zelle ganz; sonst rollt sie mit
  Widerstand oder gleitet.
- **Innerhalb des Schritts:** Die Kollision läuft nach der Positionsfortschreibung. Wie die Normalprojektion
  korrigiert die Reibung deshalb Position und Drehwinkel um ihre Geschwindigkeitsänderung × dt; sonst
  kriecht eine haftende Zelle jeden Schritt um den Zug dieses Schritts weiter (2,8 px in 3 s bei 0,4 g).
- Alle Blocker reiben (Boden, Wände, Plattformen). An einer Wand braucht Haften Andrücken durch das
  Bodenfeld, die Schwerkraft drückt nicht gegen sie.
- **Config:** `blockerFriction` (Schalter), `blockerFrictionCoefficient` 0,5, `blockerRollingResistance`
  0,5. Gespeichert in der `.cells.settings`-Datei. GUI im Umweltpanel unter „Mechanics“: Kästchen und zwei
  Regler (Reibung 0–3, Rollwiderstand 0–1).

## Prüfungen (`BlockerFrictionTest`, eine Zelle r = 20, Standardschwerkraft)

| Prüfung | Ergebnis |
|---|---|
| 100 px/s seitlich, 3 s, ohne Viskosität | ohne Reibung 300 px, mit Reibung 1: 39 px |
| ohne Schwerkraft | mit und ohne Reibung gleich (kein Andrücken) |
| mittiger Zug 0,4 g | 0,000 px (haftet, kein Kriechen) |
| mittiger Zug 0,8 g | 21 px (rollt über den Rand der Kontaktfläche weg) |
| mittiger Zug 0,8 g, zusätzlich 1 g angedrückt | 0,000 px |
| Drehstoß 2 rad/s, 2 s | ohne Reibung 0 px; als Rad 11,3 px ohne Schlupf; mit Rollwiderstand 4,2 px |

## Gang mit Reibung (`RaceGaitTest`)

Derselbe von Hand geregelte Zwei-Zellen-Gang wie für die Anker, Fußaktoren grau. Das Bodenfeld der drei
greifenden Fußaktoren drückt die Zelle mit etwa 4 g an, die Schiebekraft zwischen den Zellen beträgt etwa 1 g.
Die greifende Zelle haftet, die losgelassene gleitet oder rollt.

- Reibungsgang mit vier Takten zu je 0,5 s:
  1. vorn loslassen und vorn wegschieben;
  2. beide greifen;
  3. hinten loslassen und nachziehen;
  4. beide greifen.
  Die Haltetakte sind nötig, weil der Griff mit dem Tonus (0,5 s) aufgebaut wird.
- Der Ankergang lässt beim Schieben beide greifen. Anker rasten erst nach 0,5 s ein, mit Reibung hält
  dagegen auch die vordere Zelle sofort.
- Der Abstandsregler misst die Öffnung aus der tatsächlichen Abstandsänderung. Die Geschwindigkeit enthält
  vor der Kollision noch den Schub, den die Reibung gleich wieder nimmt. Der Regler hatte sich deshalb
  vorher selbst auf etwa 4 % gedrosselt.

| 30 s, ohne Anker | Reibung 0,25 | 0,5 | 1,0 |
|---|---|---|---|
| Reibungsgang (2 s) | 17,4 px | **21,2 px** | 5,0 px |
| Ankergang (4 s) | 12,5 px | 3,5 px | 3,9 px |

Zum Vergleich: der Ankergang mit Ankern ohne Reibung erreicht 10,8 px, ohne Anker und ohne Reibung 0 px.
Bei hoher Reibung haftet auch die losgelassene Zelle (Gewicht allein reicht gegen 1 g Schub). Die
Standardwerte 0,5/0,5 waren im Raster (Rollwiderstand 0–0,5 × Reibung 0,25–1,5) für diesen Gang am besten.
Ohne Rollwiderstand rollt die gegriffene Zelle weg und der Gang läuft sogar rückwärts (−0,7 bis −5,5 px).

## Folgen für die Rennbahn

- Mit Reibung schiebt eine Geburt den Verband an: Das Kind wird überlappend abgelegt, die Abstoßung drückt
  die Zellen auseinander, die hinteren haften. Gemessen 1,95 px in 20 s Teilung nach rechts (vor der
  Flächenteilung vom 28.10.2026, `TEILUNG.md`; seitdem −0,3 px). Das ist echte
  Physik (Wachstumsdruck), aber genau das Schieben durch Anzahl, das nicht zählen soll.
- Deshalb hat jede RACE-Episode jetzt drei Phasen:
  - **Wachstum** 0–30 s: Teilung erlaubt; bei 8 s Teilungsalter reicht das für 6 Zellen.
  - **Ruhe** 30–35 s: keine Teilung, der Geburtsschub klingt ab.
  - **Lauf** ab 35 s: Fortschritt zählt; Wertung wie bisher als Mittel über die letzten 10 s.
- Prüfung: Teilung nach vorn bis 6 Zellen, danach 10 s Lauf ergibt 0,000 px.

## Grenzen

- Der Normalimpuls stammt aus einem Schritt; eine Zelle, die nur am Rand eines Blockers streift, reibt nur,
  solange sie hineingedrückt wird.
- Die Reibung wirkt in jedem TrainMode, sobald der Schalter an ist. Gespeicherte Läufe ohne den Eintrag
  behalten den aktuellen Wert.
- Nicht bitgleich zu Läufen ohne Reibung; mit Schalter aus ist das Verhalten unverändert
  (`IntegrationEquivalenceTest`, `PhysicsRegressionTest`).
