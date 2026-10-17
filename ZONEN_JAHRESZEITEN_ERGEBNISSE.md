# Langlauf: Spektralzonen mit und ohne Jahreszeiten (17.10.2026)

Modellstand `da25f4f` (Notatmung, Umkehrmutation, Spektralzonen 000|001|010|100,
Jahreszeiten, neuronale Enzymdrossel). Default-NONE-Welt 6400 × 1200, dt 1/60,
Gehirnkosten 1×, Sonnenrate 10, `NeuralNetwork`-Seed 4711, 2700 s = 45 Simulationsminuten,
eine Messung pro Simulationsminute. Studie: `ZoneSeasonStudyTest`.

- **A „seasons“**: Jahr 10 min, Winterlicht 25 % (4,5 Jahre). Laufzeit 414 s.
- **B „noseasons“**: gleiche Zonen, keine Jahreszeiten. Laufzeit 1216 s.

Beide liefen gleichzeitig auf 12 Kernen. **Grenzen:** je ein Lauf; die Läufe sind trotz
gleichem Seed nicht bitgenau reproduzierbar (ein Probelauf hatte nach 60 s 309 statt 272
Zellen), also Momentaufnahmen, kein statistischer Beleg. Lokale Daten (nicht eingecheckt):
`target/zone-season-study/{seasons,noseasons}.csv`, `-summary.txt` und `.cells`
(Endpopulation, in der GUI ladbar).

## Population und Zonen

| | A mit Jahreszeiten | B ohne Jahreszeiten |
|---|---|---|
| Lebende Zellen, Min / Max | 12 / 367 | 227 / 528 |
| Mittel zweite Hälfte | 59 | 422 |
| Zone 000 | einzige dauerhaft besiedelte Zone | 230–355 |
| Zone 001 | nach dem ersten Winter leer (ab ~420 s) | stabil 68–200, meist mit Route 001 → 000 |
| Zone 010 | nach ~120 s leer | nach ~120 s leer, nie wieder besiedelt |
| Zone 100 | nach ~180 s leer | 14–75, 100-Vorrat meist fast voll (0,9–1,0) |

A: Jeder Winter (25 %) drückt die Population auf 12–34 Zellen; die Fremdzonen
überleben schon den ersten Winter nicht. B: 001 wird über die Ein-Mutations-Route
001 → 000 dauerhaft besiedelt; 100 hält eine kleine Population (später 3–5 Zellen
mit 100 → 000, andere vermutlich über den periodischen Mischstreifen zur 000-Zone).
010 grenzt an keine 000-Zone und blieb leer.

## Fremdformen in Zellen und Aas

B, Ende (Summen): lebende Zellen 100 ≈ 174, 001 ≈ 60, 000 ≈ 24; Überreste
100 ≈ 100, 010 ≈ 26, 001 ≈ 13, 111 ≈ 8, **000 = 0**. Anzahl Überreste wächst von
17 auf ~220. A: Überreste 001 ≈ 37, 010 ≈ 24, 100 ≈ 13, 000 = 0; über die zweite
Hälfte praktisch unverändert.

Das Ziel „Fremdformen sammeln sich an und bleiben als Aas zurück“ ist in B erreicht.
Überreste enthalten nie 000 (Tod durch 000-Mangel). Ein messbares Verwerten dieses
Aases durch Aasfresser zeigt sich in 45 Minuten nicht; die Aasvorräte stagnieren oder wachsen.

## Enzymdrossel

Die Drossel wird genutzt, aber ohne erkennbare Regulation: mittlere Aktivität aktiver
Enzyme in der zweiten Hälfte 0,973 (A) bzw. 0,974 (B); gedrosselt (< 0,99) 57 % (A)
bzw. 21 % (B). A: Sommer 0,987 vs. Winter 0,985, kein Jahreszeiteneffekt. Wahrscheinlich
neutrale Drift der Bias-Mutationen, keine gelernte Vorratsstrategie.

## Folgelauf C: Winterlicht 50 % (17.10.2026)

Gleiche Einstellungen wie A, nur Winterlicht 50 %; Laufzeit 377 s. Daten:
`target/zone-season-study/seasons50.*` (lokal).

| | A (25 %) | C (50 %) | B (ohne) |
|---|---|---|---|
| Lebende Zellen, Min / Max | 12 / 367 | 44 / 344 | 227 / 528 |
| Mittel zweite Hälfte | 59 | 99 | 422 |
| Fremdzonen | ab ~420 s leer | ab ~180 s leer, 100 kurz wiederbesiedelt (19 Zellen bei 660 s) | 001 und 100 dauerhaft |
| Aktivität aktiver Enzyme, Sommer / Winter* | 0,983 / 0,978 | 0,927 / 0,932 | — |
| Anteil gedrosselt, zweite Hälfte | 57 % | 91 % | 21 % |

