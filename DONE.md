# Abgeschlossene Schritte

## Haftung in Trainingswelten per Schalter (18.10.2026)

- Schalter `trainingAdhesion` (Standard aus, GUI): Trainingswelten nutzen dann Tonus,
  Erkennung, Bindungen und Bodenhalt wie die NONE-Welt. `CalculationService.strategyFor`,
  `TrainingAdhesionTest` (2).

## Zonenläufe Bodenhalt (18.10.2026)

- Je 4 Seeds: nur Bindungen, dazu Bodenfeld, dazu Bodenfeld und Anker. Wenige Anker (Ø 9),
  verankerte Körper flach (Überstand Ø 4 px). Das Bodenfeld verkleinert gebundene Körper
  (6–7 gegen 11 Zellen). Population gleich. `BODENHALT.md`.

## Bodenhalt Stufe A/B experimentell (18.10.2026)

- Bodenfeld für anziehende Aktoren, Anker an Geländepunkten, GUI-/Studienschalter (Standard
  aus), Ankerkennzahlen und -zeichnung, `GroundHoldTest` (10). `BODENHALT.md`.

## Momentaufnahme mit Bindungen und Überstand-Kennzahl (18.10.2026)

- PNG-Momentaufnahmen in der Zonenstudie und als Nachzeichnung gespeicherter Populationen,
  Spalten `bondedSpan`/`bondedRise`/`riseCells`, `BondShapeTest` (3). Befund: dichte
  Haufen, keine Stängel. `ZELLVERBINDUNGEN.md`, Bilder in `docs/cell-bonds/`.

## Vergleich Bindungen gegen Stufe 1/2 (18.10.2026)

- 4 Seeds mit Bindungen und Geburtsbindung gegen 4 Seeds nur Stufe 1/2 (Winter 50 %,
  3600 s): Verbände größer und höher, Population gleich. `ZELLVERBINDUNGEN.md`.

## Seed-Wiederholung und Ablation Zellhaftung (18.10.2026)

- 4 Seeds Haftung/Kontrolle, je 2 Seeds ohne Halten, Erkennung bzw. Tonus (Winter 50 %,
  3600 s). Früherer Vorteil nicht bestätigt. `ZELLVERBINDUNGEN.md`.

## Zellhaftung Stufe 3/4 experimentell (18.10.2026)

- Eingerastete Aktorpaare und Geburtsbindung, GUI-/Studienschalter (Standard aus),
  Ablationsschalter für Tonus/Halten/Erkennung, `CellBondTest` (6). `ZELLVERBINDUNGEN.md`.

## Zellhaftung Stufe 1/2 (17.10.2026)

- Tonus, Kraft halten bei Pause, Abklingen bei Überresten, Affinität aus Aktor- und
  Körperfarbe mit überwindbarer Abweisung; Policy `OPEN_WORLD`, GUI-Regler, 6 neue Tests,
  Studienkennzahlen. `ZELLVERBINDUNGEN.md`.

## Bis 17.10.2026

- Physik/Vererbung, NaN-Kontakte, Mehrport-Neuronen, Modulvererbung, Trainingshänger,
  Größenänderung, Rotation, periodische Geometrie und konsistente Snapshots korrigiert.
  Einzelheiten in den thematischen Mechanik-, Netz- und Testwelt-Dokumenten.
- Sensororientierung, Sonnenkontakt, Mutation, Zielauswahl und Kraftvorzeichen geprüft/korrigiert.
- HIGH_POSITION3 und mechanische Testwelten erstellt.
- Sonnenindex, Raster und weitere Performanceansätze geprüft/optimiert.
- Lange NONE-Kostenversuche und Funktionsvergleich abgeschlossen/archiviert.
- Netzvereinfachung untersucht; produktiver DNA-erhaltender Ausführungsplan umgesetzt.
- Sonnenrate, Gehirnkosten und maximale Lebensdauer in der GUI regelbar.
- Zwei getrennte Energiereserven samt Transfer, Teilung, Sonne und Dateikompatibilität:
  `ed69a81`, `f2a3dd9`, `1de2bd2`; 158 gezielte Tests. Siehe `MEHRERE_ENERGIEN.md`.

