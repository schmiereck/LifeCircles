# Spektralzonen der Sonne

Umgesetzt am 17.10.2026. Ziel: Umweltdruck, damit sich andere Energieformen als 000
in Zellen ansammeln und als Aas zurückbleiben, ohne Sonderregeln in den Zellen.

- GUI unten „Environment“: Schalter „Spectral zones“. Aus = bisherige einheitliche
  Sonnenform (Auswahlliste); an = Auswahlliste deaktiviert, Form nach Ort.
- Vier gleich breite Zonen entlang der periodischen x-Achse: 000 | 001 | 010 | 100.
  Bei x = 0 grenzt 100 periodisch wieder an 000.
- Schmale Übergänge: 15 % einer Zonenbreite, mittig auf jeder Grenze (Default-Welt
  6400 px: Zonen je 1600 px, Mischstreifen je 240 px). Anteil der Nachbarform steigt
  darin linear von 0 auf 1. Jeder Strahl trägt trotzdem genau eine Form mit unveränderter
  Strahlenergie; gemischt wird nur über die Strahlhäufigkeit.
- Maßgeblich ist der Auftreffpunkt des Strahls. Nur absorbierte Strahlen ziehen eine
  Zufallszahl; im Einheitsmodus bleibt die Zufallsfolge unverändert.
- Darstellung: Strahlen in ihrer RGB-Farbe (000 weiterhin gelb), Zonen dezent hinterlegt.
- Die Einstellung ist eine globale, live änderbare Umweltgröße und wird nicht mit
  `.cells` gespeichert.
- **Feste Zonenbreite** (20.10.2026): `sunZoneWidth` / GUI-Regler „Spectral zone width“.
  0 = wie oben ein Zyklus je Welt; ein Wert > 0 wiederholt 000 | 001 | 010 | 100 alle
  4 × Breite px. Für breite Welten (`WORLD2`, `WELT2.md`).

- **Einstellbare Mischung** (29.10.2026, Nutzerentscheidung nach WORLD2-Langläufen: die Zonen bildeten eine
  sehr harte Grenze). Drei globale, live änderbare Größen, in der GUI und der Einstellungsdatei:
  - `sunZoneBackground` (GUI 0–25 %): Grundanteil **jeder** Form in **jeder** Zone. Die eigene Form behält
    1 − 3 × Grundanteil, z. B. 2 % → 94 % eigene Form. 25 % = überall gleichmäßiges Licht.
  - `sunZoneTransition` (GUI 0–100 % einer Zonenbreite, Standard 15 %): Breite eines Übergangs, mittig auf der
    Grenze. 0 = harte Kante; 100 % = rein nur genau in der Zonenmitte, das Licht ändert sich überall.
  - `sunZoneSoftness` (GUI 0–100 %, Standard 0 = linear): Kurvenform über den Übergang. Gemischt wird linear
    mit einer S-Kurve (6t⁵ − 15t⁴ + 10t³); an der Grenze immer 50:50, bei 100 % erreicht der Nachbar ein Viertel in
    den Übergang hinein nur 10 % statt 25 %.
  - Anteil einer Form = Grundanteil + (1 − 4 × Grundanteil) × Zonenverlauf. Die Standardwerte (0 / 15 % / linear)
    ergeben genau die bisherigen Zonen.
  - Ziehung: Die vorherrschende Form bekommt die kleinsten Zufallszahlen, die übrigen folgen in Zonenreihenfolge.
    So bleibt eine reine Zone für jede Zahl rein. Nicht bitgleich zu früheren Läufen (andere Zuordnung im Übergang).
  - Darstellung: Zonenhintergrund unverändert als Blöcke; nicht absorbierte Strahlen zeigen die vorherrschende Form.
  - Tests `SunSpectrumTest`: Grundanteil, Breite, Kurve, harte Kante, gleichmäßiges Licht, Summe 1, Ziehung nach
    Anteilen, Grenzen der Regler, absorbierte Strahlen mit Grundanteil (5 % je Fremdform in Zonenmitte).

## Sonne: ganzer Himmel, periodisch umgebrochen (29.10.2026)

Nutzerbeobachtung: Die Strahlen zielten so auf den Boden, dass er ganz abgedeckt war. Bei schrägem Licht blieben
dadurch oben links bzw. rechts Bereiche ohne Einstrahlung. Ein Beispiel ist die Morgensonne (35°) in einer 300 px
breiten Welt: Die Strahlen starteten bei x = −210 … 90 oberhalb der Welt und endeten an den Seitenwänden. Oben
rechts (x > 90 + 0,7 · y) kam nie ein Strahl an.

- Jetzt starten die Strahlen gleichmäßig über die ganze Oberkante (x in [0, Breite)), mit unveränderter Zahl je
  Breite. Sie laufen schräg weiter und brechen am Weltrand periodisch um, wie Zellen und Gelände. Jeder Strahl
  endet erst am Boden oder beim ersten Treffer, Seitenwände gibt es nicht mehr. So wird jeder Ort der Welt
  gleich stark beleuchtet, und Schatten wandern über die Naht.
- Umsetzung (`EnergySunCalcService`): Der Strahl wird ungefaltet verfolgt. Statt Zellen und Blocker als
  periodische Bilder zu kopieren, wird der Strahl um ganze Weltbreiten verschoben und gegen die Originale
  getestet. Es zählt der früheste Treffer über alle Verschiebungen. Der Projektionsindex deckt alle verschobenen
  Strahlkoordinaten ab. Die Zone des Treffers wird am gefalteten Ort bestimmt.
- `SunRay` trägt den ungefalteten Endpunkt (Start in [0, Breite)). Der Renderer zeichnet den Strahl in Stücken
  innerhalb der Welt.
- Tests `SunSkyTest`:
  - Eine Reihe von Zellen über die Naht hinweg bekommt in drei Höhen (oben, Mitte, unten) gleich viel Licht,
    ± 15 %; gemessen 0,87–1,14 des Mittels.
  - Jeder Strahl reicht bis zum Boden.
  - Der Schatten eines Dachs bricht über die Naht um.
- Angepasst: `SunContactTest` und `MultiEnergySunTest`, deren fester Mittelstrahl jetzt bei x = 150 startet, sowie
  `SunSpectrumTest`, der die Strahlenden gefaltet zählt. Index und Scan bleiben gleich (`SunIndexRegressionTest`,
  `PerformanceEquivalenceTest`).
- Nicht bitgleich zu früheren Läufen: andere Startpunkte, mehr Licht oben auf der Gegenseite und weniger dort, wo
  sich früher die Strahlen am Boden bündelten. Der Index wird etwas breiter, die Laufzeit wurde nicht neu gemessen.

Erwartete Dynamik (noch nicht durch Langlauf belegt): In 001 genügt die Startroute
001 → 000 mit positiver Kapazität (eine Mutation). In 010/100 ist X → 000 über eine
Quellmutation erreichbar. Wegen der Umwandlungsgrenze 0,1/s × Kapazität halten dort
lebende Zellen meist volle Fremdvorräte und hinterlassen sie beim Tod als Aas.

Tests: `SunSpectrumTest` (reine Zonen, lineare periodische Mischung, Häufigkeit der
Mischformen, Bilanz absorbierter Strahlen je Zone, Einheitsmodus unverändert);
50 gezielte Sonnen-/Snapshot-/GUI-Tests, Protokoll `target/energy-environment-tests.log` (Neulauf).
