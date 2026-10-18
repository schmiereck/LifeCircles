# Halt am Boden

Status: **Stufe A und B experimentell umgesetzt** (18.10.2026), eigene Schalter, Standard
aus. Entscheidungen 1–5 unten wie vorgeschlagen angenommen. Baut auf `ZELLVERBINDUNGEN.md`
auf (Stufe 1/2 Haftung, Stufe 3 Bindungen). Unten folgen der ursprüngliche Entwurf und die
Abweichungen der Umsetzung.

## Umsetzung (18.10.2026)

- **Config:** `groundAdhesion` (A), `groundAnchors` (B, wirkt nur zusammen mit A),
  `groundAnchorsGroundOnly`, `groundStickiness` (≤ 0); alle aus bzw. 0.
  - GUI: drei Kästchen und ein Schieber „Terrain stickiness“ (in % der maximalen
    Anziehung) im Umweltpanel.
  - Studie: `-Dzone.study.ground=true`, `-Dzone.study.anchors=true`,
    `-Dzone.study.groundonly=true`.
- **Stufe A** (`SensorActorForceCellCalcService.Ground`, `Blocker.offsetToSurface`):
  - Kraft zum nächsten Oberflächenpunkt jedes Blockers in Reichweite `R`, Stärke
    `min(0, Befehl) · a + s_B`.
  - `a = sim(Aktorfarbe, Grau)^k`, ohne Erkennung 1.
  - Skala `r / r_max`.
  - **Abweichung vom Entwurf:** Abstoßende Aktoren wirken nicht auf den Boden. Stufe A ist
    reiner Halt, kein Abstoßen vom Gelände; sonst könnten Zellen neu „schweben“ oder
    springen.
  - Ein Aktor im Blocker (Abstand 0) erhält keine Kraft. Der Weltumbruch ist beachtet; der
    Boden ist breiter als die Welt.
- **Stufe B** (`SensorActor.anchor`, `CellAdhesionService.updateBonds`):
  - Ankerpunkt und eingefrorene Stärke liegen im Aktor, wie bei einer Bindung, aber ohne
    Partner. Das Feld zum Anker ist `2 · Stärke · bondFieldFactor` mit derselben Form.
    Es ersetzt für diesen Aktor das Bodenfeld.
  - **Abweichung vom Entwurf:** Zum Einrasten muss der Aktor den Blocker nicht berühren.
    Der nächste Geländepunkt muss 0,5 s lang innerhalb der halben Feldreichweite liegen,
    wie bei Zellbindungen. Im ersten Rauchlauf entstand mit „berühren“ kein einziger Anker:
    Nach der Kollisionskorrektur liegen Aktoren ruhender Zellen praktisch nie im Blocker.
  - Der Ankerpunkt ist der nächste Oberflächenpunkt, bei einem Aktor im Blocker dessen
    eigene Lage.
  - Lösen: wie geplant. Zusätzlich wird ein Anker gelöst, dessen Punkt auf keinem Blocker
    mehr liegt, etwa nach dem Laden in eine andere Welt.
  - Anker werden gespeichert.
- **Momentaufnahme:** Anker als gelbe Linie mit oranger Raute.
- **Neue Studienspalten:**
  - `anchors`, `anchoredCells`;
  - `rootedBodies`: gebundene Körper mit mindestens einem Anker;
  - `rootedRise`, `rootedSpan`.

Tests: `GroundHoldTest` (10), alle mit r = 20.

| Prüfung | Ergebnis |
|---|---|
| Ohne Schalter / abstoßend / neutral / außer Reichweite | keine Bodenkraft |
| Weltkante gegen Mitte | gleiche Kraft |
| Rote statt graue Aktoren | Faktor 0,5⁴ = 0,0625; ohne Erkennung gleich; nie abstoßend |
| Eigenhaftung −45 bei neutralen Aktoren | wie eigener Befehl −45 auf neutralem Boden |
| Aufwärtszug 3 s lang | ohne Halt hebt 1,5 g die Zelle ab (51 px); mit Halt halten 2 g und 4 g (Spalt 0); 10 g reißt ab |
| Drehstoß 2 rad/s, nur Fußaktoren (2–4) ziehen | größte Drehung 0,61 statt 4,0 rad |
| Seitlicher Zug 1 g | Stufe A unverändert (gleitet); drei Anker: 1,2 statt 101 px |
| Wand, Schwerkraft | nur Feld: rutscht 101 px; drei Anker: 1,2 px |
| Einrasten | nur mit beiden Schaltern, grau, anziehend, ≤ halbe Reichweite; 0,5 s |
| „Nur Boden“ | an der Wand kein Anker, sonst drei |
| Lösen | aktives Abstoßen ja, Pause nein; Abriss; kein Gelände mehr; Überreste mit Halbwertszeit, Holz bleibt |
| Speichern/Laden | Anker bleibt in gleicher Welt, wird ohne Gelände gelöst |
| Säule aus `CellBondTest`, Seitenstoß | lose: 90° (umgekippt); zwei verankerte Fußaktoren: 3,6° |

