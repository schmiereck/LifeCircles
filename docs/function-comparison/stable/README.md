# Funktionsvergleich: automatische Messzusammenfassung

144 Vergleichsläufe für 18 Genome, zwei Sonnenfolgen, normale und feste Steuerung.
Kalibrierung: unterschiedliche Gründerwinkel, stabile Online-Mittelwerte und Varianzen.

| Speicherstand | Gebühren | Modelle | Median Zellsekunden-Vorteil normal | Min / Max | Positive Paare |
|---|---|---:|---:|---:|---:|
| Früh (1000 s) | source | 9 | 0.000 % | 0.000 / 0.000 % | 0/18 |
| Früh (1000 s) | zero | 9 | 0.000 % | 0.000 / 0.000 % | 0/18 |
| Spät (10000 s) | source | 9 | 0.000 % | -3.062 / 3.271 % | 4/18 |
| Spät (10000 s) | zero | 9 | 0.000 % | -3.528 / 1.839 % | 6/18 |

Die festen Muster sind keine optimierten statischen Gegenspieler. Die zwei Sonnenfolgen
und Herkunftsgenome bilden eine explorative Stichprobe. Rohe Aktivität und veränderte
angewendete Ausgaben beweisen allein keinen biologischen Nutzen. Die synthetischen
Signalproben testen Einfluss auf Ausgaben, keine Fitness. `signal-summary.json` und
`signals.csv` trennen rohe Ausgaben, angewendete Ausgaben und angeforderte Teilungswahl.
`max_applied_feature` bezeichnet pro Probe nur die größte Änderung; weitere können vorhanden sein.

Der erste Marker wird oberhalb einer numerischen Schwelle von 1e-9 gezählt;
damit ist noch keine biologische Relevanzschwelle festgelegt.

Methodik: [FUNKTIONSVERGLEICH.md](../../../FUNKTIONSVERGLEICH.md).
Vorversuche werden separat aufbewahrt und nicht in die Zusammenfassung eingemischt.
