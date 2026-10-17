# Überreste als normale Zellen

Umgesetzt am 17.10.2026, Commit `367d33f`. Es gibt keinen zusätzlichen Materie-
oder Körpermaterialtyp. Ein Überrest ist dieselbe `Cell` mit einem gespeicherten
Todeszustand und ihren tatsächlich übrigen Energiereserven.

## Tod und feste Steuerung

Im NONE-Modus wird eine sterbende Zelle zum Überrest, wenn mindestens ein Vorrat
noch positiv ist. Alterstod erhält alle verbliebenen Vorräte; Primärenergiemangel
kann andere Formen hinterlassen. Sind bereits alle Vorräte leer, wird sie entfernt.
Die Altersgrenze und das bisherige Kriterium Primärenergie <= 0 bleiben bestehen.

Die Zelle behält Position, Größe, Rotation, Bewegung, Farbe und Sensor-Aktor-Punkte.
Ihre neuronale DNA und ihr Netzgedächtnis werden durch einen gemeinsamen kleinen
Inertcontroller ersetzt. Es werden keine weiteren neuronalen Entscheidungen getroffen.
Aktorkräfte, aktive Absorption, Delivery und Teilungswunsch werden auf null gesetzt.
Gehirn-Eingabeaufbereitung und Denken werden in der Simulation übersprungen.

Die Größe bleibt auf dem Todesstand, auch bei einem noch wachsenden Kind. Alter,
Grundumsatz und Gehirngebühren laufen nicht weiter. Schwerkraft, Trägheit, Dämpfung,
passive Abstoßung, fremde Kräfte, Kollisionen und periodische Geometrie bleiben aktiv.
Der Körper kann somit fallen, sich bewegen und andere Körper mechanisch beeinflussen.

Ein Überrest nimmt keine Sonnenenergie auf. Er bleibt ein undurchsichtiger Körper,
der Strahlen abschirmt. Er kann nicht durch Sonnenlicht oder eine freiwillige Spende
neu aufgefüllt werden. Im gegenwärtigen Modell gibt es weder Wiederbelebung noch
automatische Umwandlung seiner Vorräte.

## Aasfressen

Eine lebende Nachbarzelle kann die Vorräte über die vorhandene Absorption entziehen.
Alle acht Energieformen behalten ihre eigenen Kapazitäten, Durchsatzgrenzen und
gleichzeitigen Bilanzen. Vollständiges Leeren ist erlaubt; die Delivery-Reserve 0,1
gilt nicht für Absorption. Sobald alle Vorräte leer sind, wird der Überrest am Ende
des Simulationsschritts entfernt.

Es wird beim Tod nichts gutgeschrieben. Ohne Abnehmer bleiben positive Reserven
liegen; es gibt keinen zusätzlichen automatischen Zerfall. Dadurch können sich
Überreste ansammeln. Sie sparen Gehirn-/Sensoraufbereitung, beanspruchen aber weiterhin
räumliche Suche, Kontakte, Physik und Darstellung. Ihre Anzahl ist deshalb neben
der lebenden Population eine wichtige Messgröße für größere Welten.

## Sensoren, GUI und Dateien

- Für jeden Aktor gibt es ein angehängtes Nachbarsignal `neighborRemains`: 1 bei
  einem Überrest, sonst 0. Die acht Energiefüllstände des gewählten Nachbarn bleiben
  einzeln erkennbar. Kein Nachbar und Blocker liefern hier jeweils null.
- Alte DNA erhält die zusätzlichen Eingänge zunächst unverbunden. Sie kann bereits
  mit bisherigen Absorptionsbefehlen Aas fressen; eine gezielte Reaktion auf neue
  Signale muss über weitere Evolution/Mutation entstehen.
- Überreste tragen eine helle Kreuzmarkierung. Zelldetails zeigen „Überrest“ und
  alle Vorräte; anstelle des neuronalen Netzes erscheint die feste Steuerung.
- Die Statistik trennt `Living` und `Remains`; die Populationskurve zählt Lebende.
- Normales Save/Load und Load & Merge erhalten Todeszustand und alle Reserven.
  Alte Dateien enthalten keine Todesmarkierung und werden als lebende Zellen geladen.
- „Save best“ exportiert nur lebende Kandidaten. Überreste können nicht als Eltern
  für biologische Teilung oder neue Trainingsgenome verwendet werden.

## Training und Experimente

`SimulationStepPolicy.NONE` erhält Überreste, und DefaultTrainStrategy verwendet
diese Regel. Andere Trainingsepisoden entfernen sterbende Zellen weiterhin wie zuvor,
sofern ihre Policy nicht ausdrücklich `keepRemains()` aktiviert. HIGH_POSITION3
behält seine gesicherte feste Mitgliedschaft. Schon importierte Überreste sind
weiterhin passiv und werden nicht als Elterngenome ausgewählt.

Damit bleiben bestehende Trainingsbewertungen von einer unbeabsichtigten Änderung
ihrer Mitgliedschaft ausgenommen. Neue NONE-Läufe mit Aas sind dagegen ein neues
Ökologiemodell und nicht direkt mit älteren Populationsergebnissen gleichzusetzen.

## Prüfungen

222 gezielte Tests bestanden, einschließlich:

- Alterstod und Primärenergiemangel mit getrennten Restvorräten; kein leeres Aas.
- Keine Gehirnausführung, Gebühren, Alterung, Größenänderung oder Teilung nach dem Tod.
- Passive Bewegung bei unveränderten Vorräten über 1000 Schritte.
- Vollständiges Absaugen aller acht Formen durch echte Sensorerkennung, auch am Weltrand.
- Keine Sonnenaufnahme und keine freiwillige Auffüllung von Überresten.
- Keine Aktivität selbst bei nachträglich falsch gesetzten alten Aktorbefehlen.
- Save/Load, Load & Merge, konsistente Snapshots und lebende Save-best-Auswahl.
- Rekurrente DNA-Migration, Mehrressourcenbilanzen sowie bisherige Physik-/Netz-/Trainingstests.

```powershell
mvn test '-Dtest=RemainsTest,RemainsImportTest,EightEnergySensorTest'
```
