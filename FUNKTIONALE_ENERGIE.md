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

## Basale Notatmung 000 → 001

Umgesetzt am 17.10.2026 nach einem festgefahrenen NONE-Lauf: Sonne liefert nur 000,
Gründer- und Altgenome besitzen keine aktive Route 000 → 001, und jede Teilung halbiert
den 001-Vorrat. Nach wenigen Generationen pausierten fast alle Gehirne; ohne Gehirn
gibt es keine Teilung, ohne Teilung keine Mutation, die ein Enzym hätte entwickeln
können. Eine evolutionäre Sackgasse, auch in neu initialisierten Welten.

Jede lebende Zelle besitzt deshalb eine angeborene, nicht vererbte und nicht
mutierende Notatmung. Sie ist keine Enzymroute und belegt nicht den einen aktiven
Stoffwechselplatz; sie ist Teil der Gehirnfinanzierung in `BrainEnergyService.prepare`:

- Nur bei Bedarf: gedeckt wird höchstens die Lücke zwischen vorhandenem 001 und
  Speichergebühr plus Arbeitsreserve des nächsten Zyklus. Kein Vorratsaufbau.
- Halbe Enzymausbeute: 40 % statt 80 %; 60 % des verbrauchten 000 dissipieren.
- Langsam: höchstens 0,003 000 pro Simulationssekunde (gleich dem Grundumsatz), also
  maximal 0,0012 001/s. Ein Standardgründer (1286 Synapsen) braucht im Messlauf
  etwa 0,00088 001/s und wird so knapp vollständig versorgt. Größere oder aktivere
  Gehirne sparen über mehrere Schritte an und denken nur in Teilen der Schritte.
- Der 000-Anteil für den Grundumsatz dieses Schritts bleibt unangetastet. Das Gehirn
  kann den Körper nicht im selben Schritt verhungern lassen; unterhalb davon pausiert es wie bisher.
- Keine Notatmung für Überreste, für kostenlose Gehirne (keine Lücke) und für
  Episoden ohne Stoffwechsel (`useEnergyConversion() == false`, z. B. HIGH_POSITION3).
  `SimulationStepPolicy.useBasalBrainRespiration()` folgt standardmäßig diesem Schalter.
- Sie läuft vor dem Einfrieren der Eingaben; Sensoren sehen den neuen 001-Stand.

Vorteil des vererbten Enzyms bleibt messbar und ist getestet: 000 → 001 mit 80 %
kostet je Gehirnenergie halb so viel 000, liefert bis zu 0,08 001/s (66-fache
Notatmung), füllt bei Überschuss einen Vorrat bis 1 als Puffer und erlaubt damit
größere Gehirne oder Pausenfreiheit bei hohem Gehirnkostenregler. Aasfresser können
001 direkt aufnehmen. Selektion auf Stoffwechsel bleibt also wirksam, nur nicht mehr
als Alles-oder-nichts-Hürde vor jeder weiteren Evolution.

Dazu kann eine Enzymroute jetzt per Mutation ihre Richtung umkehren (eigener Genort,
gleiche effektive Rate wie Quelle/Ziel/Kapazität). Die inaktive Startroute 001 → 000
erreicht damit in einem Schritt 000 → 001; vorher war diese Route aus der Startroute
in einer einzelnen Mutation unerreichbar (die Quelle durfte nie gleich dem alten Ziel werden).

### Vergleichslauf (17.10.2026)

Default-NONE-Welt, `NeuralNetwork`-Seed 4711, dt 1/60, 600 Simulationssekunden,
gleicher Code mit Umkehrmutation, nur die Notatmung per Policy an/aus. Temporärer
Messtest, nicht eingecheckt; Lokales Protokoll durch `mvn clean` entfernt; Werte unten.
„Enzym“ zählt Zellen, deren exprimierte Route aktiv 000 → 001 ist. Ein Lauf je Variante.

| t | ohne: lebend / pausiert / Enzym | mit: lebend / pausiert / Enzym |
|---:|---|---|
| 60 s | 477 / 416 / 4 | 768 / 76 / 18 |
| 120 s | 550 / 512 / 3 | 1634 / 152 / 33 |
| 300 s | 947 / 452 / 29 | 1134 / 70 / 49 |
| 600 s | 1124 / 298 / 32 | 1191 / 126 / 28 |

Bewertung: Die Notatmung beseitigt die Sackgasse sofort. Ohne sie erholt sich eine
*neue* Welt dank Umkehrmutation zwar langsam, ein bereits vollständig pausierter
Altlauf aber nicht (keine Teilungen). Mit Notatmung setzt sich das Enzym in 600 s
jedoch nicht durch; die Welt ist durch 000 begrenzt (mittleres 000 ≈ 0,05), und für
Standardgehirne reicht die Notatmung. Ob der doppelte Wirkungsgrad langfristig
selektiert wird, ist offen; Stellschrauben sind Ausbeute/Rate der Notatmung und der
Gehirnkostenregler.

## Pause und Erholung

- Keine neue neuronale Ausführung und keine neuen Inputs in das pausierende Gehirn.
- Aktorkräfte, Delivery und Teilungswunsch werden deaktiviert. Bereits aus der letzten
  bezahlten Entscheidung im aktuellen Schritt berechnete Impulse bleiben physikalisch gültig.
- Laufendes Wachstum und Größenanpassung pausieren; der bisherige Zielwert und
  Wachstumsfortschritt werden nicht zurückgesetzt. Biologisches Alter läuft weiter.
- Zuletzt gewählte Absorptionsports bleiben verfügbar. Es gibt keinen neu erfundenen
  kostenlosen Allport-Saugmechanismus. Bei zuvor ausgeschalteter Absorption benötigt
  die Zelle etwa Sonnenenergie 001, eigene automatische Umwandlung oder die basale
  Notatmung aus freiem 000 zur Erholung.
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
Enzymroute; sie werden seit der basalen Notatmung langsam und verlustreich aus 000
versorgt und wachen in bereits festgefahrenen Welten im nächsten Schritt wieder auf.

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
