# Mechanische Gewebeproben

## Fragestellung und Ergebnis

Die Untersuchung prüft, welche Funktionen die vorhandene Physik durch Kugelpackungen
und vorgeschriebene Gehirnausgaben erlaubt. Entwicklung, DNA, Mutation und Selektion
sind nicht Gegenstand dieser Versuche. Die Produktionsphysik wurde dafür nicht geändert.

**Mehrschichtige Packungen erlauben tragfähige und rückstellende Verbände.** Zwei und
drei Schichten können bei gleicher Länge deutlich steifer sein als eine einzelne Reihe.
Eine einzelne Reihe kann Zug übertragen und gleichzeitig seitlich ausweichen. Ein
Verband kann durch berechnete Größenänderungen wiederholt gegen eine Gegenlast arbeiten.
Damit sind mehrere mechanische Grundfunktionen bereits vorhanden.

Eine schmale Brücke zwischen dicken Endstücken hält zusammen und bleibt beweglich,
hat in diesem Aufbau aber keine verlässliche Ruhelage. Ein komplettes Organmodell,
ein abgedichteter Hautverband oder die gemeinsame Funktion mehrerer solcher Bauteile
wurde damit noch nicht nachgewiesen.

## Versuchsaufbau

- Kugelradius `R = 12`, fünf oder neun Spalten, eine bis drei Schichten.
- Dreieckige Packung: horizontal `2R`, vertikal `sqrt(3) R`; jede zweite Reihe um `R` versetzt.
  Jede Reihe hat dieselbe Spaltenzahl. Bei neun Spalten sind es 9, 18 oder 27 Zellen.
- Die Brücke besitzt neun Spalten und drei Reihen, in den drei mittleren Spalten
  bleibt nur die mittlere Reihe erhalten: 21 Zellen, davon je neun in den Endstücken.
- Alle Zellen besitzen die vorhandenen festen Testgehirne, anziehende grüne Aktoren,
  keine Teilungsbereitschaft und keinen Energietransfer. Nur im aktiven Versuch wird
  die Größen-Ausgabe zusätzlich als Funktion der Simulationszeit berechnet.
- Profil: Anziehung 90, Kontakt 125, Viskosität 0,8, Rotationsdämpfung 3/s,
  Feldglättung 0,25. Diese Werte entsprechen `MechanicsPreset.BALANCED`.
- Vollständiges `Environment.update()`, räumliches Gitter, keine zusätzlichen Bindungen,
  Federn, festgehaltenen Positionen oder nachträglichen Formkorrekturen.
- Sonnenenergie aus; Gravitation durch eine ausdrücklich angelegte Gegenkraft kompensiert.
  Die Probe ist frei schwebend. Ihre Eignung als aufrechter Organismus unter Schwerkraft
  ist eine andere Frage; dafür gibt es weiterhin die Schneemannversuche.
- Große Welt 14000 × 8000; ein Prüfwert verhindert unbemerktes Erreichen der periodischen Ränder.
  Auch gebrochene Proben dürfen sich auseinanderbewegen.

### Lasten und Zeiten

Passive Versuche: vier Sekunden Einlauf, zwei Sekunden Last, sechs Sekunden Entlastung.
Die Referenz wird nach dem Einlauf aufgenommen. Laststufen: 0, 1, 6, 30, 150.
Das sind Modell-Kraftwerte, keine Newton.

| Probe | Aufgebrachte Last |
| --- | --- |
| Biegung | Gesamtkraft F auf die mittlere Spalte, ausgleichende Gegenkräfte auf die Endspalten |
| Zug | Je F nach außen auf die beiden Endspalten |
| Druck | Je F nach innen auf die beiden Endspalten; zusätzlich 1 % seitliche Biegelast |
| Aktiv | Zyklische Größensteuerung bei Druck-Gegenlast F = 0, 1, 6 oder 30, ebenfalls mit 1 % seitlicher Störung |

Kräfte werden innerhalb einer Spalte gleichmäßig verteilt. Sie haben insgesamt weder
resultierende Kraft noch Drehmoment. Zug und Druck folgen der aktuellen Verbindungslinie
der Endspalten; die Biegelast steht senkrecht dazu. Ihre Gegenkräfte berücksichtigen die
aktuelle Lage der mittleren Spalte. Somit bleibt die Gesamtlast beim Hinzufügen von
Schichten vergleichbar; der Materialeinsatz steigt dabei selbstverständlich.