Befunde und Grenzen:
- **Rollen wird nur gebremst, wenn die Anziehung einseitig ist.** Ziehen alle zwölf Aktoren
  an, ist die Zelle ein „Magnetrad“ mit 30°-Rastung. Ein Drehstoß dreht sie durch
  (insgesamt gedreht 3,98 statt 4,0 rad). Halt gegen Rollen entsteht erst, wenn vor allem die Fußseite
  anzieht. Das muss das Gehirn lernen.
- **Seitlich hält nur der Anker.** Das Bodenfeld wirkt senkrecht, wie geplant.
- **Anker brauchen graue Aktoren.** Im ersten Rauchlauf (180 s, Seed 4711, Bindungen an)
  hatten am Ende von 369 anziehenden Aktoren nahe am Gelände nur 8 eine Boden-Affinität
  ≥ 0,8. Mit der geänderten Einrastregel entstanden im zweiten Rauchlauf trotzdem 34 Anker
  an 33 Zellen und 8 verankerte gebundene Körper (bis 148 px Spanne).

## Zonenläufe (18.10.2026)

Modellstand `2f01126`, `ZoneSeasonStudyTest`, je 3600 s, Lebensdauer 2000 s, Jahr 600 s,
Winterlicht 50 %, Seeds 4711–4714, Bindungen und Geburtsbindung immer an. Drei Varianten:
- `gbond`: nur Bindungen (Referenz, auf demselben Stand neu gerechnet);
- `gfield`: dazu Bodenfeld (Stufe A);
- `ganchor`: dazu Bodenfeld und Anker (Stufe B).

Zwei Läufe liefen gleichzeitig, ein Lauf dauerte etwa 20–50 min. Lokale Daten (nicht
eingecheckt): `LifeCircles-runs/target/zone-season-study/g*-*.csv`, `-summary.txt`, `.cells`
und PNG-Momentaufnahmen.

Mittel über die zweite Hälfte (t > 1800 s), Ø der vier Seeds, Spitzen als Mittel der
Laufmaxima:

| | gbond | gfield | ganchor |
|---|---|---|---|
| Lebende Zellen, ganzer Lauf / Ende | 193 / 323 | 207 / 324 | 200 / 352 |
| Lebende Zellen, zweite Hälfte | 184 | 199 | 194 |
| Zone 001 lebend (je Seed) | 0 / 1 / 0 / 2 | 3 / 0 / 0 / 0 | **34** / 0 / 0 / **36** |
| Überreste | 161 | 183 | 164 |
| Größter Kontaktverband: Zellen / Höhe px | 140 / 125 | 184 / 136 | 152 / 130 |
| Bindungen | 102 | 75 | 81 |
| Größter gebundener Körper | 11 | 6 | 7 |
| Überstand gebundener Körper Ø / Spitze px | 36 / 52 | 33 / 64 | 32 / 56 |
| Anker (je Seed) | – | – | 9 (17 / 6 / 10 / 4) |
| Verankerte Zellen | – | – | 9 |
| Verankerte gebundene Körper | – | – | 2 |
| Überstand verankerter Körper Ø / Spitze px | – | – | 4 / 39 |
| Spanne verankerter Körper, Spitze px | – | – | 100 |

Befund:
- **Anker sind selten.** Gleichzeitig bestehen nur 3–20 Anker, fast immer einer je Zelle.
  Das liegt an der Affinitätsschwelle: Nur graue anziehende Aktoren rasten ein (siehe
  Rauchlauf oben). Die Population nutzt den Bodenhalt kaum.
- **Verankerte Körper liegen flach am Boden.** Es sind kurze Ketten aus 2–5 Zellen. Im
  Mittel ragen sie 4 px heraus, höchstens 30–52 px. In den Momentaufnahmen liegen
  sie als Reihen auf dem Boden. **Keine Stängel.**
