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

Erwartete Dynamik (noch nicht durch Langlauf belegt): In 001 genügt die Startroute
001 → 000 mit positiver Kapazität (eine Mutation). In 010/100 ist X → 000 über eine
Quellmutation erreichbar. Wegen der Umwandlungsgrenze 0,1/s × Kapazität halten dort
lebende Zellen meist volle Fremdvorräte und hinterlassen sie beim Tod als Aas.

Tests: `SunSpectrumTest` (reine Zonen, lineare periodische Mischung, Häufigkeit der
Mischformen, Bilanz absorbierter Strahlen je Zone, Einheitsmodus unverändert);
50 gezielte Sonnen-/Snapshot-/GUI-Tests, Protokoll `target/energy-environment-tests.log` (Neulauf).