\* gewichtet nach Zahl aktiver Enzyme, Messungen ohne aktives Enzym ausgelassen.

- Auch 50 % Winterlicht reicht nicht: Die Population fällt jeden Winter von etwa
  125–235 auf 50–95, die Fremdzonen sterben schon im ersten Jahr.
- Aas (C, Ende): 010 ≈ 56, 100 ≈ 33, 001 ≈ 27, 000 = 0; seit der Laufmitte unverändert,
  stammt also aus den ersten Jahren und wird nicht gefressen.
- In der 000-Zone tragen lebende Zellen bereits Routen Fremdform → 000
  (010 → 000: 11, 011 → 000: 5, 111 → 000: 4 von 64), aber kaum Fremdvorräte:
  potenzielle Aasfresser, die das vorhandene Aas nicht erreichen oder nicht aufnehmen.
- Die Drossel wird stärker genutzt als in A/B, aber wieder ohne Jahreszeiteneffekt:
  weiterhin Drift, keine Vorratsstrategie.

## Fortsetzungen D/E: angepasste Population unter Winter (17.10.2026)

Start: Endpopulation von B (382 Zellen, 001- und 100-Zone besiedelt), gesichert als
`target/zone-season-study/start-from-noseasons.cells`. Beide Läufe 2700 s, parallel.
**D „cont-seasons50“**: Jahr 10 min ab Hochsommer, Winterlicht 50 % (1143 s Laufzeit).
**E „cont-noseasons“**: Kontrolle ohne Jahreszeiten. Option `-Dzone.study.load` (`55c0ccc`).

| | D mit Winter 50 % | E Kontrolle |
|---|---|---|
| Lebende Zellen, Min / Max | 124 / 519 | 249 / 478 |
| Mittel zweite Hälfte | 244 | 404 |
| Zone 000, Mittel / Min | 168 / 50 | 284 / 169 |
| Zone 001, Mittel / Min | 68 / 63 | 80 / 57 |
| Zone 100, Mittel / Min (Ende) | 8 / 0 (3) | 40 / 13 (15) |
| Lebende 100-Vorräte Start → Ende | 169 → 3,5 | 169 → 97 |
| Aas Ende (größte Formen) | 100 ≈ 17 | 100 ≈ 60, 001 ≈ 18, 011 ≈ 13, 101 ≈ 13 |
| Aktivität aktiver Enzyme / gedrosselt | 0,996 / 7 % | 0,992 / 13 % |

- Die 001-Kolonie übersteht alle Winter nahezu unverändert. Ursache laut gespeicherter
  Endpopulation: Sie besteht großteils aus **langlebigen Altzellen** (Alter bis 5392 s,
  teils Generation 1 = Gründer des ersten Laufs) mit vollem 001 und wenig 000, die
  von 001-Sonnenlicht über 001 → 000 leben und sich kaum teilen. Die Kennzahl
  „18 Zellen mit 001 → 000“ ist in beiden Läufen die gesamte Zeit dieselbe Gruppe.
  Möglich, weil die maximale Lebensdauer 10 000 s beträgt (GUI-Regler „Cell“).
- Die „100-Zone“-Zellen liegen bei x ≈ 6160–6400, also im periodischen Mischstreifen
  zur 000-Zone, nicht im reinen 100-Gebiet. Unter Winter schrumpft diese Randkolonie
  fast vollständig; ihre 100-Vorräte verschwinden.
- Die 000-Zone schwankt stark mit den Jahreszeiten, überlebt aber.
- Drossel: Sommer 0,994 vs. Winter 0,996 – erneut kein Jahreszeiteneffekt. In D wird
  sogar seltener gedrosselt als in E. Keine Vorratsstrategie erkennbar.
- In E entstehen in der 000-Zone viele Umwandlungen in seltene Formen (111 → 110: 66,
  001 → 110: 43, 110 → 111: 32); lebende Zellen halten 011/101/111. Ob das adaptiv
  ist oder verlustreiche Drift kleiner Kapazitäten, ist ungeprüft.

## Läufe mit Lebensdauer 2000 s (17.10.2026)

Hintergrund (Nutzervorgabe): Die Lebensdauer 10 000 s war gewählt, damit entstehende
Stützstrukturen („Stängel“) nicht sofort durch Alterstod zusammenfallen. 2000 s
(≈ 33 min, 3,3 Jahre à 10 min) ist der Kompromiss für echten Generationswechsel.
Alle Läufe 3600 s, Zonen an, Option `-Dzone.study.lifespan=2000` (`79e6676`).
Neue Kennzahlen: `meanAge`, `lifted` = lebende Zellen > 40 px über der Gelände-/Blocker-
oberfläche direkt unter ihnen (also auf Zellen liegend, nicht auf Wänden), `maxLift`.