- **Das Bodenfeld macht gebundene Körper kleiner.** Mit Bodenfeld gibt es weniger Bindungen
  (75–81 gegen 102), und der größte gebundene Körper ist kleiner (6–7 gegen 11 Zellen).
  Vermutlich ziehen anziehende Aktoren am Boden und nicht mehr am Nachbarn. Der
  Kontaktverband ist dabei eher größer (152–184 gegen 140), also kein Zerfall der Haufen.
  Bei vier Seeds ist das ein Hinweis.
- **Population:** gleich im Rahmen der bekannten Streuung (184–199).
- **Zone 001:** Nur mit Ankern wird sie in zwei Seeds lange besiedelt (4711: 20–55 Zellen,
  4714: 25–61 Zellen bis ~3000 s, im letzten Winter fast erloschen). In den acht anderen Läufen
  ist sie nach dem ersten Winter leer. Ein früherer Lauf ohne Bindungen
  (`ZONEN_JAHRESZEITEN_ERGEBNISSE.md`, 4713) besiedelte sie aber auch. Mit zwei Läufen
  ist ein Zusammenhang mit den Ankern **nicht belegt**.
- **Betriebsfehler:** Im ersten Durchgang liefen vier Läufe (alle drei mit 4711, dazu
  `gbond-4712`) nicht. Zwei gleichzeitige Maven-Aufrufe übersetzten die Testklassen parallel
  neu, und Surefire meldete ohne gefundenen Test Erfolg. Die Läufe wurden nachgeholt. Das
  Skript übersetzt jetzt einmal vorab und setzt `-Dsurefire.failIfNoSpecifiedTests=true`.

Folgerung: Mechanisch hält der Bodenhalt (Tests oben). In der Evolution bleibt er bisher
fast ungenutzt und erzeugt keine aufrechten Formen. Bodenfeld und Anker bleiben
experimentell und standardmäßig aus.

Kein künstlicher Höhenvorteil (Nutzervorgabe 18.10.2026): Ein Lichtgradient wäre ein
künstlicher Weg. Höhe soll sich in der NONE-Welt aus Konkurrenz um Licht ergeben
(Barrieren werfen Schatten; wer höher baut, fängt die Energie). In den HIGH_POSITION-Welten
belohnt die Selektion Höhe; dort soll die nötige Physik entdeckt werden.

**Hindernis:** Trainingswelten verwenden `SimulationStepPolicy.DISCARD_REMAINS` bzw. eine
eigene Policy. `useCellAdhesion()` ist dort `false`. Tonus, Farbaffinität, Bindungen und
Bodenhalt wirken in HIGH_POSITION2/3 also gar nicht, unabhängig von den GUI-Schaltern.
Ein Opt-in für diese Welten ist nicht umgesetzt.

---

## Ursprünglicher Entwurf

## Anlass

- Gebundene Säulen bleiben formstabil, kippen aber als Ganzes um (`CellBondTest`): Der Fuß
  ist rund, und der Boden hält nichts fest.
- In Zonenläufen mit Bindungen entstehen dichte Haufen, aber keine Stängel
  (Momentaufnahmen in `docs/cell-bonds/`).
- Der Nutzer wollte die Begrenzungen schon länger mit Attraktoren wie bei den Zellen
  ausstatten.

## Befund im Code

| Thema | Heutiger Stand | Stelle |
|---|---|---|
| Kollision | Die Zelle wird aus dem Blocker geschoben, nur die Geschwindigkeit in den Blocker hinein wird entfernt. **Keine Reibung**, die Zelle gleitet frei. | `BlockerCellCalcService.handleBlockerCollisions` |
| Wahrnehmung | Liegt ein Aktor im Blocker, nimmt er ihn als Zelle und Aktor wahr (grauer Typ 0,5/0,5/0,5, Energie 0). Das Gehirn „sieht“ also schon den Boden. | `SensorActorForceCellCalcService`, `Blocker.getType` |
| Kräfte | Ein Blocker erzeugt und empfängt **kein Feld**. Aktoren, die den Boden berühren, suchen sogar keine Zellnachbarn mehr. | ebd. |
| Masse | Blocker sind unbeweglich; eine Gegenkraft kann ins Gelände abgeleitet werden. | – |

