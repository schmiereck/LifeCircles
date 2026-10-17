# Mehrere Energieformen und Überreste als Zellen

Präzisierter Entwurf vom 16.10.2026 nach der Vorgabe des Nutzers. Diese Fassung ersetzt
meinen früheren Vorschlag mit eigenständigen Überrestobjekten und Baustoffen.

**Umsetzungsstand 17.10.2026:** Der erste Schritt mit zwei getrennten Energieformen,
Sonnenquellenwahl, konservativem Austausch, Teilung und Dateikompatibilität ist umgesetzt.
[Regeln und Tests](MEHRERE_ENERGIEN.md). Überreste und Umwandlung fehlen weiterhin.

## Tatsächlicher Bestand

- `Cell` besitzt Primär- und Sekundärspeicher, jeweils begrenzt auf 0 bis 1.
- Neue Sekundärspeicher beginnen leer; alte Spielstände erhalten keinen zusätzlichen Vorrat.
- Sonnenaufnahme liefert wahlweise eine Form; Austausch und Teilung behandeln beide getrennt.
- Grundumsatz, Gehirngebühren, neuronale Energiesignale und Überlebensregeln verwenden vorerst Primärenergie.
- `CellType` beschreibt RGB-Eigenschaften, keine Energieform oder Stoffwechselklasse.
- Spezialisierte Verbrauchs-/Umwandlungsregeln und Überreste sind noch nicht implementiert.

## Gewünschtes Modell

Überreste bleiben normale Zellen mit ihren noch vorhandenen Energievorräten.
Sie bekommen einen Zustand „tot“ und fest vorgegebenes Verhalten, keine neuronalen
Entscheidungen und keine Teilung. Nachbarn absorbieren ihre Restenergien über den
normalen Austausch. Erst wenn alle Vorräte leer sind, verschwindet die Zelle.
Es gibt hierfür kein zusätzliches Materie-, Körpermaterial- oder Baustoffmodell.

Ein Verbleib als `Cell` passt bereits zum Zellziel des Transferdienstes. Für die
Performance sollten tote Zellen die Gehirn-Eingabeaufbereitung und das Denken
überspringen. Ihre feste Steuerung darf nicht versehentlich alte Aktorbefehle
weiterlaufen lassen. Welche passiven Kräfte und welche Aufnahme-/Abgaberegeln
nach dem Tod gelten, ist ausdrücklich festzulegen.

## Empfohlene Reihenfolge

1. Mehrere ausdrücklich benannte Energiespeicher pro Zelle einführen. Kapazitäten,
   Sonnenaufnahme, Transfer, Teilung und Dateikompatibilität eindeutig definieren.
   Anfangs keine automatische Umwandlung zwischen den Speichern.
2. Verbrauch, Überlebensbedingungen und begrenzte Umwandlung festlegen. Dann kann
   eine Zelle an Mangel einer benötigten Energieform sterben, obwohl andere
   Speicher noch gefüllt sind. Nicht jeder leere Speicher muss zwingend tödlich sein.
3. Tote Zellen mit genau diesen übrigen Vorräten bestehen lassen. Absorption
   leert sie, ohne neue Energie beim Tod gutzuschreiben.
4. Evolvierbare Umwandlungskapazitäten und deren neuronale Steuerung untersuchen.

Auch ohne Materiemodell sind Bilanzen erforderlich: Teilung verteilt vorhandene
Vorräte, Transfer erzeugt keine Energie, Tod bewahrt höchstens die verbleibenden
Reserven. Umwandlungskreisläufe dürfen keine nutzbare Energie erzeugen.

Die Anzahl und Bedeutung der Energieformen, deren Umweltquellen sowie die
Verbrauchs-/Umwandlungsregeln sind noch offen. Eine reine Erweiterung der Speicher
sollte nicht stillschweigend neue Todesregeln oder kostenlose Anfangsvorräte einführen.
Bestehende Dateien und die neuronale Ein-/Ausgabeschnittstelle benötigen eine
explizite Kompatibilitätsregel, bevor solche Änderungen aktiviert werden.
