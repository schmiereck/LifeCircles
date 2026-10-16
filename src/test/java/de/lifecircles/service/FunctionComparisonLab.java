package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Clonal developmental assays. Frozen commands are learned on an independent calibration episode. */
final class FunctionComparisonLab {
    static final double DT=1./60;
    static final int OUTPUTS=CellFactory.calcOutputCount(12);
    record Profiles(double[][] mean,long[] counts,double[] rms) {}
    record Result(double cellSeconds,double energySeconds,long births,long deaths,int cells,double energy,
                  double meanExecuted,double commandDeviation,int steps,String status) {}

    static Cell founder(Cell genome,Vector2D position,int state) {
        var net=new NeuralNetwork(((CellBrain)genome.getBrain()).getNeuralNetwork());
        Cell cell=new Cell(position,10,new CellBrain(net));
        cell.setType(genome.getType());cell.setEnergy(.75);cell.setAge(9);cell.setCellState(state);
        cell.setGeneration(genome.getGeneration());
        cell.setMutationRateFactor(genome.getMutationRateFactor());cell.setMutationStrengthFactor(genome.getMutationStrengthFactor());
        CellBrainService.think(cell);
        for(var actor:cell.getSensorActors())actor.updateCachedPosition();
        return cell;
    }
    static Environment world(Cell genome,long seed) {
        var world=new Environment(600,400,new Random(seed));world.addGroundBlocker();
        for(int i=0;i<8;i++)world.addCell(founder(genome,new Vector2D((i+.5)*75,339),genome.getCellState()));
        return world;
    }
    static final class Policy implements SimulationStepPolicy {
        final BrainEnergyCosts costs;final double[][] frozen;long births,deaths;
        Policy(BrainEnergyCosts costs,double[][] frozen){this.costs=costs;this.frozen=frozen;}
        public BrainEnergyCosts brainEnergyCosts(){return costs;}
        public void afterThinking(Cell cell){if(frozen!=null)CellBrainService.applyOutputs(cell,frozen[cell.getCellState()&7]);}
        public boolean isDead(Cell cell){boolean dead=SimulationStepPolicy.super.isDead(cell);if(dead)deaths++;return dead;}
        public Cell reproduce(Environment world,Cell parent){return ReproductionManagerService.reproduceClonally(SimulationConfig.getInstance(),world,parent);}
        public void onBirth(Cell parent,Cell child){births++;afterThinking(child);}
    }
    static double[] commands(Cell cell) {
        double[] values=new double[OUTPUTS];
        for(int j=0;j<values.length;j++){values[j]=cell.getBrain().getOutputValue(j);assertTrue(Double.isFinite(values[j]));}
        return values;
    }
    static Profiles calibrate(Cell genome,BrainEnergyCosts costs,int steps) {
        double[][] sum=new double[8][OUTPUTS],squares=new double[8][OUTPUTS];long[] counts=new long[8];
        var world=world(genome,991000);var policy=new Policy(costs,null);var grid=new SpatialGridPartitioningStrategy(600,400,60);
        for(int step=0;step<steps;step++) {
            grid.build(world.getCellList());world.update(DT,grid,policy);
            for(Cell cell:world.getCellList()) {
                int state=cell.getCellState()&7;double[] out=commands(cell);counts[state]++;
                for(int j=0;j<OUTPUTS;j++){sum[state][j]+=out[j];squares[state][j]+=out[j]*out[j];}
            }
            if(world.getCellList().size()>=2000)throw new IllegalStateException("Calibration population cap reached");
        }
        double[] rms=new double[8];
        for(int state=0;state<8;state++) {
            if(counts[state]>0) {
                for(int j=0;j<OUTPUTS;j++) {
                    sum[state][j]/=counts[state];
                    rms[state]+=Math.max(0,squares[state][j]/counts[state]-sum[state][j]*sum[state][j]);
                }
                rms[state]=Math.sqrt(rms[state]/OUTPUTS);
            } else {
                Cell probe=founder(genome,new Vector2D(300,339),state);
                for(int t=0;t<64;t++)CellBrainService.think(probe,CellBrainService.generateInputs(probe,world));
                sum[state]=commands(probe);rms[state]=Double.NaN;
            }
        }
        return new Profiles(sum,counts,rms);
    }
    interface Progress {void record(int step,Environment world,Policy policy);}
    static Result run(Cell genome,BrainEnergyCosts costs,double[][] frozen,long seed,int steps,Progress progress) {
        var world=world(genome,seed);var policy=new Policy(costs,frozen);
        if(frozen!=null)for(Cell cell:world.getCellList())policy.afterThinking(cell);
        var grid=new SpatialGridPartitioningStrategy(600,400,60);
        double cellsIntegral=0,energyIntegral=0,executed=0,deviation=0;long decisions=0;
        String status="completed";int step;
        for(step=1;step<=steps;step++) {
            int before=world.getCellList().size();double energyBefore=energy(world);
            grid.build(world.getCellList());world.update(DT,grid,policy);
            cellsIntegral+=(before+world.getCellList().size())*.5*DT;
            energyIntegral+=(energyBefore+energy(world))*.5*DT;
            // Checkpoint sampling is sufficient for diagnostic output variation, not used for selection.
            if(step%60==0)for(Cell cell:world.getCellList()) {
                executed+=cell.getBrain().getProccessedSynapses();decisions++;
                if(frozen!=null) {
                    double[] out=commands(cell),reference=frozen[cell.getCellState()&7];
                    for(int j=0;j<OUTPUTS;j++)deviation+=(out[j]-reference[j])*(out[j]-reference[j])/OUTPUTS;
                }
                assertTrue(Double.isFinite(cell.getPosition().getX()) && Double.isFinite(cell.getPosition().getY()) && Double.isFinite(cell.getEnergy()));
            }
            if(progress!=null && (step%600==0 || step==steps))progress.record(step,world,policy);
            if(world.getCellList().isEmpty()){status="extinct";break;}
            if(world.getCellList().size()>=2000){status="cap_stop";break;}
        }
        step=Math.min(step,steps);
        return new Result(cellsIntegral,energyIntegral,policy.births,policy.deaths,world.getCellList().size(),energy(world),
                executed/Math.max(1,decisions),Math.sqrt(deviation/Math.max(1,decisions)),step,status);
    }
    static double energy(Environment world){return world.getCellList().stream().mapToDouble(Cell::getEnergy).sum();}
}
