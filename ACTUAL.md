# Aktueller Projektstand

Aktualisiert: 29.10.2026. Branch `master`.

- Spektralzonen mischbar (29.10.2026): Grundanteil jeder Form in jeder Zone, Übergangsbreite und Kurvenform
  einstellbar (GUI, Einstellungsdatei); Standard wie bisher. `SPEKTRALZONEN.md`.

- Teilung und Wachstum (28.10.2026, alle Welten): Die Teilung teilt die Fläche (Mutter r/√2, Kind wächst an ihrem
  Rand auf die andere Hälfte), der Impuls wird geteilt, und keine Zelle wächst schneller als 20 px/s. Vorher
  schleuderte schnelles Aufblähen Nachbarn mit 153 px/s weg (Lücke 32 px), jetzt 75 px/s (11 px). Gegen das
  „Fliegen“ durch Teilung in großen Welten; Sichtung in der GUI steht aus. `TEILUNG.md`.
- Geländereibung ist seit 28.10.2026 Standard (vorher nur RACE).

- Brownsche Bewegung (27.10.2026, Schalter, Standard aus): zufällige Stöße ohne Strömung in allen Welten außer
  RACE; Modelle COLLISIONS und LANGEVIN austauschbar, Dichte 20 Treffer/s und Stärke 5 px/s als kalibrierter
  Standard (Einzelzellen wandern, verwandte Paare halten). `BROWNSCHE_BEWEGUNG.md`.

- Taktgeber (25.10.2026, alle Welten): sechs vererbte Perioden (Oktaven 0,5–16 s als Start), Sinus/Kosinus des
  Zellalters als Eingänge 316–327, jetzt 328 Eingänge; alte Netze ohne neue Synapsen migriert. Neue Gehirne
  verbinden die Takte direkt mit Aktorkraft und Größe (± 0,5), Zufallskörper schwingen damit. `TAKTGEBER.md`.
- RACE bewertet jedes Genom aus 3 Startlagen (Mittelwert); die Studie zählt schwingende und pulsierende Körper.
  Studie mit Taktgebern (300 Generationen, 10 % Schwerkraft): kein schwingender Körper, Bestwert gemittelt
  1–2 px ohne Trend (Abdriften, kein Gang). Mit direkten Taktsynapsen (300 Generationen): stetiger
  Fortschritt, Bestwert 5,7 → 12,6 px, Median 0,5 → 4,5 px, Vortrieb ohne Anker. `RENNBAHN.md`.
- RACE seit 26.10.2026: Start in der Bahnmitte, der Weg in beide Richtungen zählt (Betrag). Studie 600
  Generationen: Anstieg bis G200 (Bestwert ~30 px), danach Plateau 20–26 px; vermutlich löschen Katastrophen
  die besten Inseln, weil ihr Rekord aus einer verrauschten Spitze stammt. Seitdem: Rekord als gleitendes
  Mittel über 10 Generationen, die führende Insel wird nie gelöscht. Studie damit (600 Generationen): Anstieg
  bis zum Ende, Bestwert zuletzt 34–35 px (vorher 22–25), Spitze 54,9 px, 32 Katastrophen. `RENNBAHN.md`.

- Rennbahn-Training RACE (experimentell): kleine klonale Verbände (≤ 6 Zellen) werden nach eigenem
  Fortschritt nach rechts ausgewählt; Teilung nach vorn zählt nicht, Teilung nur in einer Wachstumsphase
  (Fortschritt zählt erst danach). Vortrieb über Anker oder die neue Geländereibung; Zufallsgründer finden
  auch mit Reibung in 100 Generationen keinen Gang (Bestwert ein einzelner Ruck von 0,85 px). Seit `ab64ff7`
  Inselmodell: 32 Bahnen in 4 Inseln, Turnier, Zufallsstart (Elite wird neu bewertet), Migration, Katastrophe
  nach 30 Generationen Stillstand. Studie (587 Generationen, wegen Speichermangel abgebrochen): einzelne
  Spitzen bis 5,5 px, die sich aus einer anderen Startlage nie wiederholen; Median 0; 58 Katastrophen. `RENNBAHN.md`.
