# Überreste, Aasfresser und Spezialisierung

Entwurf vom 16.10.2026. Die Lebensdauer ist jetzt live in der Cell-Box einstellbar.
Die folgenden Erweiterungen sind Vorschläge; Überreste und zusätzliche Ressourcen
sind noch nicht implementiert.

## 1. Kleiner erster Schritt: Restenergie bleibt verfügbar

Beim Alterstod besitzt eine Zelle oft noch Energie. Diese könnte vollständig oder
mit einem ausdrücklich festgelegten Verlust in ein passives Überrestobjekt übergehen.
Bei Tod mit Energie null entsteht zunächst keine neue verwertbare Energie. Die Bilanz
lautet: entnommene Zellreserve = Reserve des Überrests + ausgewiesener Verlust.

Ein Überrest hat keinen neuronalen Controller, keine Teilung und keine Sonnenaufnahme.
Sein Verhalten ist fest: passive Bewegung/Kollision, Aufnahme durch Nachbarn und
gegebenenfalls zeitabhängiger Zerfall. Restmenge und Zerfallszeit begrenzen seine Existenz.
Das wäre direkt an den Lebensdauerregler anschließbar: Mehr Alterstod erzeugt mehr
verwertbare Restenergie, ohne dass die Sterberegel Nahrung aus dem Nichts erzeugt.

Technisch sollte das ein eigener passiver Objekttyp sein, keine normale Zelle mit
einem vorgetäuschten leeren Gehirn. Sonst zahlen tausende Überreste weiterhin für
Sensorarrays, Denkvorbereitung und Zellverwaltung. Die räumliche Suche und Darstellung
können sie trotzdem berücksichtigen. Auch passive Körper verursachen Physikkosten;
leere oder vollständig zerfallene Überreste müssen verschwinden.

Der jetzige Transferdienst akzeptiert als Ziel ausdrücklich `Cell`. Für Aas müsste
der Zugriff auf einen endlichen Energiespeicher verallgemeinert werden. Lebende
Zellen könnten weiter freiwillig abgeben oder Energie entziehen; Überreste wären
passive Quellen. Aufnahme muss dieselbe gleichzeitige, konservative Verrechnung
verwenden, einschließlich begrenzter Empfängerkapazität und periodischer Distanz.
Ein unterscheidbares Sensorsignal für Überreste wäre sinnvoll; eine Netzschnittstellen-
Änderung braucht allerdings eine definierte Migration alter DNA.

Erste Prüfungen: Alterstod überträgt nur vorhandene Energie; Hunger erzeugt keine;
mehrere Aasfresser teilen endliche Vorräte ohne Reihenfolgevorteil; volle Empfänger
vernichten keine Restenergie; Zerfall ist unabhängig von der Schrittweite; Aufnahme
über den Weltrand funktioniert; Teilung/Tod/Fressen über viele Generationen hat
keinen positiven Bilanzfehler.

## 2. Danach Energie und Baustoffe trennen

Für den gewünschten Tod an einem Mangel bei gleichzeitig verbleibenden anderen
Vorräten würde ich zunächst zwei Ressourcen verwenden:

| Ressource | Verwendung | Typischer Verbleib beim Tod |
|---|---|---|
| Nutzenergie | Grundumsatz, Gehirn, Bewegung und Verarbeitung | Noch vorhandener Energievorrat |
| Baustoffe / Biomasse | Wachstum und Bildung von Kindern | Körpermaterial und Baustoffreserve |

Dadurch kann eine Zelle etwa mit vollen Baustoffvorräten an Energiemangel sterben.
Ihre Biomasse bleibt für andere verfügbar. Reiner Baustoffmangel sollte zunächst
Wachstum und Teilung verhindern; ob er auch nach einer Frist zum Tod führt, wäre
eine eigene bewusst eingestellte Regel.

Wichtig: Momentan gibt es keine eigenständige konservierte Baustoffbilanz bei
Größenänderung und Teilung. Aus dem Radius einfach Nahrung abzuleiten wäre deshalb
ein Energieeinfallstor: Wiederholtes Teilen, Wachsen und Sterben könnte verwertbare
Materie erzeugen. Vor essbarer Körpermasse müssen Eltern/Kind-Material beim Teilen
aufgeteilt und Wachstum aus einem realen Baustoffvorrat bezahlt werden. Schrumpfen
darf höchstens zuvor gebundene Bestände freigeben, keinen kostenlosen Neubestand.

