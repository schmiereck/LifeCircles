# Zugfeder-Felder (experimentell, 02.11.2026)

Vorschlag des Nutzers: Ein Anziehungsfeld, das wie eine Zugfeder wirkt. Es zieht mit wachsendem Abstand
stärker, bis es abreißt. Weil das Gehirn je Aktor nur **eine** Kraftstärke steuert, schaltet die Feder ab einer
Schwelle der Anziehung ein (Standard 75 %, wählbar z. B. 50 %). Schwächere Anziehung und Abweisung bleiben das
klassische Feld.

## Vorher: kein Federfeld

`SensorActorForceCellCalcService`: Je Sender gilt |F| = |s|·(1 − d/R)·d / max(d, R·soft) mit der Reichweite R
und soft = 0,25. Die Kraft steigt nur im Kern (d < R/4) und fällt danach linear auf 0. Ein Verband reißt also
umso leichter, je weiter er gedehnt wird. Bindungen und Anker nutzen dasselbe Profil (Bindung × 3).

## Modell

- Schalter `springAdhesion` (Standard **aus**, global in allen Welten, kein GUI-Schalter und keine Speicherung
  in der Einstellungsdatei).
- Ein Sender ist eine Feder, wenn sein Rohbefehl s ≤ −Schwelle·maxAnziehung ist (`springAdhesionThreshold`,
  Standard 0,75). Die Entscheidung fällt je Sender, beide Richtungen eines Paars rechnen also dieselben Terme
  (actio = reactio).
- Feder: |F| = k·|s|·d/R für d < R, ab d ≥ R reißt sie ab (0). Abrissfaktor k = `springTearFactor`
  (Standard 3). Die Steifigkeit am Kontakt ist dann 75 % des alten Kerns (dort 1/soft = 4).
- Steifigkeitsgrenze: Alle Zellen haben dieselbe Masse, die Paarskala r₁r₂/r_max² wächst aber mit der Größe.
  Die Federsteifigkeit wächst daher etwa mit k·r. Bei 60 Hz begann die Drift bei k·r ≈ 40. Je Paar gilt deshalb
  k_eff = min(k, `springStiffnessLimit` / max(r₁, r₂)) mit Standard 36 (k_eff bei r = 12: 3, r = 16: 2,25, r = 20: 1,8).
- Unverändert klassisch bleiben die Abweisung (auch durch Farbaffinität), Bindungspaare, Anker und das Bodenfeld.
  Haftung kostet weiterhin keine Energie.

## Labor (fest verdrahtete Gehirne)

`SpringFieldLab` (Test): Zellen mit fest verdrahtetem Gehirn (FORCE-Ausgang konstant, 12 Aktoren), Mechanik
BALANCED, Schwerkraft kompensiert, 60 Hz. Ablauf: 4 s Einlauf, 2 s Last, 6 s Entlastung.

- **Knochen**: sechseckig gepackter Balken mit 9 Spalten und 2 oder 3 Lagen. Zug und Druck an den Enden, Biegung quer
  auf die Mitte.
- **Panzer**: Ring aus 24 Zellen, 1 oder 2 Lagen (die innere Lage ist um eine halbe Teilung versetzt). Zug und Druck an
  den oberen und unteren 30°-Kappen.
- Kennzahlen: Formfehler (Spitze und Rest, in R), Anteil der größten Komponente am Ende (1 = intakt), maximale
  Überlappung.

## Ergebnisse (R = 12, volle Anziehung, `docs/spring-field/`)

| Probe | klassisch | Feder k = 3 |
|---|---|---|
| Knochen 9×3, Zug 300 | reißt (0,89), Rest 5,7 | intakt, Rest 0,01 |
| Knochen 9×3, Zug 600 | reißt (0,78), Rest 19 | intakt, Rest 0,02 |
| Knochen 9×2, Zug 600 | reißt | intakt, Rest 0,00 |
| Knochen 9×3, Biegung 150 | bleibend verbogen, Rest 1,75 | federt zurück, Rest 0,14 |
| Panzer 2 Lagen, Zug 600 | zerfällt (0,36), Rest 10,4 | intakt, Rest 0,40 |
| Panzer 2 Lagen, Druck 150 | Rest 0 | Rest 0 |
| Panzer 1 Lage, Zug 600 | zerfällt | intakt, plastisch (Rest 4,4) |
| Panzer 1 Lage, Druck 150 | plastisch (Rest ≈ 3,3) | plastisch (Rest 3,4) |

