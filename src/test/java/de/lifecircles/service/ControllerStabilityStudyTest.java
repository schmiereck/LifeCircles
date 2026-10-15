package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;

@EnabledIfSystemProperty(named="controller.study",matches="true")
class ControllerStabilityStudyTest {
    @Test void uniformAttractionSweep() throws Exception {
        Path output=Path.of(System.getProperty("controller.output","target/controller-stability"));
        Files.createDirectories(output);
        var config=SimulationConfig.getInstance(); double age=config.getReproductionAgeThreshold();
        StringBuilder csv=new StringBuilder(ControllerStabilityLab.header());
        try(var saved=new MechanicsLab.Settings()) {
            config.setReproductionAgeThreshold(Double.MAX_VALUE);
            MechanicsLab.configure(new MechanicsLab.Parameters("current",90,125,.8,.8,.25));
            for(int strength:new int[]{90,75,60,45,30,15,0}) {
                String label="uniform-minus-"+strength+",2,true,0.016666667";
                var result=ControllerStabilityLab.run(()->ControllerStabilityLab.uniformBrain(strength),
                        new CellType(0,1,0),0,2,true,1.0/60,30,null,label);
                csv.append(ControllerStabilityLab.csv(label,30,result));
                System.out.print(ControllerStabilityLab.csv(label,30,result));
            }
        } finally {config.setReproductionAgeThreshold(age);}
        Files.writeString(output.resolve("uniform-attraction.csv"),csv);
    }

    @Test void isolateControllerChannels() throws Exception {
        Path input=Path.of(System.getProperty("controller.input","experiments/high-position2-2026-10-15"));
        Path output=Path.of(System.getProperty("controller.output","target/controller-stability"));
        Files.createDirectories(output);
        StringBuilder summary=new StringBuilder(ControllerStabilityLab.header()),trajectory=new StringBuilder(ControllerStabilityLab.header());
        SimulationConfig config=SimulationConfig.getInstance();
        double age=config.getReproductionAgeThreshold();
        try(var saved=new MechanicsLab.Settings()) {
            MechanicsLab.configure(new MechanicsLab.Parameters("current",90,125,.8,.8,.25));
            config.setReproductionAgeThreshold(Double.MAX_VALUE);
            for(String source:List.of("standard","more-sun")) {
                List<Cell> donors=FileService.getInstance().loadCellsFromFile(input.resolve(source+"-best-parents.cells").toString());
                for(int index=0;index<Math.min(2,donors.size());index++) {
                    Cell donor=donors.get(index);
                    for(boolean frozen:new boolean[]{true,false}) for(int mask=0;mask<8;mask++) {
                        String label=source+"-"+index+","+mask+","+frozen+",0.016666667";
                        var result=ControllerStabilityLab.run(()->new CellBrain(donor.getBrain().mutate(0,0)),
                                donor.getType(),donor.getCellState(),mask,frozen,1.0/60,30,trajectory,label);
                        String line=ControllerStabilityLab.csv(label,30,result);
                        summary.append(line); System.out.print(line);
                        Files.writeString(output.resolve("summary.csv"),summary);
                        Files.writeString(output.resolve("trajectory.csv"),trajectory);
                    }
                    // Verify the fully released controller at twice the temporal resolution.
                    String label=source+"-"+index+",7,false,0.008333333";
                    summary.append(ControllerStabilityLab.csv(label,30,ControllerStabilityLab.run(
                            ()->new CellBrain(donor.getBrain().mutate(0,0)),donor.getType(),donor.getCellState(),7,false,1.0/120,30,null,label)));
                }
            }
        } finally {config.setReproductionAgeThreshold(age);}
        Files.writeString(output.resolve("summary.csv"),summary);
        Files.writeString(output.resolve("trajectory.csv"),trajectory);
    }
}
