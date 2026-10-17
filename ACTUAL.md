# Aktueller Projektstand

Aktualisiert: 17.10.2026. Branch `master`.

- Physik-, Vererbungs-, Netzsteuerungs-, Training- und Snapshotkorrekturen sind aktiv.
- DNA-erhaltender Ausführungsplan aktiv; bisherige Energieabrechnung bleibt erhalten.
  Siehe `NETZAUSFUEHRUNG.md`: frühere Messung vor der Achtenergie-Erweiterung mit
  42,1 Mio. bitgenau gleichen Ausgaben und isoliert ca. 1,20× schnellerer Ausführung.
- GUI unten: Environment (Sonnenrate/-energieform), Cell (Gehirnkosten/Lebensdauer).
- Acht unabhängige Energieformen mit RGB-Codes 000..111, jeweils [0,1]. Alte Primär-
  und Sekundärwerte bleiben erhalten; sechs neue Vorräte beginnen leer. Siehe `MEHRERE_ENERGIEN.md`.
- Eigene und benachbarte Vorräte sind vollständig sensorisch zugänglich. Für zwölf
  Aktoren jetzt 316 Eingänge / unverändert 133 Ausgänge. Neue Eingänge angehängt;
  Migration von 213/304 erhält alte Indizes, Ausgaben und bestehendes Gedächtnis.
- Überreste im NONE-Modus: normale Zellen, passive Physik, kein neuronales Denken,
  keine Sonnenaufnahme/Teilung; normale Absorption leert Restvorräte bis zur Entfernung.
  Trainingsepisoden erzeugen standardmäßig keine Überreste. Siehe `UEBERRESTE.md`.
- GUI zeigt alle Energien in Zelldetails, Überreste mit Kreuz und getrenntem Zähler.
- 222 gezielte Tests erfolgreich; aktuelle Codeschritte: `fa1060b`, `367d33f`.
- Energieverbrauch und Überleben verwenden bisher Primärenergie. Weitere Zuordnungen
  und Umwandlungen sind noch nicht festgelegt.
- Austauschbefehle wirken weiterhin gemeinsam auf alle Formen, mit getrennten Bilanzen.
- Projektgedächtnis: `AGENTS.md`, `ACTUAL.md`, `TODO.md`, `CONCEPTS.md`, `IDEAS.md`, `DONE.md`.
- Vorangegangene Kosten-/Funktionsstudien bewerten frühere Modellstände und ersetzen
  keinen neuen Ökologietest. Siehe `FUNKTIONS_ERGEBNISSE.md` und `NONE_LANGZEIT_ERGEBNISSE.md`.
