# Prüfung der Netzlogik, Mutation und Trainingssteuerung

Stand: 15. Oktober 2026. Produktionslogik unverändert. Die Prüfung enthält kleine,
deterministische Versuche und Codeanalyse; keine langen Evolutions- oder Sprachmodellläufe.

## Umsetzungsstand

- Strukturmutationen korrigiert: tats�chliche Neuronl�schung, vollst�ndige Layerl�schung,
  beidseitiges Entfernen von Synapsen, keine Verbindungen zu Eingaben, aggressive
  Mutation ohne leere Zufallsbereiche und ohne Verschieben fester Layer.
- `StructuralMutationTest`: sechs regul�re Tests, darunter 960 Strukturmutationsschritte
  mit Graphpr�fung, Kopien und endlichen Ausgaben. Zusammen mit Vererbungs-, Port-
  und Mutationsfaktorregressionen: 40 Tests erfolgreich.
- Weitere Befunde werden im Anschluss bearbeitet.

## Bewertung

Die Grundidee ist brauchbar: Ein vererbbares Netz steuert Zellfunktionen, Zustandsbits
schalten Entwicklungsprogramme, und dynamische Layer können Rechenarbeit sparen und
Werte als Gedächtnis halten. Vorwärtsrechnung, gekapselte Modulausführung und gewöhnliche
Gewichtsvererbung funktionieren in den vorhandenen kleinen Regressionen.

**Die aktuelle Gesamtkonstruktion ist aber noch keine zuverlässige Grundlage für die
Bewertung langer Trainingsläufe.** Strukturelle Mutation verletzt die Graphstruktur,
Gradiententraining verwendet falsche Ausführungszustände, und die Trainingsstrategien
erzeugen teilweise keine Trainingsnachkommen. Das sind konkrete Implementierungsfehler
beziehungsweise Abweichungen von den dokumentierten Strategien.

Unabhängig davon passen sehr häufige Genomänderungen bei jeder Zellteilung und die
Auswahl einzelner Zellen nur eingeschränkt zum Ziel eines mehrzelligen Organismus mit
stabiler DNA und verschiedenen Zellfunktionen. Diese Modellentscheidungen müssen getrennt
von Fehlerkorrekturen untersucht werden.

## Wichtigste Befunde

| Priorität | Befund | Nachweis |
| --- | --- | --- |
| 1 | Neuronlöschung entfernt das Neuron nicht; anschließendes Hinzufügen erzeugt doppelte IDs | deterministischer Test |
| 1 | Layerlöschung hinterlässt Synapsen zu nicht mehr vorhandenen Neuronen | deterministischer Test |
| 1 | Backpropagation benutzt die Aktivitätsflags des nächsten Schritts | analytischer Fall mit numerischem Vergleich |
| 1 | Rückkopplungsgradient verwendet einen anderen Quellwert als der Vorwärtslauf | numerischer Gegenbeweis |
| 2 | Bei 16 ausgewählten Gewinnern lassen die Höhenstrategien keine Plätze für Trainingsnachkommen | deterministische Selektionsrunden |
| 2 | HIGH_POSITION2 bevorzugt bei gleicher Höhe Randzellen vor mittigen Zellen | deterministische Selektionsrunde |
| 2 | Gedächtnisreset setzt Layerphase und Aktivitätsflags nicht zurück | identische Eingabesequenz liefert anderes Ergebnis |
| 2 | Zustand 0 schaltet bei Initialzellen andere Layer als bei Nachkommen | deterministischer Test |
| 2 | Energieabrechnung erfasst Modularbeit unvollständig | exakt gezählte Beispielverbindungen |
| 2 | Modul-Biasmutation verändert Werte sogar bei Mutationsstärke 0 | deterministischer Test |

### 1. Strukturelle Mutation ist nicht konsistent

`NeuralNet.removeNeuron()` entfernt aus `layer.getNeuronList()`. Diese Methode liefert
eine **Kopie**. Synapsen werden entfernt und die ID wird freigegeben, aber das Neuron
bleibt im tatsächlichen Layerarray und wird weiterhin ausgeführt. Beim nächsten Hinzufügen
vergibt `fetchNextFreeId()` die freigegebene ID erneut: Zwei lebende Neuronen schreiben
und lesen denselben Wert, dieselbe Eingangssumme und dasselbe Delta.

