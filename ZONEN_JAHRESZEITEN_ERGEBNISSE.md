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

## Folgerungen

- 25 % Winterlicht ist in dieser Welt zu hart: Fremdzonen sterben, bevor Selektion
  wirken kann. Nächster Versuch: Winterlicht 50 % und/oder längeres Jahr.
- 010-Zone: ohne 000-Nachbarn keine Besiedlung beobachtet; prüfen, ob Zonenreihenfolge,
  Geländeblocker in diesem Bereich oder fehlende Zuwanderung die Ursache sind.
- Aas mit Fremdformen entsteht, wird aber kaum gefressen. Mögliche Hebel: Aasdichte,
  längere Läufe, oder Austauschbefehle getrennt nach Energieform (offener TODO-Punkt).
