# LifeCircles

Es soll eine 2D Simulation von biologischen Entwicklungen in Java geschrieben werden.
Die java Anwendung soll mit Maven gebaut werden.

Die Anforderungen an die Simulation sind die folgenden:

Simulation
* Umgebung
  * Viskose Umgebung (ähnlich Wasser, kann aber auch als Reibung auf dem Hintergrund abgebildet werden).
  * Gravitation.
  * Endlose Umgebung (was aus der Simulations-Area auf einer Seite raus geht, kommt auf der gegenüberligenden Seite wieder herein)
* Zellen
  * Zellen werden als Kreise abgebildet.
  * Zellen können frei rotieren und bewegt werden.
  * Zellen sollen sich möglichst nicht überschneiden. Deshalb wirken Zellen auf andere Zellen eine abstoßende Kraft, wenn sich ihre Kreiskörper überscheiden. Diese Kraft wird umso stärker, je näher sich ihre Zentren kommen.
  * Auf der Oberfläche der Kreise sitzen Sensor- /Aktor-Punkte. 
    * Die Aktoren werden bei Drehungen der Zelle mit rotiert.
	* Die Aktoren haben verschiedene Typen (Farben).
	* Als Sensor können sie andere Aktoren anderer Zellen innerhalb eines begrenzen Radius wahrnehmen.
	* Als Aktoren können sie, eines begrenzen Radius, eine anziehende oder abstoßende Kraft auf die Aktoren der anderen Zellen bewirken (wie ein Feld).
	* Ein Aktor eines bestimmten Typs sendet immer ein bestimmtes Feld aus. Wie dieses Feld auf Aktoren anderes Typs wirkt, ist global in der Simulation festgelegt.
    * Die Anzahl der Aktoren ist Fix und wird erst einmal auf 12 fest gelegt die gleichmäsig über die Oberfläche verteilt sind.
  * Eine Zelle hat einen Typ (Farbe) der von anderen zellen in der Umgebung wahrgenommen werden kann.
  * Eine Zelle hat eine Größe. Diese kann si innerhalb eines betimmten Bereiches aktiv vergrößern oder verkleinern.
  * Eine Zelle kann sich nicht aktiv bewegen oder rotieren, dies geschieht nur durch die Interaktion mit anderen Zellen und den Kraftfeldern die auf sie einwirken.
* Erbinformation
  * Die Erbinformation ist als neuronales Netz mit Synapsen und neuronen abgelegt.
    Sie bestimmt das Verhalten der Zelle über ihre Inputs und Outputs.
  * Inputs
    * Sensoren (Typ des eigenen Aktors, Feld-Typ fremder Aktoren (kann eine Mischung sein), Kraft auf den Sensor (anziehend oder abstoßend))
	* Typ der eigenen Zelle
	* Typ der Zellen in der Umgebung (kann eine Mischung sein)
  * Outputs
    * Größe der Zelle
	* Aktoren (Typ des eigenen Aktors, Kraft auf den Sensor).
* Typen
  * Ein Typ ist eine Mischung aus drei Zahlen-Werten (R, G, B).
    Er wird zur Kennzeichnung verwendet und kann als Input- oder Output durch drei Werte repräsentiert werden.
* There is no ground the cells can life on.
  We need something on the bottom that blocks Cells.
  Make it generic, so we can use this type of Blocker to generate later more complex environments.


View:
* Zoom und Pan.

Das System-Design der Anwendung soll enthalten:
* CalculationService der die Berechnungen in einem eigenen Thread ausführt.
* SimulationView das die Darstellung in einem eigenen Thread und einer von dem Berechungen unabhängigen Framerate darstellen kann (Datenaustausch über ein Dto-Objekt).

# TODO

* Es scheint mir so, als ob bei den ActorSensors die Interaktionen nicht korrekt funktionieren. Es sieht so aus, als ob gerade unterschiedliche Farben für die anziehende Interaktion verwendet werden (Schwarz zieht weiß an, Rot zieht Blau an, etc.).
  Prüfe das und ändere es so, dass gleiche Farben aufeinander anziehen wirken und unterschiedliche Farben abstoßend.

# DONE

* Fasse in der Anzeige der "Population History" immer die ähnlichsten 10% der Celltypes zusammen und stelle diese zusätzlich als Kurven dar.

* Die ausgesendete EnergieRays sollen auch nur bis zur maximalen größe der Zelle weit reichen. 
  Somit kann nach der Änderung auch für die Suche nach intersections der Partitioner verwendet werden, um Zellen zu finden die der Strahl treffen kann.

* Age
  * Cells have an age that is increased by 1 every frame.