Neue Abschlüsse hier mit Commit/Testbeleg ergänzen; offene Ideen nicht als erledigt markieren.

## Läufe mit Lebensdauer 2000 s (17.10.2026)

- Studienoptionen `zone.study.load` (`55c0ccc`) und `zone.study.lifespan` samt Höhenkennzahlen
  (`79e6676`); Fortsetzungs- und Lebensdauerläufe in `ZONEN_JAHRESZEITEN_ERGEBNISSE.md`.

## Langlauf Zonen ± Jahreszeiten (17.10.2026)

- `ZoneSeasonStudyTest` (opt-in), zwei Läufe à 2700 s. Ergebnisse und Grenzen in
  `ZONEN_JAHRESZEITEN_ERGEBNISSE.md`.
- `InheritanceRegressionTest` an Geburt ohne kostenlosen Gedanken angepasst (`8047f5b`).

## Neuronale Enzymdrossel (17.10.2026)

- `OutputLayout`, `NeuralNet.ensureOutputCount`, `CellBrain.ensureEnzymeOutput`,
  `Cell.enzymeThrottle`; Umwandlungsdurchsatz × Aktivität; Anzeige im Zelldialog.
- `EnzymeThrottleTest` (Abbildung, bitgenaue Migration, Gründer, Altdatei, Durchsatz,
  feste Controller, Pause). 262 von 263 gezielten Tests erfolgreich,
  `target/enzyme-throttle-tests.log`; der eine Fehlschlag bestand schon vorher und ist
  inzwischen angepasst (`InheritanceRegressionTest`, 12/12).

## Jahreszeiten und Sonnenkurve (17.10.2026)

- Jahresfaktor auf die Tag/Nacht-Intensität, GUI-Regler für Jahreslänge und Winterlicht.
- Populationsdiagramm mit angebotener Sonnenenergie; Abtastung in Simulationszeit.
- 204 gezielte Tests aller heutigen Energie-/Umweltschritte erfolgreich,
  `target/energy-environment-tests.log`. Siehe `JAHRESZEITEN.md`.
- Hinweis: Ein `mvn clean` hat ältere lokale Protokolle in `target/` entfernt; die in
  den Dokumenten genannten Messwerte bleiben die Referenz.

## Spektralzonen und Energieanzeige (17.10.2026)

- `SunSpectrum`: vier Zonen, 15-%-Mischstreifen, periodisch; Form je Strahl am
  Auftreffpunkt. GUI-Schalter, farbige Strahlen und Zonenhintergrund.
- Zelldialog: kleine Vorräte/Kapazitäten wissenschaftlich statt `0,00` (`024df5c`).
- 50 gezielte Tests erfolgreich, `target/energy-environment-tests.log` (Neulauf). Siehe `SPEKTRALZONEN.md`.

## Basale Notatmung gegen Gehirnpause-Sackgasse (17.10.2026)

- Ursache: Sonne nur 000, keine aktive Route 000 → 001, Teilung halbiert 001; pausierte
  Gehirne teilen nicht, also entsteht auch kein Enzym. Aus 001 → 000 war 000 → 001
  in einer Mutation unerreichbar.
- `BrainEnergyService`: Bedarfslücke aus freiem 000 mit 40 %, max. 0,003/s; schaltbar
  über `SimulationStepPolicy.useBasalBrainRespiration()`. `EnergyConversion`: Umkehrmutation.
- Neue `BasalBrainRespirationTest` (Bilanz, Ratenlimit, Grundumsatzschutz, Ausnahmen,
  Enzymvorteil, Umkehrmutation); Pausentests laufen explizit ohne Notatmung.
  146 gezielte Tests erfolgreich, Protokoll `target/energy-environment-tests.log` (Neulauf).

## Performance der Gehirnfinanzierung (17.10.2026)

- Serielle Vorabprüfung aus `daf8fb0` als zusätzlichen Engpass nachgewiesen;
  Finanzierung und Eingabeerzeugung mit getrennten, vollständigen Phasenbarrieren parallelisiert.
