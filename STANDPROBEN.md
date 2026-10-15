# Standfestigkeit und Energie nach den ersten Evolutionslaeufen

## Was die Physik nachweislich erlaubt

108 kurze Weltlaeufe pruefen aufrechte Packungen unter **voller Schwerkraft**.
Die frueheren Gewebeproben hatten die Schwerkraft kompensiert und beantworteten
diese Frage nicht. Die Produktionsphysik wurde fuer diese Untersuchung nicht geaendert.

Aufbau: 1/3/5 Zellen je Reihe, 4/8/12 Reihen, Radius 12 oder 20. Dreieckige
Packung auf einem ebenen Boden, ohne Seitenwaende, Befestigungen oder gespeicherte
Bindungen. Die oberste Reihe ist um 1 % eines Radius seitlich gestoert. Vorgegebene
Gehirne halten Radius und gruene, maximal anziehende Aktoren konstant. Keine Teilung,
kein Wachstum, kein Energietransfer, keine Sonne. Jede Probe laeuft 30 Simulationssekunden.

Profile: aktuelle Werte (Anziehung 90, Kontakt 125, Viskositaet 0,8, Drehdaempfung
0,8/s, Kontaktzone 0,25), BALANCED (Drehdaempfung 3/s) und steiferer Kontakt
(Kontakt 250, sonst BALANCED). Alle 27 Kombinationen je Radius bei 60 und 120 Hz.

Beispiele mit aktuellen Parametern, 60 Hz:

| Radius | Breite x Reihen | Hoehe nach 30 s / Anfang | Breite nach 30 s / Anfang |
| --- | --- | ---: | ---: |
| 12 | 1 x 8 | 51,9 % | 197,9 % |
| 12 | 3 x 4 | 97,9 % | 99,3 % |
| 12 | 5 x 8 | 96,4 % | 99,2 % |
| 12 | 5 x 12 | 65,5 % | 197,7 % |
| 20 | 3 x 12 | 96,0 % | 105,4 % |
| 20 | 5 x 12 | 95,9 % | 98,1 % |

Die grosse 5 x 12-Probe besteht aus 60 Zellen und bleibt nach dem Setzen praktisch
ruhig. Ihre Ausgangshoehe ist etwa 421, ihre Breite etwa 220 Welteinheiten.
Hoehen-/Breitenverhaeltnisse stimmen bei 60 und 120 Hz bis auf die ausgegebenen
neun Dezimalstellen ueberein. Die schmalere grosse 3 x 12-Probe behaelt zwar ihre
Hoehe, bewegt sich im letzten Messfenster noch mit RMS etwa 0,26 Einheiten/s;
sie ist kein ebenso klarer Ruhezustand.

Die schmalen oder ueberlasteten Proben bleiben geometrisch zusammenhaengend,
verlieren aber ihre Form. Genau dieses Verhalten kann visuell wie Zerfliessen wirken.
Zusammenhang ist keine Standfestigkeit. Bei instabilen Proben unterscheiden sich
Umordnungswege mit dem Zeitschritt teils deutlich; die 12er-Zellen bilden durch
Halbieren des Schritts keinen ploetzlich stabilen hohen Turm.

Eine Erhoehung der Kontaktsteifigkeit auf 250 stabilisiert beispielsweise die
3 x 8-Packung mit Radius 12 (97,9 % Hoehe statt 85,7 %). Sie loest nicht alle
Hoehenprobleme: Die 3 x 12-Packung faellt weiterhin um bzw. ordnet sich um.
Mehr Drehdaempfung beseitigt ebenfalls nicht allgemein mangelnde Tragfaehigkeit.

**Fazit:** Aufrechte tragende Verbaende sind moeglich. Das ist kein Nachweis, dass
beliebig schlanke Tuerme stabil sind oder dass sie waehrend Zellteilung und Wachstum
stabil aufgebaut werden koennen. Die guenstige Anziehungsstaerke, einheitliche Farbe
und Packung waren vorgeschrieben. Die ausgewerteten Evolutionspopulationen liegen
fuer einen direkten Vergleich ihrer Kraftausgaben noch nicht vor.

Die Groessenabhaengigkeit ist im Modell relevant: Aktorkraefte enthalten eine
Radiusquadrat-Skalierung, die Schwerkraft pro Zelle ist dagegen konstant. Grosse
Zellen erhalten dadurch relativ mehr Zusammenhalt. Die Versuche vergleichen kein
Material mit groessenunabhaengigen Eigenschaften.

Rohdaten: [Radius 12, 60 Hz](docs/mechanics/standing-60.csv),
[Radius 12, 120 Hz](docs/mechanics/standing-120.csv),
[Radius 20, 60 Hz](docs/mechanics/standing-radius20-60.csv),
[Radius 20, 120 Hz](docs/mechanics/standing-radius20-120.csv).