- **Schwelle bestätigt**: Bei 60 % Anziehung ist `spring75` bitgleich zum klassischen Feld, `spring50` wirkt als Feder
  (`sweep.csv`).
- **Abrissfaktor** (`tear.csv`, ohne Steifigkeitsgrenze): k = 1 zu schwach. Mit k = 1,5–2 hält Knochen 9×3 Zug 600, der
  Panzer mit 2 Lagen aber nicht. Mit k = 2,5 hält alles außer dem einlagigen Panzer bei Zug 600. Mit k = 3 halten alle Proben. Bei k = 3,5 beginnt
  die Drift ohne Last, bei k = 4 kollabiert der unbelastete Knochen 9×3 (Überlappung 0,52, Zusammenhang 0,63): Der
  Einwärtszug übersteigt die Kontaktsteifigkeit.
- **Größe und Zeitschritt** (`size.csv`, `sizelimit.csv`): Ohne Grenze kollabiert k = 3 bei r = 20 und 60 Hz schon
  unbelastet, bei 120 Hz nicht (also numerisch). Ohne Grenze beginnt die Drift bei k·r ≈ 40 (r 12: k 3,5; r 16: k 2,5; r 20: k 2).
  Mit Grenze 36 sind r = 8, 12 und 20 bei 60 und 120 Hz unbelastet stabil. r = 20 hält alle Lasten,
  r = 8 hält Zug 300, ist aber bei Biegung 150 (2 Lagen) und beim Panzer unter Zug 600 zu schwach. Die Paarskala macht
  kleine Zellen grundsätzlich schwächer, klassisch reißen dort alle Proben.
- 60 und 120 Hz liefern mit Grenze vergleichbare Ergebnisse.

## TEST_A: Zwillingsfiguren unter Schwerkraft

TEST_A (fest verdrahtete Gehirne, BALANCED, Radius 20, Bodenreibung) schaltet die Feder mit **Schwelle 1,0** ein.
Nur volle Anziehung (FORCE = 0) ist Feder. Schneemann und Altfiguren ziehen mit 99 % an und bleiben klassisch.
Rechts davon steht jede Figur zweimal, links klassisch (99 %), rechts als Feder (100 %).
Die Figuren sind hexagonal gepackt (`TestATrainStrategy.addFigure`, Masken mit `#`). Die Welt ist 7200 px breit
(Skalierung 7,2); zwischen allen Figuren bleiben mindestens 12 R Luft (vom Test geprüft, > 4 R).

Formfehler nach 20 s bei 60 Hz, starr ausgerichtet, in Radien. `TestASpringFiguresTest`, 02.11.2026:

| Figur | klassisch | Feder | Bemerkung |
|---|---|---|---|
| Kragarm (Säule 2×4, Arm 9×2) | 0,61 R, ganz | 0,42 R, ganz | Arm hängt in beiden Modi (Spitze ~5 R tiefer) |
| Brücke (Pfeiler 2×2, Deck 14×1) | 2,56 R | 2,37 R | beide ganz, ähnliches Durchhängen |
| Turm 2×16 | 1,68 R, knickt (Höhe 86 %) | 0,68 R (Höhe 93 %) | |
| Fallbalken 11×2 auf Sockel (22 R Fall) | 2,72 R, in zwei Hälften | 1,19 R, ganz | |

Korrektur: In der ersten, engeren Anordnung (4800 px) brach der klassische Kragarm ab. Ursache war die Berührung
mit seinem Federzwilling, nicht das Feld. Mit mehr Abstand bleibt er ganz.

Die Feder ist nahe am Kontakt weicher als das klassische Feld (|F| wächst erst mit dem Abstand). Darum hängen
ruhende Strukturen etwas mehr durch, reißen aber unter großer Dehnung nicht ab. Die Brücke zeigt das als Gleichstand.

## Verstärkung (experimentell, Standard aus)

Zwei Schalter in `SimulationConfig`, beide in `newSimulation` zurückgesetzt, kein GUI-Schalter:

- `springPreload`: Ein Federsender zieht nie schwächer als das klassische Feld, |F| = max(Feder, klassisch).
  Das schließt die weiche Stelle am Kontakt. Stetig, Abriss weiterhin bei R.
- `springDamping` c: Dämpfer entlang der Achse jedes Feder-Aktorpaars, F = c·((v_B − v_A)·u)·u mit den
  Aktor-Geschwindigkeiten (v + ω×r). Symmetrisch (actio = reactio) und nur dissipativ, keine Energie aus dem Nichts.
  Pro Zelle wirken Dutzende Aktorpaare, darum sind nur kleine Werte stabil: c = 1 explodiert bei 60 Hz.

Variantenstudie (`TestASpringFiguresTest#reinforcementVariantsStudy`, `-Dlife.springVariants=true`, Schrittrate
`-Dlife.springHz`), Formfehler der Federzwillinge in R nach 20 s, 02.11.2026:

| Variante | Kragarm | Brücke | Turm | Fallbalken |
|---|---|---|---|---|
| Basis (k 3, Grenze 36 → wirksam 1,8), 60 Hz | 0,42 | 2,37 | 0,68 | 1,19 |
| Vorspannung, 60 Hz | 0,52 | 2,44 | 0,66 | 1,02 |
| Dämpfung 0,03, 60 Hz | 0,42 | 2,36 | 0,56 | 1,21 |
| Dämpfung 0,3, 60 Hz | 0,55 | 2,47 | 0,44 | 1,23 |
| k 3 ohne wirksame Grenze (≥ 60) oder k 4/6, 60 Hz, auch mit Dämpfung | zerfällt | zerfällt | zerfällt | zerfällt |
| Basis, 180 Hz | 0,60 | 2,24 | 0,62 | 1,22 |
| k 4 (Grenze 80) + Dämpfung 0,1, 180 Hz | 0,49 | 2,52 | 0,63 | 0,74 |
| k 6 (Grenze 120) + Dämpfung 0,03, 180 Hz | 0,61 | 1,43 | 0,87 | 0,72 |

Befunde:
- Bei 60 Hz begrenzt das explizite Verfahren die Steifigkeit. Dämpfung hebt diese Grenze **nicht** an.
- Vorspannung und kleine Dämpfung bringen nur wenig (Turm bis 0,44 R, Balken 1,02 R).
- Mit kleineren Schritten (180 Hz, dreifache Rechenzeit) sind k = 4–6 stabil. Der Fallbalken wird dann deutlich steifer
  (0,72 R statt 1,19 R).
- Kragarm und einlagige Brücke ändern sich durch mehr Zugsteifigkeit kaum. Sie versagen über Gelenkdrehung: Zwei Kreise
  berühren sich an einem Punkt, und das Biegemoment entsteht nur über den kleinen Abstand benachbarter Randaktoren.
  Eine haltende einlagige Brücke braucht Biegesteifigkeit, also ein Drehmoment zwischen gekoppelten Zellen
  (Winkelfeder), nicht mehr Zug.

## Winkelfeder (experimentell, Standard aus)

`springBending` κ (0 = aus), `springBendingDamping` (Sekunden, 0 = aus), in `newSimulation` zurückgesetzt, kein GUI-Schalter.
Wirkt nur zwischen Zellpaaren, die gerade über ein Feder-Aktorpaar ziehen (reißt also mit der Feder ab), und nur bei
gleicher Aktorzahl n = 12. Gedächtnislos: Ruhelage ist „der Nachbar sitzt vor einem Aktor“ (Winkel vom Zellmittelpunkt).

- Energie E = c·[(1 − cos nΔ_A) + (1 − cos nΔ_B)], Δ_A = Bindungswinkel − Drehung A, Δ_B analog von B aus,
  c = κ·w·scale/n², w = stärkster Federzug des Paars. Drehmoment auf jede Zelle plus Querkraftpaar an den Mittelpunkten:
  Impuls und Drehimpuls bleiben erhalten (Test `bendingSpringTurnsATwistedCellBack`).
- Kosten: ein atan2 und zwei sin pro ziehendem Zellpaar, nur wenn κ > 0. Turm-Lauf 80 s: 1,30 s gegen 1,27 s ohne.