`removeHiddenLayer()` entfernt nur Layer mit genau einem Neuron. Dabei bleiben deren
Synapsen in `synapseArray` und an den übrigen Endpunkten eingetragen. Im Test besitzt
das Netz danach keine versteckten Layer, aber zwei Verbindungen zu einem entfernten
Neuron. Eine anschließende Genomkopie verwirft diese Verbindungen, weil deren Endpunkte
nicht in der Kopiermap stehen. Eine bloße Kopie kann dadurch die wirksame Struktur verändern.

`addRandomSynapse()` kann trotz des Ausschlusskommentars auf ein Eingabeneuron zielen:
Wird die Ausgabeschicht gleichzeitig als Quell- und Zielschicht gewählt, setzt die
Ausweichregel das Ziel auf Schicht 0. `setInputs()` überschreibt Eingabewerte direkt;
die hinzugefügte Verbindung hat keine reguläre Vorwärtswirkung.

`mutateAggressively()` kann bei einem zulässigen Netz ohne versteckte Layer
`nextInt(0)` aufrufen. Es kann außerdem vor den festen Zustandslayern einfügen und damit
deren Indexbedeutung verschieben. Dieser Pfad wird von den normalen Geburten nicht benutzt,
ist aber für die getrennten Netzexperimente relevant.

**Empfehlung:** Zuerst eine gemeinsame Mutations-API für Hinzufügen und Entfernen.
Nach jedem Strukturumbau müssen IDs eindeutig, Endpunkte lebend, Ports gültig und
Adjazenzlisten mit dem globalen Synapsenarray konsistent sein. Änderungen an der
Reihenfolge fester Zustandslayer brauchen eine ausdrücklich festgelegte Regel.
Danach kurze, zufallsgesteuerte Mutationsketten mit diesen Invarianten prüfen.

Quellen: `NeuralNet.removeNeuron`, `removeHiddenLayer`, `addRandomSynapse`,
`mutateAggressively`; `Layer.getNeuronList`; `ValuesNeuronValueFunction.fetchNextFreeId`.

### 2. Aktiv und inaktiv haben zwei verschiedene Bedeutungen

Die Steuerung gilt für **Layer**, nicht für beliebige einzelne Neuronen:

| Layer | Aktiver Aufruf | Inaktiver Aufruf |
| --- | --- | --- |
| Erste drei, durch Zellzustand geschaltet | Alle Neuronen rechnen | Alle Ausgänge werden auf 0 gesetzt |
| Weitere, dynamisch geschaltet | Alle Neuronen rechnen | Nur Neuron 0 rechnet; die übrigen Ausgänge bleiben stehen |
| Ausgabeschicht | Rechnet immer | Kein entsprechender Schlafzustand |

Bei dynamischen Layern ist Neuron 0 gleichzeitig normales Rechenelement und Taktgeber.
Sein Ausgang 0 wird nach dem Vorwärtslauf zum Aktivierungszähler addiert. Erreicht die
Summe 1, wird der Zähler auf 0 gesetzt und der Layer für den **nächsten** Aufruf aktiviert.
Andernfalls wird er für den nächsten Aufruf deaktiviert. Überschuss oberhalb 1 geht verloren.

Ein konstanter Taktgeberwert 0,5 lässt einen größeren Layer ungefähr jeden zweiten
Aufruf vollständig rechnen. Im Versuch sinkt die Zahl verarbeiteter Verbindungen von
2 auf 1, während die Nutzneuronen ihren alten Ausgang halten. Das ist sinnvolles
zeitliches Abtasten mit gehaltenem Ausgang, sofern diese Gedächtniswirkung beabsichtigt ist.

`max(0.001, counter + value)` erzwingt **keinen** Rechenschritt spätestens nach 1000
Aufrufen. Bei Ausgang 0 bleibt der Zähler dauerhaft bei 0,001, bei negativen Ausgängen
ebenfalls. Im Test schläft der Layer nach 1500 weiteren Aufrufen noch. Durch neue
Eingaben könnte sein weiterhin laufender Taktgeber wieder positiv werden. Ob solches
beliebig langes Schlafen erlaubt sein soll, ist eine Modellentscheidung.

