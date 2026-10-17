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

Erwartete Dynamik (noch nicht durch Langlauf belegt): In 001 genügt die Startroute
001 → 000 mit positiver Kapazität (eine Mutation). In 010/100 ist X → 000 über eine
Quellmutation erreichbar. Wegen der Umwandlungsgrenze 0,1/s × Kapazität halten dort
lebende Zellen meist volle Fremdvorräte und hinterlassen sie beim Tod als Aas.

Tests: `SunSpectrumTest` (reine Zonen, lineare periodische Mischung, Häufigkeit der
Mischformen, Bilanz absorbierter Strahlen je Zone, Einheitsmodus unverändert);
50 gezielte Sonnen-/Snapshot-/GUI-Tests, Protokoll `target/energy-environment-tests.log` (Neulauf).
