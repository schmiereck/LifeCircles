
# TODO

# Train Strategien

* Was für Train Strategien in was für Umgebnungen mit welchen Settings könnte ich für die Zellen aufbauen?
  Mein Ziel wäre eine Entwicklung hin zu mehrzelligen Verhalten in dem sie Strukturen schaffen oder kooperativ zusammen arbeiten. Am liebsten mittels der verschiedenen Cell-Status Typen.
  Bisher kämpft eher jeder gegen jeden um mehr Energie.
---
Um die Entwicklung hin zu mehrzelligem Verhalten und Kooperation zu fördern, könnten Sie folgende Trainingsstrategien in Betracht ziehen, die gezielt die Nutzung der verschiedenen Zell-Status-Typen anregen:

1.  **Strategie: "Stabile Brückenbauer"**
    *   **Umgebung:** Erstellen Sie eine Umgebung mit zwei voneinander getrennten Plattformen (aus Blockern) und einer Energiequelle, die nur erreichbar ist, wenn die Zellen eine stabile Brücke zwischen den Plattformen bilden.
    *   **Selektionskriterien:**
        *   Bevorzugen Sie Populationen, in denen Zellen eine durchgehende und stabile Verbindung zwischen den Plattformen herstellen.
        *   Belohnen Sie Zellen, die Teil der Brücke sind und über längere Zeit ihre Position halten.
        *   Zellen, die Energie von der schwer erreichbaren Quelle sammeln und ggf. an Brückenzellen weitergeben.
    *   **Zell-Status-Nutzung:** Verschiedene Status könnten für unterschiedliche Rollen optimiert werden:
        *   Status 1: "Ankerzellen", die sich fest an den Plattformen verankern.
        *   Status 2: "Brückensegmentzellen", die stabile Verbindungen zu Nachbarn aufbauen.
        *   Status 3: "Energiesammler/-verteiler", die die Brücke nutzen.
    *   **Settings:** Die Abstoßungs-/Anziehungskräfte zwischen den Zellen und die Interaktion mit den Blockern sind hier entscheidend. Die Energiequelle sollte so platziert sein, dass sie ohne Brücke nicht oder nur sehr schwer erreichbar ist.

2.  **Strategie: "Kooperative Energieoase"**
    *   **Umgebung:** Eine Umgebung mit wenigen, aber ergiebigen Energiequellen, die von mehreren Zellen gleichzeitig "angezapft" oder verteidigt werden müssen, um effizient genutzt zu werden. Alternativ: Energiequellen, die nur durch eine Kette von Zellen erreichbar sind (z.B. hoch oben).
    *   **Selektionskriterien:**
        *   Bevorzugen Sie Gruppen von Zellen, die gemeinsam eine Energiequelle nutzen und eine hohe Gesamtenergie aufweisen.
        *   Belohnen Sie Zellen, die Energie an andere Zellen (ggf. spezifische Typen/Status) weitergeben.
        *   Strafen Sie übermäßige Aggression innerhalb der Gruppe ab, die die Energieaufnahme behindert.
    *   **Zell-Status-Nutzung:**
        *   Status 1: "Sammler", optimiert für die Energieaufnahme an der Quelle.
        *   Status 2: "Transporter/Verteiler", die Energie von Sammlern empfangen und an andere Zellen in der Kolonie weiterleiten.
        *   Status 3: "Beschützer", die die Energiequelle oder die Kolonie vor "feindlichen" Einzelzellen abschirmen.
    *   **Settings:** Die Mechanismen zum Energietransfer zwischen Zellen sind hier zentral. Die Umgebung sollte so gestaltet sein, dass Einzelgänger benachteiligt sind.

3.  **Strategie: "Schutzwall-Formation"**
    *   **Umgebung:** Eine Umgebung, in der periodisch "Gefahren" auftreten (z.B. Bereiche mit starkem Energieentzug oder "aggressive" Blocker, die sich bewegen), vor denen sich die Zellen schützen müssen.
    *   **Selektionskriterien:**
        *   Bevorzugen Sie Populationen, die stabile, geschlossene Strukturen (Wälle) bilden können.
        *   Belohnen Sie Zellen, die Teil eines solchen Schutzwalls sind und deren "innere" Zellen eine hohe Überlebensrate/Energie haben.
        *   Die Effektivität des Walls könnte anhand der durchschnittlichen Energie der "eingeschlossenen" Zellen gemessen werden.
    *   **Zell-Status-Nutzung:**
        *   Status 1: "Außenwandzellen", robust, stark abstoßend gegenüber der Gefahr, bilden dichte Verbindungen.
        *   Status 2: "Innenzellen", optimiert für Energieerhalt oder Reproduktion im geschützten Bereich.
        *   Status 3: "Reparaturzellen", die Lücken im Wall füllen.
    *   **Settings:** Die Art der "Gefahr" muss klar definiert sein. Die Interaktion der Zellen untereinander (Anziehung für Wallbildung) und mit der Gefahr ist wichtig.

4.  **Strategie: "Spezialisierte Aufgabenteilung für Ressourcengewinnung"**
    *   **Umgebung:** Eine Umgebung, in der eine wertvolle Ressource (z.B. ein spezieller Energie-Typ) nur durch eine Abfolge von Aktionen verschiedener spezialisierter Zellen gewonnen werden kann. Beispiel: Eine Zelle muss einen "Schalter" (Blocker-Interaktion) umlegen, damit eine andere Zelle die Ressource abbauen kann, und eine dritte transportiert sie.
    *   **Selektionskriterien:**
        *   Belohnen Sie Gruppen von Zellen, die erfolgreich die gesamte Aufgabenkette abschließen und die Ressource gewinnen.
        *   Die Bewertung erfolgt auf Gruppenebene.
    *   **Zell-Status-Nutzung:** Jeder Schritt in der Aufgabenkette könnte von Zellen mit einem spezifischen Status ausgeführt werden.
        *   Status A: "Aktivator" (interagiert mit Umgebungsobjekt X).
        *   Status B: "Sammler" (kann Ressource Y sammeln, nachdem X aktiviert wurde).
        *   Status C: "Transporter" (bewegt Ressource Y zu einem Zielpunkt).
    *   **Settings:** Erfordert eine komplexere Umgebung mit spezifischen Interaktionsmöglichkeiten. Die Sensorfähigkeiten der Zellen müssen ausreichen, um die relevanten Umgebungselemente und andere Zellstatus zu erkennen.

Bei all diesen Strategien ist es wichtig, die Selektionskriterien sorgfältig zu wählen, um tatsächlich kooperatives Verhalten und die Nutzung der Zell-Status-Vielfalt zu belohnen, anstatt nur individuelle Fitness. Die Umgebung muss so gestaltet sein, dass Kooperation einen deutlichen Vorteil gegenüber rein egoistischem Verhalten bietet.