Folgen:
- Eine Zelle, die auf dem Boden ruht, wird nur von der Schwerkraft gehalten.
- Seitlich gleitet sie reibungsfrei, und sie rollt frei.
- An Wänden rutscht sie ab.

## Grundidee

Begrenzungen bekommen **Aktorfelder wie Zellen**, aber nicht als festes Raster, sondern als
Fläche:
- **Stufe A (Bodenfeld):** Jede Blockeroberfläche wirkt wie ein Aktor, der überall auf ihr
  sitzt. Für einen Zellaktor zählt der nächste Oberflächenpunkt.
- **Stufe B (Verankerung):** Ein Aktor, der lange genug anziehend am Boden liegt, rastet an
  einem festen Punkt ein. Dieser Punkt ist ein unbeweglicher Attraktor auf dem Blocker,
  genau dort, wo er gebraucht wird. Technisch ist das eine Bindung (Stufe 3) mit einem
  Partner, der sich nicht bewegt.

Beide Stufen verwenden dieselbe Feldformel, Affinität, Tonus und Halteregeln wie zwischen
Zellen. Neu ist nur der Partner: eine Fläche bzw. ein fester Punkt.

### Warum kein festes Raster aus Bodenaktoren?

Ein Raster virtueller Aktoren im Abstand der Zellaktoren wäre die wörtlichste Umsetzung.
Es hat aber Nachteile:
- Es macht den Boden zum „Eierkarton“: Zellen rasten in festen Abständen ein, und je nach
  Rasterweite ist der seitliche Halt entweder zu schwach oder ruckelig.
- Es kostet bei 6400 px Bodenbreite viele zusätzliche Aktoren.

Die Fläche (A) liefert gleichmäßigen Halt senkrecht zum Boden. Der Ankerpunkt (B) liefert
echten seitlichen Halt nur dort, wo eine Zelle ihn will.

## Stufe A: Bodenfeld

Für jeden Aktor einer lebenden Zelle oder eines Überrests, und für jeden Blocker in seiner
Reichweite `R` (die eigene Feldreichweite, etwa 9,3 px bei r = 20):

- `p` ist der nächste Punkt auf der Blockeroberfläche (`Blocker.getNearestPoint`, unter
  Beachtung des Weltumbruchs), `d` der Abstand dorthin.
- Kraft auf die Zelle am Aktor, Richtung zu `p`, mit derselben Form wie zwischen Zellen:
  `s_eff · (1 − d/R) / max(d, R·softening) · r/r_max`.
- `s_eff` setzt sich aus zwei Teilen zusammen:
  - dem **eigenen Befehl** des Aktors (gesendetes Feld), gewichtet mit der
    Boden-Affinität `a = sim(Aktorfarbe, Bodenfarbe)^k`;
  - der **Eigenhaftung des Blockers** `s_B` (empfangenes Feld, Weltparameter je
    Blockertyp, Standard 0 = neutral).
- Die Gegenkraft geht ins Gelände. Ein Impulserhalt ist nicht nötig, das gilt auch heute
  schon für die Kollision.

Wirkung:
- **Abheben:** Ein Fuß mit anziehenden unteren Aktoren muss gegen die Anziehung abgehoben
  werden. Bei voller Anziehung (−90) und r = 20 schätze ich das auf etwa ein Zellgewicht.
  Der Druck-Test für Zellpaare ergab etwa das Doppelte, weil dort beide Aktoren senden.
  Der genaue Wert ist im Test zu messen.
- **Kippen und Rollen:** Die anziehenden Aktoren sitzen fest auf der drehenden Zelle.
  Rollt der Fuß, heben sie vom Boden ab, und die Anziehung zieht zurück. Das ergibt ein
  Rückstellmoment gegen Kippen, wie bei Magneten auf einem Rad.
- **Gleiten:** Das Feld zeigt immer senkrecht zur Oberfläche, deshalb gibt es **keinen
  seitlichen Halt**. An Wänden rutscht eine Zelle weiter ab. Seitlichen Halt bietet erst
  Stufe B.
- **Pause und Tod:** Wie bei Zellpaaren bleibt der Befehl bei Gehirnpause erhalten (Stufe 1).
  Bei Überresten klingt er mit der Halbwertszeit ab, mit ∞ (Holz) bleibt der tote Fuß
  dauerhaft haften.

