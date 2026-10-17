# Jahreszeiten und Sonnenkurve

Umgesetzt am 17.10.2026.

## Jahreszeiten

- GUI unten „Environment“: „Year length“ 0–60 Simulationsminuten (0 = aus, ständiger
  Sommer, bisheriges Verhalten) und „Winter light“ 0–100 % des Sommerlichts.
  Standard: aus, Winterlicht 25 %. Beide Werte sind live änderbar, global und nicht in `.cells` gespeichert.
- Jahresfaktor `winter + (1 − winter) · (½ + ½·cos 2π·Phase)`: Phase 0 = Hochsommer
  (1), Phase ½ = Mittwinter (Winterfaktor). Er multipliziert die bisherige
  Tag/Nacht-Intensität, verändert also die Strahlzahl, nicht die Energie je Strahl.
- Die Phase schreitet um `dt / Jahreslänge` fort; eine live geänderte Jahreslänge
  ändert daher nur das Tempo, ohne Sprung. Neue Welten beginnen im Hochsommer.
- Wirkt auf jede Sonnenform, auch zusammen mit Spektralzonen (`SPEKTRALZONEN.md`).

## Sonnenkurve im Populationsdiagramm

- Gelb: angebotene Sonnenenergie pro Simulationssekunde =
  Intensität (Tag/Nacht × Jahreszeit) × Weltbreite / Strahlabstand × Strahlrate × Energie je Strahl.
  Eigene Skala: oben = größter Wert im Fenster. Weiß wie bisher: lebende Zellen.
  Label „Sun: … energy/s offered“ zeigt den aktuellen Wert. Episoden ohne Sonne: 0.
- Abtastung jetzt in **Simulationszeit** statt einmal pro Echtzeitsekunde: alle 5 s ein
  Punkt, 300 Punkte = 25 Minuten (12,5 Tag/Nacht-Zyklen). Sonnenwert ist der Mittelwert
  über das Intervall, Population der Stand am Abtastzeitpunkt. Vorher zeigte das
  Diagramm 100 Echtzeitsekunden und damit oft weniger als einen Tag.

Tests: `SeasonTest` (Jahresfaktor, Mittwinter skaliert Tag/Nacht exakt, weniger Licht
auf einer Zelle, kein Phasensprung bei Längenänderung, Validierung, Abtastung/Mittelwert/
Historiengröße/Reset).
