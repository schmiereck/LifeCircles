# Begrenzter evolvierbarer Stoffwechsel

Implementiert am 17.10.2026. Ergänzt die acht Vorräte aus `MEHRERE_ENERGIEN.md`.

## Eine aktive Route je Spezialisierung

Der gemeinsame Genotyp enthält ein unveränderliches `EnergyConversionProgram` mit
acht Regeln. „Show Specialization“ zeigt bereits den Zellzustand 0..7 an. Genau
diese drei Zustandsbits wählen nun auch die aktive Stoffwechselregel:

`Quellform → Zielform, Kapazität in [0,1]`

Quelle und Ziel sind verschiedene RGB-Energiecodes; alle 56 gerichteten Routen sind
möglich. Jede Spezialisierung kann eine andere Route/Kapazität oder keine aktive
Umwandlung besitzen. Die Tabelle ist ein vererbtes Merkmal außerhalb des neuronalen
Netzes, unabhängig von kontinuierlicher Zellfarbe und Netzgedächtnis. Pro Zelle wird
weiterhin nur **eine** Regel ausgeführt; acht Genorte vervielfachen nicht den Durchsatz.

Beispiel gleicher DNA und Zellfarbe: Spezialisierung 0 macht 111 → 010, Spezialisierung
1 macht 010 → 001, Spezialisierung 2 macht 001 → 000. Dazwischen liegen die normalen
konservativen Austauschvorgänge. Die Rollen haben keine fest vorgegebenen Routen;
die Zuordnung selbst kann evolvieren.

Das Gehirn bestimmt bereits über die drei STATE-Ausgänge jedes Teilungsaktors die
Spezialisierung des Kindes. Die vollständige Enzymtabelle wird dabei vererbt; das
Kind exprimiert den zu seiner eigenen Spezialisierung passenden Eintrag. Es braucht
keine Mutation der DNA, um einen anderen Zelltyp innerhalb des Verbandes auszubilden.
Eine Zustandsänderung wählt lediglich eine andere vorhandene Regel und verschiebt
keine Energievorräte. Bei nichtkanonischen alten Zellzuständen gelten dieselben
unteren drei Bits wie bei den vorhandenen Zustands-Sensoren.

- Maximaler Quellverbrauch: `0,1 × Kapazität` Energie pro Simulationssekunde.
- Zielform erhält 80 % der tatsächlich verbrauchten Quelle; 20 % dissipieren.
- Verbrauch ist zusätzlich durch vorhandene Quelle und freien Zielraum begrenzt.
  Volles Ziel: keine Reaktion, kein Verlust von Quellenergie.
- Genau eine Reaktion je Zelle und Schritt. Kein beliebiger sofortiger Wechsel
  zwischen allen Formen; wiederholte oder zirkuläre Reaktionen verlieren Energie.
- Umgewandelt werden gespeicherte Vorräte, auch wenn sie bereits länger in der Zelle
  liegen. Die Einschränkung ist das einzelne Enzym samt Rate und Ausbeute. Eine reine
  Umwandlung beim Aufnahmeereignis wurde für diesen Schritt nicht gewählt.
- Der Prozess ist automatisch innerhalb der gewählten Spezialisierung. Das Gehirn
  steuert Aufnahme, Abgabe und die Spezialisierung seiner Kinder; es gibt noch keine
  zusätzliche kontinuierliche Enzymaktivitätssteuerung. Keine neuen neuronalen
  Ausgänge und keine Umdeutung alter Ausgänge.
- Es gibt bisher keinen separaten Preis für Enzymsynthese oder das Vorhalten einer
  Kapazität; der reale Preis ist Quellverbrauch mit 20 % Umwandlungsverlust. Das ist
  eine erste experimentelle Regel, kein bereits austariertes Stoffwechselmodell.

Unterhalt und bisherige Lebens-/Teilungsbedingungen verwenden Primärenergie 000.
Gehirnspeicherung und Rechenarbeit verwenden jetzt 001. Andere Formen werden nur
über passende Umwandlung nutzbar; Regeln zu Finanzierung und Pause in `FUNKTIONALE_ENERGIE.md`.
Eine Zelle mit Engpass kann weiterhin mit Restvorräten anderer Formen sterben.
Eine rechtzeitige Umwandlung nach 000 kann einen ansonsten tödlichen Engpass abwenden.
Alterstod wird durch Umwandlung nicht verhindert.

## Vererbung und Mutation

Neue Gründer und Dateien aus der Zeit vor den Umwandlungen beginnen in allen acht
Spezialisierungen ohne aktive Umwandlung (Kapazität null). Die zunächst inaktive
Route 001 → 000 ist nur ein Ausgangspunkt der Mutationen, keine automatische
Fähigkeit oder Ressourcensubvention. Dateien aus dem Einzel-Enzym-Stand behalten
ihre bisherige Regel unverändert in allen acht Spezialisierungen. So entsteht durch
Migration weder ein Verhaltensbruch noch zusätzlicher Durchsatz.