- Unnötige Genzählung bei kostenlosen Gehirnen und Arbeitsprüfung bei unbezahltem
  Speicher entfallen. Energiepreise und Pausenregeln unverändert.
- 2000-Zellen-Vergleich: 58,996 → 34,935 ms/Schritt; Zustandsprüfsumme über 180
  Schritte bitgenau gleich. 139 gezielte Tests erfolgreich. Konkreter Nutzerlauf offen.
- Messbedingungen und reproduzierbare Diagnose: `PERFORMANCE_REGRESSION_2026_10_17.md`.

## Acht Formen, Sensoren und Überreste (17.10.2026)

- `3702b83`: dauerhaftes Projektgedächtnis und AGENTS-Regeln angelegt.
- `fa1060b`: acht RGB-Energieformen, vollständige Eigen-/Nachbarsensorik,
  angehängte DNA-Eingänge und Migration ohne neue Verbindungen.
- `367d33f`: Überreste als normale Zellen mit Inertsteuerung, konservativem
  Absaugen, passiver Physik, Anzeige, Speicherung und Ausschluss als Elterngenome.
- 222 gezielte Tests erfolgreich, einschließlich warmer rekurrenter DNA-Migration
  von 213/304 Eingängen, acht Ressourcenbilanzen, 1000-Schritte-Testwelt,
  leer gesaugter Überreste am Weltrand und Save/Load/Load & Merge.
- Regeln und Grenzen: `MEHRERE_ENERGIEN.md`, `UEBERRESTE.md`.

## Aasregen und Szenariowechsel (17.10.2026)

- `CARRION_RAIN`: acht getrennte Kammern, normale Überreste als Futter, Sonne/Teilung
  gesperrt, 120-s-Episoden, Auswahl nach Überleben und tatsächlicher Aufnahme.
- Deterministischer Absorber überlebt 120 s und nimmt rund 1,180 Energie auf;
  Nichtabsorber stirbt nach 60 s. Futterzufuhr = Aufnahme + Verfall + Restfutter.
- Fünf Szenariotests einschließlich Fluchtausschluss; zwei Tests für Wechsel/Reset,
  Pause und wiederholtes Start/Stop. Welt-/Raster-/Snapshotwechsel erfolgt atomar.
- Details und Grenzen des ersten Curriculums in `AASREGEN.md`.
- `33c48d8`: Aasregen und sicherer Szenariowechsel im Berechnungsdienst.
- GUI: „New…“-Dialog mit Szenariobeschreibung, unverändertem Lauf bei Abbrechen,
  pausierter neuer Welt, aktualisiertem Fenstertitel und zurückgesetzter Kamera/Details.
- Abschlussprüfung: 70 Tests erfolgreich; Protokoll lokal
  `target/carrion-scenario-complete-tests.log`. Kein langer Evolutionslauf gestartet.

## Kompakte Default-Gründer (17.10.2026)

- Default-Fabrik mit 48 versteckten Neuronen statt 590: 24 gemeinsam, drei
  Zustandsgruppen à acht; 316 Eingänge/133 Ausgänge bleiben erhalten.
- NONE und HIGH_ENERGY nutzen nun ebenfalls diese Default-Fabrik. Explizite
  Versuchsdimensionen, geladene Genome und Vererbung bleiben unabhängig davon.
- Wegen der kleineren Breite höhere Verbindungsdichten: 0,1 im gemeinsamen Netz,
  0,025 für Zustandsgruppen. Einzelvergleich mit Seed 417: 4.275 → 1.303 Synapsen;
  niedrigere Speichergebühr, keine gemessene Ganzwelt-Performanceaussage.
- 71 gezielte Tests erfolgreich, darunter Sensorreaktion, Zustandssteuerung,
  unabhängige Vererbung, Migration, Aasregen und bestehende Trainingsstrategien.
  Lokales Protokoll: `target/compact-founder-final-tests.log`.

## Begrenzte Energieumwandlung (17.10.2026)

