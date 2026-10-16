# Netze vereinfachen, ohne ihre Ausgaben zu verändern

Umgesetzter Folgeschritt: [DNA-erhaltender Ausführungsplan und GUI-Kostenregler](NETZAUSFUEHRUNG.md).
Die folgenden Zahlen beschreiben weiterhin die frühere strukturelle Forschungs-Kopie.

Untersuchung vom 16. Oktober 2026, Java 25.0.3, Windows. Grundlage sind dieselben
18 Genome aus den langen NONE-Kostenversuchen wie beim Funktionsvergleich.

## Ergebnis

Ein erheblicher Teil dieser Netze hat in der vorhandenen Verdrahtung keinen Weg
zu einem Ausgang oder zu einer Layer-Taktsteuerung. Dieser Teil lässt sich entfernen,
ohne die geprüften neuronalen Ausgaben oder Layer-Takte zu verändern:

| Messgröße, Summen über 18 Genome | Original | Vereinfacht | Verringerung |
|---|---:|---:|---:|
| Alle Neuronen im Wurzelnetz, einschließlich Ein-/Ausgängen | 11.644 | 8.721 | 25,1 % |
| Synapsen im Wurzelnetz | 13.585 | 7.914 | 41,7 % |
| Gespeicherte Synapsen einschließlich Moduldefinitionen | 13.882 | 8.205 | 40,9 % |
| Ausgeführte Synapsen in den Vergleichsfolgen | 188.418.940 | 137.955.280 | 26,8 % |
| Serialisierte frische Netzkopien, Bytes | 1.127.094 | 775.793 | 31,2 % |

Über 144 Folgen mit insgesamt 316.800 Netzschritten stimmen **42.134.400 rohe
Ausgabewerte bitgenau überein**. Die aktiven Layer und ihre Aktivierungszähler
stimmen ebenfalls nach jedem Schritt überein. Das ist stärker als lediglich
ähnliche Fitness oder gleiche gerundete Aktorbefehle in einer Testwelt.

Die isolierte Vorwärtsrechnung erreicht einen medianen Beschleunigungsfaktor
von **1,62**, mit Medianen je Genom zwischen 1,33 und 1,97. Das entspricht beim
mittleren Faktor ungefähr 38 % weniger Zeit für diesen Rechenschritt. Es ist
keine Messung der gesamten Weltgeschwindigkeit. Mutation, Physik, Sensoren,
Sonne und Darstellung sind darin nicht enthalten.

[Grafik und Streuung](docs/network-simplification/2026-10-16/simplification.png),
[Rohdaten](docs/network-simplification/2026-10-16/counts.csv),
[Zeitmessungen](docs/network-simplification/2026-10-16/timing.csv),
[Zusammenfassung](docs/network-simplification/2026-10-16/summary.json).

## Welche Vereinfachung ist sicher?

Die implementierte `NeuralNetwork.simplifiedCopy()` erstellt eine unabhängige
Genotypkopie mit leerem Gedächtnis. Sie beginnt bei allen Ausgangsneuronen und
allen ersten Neuronen dynamischer Layer, also deren Taktgebern. Dann verfolgt sie
alle eingehenden Synapsen rückwärts bis zum Abschluss. Die dabei nicht erreichten
versteckten Neuronen und ihre Verbindungen werden entfernt.

Diese Regel berücksichtigt auch Selbstverbindungen, rückwärts gerichtete Kanten
und Schleifen. Ein Neuron bleibt erhalten, wenn es erst in einem späteren Schritt
einen relevanten Wert beeinflussen kann. Ein isolierter Zyklus ohne Verbindung
zu Ausgang oder Taktgeber darf dagegen verschwinden.

Erhalten bleiben:

- Alle Ein-/Ausgangspositionen, Layerpositionen und festen Zellzustands-Slots.
- Sämtliche dynamischen Taktgeber samt ihren indirekten Eingängen, auch wenn
  die Layersteuerung momentan deaktiviert ist.
