package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Environment;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/** Studies may lengthen HIGH_POSITION2 generations; the default stays 50 s. */
class HighPosition2GenerationLengthTest {
    static int selections(Integer generationSteps, int steps) {
        int[] calls={0};
        HighPosition2TrainStrategy.EvaluationObserver observer=(world,fitness,winners)->calls[0]++;
        var strategy=generationSteps==null?new HighPosition2TrainStrategy(new Random(1),observer)
                :new HighPosition2TrainStrategy(new Random(1),observer,generationSteps);
        Environment world=strategy.initializeEnvironment();strategy.initialize(world);
        for(int i=0;i<steps;i++)strategy.selectAndMutate(world);
        return calls[0];
    }

    @Test void selectsAfterTheConfiguredNumberOfSteps() {
        assertEquals(0,selections(null,2999));
        assertEquals(1,selections(null,3000));
        assertEquals(1,selections(7200,7200*2-1));
        assertEquals(2,selections(7200,7200*2));
        assertThrows(IllegalArgumentException.class,()->new HighPosition2TrainStrategy(new Random(1),null,600));
    }
}
