# Abgeschlossene Schritte

## Taktgeber (25.10.2026)

- Sechs vererbte Taktgeber, Sinus/Kosinus des Zellalters, angehängt (328 Eingänge), Migration von
  213/304/316 Eingängen ohne Synapsen, Vererbung bei Teilung/Gründern/HIGH_POSITION3, alte Speicherstände
  mit Oktaven. `TAKTGEBER.md`, `ClockInputTest`, angepasst `EightEnergySensorTest`, `CompactFounderBrainTest`,
  `EnzymeThrottleTest`.
- `ActorRhythmTest`: Tonus lässt Takte ab 1 s als Vorzeichenwechsel durch (0,5 s nicht, ungleiche Kraftgrenzen);
  Zufallsgehirne bewegen die Kraft durch die Takte im Median nur um 0,002.
- Direkte Taktsynapsen neuer Gehirne (12 Takteingänge → 12 Aktorkräfte und Größe, ± 0,5): Zufallsgründer
  schwingen (16 von 16; RACE 32 von 32). `ClockInputTest`, `ActorRhythmTest`.
- RACE-Studie mit Taktgebern, 300 Generationen, 10 % Schwerkraft: `oscillating 0` durchgehend, kein Trend,
  23 Katastrophen (`RENNBAHN.md`).

## RACE-Inselmodell (24./25.10.2026, experimentell)

- 32 Bahnen in 4 Inseln, Elite je Insel mit Neubewertung (Zufallsdrehung, ± 40 px), Turnier aus 3,
  Ringmigration alle 10 Generationen, Katastrophe nach 30 Generationen ohne Rekord (> 10 % + 0,1 px).
  `RaceTrainStrategyTest` prüft Inseln, Migration, Katastrophe und Zufallsstart.
- Studie, 587 Generationen: kein Gang. Spitzen bis 5,5 px hingen an der Startlage, Median 0, 58 Katastrophen.
- 25.10.2026: Bewertung aus 3 Startlagen (Mittelwert); Studie zählt schwingende Aktoren und pulsierende Größe.

## Geländereibung und RACE-Wachstumsphase (24.10.2026, experimentell)

- Schalter `blockerFriction` mit Reibung (0,5) und Rollwiderstand der Kontaktfläche (0,5): Haften, Gleiten
  und Rollen am Kontaktpunkt, gemeinsam gelöst, innerhalb des Schritts (kein Kriechen). GUI, Settings-Datei.
  `BODENREIBUNG.md`, `BlockerFrictionTest`.
- Reibungsgang (`RaceGaitTest`): 21 px in 30 s ohne Anker. Abstandsregler misst die Öffnung jetzt aus der
  Abstandsänderung; vergessener Diagnoseblock im Gangtest entfernt (Anker 15 → 10,8 px).
- RACE: Geburtsschub über Reibung (1,95 px) entdeckt; Teilung nur 0–30 s, Fortschritt ab 35 s.
- Studie 100 Generationen: kein Gang, kein Anker; Bestwert ein Ruck von 0,85 px, ab Generation 2 unverändert.

## Rennbahn-Training RACE (24.10.2026, experimentell)

- `TrainMode.RACE`: 16 Bahnen, je ein Gründer, klonale Teilung bis 6 Zellen, Energie gesichert.
  Bewertung „Eigenfortschritt“ (mittlerer Schritt der schon vorhandenen Zellen) ist immun gegen
  Teilung nach vorn und Schieben durch Masse; Geburt in der Rennbahn impulsneutral.
- Befund: Vortrieb nur über Anker (Kräfte zwischen Zellen heben sich auf, Boden reibungsfrei); ein
  von Hand geregelter Zwei-Zellen-Gang schafft 15 px in 30 s. Zufallsgründer finden in 30 Generationen
  keinen Fortschritt (Startproblem). `RENNBAHN.md`, `RaceGaitTest`, `RaceTrainStrategyTest`.

## Schwerkraft- und Reibungsregler (24.10.2026)

- Umweltpanel „Mechanics“: Schwerkraft in % des Standards (0–300 %, Standard 9,81 · 3), Reibung der
  Bewegung (Viskosität 0–4) und Drehreibung (0–4), beide Standard 0,8. Live wirksam, in der
  `.cells.settings`-Datei gespeichert (`gravity`, `viscosity`, `rotationalFriction`); alte Dateien
  behalten die aktuellen Werte. `MechanicsSettingsTest` prüft die Wirkung, GUI- und Settings-Tests
  den Weg über Regler und Datei.
- Zelldialog: Gehirnbild unter der Sperre nur kopiert, gezeichnet danach.