Boden-Affinität:
- Der Boden hat eine **Materialfarbe**, heute für alle Blocker Grau 0,5/0,5/0,5. Grau hat zu
  jeder Ecke des Farbwürfels dieselbe Ähnlichkeit (0,5, mit k = 4 also 0,06). Voll haften
  nur Aktoren nahe Grau. Kein Stoffwechseltyp wird dadurch bevorzugt.
- Die Körperfarbe zählt hier nicht, weil ein Blocker keinen Körper hat. Mimikry ist beim
  Boden kein Thema.
- **Keine Abweisung:** Ein fremdfarbiger Aktor haftet nur schwächer (`s·a`), er wird nicht
  abgestoßen. Abweisung ist für feindliche Zellen gedacht; ein abstoßender Boden würde nur
  zufällig Zellen wegschieben, die eigentlich ihre Nachbarn anziehen wollen.
- Optional später: eigene Materialfarbe je Blockertyp (Boden, Wand, Plattform). Dann
  würden „Wurzeln“, „Kletterer“ und „Hänger“ verschiedene Aktorfarben brauchen, eine
  Nische aus der Umwelt statt aus einer Zellregel.

Nebeneffekte:
- **Wahrnehmung:** Ein Aktor, der den Boden berührt, nimmt weiterhin den Boden wahr. Das
  Gehirn kann also schon heute lernen: „Boden gespürt → anziehen“. Es braucht keine neuen
  Eingänge.
- **Eigenhaftung `s_B` < 0:** Sie macht den Boden für alle klebrig, wie Schlamm. Das ist
  ein Umweltregler für Experimente (GUI-Schieber) und keine Entscheidung der Zelle.
  Standard 0.
- **Rechenzeit:** Pro Aktor läuft heute schon eine Schleife über alle Blocker
  (`containsPoint`). Dazu kommt ein Abstandstest mit dem um `R` vergrößerten Rechteck.
  Das ist gering.

## Stufe B: Verankerung

Sie erweitert die Bindung (Stufe 3) um einen festen Partner:

- **Einrasten:** Alle Bedingungen müssen 0,5 s lang erfüllt sein (`LOCK_SECONDS`):
  - Der Aktor berührt einen Blocker; das wird schon heute erkannt (`touchingBlocker`).
  - Der Aktor zieht an.
  - Die Boden-Affinität ist ≥ 0,8 (bei aktiver Erkennung).
  - Die Zelle lebt.
  Dann wird der nächste Oberflächenpunkt als **Ankerpunkt** gespeichert, mit der aktuellen
  Anziehung als eingefrorener Stärke. Jeder Aktor hat höchstens einen Partner, also
  entweder eine Zellbindung oder einen Anker.
- **Feld:** Der Aktor wird zum Ankerpunkt gezogen, mit derselben Formel und
  `bondFieldFactor` wie bei einer Zellbindung (gesendeter und empfangener Anteil).
  - Für `d < R·softening` ist die Kraft linear in `d`. Der Anker wirkt also wie eine
    Feder mit Ruhelage 0.
  - Er hält **in alle Richtungen**, also auch seitlich und an Wänden.
- **Wirkung:**
  - Ein Aktor mit Anker lässt die Zelle nur noch um diesen Punkt drehen.
  - Zwei Anker halten die Zelle fest.
  - Eine gebundene Säule mit zwei verankerten Fußaktoren steht wie ein eingespannter Stab.
- **Lösen:** wie bei Bindungen:
  - Der Abstand zum Ankerpunkt übersteigt `R` (Abreißen).
  - Ein denkendes Gehirn steuert den Aktor 0,5 s lang abstoßend.
  - Bei Überresten klingt die Stärke mit der Halbwertszeit ab; mit Holz bleibt der Anker.
- **Speicherung:**
  - Gespeichert wird der Ankerpunkt mit seiner Stärke.
  - Beim Laden bleibt der Anker nur, wenn der Punkt auf einer Oberfläche der neuen Welt
    liegt; sonst wird er gelöst. Blocker selbst werden nicht gespeichert.
  - Alte Dateien haben keine Anker.
- **Momentaufnahme:** Anker als kurzer gelber Strich zum Punkt, mit kleiner Raute.
- **Klettern:** Anker an Wänden erlauben das Hochklettern, wenn sich die Aktoren
  abwechselnd lösen und neu einrasten. Ob das gewollt ist, ist eine offene Entscheidung.
  Es wäre ein Weg zu Höhe, der nichts mit Stängeln zu tun hat.

## Schalter und Standardwerte