- Eine vererbte und mutierbare gerichtete Route mit Kapazität je Zelle; 80 % Ausbeute,
  maximal 0,1 Quellenenergie/s mal Kapazität. Keine kostenlose Rettung alter Genome.
- Alle 56 Routen, Verluste, Grenzen und Rückreaktionen geprüft. Save/Load erhält Gene;
  echte alte Dateien bleiben inaktiv. Eigene/Nachbarsensoren nach vollständiger Reaktion.
- Enzyme in biologischer Teilung, Trainingsnachkommen und HIGH_POSITION3-Körpern
  vererbt; Elternfaktoren berücksichtigt. Überreste und mechanisches HIGH_POSITION3
  wandeln nicht um. Anzeige der Gene in Zelldetails und eigener Profilingabschnitt.
- Details: `ENERGIEUMWANDLUNG.md`.
- `6fab9bd`: begrenzte Umwandlung, Vererbung, Sensorik und Dateikompatibilität.
- Variante `CARRION_CONVERSION`: ausschließlich 001-Futter, wählbar über New-Dialog.
  Kontrollnetz mit Aufnahme + Enzym überlebt 120 s und nimmt 1,68 Energie 001 auf;
  Aufnahme ohne Enzym, falsche Quelle und Enzym ohne Aufnahme genügen nicht.
- 201 gezielte Tests erfolgreich; lokales Protokoll `target/energy-conversion-final-tests.log`.
  Kein langer Evolutionslauf gestartet. Regeln und Grenzen in `AASREGEN.md`.

## Umwandlung nach Specialization (17.10.2026)

- Gemeinsame unveränderliche Genotyp-Tabelle mit acht Umwandlungsregeln, exprimiert
  über vorhandene Zellzustandsbits. Volle Tabelle in Teilung, Training und Zellverbänden
  vererbt; unabhängige Mutation je Genort. Keine neuen Sensor-/Output-Indizes.
- Alte Einzel-Enzyme behalten Verhalten in allen acht Rollen; echte Kompatibilitätsdatei
  aus `adb7158`, Herkunft und SHA-256 unter `src/test/resources/energy/README.md`.
- Gleiche feste Controller, gleiche Zellfarbe und gleiches Programm ermöglichen drei
  Stoffwechselschritte über normale Energieabgabe: Endenergie 0,146493 statt 0,097000
  ohne mittlere Rolle, nach 1 s ab Startwert 0,1. Gesamtbilanz inklusive Verluste und
  Grundumsatz stimmt, auch bei anderer Zellreihenfolge und über periodischen Rand.
- 216 gezielte Tests erfolgreich; lokales Protokoll `target/specialized-conversion-final-tests.log`.
  Kein langer Evolutionslauf; Regeln und offene Selektionsfragen in `ENERGIEUMWANDLUNG.md`.

## Getrennte Verbrauchsfunktionen (17.10.2026)

- 000 bezahlt Grundumsatz, 001 Speicherung/Rechenarbeit. Entscheidung vorfinanzieren,
  tatsächliche Synapsenarbeit einmal bezahlen, sonst ohne Gedächtnis-/Taktänderung pausieren.
- Gedächtniserhalt, Aufnahme/Umwandlung als Wiederanlauf, Stilllegung von Aktorkräften/
  Delivery/Teilung und Größe während Pause geprüft. Keine erneute Rechnung alter Arbeit.
- Standard-Gründer 0,8/0,2 statt 1/0, Aasgründer 0,144/0,036 statt 0,18/0; gleiche
  Gesamtenergie. Kein kostenloser erster Denkzyklus bei Geburten oder Trainingsgründern.
- Alte lebende Dateien beim Load/Merge einmalig konservativ umstellen und versionieren;
  Überreste behalten Vorräte. HIGH_POSITION3 bleibt expliziter Mechanikversuch ohne Gehirngebühr.
- 315 unterschiedliche gezielte Tests erfolgreich; Details und Protokolle in
  `FUNKTIONALE_ENERGIE.md`. Kein langer Evolutionslauf gestartet.