Turm-Langlauf (`TestASpringFiguresTest#towerLongRunTrace`, `-Dlife.towerTrace=true -Dlife.towerVariant=true
-Dlife.springBending=κ`, 03.11.2026). Der Federturm bleibt in allen Läufen ein Stück. Ohne Winkelfeder kippt er ab
~25 s langsam, exponentiell wachsend (Neigung −4,6 R bei 40 s, liegt bei ~60 s). Fuß bleibt flach, Mitte fast auf der
Linie Fuß–Spitze: der Turm schert bzw. knickt in den unteren Reihen, er schaukelt nicht auf dem Fuß.

| Variante | liegt ab ca. |
|---|---|
| κ 0, 60 Hz | 55–60 s |
| κ 30 / 100 / 300, 60 Hz | 45–60 s (Richtung zufällig, kein Gewinn) |
| κ 1000, 60 Hz | 75–80 s (Neigung bei 40 s nur 0,1 R) |
| κ 1500 oder 3000, 60 Hz | instabil (Zellen rotieren auf), fällt in 10–20 s |
| κ 1000/3000 + Winkeldämpfung 0,01–0,1 s, 60 Hz | schlechter bis explodiert (explizite Dämpfung) |
| κ 0, 120 Hz | 55–60 s |
| κ 3000, 120 Hz (Laufzeit ×1,9) | ~80 s |

Befund: Die Winkelfeder verzögert das Umfallen bis etwa zur Stabilitätsgrenze des expliziten Schritts
(Paar ~10000, im Gitter mit sechs Nachbarn ~1000–1500 bei 60 Hz), verhindert es aber nicht. Die Dämpfung macht es
schlimmer, weil sie selbst explizit integriert wird. Dauerhaft stehende Türme brauchen nach jetzigem Stand ein
implizites bzw. positionsbasiertes Lösen der Paarbedingungen (XPBD-artig) oder Substeps. → Umgesetzt als
„XPBD-Gelenke“ (unten).

## XPBD-Gelenke (experimentell, Standard aus)

`springSolver` (aus), `springSolverStiffness` (Faktor auf die Federsteifigkeit, Standard ∞ = starr),
`springSolverIterations` (Standard 8), in `newSimulation` zurückgesetzt, kein GUI-Schalter. Klasse `SpringLinkSolver`.

- Die Kraftphase wendet für ziehende Feder-Aktorpaare keine Federkraft (und keine `springDamping`) an, sondern fasst
  jedes ziehende Zellpaar zu **einem Gelenk** zusammen: je ein Punkt auf dem Rand beider Zellen in Richtung des
  steifigkeitsgewichteten Schwerpunkts der ziehenden Aktoren; erfüllt, wenn sich die Zellen dort berühren.
  Gesamtsteifigkeit Σk, Haltekraft gedeckelt auf die Summe der Reißkräfte (das Gelenk rutscht, statt unbegrenzt zu
  halten). Abstoßung, Klassik-Paare und alle anderen Kräfte bleiben explizit.
- Nach der Integration (vor der Gelände-Kollision) korrigieren Gauss-Seidel-Durchläufe die Gelenkpunkte mit
  Nachgiebigkeit 1/(k·Faktor·dt²); Verschiebung und Drehung beider Zellen mit den inversen Massen des expliziten
  Integrators. Geschwindigkeiten folgen den Korrekturen (Δx/dt, Δθ/dt). Impuls und Drehimpuls bleiben erhalten
  (Test `solverHingeClosesTheGapAndKeepsMomentum`). Das Feld reißt weiter über die Aktorreichweite ab.
- Modelländerung gegenüber den Einzelfedern: Der Rotationsrest der Aktorpaare entfällt; die Starrheit kommt aus der
  Triangulierung (drei Scheiben mit drei Gelenken bilden ein starres Dreieck).
- Fehlversuch zuvor: ein Punkt-Constraint je Aktorpaar (Ruhelänge 0). Mehrere Aktorpaare eines Zellpaars
  widersprechen sich, die Zellen verdrehen sich (bis 30°), steifer wurde schlechter (Turm liegt bei ~40 s).
  Ebenso Gelenkpunkte im Schwerpunkt *innerhalb* der Zelle: nur Verzögerung (∞: 96 % Höhe bei 80 s, liegt bei 120 s).

