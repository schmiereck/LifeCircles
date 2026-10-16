# Funktionsvergleich: Ergebnisse vom 16.10.2026

## Abschluss und Datenprüfung

Alle 144 Vergleichsläufe und 2880 synthetischen Signalproben sind abgeschlossen.
Kein Vergleich wurde durch Aussterben oder das Populationslimit vorzeitig beendet.
Archivierte CSV-Dateien und komprimierte Verläufe stimmen mit den Rohdaten überein;
die Prüfsummen aller 18 ausgewählten Modelldateien wurden kontrolliert.
Die automatische Nachbearbeitung meldet Abschluss um 17:35:45 UTC.

Messdaten: [Archiv](docs/function-comparison/stable/README.md),
[Vergleichsgrafik](docs/function-comparison/stable/function-gains.png).
Versuchsaufbau und Grenzen: [FUNKTIONSVERGLEICH.md](FUNKTIONSVERGLEICH.md).

## Laufende Steuerung gegenüber festen Befehlen

Verglichen wurden je neun repräsentative Genome aus frühen und späten Speicherständen
der langen NONE-Kostenversuche. Pro Genom liefen zwei Sonnenfolgen mit Herkunftsgebühren
und zwei ohne Gehirngebühren, jeweils normal und mit zustandsabhängig eingefrorenen
Ausgaben. Das Gehirn rechnet auch im eingefrorenen Modus weiter. Innerhalb eines Paares
werden somit Steuerungswirkungen verglichen, keine Einsparung durch Abschalten des Netzes.

Positive Werte bedeuten mehr integrierte lebende Zellen über dieselbe Versuchsdauer
(Zellsekunden). Pro Genom wird das Verhältnis aus den Summen beider Sonnenfolgen berechnet.

| Herkunftsstand | Gebühren im Vergleich | Mittlerer Vorteil | Median | Bereich über neun Genome |
|---|---|---:|---:|---:|
| Früh, 1000 s | Herkunftspreise | 0,000 % | 0,000 % | 0,000 bis 0,000 % |
| Früh, 1000 s | keine | 0,000 % | 0,000 % | 0,000 bis 0,000 % |
| Spät, 10000 s | Herkunftspreise | +0,302 % | 0,000 % | −3,062 bis +3,271 % |
| Spät, 10000 s | keine | +0,039 % | 0,000 % | −3,528 bis +1,839 % |

Bei den frühen Genomen sind Zellsekunden und Geburten in allen Paaren identisch.
Bei den späten bleiben fünf von neun Genomen hinsichtlich Zellsekunden exakt gleich;
vier zeigen Unterschiede. Eine breite Überlegenheit dynamischer Steuerung ist damit
nicht belegt. Die kleine Stichprobe erlaubt keine belastbare Rangfolge der Gehirngebühren.

Ein Kandidat ist Modell 3: Herkunftspreise 1/1, Evolutionsseed 0, später Speicherstand.
Es erreicht mit Herkunftspreisen +3,271 % und ohne Gebühren +1,839 % Zellsekunden;
alle vier einzelnen Sonnen-/Gebührenpaare sind positiv. Die Einzelvorteile reichen
allerdings von +0,110 % bis +6,636 %. Das rechtfertigt gezielte Folgeprüfungen,
belegt aber keine allgemeine Überlegenheit. Andere Genome wechseln je nach Sonnenfolge
oder Gebührenvariante das Vorzeichen. Geburten und Zellsekunden bewegen sich teilweise
in entgegengesetzte Richtungen; viele Teilungen allein sind kein ausreichendes Erfolgskriterium.

Alle im eingefrorenen Zustandsgraphen erreichbaren Geburtszustände waren in der
Kalibrierung vertreten. Unkalibrierte Ersatzmuster erklären diese Unterschiede daher nicht.

## Was steuern die Netze noch?