Ein Layer mit nur einem Neuron spart durch dieses Verfahren keine Neuronenrechnung:
Sein einziges Neuron ist der ständig laufende Taktgeber. Ist dieser ein verschachteltes
Netz, kann der vermeintlich schlafende Layer weiterhin erhebliche Arbeit verursachen.

### 3. Die Gradienten passen nicht zu dieser Ausführung

`process()` verändert `isActiveLayer` **vor** dem anschließenden `backpropagate()`.
Backpropagation entscheidet anhand dieses Flags, welche Layer gerechnet haben sollen.
Im Test hat ein Layer gerade seine Nutzneuronen berechnet, wird danach für den nächsten
Schritt deaktiviert und erhält beim Training keinen Eingangsgewichtsgradienten.
Die numerische Ableitung der aktuellen Verlustfunktion ist aber eindeutig positiv.
Umgekehrt können gerade nicht berechnete Nutzneuronen anhand alter Werte trainiert werden,
wenn ihr Layer für den nächsten Aufruf aktiviert wurde.

Hinzu kommt ein unabhängiges Problem bei Rückkopplungen. Eine Verbindung von einem
später berechneten Ausgang zu einem früheren Layer verwendet im Vorwärtslauf den alten
Ausgang. Beim Gewichtsupdate liest der Code den inzwischen neu berechneten Ausgang.
Im Test ist der alte Ausgang 0: Die erste Verlustfunktion kann deshalb überhaupt nicht
von diesem Rückkopplungsgewicht abhängen. Numerische Ableitung: 0. Dennoch verändert
Backpropagation das Gewicht.

Das Verfahren ist damit weder korrektes Training durch die Zeit noch durchgehend
korrektes Einzelschritttraining eines rekurrenten Netzes. Die Berechnung erfolgt in
Layerreihenfolge mit gemischten aktuellen und alten Werten; welche Verbindung wie viel
Vergangenheit sieht, hängt von der Anordnung und der Aktivität ab.

**Empfehlung:** Ausführungsmaske und für jede Rechnung verwendete Quellwerte vom
Vorwärtslauf aufbewahren. Die Steuerungsflags für den nächsten Aufruf getrennt halten.
Für Rückkopplungen ein explizites Zeitmodell wählen. Evolution benötigt keine
Gradienten durch die Zeit; die bisherigen `calcTrain()`-Experimente benötigen dagegen
korrekte Gradienten oder eine klar eingeschränkte, getestete Trainingsdefinition.
Einzelne sequenzielle Gewichtsanpassungen ersetzen diese Definition nicht.

Die bestehenden Mehrport-Gradiententests prüfen kleine vorwärtsgerichtete Netze mit
abgeschalteter Layerdeaktivierung. Dass sie bestehen, widerspricht diesen Befunden nicht.

### 4. Reset und Zustandssteuerung sind unvollständig getrennt

`rnnClearPreviousState()` löscht versteckte und Ausgangswerte sowie Modulzustände.
Aktivierungszähler und Aktivitätsflags bleiben bestehen. Im Test liefert dieselbe
Ein-Schritt-Eingabe nach dem Reset ein anderes Ergebnis als am Anfang oder in einer
frischen Kopie. So können Bewertungen verschiedener Episoden voneinander abhängen.

Bei normalen `CellFactory`-Zellen stehen zunächst alle drei Zustandslayer auf aktiv,
obwohl der Zellzustand 0 ist. Nachkommen durchlaufen `calcActiveLayersByState()` und
haben bei Zustand 0 alle drei deaktiviert. Der Setter `setCellState()` selbst führt
keine Anpassung durch. Das macht Initialpopulation und Nachkommen inkonsistent.