- Relevante Neuronen in momentan inaktiven Layern. Inaktivität ist kein Löschgrund.
- Reihenfolge der verbleibenden Neuronen und ihrer Summanden.
- Alle Ports eines verbleibenden Moduls. Seine Definition wird ebenfalls vereinfacht;
  gemeinsam benutzte Definitionen werden nur einmal bearbeitet.
- Ungenutzte Modultyp-Kandidaten im Vorrat. Sie werden nicht eigens entfernt oder
  vereinfacht; eine mit einem benutzten Modul geteilte Definition kann sich aber ändern.
- Nullgewichte und kleine Gewichte auf relevanten Pfaden. Selbst Nullgewichte sind
  mit nichtendlichen Zwischenwerten nicht grundsätzlich wirkungslos.

Die graphische Begründung lautet: Jeder entfernte Wert liegt außerhalb der rückwärts
erreichbaren Abhängigkeiten aller Ausgaben und Taktgeber. Deshalb kann er auch über
zukünftige Rückkopplungsschritte keinen dieser Werte verändern. Das setzt die bestehende
Ausführungslogik voraus, bei der Neuronen über ihre Synapsen wirken und keine sonstigen
globalen Seiteneffekte haben. Die Vergleichstests prüfen die Implementierung dieser
Regel; sie sind kein erschöpfender Test aller denkbaren Eingaben und Fremdimplementierungen.

## Prüfungen und Messverfahren

83 gezielte Kontrollen bestanden, einschließlich acht neuer Vereinfachungstests.
Sie prüfen isolierte und relevante Schleifen, indirekte Taktgeber-Eingänge,
Aufwachen nach mindestens 1000 Aufrufen, Zustandswechsel, leere feste Layer,
deaktivierte Layersteuerung, Nullgewichte/NaN, gemeinsame Mehrport-Module,
zwei Modulebenen, Serialisierung, Kopieren, weitere Mutation und unbenutzte Typkandidaten.
Zusätzlich wurden 40 zufällige dünne Graphen mit Rückwärts- und Selbstkanten geprüft.
Bestehende Prüfungen für Vererbung, Training, Gradienten und Energiekosten bestanden
ebenfalls; das belegt keine allgemeine Gleichheit künftig trainierter oder mutierter Netze.

Die Genomstudie startet jedes Modell mit jedem der acht Zustände und frischem Gedächtnis.
Jede Folge dauert 2200 Aufrufe; bei Aufruf 1100 wird ein anderer Zustand eingestellt.
Die reproduzierbaren Eingaben enthalten Werte in [-2,2] sowie größere Werte in [-100,100].
Alle 133 Ausgänge müssen endlich und bitgenau gleich sein. Es handelt sich um direkte
Netzproben, nicht um Fitnessversuche in einer vollständigen Welt.

Die Zeitmessung verwendet den Produktionspfad ohne Trainingsspur, einen Thread,
Zustand 7, 500 Aufwärmaufrufe und 2000 gemessene Aufrufe. Sieben Wiederholungen wechseln
die Reihenfolge Original/Kompakt. Aus sieben gepaarten Zeitverhältnissen wird pro Genom
der Median gebildet, daraus der Median über 18 Genome. Einzelwerte streuen; die 18
Genome sind zudem verwandte frühe/späte Proben aus neun Evolutionsgeschichten.
Die Zeitmessung zeigt hier einen konsistenten Ansatz, keine garantierte Beschleunigung
beliebiger Netze. Serialisierte Größe misst weder die gesamte RAM-Belegung noch
temporäre Allokationen; letztere stehen separat in den Zeitmessdaten.

Die Modellprüfsummen stimmen mit dem abgeschlossenen Funktionsvergleich überein.
Die Implementierung der Vereinfachung stammt aus Commit `5100edb`; Quellprüfsummen
der untersuchten Implementierung und des Studiencodes stehen im Archiv.

