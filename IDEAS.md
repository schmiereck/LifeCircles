# Ideen, noch keine abgeschlossenen Implementierungen

- Horizontaler genetischer Austausch, etwa über virenartige Überträger.
- Mehrzellige Arbeitsteilung: Gelenke, Sehnen, Knochen, Bindegewebe, Haut und Muskeln
  als funktionelle Entsprechungen aus Zellverbänden, auch mehrlagige Kugelpackungen.
- Dauerhafte Zellverbindungen, Stufe 3/4: eingerastete Aktorpaare und Geburtsbindung
  für Fadenwachstum. Entwurf in `ZELLVERBINDUNGEN.md` (Stufe 1/2 umgesetzt, 3/4 experimentell).
- Halt am Boden: experimentell umgesetzt (`BODENHALT.md`). Offen: Materialfarbe je
  Blockertyp als Nischenversuch, Kosten für Anker und Bindungen.
- Weitere Funktionsenergien nach 000/001: Größenänderung, Kraftaufwand und andere
  Zellfunktionen getrennt untersuchen; Engpässe können verwertbare Reserven hinterlassen.
- Weiterentwicklung der Enzymexpression: Beschränkung auf Aufnahmeereignisse oder ein
  Preis für Enzymsynthese/vorgehaltene Kapazität (neuronale Drossel ist umgesetzt).
- Rechenaufgaben als Umwandlungsvoraussetzung als getrenntes Experiment prüfen;
  noch nicht als allgemeine Weltregel festgelegt.
- Ressourcenwelten, in denen Kooperation und dynamische Regulation messbare Vorteile bieten.
- Nischen und stabile Lebensweisen über längere Zeit untersuchen; nicht nur Gesamtpopulation zählen.
- **Brownsche Bewegung als Hintergrund** (Idee des Nutzers, 26.10.2026, noch nicht beschlossen): Die Umgebung
  schubst und dreht die Zellen zufällig, statistisch gleichmäßig, ohne Strömung. Stark genug für Bewegung und
  Drehung, schwach genug, dass Verbindungen halten; zusammen mit schwacher Gravitation mehr Bewegung im Bild.
  Entwurf: Langevin-Thermostat auf der vorhandenen Dämpfung (Viskosität, Drehdämpfung). Je Zelle und Schritt ein
  zufälliger Kraftstoß (Gauß, beide Achsen) und ein Drehstoß; Varianz aus Dämpfung und einer „Temperatur“ T
  (Fluktuation-Dissipation), damit die mittlere Bewegungsenergie je Zelle T entspricht, unabhängig vom Zeitschritt
  und mit kleinen Zellen stärker bewegt als großen. Eigener Zufallsstrom, Schalter, Standard aus. Keine
  Energieform: mechanische Wärme aus der Umgebung, keine Zellenergie. Offene Punkte: T relativ zu
  Haftung/Bindungslösung kalibrieren (Bindungen dürfen nicht ständig reißen), welche Welten, und RACE: dort würde
  die zufällige Verschiebung (Irrfahrt, ~√t) mitgezählt; ohne Korrektur belohnt die Auswahl dann leicht schubsbare
  Körper statt eigenen Vortrieb.
