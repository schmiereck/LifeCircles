# Dauerhafte Zellverbindungen – Entwurf

Status: **Stufe 1 und 2 umgesetzt** (17.10.2026), Stufe 3 und 4 Entwurf. Ziel sind Stängel und Stützstrukturen
aus gemeinsamer DNA, die Gehirnpausen und einzelne Todesfälle überstehen. Dazu soll
die Abweisung fremder Zellen kommen, die starkem Druck aber nachgibt. Grundlage sind
die bestehenden Aktorfelder (`SensorActorForceCellCalcService`), keine zweite Mechanik.

## Umsetzung Stufe 1 und 2 (17.10.2026)

Entscheidungen des Nutzers: Überreste lassen nach 300 s Halbwertszeit los, „Holz“ (∞) ist
wählbar; Haftung kostet vorerst nichts; Mimikry soll möglich, aber durch weitere Merkmale
auflösbar sein; erst Stufe 1/2, dann messen.

- Aktiv nur in der Policy `SimulationStepPolicy.OPEN_WORLD` (Default-NONE-Welt der GUI und
  der Zonenstudie) und wenn der GUI-Schalter „Cell adhesion“ an ist (Standard an).
  `SimulationStepPolicy.NONE` (Testwelten) und Trainings bleiben unverändert.
- **Tonus** (`CellAdhesionService.settleForces`, nach dem Denken): wirksame Kraft
  `F ← F_alt + (Befehl − F_alt)·(1 − e^(−dt/τ))`, τ = 0,5 s. Pausierte Zellen
  (`BrainEnergyService.prepare(..., holdForces)`) behalten ihre Kräfte; Energieabgabe
  und Teilungswunsch werden weiter genullt. Überreste (`becomeRemains(true)`) behalten
  ihre Kräfte und verlieren sie mit Halbwertszeit `remainsForceHalfLife` (300 s, GUI 1–60 min
  oder ∞). Gespeicherte Überreste behalten ihre gespeicherten Felder (ältere Dateien: 0).
- **Erkennung** (`SensorActorForceCellCalcService.Affinity`):
  `a = Ähnlichkeit(Aktorfarben)^k · Ähnlichkeit(Körperfarben)^k`, k = 4, Ähnlichkeit =
  `CellType.similarity` (1 − RMS-Abstand). Anziehung `s < 0` wirkt als `s·a − s·(1 − a)·ρ`,
  ρ = 0,5 (GUI 0–100 %). Abstoßung wirkt wie bisher auf alle. Die Körperfarbe ist
  vererbt und mutiert nur langsam bei der Teilung, das Gehirn kann sie nicht steuern.
  Aktorfarben-Mimikry allein genügt daher nicht: Gleiche Aktorfarbe auf fremdem Körper
  wird weiter abgewiesen (Test). Eine langsam evolvierte Körperfarbe kann das aufheben.
- **Überwindbar** (Test, zwei Zellen Radius 20, alle Aktoren −90, fremde Aktorfarbe,
  äußerer Druck 3 s): Ein bis zwei Zellgewichte (Schwerkraft 29,4) werden mit Abstand
  abgehalten, zehn Zellgewichte drücken die Zellen in Kontakt. Grenze bei ρ = 0,5 etwa
  drei Zellgewichte. Bei Radius 10 wirkt das Feld nur ein Viertel so stark (Größenskalierung
  `r₁·r₂/r_max²`), dort reicht ein Zellgewicht.
- Tests: `CellAdhesionTest` (6), GUI in `MainControllerLayoutTest`; unverändert grün:
  `ForceSignTest`, `PhysicsRegressionTest`, Gehirnenergie-, Notatmungs-, Drossel- und
  Vererbungstests.