- Geländereibung (Schalter, Standard an seit 28.10.2026; RACE schaltet sie ohnehin ein): Coulomb-Reibung und
  Rollwiderstand an Blockern, begrenzt durch das Andrücken. Ein von Hand geregelter Zwei-Zellen-Gang läuft
  damit ohne Anker 21 px in 30 s (mit Ankern 11 px). `BODENREIBUNG.md`.

- Java-Laufzeit und Compilerziel sind auf Java 25 LTS. Verifiziert mit Temurin 25.0.3:
  97 Produktions- und 120 Testquellen frisch kompiliert, `Main.class` hat Classfile-Version
  69. `BrainEnergyLabTest` und `PeriodicWorldTest`: 50 Tests bestanden. Der vollständige
  Satz mit langen Studien wurde nicht gestartet. Details:
  `.github/modernize/java-upgrade/20261014105451/progress.md`.

- Zellhaftung Stufe 1/2 in der Default-NONE-Welt (GUI-Schalter, Standard an): Kräfte
  bleiben bei Gehirnpause, Überreste lassen mit 300 s Halbwertszeit los (∞ = Holz),
  Anziehung nur zwischen passender Aktor- und Körperfarbe, Fremde werden begrenzt
  abgewiesen. Der Vorteil des ersten Vergleichslaufs (437 statt 183 Zellen, besiedelte
  010-Zone) hat sich mit 4 Seeds nicht bestätigt (Ø 236 gegen 168, ein Seed klar
  schlechter, 010-Zone nur einmal). Ohne Halten am schwächsten, farbblind größere Verbände;
  keine Stängel. Stufe 3/4 (Bindungen, Geburtsbindung) experimentell per
  Schalter; gebundene Säulen bleiben formstabil, kippen aber als Ganzes. In Zonenläufen
  (4 Seeds) sind die Verbände mit Bindungen größer und höher (Ø 173 gegen 112 px),
  Population gleich. Momentaufnahmen mit Bindungslinien zeigen dichte Haufen mit kurzen
  Bindungen; am Ende ragt kein gebundener Körper mehr als 27 px heraus, keine Stängel. `ZELLVERBINDUNGEN.md`.
- Bodenhalt experimentell (Schalter aus): Bodenfeld für anziehende Aktoren (Halt gegen
  Abheben, gegen Rollen nur bei einseitiger Anziehung) und Anker an festen Geländepunkten
  (seitlich und an Wänden). Eine gebundene Säule mit zwei Fußankern bleibt nach Seitenstoß
  stehen (3,6° statt 90°). Anker brauchen graue Aktoren. In Zonenläufen (4 Seeds) gibt es
  nur etwa 9 Anker gleichzeitig. Verankerte Körper liegen flach, keine Stängel.
  Population gleich. Haftung lässt sich in Trainingswelten zuschalten (`trainingAdhesion`).
  HIGH_POSITION2 wird damit nicht höher. In HIGH_POSITION3 halten Bindungen die Blöcke
  besser: 1 von 4 Seeds kam über HOLD hinaus, ohne Bindungen 0 von 8. Mit 120-s-Generationen
  wird HIGH_POSITION2 in beiden Varianten höher (293 / 251 px statt ~150), aber als größere
  Haufen; mit Bindungen eher niedriger. `BODENHALT.md`.

- Langlauf 45 min Zonen ± Jahreszeiten: ohne Jahreszeiten stabile ~420 Zellen, 001- und
  teils 100-Zone besiedelt, Aas mit 100/010/001 (nie 000). Mit 25 % Winterlicht sterben
  alle Fremdzonen; Drossel driftet ohne Jahreszeiteneffekt. Folgelauf mit 50 % Winterlicht:
  ebenso, Fremdzonen im ersten Jahr leer. Fortsetzung einer angepassten Population mit
  50 % Winter: 001-Kolonie überlebt (langlebige Altzellen, Lebensdauer 10 000 s),
  Randkolonie 100 schrumpft, Drossel weiter ohne Jahreszeiteneffekt. Mit Lebensdauer
  2000 s: echte 100-Besiedlung ohne Winter, erste Aasverwertung (001-Aas, Seed 4713),
  Winter 50 % in 1 von 3 Seeds Aussterben; Höhe stagniert (Haufen, keine Stängel).
  `ZONEN_JAHRESZEITEN_ERGEBNISSE.md`.

