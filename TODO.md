# Aufgaben

## Aktueller Auftrag

- [x] Gehirnpause-Sackgasse (nur 000 aus Sonne, keine 000→001-Route) durch basale Notatmung und Umkehrmutation lösen.
- [x] 600-s-NONE-Vergleich mit/ohne Notatmung auswerten (Enzymanteil, Pausen, Population).
- [ ] Prüfen, ob der Enzymvorteil unter Notatmung stark genug selektiert (längerer Lauf,
  ggf. höhere Gehirnkosten oder schwächere Notatmung als Parameterstudie).
- [ ] GUI-Gegenprobe des Nutzers auf zweitem Rechner (neue Welt und festgefahrener Altlauf).
- [x] Spektralzonen 000 | 001 | 010 | 100 mit schmalen Mischstreifen, GUI-schaltbar.
- [x] Gesamtintensität der Sonne in der Populationskurve.
- [x] Jahreszeiten, in der GUI regelbar (Dauer, Wintertiefe).
- [x] Neuronale Drosselung der Enzymaktivität (angehängter Ausgang, verlustfreie Migration).
- [ ] Prüfen, ob Winter Vorratshaltung in Fremdformen belohnt und die Drossel genutzt wird.
- [x] Veralteten Test `InheritanceRegressionTest.reproductionUsesChildStateBeforeFirstThink…`
  an „kein kostenloser Geburtsgedanke“ angepasst: prüft die Eingaben der ersten Entscheidung.
- [x] Langlauf mit Zonen: Fremdformen in lebenden Zellen und Überresten, Nischen an Grenzen.
- [x] Folgelauf mit milderem Winter (50 %): Fremdzonen sterben weiterhin im ersten Jahr.
- [x] Jahreszeiten auf angepasste Population (Fortsetzung von B) anwenden, mit Kontrolle.
- [x] Evolutionsläufe mit Lebensdauer 2000 s wiederholt (1 + 3 Seeds); Besiedlung und erste Aasverwertung.
- [x] Zellhaftung Stufe 1/2 (Tonus über Pause/Tod, Farb-/Körperfarb-Affinität) umgesetzt.
- [x] Zonenläufe mit/ohne Zellhaftung verglichen (je 1 Seed): Population und Besiedlung besser, Höhe gleich.
- [x] Haftungseffekt mit 4 Seeds und Ablation geprüft: nicht gesichert (Streuung), ohne Halten am schwächsten.
- [ ] Bei Bedarf belastbarer Populationsvergleich (8–10 Läufe je Bedingung, kürzer oder kleinere Welt).
- [x] Stufe 3/4 (Einrasten, Geburtsbindung) experimentell und schaltbar umgesetzt.
- [x] Zonenläufe mit Bindungen/Geburtsbindung gegen Stufe 1/2 verglichen (4 Seeds): Verbände größer und höher, Population gleich.
- [x] Form gebundener Verbände geprüft (Momentaufnahme, Überstand-Kennzahl): dichte Haufen, keine Stängel.
- [ ] Standfestigkeit: Halt am Boden oder breite Basis; ein gebundener Stab kippt auf der runden Fußzelle.
  Bodenfeld + Anker experimentell umgesetzt (`BODENHALT.md`, `GroundHoldTest`).
- [x] Zonenläufe Bodenhalt (je 4 Seeds): wenige Anker, verankerte Körper flach, keine Stängel; Bodenfeld verkleinert gebundene Körper (`BODENHALT.md`).
- [x] Haftung, Bindungen und Bodenhalt in Trainingswelten per Schalter `trainingAdhesion` nutzbar (`TrainingAdhesionTest`).
- [x] TrainMode-Läufe mit/ohne Haftung (HIGH_POSITION2/3, je 4 Seeds): kein Höhenvorteil in HIGH_POSITION2; in HIGH_POSITION3 halten Bindungen die Form besser (`BODENHALT.md`).
- [x] HIGH_POSITION2 mit 120-s-Generationen (4 Seeds je off/full): beide Varianten höher (off 293, full 251 px statt ~150), Haftung ohne Vorteil, weiter Haufen (`BODENHALT.md`).
- [x] Spektralzonen mit fester, sich wiederholender Breite; `TrainMode.WORLD2` (4 × NONE-Breite, alle neue Physik) (`WELT2.md`).
- [ ] WORLD2: Sichtung durch den Nutzer, danach lange Läufe (Besiedlung, Nischen, Höhe durch Lichtkonkurrenz).
- [ ] HIGH_POSITION mit Haftung nur mit Speicherbegrenzung oder kleinerer Population vergleichen. Kein künstlicher Lichtgradient: Höhe soll aus Selektion (HIGH_POSITION) bzw. Lichtkonkurrenz (NONE) kommen.
- [ ] Vorteil für Vorratshaltung schaffen/prüfen (Speicher über 000-Kapazität, Rückwandlung im Winter, ggf. Jahreszeitsensor).
- [ ] Ursache der leeren 010-Zone klären (Zonenreihenfolge, Geländeblocker, Zuwanderung).
- [ ] Warum wird Aas mit Fremdformen kaum gefressen? Aasfresser-Nische gezielt prüfen.

