package de.lifecircles.view;

import de.lifecircles.service.SimulationConfig;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Logarithmic live brain-cost control, separate from the environmental supply. */
public class CellPanel extends VBox {
    public CellPanel() {
        SimulationConfig config=SimulationConfig.getInstance();
        setPadding(new Insets(10));setStyle("-fx-background-color: #333333;");
        Label value=new Label();
        Slider cost=new Slider(0,5,toPosition(config.getBrainEnergyCosts().work()));
        cost.setId("brain-energy-slider");cost.setPrefWidth(270);
        cost.setShowTickLabels(true);cost.setShowTickMarks(true);cost.setMajorTickUnit(1);cost.setMinorTickCount(0);
        cost.setBlockIncrement(.1);
        cost.setLabelFormatter(new StringConverter<>() {
            @Override public String toString(Double position) {return position==0?"0":String.format("%.3g×",multiplier(position));}
            @Override public Double fromString(String value) {return toPosition(Double.parseDouble(value));}
        });
        Runnable refresh=()->value.setText(String.format("Brain energy: %.3g× (1× = previous standard)",config.getBrainEnergyCosts().work()));
        cost.valueProperty().addListener((obs,old,next)->{config.setBrainEnergyMultiplier(multiplier(next.doubleValue()));refresh.run();});
        refresh.run();
        Label hint=new Label("Storage + computation, 0–1000×.\nBasic metabolism stays constant.");
        cost.setTooltip(new Tooltip("Applies on the next simulation step."));
        Label lifespanValue=new Label();
        Slider lifespan=new Slider(Math.min(1,Math.log10(config.getCellDeathAge())),
                Math.max(4,Math.log10(config.getCellDeathAge())),Math.log10(config.getCellDeathAge()));
        lifespan.setId("cell-lifespan-slider");lifespan.setPrefWidth(270);
        lifespan.setTooltip(new Tooltip("Maximum age in simulation seconds, not wall time.\nAlready older cells die at the next age check."));
        lifespan.setShowTickLabels(true);lifespan.setShowTickMarks(true);
        lifespan.setMajorTickUnit(1);lifespan.setMinorTickCount(0);lifespan.setBlockIncrement(.1);
        lifespan.setLabelFormatter(new StringConverter<>() {
            @Override public String toString(Double position) {return String.format("%.0f s",Math.pow(10,position));}
            @Override public Double fromString(String text) {return Math.log10(Double.parseDouble(text));}
        });
        Runnable refreshLifespan=()->lifespanValue.setText(String.format("Max. lifespan: %.0f simulation s",config.getCellDeathAge()));
        lifespan.valueProperty().addListener((obs,old,next)-> {
            config.setCellDeathAge(Math.max(1,Math.round(Math.pow(10,next.doubleValue()))));
            refreshLifespan.run();
        });
        refreshLifespan.run();
        Label lifespanHint=new Label("Existing older cells also expire.");
        VBox content=new VBox(6,value,cost,hint,lifespanValue,lifespan,lifespanHint);content.setPadding(new Insets(5));
        getChildren().add(new TitledPane("Cell",content));
    }
    /** Continuous near zero, then logarithmic from 0.1× to 1000×. */
    public static double multiplier(double position) {return position<=1?Math.max(0,position)*.1:Math.pow(10,position-2);}
    public static double toPosition(double multiplier) {return multiplier<=.1?multiplier*10:2+Math.log10(multiplier);}
}
