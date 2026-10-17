# Acht getrennte Energieformen

Stand: 17.10.2026. Acht Speicher, vollständige Energie-Sensorik und Überreste sind
implementiert. Begrenzte, genetisch evolvierbare Umwandlung ist ergänzt;
siehe [ENERGIEUMWANDLUNG.md](ENERGIEUMWANDLUNG.md). Spezielle Verbrauchszuordnungen sind noch offen.
Aktueller Überblick: [ACTUAL.md](ACTUAL.md), Aufgaben: [TODO.md](TODO.md).

## RGB-Codes und Vorräte

| Code (R,G,B) | API-Bezeichnung | Anfangsvorrat einer neuen Zelle |
|---|---|---:|
| 000 | PRIMARY | 1,0 wie bisher |
| 001 | SECONDARY | 0,0 |
| 010 | RGB_010 | 0,0 |
| 011 | RGB_011 | 0,0 |
| 100 | RGB_100 | 0,0 |
| 101 | RGB_101 | 0,0 |
| 110 | RGB_110 | 0,0 |
| 111 | RGB_111 | 0,0 |

Jede Form hat eine unabhängige Kapazität 1. RGB-Komponenten des Typs sind ausschließlich
0 oder 1; 0,5, negative Werte und andere Komponenten werden zurückgewiesen. Die
Füllstände selbst sind kontinuierlich in [0,1]. Diese Klassifizierung ist unabhängig
von den bisherigen kontinuierlichen RGB-Farben der Zellen und Aktoren.

Die bisherigen untypisierten Energiemethoden bezeichnen weiter PRIMARY/000.
Gehirnkosten, Grundumsatz und Überlebens-/Teilungsbedingungen verwenden weiterhin
Primärenergie. Andere Vorräte können nur über eine vorhandene aktive Umwandlung
nach 000 dafür einspringen. Getrennte Verbrauchsregeln je Funktion bleiben offen.
`getTotalEnergy()` ist eine Bilanzhilfe, keine neue Überlebensbedingung.

Die bisherigen serialisierten Felder für Primär- und Sekundärenergie bleiben erhalten.
Die sechs weiteren Speicher werden nur bei Bedarf angelegt. Alte Dateien erhalten
keine neue Energie; fehlende Vorräte sind leer. Neue Dateien speichern alle vorhandenen
Werte und gegebenenfalls den Überrestzustand. Sie sollten mit diesem oder einem
neueren Programmstand weiterverwendet werden.

## Sonne, Austausch und Teilung

Environment → Sun energy form enthält alle acht Codes. Jeder Strahl liefert seine
bisherige Energiemenge in genau die gewählte Form. Die Einstellung gilt für eine
vollständige Strahlcharge; es gibt keine zusätzliche Gesamtenergie durch die Auswahl.
Ein voller Speicher leitet den Überschuss nicht in eine andere Form um.

Die vorhandenen Absorptions-/Delivery-Befehle wirken weiterhin gemeinsam auf alle
Formen, werden jedoch je Form getrennt bilanziert. Pro Form/Aktor gelten die bisherigen
Maximalraten 0,6/s für Absorption und 0,3/s für Delivery, jeweils multipliziert mit
Ausgang, Distanzfaktor und Zeitschritt. Delivery schützt 0,1 der betreffenden Form;
Absorption darf vollständig leeren. Knappheit und Empfängerkapazität werden proportional
aufgeteilt. Frisch erhaltene Energie wird erst im nächsten Schritt weitergereicht.

Leere Formen benötigen keinen eigenen Transferdurchlauf. Eine Belegungsmaske erkennt
vorhandene Formen, ohne bei rein primären Populationen sechs nicht angelegte Speicher
pro Zelle auslesen zu müssen. Acht gleichzeitig belegte Formen bedeuten trotzdem
mehr Arbeit; eine neue Welt-Performancezahl wurde nicht gemessen.

Biologische Teilung verteilt jede Form mit dem gewählten Kindanteil. Eine gescheiterte
Teilung verändert keinen Vorrat. Trainingsgründer sind frische Episoden mit den
obigen Anfangswerten, keine Vervielfältigung elterlicher Ressourcen.