- [x] Performance-Regression nach funktionaler Gehirnenergie untersuchen und seriellen Engpass beheben.
- [ ] Wirkung des Fixes im gemeldeten langsamen 2500er-Lauf nach GUI-Neustart nachmessen.

- [x] Feste Zuordnung 000 → Erhalt, 001 → Gehirn; Mangel, Erholung, Gründerbudgets und Altpopulationen prüfen.
- [x] Umwandlungsregeln an Specialization koppeln; gemeinsame vererbbare Tabelle und Zellketten prüfen.
- [x] Default-Gründer auf 48 versteckte Neuronen verkleinern, einschließlich Zustandsnetzen.
- [x] Aasregen-Training ohne Sonne mit kleinen Kontrollwelten und Futterbilanz.
- [x] Aasregen-Variante mit 001-Futter und notwendiger Umwandlung 001 → 000 prüfen.
- [x] Oben „New…“ mit Szenarioauswahl und konsistentem Neustart anbieten.
- [x] Energieformen auf acht RGB-Bitkombinationen erweitern.
- [x] Eigene und benachbarte Energievorräte sensorisch zugänglich machen.
- [x] Bestehende DNA/Dateien mit Tests migrieren; alte Sensorpositionen erhalten.
- [x] Überreste als normale, nicht denkende Zellen; normale Absorption bis leer.
- [x] Projektgedächtnis und thematische Dokumente auf Abschlussstand bringen.

- [x] Rennbahn-Training RACE mit dichter Bewertung (keine Teilung/Masse, impulsneutrale Geburt).
- [x] Optionale Geländereibung (Nutzerentscheidung): `BODENREIBUNG.md`, RACE mit Wachstumsphase.
- [x] RACE-Auswahl als Inselmodell (Nutzerentscheidung: echte Evolution, kein Lehrplan): 32 Bahnen in
  4 Inseln, Turnier, Elite mit Neubewertung durch Zufallsstart, Migration, Katastrophe bei Stillstand.
- [x] RACE-Langlauf mit Inseln (587 Generationen): Spitzen nur aus Startglück, kein Gang (`RENNBAHN.md`).
- [x] RACE: Bewertung aus 3 Startlagen je Genom, Mittelwert, Rekord daraus (Nutzerentscheidung 25.10.2026).
- [ ] RACE: Zufallsgehirne schwingen nicht (Gewichte ± 0,001, Vorwärtsnetz, kein Takteingang). Angeborener
  Takteingang? (Entscheidung Nutzer, `RENNBAHN.md`).

## Anschließend

- [x] Erste Verbrauchs-/Überlebensanforderungen: 000 Erhalt, 001 Gehirn.
- [ ] Getrennte Kosten für Größenänderung und Aktorkräfte entwerfen und mechanisch prüfen.
- [x] Begrenzte Umwandlungen mit evolvierbarer Route/Kapazität implementieren und bilanzieren.
- [ ] Prüfen, ob Austauschbefehle getrennt nach Energieform evolvierbar werden sollen.
- [ ] Neue Ökologietests: Produzenten, Räuber/Aasfresser und kooperierende Zellverbände.
- [ ] Große/dichte Welten weiter profilieren; zusätzlichen Aufwand neuer Sensoren messen.
  Gehirne kompiliert (`NETZKOMPILIERUNG.md`), Wechselwirkungen beschleunigt (`PERFORMANCE.md`).
  Energietransfer parallelisiert, Integration entlastet. WORLD2 ~12–15 ms/Schritt; größte Blöcke
  WORLD2 ~9,3 ms/Schritt (unbelastet): Gehirne ~2,5, INTERACTIONS ~1,6, TRANSFER ~1,5, INTEGRATION ~1,4,
  BRAIN_FUNDING ~0,8 ms. Nächster Ansatz Wechselwirkungen: Paare einmal rechnen, beiden gutschreiben.