Die seitliche Störung ist wesentlich: Eine perfekt gerade einzelne Reihe blieb bei
exakt symmetrischem Druck zunächst gerade. Mit der Störung zeigt sie klar ihr Einknicken.

Aktiv: Nach vier Sekunden Einlauf folgen drei Perioden zu je drei Sekunden. Der Sollradius
aller Zellen ist `12 + 2 sin²(pi (t - 4) / 3)`. Danach folgen sechs Sekunden Entlastung
bei Sollradius 12. Die tatsächliche Größe durchläuft die reguläre Größenverzögerung und
Begrenzung. Das ist ein vorgeschriebener dehnender Aktuator, noch kein biologischer Muskel
oder Regelkreis. Die bestehende Anziehungsmodulation wurde separat in `MechanicsRegressionTest`
als reversibler Aktuator geprüft.

## Messwerte

Die CSV-Dateien enthalten **238 Weltläufe**: jeweils 119 bei 60 und 120 Hz.
Die folgenden Beispiele stammen aus den 60-Hz-Läufen, bei neun Spalten.

### Geometrie bestimmt Biegesteifigkeit

Gesamtlast F = 6:

| Schichten | Maximale Mittendurchbiegung / R | Verbleibende Formabweichung / R |
| --- | ---: | ---: |
| 1 | 1,73375 | 0,57499 |
| 2 | 0,11822 | 0,0000138 |
| 3 | 0,03460 | 0,0000116 |

Alle ursprünglichen Nachbarschaften bleiben während dieser Versuche in Reichweite.
Die dreischichtige Packung biegt sich ungefähr 50-mal weniger als die einzelne Reihe,
die zweischichtige ungefähr 15-mal weniger. Die dickeren Proben kehren praktisch zur
Referenzform zurück. Die einzelne Reihe behält eine andere Form.

Die schmale Brücke muss mit ihrer unbelasteten Kontrolle verglichen werden: Sie driftet
auch ohne Last. Ein Großteil ihrer Formänderung ist deshalb keine reine Lastantwort.
Ihre Endstücke bleiben dennoch nahezu formstabil. Zusammenhalt und Beweglichkeit sind
nachgewiesen, ein stabiler Gelenkwinkel oder eine definierte elastische Rückstellung nicht.

### Belastbarkeit ist endlich

Bei Zug F = 150 zerfallen die ein- und zweischichtigen Proben: Die größte verbleibende
Komponente enthält zeitweise nur 7 von 9 beziehungsweise 14 von 18 Zellen. Die dreischichtige
Probe bleibt vollständig zusammen und erreicht maximal etwa 1,95 % Längendehnung.
Nach Entlastung beträgt ihre Formabweichung weniger als 0,000012 R.

Bei Druck F = 6 und seitlicher Störung erreicht die einzelne Reihe 1,30645 R Durchbiegung,
die zweischichtige 0,00122 R, die dreischichtige 0,00035 R. Auch bei F = 150 bleibt die
dreischichtige Probe in diesem Versuch unter 0,011 R. Unter hoher **Biegelast** F = 150
ordnen sich dagegen auch mehrschichtige Proben dauerhaft um. Ein zusammenhängender
Verband ist daher nicht automatisch ein formstabiler Verband.

### Wiederholte aktive Bewegung unter Last

Bei Gegenlast F = 6:

| Schichten | Längenhub Zyklus 1 | Zyklus 2 | Zyklus 3 | Formabweichung nach Entlastung / R |
| --- | ---: | ---: | ---: | ---: |
| 1 | 16,526 % | 16,039 % | 15,443 % | 0,64437 |
| 2 | 16,498 % | 16,675 % | 16,675 % | 0,0000139 |
| 3 | 16,487 % | 16,644 % | 16,644 % | 0,0000115 |

Alle bleiben zusammen. Die einzelne Reihe weicht seitlich aus und verliert ihre Ausgangsform.
Die dickeren Verbände behalten reproduzierbare Hübe und kehren zur Ausgangsform zurück.
Ein größerer Querschnitt verhindert aktive Bewegung also nicht.

## Bedeutung der Metriken und Grenzen

- `peak_shape_R`, `residual_shape_R`: RMS-Formabweichung nach optimaler gemeinsamer
  Verschiebung und Drehung, durch R geteilt. Die Endstücke werden zusätzlich jeweils
  separat ausgewertet. Eigenrotation einzelner Zellen ist dabei keine Formabweichung.
