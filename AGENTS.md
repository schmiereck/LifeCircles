# LifeCircles: Arbeitsregeln und Projektgedächtnis

## Ziel

Eine offene Evolution in größeren Welten untersuchen. Langfristiges Ziel sind aus
gemeinsamer DNA entstehende mehrzellige Organismen mit unterschiedlichen Zellrollen,
Arbeitsteilung und koordiniertem Verhalten. Mechanik, Energieökologie, Gehirnleistung
und Laufzeit gemeinsam betrachten. Training dient als Vorbereitung für NONE-Welten.

## Vor Änderungen lesen

- `ACTUAL.md`: aktueller Stand und Grenzen.
- `TODO.md`: priorisierte nächste Arbeiten.
- `CONCEPTS.md`: akzeptierte Modellentscheidungen.
- `IDEAS.md`: Vorschläge, die noch nicht als Umsetzung beschlossen sind.
- `DONE.md`: abgeschlossene Schritte und Belege.

Diese Dateien sind das dauerhafte Projektgedächtnis. Bei fachlichen Änderungen
mitpflegen; neuere ausdrückliche Nutzervorgaben haben Vorrang. Bereits implementierte
Funktionen, Experimente und Zukunftsideen klar kennzeichnen. Wichtige Details in
thematische Dokumente verlinken, Messwerte mit Herkunft und Grenzen dokumentieren.

## Zusammenarbeit

- Mit dem Nutzer auf Deutsch kommunizieren und autorisierte Arbeit vollständig erledigen.
- Nach jedem abgeschlossenen logischen Schritt committen; Nutzeränderungen erhalten.
- Lokal auf `master` arbeiten, sofern nicht anders gewünscht. Kein Push ohne Auftrag.
- Keine Delegation an weitere Agents ohne ausdrücklichen Auftrag.
- Für neue Modellregeln kleine deterministische Testwelten und sinnvolle Bilanztests bauen.
- Keine pauschale vollständige Maven-Testsuite: alte Testklassen enthalten sehr lange
  Studien. Gezielte Tests wählen; lange Evolutionsexperimente ausdrücklich starten.
- Laufende GUI und laufende Studien nicht beenden. Änderungen beeinflussen ihre bereits
  gestarteten JVMs nicht; Messreihen verschiedener Modellstände getrennt halten.

## Technische Leitplanken

- Java 25 / JavaFX. Maven: `C:/Users/thomas/.maven/maven-3.10.0/bin/mvn.cmd`.
- Periodische Geometrie überall beachten; positive Y-Richtung zeigt nach unten.
- Snapshots nur aus konsistentem Simulationszustand erzeugen.
- DNA und Ausführung trennen. Optimierungen dürfen das Modell nicht still verändern
  (Gedächtnis, Layer-Takt, Energiegebühren, Bilanzen). Bitgleiche Ergebnisse sind nicht nötig
  (Nutzervorgabe 24.10.2026): geänderte Rechenwege und Reihenfolgen sind erlaubt, solange der
  Ablauf sinnvoll bleibt und die Evolution keine Lücke bekommt, in die sie sich hineinoptimiert.
  Topologieänderungen nach übersprungener Ausführung erfordern eine frische Kopie oder einen
  expliziten Gedächtnisreset.
- Arrays, die Netz, Layer und Neuronen über Getter herausgeben (Neuronen, Layer, Synapsen), nicht
  an Ort und Stelle beschreiben, sondern über die Methoden ersetzen; sonst bemerken kompilierte
  Gehirne und der Finanzierungszensus die Änderung nicht (`NETZKOMPILIERUNG.md`). Tests prüfen
  Verstöße mit `-Dlife.verifyCompiled=true` bzw. `CompiledForward.setVerifyContents(true)`.
- Ressourcen separat bilanzieren: keine kostenlose Energie bei Teilung, Tod oder
  Umwandlung. Änderungen an DNA-Ein-/Ausgängen und Dateiformaten explizit migrieren.