Die drei Anfangslayer werden außerdem seriell vor dem Basisnetz eingefügt; vorhandene
Verbindungen werden erhalten. Im vollständig verbundenen kleinen Beispiel hat Bit 0
allein keine Wirkung über seinen Zustandslayer, weil der nächste Zustandslayer ausgeschaltet
ist. Dasselbe gilt für Bit 1 ohne Bit 2. Die Gates sind damit keine drei unabhängigen
Entwicklungsprogramme. Die direkt eingespeisten Zustandsbits können das Basisnetz trotzdem
beeinflussen; Evolution kann später zusätzliche Verbindungen herstellen.

Eigene Zustandswechsel während des Zelllebens sind aktuell nicht als Gehirnausgabe
vorgesehen. Die STATE-Ausgaben eines Aktors bestimmen den Zustand seines Nachkommen.
Das erlaubt geburtsabhängige Differenzierung, aber keine allgemeine spätere Umwandlung
derselben Zelle in eine andere Funktion. Ob das gewollt ist, sollte ausdrücklich feststehen.

**Empfehlung:** Getrennte Operationen für Genomkopie, vollständigen Episodenreset,
Simulation fortsetzen und Zustandswechsel definieren. Initialzellen und Nachkommen müssen
dieselbe Initialisierung durchlaufen. Für unabhängige Programme wären parallel erreichbare
Zustandsblöcke mit Anschlüssen an einen gemeinsamen Stamm verständlicher als die jetzige Kette.

### 5. Mutation ist für Entwicklung mit stabiler DNA sehr stark

Das normale Zellgehirn im reproduzierbaren Beispiel besitzt die Layergrößen
`[213, 53, 53, 53, 298, 133, 133]`: 936 Neuronen einschließlich Eingaben und 3335 Synapsen.
Bei Mutationsrate 0,1 und Stärke 0,2 änderten sich in einer Geburt **314 Gewichte**,
ohne Änderung der Layergrößen oder Synapsenzahl. Die Faktoren stehen hier auf ihrem
normalen Ausgangswert 1.

Die Rate gilt pro Synapse und pro ausgewähltem Neuron-Bias, nicht pro Geburt. Bei
dieser Netzgröße werden im Mittel rund 427 solcher Parameter-Mutationsereignisse
pro Geburt erwartet; manche betreffen wirkungslose Eingabebiases. Größere Netze
bekommen absolut noch mehr Änderungen. Die anfänglichen Gewichte liegen hier grob
in ±0,132, die mögliche Gewichtsänderung bei ±0,2. Das ist kein kleiner Schritt relativ
zur anfänglichen Gewichtsskala.

Damit können verwandte Zellen weiterhin ähnlich funktionieren. Eine Zellteilung erzeugt
aber normalerweise keine unveränderte DNA-Kopie. Das ist eine plausible Mitursache der
von Dir beobachteten schnellen Veränderung von Lebensweisen, kein experimenteller
Nachweis ihrer tatsächlichen Ursache in einer Konkurrenzwelt.

Für mehrzellige Entwicklung würde ich eine weitgehend genomtreue Entwicklungsphase
und seltenere evolutionäre Änderungen zwischen Organismusgenerationen getrennt testen.
Alternativ kann die Rate von der Genomgröße abhängen, wenn eine feste erwartete Anzahl
von Änderungen je Geburt gewünscht ist. Die vererbbaren Mutationsfaktoren funktionieren
laut bestehenden Tests, lösen aber die ungünstige Ausgangsskalierung nicht automatisch.

Zusätzliche Inkonsistenzen:

- `NeuronNetwork.mutateNeuron()` verändert Modulbiases um eine feste Spanne ±0,05;
  die übergebene Stärke wird dafür ignoriert. Selbst Stärke 0 verändert im Test einen
  Bias um 0,025. Außerdem wird die Stärke in diesem Pfad als strukturelle Wahrscheinlichkeit
  weitergereicht. Das ist anders als die Parametrisierung gewöhnlicher Neuronen.
- Die Mutation von Typdefinitionen erhält `structuralMutationRate` als innere
  Mutationsstärke. Die globale Mutationsstärke wird diesem Pfad nicht mitgegeben.
  Eine absichtlich kleinere Modulmutation wäre möglich, müsste aber separat parametrisiert sein.