Die Signalproben finden in allen 18 Genomen mindestens kleine Änderungen angewendeter
Ausgaben oberhalb der numerischen Schwelle 1e-9. Nur vier Genome (1, 5, 15, 17) erreichen
in mindestens einer Probe eine Änderung über 0,01. Diese zweite Schwelle beschreibt
eine Größenordnung; sie ist keine validierte biologische Relevanzgrenze.

Zu den jeweils stärksten Änderungen gehören Aktorfarben, Energieabgabe,
Teilungsenergieanteile, Teilungswünsche und ein Kinderzustandsbit. Die archivierte
Featureliste nennt pro Probe nur die größte Änderung, nicht alle reagierenden Ausgänge.
Unter den getesteten Eingabevarianten verändert sich bei keinem Genom der angeforderte
Teilungsaktor oder dessen Kinderzustand. Das veränderte Kinderzustandsbit von Modell 15
betrifft somit nicht die angeforderte Teilungswahl in diesen Proben.

Die Proben verändern synthetische Eingaben, teilweise gleichzeitig an allen zwölf
Sensoren. Sie messen Ausgabeeinfluss, keine Fitness und keine vollständige Abdeckung
aller Situationen oder längerer Gedächtnisverläufe. Identische Zellsekunden beweisen
umgekehrt nicht, dass ein Netz auf keine Eingabe reagiert.

## Einordnung und nächste Schritte

Die langen Kostenversuche ließen große Netze bestehen. Dieser Vergleich liefert wenig
Evidenz dafür, dass ihre fortlaufende Eingabeverarbeitung in dieser einfachen Welt
generell einen nennenswerten Vorteil bringt. Meine Arbeitshypothese ist, dass hier ein
erheblicher Teil der Netzgröße ohne entsprechenden Steuerungsnutzen mitgetragen wird.
Gemessen wurde allerdings der Nutzen variabler Ausgaben, nicht die kleinste Netzgröße,
die dieselben festen oder zustandsabhängigen Befehle erzeugen könnte.

Ich würde die Standardpreise anhand dieser Daten noch nicht pauschal erhöhen.
Das könnte statische Steuerungen begünstigen, ohne zu zeigen, wie nützliche kleine
Netze für wechselnde Aufgaben erhalten bleiben. Weitere lange Wiederholungen derselben
einfachen Welt haben vorerst weniger Priorität. Sinnvolle nächste Schritte wären:

1. Modell 3 gezielt prüfen: weitere Sonnenfolgen, längere Bewertungsdauer und einzelne
   Ausgänge einfrieren, um die Ursache seines Vorteils einzugrenzen.
2. Für Laufzeitgewinne exakte Vereinfachungen untersuchen: Welche Neuronen und
   Verbindungen beeinflussen Ausgaben oder künftige Netzsteuerung? Rekurrenz,
   Aktivierung und verschachtelte Netze berücksichtigen und Gleichheit prüfen,
   bevor etwas entfernt wird. Kleine Gewichte allein sind kein sicheres Kriterium.
3. Danach Kostenvarianten in einer wechselnden Ressourcenwelt und bei den logischen/
   Gedächtnisaufgaben gemeinsam prüfen. Erwerb, Erhalt, Netzgröße und tatsächlich
   eingesparte Rechenzeit getrennt erfassen; statische Muster weiterhin vergleichen.

Diese Schritte sind Empfehlungen. Es wurden keine weiteren langen Läufe gestartet
und keine Standard-Gehirnpreise geändert.

## Gültiger Physikstand

Die Weltvergleiche liefen im bereits gestarteten Prozess mit Startcommit `674ffcc`,
einschließlich der früheren Energieübertragung. Die spätere Korrektur aus `a932b5a`
ist darin nicht enthalten. Die nachgelagerten Gehirn-Signalproben rufen keine
Welt-Energietransfers auf. Die Befunde bewerten somit keine Population unter den
neuen Transferregeln. Künftige NONE-Läufe mit dem aktuellen Stand sind als neue
Versuchsreihe zu führen.
