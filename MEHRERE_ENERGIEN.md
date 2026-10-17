# Getrennte Energieformen: erster Umsetzungsschritt

Stand: 17.10.2026. Zwei Speicher sind implementiert; Umwandlungen, neue
Verbrauchszuordnungen und tote Zellen als Überreste folgen in späteren Schritten.

## Speicher und bestehende Regeln

| Energieform | Kapazität pro Zelle | Anfangsvorrat neuer Zellen | Bisherige Funktion |
|---|---:|---:|---|
| `PRIMARY` / Primärenergie | 1,0 | 1,0 wie bisher | Grundumsatz, Gehirnkosten, Überlebens- und Teilungsbedingungen |
| `SECONDARY` / Sekundärenergie | 1,0 | 0,0 | Getrennter Vorrat, noch ohne eigenen Verbrauch |

Die Namen legen keine chemische Bedeutung und keine spätere Zellrolle fest.
Jeder Speicher wird unabhängig auf [0,1] begrenzt. Eine leere Sekundärreserve ist
nicht tödlich; eine volle Sekundärreserve rettet noch nicht vor Primärenergiemangel.
Altersgrenze und bisherige Todesregel bleiben unverändert. Beim Tod verschwinden
Zellen weiterhin mitsamt verbleibenden Vorräten: Überreste sind noch nicht umgesetzt.

Die vorhandenen Methoden `getEnergy()`, `setEnergy(value)` und `getMaxEnergy()`
bezeichnen ausdrücklich Primärenergie. Typisierte Überladungen erlauben den Zugriff
auf jede Form. `getTotalEnergy()` summiert die Speicher lediglich für Bilanzen;
es ersetzt keine Überlebens-, Kosten- oder Trainingsentscheidung.
Die beiden Vorräte liegen in skalaren Feldern; pro Zelle entstehen keine neuen
Gehirne oder Ressourcen-Maps/-Arrays.

## Sonne und GUI

In **Environment → Sun energy form** ist Primary oder Secondary wählbar.
Standard ist Primary. Ein Strahl liefert seine bisherige Energiemenge ausschließlich
in den gewählten Speicher. Die Einstellung wird vor einer Strahlcharge gelesen;
eine Änderung während der Berechnung gilt für die nächste Charge.

Ein voller Speicher leitet Energie nicht in den anderen um. Strahlzahl und Energie
pro Strahl werden durch die Typwahl nicht erhöht. Sonnenkontakt bleibt das bisherige
binäre Sensorsignal, unabhängig von der gewählten Form.

In den **Zelldetails** stehen beide Vorräte. Darstellungs-Snapshots enthalten beide
getrennt und unveränderlich. Bisherige Energiebalken, Auswertungen und Save-best-Auswahl
verwenden weiterhin Primärenergie. Die Quellenauswahl gilt für die laufende Anwendung
und wird nicht als Weltkonfiguration in `.cells` gespeichert.

Für einen ersten Versuch kann man eine lebende Population zunächst mit Primärenergie
versorgen und dann zeitweise Sekundärenergie einstrahlen. Dauerhaft ausschließlich
Sekundärenergie ersetzt die notwendige Primärversorgung in diesem Schritt noch nicht.

## Austausch und biologische Teilung

Die bisherigen Aktorbefehle für Absorption und Delivery wirken auf **beide Formen
getrennt**. Jede Form besitzt ihre eigene Spenderreserve, Empfängerkapazität und
gleichzeitige Verrechnung. Es gibt keine Umwandlung und keinen Ausgleich zwischen
den Formen. Ein voller Primärspeicher verhindert deshalb keine Sekundäraufnahme.

Die bisherigen Raten gelten **pro Energieform und Aktor**:

- Delivery höchstens 0,3/s × Ausgang × Distanzfaktor; Abgabe bewahrt 0,1 dieser Form.
- Absorption höchstens 0,6/s × Ausgang × Distanzfaktor; Entzug kann den Vorrat leeren.

