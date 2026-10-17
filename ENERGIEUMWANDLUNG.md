# Begrenzter evolvierbarer Stoffwechsel

Implementiert am 17.10.2026. Ergänzt die acht Vorräte aus `MEHRERE_ENERGIEN.md`.

## Eine Route je Zelle

Eine lebende Zelle kann eine gerichtete Umwandlung besitzen:

`Quellform → Zielform, Kapazität in [0,1]`

Quelle und Ziel sind verschiedene RGB-Energiecodes; alle 56 gerichteten Routen sind
möglich. Das unveränderliche `EnergyConversion`-Objekt ist ein vererbtes Zellmerkmal,
ähnlich den vorhandenen Zellmerkmalen außerhalb des Netzes. Es ist unabhängig von
den kontinuierlichen Zellfarben, dem aktuellen Zellzustand und dem Netzgedächtnis.

- Maximaler Quellverbrauch: `0,1 × Kapazität` Energie pro Simulationssekunde.
- Zielform erhält 80 % der tatsächlich verbrauchten Quelle; 20 % dissipieren.
- Verbrauch ist zusätzlich durch vorhandene Quelle und freien Zielraum begrenzt.
  Volles Ziel: keine Reaktion, kein Verlust von Quellenergie.
- Genau eine Reaktion je Zelle und Schritt. Kein beliebiger sofortiger Wechsel
  zwischen allen Formen; wiederholte oder zirkuläre Reaktionen verlieren Energie.
- Umgewandelt werden gespeicherte Vorräte, auch wenn sie bereits länger in der Zelle
  liegen. Die Einschränkung ist das einzelne Enzym samt Rate und Ausbeute. Eine reine
  Umwandlung beim Aufnahmeereignis wurde für diesen Schritt nicht gewählt.
- Der Prozess ist zunächst automatisch. Das Gehirn steuert weiterhin Aufnahme und
  Abgabe, aber noch nicht Route oder momentane Enzymaktivität. Keine zusätzlichen
  neuronalen Ausgänge, keine Umdeutung alter Ausgänge.
- Es gibt bisher keinen separaten Preis für Enzymsynthese oder das Vorhalten einer
  Kapazität; der reale Preis ist Quellverbrauch mit 20 % Umwandlungsverlust. Das ist
  eine erste experimentelle Regel, kein bereits austariertes Stoffwechselmodell.

Unterhalt, Gehirn und bisherige Lebens-/Teilungsbedingungen verwenden weiterhin
Primärenergie 000. Andere Formen werden nur über passende Umwandlung nutzbar.
Eine Zelle mit Engpass kann weiterhin mit Restvorräten anderer Formen sterben.
Eine rechtzeitige Umwandlung nach 000 kann einen ansonsten tödlichen Engpass abwenden.
Alterstod wird durch Umwandlung nicht verhindert.

## Vererbung und Mutation

Neue Gründer und alte Dateien beginnen ohne aktive Umwandlung (Kapazität null).
Die zunächst inaktive Route 001 → 000 ist nur ein Ausgangspunkt der Mutationen,
keine automatische Fähigkeit oder Ressourcensubvention.

Quelle, Ziel und Kapazität können jeweils mit der effektiven Mutationsrate variieren.
Kapazität ändert sich um einen gleichverteilten Wert in ± effektiver Mutationsstärke,
begrenzt auf [0,1]. Quelle/Ziel bleiben stets verschieden. Elternfaktoren und
Teilungsmultiplikator gelten wie bei der Gehirnmutation. Kapazität null kann positiv
werden und später wieder auf null sinken.

Biologische Nachkommen und Trainingsnachkommen erben das Merkmal. Unmutierte Eliten,
klonale Teilung und die Mitglieder eines HIGH_POSITION3-Körpers erhalten dieselben
Enzymgene. Ein geteiltes unveränderliches Objekt hat keinen gemeinsamen Laufzeitzustand.
Die acht Vorräte werden bei biologischer Teilung weiterhin einzeln aufgeteilt;
Trainingsgründer erhalten weiterhin die ausdrücklich definierten Episodenreserven.

Das neue optionale Zellfeld wird mit `.cells` gespeichert. Alte Dateien bleiben
ladbar und erhalten keine aktive Reaktion oder zusätzliche Energie. Eingangs-/Ausgangs-
dimensionen bleiben 316/133, ihre bisherigen Migrationen bleiben unverändert.

## Reihenfolge und Beobachtung

Im Weltschritt: Sonne → normaler Austausch → Umwandlung aller Zellen → gemeinsame
Momentaufnahme der eigenen und benachbarten Sensorsignale → Denken → Unterhalt/Tod.
Alle Gehirne sehen somit dieselben abgeschlossenen Reaktionen. Neu umgewandelte
Energie kann erst im nächsten Schritt an Nachbarn weitergereicht werden. Bereits
erhaltene Nahrung kann noch vor dem nächsten Unterhalt umgewandelt werden.

Überreste behalten ihre vorhandenen Vorräte, führen aber keinerlei Reaktionen aus.
Ihre gespeicherten Enzymmerkmale sind inaktiv und machen sie nicht reproduktionsfähig.
HIGH_POSITION3 erhält die Gene, unterdrückt Reaktionen aber ausdrücklich, um die
Mechanikaufgabe nicht mit Stoffwechsel zu vermischen. Andere Welten verwenden die
Regel regulär. Der Profiler erfasst den Abschnitt separat als `CONVERSION`.

In den Zelldetails steht die Route mit Kapazität und Ausbeute; bei Überresten
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

Noch keine funktionenspezifischen Energiepreise, variable Speicherkapazitäten,
neuronale Enzymregulation oder durch Rechenaufgaben freigeschalteten Reaktionen.
Ein gemeinsames Organismusgenom mit je nach Zellrolle verschieden exprimierten
Enzymen ist durch dieses einzelne Zellmerkmal noch nicht gelöst. Langzeit-Evolution
und ökologische Balance müssen gesondert untersucht werden.
