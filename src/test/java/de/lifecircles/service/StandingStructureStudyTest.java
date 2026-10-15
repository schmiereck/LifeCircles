package de.lifecircles.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;

/** Ground-supported columns with full gravity; no growth, division or imposed bonds. */
@EnabledIfSystemProperty(named="standing.study",matches="true")
class StandingStructureStudyTest {
    @Test void measureStandingColumns() throws Exception {
        StringBuilder csv = new StringBuilder("profile,radius,columns,rows,cells,dt,seconds,height_ratio,width_ratio,shape_R,component,late_speed,max_overlap\n");
        try (var saved = new MechanicsLab.Settings()) {
            for (var profile : List.of(
                    new MechanicsLab.Parameters("current",90,125,.8,.8,.25),
                    new MechanicsLab.Parameters("balanced",90,125,.8,3,.25),
                    new MechanicsLab.Parameters("stiffer_contact",90,250,.8,3,.25)))
                for (int columns : new int[]{1,3,5}) for (int rows : new int[]{4,8,12}) {
                    double radius = Double.parseDouble(System.getProperty("standing.radius","12"));
                    double dt = Double.parseDouble(System.getProperty("standing.dt","0.016666666666666666"));
                    var result = StandingStructureLab.run(profile,columns,rows,radius,dt);
                    String line = String.format(Locale.ROOT,"%s,%.3f,%d,%d,%d,%.9f,30,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                            profile.name(),radius,columns,rows,columns*rows,dt,result.heightRatio(),result.widthRatio(),
                            result.shape(),result.component(),result.speed(),result.overlap());
                    csv.append(line); System.out.print(line);
                }
        }
        Path output = Path.of(System.getProperty("standing.output","target/standing-structures.csv")); Files.writeString(output,csv);
    }

}
