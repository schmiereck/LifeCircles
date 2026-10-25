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
- **Direkte Taktsynapsen** (seit 25.10.2026, Nutzerentscheidung, `CellBrain.wireClocks`): Neue Gehirne mit vollem
  Eingang (328) verbinden jeden der 12 Takteingänge direkt mit der Kraft aller 12 Aktoren und mit der Größe,
  Gewichte gleichverteilt in ± 0,5 (`CLOCK_SYNAPSE_WEIGHT`). Das sind 156 gewöhnliche Synapsen: Sie werden vererbt,
  mutiert und als Speicher bezahlt (Standardgründer 1481 statt 1325 Synapsen). Zufallskörper schwingen damit von
  Anfang an mit zufälliger Phase und Mischung der Perioden; die Evolution wählt Phase, Periode und Stärke aus und
  kann die Gewichte auch wieder abschwächen. Migrierte und andere Netze bleiben unverbunden.
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

## Kommt der Takt am Aktor an? (`ActorRhythmTest`, 25.10.2026)

- **Signalweg:** Ein Testgehirn ohne Synapsen setzt die Kraft aller Aktoren auf den Sinus eines Takts, wie es ein
  großes Gewicht täte. Es läuft durch den vollen Schritt mit Haftung (Finanzierung, Eingänge, Ausgänge, Tonus
  0,5 s). Der Tonus ist ein Tiefpass erster Ordnung, der Ausschlag entspricht genau 1/√(1 + (2πτ/T)²):

  | Periode | Kraft | Ausschlag | Vorzeichenwechsel (alle 0,5 s gemessen) |
  |---|---|---|---|
  | 0,5 s | 0,4 … 38 | 16 % | 0 von 48 (0) |
  | 1 s | −16 … 57 | 30 % | 24 von 24 (23) |
  | 2 s | −42 … 87 | 54 % | 12 von 12 (12) |
  | 4 s | −69 … 120 | 79 % | 6 von 6 (6) |
  | 8 s | −84 … 140 | 93 % | 3 von 3 (3) |

  Ab 1 s kommt jeder Takt als Wechsel zwischen Ziehen und Drücken an. Der 0,5-s-Takt wechselt nie: Anziehung
  (max. 90) und Abstoßung (max. 150) sind ungleich, ein voller Sinusbefehl drückt deshalb im Mittel mit 19, und
  der Tonus lässt nur den kleinen Ausschlag um dieses Mittel durch. Die Messung der RACE-Studie alle 0,5 s
  erkennt die Takte ab 1 s.
- **Zufallsgehirne:** Für 16 RACE-Gründer laufen nur die Takte 16 s lang, alle anderen Eingänge bleiben fest.
  Schwankung der Aktorkraft (Bereich −90 … 150): Median 0,002, Maximum 0,014. Kein Gründer schwingt. Nach 30,
  100 und 300 Mutationen ohne Auswahl bleibt der Median bei 0, das Maximum bei 0,3 bis 2,5.
- **Ursache:** Die Takteingänge erreichen die Kraftausgänge nur über mehrere Schichten. Die Anfangsgewichte
  dieser Schichten sind klein (Xavier, ± 0,11), Synapsen aus der Mutation `addRandomSynapse` starten mit ± 0,001,
  und nur 2,5 % der möglichen Verbindungen werden angelegt. Über mehrere Stufen bleibt vom Takt fast nichts.

- **Mit direkten Taktsynapsen:** Schwankung der Kraft durch die Takte im Median 162 (Maximum 197), alle 16 Gründer
  schwingen; nach 30–300 Mutationen ohne Auswahl Median 177–227. Kurzer RACE-Lauf (3 Generationen, 10 %
  Schwerkraft, Seed 4711): `oscillating 32 pulsing 32` in jeder Generation (vorher 0), Bestwert gemittelt
  4,6 · 7,4 · 4,8 px, Median teils von 0 verschieden (−0,37 · −0,20 · 0,47 px).

## Grenzen

- Ohne direkte Taktsynapsen spürten Zufallsgehirne die Takte kaum (oben); in 300 RACE-Generationen schwang
  kein Körper. Die Studie dazu lief vor dieser Änderung.
- Der 0,5-s-Takt kommt wegen des Tonus nicht als Vorzeichenwechsel an (oben), wohl aber als Schwankung. Auch nach 300 RACE-Generationen (3 Startlagen, 10 % Schwerkraft)
  schwang kein Körper (`RENNBAHN.md`, Studie mit Taktgebern).
- Nicht bitgleich zu früheren Läufen: Neue Zufallsgehirne haben andere Synapsen, und die Mutation der
  Perioden zieht zusätzliche Zufallszahlen.