- Neuronale Enzymdrossel: angehängter Ausgang 133, Aktivität = clamp(2 × Ausgang, 0, 1),
  skaliert den Enzymdurchsatz. Alte Netze: unverbundener Ausgang = exakt volle Aktivität,
  alte Ausgänge bitgenau unverändert. Jetzt 316/134. Siehe `ENERGIEUMWANDLUNG.md`.

- WORLD2: offene Welt, 25 600 px breit (4 × NONE), NONE-Nischen viermal je um eine Zone
  versetzt, Spektralzonen mit fester Breite 1600 px wiederholt; schaltet Zonen, Haftung,
  Bindungen, Geburtsbindung, Bodenfeld und Anker ein. Etwa 1400 lebende Zellen nach 2 min,
  Studie rechnet mit etwa halber Echtzeit. `WELT2.md`.
- Spektralzonen der Sonne (GUI-Schalter): 000 | 001 | 010 | 100 mit schmalen linearen
  Mischstreifen, Form am Auftreffpunkt; Zonenbreite fest einstellbar (wiederholt). Zelldialog zeigt kleine Vorräte sichtbar an.
  Noch kein Langlauf zur Aas-/Nischenbildung. Siehe `SPEKTRALZONEN.md`.
- Jahreszeiten (GUI: Jahreslänge 0–60 min, Winterlicht 0–100 %, Standard aus) und
  angebotene Sonnenenergie als gelbe Kurve im Populationsdiagramm; Historie jetzt in
  Simulationszeit (5 s × 300 = 25 min). 204 gezielte Tests. Siehe `JAHRESZEITEN.md`.

- Evolutionäre Sackgasse der 001-Gehirnenergie behoben: angeborene basale Notatmung
  000 → 001 (nur Bedarfslücke, 40 %, max. 0,003 000/s, Grundumsatz geschützt) plus
  Richtungsumkehr als Enzymmutation. Vererbte Enzyme bleiben doppelt so effizient.
  146 gezielte Tests. Kurzer Default-NONE-Vergleich (Seed 4711, 120 s): ohne Notatmung
  441/543 Gehirne pausiert, mit Notatmung 88/1669. 600-s-Vergleich: Notatmung hält
  Pausen bei 6–10 %, aber der Enzymanteil 000→001 steigt dabei nicht (~2–4 %); ohne
  Notatmung erholt sich die Welt nur langsam über Umkehrmutanten (Pausen 87 % → 27 %).
  Details: `FUNKTIONALE_ENERGIE.md`.

- Performance-Regression der neuen Gehirnfinanzierung behoben: unabhängige
  Finanzierungsprüfungen und Eingabeerzeugung parallel, unveränderte Phasenbarrieren.
  Lokaler Vergleich mit 2000 Zellen: 59 → 35 ms/Schritt, identische Zustandsprüfsumme
  über 180 Schritte; 139 gezielte Tests erfolgreich. Der konkrete gemeldete 2500er-Lauf
  ist noch nicht nachgemessen. Siehe `PERFORMANCE_REGRESSION_2026_10_17.md`.