- Zonenstudie: Schalter `-Dzone.study.adhesion=false` für die Kontrolle; neue Spalten
  `attractive` (Anteil anziehender Aktoren lebender Zellen), `pausedAttractive` (dasselbe
  bei pausierten), `largestCluster` (größte Gruppe sich berührender Zellen inkl. Überresten,
  Spalt ≤ 2 px), `clusterRemains`, `clusterLift` (höchste Zelle dieser Gruppe über Gelände).

## Befund vor der Umsetzung

| Situation | Was mit den Aktorkräften passiert | Stelle |
|---|---|---|
| Gehirn denkt | Jeder Denkschritt überschreibt die Kraft vollständig aus dem Netzausgang | `CellBrainService.applyOutputs` |
| Gehirn pausiert (001 fehlt) | **Alle Kräfte werden auf 0 gesetzt** | `Cell.setBrainPaused` |
| Zelle stirbt (Überrest) | **Alle Kräfte werden auf 0 gesetzt** | `Cell.becomeRemains` |
| Zwei Aktoren begegnen sich | Feldkraft unabhängig von Farbe oder Verwandtschaft, jeder Aktor wirkt auf alle Aktoren in Reichweite | `SensorActorForceCellCalcService` |

Zusammenhalt gibt es damit nur, solange **alle** beteiligten Gehirne bezahlt denken
und dauerhaft Anziehung ausgeben. In NONE-Welten pausieren Zellen regelmäßig, deshalb
zerfallen Verbände dort. Das erklärt, warum es in HIGH_POSITION3 besser klappte: Dort
wird Energie vor jedem Schritt aufgefüllt, Gehirne pausieren also nie, und Alterstod
ist abgeschaltet.

Zweitens unterscheidet das Feld nicht zwischen Partnern. Anziehung wirkt auf
Geschwister wie auf Fremde. Wer Fremde abweisen will, stößt auch die eigenen Nachbarn ab.

## Vorschlag in vier Stufen

Jede Stufe ist einzeln nutzbar und messbar. Alle bleiben Feldkräfte zwischen
Aktorpaaren. Actio = Reactio bleibt erhalten, weil jede Regel symmetrisch in beiden
Partnern ist.

### Stufe 1: Tonus – Kräfte bleiben ohne Denken bestehen

- Der Netzausgang ist ein **Sollwert**. Die wirksame Aktorkraft folgt ihm mit einer
  Zeitkonstante `τ_tonus` (Vorschlag 0,5 s). Das dämpft auch Zittern durch schwankende
  Ausgänge.
- **Pause:** Die zuletzt wirksame Kraft bleibt erhalten. Sie wird nicht mehr auf 0
  gesetzt. Energieabgabe und Teilungswunsch werden weiter genullt, wie bisher.
- **Tod:** Überreste behalten ihre Aktorkräfte und lassen sie mit der Halbwertszeit
  `T_starre` abklingen (Vorschlag 300 s; ∞ wäre „Holz“: tote Stützzellen tragen weiter,
  wie Xylem). Überreste verschwinden weiterhin erst, wenn ihre Vorräte leer sind.
- Keine DNA-Änderung. Die Pausensemantik in AGENTS.md („Pause erhält Gedächtnis und
  Takte“) wird um „und die zuletzt gehaltenen Kräfte“ ergänzt.

### Stufe 2: Erkennung – Haftung nur zwischen passenden Aktorfarben

Aktorfarben (RGB) sind schon heute Gehirnausgänge, wirken aber nicht mechanisch.
Daraus wird ein Schlüssel-Schloss-Prinzip, ähnlich den Cadherinen echter Zellen:

- Affinität zweier Aktoren: `a = 1 − |Farbe_A − Farbe_B| / √3`, also 1 bei gleicher
  Farbe und 0 bei maximal verschiedener.
- Ein **anziehendes** Feld (Stärke `s < 0`) wirkt auf den Partner als
  `s · a^k` (Anziehung). Der Rest wirkt als Abweisung, `+|s| · (1 − a^k) · ρ`.
  Vorschlag: `k = 4` (scharfe Erkennung), `ρ = 0,5`.
