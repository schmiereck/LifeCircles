# Taktgeber

Status: **umgesetzt** (25.10.2026), gilt in allen Welten. Nutzerentscheidung: Takteingang mit vererbbarer
Periode, sechs Stück für verschiedene Zwecke und Überlagerungen.

## Anlass

In RACE waren nach Hunderten Generationen keine zyklischen Änderungen von Aktoren oder Größe zu sehen
(Beobachtung des Nutzers, Zählung in `RaceTrainStrategyTest#study`: 0 von 32 Körpern). Ein Gang braucht aber
einen Takt. Die Zufallsgehirne sind beim Anlegen reine Vorwärtsnetze, und das Alter war der einzige
zeitabhängige Eingang (`RENNBAHN.md`, Abschnitt Taktgeber).

## Modell

- **Gen:** Jede Zelle trägt sechs Perioden `clockPeriods` in Sekunden. Neue Zellen und Zellen aus älteren
  Speicherständen bekommen Oktaven: 0,5 · 1 · 2 · 4 · 8 · 16 s. Erlaubter Bereich 0,1–120 s.
- **Eingänge:** Je Takt Sinus und Kosinus von `2π · Alter / Periode`, also 12 Eingänge. Sie hängen hinter allen
  bisherigen Kanälen (`EnergyInputLayout.clockSine/clockCosine`, Index 316–327). Das Netz hat jetzt 328 Eingänge.
- **Phase:** Die Uhr läuft auf dem eigenen Alter der Zelle. Jede Zelle beginnt bei der Geburt mit Phase 0.
  Zellen eines Verbands, die zu verschiedenen Zeiten geboren wurden, laufen deshalb versetzt. Wie weit, legt
  der Teilungszeitpunkt fest, und den bestimmt das Gehirn. Einen weltweiten gemeinsamen Takt gibt es nicht.
  Er wäre ein globales Signal, das keine Zelle wahrnehmen kann.
- **Sinus und Kosinus:** Aus beiden zusammen kann das Netz mit zwei Gewichten jede Phasenlage bilden,
  etwa für Fuß und Schubaktor verschieden.
- **Vererbung:** Teilung, Trainingsgründer und HIGH_POSITION3 kopieren die Perioden. Die Mutation wirkt mit
  derselben effektiven Rate und Stärke wie bei den Gewichten und multipliziert die Periode mit
  `exp(±Stärke)`, also etwa ± 20 % bei Stärke 0,2. Kurze und lange Perioden ändern sich dabei relativ gleich.
  Eliten (`mutate = false`) und Klonteilung (Rate 0) behalten sie.
- **Migration:** Netze mit 213, 304 oder 316 Eingängen bekommen die neuen Eingänge ohne Synapsen. Ihre Ausgänge
  bleiben bitgenau gleich (`EightEnergySensorTest`). Neue Zufallsgehirne werden auch mit den Takteingängen
  zufällig verbunden.
- Im Gehirnstillstand (Pause) läuft das Alter und damit die Uhr weiter. Der Takt selbst kostet keine Energie.

## Kann das Netz aus einem Takt andere Perioden ableiten?

- **Kürzere Perioden, ganzzahlige Bruchteile, ja:** Nichtlinearitäten erzeugen Oberschwingungen. Zum Beispiel ist
  sin² = (1 − cos 2ωt)/2, also die halbe Periode, und eine Schwelle macht aus dem Sinus ein Rechteck
  derselben Periode. Mit Sinus und Kosinus geht 2 sin cos = sin 2ωt, wenn das Netz das Produkt annähert.
- **Längere oder unabhängige Perioden, nein:** Ein Vorwärtsnetz hat kein Gedächtnis. Eine Teilung der Frequenz,
  also ein Schritt mit jedem zweiten Takt, braucht einen Zustand, der mitzählt. Das können nur Rückkopplungen,
  und die müsste die Evolution erst aufbauen. Perioden, die kein ganzzahliges Verhältnis zueinander haben,
  lassen sich gar nicht aus einem Takt ableiten.
- Deshalb gibt es sechs unabhängige Takte. Ihre Perioden kann die Evolution einzeln verschieben.

## Prüfungen (`ClockInputTest`)

- Eingänge: Sinus und Kosinus des Alters je Takt, hinter allen bisherigen Kanälen.
- Grenzen 0,1–120 s; Mutation mit Rate 0 hält die Perioden, mit Rate 1 ändert sie sie um einen Faktor
  innerhalb von exp(± Stärke).
- Teilung, mutierter und nicht mutierter Trainingsgründer: Vererbung wie oben.
- Gespeicherte Zelle ohne das Feld lädt mit den Oktaven.

## Grenzen

- Neue Synapsen starten mit Gewichten von höchstens ± 0,001. Ein Zufallsgehirn spürt den Takt deshalb noch
  kaum. In den ersten 4 RACE-Generationen schwang kein Körper (`oscillating 0`). Erst die Mutation (± Stärke
  je Treffer) macht die Gewichte groß genug.
- Nicht bitgleich zu früheren Läufen: Neue Zufallsgehirne haben andere Synapsen, und die Mutation der
  Perioden zieht zusätzliche Zufallszahlen.