Quelle, Ziel und Kapazität jedes der acht Einträge können unabhängig mit der
effektiven Mutationsrate variieren, auch wenn der Eintrag im Elternteil inaktiv war.
Kapazität ändert sich um einen gleichverteilten Wert in ± effektiver Mutationsstärke,
begrenzt auf [0,1]. Quelle/Ziel bleiben stets verschieden. Elternfaktoren und
Teilungsmultiplikator gelten wie bei der Gehirnmutation. Kapazität null kann positiv
werden und später wieder auf null sinken.

Biologische Nachkommen und Trainingsnachkommen erben das Merkmal. Unmutierte Eliten,
klonale Teilung und die Mitglieder eines HIGH_POSITION3-Körpers erhalten dieselben
Enzymgene. Ein geteiltes unveränderliches Objekt hat keinen gemeinsamen Laufzeitzustand.
Die gesamte Tabelle und alle noch ungenutzten Einträge werden kopiert, nicht nur die
im Elternteil aktive Route. Die acht Vorräte werden bei biologischer Teilung weiterhin einzeln aufgeteilt;
Trainingsgründer erhalten weiterhin die ausdrücklich definierten Episodenreserven.

Die optionale Tabelle wird mit `.cells` gespeichert. Alte Dateien bleiben ladbar;
vorhandene Einzelregeln werden erhalten, fehlende bleiben inaktiv. Keine zusätzliche Energie. Eingangs-/Ausgangs-
dimensionen bleiben 316/133, ihre bisherigen Migrationen bleiben unverändert.

## Reihenfolge und Beobachtung

Im Weltschritt: Sonne → normaler Austausch → Umwandlung aller Zellen → Gehirnspeichergebühr/
Finanzierungsprüfung → gemeinsame Momentaufnahme der eigenen und benachbarten
Sensorsignale → bezahltes Denken oder Pause → Unterhalt/Tod.
Alle Gehirne sehen somit dieselben abgeschlossenen Reaktionen. Neu umgewandelte
Energie kann erst im nächsten Schritt an Nachbarn weitergereicht werden. Bereits
erhaltene Nahrung kann noch vor dem nächsten Unterhalt umgewandelt werden.

Überreste behalten ihre vorhandenen Vorräte, führen aber keinerlei Reaktionen aus.
Ihre gespeicherten Enzymmerkmale sind inaktiv und machen sie nicht reproduktionsfähig.
HIGH_POSITION3 erhält die Gene, unterdrückt Reaktionen aber ausdrücklich, um die
Mechanikaufgabe nicht mit Stoffwechsel zu vermischen. Andere Welten verwenden die
Regel regulär. Der Profiler erfasst den Abschnitt separat als `CONVERSION`.

In den Zelldetails steht die aktive Spezialisierung samt Route, Kapazität und Ausbeute; bei Überresten
„inaktiv“. Eigene und benachbarte Energiestände werden weiter über die vorhandenen
acht Sensorkanäle erfasst. Der alte Energiebalken zeigt weiterhin nur Primärenergie.

## Nachweise und Grenzen

- Alle 56 Routen mit expliziter Bilanz einschließlich Verlust geprüft.
- Quellenknappheit, volle Ziele, Zeitunterteilung, Rückreaktionszyklen, ungültige
  Parameter und Überreste geprüft.
- Eigene und Nachbarsensoren sehen die umgewandelten Vorräte vor dem Unterhalt.
- Echte alte Datei ohne neue Genfelder sowie neue Save/Load-Rundreise geprüft.
- Biologische und Trainingsvererbung, Elternfaktoren, klonale Abschaltung und Erwerb
  einer Kapazität aus einem inaktiven Gen geprüft.
- Alle acht Spezialisierungen, unveränderliche Tabellen, inaktive Genorte, ganze
  Tabellenvererbung bei anderer Kindspezialisierung und Save/Load geprüft.
- Echte Datei mit Einzel-Enzym-Gen aus `adb7158` nach acht Regeln migriert; Fixture
  und Herkunft unter `src/test/resources/energy`.
- Drei gleichfarbige Zellen mit gleichem festen Controller und derselben Tabelle
  führen 111 → 010 → 001 → 000 über normalen Delivery-Austausch aus. Nach einer
  Sekunde erreicht die Endzelle 0,146493 Primärenergie statt 0,097000 bei fehlender
  mittlerer Spezialisierung (Startwert 0,1, kostenlose feste Gehirne). Gesamtbilanz
  inklusive aller Umwandlungsverluste und Grundumsatz geprüft; identisches Ergebnis
  bei umgekehrter Zellliste und über dem periodischen Rand.

Außer 000-Erhalt/001-Gehirn noch keine weiteren funktionenspezifischen Energiepreise, variablen Speicherkapazitäten,
kontinuierliche neuronale Enzymregulation oder durch Rechenaufgaben freigeschalteten
Reaktionen. Das gemeinsame Programm ermöglicht je nach Zellrolle unterschiedliche
Enzyme; es erzwingt keine Arbeitsteilung. Direkte Umwandlungen nach 000 bleiben
möglich und können in einfachen Umgebungen günstiger sein als eine Zellkette.
Die Aasregen-Trainings bewerten weiterhin Einzelzellen, keine Mehrzeller. Die kleine
Testkette belegt physikalische/mathematische Lösbarkeit, noch keine spontane Evolution
oder stabile mehrzellige Lebensweise. Langzeit-Evolution und ökologischer Selektionsdruck
müssen gesondert untersucht werden.