## Rechen- und Render-Thread entkoppelt (24.10.2026)

- Snapshot nach dem Schritt im Rechen-Thread (auf Anforderung, `volatile` veröffentlicht), faires
  `ReentrantLock`, Schlafen nur bei Vorsprung (das alte `sleep(0, 500_000)` kostete 1,25 ms pro Schritt),
  minimiert kein Zeichnen. Etwa +12 % Schritte/s bei 30 FPS (`RenderHandoffTest`).

## Wechselwirkungen ohne periodische Rechnung pro Aktorpaar (24.10.2026)

- Paarversatz aus Mittelpunktsversatz und relativen Aktorpositionen, Reichweiten und Drehung einmal pro
  Zelle. Nicht mehr bitgleich (Rundung), Kraft = Gegenkraft geprüft. A/B 1,98 → 1,55 ms pro Aufruf.

## Bindungsupdate in zwei Phasen (24.10.2026)

- Eigene Aktoren parallel, Paarfälle seriell in Listenreihenfolge; reihenfolgeunabhängig und
  deterministisch. Verankerte Aktoren nehmen keinen Partner mehr. `IntegrationEquivalenceTest`
  prüft Konsistenz, Determinismus und Nähe zur seriellen Referenz. WORLD2 INTEGRATION 2,1 → 1,4 ms/Schritt.

## Gehirne nur mit Referenz- und Stempelprüfung (24.10.2026)

- Neue Regel in `AGENTS.md`: Getter-Arrays nicht an Ort und Stelle beschreiben (Produktivcode geprüft).
  Inhaltsvergleich nur noch mit `-Dlife.verifyCompiled=true`; die Tests mit absichtlichem
  Direktschreiben nutzen ihn. Ausgänge direkt aus dem Wertearray. Isoliert 15,6 → 8,0 ms pro Runde,
  WORLD2 BRAINS 2,5 ms/Schritt, alle Phasen 10,1 ms (heute früh 29,0).
- Nutzervorgabe: Bitgleichheit ist für Optimierungen nicht nötig, solange der Ablauf sinnvoll bleibt
  und die Evolution keine Lücke bekommt (`AGENTS.md`).

## Gehirnzensus für die Finanzierung (24.10.2026)

- Arbeitsschranke und gespeicherte Gene von Netzen ohne Module werden zwischengespeichert und über
  Referenzen und Gen-Stempel geprüft statt jedes Mal das ganze Netz zu durchlaufen. Exakt gleich
  (`BrainCensusTest`); isoliert 8,9 → 2,6 ms pro Runde, WORLD2 BRAIN_FUNDING 1,4 → 1,0 ms/Schritt.

## Schnellere Integration (24.10.2026)

- Blockerkollision mit achsweisem Vortest vor `hypot`; `updateBonds` mit Zellmarke statt
  `IdentityHashMap` und indexierter Anker-Untergrundprüfung. Bitgleich zu `IntegrationReference`
  (`IntegrationEquivalenceTest`, Gegenprobe mit zu knappem Vortest schlägt an). WORLD2:
  INTEGRATION 2,2 ms/Schritt bei 989 Zellen (vorher 3,1–3,6 bei 1152–1246).

## Paralleler Energietransfer (24.10.2026)

- Energieformen parallel berechnet, seriell in Formreihenfolge geschrieben; Ports pro Zelle
  parallel gesammelt. Bitgleich (`EnergyTransferLinkRegressionTest`); isoliert 8,8 → 1,95 ms pro
  Aufruf, WORLD2-Phase 3,6 → 1,6 ms/Schritt. Benchmark: `TransferBenchmarkTest` (opt-in).

## Schnellere Sensor-/Aktor-Wechselwirkungen (24.10.2026)

- Berührte Blocker über `TerrainIndex`, Nachbarzellen pro Aktor per Dreiecksungleichung
  ausgeschlossen, Affinität nur in Reichweite. Bitgleich zur Referenzkopie
  (`InteractionEquivalenceTest`); isoliert 6,5 → 1,7 ms pro Aufruf, WORLD2-Phase 9,1 → 1,7 ms/Schritt.
- Laden: Lade-Logik in `MainController.loadCells`, GUI-Test mit echter Settings-Datei;
  die Konsole meldet, ob Settings übernommen wurden oder keine Datei da war.
- „Load & Merge“ hat die Option „with settings“ (Standard aus, wird gemerkt): übernimmt dann
  auch die gespeicherten Umwelt-Einstellungen, den Schrittzähler aber nie.