Wie bei den Bindungen schaltbar, Standard **aus**, nur wirksam in `OPEN_WORLD`. NONE- und
Trainings-Policies bleiben unverändert.

| Parameter | Bedeutung | Vorschlag |
|---|---|---|
| `groundAdhesion` | Stufe A an/aus | aus |
| `groundAnchors` | Stufe B an/aus (setzt A voraus) | aus |
| `groundStickiness` `s_B` | Eigenhaftung der Blocker (≤ 0) | 0 |
| Bodenfarbe | Materialfarbe für die Affinität | Grau 0,5/0,5/0,5 (alle Typen) |
| Anker auf | Welche Blockertypen verankern | alle (Boden, Wand, Plattform) |

GUI: zwei Kästchen „Experimental“ und ein Schieber für die Eigenhaftung im Umweltpanel.
Studie: `-Dzone.study.ground=true`, `-Dzone.study.anchors=true`.

## Tests (klein, deterministisch)

1. **Aus = alt:** Ohne Schalter und in Trainings-Policies gibt es keine Bodenkraft;
   bestehende Mechaniktests bleiben unverändert.
2. **Abheben:**
   - Eine Zelle mit anziehenden unteren Aktoren bleibt bei kleiner Aufwärtskraft liegen und
     hebt erst bei größerer ab.
   - Ein neutraler Aktor hebt sofort ab.
   - Die Schwelle wird wie beim Druck-Test in Zellgewichten angegeben.
3. **Affinität:** Graue Aktoren haften voll, rote nur mit `a` und werden nie abgestoßen.
4. **Rollen:**
   - Eine Zelle mit Drehimpuls rollt mit Bodenfeld weniger weit als ohne.
   - Seitliches Gleiten ohne Drehung bleibt in Stufe A unverändert (dokumentierte Grenze).
5. **Periodisch:** Der Bodenkontakt an der Weltkante wirkt gleich wie in der Mitte; der
   Boden ist breiter als die Welt.
6. **Anker:**
   - Einrasten nach 0,5 s nur mit Schalter, passender Farbe und Anziehung.
   - Lösen durch aktives Abstoßen.
   - Pausierte Zellen lösen nicht.
   - Abreißen über `R` hinaus.
   - Überreste: Abklingen bzw. Holz.
7. **Säule:** Die Säule aus `CellBondTest` mit zwei verankerten Fußaktoren steht nach dem
   Seitenstoß noch (Spitze bleibt über dem Fuß, Neigung < 10°). Ohne Anker kippt sie wie
   bisher.
8. **Wand:** Eine verankerte Zelle hängt an einer Wand gegen die Schwerkraft. Eine nur mit
   dem Bodenfeld haftende Zelle rutscht ab.

## Messung

Zonenläufe wie beim Bindungsvergleich (4 Seeds, Winter 50 %, Bindungen an):
- mit Bodenfeld;
- mit Bodenfeld und Ankern;
- als Referenz nur Bindungen.

Neue Kennzahlen:
- verankerte Aktoren;
- verankerte gebundene Körper;
- deren Überstand und Spanne;
- Anteil der Körper, die mit dem Boden verbunden sind.

Momentaufnahmen zeigen die Anker.

## Grenzen und offene Punkte

- **Grund nach oben zu wachsen:** Ohne Vorteil für Höhe, etwa mehr Licht weiter oben,
  wird Halt allein vermutlich nur festsitzende Matten erzeugen. Das ist getrennt zu
  entscheiden; der Vorschlag steht in der TODO.
- **Kosten:** Anker und Felder sind wie Bindungen zunächst kostenlos.
- **Kein echtes Reibungsmodell:** Seitlicher Halt entsteht nur über Anker. Coulomb-Reibung
  am Kontakt wäre eine Alternative, ist aber eine neue Mechanik.

## Offene Entscheidungen

1. Stufe A und B je ein eigener Schalter, beide Standard aus? (Vorschlag: ja)
2. Keine Abweisung durch den Boden, nur abgeschwächte Haftung? (Vorschlag: ja)
3. Eine Materialfarbe für alle Blocker oder je Typ eine eigene? (Vorschlag: zunächst eine,
   je Typ später als Nischenversuch)
4. Anker auch an Wänden und Plattformen, also Klettern erlauben? (Vorschlag: ja, aber
   eigener Unterschalter „nur Boden“)
5. Eigenhaftung `s_B` als GUI-Regler aufnehmen? (Vorschlag: ja, Standard 0)