- `peak_bend_R`: größter Betrag der Änderung des senkrechten Abstands der mittleren
  Spalte von der Endverbindung. Maximum umfasst Belastung und Entlastung.
- `peak_strain`: größte absolute Änderung des Endabstands relativ zur Referenzlänge.
- `min_links`: kleinster Anteil der Referenznachbarschaften, die in geometrischer
  Interaktionsreichweite bleiben. Das sind keine gespeicherten Bindungen und kein Beweis
  einer in jedem Moment tatsächlich anziehenden Aktorpaarung.
- `min_component`: kleinster Anteil der größten geometrisch zusammenhängenden Komponente.
- `residual_speed`: mittlere relative RMS-Geschwindigkeit in der letzten Sekunde;
  gemeinsame Translation wird abgezogen, gemeinsame Rotation nicht.
- `stroke1..3`: maximaler minus minimaler relativer Endabstand innerhalb der jeweiligen Periode.
- `peak_work`: maximaler positiver Wert `F * (Endabstand - Referenzabstand)` im aktiven
  Versuch. Er zeigt Expansion gegen die axiale Gegenlast, ist aber weder Nettozyklusarbeit
  noch Energieeffizienz oder ein Wert in Joule. Die kleine seitliche Prüflast ist nicht enthalten.

Endliche Beobachtungszeit und ausgewählte Geometrien liefern keine allgemeine Stabilitätsgarantie.
Insbesondere die Brücke erreicht innerhalb des Beobachtungsfensters keine ruhende Referenz.
Die Routineprüfungen vergleichen die intakte dreischichtige Probe zusätzlich zwischen
60 und 120 Hz: Durchbiegung und Restform innerhalb 0,001 R, dritter aktiver Hub innerhalb
0,2 Prozentpunkten. Bruch- und Umordnungswege dürfen sich bei anderem Zeitschritt unterscheiden.
Die CSV-Dateien enthalten auch diese Fälle und die unbelasteten Kontrollen.
Konkret beträgt bei F = 6 die Durchbiegung des dreischichtigen Balkens 0,034599 R
bei 60 Hz und 0,034672 R bei 120 Hz (etwa 0,21 % Unterschied). Sein dritter aktiver
Hub beträgt 16,6439 % beziehungsweise 16,6556 % (0,0117 Prozentpunkte Unterschied).
Auch die unbelastete Brücke driftet bei beiden Zeitschritten: Restformabweichung
0,15335 R beziehungsweise 0,14092 R. Die Einschränkung verschwindet durch Halbieren
des Zeitschritts somit nicht.

Für die nächste mechanische Untersuchung wären Kombinationen der nachgewiesenen Bauteile
interessant: tragender mehrschichtiger Balken, schmaler Zugverband und aktive Elemente in
einem gemeinsamen Aufbau. Die Evolution solcher Geometrien ist eine getrennte Aufgabe.
Für starre Teile sind neue punktförmige Gelenkregeln nach diesen Ergebnissen jedenfalls
keine Voraussetzung.

## Reproduzieren

```powershell
mvn test '-Dtest=TissueMechanicsTest'
mvn test '-Dtest=TissueMechanicsSweepTest' '-Dtissue.sweep=true' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=1'
mvn test '-Dtest=TissueMechanicsSweepTest' '-Dtissue.sweep=true' '-Dtissue.dt=0.008333333333333333' '-Dtissue.output=target/tissue-mechanics-120' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=1'
```

Der gemeinsame gezielte Lauf aus [TESTWELTEN.md](TESTWELTEN.md) bestand mit 274 Tests
ohne Fehler, hier mit `-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=1`.
Die vollständigen evolutionären Langzeitexperimente wurden nicht gestartet.

- [60-Hz-Messwerte](docs/mechanics/tissue-60.csv), [120-Hz-Messwerte](docs/mechanics/tissue-120.csv).
- [Labor](src/test/java/de/lifecircles/service/TissueMechanicsLab.java),
  [zwölf Regressionen](src/test/java/de/lifecircles/service/TissueMechanicsTest.java),
  [explizite Studie](src/test/java/de/lifecircles/service/TissueMechanicsSweepTest.java).


## Ergaenzung: aufrechte Verbaende

Die [Standproben](STANDPROBEN.md) untersuchen 108 aufrechte Packungen unter voller
Schwerkraft. Sie ergaenzen die hier beschriebenen frei schwebenden Materialproben
um Tragfaehigkeit auf einem Boden und den Einfluss der Zellgroesse.