- Ein **abstoßendes** Feld wirkt wie bisher auf alle.
- Ein Ausgang genügt damit für beides: Haftaktoren binden Gleichfarbige und weisen
  Fremde ab. Geschwister erben die Netze, erzeugen also mit hoher Wahrscheinlichkeit
  ähnliche Farben.
- **Überwindbar:** Das Feld ist begrenzt, durch die vorhandene lineare Glättung im Kern
  (`actorFieldSofteningFraction`) und die kurze Reichweite (bei Radius 20 und zwölf
  Aktoren etwa 9 px vor der Oberfläche). Höchstwert je Aktorpaar bei den aktuellen
  Werten: rund 48 bei maximaler Abstoßung. Die Kontaktabstoßung (125 je px
  Überlappung) und der Druck eines Haufens übersteigen das. Fremde werden also
  ferngehalten, unter Last aber durchgedrückt. Wie stark, wird mit `ρ` im Test kalibriert.
- Farben kann man nachahmen: Ein fremdes Netz, das die richtige Farbe ausgibt, haftet
  mit. Das ist gewollt offen (Mimikry). Eine Erkennung über DNA-Abstand wäre
  fälschungssicher, aber eine neue Regel. Siehe offene Fragen.

### Stufe 3: Einrasten – Bindungen zwischen zwei Aktoren

Tonus allein ergibt klebrige, flüssige Klumpen, weil jedes Feld auf alle Nachbaraktoren
wirkt. Stabile Formen brauchen paarweise Verbindungen:

- Zwei **anziehende** Aktoren mit Affinität `a ≥ a_min` (Vorschlag 0,8), die sich
  `t_lock` lang (Vorschlag 0,5 s) im Feldkern nahe sind, **rasten ein**. Jeder Aktor
  hat höchstens einen Partner.
- Eine Bindung ist dasselbe Feld wie bisher, nur mit **eingefrorener Stärke**: der
  schwächeren Anziehung beider Aktoren beim Einrasten. Der Gehirnausgang verändert sie
  nicht, auch Pause und Tod nicht. Bei Überresten klingt sie mit `T_starre` ab.
- Zwei Bindungen zwischen denselben Zellen an benachbarten Aktoren sperren die
  Drehung. Das ergibt ein steifes Glied; dreieckige Packungen sind laut MECHANIK.md
  rund 38-mal biegesteifer als Ketten.
- **Lösen:**
  - aktiv, wenn ein Gehirn den Aktor `t_lock` lang abstoßend steuert;
  - mechanisch, wenn der Abstand die Feldreichweite übersteigt (das Feld ist dort 0)
    oder die Zugkraft eine Bruchgrenze überschreitet.
  Zug und Druck können eine Verbindung also immer brechen.
- Ein gebundener Aktor wählt seinen Partner bevorzugt als Sensorziel. Damit bleiben
  Wahrnehmung und Energieabgabe entlang des Stängels stabil, der heutige Weg über
  `EnergyTransferCellCalcService`.
- Bindungen entstehen in einem sequentiellen Durchgang nach der Kraftberechnung,
  sortiert nach `sensingId`, und bleiben dadurch deterministisch. Speicherung erfolgt
  über Java-Serialisierung (Objektbezüge bleiben erhalten). Alte Spielstände haben
  keine Bindungen.

### Stufe 4: Geburtsbindung – Wachsen wie ein Faden

Das ist der wichtigste Schritt in Richtung Stängel. Ein Stängel entsteht nicht dadurch,
dass sich Fremde zusammenfinden, sondern aus wiederholter Teilung, die zusammenbleibt
(wie Fadenalgen oder Moos-Protonema):

- Ist der Teilungsaktor der Mutter bei der Geburt anziehend, rastet er sofort mit dem
  gegenüberliegenden Aktor des Kindes ein. Das Kind übernimmt dabei die Aktorfarbe.