| Lauf | Seed | Ende | Min / Max lebend | Mittel 2. Hälfte | Fremdzonen |
|---|---|---|---|---|---|
| ohne Jahreszeiten | 4711 | 3600 s | 178 / 597 | 412 | 001 früh leer; 100-Zone ab ~3000 s 113–258 Zellen, 59–67 mit 100 → 000 |
| Winter 50 % | 4711 | **ausgestorben 3120 s** | 0 / 260 | 4 | nach 3. Winter (~1100 s) 32 Zellen ohne Erholung |
| Winter 50 % | 4712 | 3600 s | 42 / 378 | 120 | praktisch nur 000-Zone |
| Winter 50 % | 4713 | 3600 s | 62 / 520 | 208 | 001-Zone ~420–3120 s besiedelt (41–230 Zellen, bis 132 mit 001 → 000), im Winter bei ~3300 s erloschen |

Beobachtungen:

- Ohne unsterbliche Gründer verschwindet die frühere statische 001-Kolonie (Lauf ohne
  Jahreszeiten). Dafür evolviert dort nach einem Kohortensterben (~2940 s, 186 Zellen)
  eine echte 100-Besiedlung mit passender Route; auch in der 000-Zone tragen dann
  40 Zellen 100 → 000 (Zuwanderung/Grenzgänger).
- Mit Winter 50 % streuen die Ergebnisse stark: 1 von 3 Läufen stirbt aus. Seed 4713
  zeigt eine über ~45 min evolvierte, sich erneuernde 001-Kolonie (mittleres Alter
  100–650 s), die erst in einem späten Winter erlischt.
- **Erste beobachtete Aasverwertung (Seed 4713):** Aas-001 fällt bei beiden
  Besiedlungsschüben der 001-Zone (300 → 420 s: 35 → 16; 2460 → 2940 s: 63 → 0),
  zeitgleich sinkt die Zahl der Überreste (171 → 113). Überreste verschwinden nur, wenn
  sie leergesaugt werden; die 001-Kolonisten verwerten also 001-Aas, das sie per
  001 → 000 nutzen können. Aas mit 010/100 bleibt in allen Läufen unberührt.
- Höhe: Im Lauf ohne Jahreszeiten liegen im Mittel ~215 Zellen > 40 px über dem Gelände,
  `maxLift` bleibt aber über 60 min bei 135–205 px (≈ 4–10 Zellradien) ohne Trend.
  Mit Winter brechen diese Haufen jeden Winter zusammen (`lifted` → 1–10). Das sind
  Zellhaufen, keine wachsenden Stängel; ob Teile davon dauerhaft verbunden sind, misst
  die Kennzahl nicht.
- Drossel: weiterhin kein Jahreszeiteneffekt (4712: 0,982/0,980; 4713: 0,959/0,961).

## Folgerungen

- 25 % und auch 50 % Winterlicht sind ab Weltbeginn zu hart: Gründer in Fremdzonen
  sterben, bevor passende Enzyme evolvieren; aus der 000-Zone wandert danach niemand
  mit passender Route ein. Nächster Versuch: Jahreszeiten erst auf eine bereits
  angepasste Population anwenden (Fortsetzung von B) und/oder deutlich längeres Jahr.
  Ergebnis D: Die 001-Kolonie überlebt, aber als nahezu statische Gruppe alter Zellen.
- Lebensdauer 10 000 s bremst die Evolution in Randnischen: Gründer überleben
  Stunden und blockieren Platz. Mit 2000 s entstehen echte, sich erneuernde Besiedlungen
  (100-Zone ohne Winter, 001-Zone in Seed 4713) und erstmals Aasverwertung. Unter Winter
  50 % wird das Aussterberisiko jedoch relevant (1/3 Läufe).
- Für Stängel fehlt bisher jeder Hinweis auf wachsende Höhe. Dafür ist vermutlich ein
  Mechanismus nötig, der Zellen dauerhaft verbindet (Haftung/Gelenk) bzw. Höhe einen
  klaren Lichtvorteil gibt, den Haufen nicht schon erreichen.
- Für eine Vorratsstrategie mit Drossel fehlt bisher ein klarer Vorteil: Weder Winter
  25/50 % noch Zonen zeigen Jahreszeitenregelung. Wahrscheinlich nötig: ein Speicher,
  der 000 im Sommer wirklich übersteigt (000 ist bei 1 voll) und dessen Rückwandlung im
  Winter mehr bringt als der 20-%-Verlust kostet; ggf. Sensor für Jahreszeit/Lichttrend.
- 010-Zone: ohne 000-Nachbarn keine Besiedlung beobachtet; prüfen, ob Zonenreihenfolge,
  Geländeblocker in diesem Bereich oder fehlende Zuwanderung die Ursache sind.
- Aas mit Fremdformen entsteht, wird aber kaum gefressen. Mögliche Hebel: Aasdichte,
  längere Läufe, oder Austauschbefehle getrennt nach Energieform (offener TODO-Punkt).