- Energie-Sensoren umfassen eigene **und benachbarte** Vorräte; neue Formen dort mitführen.
- Sensorpositionen nicht still verschieben: Erweiterungen liegen im angehängten
  `EnergyInputLayout`, Ausgänge im angehängten `OutputLayout`. Aktuell zwölf Aktoren:
  328 Eingänge / 134 Ausgänge (133 = Enzymdrossel; Eingänge 316–327 = sechs vererbte Taktgeber als
  Sinus/Kosinus des Zellalters, `TAKTGEBER.md`); vorhandene Netze mit 213, 304 oder 316
  Eingängen bzw. 133 Ausgängen werden ohne neue Synapsen migriert; nur neue Gehirne mit 328 Eingängen erhalten
  direkte Taktsynapsen zu Kraft und Größe (`CellBrain.wireClocks`). Neue Zellgene (wie `clockPeriods`)
  bei Teilung, Trainingsgründern und HIGH_POSITION3 mitkopieren und alte Speicherstände in `readObject` füllen.
- Überreste sind normale `Cell`-Objekte mit festem Verhalten und Restreserven. Kein
  zusätzliches Materie-/Körpermaterialmodell. Sie verschwinden, wenn alle Vorräte leer sind.
- Acht Energieformen entsprechend RGB-Bits 000 bis 111. Nur Komponenten 0 und 1;
  keine Zwischenwerte oder negativen Typcodes. Energievorräte selbst bleiben kontinuierlich.
- Stoffwechselprogramm vollständig vererben: acht Regeln für die bestehenden
  Spezialisierungen (untere drei cellState-Bits), nur eine aktive Route pro Zelle.
  Kind-Spezialisierung bestimmt das Gehirn; dieselbe DNA darf andere Rollen exprimieren.
  Alte Einzel-Enzyme in alle acht Einträge migrieren. Keine feste Kopplung an Zellfarben.
- Verbrauch: 000 Erhalt/Tod, 001 Gehirn. Kein produktiver Denkzyklus ohne vorherige
  Finanzierungsprüfung; tatsächliche Arbeit einmal abrechnen. Pause erhält Gedächtnis
  und Takte, Aufnahme/Enzyme ermöglichen Erholung. Angeborene Notatmung deckt nur die
  Gehirnlücke aus freiem 000 (40 %, max. 0,003/s), nie den Grundumsatz des Schritts;
  vererbte Enzyme müssen besser bleiben. Gründerbudgets 80:20 teilen;
  biologische Geburten teilen vorhandene Vorräte und denken erst im bezahlten Schritt.
  Altpopulationen via energyUsageVersion einmalig konservativ umstellen; tote Vorräte
  unverändert lassen. HIGH_POSITION3 hat ausdrücklich freie Gehirne (Mechanikversuch).
- Zellhaftung (`ZELLVERBINDUNGEN.md`, Policy `OPEN_WORLD` der Default-NONE-Welt, GUI-Schalter):
  Aktorkräfte folgen dem Gehirn mit Tonus, bleiben bei Pause erhalten und klingen bei
  Überresten mit Halbwertszeit ab (∞ = Holz). Anziehung wirkt voll nur bei ähnlicher
  Aktorfarbe **und** Körperfarbe, sonst teils als begrenzte, überwindbare Abweisung.
  WORLD2 (`WELT2.md`) ist offen wie NONE und schaltet beim Anlegen Zonen, Haftung, Bindungen und Bodenhalt ein.
  RACE (`RENNBAHN.md`) folgt ebenfalls dem Hauptschalter Haftung und schaltet Geländereibung ein
  (Vortrieb über Reibung und Anker; Teilung nur in der Wachstumsphase, Fortschritt zählt danach).
  Trainingswelten (alle übrigen TrainModes außer NONE/EVOLUTIONARY/WORLD2/RACE) nutzen Haftung mit Tonus, Erkennung,
  Bindungen und Bodenhalt nur mit dem Schalter `trainingAdhesion` (Standard aus, Policy `TRAINING`);
  die Alt-Policy `SimulationStepPolicy.NONE` bleibt farbblind und ohne Tonus.
  Bindungen (Stufe 3) und Geburtsbindung (Stufe 4) sind experimentell und nur per Schalter aktiv.
- Bodenhalt (`BODENHALT.md`, experimentell, Schalter aus): Gelände zieht nur anziehende Aktoren
  an (Affinität zu Grau, keine Abweisung, kein Abstoßen); Anker sind Bindungen an einen festen
  Geländepunkt mit denselben Lösungsregeln.
- Geländereibung (`BODENREIBUNG.md`, experimentell, Schalter `blockerFriction` aus): Coulomb-Reibung und
  Rollwiderstand am Kontaktpunkt, begrenzt durch den Normalimpuls der Kollision, wirkt innerhalb des
  Schritts (Position/Drehwinkel korrigiert, kein Kriechen).