Bei zwei gefüllten Formen ist damit die Summe möglicher Transfers größer; jeder
einzelne Vorrat bleibt konserviert. Beide Durchläufe sind unabhängig, ohne sofortige
Weitergabe frisch empfangener Energie im selben Schritt. Bei ausschließlich primären
Vorräten entfällt der zweite Transferdurchlauf; es bleibt nur eine Prüfung, ob überhaupt
Sekundärenergie vorhanden ist. Diese Änderung enthält kein neues Performance-Versprechen
für Populationen mit zwei aktiven Energieformen.

Biologische Teilung verteilt **jede** Form mit demselben vom gewählten Aktor bestimmten
Kindanteil. Sie erzeugt keine zusätzliche Reserve. Eine blockierte Teilung verändert
keinen Speicher. Gehirn und Sensoren des Kindes werden erst mit seinen zugewiesenen
Vorräten ausgewertet. Trainingsgründer bleiben dagegen ausdrücklich neue, normalisierte
Episoden mit Primärenergie 1 und Sekundärenergie 0; sie kopieren keine Elternvorräte.

## DNA- und Dateikompatibilität

Die neuronale Schnittstelle bleibt bei **213 Eingängen und 133 Ausgängen** für zwölf
Aktoren. Die vorhandenen Eigen-/Nachbar-Energiesignale melden weiterhin Primärenergie.
Es gibt noch kein gesondertes Sekundärsignal und keine unabhängig nach Energieform
steuerbaren Austauschbefehle. Neue Sensoren, spezialisierte Steuerung, Verbrauch und
Umwandlung brauchen einen weiteren expliziten Schritt samt DNA-Migrationsregel.

`Cell.serialVersionUID` und das bisherige Feld `double energy` bleiben erhalten.
Die neue Sekundärreserve ist ein zusätzliches `double`-Feld; beim Lesen alter Dateien
initialisiert Java es mit null. Alte gespeicherte Energie wird unverändert als
Primärenergie übernommen, weder aufgeteilt noch um zusätzliche Vorräte ergänzt.
Neue Dateien speichern beide Werte, auch bei Load & Merge.

Die Kompatibilitätsprüfung verwendet eine **echte Datei vom Stand vor der Änderung**:
[Fixture und Prüfsumme](src/test/resources/energy/README.md). Die Rückwärtsrichtung
ist nicht zugesichert: Alte Programmversionen kennen die neuen Reserven nicht und
könnten sie beim erneuten Speichern verlieren. Neue Spielstände sollten daher mit
diesem oder einem neueren Stand weiterverwendet werden.

## Prüfungen

158 gezielte Tests bestanden. Neue Tests sichern getrennte Kapazitäten, echte alte
Dateien, neue Save/Load-Rundläufe, Sonnenquellenwahl, getrennte Transfers bei knappen
Reserven, vollen Empfängern, 30/60/120 Hz, umsortierten dichten Kontakten und periodischen
Welträndern. Teilung, gescheiterte und wiederholte Teilung sowie die unveränderte
Primär-Sterberegel sind ebenfalls geprüft.

Eine deterministische Welt mit drei Zellen und fest verdrahteter Weitergabe läuft
1000 Schritte mit vollständiger Physik und echter Sensorerkennung: Die Summe der
Sekundärenergie bleibt 1,2; die anfänglich 2,4 Primärenergie sinkt exakt um den
Grundumsatz aller drei Zellen. Sekundärenergie erreicht dabei die dritte Zelle.
Das prüft den Kreislauf ohne Umwandlung und ohne zusätzliche Energiequelle.

Zusätzlich bestanden bestehende Prüfungen für Sonnenindex, Physik, Darstellung,
Dateiimport, Trainingsgenerationen und HIGH_POSITION3. Es wurden keine langen
Evolutionsläufe gestartet und keine Standardkosten, Strahlraten oder Altersgrenzen
für diesen Schritt geändert.

Kleine Prüfauswahl:

```powershell
mvn test '-Dtest=MultiEnergyStorageTest,MultiEnergyTransferTest,MultiEnergyLifecycleTest,MultiEnergySunTest,MultiEnergyWorldTest,MainControllerLayoutTest'
```