## Kompilierte Vorwärtsrechnung (24.10.2026)

- `CompiledForward`: flache Arrays statt Objektgraph, gleiche Rechenreihenfolge; bitgleich
  in Ausgaben, Eingangssummen, Layer-Takt und Synapsenarbeit. Gen-Stempel plus
  Inhaltsvergleich erkennen Änderungen. Kompilierte Netze verzichten auf den
  Ausführungsplan, der in der Populationsmessung netto langsamer war.
- Belege: Populationsbenchmark 64–73 → 30,5 ms/Runde (1 Thread, 1117 Gehirne);
  WORLD2 (Seed 4711, 300 s) Gehirnphase 9,56 → 4,49 ms/Schritt, Summe 29,0 → 24,4.
  `CompiledForwardTest` neu, `ExecutionPlanWorldTest` vergleicht nun gegen den reinen
  Objektpfad. Details: `NETZKOMPILIERUNG.md`.

## Java-25-LTS-Laufzeit verifiziert (23.10.2026)

- Die vorhandene Maven-Konfiguration setzt `maven.compiler.source` und `target` bereits
  auf 25; keine Code- oder Abhängigkeitsänderung war nötig.
- Verifiziert mit Temurin 25.0.3 und Maven 3.10.0: alle 97 Produktions- und 120
  Testquellen frisch kompiliert; `Main.class` hat Classfile-Version 69. Die gezielten
  Tests `BrainEnergyLabTest` und `PeriodicWorldTest` bestanden (50/50).
- Alte Java-21-Buildausgaben wurden in den beiden Klassenverzeichnissen gezielt erneuert;
  Studienprotokolle und sonstige `target`-Daten blieben erhalten. Keine vollständige
  Testsuite wegen der darin enthaltenen Langzeitstudien. JavaFX-GUI-Start nicht geprüft.
- Ausführungsdetails: `.github/modernize/java-upgrade/20261014105451/progress.md`.

## Umwelt-Einstellungen neben der Zelldatei (22.10.2026)

- „Save“ und „Save Best“ schreiben zusätzlich `<datei>.cells.settings` (lesbare Properties):
  Sonnenrate und -form, Spektralzonen und Zonenbreite, Jahreszeiten, Haftung, Trainingshaftung,
  Bindungen, Geburtsbindung, Halbwertszeit der Überrestkräfte, Abweisung, Bodenfeld, Anker,
  „nur Boden“ und Geländehaftung. „Load“ übernimmt sie und aktualisiert das Umweltpanel;
  „Load & Merge“ lässt die aktuellen Einstellungen stehen. Format der `.cells`-Datei
  unverändert; ohne Settings-Datei bleibt alles wie bisher. Szenario und Weltgröße werden nur
  vermerkt: Laden behält die aktuelle Welt und meldet auf der Konsole, wenn die Zellen aus einer
  anderen stammen (z. B. WORLD2-Zellen in NONE). Ungültige Werte werden gemeldet und übergangen.
- Die Schrittzahl wird mitgespeichert; „Load“ setzt den Zähler der Statusleiste auf den
  gespeicherten Stand, damit die gesamte Rechenzeit einer Welt sichtbar bleibt. Anzeige mit
  Tausenderpunkten („Steps: 1.234.567“).
  `SimulationSettingsFile`, `SimulationSettingsFileTest` (4), `MainControllerLayoutTest`.

## WORLD2 und wiederholte Spektralzonen (20.10.2026)

- `sunZoneWidth` (GUI-Regler), `SunSpectrum` mit Zonenbreite, `World2TrainStrategy`,
  Umweltpanel übernimmt Szenariowerte. Beschleunigung ohne Ergebnisänderung: Geländesuche
  über x-Index (`TerrainIndex`), Energietransfer berechnet Ports einmal je Schritt
  (`EnergyTransferLinkRegressionTest` vergleicht bitgenau mit dem alten Algorithmus). `WELT2.md`.

## HIGH_POSITION2 mit 120-s-Generationen (19.10.2026)

- `off`/`full`, 60 Generationen, Seeds 4711–4714 vollständig. Beide Varianten höher als mit
  50 s (293 / 251 px), Haftung ohne Vorteil.
  `BODENHALT.md`.

## Trainingsläufe mit Haftung (19.10.2026)

- `TrainModeStudyTest`; HIGH_POSITION2/3 × off/adhesion/full × 4 Seeds. Kein Höhenvorteil
  in HIGH_POSITION2; in HIGH_POSITION3 schaffte nur ein Lauf mit Bindungen HOLD und RECOVER.
  `BODENHALT.md`.

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
