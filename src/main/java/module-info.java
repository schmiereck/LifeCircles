module de.lifecircles {
    requires java.management;
    requires jdk.management;
    requires javafx.controls;
    requires javafx.graphics;
    requires java.prefs;
    requires java.desktop; // AWT/ImageIO for headless study snapshots (tests run inside this module)

    exports de.lifecircles;
    exports de.lifecircles.model;
    exports de.lifecircles.model.neural;
    exports de.lifecircles.service;
    exports de.lifecircles.service.dto;
    exports de.lifecircles.view;
    exports de.lifecircles.service.trainStrategy;
    exports de.lifecircles.service.partitioningStrategy;
}