## Was bedeutet das für die Evolution?

**Gleiche Netzausgaben sind nicht dasselbe wie unverändertes Evolutionsverhalten.**
Physisches Entfernen von Neuronen und Synapsen verändert drei Dinge:

1. Speicher- und Rechengebühren fallen anders aus. Damit verändern sich Energie,
   Teilungszeitpunkte und später auch die Eingaben an das Netz.
2. Mutation findet andere Ziele und verbraucht andere Zufallsfolgen.
3. Der entfernte Teil kann bisher neutrales genetisches Material sein, das eine
   spätere Strukturmutation wieder an einen Ausgang anschließen könnte.

Die gefundenen 41 % sind daher keine nachgewiesenen nutzlosen Evolutionsreserven.
Sie sind für die **heutige Verdrahtung** ohne Einfluss auf Ausgänge und Taktsteuerung.
Das passt zu neutralem Mitführen von Struktur, beweist aber weder dessen Ursache
noch einen langfristigen Nachteil. Umgekehrt muss eine erhaltene Verbindung nicht
biologisch nützlich sein: Die Analyse erhält vorsichtshalber jeden möglichen
Einflusspfad, auch wenn ein Aktorbefehl später begrenzt oder ignoriert wird.

## Empfehlung für den nächsten Umsetzungsschritt

Ich würde **DNA und Ausführung trennen**: Die vollständige DNA einschließlich
neutraler Reserven bleibt erhalten; ein vorbereiteter Ausführungsplan überspringt
nachweislich irrelevante Berechnungen. Neu berechnet wird der Plan nach einer
Strukturänderung. Damit ließe sich Rechenzeit sparen, ohne das zukünftige genetische
Material zu beschneiden.

Für unveränderte Weltverläufe müssen zusätzlich die bisherigen Energiegebühren
erhalten bleiben. Bei übersprungenen skalaren Neuronen kann ihre bisherige
Synapsenarbeit anhand des identischen Layer-Takts mitgezählt werden. Bei einem
ganzen übersprungenen Modul kann dessen Arbeitsmenge selbst eingabe- und
gedächtnisabhängig sein; dort ist eine pauschale Ersatzzahl nicht sicher. Eine erste
Umsetzung sollte solche Module weiter ausführen und nur sicher zählbare Arbeit sparen.
Eine spätere Umstellung auf Gebühren der tatsächlich optimierten Ausführung wäre
ein eigenes Evolutionsexperiment.

Vor einem automatisch gecachten Plan müssen die öffentlich veränderbaren
Layer-/Neuronarrays und Synapsen kontrolliert werden: Sonst kann eine Änderung eine
neue Abhängigkeit schaffen, während der alte Plan sie weiter überspringt.
Ein einmal vorbereiteter Plan ist nur für eine unveränderte Topologie gültig.
Diese Absicherung ist Voraussetzung für eine Aktivierung in der Simulation.

Die jetzige Vereinfachung ist deshalb bewusst eine ausdrücklich aufgerufene
Forschungsfunktion auf einer Kopie. **Die GUI vereinfacht keine Zellen automatisch;
Standardkosten und DNA bestehender Populationen bleiben unverändert.**

## Wiederholen

```powershell
mvn test '-Dtest=NetworkSimplificationTest'
mvn test '-Dtest=NetworkSimplificationStudyTest' '-Dsimplification.study.folder=experiments/function-comparison-stable-2026-10-16' '-Dsimplification.study.output=experiments/network-simplification-new' '-DargLine=-Xmx2g'
python scripts/analyze_network_simplification.py experiments/network-simplification-new --models experiments/function-comparison-stable-2026-10-16 --output docs/network-simplification/new
```

Einen neuen Ausgabeordner verwenden. Die `.cells`-Modelle liegen lokal im
Experimentordner; das Ergebnisarchiv enthält Messwerte und deren Modellprüfsummen.