Vier regulaere Regressionen sichern einen tragenden und einen zusammenfallenden
Aufbau bei beiden Schrittweiten. Zusammen mit `MechanicsRegressionTest`: 16 Tests erfolgreich.

## Warum HIGH_POSITION2 das gewuenschte Verhalten nicht gezielt belohnt

Codeanalyse, keine gemessene Erklaerung fuer einen bestimmten gespeicherten Lauf:

- Alle 3.000 Schritte, bei 60 Hz also alle 50 Simulationssekunden, werden einzelne
  Zellen nach ihrer momentanen Hoehe und Naehe zur Abschnittsmitte bewertet.
- Es gibt keinen Bewertungswert fuer dauerhafte Hoehe, eine tragende Basis oder
  den Zusammenhalt des gesamten Verbandes. Eine kurzzeitig hochgedrueckte Zelle
  kann gewinnen. Die Hoehennormalisierung mit globalem Min/Max gibt auch einem
  sehr kleinen Hoehenunterschied den vollen relativen Bewertungsbereich.
- Vererbt wird das Genom einzelner Gewinner; der entstandene Zellverband wird
  beim Generationswechsel nicht als gemeinsame Einheit uebernommen. Ein gutes
  Entwicklungsprogramm kann daraus entstehen, eine kooperierende Mischung aus
  mehreren voneinander verschiedenen Genomen wird so nicht gemeinsam bewahrt.
- Natuerliche Teilung mutiert standardmaessig weiter. Das erschwert stabile
  Entwicklungsprogramme mit gleicher DNA, auch wenn das Training gute Gene findet.

Ein passender naechster Trainingsversuch wuerde die anhaltende Hoehe eines
bodenverbundenen Verbandes ueber ein Zeitfenster bewerten, mit einer festen
Hoehenskala statt dem globalen Min/Max. Dabei sollte eine einzelne abgeloeste
Spitzenzelle keinen grossen Vorteil liefern. Der bisherige Modus sollte als
Vergleich erhalten bleiben. Eine belastbare Zuordnung von Verband, Abstammung
und gemeinsamer Fitness ist dafuer eigene Modellarbeit.

## Energiebilanz von NONE

Die mittlere Sonnenintensitaet eines vollstaendigen 120-s-Zyklus ist
`0,4 + 0,6 * 2/pi = 0,782`. Bei Weltbreite 6.400, Strahlrate 10 und Abstand 60
ergeben sich im Mittel folgende **angebotene Obergrenzen**:

| Energie pro Strahl | Energie pro Simulationssekunde, gesamte Welt |
| --- | ---: |
| 0,01 (Standard NONE ohne andere Aenderungen) | 8,34 |
| 0,005 (HIGH_POSITION2 setzt diesen Wert beim Start) | 4,17 |

Blocker, nicht getroffene Zellen und Saettigung bei Energie 1 reduzieren die
tatsaechlich gespeicherte Energie. Abschattung verteilt den Ertrag zusaetzlich
ungleich; eine innere Zelle benoetigt funktionierenden Energietransfer.

Beispiel fuer einen Bedarf: 3.000 gespeicherte und 3.000 ausgefuehrte Verbindungen
bei 60 Entscheidungen/s kosten einschliesslich Grundumsatz etwa 0,00660 Energie/s.
Das ist eine Rechenprobe, kein gemessener Durchschnitt der Population des Nutzers.
Selbst unter idealer vollstaendiger Nutzung tragen 8,34 Energie/s dann nur etwa
1.264 solche Zellen im Erhaltungszustand; bei halber Strahlenergie etwa 632.
Wachstum der Population erzeugt weitere Verbraucher. Wenig Teilung und lange
Lebensdauer bei niedriger Energie sind damit ein plausibles Ressourcen-Gleichgewicht.

Fuer einen kontrollierten Energieversuch zuerst die Strahlrate beispielsweise
von 10 auf 20 erhoehen und Energie pro Strahl konstant lassen. Das erhoeht den
mittleren Ertrag und verteilt ihn auf mehr kleine Ereignisse. Es garantiert keine
Strukturbildung; Population, Geburten, Todesfaelle und Laufzeit muessen mitbeobachtet
werden. Die Teilungsschwelle oder Energieerzeugung bei Teilung sollte dafuer nicht
veraendert werden. Kein Sonnenparameter wurde in dieser Untersuchung umgestellt.

## Wiederholen

```powershell
mvn test '-Dtest=StandingStructureTest,MechanicsRegressionTest'
mvn test '-Dtest=StandingStructureStudyTest' '-Dstanding.study=true' '-Dstanding.radius=12' '-Dstanding.output=target/standing-60.csv' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
mvn test '-Dtest=StandingStructureStudyTest' '-Dstanding.study=true' '-Dstanding.radius=20' '-Dstanding.dt=0.008333333333333333' '-Dstanding.output=target/standing-20-120.csv' '-DargLine=-Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
```
