# Gehirnkosten unter NONE

Abgeschlossene Messungen und Bewertung: [NONE_ERGEBNISSE.md](NONE_ERGEBNISSE.md).

Gepaarte Fortsetzungen des gespeicherten HIGH_POSITION3-Zellstands: drei Kostenvarianten,
je drei Zufallsfolgen, zunächst 36.000 Schritte bei 60 Hz (600 Simulationssekunden,
fünf Tag-/Nachtzyklen). Preise: Struktur/Berechnung 1/1, 1/0,1 und 100/0,1.

## Aufbau

- Gleiche vollständige Startpopulation je Versuch; Koordinaten, Energie, Alter und
  Genome bleiben erhalten. Datei wird ausschließlich gelesen.
- Welt 11.200 × 1.200 wie beim gespeicherten HIGH_POSITION3-Stand, mit den Hindernissen
  der NONE-Strategie. Die ehemalige Trainingseinteilung und Trainingsauswahl entfallen.
- Sonne und natürliche Teilung einschließlich Mutation laufen unverändert. Kein
  Energienachfüllen, kein künstlicher Fitnesswert, keine Wiederbesiedlung nach Tod.
- Sonnenzufall, Netzmutation und Geburtsmerkmale verwenden getrennte Seeds.
  Gleiche Seeds erzeugen dieselben Sonnenstrahlen. Nach unterschiedlichen Teilungen
  werden Mutationszahlen nicht mehr identischen Geburten zugeordnet.
- Kosten werden vor Tod/Teilung im normalen Stoffwechselschritt abgezogen. Keine
  nachträgliche Erstattung; Energiebegrenzung und Sterben bleiben wirksam.
- Die Reihenfolge der drei Varianten rotiert zwischen den Seeds. Messungen ohne
  Darstellung/Snapshots, einschließlich Raster und Phasenprofilierung. Erste Schritte
  enthalten Aufwärmung; Laufzeitvergleiche sind beschreibend.
- Bei 2.000 Zellen wird der ganze Versuch als `cap_stop` beendet. Keine künstliche
  Begrenzung der Teilung. Vorzeitig gestoppte Läufe sind zensiert und nicht mit
  vollständigen 600-Sekunden-Endständen gleichzusetzen.

Erfasst werden Zellzahl, Geburten, Todesfälle, erhaltene Gründerlinien, Energie,
Synapsen, ausgeführte Synapsen, äußere Neuronen, Generation, Zellzustände,
Sensorkontakte, Kraftausgaben und Phasenlaufzeiten. `brain_cost_requested` ist die
angeforderte Gebühr vor Begrenzung bei Energie null. Kontakte und Zustandsvielfalt
beweisen keine funktionale Kooperation. Gründerlinien sind einzelne Startzellen,
keine unterschiedlichen Genotypen. Äußere Neuronenzahlen schließen Modulinneres aus.

## Wiederholen

```powershell
mvn test '-Dtest=NoneEnergyStudyTest' '-Dnone.energy.file=C:\Pfad\stand.cells' '-Dnone.energy.output=experiments/none-energy' '-Dnone.energy.steps=36000' '-Dnone.energy.seeds=3' '-Dnone.energy.interval=600' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```

Weitere Parameter: `none.energy.seed_offset`, `none.energy.width`, `none.energy.cap`.
`none.energy.founders_per_section=1` wählt deterministisch den ersten Vertreter
jedes früheren 1400-Pixel-Trainingsbereichs (hier acht Gründer). Höhere Zahlen wählen
gleichmäßig über die Dateireihenfolge verteilte Vertreter. Null lädt alle Zellen.
`none.energy.floor=true` verwendet ausschließlich einen Boden, setzt die Gründer
gleichmäßig nebeneinander darüber und ihre Geschwindigkeiten auf null.
`none.energy.height` ergänzt die einstellbare Breite. Diese Anordnungsänderung wird
in den Einstellungen erfasst; sie ist keine Fortsetzung der ursprünglichen Geometrie.

Nach dem frühen Wachstum der großen Varianten wird zusätzlich eine 600 × 400 große
periodische Bodenwelt mit acht Gründern und denselben drei Preisen/Seeds geprüft.
Die Strahlendichte und Energie je Strahl bleiben gleich; die gesamte verfügbare
Sonnenenergie ist wegen der kleineren Breite geringer. Das erleichtert den vollständigen
Ressourcenvergleich, erlaubt aber keine direkte Übertragung auf große Kolonien.

Ordner pro Versuch enthalten `start.cells` und `end.cells`; Ergebnis- und Verlaufs-CSV,
Phasenzeiten und Einstellungen liegen darüber. Ergebnisse in einen neuen Ordner
schreiben; erneutes Ausführen überschreibt dessen CSV-Dateien.
Die Zell-Dateien speichern keine Weltgeometrie, Sonnenphase oder Zufallszustände.
Für visuelles Weiterführen muss die passende Weltgeometrie eingerichtet werden;
ein solches Laden startet eine neue Fortsetzung mit neuen Zufallsfolgen.

Routinekontrollen: `NoneEnergyControlsTest` prüft alle drei Preise und kostenfreie
Gehirne bis zum Tod sowie den reproduzierbaren Ablauf einschließlich Geburt und
Mutation. Mit den betroffenen Regressionen bestanden 88 Prüfungen.
Nach Ergänzung der Gründer-Auswahl bestanden die sieben neuen Kontrollen gemeinsam
mit der bisherigen gezielten Regression: **391 Prüfungen, keine Fehler**.

Die drei Serien laufen teilweise gleichzeitig in separaten JVMs. Biologische
Zustände und Zufallsfolgen sind unabhängig; Laufzeitmessungen konkurrieren jedoch
um CPU und Speicherbandbreite. Daraus keine belastbare Beschleunigung ableiten.
Die Laufzeiten enthalten außerdem die Buchhaltung der Versuche. Nach dem Laden
steht der transiente Ausführungszähler auf null; die Verlaufsgrafik beginnt für
diese Messgröße erst nach tatsächlich ausgeführten Entscheidungen.

## Aussagegrenzen

Diese Versuche prüfen den Übergang eines auf Statik trainierten Bestands in eine
Ressourcenwelt. Sie sind keine Wiederholung der HIGH_POSITION3-Auswahl und kein
Nachweis langfristiger Evolution. Drei Seeds teilen denselben genetischen Startpool;
für allgemeine Aussagen braucht es weitere Bestände und längere Läufe. Falls die
Startpopulation ohne Energie-Training rasch ausstirbt, lässt sich daran keine
optimale langfristige Gehirngebühr bestimmen.
