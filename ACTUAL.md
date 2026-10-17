# Aktueller Projektstand

Aktualisiert: 17.10.2026. Branch `master`.

- Physik-, Vererbungs-, Netzsteuerungs-, Training- und Snapshotkorrekturen sind aktiv.
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
  Migration unverändert in allen acht Rollen. Keine neuen neuronalen Kanäle.
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