Turm-Langlauf mit Randgelenken (`-Dlife.springSolver=true -Dlife.springSolverStiffness=… -Dlife.springSolverIterations=…
-Dlife.towerSeconds=160`, 03.11.2026), Laufzeit für 160 s Simulation (zwei Türme):

| Variante | Verlauf | Laufzeit |
|---|---|---|
| ohne Solver | liegt bei ~60–80 s | 2,42 s |
| ∞, 4 Iterationen | 80 s Neigung 8,6 R, liegt bei 120 s | 2,88 s |
| ∞, 6 Iterationen | steht 160 s, Höhe 0,998, Neigung 1,7 R, ruhig (spin 0) | 3,11 s |
| ∞, 8 Iterationen (Standard) | steht 160 s, Höhe 0,998, Neigung 1,5 R | 3,12 s |
| ∞, 10 Iterationen | steht 160 s, Höhe 0,999, Neigung 1,4 R | 3,24 s |
| 100, 10 Iterationen | steht 160 s, Höhe 0,998 | 3,28 s |
| 10, 10 Iterationen | 80 s Neigung 4,8 R, liegt bei 160 s | 3,30 s |
| ∞, 4 Iterationen + Winkelfeder κ 300/1000 | liegt bei 80 s (schlechter) | 3,1–3,2 s |

Alle Zwillinge 60 s (`solverTwinsStudy`, `-Dlife.springSolverStudy=true -Dlife.studySeconds=60`, Standard ∞/8),
Formfehler der Federfigur ohne → mit Solver: Kragarm 0,40 → 0,03 R (kippt als Ganzes, Spitze sinkt 4,8 R),
Brücke 2,38 → 2,88 R (Mitte sinkt nur 0,36 statt 0,71 R), Turm 1,69 → 0,02 R (Höhe 0,31 → 0,998),
Fallbalken 1,24 → 0,15 R; alle ganz. Klassik-Zwillinge unverändert. Laufzeit der ganzen Szene 2,00 → 2,23 s (+11 %).

Befund: Starre Gelenke mit 6–8 Iterationen halten Turm und zweilagige Körper dauerhaft bei ~+10–30 % Mechanikzeit.
TEST_A schaltet `springSolver` seit 04.11.2026 ein (`TestATrainStrategy`); der Zwillingstest prüft bei der Brücke
seitdem die Durchbiegung der Mitte statt des Formfehlers (Deck knickt an den Gelenken, Mitte sinkt weniger).
Offen: GUI-Schalter, Verhalten in Evolutionswelten (Gehirne lösen/setzen Federn ständig um), einlagige Brücke.

## Grenzen

- Druckversagen bei 600 bleibt (Kontaktmodell, nicht das Feld). Ein einlagiger Panzer verformt sich unter Druck in
  allen Modi plastisch, nur der zweilagige ist formstabil.
- Nicht geprüft: Evolution (wählen Gehirne die Feder?), Energiekosten
  starker Haftung, Wechselwirkung mit Bindungen (Bindungspaare bleiben klassisch × 3).
- Feste Gehirne mit voller Anziehung. Evolvierte Gehirne schalten zwischen Feder und Feld nur über die Stärke.

## Reproduktion

```
mvn test -Dtest=SpringFieldTest
mvn test -Dtest=TestASpringFiguresTest                          # TEST_A-Zwillinge, Tabelle auf stdout
mvn test -Dtest=TestASpringFiguresTest#reinforcementVariantsStudy -Dlife.springVariants=true [-Dlife.springHz=180]
mvn test -Dtest=SpringFieldSweepTest -Dspring.sweep=true        # sweep.csv
mvn test -Dtest=SpringFieldSweepTest -Dspring.tearsweep=true    # tear.csv
mvn test -Dtest=SpringFieldSweepTest -Dspring.sizesweep=true    # size.csv
mvn test -Dtest=SpringFieldSweepTest -Dspring.sizelimit=true    # sizelimit.csv
```
Ausgabe nach `target/spring-field` (`-Dspring.output=…`). In `sweep.csv` steht ein Name ohne „-raw“ für die
Läufe vor Einführung der Grenze. Diese entsprechen „-raw“, also ohne Grenze.
