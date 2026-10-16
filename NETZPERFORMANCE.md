# Netzperformance nach den Logikkorrekturen

Neuere Untersuchung: [Netze ohne Änderung ihrer Ausgaben vereinfachen](NETZVEREINFACHUNG.md).

Aktuelle Umsetzung: [DNA-erhaltende Ausführung mit gleichen Energiegebühren](NETZAUSFUEHRUNG.md).

Messung vom 15. Oktober 2026, Java 25. Die Ausgangsbasis enthaelt bereits die
Korrekturen an Struktur, Steuerung, Gradienten, Training und Modulmutation.
Die beiden Laeufe verwenden dieselben Seeds, Netzdefinitionen und Eingaben.
Alle aufgezeichneten Pruefsummen fuer Aufbau, Vorwaertsrechnung, gespeicherte
Verbindungen und mutierte Nachkommen stimmen zwischen vorher und nachher ueberein.

## Messprobe

`NetworkPerformanceStudyTest` misst einzeln Aufbau, Vorwaertsrechnung,
Speicherkostenzaehlung und Mutation. Je drei Wiederholungen mit 16 Netzen;
Vorwaertsrechnung und Kostenzaehlung jeweils 3.200 Aufrufe, nach 100 Aufwaermrunden.
Ein Thread; Allokationen ueber den JDK-Threadzaehler. Zeitwerte sind Mediane
dieser kurzen Probe, keine garantierten Laufzeiten grosser Welten.

| Probe | Vorher | Nachher |
| --- | ---: | ---: |
| 16 Standardnetze aufbauen, temporaere Objekte | 388,5 MB | 12,8 MB |
| 16 Standardnetze aufbauen, Zeit | 124,8 ms | 40,7 ms |
| 3.200 Standardnetz-Aufrufe, temporaere Objekte | 3,46 MB | 3,46 MB |
| 3.200 Standardnetz-Aufrufe, Zeit | 106,4 ms | 101,1 ms |
| 3.200 Modulnetz-Aufrufe, temporaere Objekte | 1,81 MB | 0,109 MB |
| 3.200 Modulnetz-Aufrufe, Zeit | 15,55 ms | 15,42 ms |
| 3.200 Standardnetz-Kostenzaehlungen, temporaere Objekte | 1,39 MB | 0,012 MB |

Der klare Gewinn liegt beim Aufbau und bei temporaeren Objekten. Die Streuung
der Vorwaertszeiten erlaubt **keine belastbare Aussage ueber hoehere laufende
Simulationsgeschwindigkeit**. Fuer Mutation ergibt sich ebenfalls kein isolierter
Optimierungsnachweis. Die Modellkorrekturen koennen unabhaengig davon das Verhalten
und die Kosten evolutionaer entstandener Populationen aendern.

Rohdaten: [vorher](docs/performance/network-before.csv),
[nachher](docs/performance/network-after.csv).

## Umgesetzt

- `connectLayers()` sammelt neue Synapsen und erweitert das globale Array einmal
  pro verbundenem Layerpaar. Das beseitigt die quadratische globale Kopierarbeit
  beim regulaeren Aufbau. Einzelne Strukturmutationen nutzen weiter einfache Arrays.
- Modulingaben gehoeren zum Ausfuehrungszustand jeder Instanz und werden wiederverwendet.
  Innere Netze erzeugen keine verworfene Ergebniskopie. Der oeffentliche Netzaufruf
  liefert weiterhin ein neues Ergebnisarray, das der Aufrufer behalten darf.
- Die Kostenzaehlung verwendet ihren Besuchsspeicher wieder. Ergebnisse werden nicht
  gecacht; auch spaetere Aenderungen an geteilten Definitionen bleiben sichtbar.
- Neue dynamische Layer erhalten ein skalares erstes Neuron als guenstigen Taktgeber.
  Vorhandene Modul-Taktgeber werden nicht umgebaut. Zufallsziehungen beim normalen
  skalaren Aufbau bleiben erhalten.
- Neuronzaehlung ohne Listenkopien; Gradienten verwenden die vorbereitete Ausfuehrungsspur
  und benoetigen keine wiederholte Suche nach dem Ziellayer einer Synapse.

## Bewusst beibehaltene Grenzen

- Die Modultopologie wird weiterhin geprueft. Ein Versionszaehler allein waere noch
  unzuverlaessig: Layer-/Neuronarrays sind oeffentlich veraenderbar. Zuerst muesste
  dieser Zugriff gekapselt und jeder Umbau ueber kontrollierte APIs gefuehrt werden.
- Inaktive feste Layer werden weiterhin sicher genullt. Nur bei einem Zustandswechsel
  zu nullen wuerde bei direkt geaenderten Aktivitaetsflags oder Wertespeichern alte
  Rueckkopplungswerte zulassen. Eine ausschliessliche Mutations-/Zustands-API ist
  auch hier Voraussetzung fuer sicheres Ueberspringen.
- Globale Denkfrequenz und Auswahlintervalle bleiben unveraendert. Auswahlintervalle
  zaehlen weiterhin Simulationsaufrufe, Rechenkosten ausgefuehrte Entscheidungen.
  Vor einer variablen Denkfrequenz braucht es ein gemeinsames Zeitmodell fuer
  Gedächtnis, Kosten und Trainingsintervalle.

```powershell
mvn test '-Dtest=NetworkPerformanceStudyTest' '-Dnetwork.performance=true' '-Dnetwork.performance.output=target/network-performance.csv'
```
