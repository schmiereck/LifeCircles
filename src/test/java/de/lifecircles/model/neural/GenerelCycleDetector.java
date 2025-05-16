package de.lifecircles.model.neural;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class GenerelCycleDetector {
    private final Set<Object> visited = new HashSet<>();
    private final List<ObjectInfo> currentPath = new ArrayList<>();
    private List<ObjectInfo> cyclePath = null;
    
    // Set von Klassen, die ignoriert werden sollen, da sie keine echten Zyklen darstellen
    private final Set<Class<?>> ignoredClasses = new HashSet<>();
    
    public GenerelCycleDetector() {
        // Wrapper-Klassen für primitive Typen
        ignoredClasses.add(String.class);
        ignoredClasses.add(Boolean.class);
        ignoredClasses.add(Character.class);
        ignoredClasses.add(Byte.class);
        ignoredClasses.add(Short.class);
        ignoredClasses.add(Integer.class);
        ignoredClasses.add(Long.class);
        ignoredClasses.add(Float.class);
        ignoredClasses.add(Double.class);
        ignoredClasses.add(Void.class);
        ignoredClasses.add(Class.class);
        
        // Wir ignorieren keine Java Collections mehr, da sie Teil eines Zyklus sein können
    }
    
    private static class ObjectInfo {
        final Object object;
        final Object parent;
        final String fieldName;
        final boolean isTransient; // Neue Eigenschaft, um transiente Felder zu markieren

        ObjectInfo(Object object, Object parent, String fieldName) {
            this(object, parent, fieldName, false);
        }

        ObjectInfo(Object object, Object parent, String fieldName, boolean isTransient) {
            this.object = object;
            this.parent = parent;
            this.fieldName = fieldName;
            this.isTransient = isTransient;
        }
        
        @Override
        public String toString() {
            if (parent == null) {
                return object.getClass().getSimpleName() + "@" + 
                       Integer.toHexString(System.identityHashCode(object));
            }
            return parent.getClass().getSimpleName() + "." + fieldName + " -> " +
                   object.getClass().getSimpleName() + "@" + 
                   Integer.toHexString(System.identityHashCode(object)) +
                   (isTransient ? " (transient)" : "");
        }
    }

    public boolean hasCycle(Object obj) {
        cyclePath = null;
        visited.clear();
        currentPath.clear();
        return detectCycle(obj, null, null);
    }
    
    private boolean detectCycle(Object obj, Object parent, String fieldName) {
        return detectCycle(obj, parent, fieldName, false);
    }
    
    private boolean detectCycle(Object obj, Object parent, String fieldName, boolean isTransient) {
        if (obj == null) {
            return false; // Null-Objekte ignorieren
        }
        
        // Ignoriere Enums und andere problematische Klassen
        if (obj.getClass().isEnum() || 
            ignoredClasses.contains(obj.getClass()) || 
            obj.getClass().getName().startsWith("java.lang.")) {
            return false;
        }

        // Transiente Felder ignorieren
        if (isTransient) {
            return false;
        }

        ObjectInfo currentInfo = new ObjectInfo(obj, parent, fieldName, isTransient);
        
        if (visited.contains(obj)) {
            // Zyklus gefunden - Pfad speichern
            cyclePath = new ArrayList<>(currentPath);
            cyclePath.add(currentInfo);
            
            // Startpunkt des Zyklus finden
            int cycleStart = -1;
            for (int i = 0; i < currentPath.size(); i++) {
                if (currentPath.get(i).object == obj) {
                    cycleStart = i;
                    break;
                }
            }
            
            if (cycleStart >= 0) {
                cyclePath = cyclePath.subList(cycleStart, cyclePath.size());
            }
            
            return true;
        }
        
        visited.add(obj);
        currentPath.add(currentInfo);

        boolean cycleFound = false;

        // Prüfstrategie basierend auf Objekttyp
        if (obj instanceof Collection) {
            cycleFound = handleCollection((Collection<?>) obj, obj);
        } else if (obj instanceof Map) {
            cycleFound = handleMap((Map<?, ?>) obj, obj);
        } else if (obj.getClass().isArray() && !obj.getClass().getComponentType().isPrimitive()) {
            // Arrays mit Objektreferenzen prüfen
            cycleFound = handleArray((Object[]) obj, obj);
        } else {
            // Alle anderen Objekte über Reflexion prüfen
            cycleFound = handleObjectFields(obj);
        }

        if (!cycleFound) {
            currentPath.remove(currentPath.size() - 1);
            visited.remove(obj);
        }
        
        return cycleFound;
    }
    
    private boolean handleCollection(Collection<?> collection, Object parent) {
        int index = 0;
        for (Object item : collection) {
            if (item != null && detectCycle(item, parent, "item[" + index + "]")) {
                return true;
            }
            index++;
        }
        return false;
    }
    
    private boolean handleMap(Map<?, ?> map, Object parent) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && detectCycle(entry.getKey(), parent, "key")) {
                return true;
            }
            if (entry.getValue() != null && detectCycle(entry.getValue(), parent, "value")) {
                return true;
            }
        }
        return false;
    }
    
    private boolean handleArray(Object[] array, Object parent) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && detectCycle(array[i], parent, "array[" + i + "]")) {
                return true;
            }
        }
        return false;
    }
    
    private boolean handleObjectFields(Object obj) {
        // Ignoriere Enums und Systemklassen
        Class<?> objClass = obj.getClass();
        if (objClass.isEnum() || objClass.getName().startsWith("java.") || 
            objClass.getName().startsWith("javax.") || objClass.getName().startsWith("sun.")) {
            return false;
        }
        
        // Prüfe alle Felder des Objekts und der Elternklassen
        Class<?> clazz = objClass;
        while (clazz != null && !clazz.equals(Object.class)) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // Statische Felder ignorieren
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                // Transiente Felder bei Serialisierung ignorieren
                boolean isFieldTransient = Modifier.isTransient(field.getModifiers());
                try {
                    // Sicherheitsprüfung vor dem Zugriff
                    if (!field.trySetAccessible()) {
                        continue; // Überspringe Felder, auf die nicht zugegriffen werden kann
                    }
                    
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null && detectCycle(fieldValue, obj, field.getName(), isFieldTransient)) {
                        return true;
                    }
                } catch (IllegalAccessException | SecurityException | IllegalArgumentException e) {
                    // Bei Problemen mit der Reflexion das Feld überspringen
                    continue;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }
    
    /**
     * Gibt Informationen zum gefundenen Zyklus aus
     * @return Eine Beschreibung des Zyklus oder null wenn kein Zyklus gefunden wurde
     */
    public String getCycleDescription() {
        if (cyclePath == null || cyclePath.isEmpty()) {
            return "Kein Zyklus gefunden.";
        }
        
        StringBuilder sb = new StringBuilder("Gefundener Zyklus:\n");
        for (ObjectInfo info : cyclePath) {
            sb.append(" -> ").append(info).append("\n");
        }
        return sb.toString();
    }

    /**
     * Prüft, ob der gefundene Zyklus problematisch ist (nicht-transient).
     * Ein problematischer Zyklus enthält mindestens eine nicht-transiente Referenz.
     * 
     * @return true wenn der Zyklus problematisch ist, false wenn alle Referenzen transient sind
     */
    public boolean isProblematicCycle() {
        if (cyclePath == null || cyclePath.isEmpty()) {
            return false; // Kein Zyklus gefunden
        }
        
        // Überprüfe, ob mindestens ein Feld im Zyklus nicht-transient ist
        for (ObjectInfo info : cyclePath) {
            if (!info.isTransient) {
                return true; // Nicht-transientes Feld gefunden
            }
        }
        
        // Alle Felder im Zyklus sind transient
        return false;
    }

    /**
     * Führt einen direkten Test auf zyklische Referenzen zwischen Cell und SensorActor durch
     * @param cell Die zu prüfende Zelle
     * @return True wenn eine zyklische Referenz gefunden wurde
     */
    public boolean testCellSensorActorCycle(Object cell) {
        try {
            // Spezifische Tests für Cell <-> SensorActor Zyklen
            if (cell.getClass().getSimpleName().equals("Cell")) {
                Field sensorActorsField = cell.getClass().getDeclaredField("sensorActors");
                if (!sensorActorsField.trySetAccessible()) {
                    return false; // Kann nicht auf das Feld zugreifen
                }

                List<?> sensorActors = (List<?>) sensorActorsField.get(cell);
                if (sensorActors != null && !sensorActors.isEmpty()) {
                    Object sensorActor = sensorActors.get(0);

                    // Prüfe, ob SensorActor zurück zur Zelle referenziert
                    Field parentCellField = sensorActor.getClass().getDeclaredField("parentCell");
                    if (!parentCellField.trySetAccessible()) {
                        return false; // Kann nicht auf das Feld zugreifen
                    }

                    Object parentCell = parentCellField.get(sensorActor);
                    if (parentCell == cell) {
                        cyclePath = new ArrayList<>();
                        boolean isTransient = Modifier.isTransient(parentCellField.getModifiers());
                        cyclePath.add(new ObjectInfo(cell, null, "root"));
                        cyclePath.add(new ObjectInfo(sensorActor, cell, "sensorActors[0]"));
                        cyclePath.add(new ObjectInfo(cell, sensorActor, "parentCell", isTransient));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Bei Fehlern den Test überspringen
        }

        // Standardprüfung falls der spezifische Test nichts findet
        return hasCycle(cell);
    }
}
