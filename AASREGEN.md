# Aasregen: kontrollierter Nahrungserwerb

Implementiert am 17.10.2026 als `TrainMode.CARRION_RAIN`.
Ergänzt um `CARRION_CONVERSION` mit Nahrung aus Energie 001 statt 000.

## Aufbau und Regeln

- Acht Kammern à 400 × 600 mit Boden und durchgehenden Seitenwänden.
- Eine lebende Gründerzelle je Kammer, identisch auf 0,18 Primärenergie initialisiert.
  Alle anderen Reserven beginnen leer. Das ist eine ausdrücklich neue Trainingsepisode,
  keine biologische Teilung. Gehirnzustand, Alter und Bewegung werden zurückgesetzt.
- Neue zufällige Genome verwenden den kompakten Default mit 48 versteckten Neuronen
  (24 gemeinsam + drei Zustandsgruppen à acht). Die 316 Eingänge und 133 Ausgänge
  kommen hinzu. Geladene Genome behalten ihre Größe; Evolution ist nicht auf 48 begrenzt.
- Keine Sonne, unabhängig vom Sonnenregler. Keine biologische Teilung. Kräfte,
  Größensteuerung, Farben und Absorption bleiben evolvierbar; normale Physik gilt.
- Ein Überrest mit Radius 8 und 0,12 Primärenergie pro Kammer zu Beginn, danach alle
  sechs Simulationssekunden. Der erste liegt kurz über der Gründerzelle; weitere
  fallen von oben. Alle Kammern erhalten jeweils dieselbe relative Regenposition.
- Futter sind normale `Cell`-Überreste mit `RemainsBrain`, ohne aktive Abgabe.
  Nur die normale, konservative Energieabsorption kann ihre Vorräte entnehmen.
  Keine zusätzliche Energieform oder Umwandlung wird hierfür eingeführt.
- Höchstens zwölf Futterzellen pro Kammer. Ungegessenes Futter wird nach 60 Sekunden
  entfernt; dabei verworfene Restenergie wird separat als `expired` protokolliert.
  Dieses Ablaufdatum gilt ausschließlich für das künstlich erzeugte Trainingsfutter.
- Gehirnkosten und maximale Lebensdauer gelten gemäß GUI-Einstellungen. Hohe Gebühren
  oder sehr kurze Lebensdauer können die Aufgabe unlösbar machen.
- Gestorbene Kandidaten werden entfernt. Ihre Genome bleiben nur für die Auswahl
  verfügbar; es entstehen keine zusätzlichen essbaren Konkurrenten.

## Auswahl und Beobachtung

Eine Episode dauert 120 Simulationssekunden. Fitness:

`Überlebenssekunden / 120 + 0,1 × aus Futter entnommene Energie der angebotenen Form`

Kammerflucht setzt die Fitness auf null. Ein unverändertes Elitegenom und sieben
mutierte Nachkommen aus den drei Besten starten die nächste Episode. Jede Kopie
hat einen eigenen Gehirnzustand. Ohne Nahrungsaufnahme reichen selbst bei kostenlosen
Gehirnen 0,18 Energie höchstens 60 Sekunden (Grundverbrauch 0,003/s).

Biologische Teilung ist hier gesperrt und kann nicht erlernt werden. Vermehrung findet
als kontrollierter Genomwechsel zwischen Episoden statt. Der Aufnahmebonus belohnt
Nahrungserwerb; er ist kein reines Effizienzkriterium und kann auch erhöhten Verbrauch
mit anschließendem Nachfüllen honorieren. Fortpflanzungserfolg und Kooperation sind
noch keine Auswahlkriterien dieses ersten Nahrungserwerb-Trainings.

Statuszeile: Generation, Episodenzeit, insgesamt aufgenommene Futterenergie und
Futterzufuhr der laufenden Episode; Bestwert der letzten Episode. Das Konsolenprotokoll
enthält zusätzlich Überlebenszeit und Aufnahme des besten Kandidaten sowie gesamte
Futterzufuhr, Gesamtaufnahme, verworfene Restenergie und Restfutter. Geprüfte Bilanz:
`Futterzufuhr = Gesamtaufnahme + Verfall + Restfutter`. Zufuhr ist eine externe Subvention dieses
Versuchs, keine Energieerzeugung durch die Zellen. Energie des am Episodenende noch
vorhandenen Futters wird mit der alten Versuchswelt verworfen.