* Simulation: Die Anzahl der SunRay die auf eine Fläche geht soll unabhängig von der Größe (Breite) der Simulation sein.
  Füge also pro Calculation-Step immer so viele SunRay in die Simulation ein, das z.B. im Schnitt alle 100 px ein SunRay auftrifft.

* If the last Cell dies, use this one und insert mutation of the last cell to create the new population.

* Mutation: Neue Hidden-Layer einfügen.
* Mutation: Neue Synapse im Hidden-Layer einfügen.
* Mutation: Neue Neuronen Hidden-Layer einfügen.
* Mutation: Neuronen in den Hidden-Layer entfernen.
* Mutation: Synapse in den Hidden-Layer entfernen.

* Lagere alle Funktionalität die mit der Berechnung der Zellen-Energie zu tun hat in einen "EnergyCellCalcService" aus.

* Lagere alle Funktionalität die mit der Berechnung der Zellen-Abstoßung zu tun hat in einen "RepulsionCellCalcService" aus.

* Lagere alle Funktionalität die mit der Berechnung der Zellen-Interaktion zu tun hat in einen "ActorSensorCellCalcService" aus.

* Simplify the ActorSensor rules. ActorSensor vom selben Typ (gleiche farben) ziehen sich an, 
  ActorSensor mit unterschiedlichen Typ stoßen sich ab. 
  Die Berechnung berücksichtigt alle drei Farben und berechnet einen Mittelwert aus allen Unterschieden der Farben.
* It looks like, the visualisation of the ActorSensor fields are not respect the radius used in calculation.
Check this and fix it, if this is true.
* Füge in dem Neuronalen Netz einen neuen Output hinzu der in der ReproductionManager.canReproduce() verwendet wird, um zu entscheiden, ob sich eine zelle teilen will. Beachte, das bei der Zellteilung die Energie der Mutter-Zelle auf beide aufgeteilt wird.
* View: zeichne einen weißen Rahmen mit abgerundeten Ecken um die Simulation-Area.
* View: zeichne einen hellgrauen Rahmen mit abgerundeten Ecken um den bereich der Simulation, in der die Zellen sich befinden können, so dass dieser beim Zoom und Pan mit verschoben wird.
* View: Beim Zoom soll die aktuelle Maus-Position als Mittelpunkt benutzt werden.

* Simulation: Wenn nur noch 10% der initialen Anzahl Zellen vorhanden ist, erzeuge neue Zellen als Mutation von zufällig ausgewählten Zellen. Ausch hierbei wird die Energie auf Mutter- und Kind-Zelle aufgeteilt.

* Simulation: Um die Geschwindigkeit zu verbessern, führe ein viruelles Raster von der maximalen Zellgröße ein. 
  Je nach position einer zelle wird diese in einer dieser Rasterzellen platziert.
  Bei der berechnung von Kräften müssen dann nur die Zellen in der eigene Rasterzelle und der jeweiligen acht Nachbar-Rasterzellen berücksichtigt werden.

* Simulation: Um Energie zwischen Zellen austauschen zu können, kann eine Zelle einen Energie-Strahl, entsprechen des SunRay in Richtung eines ihrer Sensor-Aktoren nach außen senden. 
  * Der Betrag der Energie ist immer 3% der Energie der Zelle.
    Ihre Energie wird dann auf jeden Fall um diesen Betrag reduziert.
  * Die Nachbarzelle die den Energie-Strahl schneidet erhöht ihre Energie um diesen Betrag. Wenn er keine Nachbarzelle trifft, ist die Energie verloren.
  * Füge im Neuronalen Netz einen neuen Output für jeden SensorActor hinzu, der beim überschreiten eines Schwellwertes einen solchen Strahl sendet vom Mittelpunkt der zelle in Richtung dieses SensorActors nach außen sendet.

* If all Cells are dead, create new population with random induviduums in the inial size.

* Lagere alle Funktionalität die mit der Berechnung der Sun-Energie zu tun hat in einen "EnergySunCalcService" aus.

* Energie
  * Cells have an energy.
  * Cells can absorb energy from the environment.
    * Sun-Energy Rays are emitted from the top of the environment on random Places in a specified rate. Rays are existing only for one calculation cycle.
    * If a Ray intersect a Cell, the Cell absorbe the energy an the cell's energy is increased. The Ray is intersected and removed.
    * If a Ray intersect a Blocker, the Ray is intersected and removed.
* Cells lost energy over time.
  * If the cell's energy is below a certain threshold, the cell will die.

* Lagere alle Funktionalität die mit der Berechnung der Blocker und der Interaktion mit Zellen zu tun hat in einen "BlockerCellCalcService" aus.
