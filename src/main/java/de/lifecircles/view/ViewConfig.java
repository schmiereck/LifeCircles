package de.lifecircles.view;

import javafx.scene.paint.Color;

/**
 * Configuration settings for the visualization.
 */
public class ViewConfig {
    private static final ViewConfig INSTANCE = new ViewConfig();

    // Background and text
    private Color backgroundColor = Color.BLACK;
    private Color textColor = Color.WHITE;

    // Cell rendering
    private boolean showCellBodies = true;
    private boolean showCellTypes = true;
    private double cellOutlineWidth = 2.0;
    private Color cellOutlineColor = Color.WHITE;

    // Actor rendering
    private boolean showActors = true;
    private double actorSize = 4.0;
    private boolean showForceFields = true;
    private double forceFieldOpacity = 0.3;

    // Debug visualization
    private boolean showDebugInfo = false;
    private boolean showGrid = false;
    private double gridSpacing = 50.0;
    private Color gridColor = Color.GRAY.deriveColor(0, 1, 1, 0.2);

    // Cell state visualization
    private boolean showEnergy = true;
    private boolean showAge = false;
    private boolean showSpecialization = true;
    // Sun ray visualization
    private boolean showSunRays = true;
    private Color energyBarColor = Color.GREEN;
    private Color ageBarColor = Color.BLUE;

    private ViewConfig() {}

    public static ViewConfig getInstance() {
        return INSTANCE;
    }

    // Getters and setters
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public boolean isShowCellBodies() {
        return showCellBodies;
    }

    public void setShowCellBodies(boolean showCellBodies) {
        this.showCellBodies = showCellBodies;
    }

    public boolean isShowCellTypes() {
        return showCellTypes;
    }

    public void setShowCellTypes(boolean showCellTypes) {
        this.showCellTypes = showCellTypes;
    }

    public double getCellOutlineWidth() {
        return cellOutlineWidth;
    }

    public void setCellOutlineWidth(double cellOutlineWidth) {
        this.cellOutlineWidth = cellOutlineWidth;
    }

    public Color getCellOutlineColor() {
        return cellOutlineColor;
    }

    public void setCellOutlineColor(Color cellOutlineColor) {
        this.cellOutlineColor = cellOutlineColor;
    }

    public boolean isShowActors() {
        return showActors;
    }

    public void setShowActors(boolean showActors) {
        this.showActors = showActors;
    }

    public boolean isShowEnergy() {
        return showEnergy;
    }

    public void setShowEnergy(boolean showEnergy) {
        this.showEnergy = showEnergy;
    }

    public boolean isShowAge() {
        return showAge;
    }

    public void setShowAge(boolean showAge) {
        this.showAge = showAge;
    }

    public boolean isShowSpecialization() {
        return showSpecialization;
    }

    public void setShowSpecialization(boolean showSpecialization) {
        this.showSpecialization = showSpecialization;
    }

    public double getActorSize() {
        return actorSize;
    }

    public void setActorSize(double actorSize) {
        this.actorSize = actorSize;
    }

    public boolean isShowForceFields() {
        return showForceFields;
    }

    public void setShowForceFields(boolean showForceFields) {
        this.showForceFields = showForceFields;
    }

    public double getForceFieldOpacity() {
        return forceFieldOpacity;
    }

    public void setForceFieldOpacity(double forceFieldOpacity) {
        this.forceFieldOpacity = forceFieldOpacity;
    }

    public boolean isShowDebugInfo() {
        return showDebugInfo;
    }

    public void setShowDebugInfo(boolean showDebugInfo) {
        this.showDebugInfo = showDebugInfo;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public double getGridSpacing() {
        return gridSpacing;
    }

    public void setGridSpacing(double gridSpacing) {
        this.gridSpacing = gridSpacing;
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
    }

    public Color getEnergyBarColor() {
        return energyBarColor;
    }

    public void setEnergyBarColor(Color energyBarColor) {
        this.energyBarColor = energyBarColor;
    }

    public Color getAgeBarColor() {
        return ageBarColor;
    }

    public void setAgeBarColor(Color ageBarColor) {
        this.ageBarColor = ageBarColor;
    }

    // Sun ray visualization accessors
    public boolean isShowSunRays() {
        return showSunRays;
    }

    public void setShowSunRays(boolean showSunRays) {
        this.showSunRays = showSunRays;
    }
}