- Der Typkatalog wächst durch neue Varianten; das Entfernen unbenutzter Typen ist
  auskommentiert. Genomkopien kopieren auch solche Vorlagen. Das kann Kosten ohne
  entsprechenden zusätzlichen Funktionsumfang ansammeln.
- Neue Strukturverbindungen starten mit ungefähr ±0,001 und niedriger Konnektivität.
  Sie sind damit wesentlich schwächer als initiale Verbindungen. Gleichzeitig entstehen
  durch mehrere unabhängige Ereignisse eher neue Synapsen als gezielt funktionsneutrale
  Erweiterungen. Ob diese Varianten häufig nutzbar werden, wurde nicht in langen Läufen gemessen.

Diese letzten Katalog-/Skalierungsbefunde stammen aus Codeanalyse. Es wurde keine
Wachstumsrate für entwickelte Modulbibliotheken oder erfolgreiche Strukturinnovationen gemessen.

### 6. Modulrechnung wird nicht vollständig bezahlt

`NeuronNetwork.activate()` summiert seine äußeren Eingangsverbindungen, meldet aber
nur die im inneren Netz verarbeiteten Synapsen zurück. Im Test werden tatsächlich fünf
Verbindungen benutzt: zwei zum Modul, zwei innen, eine zum äußeren Ausgang. Gemeldet
werden drei. Der gespeicherte Synapsenzähler des äußeren Netzes meldet ebenfalls drei
und enthält die zwei inneren Verbindungen nicht.

Die biologische Energieabrechnung verwendet diese Zähler. Modulare und flache Netze
werden deshalb nicht nach derselben Kostenregel bewertet. Bei geteilten Typdefinitionen
muss außerdem zwischen einmal gespeicherten Genen und mehrfach ausgeführter Arbeit
unterschieden werden.

**Empfehlung:** Drei getrennte Kennzahlen: gespeicherte Gene, Ausführungsinstanzen und
tatsächlich verarbeitete Verbindungen einschließlich Modulanschlüssen. Danach die
Energiekosten anhand des gewünschten biologischen Modells definieren. Die tatsächliche
CPU-Laufzeit ist eine vierte Kennzahl und darf nicht mit dem Synapsenzähler gleichgesetzt werden.

### 7. Die Trainingsstrategien selektieren andere Dinge als teilweise beschrieben

In der Anwendung findet kein `calcTrain()` statt. Zellgehirne ändern ihre Gewichte
durch Vererbung/Mutation; die Strategien wählen Zellen aus. Backpropagation wird in
getrennten Netztests und Experimenten benutzt.

`TrainingPopulationBuilder` behält alle Gewinner bis zur Zielgröße als Elite. Nur
die verbleibenden Plätze werden mit mutierten Trainingskindern aufgefüllt:

| Strategie | Bei voller Population | Folge |
| --- | --- | --- |
| HIGH_POSITION | 16 Gewinner, Ziel 16 | Keine Trainingskinder bei mindestens 16 Überlebenden |
| HIGH_POSITION2 | Bis zu 16 Gewinner, Ziel 16 | Keine Trainingskinder, sobald 16 Gewinner ausgewählt werden |
| HIGH_ENERGY | 20 % Gewinner, Ziel 20 | Bei 20 Überlebenden vier Eliten + 16 Kinder; ab 100 Überlebenden 20 Eliten + keine Kinder |

Natürliche Geburten während der Simulation können weiterhin Mutanten liefern.
Die Strategien selbst erzeugen in den genannten Situationen aber keine neuen Kandidaten,
entgegen ihren Klassenbeschreibungen. Abhilfe wäre eine bewusst kleinere Elitequote
mit reservierten Plätzen für Varianten.

In `HIGH_POSITION2` ergibt die Mittigkeit einen hohen positiven Score, sortiert wird
aufsteigend. Bei gleicher Höhe gewinnt deshalb der Rand. Die Höhe ist nach unten
positiv, so dass der kleinere Höhenscore in derselben Sortierung tatsächlich die höhere
Zelle bevorzugt. Die beiden Ziele sind unterschiedlich herum codiert. Der Test bietet
je Abschnitt eine mittige und zwei randnahe Zellen an; keine mittige Zelle wird behalten.

