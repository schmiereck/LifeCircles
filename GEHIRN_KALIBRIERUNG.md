# Kalibrierung der Gehirnkosten

Ziel: günstige, funktionierende Gehirne für spätere Ressourcenwelten unter NONE.
HIGH_POSITION-Training bleibt ein vorbereitendes Strukturtraining und wird hier
nicht auf Energieeffizienz umgestellt. Produktionskosten und Standardparameter
werden durch diese Experimente nicht verändert.

## Versuchsaufbau

`BrainEnergyCalibrationTest` ist optional. Die sechs Aufgaben sind CONSTANT,
AND, OR, XOR, DELAY (vorherige Eingabe ausgeben) und COUNTER (Modulo vier mit
zufälligen Ticks, Pausen und vorrangigem Reset). Zwei Eingänge, ein Ausgang.
Keine Alters-/Phaseneingabe oder vorherigen Zielwerte. Die Logikfälle enthalten
die vollständige Wahrheitstabelle; jede unabhängige Frage startet ohne Gedächtnis.
Gedächtnisaufgaben starten pro Folge frisch und erhalten ihren Zustand innerhalb
der Folge. Prüfung: acht unbekannte Folgen mit je 64 Entscheidungen.

Zwei getrennte Untersuchungen:

- **acquire:** zufällige Netze, vier Anfangsarchitekturen (kein Hidden Layer,
  vier bzw. acht Neuronen, zwei Layer mit je vier Neuronen). Zufällige zusätzliche
  Verbindungen erlauben auch Rückkopplung. Keine vorbereiteten Lösungen.
- **compress:** explizite funktionierende Kontrollnetze, jeweils ergänzt um einen
  überflüssigen Layer mit zwölf Neuronen und 24 Eingangsverbindungen. Keine
  Funktion hängt von diesem Layer ab. Das ist Erhalt/Verdichtung, kein Beleg
  dafür, dass Evolution das Kontrollprogramm selbst entdeckt hat.

Population 32, höchstens 20 % Eliten, Eltern aus dem oberen Viertel. Mutation
mit der heutigen `NeuralNetwork.mutate()`-Implementierung, Rate 0,1 und Stärke
0,2, einschließlich Struktur, Neuronentypen, Modulen und vererbten Faktoren.
Meta-Mutation wie bei Zellen, jedoch mit explizitem Zufallsgenerator. Keine
Backpropagation und kein Crossover. Dynamische Layer-Steuerung bleibt aktiv.
Mutation, Auswahl, Meta-Mutation, Anfangspopulationen und Aufgaben sind über
getrennte Seeds reproduzierbar. Kostenvarianten verwenden gleiche Anfangspools
und Aufgabenfolgen. Verzweigte Evolutionswege sind erwartungsgemäß verschieden.

Bewertung in Energieeinheiten pro Sekunde:

```text
Kosten = Speicherfaktor × gespeicherte Synapsen × 3e-10
       + Arbeitsfaktor × mittlere ausgeführte Synapsen × 60 × 2e-8
Nutzen = Belohnungsskala × (Fehler des besten konstanten Ausgangs − Netzfehler)
Auswahlwert = Nutzen − Kosten
```

Fehler ist der mittlere quadratische Fehler. Die Belohnungsskala ist kein
pauschaler Energieertrag pro richtiger Entscheidung: Bei Skala 0,003 bringt
eine perfekte XOR-Lösung gegenüber dem konstanten Ausgang höchstens
0,003 × 0,25 = 0,00075 Energie/s zusätzlich. Grundverbrauch ist hier ein gemeinsamer
konstanter Term und beeinflusst die Rangfolge nicht. Das ist ein wirtschaftlicher
Auswahlversuch, noch keine vollständige Zellökologie mit Tod und Fortpflanzung.

Trainingsfolgen werden alle 25 Generationen erneuert. Prüfungen beeinflussen
die Auswahl nicht. Erfolg bedeutet mindestens 95 % korrekte Ausgaben und MSE
unter 0,02; beim konstanten Zielwert MSE unter 0,001. Logikfälle müssen damit
alle korrekt sein. Entdeckungszeit wird in Abständen von 25 Generationen erfasst;
ein späterer Verlust der Fähigkeit wird separat gezählt.

Als Ressourcenwächter werden Kandidaten mit mehr als 4096 gespeicherten Synapsen,
128 äußeren Neuronen oder nicht endlichen Ausgaben abgewertet. Auftreten wird
pro Lauf protokolliert; das sind Versuchsgrenzen, keine neuen DNA-Grenzen.
Neuron-/Aktivierungszahlen des äußeren Netzes schließen Modulinneres nicht ein;
Speicher- und Synapsenausführungskosten enthalten Module.

## Pilot ohne Kosten

600 Generationen, drei Starts, sechs Aufgaben, 18 Läufe:

- CONSTANT, AND, OR, XOR jeweils 3/3 erfolgreich.
- DELAY 0/3: Prüfgenauigkeit etwa 77–86 %; noch nicht gelöst.
- COUNTER 0/3: etwa 35–44 %; noch nicht gelöst.

Somit dürfen Fehlschläge der Gedächtnisaufgaben nicht bereits als Folge hoher
Energiekosten ausgelegt werden. Die bekannten Positivkontrollen lösen dagegen
alle Aufgaben einschließlich des zufällig zurückgesetzten Zählers.
17 Routineprüfungen bestanden: Kontrollen, neutrale Zusatzstruktur, Gedächtnis
und Reset, konstante Referenzen, reproduzierbare Evolution und exakte
Übereinstimmung der Kosten mit dem Zell-Energiemodell bei 60 Hz.

Rohdaten: `docs/brain-calibration/pilot/`. Die größeren Serien und ihre
Auswertung werden in einem separaten Schritt ergänzt.

## Wiederholen

```powershell
mvn test '-Dtest=BrainEnergyLabTest'
mvn test '-Dtest=BrainEnergyCalibrationTest' '-Dbrain.energy.study=true' '-Dbrain.energy.output=target/brain-energy-acquire' '-DargLine=-Xmx1g'
mvn test '-Dtest=BrainEnergyCalibrationTest' '-Dbrain.energy.study=true' '-Dbrain.energy.phase=compress' '-Dbrain.energy.generations=600' '-Dbrain.energy.output=target/brain-energy-compress' '-DargLine=-Xmx1g'
```

Standardraster: Speicherfaktoren 0/1/100/1000, Arbeitsfaktoren 0/0,25/1/4,
zehn Seeds. Erwerb: 1200 Generationen. Zusätzlich konfigurierbar:
`brain.energy.tasks`, `brain.energy.reward`, `brain.energy.seed_offset`,
`brain.energy.population`, `brain.energy.seeds`, `brain.energy.storage`,
`brain.energy.work`. Ergebnisse werden je abgeschlossenem Lauf geschrieben.
Die Kurzzeiten in `results.csv` enthalten Prüf-/Resetbuchhaltung und werden bei
gleichzeitig laufenden Serien nicht als belastbarer Performancevergleich verwendet.
