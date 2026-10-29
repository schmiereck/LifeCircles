# Teilung und Wachstum

Status: **umgesetzt** (28.10.2026), gilt in allen Welten. Nutzerentscheidung nach Beobachtung großer Welten
mit wenig Schwerkraft und Brownscher Bewegung: Populationen bewegten sich fast nur durch schnelles Teilen. Das
Kind wurde von der Mutter weggeschleudert („fliegen“). Das verhinderte stabile Strukturen, und nur das
Kollektiv lebte (ständiges Teilen, schnelles Sterben).

## Ursache (Messung `DivisionMechanicsTest`, vor der Änderung)

- Teilung: Die Mutter behielt ihre volle Größe. Das Kind entstand mit Radius 0,5 an ihrem Rand und wuchs in 1 s
  auf seine Zielgröße. Es entstand also Fläche aus dem Nichts.
- Ausgewachsene Zellen folgten dem Größenbefehl des Gehirns mit einer Zeitkonstante von 100 ms, ohne
  Kosten und ohne Grenze.
- Die Abstoßung ist eine steife Feder (125 je px Überlappung). Die Viskosität lässt Geschwindigkeit erst nach
  etwa 0,26 s abklingen, die Zellen gleiten also nach.

| Paar ohne Schwerkraft, 5 s | stärkste Trenngeschwindigkeit | Lücke danach |
|---|---|---|
| Teilung r = 20, Kind wächst in 1 s | 36 px/s | 4,7 px |
| zwei Hälften r/√2, die in 100 ms auf r = 20 aufblähen | **153 px/s** | **32 px** |

Das Katapult war vor allem das schnelle Aufblähen. Eine Flächenteilung allein hätte es verstärkt.

## Modell

- **Flächenteilung:** Bei der Teilung behält die Mutter sofort die Hälfte ihrer Fläche (Radius r/√2). Das Kind
  startet mit Radius 0,5 an ihrem neuen Rand. Es liegt damit innerhalb der alten Mutterscheibe und wächst
  zunächst auf die andere Hälfte (Ziel r/√2, mindestens die Mindestgröße 6). Danach bestimmen die Gehirne die
  Größe beider Zellen wie bisher.
- **Wachstumsgrenze:** Keine Zelle wächst schneller als 20 px/s im Radius (`SimulationConfig.CELL_MAX_GROWTH_SPEED`).
  Das gilt für das Geburtswachstum und für ausgewachsene Zellen. Schrumpfen ist nicht begrenzt.
  - Ausgewachsene Zellen: Die Glättung (100 ms) verlangt Lücke/100 ms. Solange die Lücke größer als 2 px ist,
    wächst der Radius deshalb linear mit 20 px/s, danach exponentiell. Das ist exakt gelöst, damit eine
    Unterteilung des Schritts nichts ändert (`SizeChangeTest`).
  - Geburtswachstum: Es bleibt linear über 1 s. Hebt das Gehirn das Ziel unterwegs an, springt der Radius
    nicht mehr, sondern wächst höchstens mit 20 px/s weiter. Vorher war das ein Sprung von bis zu 10 px in einem
    Schritt.
- **Impuls:** Alle Zellen haben dieselbe lineare Masse. Erbte das Kind die Geschwindigkeit, verdoppelte sich der
  Impuls. Mutter und Kind teilen ihn jetzt (je halbe Geschwindigkeit). Das galt bisher nur in RACE (`onBirth`).
  Jetzt gilt es allgemein in `ReproductionManagerService`. Drehung und Drehgeschwindigkeit erbt das Kind wie
  bisher.
- Eine Teilung, die das Gelände blockiert, ändert weder Energie noch Größe der Mutter. Weil das Kind innerhalb der
  alten Mutterscheibe startet, blockiert nur Gelände, das die Mutter schon berührt.
- Trainingsgründer (`createTrainingFounder`) sind frische Episoden und bleiben unverändert: halbe Maximalgröße,
  Geburtswachstum.

## Messung danach (`DivisionMechanicsTest`)

| Paar ohne Schwerkraft, 5 s | stärkste Trenngeschwindigkeit | Lücke danach |
|---|---|---|
| Hälften r/√2, die auf r = 20 aufblähen | 75 px/s (vorher 153) | 10,7 px (vorher 32) |
| Teilung r = 20, beide Gehirne wollen wieder r = 20 | 72 px/s | 5,6 px |
| Teilung r = 8 / 15 | 39 / 64 px/s | 2,1 / 0,8 px |

- Bei einer Teilung wachsen beide Zellen gleichzeitig mit 20 px/s, sie entfernen sich also mit mindestens
  40 px/s. Der Rest ist das Nachschwingen der Feder. Das Gleiten danach ist durch die Viskosität bestimmt.
- Prüfungen:
  - Nach der Teilung hat die Mutter r/√2, das Ziel des Kindes ist die andere Hälfte, und der Impuls ist geteilt.
  - Keine Trennung schneller als 100 px/s.
  - Kein Wachstumsschritt über 20 px/s · dt, auch nicht bei angehobenem Ziel während der Geburt.
  - Schrumpfen ist schnell.
- RACE (`RaceTrainStrategyTest`): Die Geburten schoben einen Verband vorher in 20 s Wachstum um mehr als 0,5 px
  an, jetzt um −0,3 px. Die Wachstumsphase bleibt trotzdem ungewertet.

## Teilungspause

Status: **umgesetzt** (29.10.2026), Nutzerwunsch nach HIGH_POSITION2-Läufen. „Min. Age“
(`reproductionAgeThreshold`) zählt nur ab der Geburt. Danach durfte eine Mutter in jedem Schritt erneut
teilen, solange Energie und Teilungswunsch reichten. Höheres Mindestalter bremste das Hochschieben durch
schnelle Teilung deshalb kaum.

- Neuer Parameter `reproductionCooldown` (GUI „Division pause (s)“, 0–20 s, Standard 0 = wie bisher): Eine Zelle
  darf erst wieder teilen, wenn seit ihrer letzten eigenen Teilung so viel Zeit vergangen ist. Beide Bedingungen
  gelten: Mindestalter ab Geburt und Pause ab letzter Teilung.
- Die Zelle merkt sich ihr Alter bei der letzten Teilung (`Cell.lastDivisionAge`, nur bei erfolgreicher
  Teilung gesetzt). Ein Neugeborenes hat noch nicht geteilt; für es gilt nur das Mindestalter.
- Gilt für alle biologischen Teilungen, auch klonale (RACE, HIGH_POSITION3). Trainingsgründer sind unberührt.
- Alte Speicherstände lesen das Feld als 0, als hätte die Zelle bei Geburt geteilt. Das wirkt nur auf Zellen,
  die jünger als die Pause sind.
- Wie alle Reproduction-Einstellungen steht die Pause seit 29.10.2026 in der Einstellungsdatei (`reproductionCooldown`).
- Test: `PopulationEnergyTest.motherRestsBetweenDivisions`.

## Grenzen und offene Fragen

- Wachstum kostet weiterhin keine Energie. Ein Energiepreis je gewonnener Fläche wäre der nächste Schritt
  (Nutzerentscheidung offen, berührt die Energieökologie).
- Pulsieren neben einem Nachbarn stößt diesen weiter ab, jetzt aber höchstens mit dem Wachstumstempo. Das ist der
  gewollte Weg für Raupen- und Kriechgänge.
- Nicht bitgleich zu früheren Läufen. Gespeicherte Populationen laufen mit den neuen Regeln weiter.
- Ob sich das schnelle Teilen in großen Welten damit legt, muss eine Sichtung in der GUI zeigen.
