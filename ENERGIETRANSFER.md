# Energie zwischen Zellen

Seit 17.10.2026 gelten die konservativen Transferregeln getrennt für Primär- und
Sekundärenergie: [Mehrere Energieformen](MEHRERE_ENERGIEN.md).

## Befund und Korrektur (16.10.2026)

Der bisherige Transfer erhält grundsätzlich die Gesamtenergie, hat aber mehrere Steuerungsfehler:

- `min(Maximum, Ausgang * Distanzfaktor)` sättigt schon bei sehr kleinen Ausgängen. 0,5 liefert genauso viel wie 1,0; auch die Distanz ist dann wirkungslos.
- Die Mengen gelten pro Aufruf, ohne Simulationszeit zu berücksichtigen.
- Die Prüfung `Energie > 0,1` schützt keine Reserve: Ein Transfer kann sie unterschreiten.
- Sofortiges Abbuchen/Gutschreiben bevorzugt frühe Empfänger und Spender. Frisch erhaltene Energie kann im selben Schritt weitergereicht werden, abhängig von der Zellreihenfolge.

Jetzt werden Anforderungen aus einem gemeinsamen Energiezustand gesammelt und gleichzeitig verrechnet. Knappe Spenderenergie und Empfängerkapazität werden proportional verteilt. Empfangene Energie steht ab dem nächsten Schritt zur Weitergabe bereit. Abgehende Energie schafft ebenfalls erst im nächsten Schritt neue Empfangskapazität; die Regel ist bewusst konservativ.

Die Rate ist `Maximum pro Sekunde * Ausgang * Distanzfaktor * deltaTime`:

| Kanal | Maximum pro Aktor und Simulationssekunde | Regel |
|---|---:|---|
| Delivery | 0,3 | Freiwillige Abgabe, alle Ports zusammen bewahren 0,1 Reserve |
| Absorption | 0,6 | Entzug vom Nachbarn, kann ihn vollständig leeren |

Die Maximalraten entsprechen den bisherigen Werten bei 60 Hz und vollem Ausgang in unmittelbarer Nähe. Absorption benötigt weiterhin keine Zustimmung des Spenders. Sie kann daher auch dessen Delivery-Reserve angreifen. Der vorhandene periodische Distanzfaktor reduziert den Durchsatz, vernichtet aber keine Energie. Blocker, Selbstkontakte und Ziele außerhalb der übergebenen Zellliste übertragen keine Energie.

Das ist noch kein spezialisierter Organismuskanal: Pro Aktor wird weiterhin nur das vom Sensor ausgewählte Ziel angesprochen, ohne DNA- oder Verwandtschaftsprüfung. Eine Kooperation kann Energie verteilen; andere Zellen können aber auch Energie entziehen. Aus dieser Prüfung folgt keine Aussage darüber, wie leicht Evolution passende Steuerungen findet.

## Prüfungen

`EnergyTransferTest` prüft kleine/halbe Ausgänge, Absorption, alle Ports bei knapper Reserve, mehrere Spender bei fast vollem Empfänger, Zellketten, Reihenfolge, Energiegrenzen, Nullzeit, Distanz und gleiche Raten bei 30/60/120 Hz. Ein dichter Mischtest permutiert 30 Zellen bei gleichzeitiger Abgabe und Absorption. Ein Test verwendet echte Sensorerkennung und das vollständige Welt-Update.

Ein isolierter Verband aus Quelle → Weiterleiter → beschatteter Zelle läuft 60 Simulationssekunden mit festen Kontakten und Ausgängen, ohne Physikbewegung, Teilung oder Gehirnkosten. Die Quelle erhält 0,015 Energie/s, gibt 0,012 weiter; der Weiterleiter gibt 0,006 weiter. Jede Zelle zahlt 0,003 Grundumsatz/s. Ergebnis: Quelle bleibt bei 0,8; die beiden anderen steigen von 0,2 auf 0,38. Die Bilanz stimmt mit Einspeisung minus Grundumsatz überein. Das bestätigt die energetische Möglichkeit solcher Versorgung, nicht die mechanische Stabilität eines Verbands.

## GUI und laufende Versuche

Die neue Environment-Box regelt die Sonnenstrahlrate von 0 bis 100. Die Rate gilt pro Simulationssekunde und `sunRaySpacingPx` Weltbreite (standardmäßig 60 px); Weltbreite und Tagesintensität bestimmen die Gesamtzahl. Energie pro Strahl bleibt konstant. Die Bedienboxen liegen unten und können bei schmalen Fenstern gescrollt werden. Der Regler ist nach einem GUI-Neustart verfügbar.

Die bereits gestarteten Funktionsvergleiche verwenden weiterhin ihren ursprünglichen Code (Startcommit `674ffcc`), einschließlich der alten Transferregel. Ihre Ergebnisse dürfen nicht als Messung des korrigierten Transfers ausgegeben werden. Die anschließenden reinen Gehirn-Signalproben verwenden keine Welt-Energietransfers. Bestehende Populationen können unter der neuen linearen Steuerung deutlich andere Übertragungsmengen entwickeln.
