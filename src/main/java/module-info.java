module de.lifecircles {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.prefs;

    exports de.lifecircles;
    exports de.lifecircles.model;
    exports de.lifecircles.model.neural;
    exports de.lifecircles.service;
    exports de.lifecircles.service.dto;
    exports de.lifecircles.view;
    exports de.lifecircles.service.trainStrategy;
    exports de.lifecircles.service.partitioningStrategy;
}