- Kein neuer Ausgang: Die vorhandene Kraft am Teilungsaktor steuert, ob die Tochter
  angebunden bleibt oder frei wird. Ein Gehirn, das bei jeder Teilung nach oben
  anziehend teilt, baut einen Faden nach oben.
- Optional später: ein Eingang je Aktor „gebunden ja/nein“ (zwölf Eingänge, im
  `EnergyInputLayout` angehängt, Migration ohne Synapsen). Dann können Gehirne
  Position und Rolle im Verband erkennen.

## Was das für die Evolution bedeutet

- Ein Stängel übersteht Pausen und den Tod einzelner Zellen. Mit „Holz“ trägt er
  sogar tot weiter. Die Lebensdauer muss dann nicht künstlich hoch bleiben.
- Die unteren Zellen eines Stängels bekommen wenig Licht. Ihr Grundumsatz ist aber
  klein (000). Pausieren sie, halten sie trotzdem. Versorgung von oben ist nötig, damit
  sie leben, aber nicht mehr, damit der Stängel steht. Das senkt die evolutionäre
  Hürde deutlich.
- Fremdabweisung schützt einen Verband vor Überwucherung und erzeugt erstmals einen
  mechanischen Unterschied zwischen „wir“ und „die anderen“.

## Kosten

Mechanische Arbeit ist heute kostenlos (MECHANIK.md, Punkt 5). Vorschlag: Bindungen
zunächst kostenlos lassen und messen. Kleben dann alle Zellen zu einem Teppich
zusammen, folgt ein kleiner 000-Preis je gehaltener Bindung und Sekunde. Der würde
das Lösen bei Mangel automatisch fördern.

## Tests (deterministisch, klein)

1. Zwei haftende Zellen, eine Pause: Mit Tonus bleiben sie unter seitlichem Zug
   verbunden, ohne Tonus trennen sie sich.
2. Gleiche Farbe zieht an, fremde Farbe stößt ab. Unter starkem äußerem Druck werden
   die fremden Zellen trotzdem in Kontakt gedrückt.
3. Eine Bindung übersteht Pause und Tod, klingt beim Überrest ab und bricht bei
   Überdehnung sowie bei aktiver Abstoßung.
4. Geburtsbindung: Fünf Teilungen nach oben stehen unter Schwerkraft als Säule.
   Gemessen wird die Höhe; zur Kontrolle dasselbe ohne Bindung.
5. Kraftbilanz: Die Summe aller Feld- und Bindungskräfte ist 0, auch über die
   periodische Grenze.

In der Zonenstudie kommen neue Kennzahlen hinzu: Zahl der Bindungen, größter
verbundener Verband, dessen Höhe über Gelände (statt `maxLift` einzelner Zellen).

## Parameter (Vorschläge, GUI-regelbar)

| Parameter | Vorschlag | Bedeutung |
|---|---|---|
| `τ_tonus` | 0,5 s | Nachlauf der wirksamen Kraft zum Sollwert |
| `T_starre` | 300 s (∞ = Holz) | Abklingen von Kräften/Bindungen bei Überresten |
| `k` | 4 | Schärfe der Farberkennung |
| `ρ` | 0,5 | Anteil Abweisung bei fremder Farbe |
| `a_min` | 0,8 | Mindestaffinität zum Einrasten |
| `t_lock` | 0,5 s | Kontaktdauer zum Einrasten bzw. Abstoßdauer zum Lösen |
| Bruchgrenze | Feldreichweite | Abstand, ab dem eine Bindung reißt |

## Offene Entscheidungen

1. Sollen tote Zellen tragen („Holz“, `T_starre = ∞`) oder nach einiger Zeit loslassen?
2. Sollen Bindungen zuerst kostenlos sein?
3. Erkennung über Aktorfarbe (vom Gehirn steuerbar, nachahmbar) oder über DNA-Ähnlichkeit?
4. Reihenfolge: Vorschlag ist erst Stufe 1 und 2 umsetzen und messen, dann 3 und 4.
