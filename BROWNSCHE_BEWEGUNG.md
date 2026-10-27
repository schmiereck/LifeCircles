# Brownsche Bewegung

Status: **umgesetzt** (27.10.2026), Schalter, Standard aus. Idee und Vorgaben des Nutzers: Auf der Ebene von
Zellen und „intelligenten Molekülen“ schubst und dreht die Wärmebewegung die Elemente und bringt sie in Kontakt.
Ein Hintergrund soll die Zellen statistisch gleichmäßig bewegen, ohne Strömung; stark genug zum Bewegen und
Drehen, schwach genug, dass Verbindungen halten. Dichte und Stärke einstellbar, alle Welten, in RACE aus.
Die Physik ist ausgelagert, weil verschiedene Umsetzungen verglichen werden sollen.

## Aufbau (`de.lifecircles.service.brownian`)

- `BrownianMotion`: Schnittstelle eines Modells, `agitate(cell, dt, density, strength, random)`.
- `BrownianKicks`: gemeinsame Physik eines Stoßes. Er wirkt als Kraft für diesen Schritt an einem Randpunkt der
  Zelle, so dass er die Geschwindigkeit genau um den gewünschten Betrag ändert. Das vorhandene Hebelmodell der
  Zelle macht aus dem tangentialen Anteil Drehung. Danach integriert der Schritt alles wie jede andere Kraft:
  Dämpfung (Viskosität, Drehdämpfung), Abstoßung, Haftung und Bindungen wirken auf das Ergebnis.
- Modelle (`BrownianModel`, Auswahl in der GUI und in der Einstellungsdatei):
  - `COLLISIONS`: einzelne Teilchenstöße. Je Schritt eine Poisson-verteilte Zahl von Treffern, jeder an einem
    zufälligen Randpunkt, nach innen gerichtet und um bis zu ± 90° geneigt.
  - `LANGEVIN`: Grenzfall vieler kleiner Stöße. Je Schritt ein Gauß-verteilter Stoß mit demselben mittleren
    Quadrat (Trefferrate · dt · Stärke²) an einem zufälligen Randpunkt.
  - Neue Modelle: Klasse mit `BrownianMotion` anlegen und in `BrownianModel` eintragen.
- `BrownianMotionService`: wendet das gewählte Modell im Schritt nach Viskosität und Gravitation auf alle Zellen
  an, mit eigenem Zufallsstrom (die übrigen Zufallszahlen der Welt bleiben unverändert).
- `SimulationStepPolicy.useBrownianMotion()`: alle Welten, RACE schaltet sie ab. Dort würde die Irrfahrt die
  gewertete Strecke erhöhen und leicht schubsbare Körper belohnen.

## Einstellungen

- **Dichte:** Treffer je Sekunde auf eine Zelle mit Radius 10 px; größere Zellen werden entsprechend ihrem Umfang
  öfter getroffen. Standard 20.
- **Stärke:** Geschwindigkeitsänderung eines Treffers in px/s. Standard 5.
- Keine Energieform: Die Bewegung ist Wärme der Umgebung und kostet keine Zellenergie. Alle Zellen haben im Modell
  dieselbe Masse; nur die Drehträgheit wächst mit dem Radius.

## Prüfungen und Kalibrierung (`BrownianMotionTest`)

- Beide Modelle (Prüfung mit Dichte 20, Stärke 10): mittleres Quadrat der Geschwindigkeitsänderung 2001 je
  Sekunde (erwartet 20 · 10² = 2000), x/y gleich, kein Mittelwert (keine Strömung), Drehung vorhanden.
- Unabhängig vom Zeitschritt (60 und 120 Hz gleich).
- Standard aus, RACE aus, offene Welt schubst.
- Kalibrierung ohne Gravitation, Haftung an, 30 s, 16 Einzelzellen und 16 Paare verwandter Zellen mit voller
  Anziehung, Dichte 20:

  | Stärke (px/s) | Einzelzelle bewegt (rms) | gedreht | Paare getrennt |
  |---|---|---|---|
  | 3 | 18 px | 12 rad | 0 von 16 |
  | 5 | 31 px | 20 rad | 0 von 16 |
  | 6 | 37 px | 24 rad | 0 von 16 |
  | 7 | 43 px | 28 rad | 1 von 16 |
  | 8 | 49 px | 32 rad | 3 von 16 |
  | 10 | 62 px | 39 rad | 10 von 16 |
  | 15 | 234 px | 59 rad | 15 von 16 |

  Standard 5 px/s: Einzelzellen wandern etwa drei Radien und drehen sich gut dreimal in 30 s, verwandte Paare
  halten. Ab etwa 7 px/s reißen die ersten Paare. Bei 15 px/s jagen gelöste, voll anziehende Zellen umher und
  ziehen andere mit (daher der Sprung).

## Grenzen

- Noch nicht in einer laufenden Welt mit Population beobachtet; Wirkung auf Bodenhalt, Anker und Überreste offen.
- Die Kalibrierung gilt für Zellen mit Radius 10 und voller Anziehung; schwächer haftende Verbände reißen früher.