Die Evaluation hat weitere methodische Grenzen: Gewinner behalten Teile ihres Gedächtnisses
und ihrer Bewegung, neue Kinder starten mit anderen Laufzuständen und Wachstum. Positions-
strategien setzen die Zellen zurück und zerstören bestehende Verbände; `HIGH_POSITION2`
randomisiert zusätzlich Körperfarben, die wiederum Gehirneingaben sind. Der dokumentierte
globale Auswahlmodus von HIGH_POSITION2 entspricht außerdem nicht vollständig der
implementierten Begrenzung auf zwei Gewinner je Abschnitt. Intervalle zählen Aufrufe,
nicht Simulationssekunden; eine andere Physikfrequenz verändert die Evaluationsdauer.

Momentane Zellenergie kann sparsames Nichtteilen belohnen. Individuelle Höhe kann eine
hohe Zelle statt eines stabilen kooperativen Organismus belohnen. Beides kann für eng
definierte Einzelversuche sinnvoll sein, belegt aber keine erfolgreiche Evolution von
arbeitsteiliger Mehrzelligkeit.

**Empfehlung:** Faire, wiederholbare Episoden für Genotypvergleiche und eine eigene
Auswertung ganzer Entwicklungs- beziehungsweise Abstammungsverbände. Funktionsfähigkeit,
Zusammenhalt, Energieversorgung und lebensfähige Nachkommen über ein Zeitfenster erfassen.
Interessante Populationen archivieren und getrennt von Mutation/Konkurrenz untersuchen.
Die geeignete Fitness ist eine Forschungsentscheidung, die nicht durch einen einzelnen
universellen Zahlenwert ersetzt werden kann.

## Performance: was zuerst lohnt

Die bisherige Phasenmessung zeigt große Gehirnkosten bereits an kleinen Beispielnetzen.
Die konkreten folgenden Ansatzpunkte sind aus Codeanalyse abgeleitet; ihre Rangfolge
wurde in diesem Audit nicht neu gemessen.

1. **Defekte und unbenutzte Struktur beseitigen.** Tote Synapsen, nicht entfernte Neuronen
   und ungenutzte Typkataloge kosten Speicher, Kopierarbeit und teilweise Rechnung.
2. **Inaktive Zustandslayer effizient behandeln.** Aktuell werden deren Ausgänge bei
   jedem Denken erneut auf 0 geschrieben. Bei unverändertem Zustand kann eine saubere
   Zustandsmaske oder Nullsetzung beim Übergang Arbeit sparen. Rückkopplungen und
   Zustandswechsel müssen dabei korrekt abgefangen werden.
3. **Taktgeber leicht halten.** Ein verschachteltes Netz als ständig laufendes erstes
   Neuron kann den Nutzen schlafender Layer aufheben. Ein eigener günstiger Taktgeber
   wäre klarer, verändert aber bestehende Genomsemantik.
4. **Arrayaufbau bündeln.** `addSynapse()` kopiert bei jedem Hinzufügen das komplette
   globale Synapsenarray. Beim initialen Aufbau ergibt das quadratische Kopierarbeit.
   Lokale Adjazenzarrays wachsen ebenfalls einzeln. Listen mit Kapazität oder ein
   Aufbau mit Zählen und anschließendem Füllen können das reduzieren.
5. **Trainings-Metadaten vorberechnen.** `updateBiasAndWeights()` sucht für viele
   Verbindungen den Ziellayer durch Scannen aller versteckten Neuronen. Ein beim
   Strukturumbau gepflegter Index spart diese wiederholte Suche.
6. **Modultopologie versionieren.** Der Modulzustand prüft beim Aktivieren wiederholt
   die Topologie. Eine verlässliche Genomversion kann das ersetzen, sobald sämtliche
   Mutationen über kontrollierte APIs laufen.
7. **Temporäre Arrays prüfen.** Eingabearrays, Modulingaben und innere Ausgangsarrays
   werden häufig neu erzeugt; `NeuronNetwork.activate()` verwirft das Array, das die
   innere `process()`-Methode zurückgibt, und liest anschließend die Werte einzeln.
   Ein Ausführungspfad ohne unbenötigte Ausgabekopie ist ein konkreter Kandidat.