Load / Load & Merge übernehmen lebende neuronale Genome aus der geladenen Datei
und starten damit einen neuen Vergleich bei Generation 1. Positionen, Vorräte und
mitgeladene Überreste sind kein zusätzlicher Vorteil. Reine Überrestdateien werden
abgewiesen. Der Modus, Regler und Episodenzustand werden nicht in `.cells` gespeichert.

## Kleine Testwelten

`CarrionRainTrainStrategyTest`, deterministische DNA ohne Synapsen, keine Gehirngebühr:

- Fester Absorber: 120/120 Sekunden überlebt, rund 1,180 Primärenergie aufgenommen.
- Keine Absorption: Tod nach 60 Sekunden, keine Aufnahme; Fitness 0,500.
- Die maximal eingestellte Sonne bleibt ohne Wirkung; Teilung trotz hohem
  Teilungswunsch gesperrt. Überreste, Energiebilanzen/Startwerte, Futterbegrenzung,
  Verfall und Auswahl des Elitegenoms werden geprüft.

Das belegt die Lösbarkeit durch normale Absorption. Es ist noch kein Nachweis,
dass zufällige Netze diese Lösung zuverlässig oder schnell evolvieren. Ein statischer
Absorber kann diese erste Aufgabe lösen; aktive Suche, Ressourcenumwandlung und
mehrzellige Kooperation benötigen spätere, anspruchsvollere Szenarien.

## GUI

Oben `New…` → `CARRION_RAIN` → `Erstellen` → `Start`.
Der Dialog ersetzt die bisherige Welt erst beim Erstellen; Abbrechen lässt sie weiterlaufen.
Auch NONE, HIGH_ENERGY, HIGH_POSITION(2/3) und TEST_A/B/C sind wählbar.
Die neue Welt beginnt pausiert, mit neuer Kamera, ohne alte Auswahl und mit neuer
Statistik. Gehirn-, Lebensdauer- und Sonnenregler bleiben eingestellt. Die Sonderregel
„keine Sonne“ ist im Dialog und in der Statuszeile des Aastrainings sichtbar.
Alternativ ist weiterhin `--trainMode=CARRION_RAIN` beim Programmstart möglich.

## Variante CARRION_CONVERSION

`New…` → `CARRION_CONVERSION` → `Erstellen` → `Start` (nach GUI-Neustart).

Gleiche Kammern, Futterpositionen, Zufuhrmenge und Auswahlregeln, aber das Futter
enthält ausschließlich 0,12 Energie 001 und **keine Primärenergie**. Die lebenden
Gründer beginnen wie bisher mit 0,18 Primärenergie und leeren übrigen Speichern.
Neue Genome starten mit inaktiver Umwandlung. Es werden keine fertigen Stoffwechsel-
Lösungen eingeschleust. Die Kapazität und Route müssen durch Mutation/Selektion
entstehen; auch eine aus einer Datei geladene passende Route ist verwendbar.

Zum Überleben über die Anfangsreserve hinaus benötigt eine Zelle beides:

1. Normale Aufnahme der Aasenergie 001.
2. Genetische Umwandlung 001 → 000 mit ausreichendem Durchsatz.

Beispiel aus dem deterministischen Kontrollversuch: Absorption + Route 001 → 000
mit Kapazität 0,1 überlebt 120 Sekunden; der Sieger nimmt 1,68 Energie 001 auf.
Absorber ohne Enzym verhungern trotz positiver 001-Reserve. Eine falsche Quellform
oder das Enzym ohne Absorption reichen ebenfalls nicht. Die Gegenprobe verwendet
fest verdrahtete Netze und kostenlose Gehirne; sie beweist Lösbarkeit, noch keine
zuverlässige Evolution zufälliger Netze unter normalen Gehirngebühren.

Die Futterbilanz gilt für 001 separat: `Zufuhr = Absorption + Verfall + Restfutter`.
Nach der Absorption dissipieren bei Umwandlung weitere 20 %; diese Verluste sind
nicht noch einmal Futterverfall. Status und Konsolenprotokoll nennen den Futtercode.
Der Primärenergiebalken eines solchen Überrests ist leer, obwohl 001 noch gefüllt ist.

Details der Enzyme und weitere Grenzen: `ENERGIEUMWANDLUNG.md`.