- Physik-, Vererbungs-, Netzsteuerungs-, Training- und Snapshotkorrekturen sind aktiv.
- Sensor-/Aktor-Wechselwirkungen bitgleich beschleunigt (Blocker per x-Index, Nachbarzellen
  pro Aktor ausgeschlossen): WORLD2 9,1 → 1,7 ms/Schritt, alle Phasen 29,0 → 15,4 ms
  gegenüber 8450ca4 (`PERFORMANCE.md`, Abschnitt 24. Oktober). Energietransfer bitgleich
  parallelisiert (Formen und Ports): 3,6 → 1,6 ms/Schritt. Integration bitgleich entlastet
  (Kollisions-Vortest, Bindungsupdate ohne Identity-Set): pro Zelle etwa 20 % schneller.
  Gehirnfinanzierung mit gestempeltem Zensus für Netze ohne Module: 1,4 → 1,0 ms/Schritt.
  Regel: Getter-Arrays von Netz/Layer/Neuronen nicht an Ort und Stelle beschreiben; dadurch prüfen
  kompilierte Gehirne nur Referenzen und Stempel (BRAINS 2,5 ms/Schritt). WORLD2 gesamt ~10 ms/Schritt.
- Schwerkraft, Reibung und Drehreibung per Regler im Umweltpanel, mit den Settings gespeichert.
- Render-Thread wartet nicht mehr auf die Rechnung: Snapshots erzeugt der Rechen-Thread auf Anforderung,
  faire Simulationssperre, keine Pause nach jedem Schritt, minimiert wird nicht gezeichnet (`PERFORMANCE.md`).
- Bindungsupdate in zwei Phasen (eigene Aktoren parallel, Paare seriell): gelöste Aktoren bilden erst
  im nächsten Schritt neu, verankerte Aktoren nehmen keinen Partner. INTEGRATION ~1,4 ms/Schritt.
- Kompilierte Vorwärtsrechnung für Netze aus skalaren Neuronen (`NETZKOMPILIERUNG.md`):
  bitgleiche Ausgaben, Gedächtnis und Arbeit; WORLD2-Gehirnphase 9,6 → 4,5 ms/Schritt.
  Modulnetze nutzen weiter den Ausführungsplan. Backpropagation existiert nur für Tests.
- DNA-erhaltender Ausführungsplan aktiv; bisherige Energieabrechnung bleibt erhalten.
  Siehe `NETZAUSFUEHRUNG.md`: frühere Messung vor der Achtenergie-Erweiterung mit
  42,1 Mio. bitgenau gleichen Ausgaben und isoliert ca. 1,20× schnellerer Ausführung.
- GUI unten: Environment (Sonnenrate/-energieform), Cell (Gehirnkosten/Lebensdauer).
- Neue Default-Gründer (u.a. NONE, HIGH_ENERGY, CARRION_RAIN): 48 versteckte
  Neuronen, davon 24 gemeinsam und 3 × 8 für Zustände. Schnittstelle weiterhin
  316 Eingänge / 133 Ausgänge. Vorher 590 versteckte Neuronen. Bestehende/importierte
  DNA und explizit parametrierte Versuchskonstruktoren werden nicht verkleinert.
  71 gezielte Tests zum kompakten Default und betroffenen Netz-/Trainingspfaden bestehen.
- Acht unabhängige Energieformen mit RGB-Codes 000..111, jeweils [0,1]. Alte Primär-
  und Sekundärwerte bleiben erhalten; sechs neue Vorräte beginnen leer. Siehe `MEHRERE_ENERGIEN.md`.
- Eigene und benachbarte Vorräte sind vollständig sensorisch zugänglich. Für zwölf
  Aktoren jetzt 316 Eingänge / unverändert 133 Ausgänge. Neue Eingänge angehängt;
  Migration von 213/304 erhält alte Indizes, Ausgaben und bestehendes Gedächtnis.
- Überreste im NONE-Modus: normale Zellen, passive Physik, kein neuronales Denken,
  keine Sonnenaufnahme/Teilung; normale Absorption leert Restvorräte bis zur Entfernung.
  Trainingsepisoden erzeugen standardmäßig keine Überreste. Siehe `UEBERRESTE.md`.
- GUI zeigt alle Energien in Zelldetails, Überreste mit Kreuz und getrenntem Zähler.
- Achtenergie-/Überrestestand: 222 gezielte Tests erfolgreich (`fa1060b`, `367d33f`).
  Aktueller Aasregen-/GUI-Schritt: 70 gezielte Tests erfolgreich, einschließlich
  Trainings-, Import-, Überrest-, Snapshot-, Dialog- und Nebenläufigkeitsprüfungen.
