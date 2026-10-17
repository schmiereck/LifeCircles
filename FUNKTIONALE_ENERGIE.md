# Energie 000 für Erhalt, 001 für das Gehirn

Umgesetzt am 17.10.2026. Erste feste Verbrauchszuordnung; Umwandlung und
Spezialisierungen bleiben evolvierbar. Diese Zuordnung selbst mutiert nicht.

| Form | Verbrauch | Bei Mangel |
|---|---|---|
| 000 / PRIMARY | Grundumsatz, unverändert 0,003 pro Simulationssekunde | Bisheriger Tod bei 000 <= 0; andere Reserven bleiben als Überrest |
| 001 / SECONDARY | Speicherung der Gehirn-DNA und tatsächlich berechnete Synapsen | Denken pausiert, Zelle bleibt bei genügend 000 lebendig |
| 010..111 | Zunächst Vorräte und Zwischenprodukte | Keine zusätzlichen Todesbedingungen |

Die GUI benennt die Formen als `000 · Maintenance` und `001 · Brain`. Der bestehende
Gehirnkostenregler skaliert weiterhin beide bisherigen Preise. Die Zahlenwerte der
Preise und die logische Synapsenabrechnung des DNA-erhaltenden Ausführungsplans
bleiben erhalten; die Abbuchung erfolgt jetzt aus 001. Bei Regler 0 sind Gehirne
ausdrücklich kostenlos und dürfen auch mit leerem 001-Speicher denken.

Größenänderung und Aktorkräfte bekommen in diesem Schritt keine eigenen Preise.
Sie später zu tarifieren benötigt separate mechanische Prüfungen und eine Entscheidung,
ob Aufwand, Arbeit oder Haltekraft bezahlt werden sollen.

## Finanzierung vor Ausführung

Im Weltschritt laufen zuerst Sonne, normaler Austausch und automatische Umwandlung.
Danach werden für alle lebenden Zellen die Speichergebühren aus 001 bezahlt, erst
anschließend die gemeinsamen eigenen und benachbarten Eingabesensoren eingefroren.

Vor jeder Entscheidung prüft `BrainEnergyService`, ob die Zelle nach der Speichergebühr
die maximale mögliche Rechengebühr eines vollständigen Zyklus bezahlen könnte.
Die Obergrenze umfasst alle Schichten und jeden tatsächlichen Moduleinsatz, auch
mehrere Einsätze derselben gespeicherten Moduldefinition. Ein deduplizierter
Speicher-Synapsenzähler allein wäre keine sichere Arbeitsobergrenze.

Diese Reservierung ist eine Berechtigungsprüfung, keine zusätzliche Abbuchung.
Nach erfolgreicher Berechnung werden nur die wirklich ausgeführten logischen
Verbindungen bezahlt. Restliche Reserve bleibt im 001-Speicher. Jede Berechtigung
gilt für genau eine Entscheidung; kein zweites Ausführen mit derselben Freigabe.
Die Preise sind für alle Zellen eines Schritts konsistent eingefroren.

Die Vorabprüfung ist konservativ: Nahe an Energiemangel kann ein Zyklus pausieren,
obwohl ein günstiger tatsächlicher Aktivierungszustand gerade noch bezahlbar wäre.
Es wird weder probeweise kostenlos gedacht noch das Netzgedächtnis zurückgerollt.
Neuronenwerte, Modulgedächtnis, Schichttakte und Eingabespeicher bleiben während
der Pause unverändert. Topologieprüfungen und Verwaltungsarbeit sind wie bisher
keine einzeln abgerechneten neuronalen Operationen.

Die Speichergebühr gilt auch während der Pause. Reicht 001 nicht einmal dafür,
wird nur der noch vorhandene Rest abgebucht; es gibt keine negativen Vorräte oder
Schulden. Alte Arbeitszähler werden während einer Pause nicht erneut verrechnet.

## Pause und Erholung

- Keine neue neuronale Ausführung und keine neuen Inputs in das pausierende Gehirn.
- Aktorkräfte, Delivery und Teilungswunsch werden deaktiviert. Bereits aus der letzten
  bezahlten Entscheidung im aktuellen Schritt berechnete Impulse bleiben physikalisch gültig.
- Laufendes Wachstum und Größenanpassung pausieren; der bisherige Zielwert und
  Wachstumsfortschritt werden nicht zurückgesetzt. Biologisches Alter läuft weiter.
- Zuletzt gewählte Absorptionsports bleiben verfügbar. Es gibt keinen neu erfundenen
  kostenlosen Allport-Saugmechanismus. Bei zuvor ausgeschalteter Absorption benötigt
  die Zelle etwa Sonnenenergie 001 oder eigene automatische Umwandlung zur Erholung.
- Sonne, Austausch, automatische Enzyme und passive Physik laufen weiter. Sobald
  genügend 001 vorhanden ist, folgt der nächste bezahlte Zyklus aus dem alten Gedächtnis.
- Zelldetails zeigen `Gehirn pausiert (001)`. Eine Pause allein macht die Zelle nicht
  zum Überrest. Tod durch Alter oder fehlende Energie 000 bleibt davon unabhängig.
