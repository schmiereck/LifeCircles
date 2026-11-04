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
Die Figuren sind hexagonal gepackt (`TestATrainStrategy.addFigure`, Masken mit `#`).

Formfehler nach 20 s bei 60 Hz, starr ausgerichtet, in Radien. `TestASpringFiguresTest`, 02.11.2026:

| Figur | klassisch | Feder | Bemerkung |
|---|---|---|---|
| Kragarm (Säule 2×4, Arm 9×2) | 2,68 R, Teil abgebrochen | 0,55 R, ganz | Feder: Arm hängt elastisch (Spitze 4,8 R tiefer) |
| Brücke (Pfeiler 2×2, Deck 14×1) | 2,39 R | 2,37 R | beide ganz, gleiches Durchhängen |
| Turm 2×16 | 1,70 R, knickt (Höhe 86 %) | 0,68 R (Höhe 93 %) | |
| Fallbalken 11×2 auf Sockel (22 R Fall) | 2,72 R, in zwei Hälften | 1,19 R, ganz | |

Die Feder ist nahe am Kontakt weicher als das klassische Feld (|F| wächst erst mit dem Abstand). Darum hängen
ruhende Strukturen etwas mehr durch, reißen aber unter großer Dehnung nicht ab. Die Brücke zeigt das als Gleichstand.

## Grenzen

- Druckversagen bei 600 bleibt (Kontaktmodell, nicht das Feld). Ein einlagiger Panzer verformt sich unter Druck in
  allen Modi plastisch, nur der zweilagige ist formstabil.
- Nicht geprüft: Evolution (wählen Gehirne die Feder?), Energiekosten
  starker Haftung, Wechselwirkung mit Bindungen (Bindungspaare bleiben klassisch × 3).
- Feste Gehirne mit voller Anziehung. Evolvierte Gehirne schalten zwischen Feder und Feld nur über die Stärke.

## Reproduktion

```
mvn test -Dtest=SpringFieldTest
mvn test -Dtest=TestASpringFiguresTest                          # TEST_A-Zwillinge, Tabelle auf stdoutmvn test -Dtest=SpringFieldSweepTest -Dspring.sweep=true        # sweep.csv
mvn test -Dtest=SpringFieldSweepTest -Dspring.tearsweep=true    # tear.csv
mvn test -Dtest=SpringFieldSweepTest -Dspring.sizesweep=true    # size.csv
mvn test -Dtest=SpringFieldSweepTest -Dspring.sizelimit=true    # sizelimit.csv
```
Ausgabe nach `target/spring-field` (`-Dspring.output=…`). In `sweep.csv` steht ein Name ohne „-raw“ für die
Läufe vor Einführung der Grenze. Diese entsprechen „-raw“, also ohne Grenze.