Eine niedrigere globale Denkfrequenz sollte erst nach klarer Zeit-/Gedächtnissemantik
folgen. Die jetzigen dynamischen Layer bieten bereits ein feineres Sparinstrument,
benötigen dafür aber korrekte Steuerung und verlässliche Kostenmessung.

Eingaben sind außerdem unterschiedlich skaliert: RGB/Energie/Zustandsbits liegen grob
in 0–1, Kraftstärken werden roh eingespeist, Alter als Sekunden. Das kann Sigmoid-Neuronen
sättigen und Taktgeber Richtung dauernd an/aus treiben. Eine einheitlichere Skalierung
ist ein sinnvoller Versuch, aber eine Modelländerung; ihr Nutzen wurde hier nicht gemessen.

## Empfohlene Reihenfolge

1. Strukturinvarianten, Neuronen-/Layerlöschung und Zielwahl neuer Verbindungen reparieren.
2. Ausführungsmaske vom nächsten Aktivitätszustand trennen; Reset und Zellzustandsinitialisierung vereinheitlichen.
3. Gradienten für tatsächlich ausgeführte Rechnungen korrigieren; rekurrentes Zeitmodell festlegen.
4. Trainings-Elitequote und HIGH_POSITION2-Score korrigieren; Episoden fair zurücksetzen.
5. Modulmutation, vollständige Kosten und Bereinigung ungenutzter Typen vereinheitlichen.
6. Erst dann Entwicklungs-/Evolutionsmutation trennen und die genannten Performancekandidaten messen.

## Prüffälle und Grenzen

18 neue Prüffälle sind ausdrücklich **Beobachtungstests des aktuellen Verhaltens**,
darunter nachgewiesene Fehler. Ein erfolgreicher Lauf bedeutet, dass die beschriebenen
Probleme reproduziert sind. Er bedeutet nicht, dass das Verhalten richtig ist.
Die Fälle laufen nur mit `-Dnetwork.audit=true`, damit sie die Fehler nicht als normale
Regressionsanforderung festschreiben. Bei einer Reparatur müssen die entsprechenden
Erwartungen in echte Korrektheitsprüfungen umgebaut werden.

Der gemeinsame gezielte Lauf bestand mit 72 Fällen: 18 Beobachtungen und 54 vorhandenen
Netz-, Vererbungs-, Mutations- und Trainingsregressionen. Die 282 übrigen gezielten
Projektprüfungen wurden für diese ausschließlich prüfende Änderung nicht vollständig wiederholt.

```powershell
mvn test '-Dtest=NetworkLogicAuditTest,TrainingLogicAuditTest' '-Dnetwork.audit=true'
mvn test '-Dtest=NetworkLogicAuditTest,TrainingLogicAuditTest,MultiPortNeuronTest,BackpropagateTest,NestedInheritanceTest,MutationFactorsTest,TrainingGenerationTest' '-Dnetwork.audit=true' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```

- [Netzprüffälle](src/test/java/de/lifecircles/model/neural/NetworkLogicAuditTest.java).
- [Trainingsprüffälle](src/test/java/de/lifecircles/service/trainStrategy/TrainingLogicAuditTest.java).
- [Netz und Mutation](src/main/java/de/lifecircles/model/neural/NeuralNet.java),
  [Module](src/main/java/de/lifecircles/model/neural/NeuronNetwork.java),
  [Zellgehirn](src/main/java/de/lifecircles/model/neural/CellBrain.java),
  [Generationserzeugung](src/main/java/de/lifecircles/service/trainStrategy/TrainingPopulationBuilder.java).

Die Prüfung ist kein Vollständigkeitsbeweis für seltene verschachtelte Mutationspfade.
Die Wirkung vorgeschlagener Fitness-, Skalierungs- und Entwicklungsmodelle muss durch
eigene Versuche untersucht werden. Die Produktionslogik wurde in diesem Schritt bewusst
als Grundlage für Deine Bewertung beibehalten.