- Überreste zahlen nichts, denken und reagieren nicht, behalten ihre Restreserven.

## Gründer, Geburten und bestehende Dateien

Neue Standard-Gründer aus `CellFactory` und Trainingsgründer teilen ihr bisheriges
Gesamtbudget 80:20 auf 000 und 001 auf. Andere Speicher bleiben leer:

| Gründer | Gesamtbudget | 000 | 001 |
|---|---:|---:|---:|
| Standard / normale Trainingsgeneration | 1,0 | 0,8 | 0,2 |
| Aasregen-Episode | 0,18 | 0,144 | 0,036 |

Der niedrige `Cell`-Konstruktor erstellt weiterhin nur einen rohen Körper; explizite
Testaufbauten, Überreste und biologische Kinder weisen ihre tatsächlichen Vorräte
anschließend selbst zu. Er ist kein Ersatz für die Gründer-Fabrik.

Eine biologische Teilung teilt jeden der acht vorhandenen Vorräte wie bisher.
Sie erzeugt keine Gründerreserve. Kinder und Trainingsgründer führen beim Erzeugen
keinen kostenlosen Initialisierungszyklus mehr aus: neutrale Aktorbefehle bis zum
ersten regulär finanzierten Weltschritt. Dadurch werden auch keine Denkgebühren
versehentlich dem Elternteil zurückerstattet, wenn dessen Reserven aufgeteilt werden.

Alte `.cells` ohne `energyUsageVersion` werden beim Einbringen in eine Simulation
einmalig umgestellt: 20 % des vorhandenen 000-Vorrats wandern nach 001, begrenzt durch
freien 001-Speicherraum. Andere Vorräte, DNA und Gedächtnis bleiben erhalten; Gesamtenergie
ändert sich nicht. Bereits vorhandenes 001 bleibt erhalten. Voller 001-Speicher erzeugt
keinen Überschussverlust. Überreste werden nicht umverteilt.

`Load` und `Load & Merge` veröffentlichen die umgestellte Population mitsamt Snapshot
unter derselben Simulationssperre. Direkte alte Weltaufbauten werden spätestens am
ersten Weltschritt umgestellt. Reines Lesen einer Archivdatei erhält ihre Originalwerte.
Die Versionsmarkierung wird gespeichert, sodass erneutes Laden/Speichern/Weiterlaufen
keine wiederholte Umverteilung auslöst. Alte Genome erhalten keine kostenlose passende
Enzymroute und können deshalb unter den neuen Bedingungen eine Denkpause erreichen.

## Trainings und frühere Studien

- NONE sowie HIGH_ENERGY, HIGH_POSITION und HIGH_POSITION2 verwenden die normale
  neue Verbrauchsregel; Gründer werden aufgeteilt. Ihre älteren Lernkurven sind keine
  Referenzwerte für dieses neue Modell.
- HIGH_POSITION3 bleibt ein ausdrücklich abgesicherter Mechanikversuch: 000 = 0,75
  wird wie bisher gesetzt, Umwandlung aus, Gehirngebühren ausdrücklich null. So wird
  nicht unbemerkt eine Stoffwechselaufgabe in den Mechanik-Lehrplan eingebaut.
- TEST_A/B/C verwenden fest verdrahtete Controller ohne berechnete Synapsen.
- Beide Aasregen-Varianten behalten Sonne aus, Teilung aus, 120-s-Episoden und die
  bisherigen Auswahlkriterien. Jetzt sind 000-Erwerb und 001-Versorgung getrennt relevant.
  Feste kostenlose Kontrollnetze überleben weiterhin mit Aufnahme; ohne Nahrung reicht
  0,144 Energie 000 noch 48 statt früher 60 Sekunden. Die 001-Variante benötigt weiter
  eine passende Umwandlung nach 000; dieselbe Futterform versorgt zugleich das Gehirn.
- Frühere lange Kosten-/Funktionsstudien verwendeten eine andere Energieökologie.
  Die isolierten Synapsenpreise bleiben numerisch vergleichbar; ihre Nutzenrechnung
  mit einem einzigen Energievorrat bewertet jedoch nicht das neue Zwei-Verbraucher-Modell.

## Prüfungen

315 unterschiedliche gezielte Tests bestanden (249 Kern-/Integrationsfälle plus
66 weitere Kalibrierungs- und Mechanikfälle; zwölf gemeinsame Fälle nicht doppelt gezählt).
Lokale Protokolle: `target/functional-energy-final-tests.log` und
`target/functional-energy-mechanics-tests.log`.

Geprüft: getrennte Abbuchung, Vorabfinanzierung, exakte tatsächliche Rechnung, einmalige
Freigabe, fehlende/knappe Speichergebühr, Pause ohne Speicher-/Taktänderung, Wiederanlauf
durch echte Absorption und automatische Umwandlung, kein erneutes Bezahlen alter Arbeit,
Altdateimigration mit Load/Merge/Save, unveränderte Gesamtbudgets, biologische Teilung,
beide Aasregen-Episoden, HIGH_POSITION3 sowie Größe, Rotation und periodische Geometrie.
Kein neuer langer Evolutionslauf und keine neue Ganzwelt-Performancebehauptung.