Mehrere Energieträger sind ebenfalls möglich. Für den Einstieg macht die Trennung
von Energie und Materie die Bilanzen und Engpässe aber leichter prüfbar als ein
separater Energieträger für jede einzelne Aktion. Ob der Gehirnverbrauch später
einen besonderen Energieträger braucht, sollte ein gezieltes Folgeexperiment zeigen.

## 3. Umwandlung als begrenzte Fähigkeit

Die gemeinsame Basis könnte wenige feste, prüfbare Reaktionen enthalten, beispielsweise
Verdauung organischer Reserven in Nutzenergie. Die DNA bestimmt die Kapazität und
die laufenden Kosten der dafür nötigen Ausstattung. Das Gehirn regelt deren Aktivität.
So ist Umwandlung prinzipiell für jede Linie erreichbar, aber nicht gleichzeitig
beliebig stark und kostenlos verfügbar.

Begrenzungen könnten sein:

- Ein gemeinsames Kapazitätsbudget pro Zelle: mehr Photosyntheseausstattung lässt
  weniger Platz für Verdauung oder andere Verarbeitung.
- Ausstattung kostet Material und Unterhalt, auch wenn sie gerade nicht genutzt wird.
- Jede Reaktion hat endlichen Durchsatz und benötigt tatsächlich vorhandene Eingangsvorräte.
- Umwandlung verursacht ausgewiesene Verluste beziehungsweise zusätzliche Kosten.
- Ausgabe kann gespeichert, verbraucht oder an Nachbarn abgegeben werden.

Damit werden Kooperation und verschiedene Zellrollen möglich: Eine Zelle beschafft
Ressourcen, eine andere verarbeitet sie, weitere wachsen oder bewegen den Verband.
Das sind Hypothesen über Selektionsanreize, keine Garantie, dass Spezialisierung entsteht.
Eine universelle Zelle darf weiterhin erfolgreich sein, wenn die Umwelt keine Vorteile
für Arbeitsteilung bietet. Zu prüfen sind daher Spezialisten, Generalisten und
kooperierende Verbände unter denselben Bilanzen und wechselnden Ressourcenbedingungen.

Für jede Reaktion braucht es eine explizite Stoffbilanz und eine Energiebilanz.
Unterschiedliche Ressourcen lassen sich nicht ohne festgelegte Einheiten einfach
aufsummieren. Ein geschlossener Umwandlungskreislauf darf weder Materie noch nutzbare
Energie erzeugen. Auch Sensor-/Reglerausgänge wählen nur Raten; sie schreiben keine
Ressourcen direkt gut.

## 4. Muss das Netz den Umwandlungsalgorithmus erfinden?

Ein erst zu lösender Rechencode könnte Netzfunktionen erzwingen. Dann hängen jedoch
ökologischer Erfolg und Gehirngröße stark von einer von uns festgelegten Prüfaufgabe ab.
Das wäre ein interessantes separates Experiment, ähnlich den logischen Trainingsaufgaben.

Für die offene Welt würde ich zunächst feste Reaktionsregeln und evolvierbare
Ausstattung verwenden. Die anspruchsvolle Netzaufgabe wäre die passende Steuerung:
Wann aufnehmen, verarbeiten, speichern oder weitergeben? Wann Ausstattung aktivieren,
und welche Rolle soll ein Kind übernehmen? Damit bleibt echte Regulation eine
potenziell nützliche Leistung, ohne dass jeder Energiegewinn zwingend ein künstliches
Rechenrätsel erfordert. Danach lässt sich messen, ob dynamische Steuerung gegenüber
festen Regeln tatsächlich einen Vorteil bietet.

## Empfohlene Reihenfolge

1. Passive Überreste mit ausschließlich vorhandener Restenergie und konservativer Aufnahme.
2. Geschlossene Baustoffbilanz für Wachstum und Teilung, danach verwertbare Körpermasse.
3. Wenige begrenzte Reaktionen mit evolvierbarer Kapazität und Kosten.
4. Ökologische Vergleiche von Aasfressern, Räubern, Produzenten und kooperierenden Verbänden.

Jeder Schritt sollte zuerst mit kleinen deterministischen Welten und einer vollständigen
Ressourcenbilanz geprüft werden. Das hält neue Wachstums-/Sterbeschleifen kontrollierbar
und vermeidet eine sofortige Erhöhung der Laufzeit durch weitere Gehirne.