## Eigene und benachbarte Energie-Sensoren

Alle acht eigenen Vorräte und die acht Vorräte des jeweils gewählten Nachbarn sind
getrennt lesbar. Leere Sensorkanäle und Blocker liefern null. Die Energiewerte werden
wie bisher nicht mit dem geometrischen Distanzsignal vermischt.

Für zwölf Aktoren hat die aktuelle Schnittstelle **316 Eingänge und 133 Ausgänge**:

- Eingänge 0..212: unveränderte ursprüngliche globale und Aktor-Sensorwerte.
- 213..219: sieben zusätzliche eigene Energiestände.
- 220..303: je sieben zusätzliche Nachbar-Energiestände für zwölf Aktoren.
- 304..315: je ein Nachbarsignal für den Überrestzustand.

Die Primärenergiewerte bleiben an ihren bisherigen Positionen. Indizes sind über
`EnergyInputLayout` adressierbar; neue Werte wurden angehängt, nicht zwischen alte
Sensorblöcke geschoben. Die Ausgabe-Schnittstelle bleibt unverändert. Unabhängige
Austauschbefehle je Form wären eine weitere, noch offene DNA-Erweiterung.

## DNA-Migration und Kosten

Alte Zellnetze mit 213 Eingängen und die zwischenzeitlichen Netze mit 304 Eingängen
bekommen beim Laden beziehungsweise beim Binden an eine Zelle zusätzliche unverbundene
Eingangsneuronen. Alte Positionen, Neuronen, Synapsen, Biases und Netzgedächtnis bleiben
erhalten. Die Migration verbraucht keine Zufallsziehungen aus dem Evolutionsgenerator.
Unabhängige kleine Labor-Netze werden nicht als Zellnetze umgedeutet.

Die angehängten Neuronen erzeugen keine zusätzlichen Synapsengebühren oder abgearbeitete
Verbindungen. Auslesen und Schreiben der längeren Sensorvektoren beansprucht allerdings
CPU und Speicher. Neue Genome können von Anfang an Verbindungen zu diesen Signalen
haben. Neue Standard-Zellnetze besitzen seit dem kompakten Gründer-Schritt insgesamt
48 versteckte Neuronen, unabhängig von der Größe der Sensor-Schnittstelle.

Gleiche alte Ausgaben beweisen nicht, dass alte DNA sofort auf neue Ressourcen reagiert:
Die neuen Eingänge sind zunächst unverbunden. Eine solche Reaktion muss sich durch
weitere Mutation/Evolution entwickeln. Erweiterte Genotypen haben außerdem zusätzliche
mögliche Eingangsknoten für künftige Strukturmutation.

Eine echte Datei vor der Mehrenergie-Erweiterung liegt unter
[src/test/resources/energy](src/test/resources/energy/README.md). Getestet sind außerdem
warme rekurrente Netze vor und nach der Migration, mit identischen Ausgaben und
abgerechneten Synapsenausführungen.

## GUI, Überreste und Nachweise

Zelldetails und unveränderliche Darstellungs-Snapshots enthalten alle acht Vorräte.
Bisherige Energiebalken und Fitness-/Save-best-Auswahl verwenden weiterhin Primärenergie.
Überreste bleiben normale Zellen und werden über die normale Absorption geleert;
Regeln und Grenzen: [UEBERRESTE.md](UEBERRESTE.md).

Die aktuelle Änderung ist durch **222 gezielte Tests** geprüft: RGB-Codes, Eigen- und
Nachbarsensoren, DNA-/Dateimigration, Reserven, Sonnenquellen, Austausch, Teilung,
Überreste, Darstellung und bisherige Netz-/Physik-/Trainingsregeln. Es wurden keine
weiteren langen Evolutionsläufe gestartet.

```powershell
mvn test '-Dtest=EightEnergySensorTest,MultiEnergyStorageTest,MultiEnergyTransferTest,MultiEnergyLifecycleTest,MultiEnergySunTest,RemainsTest,RemainsImportTest'
```
