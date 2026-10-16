package de.lifecircles.view;

import de.lifecircles.service.SimulationConfig;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
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
        Label hint=new Label("Storage + computation, 0–1000×.\nBasic metabolism stays constant.\nApplies on the next simulation step.");
        VBox content=new VBox(10,value,cost,hint);content.setPadding(new Insets(5));
        getChildren().add(new TitledPane("Cell",content));
    }
    /** Continuous near zero, then logarithmic from 0.1× to 1000×. */
    public static double multiplier(double position) {return position<=1?Math.max(0,position)*.1:Math.pow(10,position-2);}
    public static double toPosition(double multiplier) {return multiplier<=.1?multiplier*10:2+Math.log10(multiplier);}
}