- Feste Verbrauchszuordnung: 000 für Grundumsatz/Tod, 001 für Gehirnspeicher und
  Rechenarbeit. Vorabfinanzierung, Pause ohne Gedächtnisfortschritt und Wiederanlaufen
  durch Aufnahme/Umwandlung aktiv. Neue Gründer teilen dasselbe Budget 80:20 auf;
  alte lebende Zellen werden beim Simulationsimport einmalig konservativ umgestellt.
  HIGH_POSITION3 bleibt ausdrücklich mit kostenlosen Gehirnen abgesichert.
  Details: `FUNKTIONALE_ENERGIE.md`. Größen-/Kraftpreise sind weiterhin offen.
- Der Genotyp
  enthält acht vererbte, mutierbare Umwandlungsregeln, je eine pro „Specialization“
  (Zellzustand 0..7). Aktiv ist nur die zur Zelle passende Regel: maximal 0,1/s mal
  Kapazität, 80 % Ausbeute. Neue Genome sind inaktiv; alte Einzelregeln gelten nach
  Migration unverändert in allen acht Rollen. Aktivität seit Enzymdrossel neuronal regelbar.
  Eigene/Nachbarsensoren sehen die abgeschlossene Umwandlung; Überreste reagieren nicht.
  Siehe `ENERGIEUMWANDLUNG.md` und `FUNKTIONALE_ENERGIE.md`.
- Austauschbefehle wirken weiterhin gemeinsam auf alle Formen, mit getrennten Bilanzen.
- Projektgedächtnis: `AGENTS.md`, `ACTUAL.md`, `TODO.md`, `CONCEPTS.md`, `IDEAS.md`, `DONE.md`.
- Neues Training `CARRION_RAIN`: acht Kammern, keine Sonne/Teilung, normale Überreste
  als begrenzte Futterzufuhr; Überlebenszeit und Aufnahme werden selektiert.
  Kontrollnetz überlebt 120 s, ohne Aufnahme Tod nach 60 s. Siehe `AASREGEN.md`.
- `CARRION_CONVERSION` im New-Dialog bietet ausschließlich Energie 001 als Aas an.
  Zum längeren Überleben sind Absorption und eine ausreichende Umwandlung 001 → 000
  nötig. Feste Gegenprobe besteht; ein langer Evolutionslauf ist noch offen.
- Stoffwechsel-/Szenarioabschluss: 201 gezielte Tests erfolgreich, einschließlich
  acht Ressourcen, 56 Routen, Vererbung, echter Altdatei, Sensoren, GUI und Testwelten.
- Spezialisierungsabhängiger Stoffwechsel: 216 gezielte Tests erfolgreich. Gleiche
  DNA/Zellfarbe kann über normale Energieabgabe eine dreistufige Kette ausführen;
  fehlende Zwischenrolle unterbricht sie. Bilanz, Listenreihenfolge, periodischer Rand
  und echte Einzel-Enzym-Altdatei geprüft. Noch kein Evolutionsnachweis für Mehrzeller.
- Szenariowechsel im Berechnungsdienst ersetzt Welt, Raster und Snapshot unter einer
  Sperre und startet pausiert. Oben „New…“ mit erklärter Szenarioauswahl; auch NONE
  und bestehende Trainings-/Testaufbauten sind ohne Codeänderung wählbar.
- „Show Energy“ bleibt ein Primärenergiebalken; nur die Zelldetails zeigen alle acht Formen.
- Verbrauchsmodell-Abschluss: 315 unterschiedliche gezielte Tests erfolgreich,
  einschließlich neu geprüfter Finanzierung/Pause/Erholung, Altimport, Trainings und Physik.
- Vorangegangene Kosten-/Funktionsstudien bewerten frühere Modellstände und ersetzen
  keinen neuen Ökologietest. Siehe `FUNKTIONS_ERGEBNISSE.md` und `NONE_LANGZEIT_ERGEBNISSE.md`.
