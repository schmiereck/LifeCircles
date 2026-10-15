package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.TestTrainStrategyUtils;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Output interventions only: real gravity, collisions, sensing and brain execution. */
final class ControllerStabilityLab {
    static final int SIZE=1, FORCE=2, COLOR=4;
    static final double ENERGY=.75;
    static CellBrainInterface uniformBrain(double attraction) {
        Environment fixture=new Environment(1400,1200);
        TestTrainStrategyUtils.createAndAddCell(fixture,700,1130,20,true,false,false,false);
        var brain=fixture.getCellList().getFirst().getBrain();
        double[] outputs=brain.process();
        for(int a=0;a<12;a++) outputs[GlobalOutputFeature.values().length
                +a*ActorOutputFeature.values().length+ActorOutputFeature.FORCE.ordinal()]=(1-attraction/90)/2;
        return brain;
    }
    record Result(double height, double width, double shape, double speed, double radius,
                  double attractiveFraction, double meanSignedForce, double meanAbsoluteForce,
                  double minForce, double maxForce, double contacts) {}

    static final class InterventionBrain implements CellBrainInterface {
        final CellBrainInterface delegate;
        final int mask;
        final boolean frozen;
        final double[] outputs=new double[CellFactory.calcOutputCount(12)];
        double[] first;
        InterventionBrain(CellBrainInterface delegate,int mask,boolean frozen) {
            this.delegate=delegate; this.mask=mask; this.frozen=frozen;
        }
        public double[] process() {
            double[] proposed=delegate.process();
            if(first==null) first=proposed.clone();
            double[] source=frozen?first:proposed;
            Arrays.fill(outputs,0);
            outputs[GlobalOutputFeature.SIZE.ordinal()]=(mask&SIZE)!=0?source[GlobalOutputFeature.SIZE.ordinal()]:1;
            for(int a=0;a<12;a++) {
                int b=GlobalOutputFeature.values().length+a*ActorOutputFeature.values().length;
                outputs[b+ActorOutputFeature.TYPE_RED.ordinal()]=(mask&COLOR)!=0?source[b+ActorOutputFeature.TYPE_RED.ordinal()]:0;
                outputs[b+ActorOutputFeature.TYPE_GREEN.ordinal()]=(mask&COLOR)!=0?source[b+ActorOutputFeature.TYPE_GREEN.ordinal()]:1;
                outputs[b+ActorOutputFeature.TYPE_BLUE.ordinal()]=(mask&COLOR)!=0?source[b+ActorOutputFeature.TYPE_BLUE.ordinal()]:0;
                outputs[b+ActorOutputFeature.FORCE.ordinal()]=(mask&FORCE)!=0?source[b+ActorOutputFeature.FORCE.ordinal()]:0;
            }
            return outputs;
        }
        public int getSynapseCount(){return delegate.getSynapseCount();}
        public boolean[] determineActiveHiddenLayers(int state){return delegate.determineActiveHiddenLayers(state);}
        public void setInputs(double[] inputs){delegate.setInputs(inputs);}
        public int getInputCount(){return delegate.getInputCount();}
        public long getProccessedSynapses(){return delegate.getProccessedSynapses();}
        public NeuralNetwork mutate(double rate,double strength){throw new UnsupportedOperationException("Assay only");}
        public double getOutputValue(int index){return outputs[index];}
        public double getInputValue(int index){return delegate.getInputValue(index);}
    }

    static Result run(Supplier<CellBrainInterface> program,CellType type,int state,int mask,boolean frozen,
                      double dt, double seconds,StringBuilder trajectory,String label) throws Exception {
        Environment world=new Environment(1400,1200); world.addGroundBlocker();
        var brainField=Cell.class.getDeclaredField("brain"); brainField.setAccessible(true);
        for(int row=0;row<12;row++) for(int col=0;col<5;col++) {
            double x=700+(col-2)*40+(row%2)*20+(row==11?.2:0);
            Cell cell=new Cell(new Vector2D(x,1130-row*Math.sqrt(3)*20),20,program.get());
            cell.setType(type); cell.setCellState(state); cell.setEnergy(ENERGY);
            // Apply state gates while the concrete CellBrain is still installed.
            brainField.set(cell,new InterventionBrain(cell.getBrain(),mask,frozen));
            world.addCell(cell);
        }
        List<Cell> body=new ArrayList<>(world.getCellList());
        double[][] reference=MechanicsLab.positions(body);
        double initialHeight=MechanicsLab.height(body),initialWidth=width(body);
        Set<Long> initialContacts=contacts(body);
        var grid=new SpatialGridPartitioningStrategy(1400,1200,80);
        double lateSpeed=0; int samples=0;
        int steps=(int)Math.round(seconds/dt),sampleEvery=(int)Math.round(1/dt);
        Result result=null;
        for(int step=0;step<=steps;step++) {
            if(step%sampleEvery==0 || step==steps) {
                result=measure(body,reference,initialHeight,initialWidth,initialContacts,lateSpeed/Math.max(1,samples));
                if(trajectory!=null) trajectory.append(csv(label,step*dt,result));
            }
            if(step==steps) break;
            // Same energy input for every controller; no sunlight, transfer or division.
            for(Cell cell:body) cell.setEnergy(ENERGY);
            grid.build(body); world.update(dt,grid);
            assertEquals(60,world.getCellList().size(),"Birth/death would invalidate this assay");
            for(Cell cell:body) {
                assertTrue(Double.isFinite(cell.getPosition().getX()));
                assertTrue(Double.isFinite(cell.getPosition().getY()));
            }
            if(step*dt>=seconds-5) {lateSpeed+=body.stream().mapToDouble(c->c.getVelocity().length()).average().orElseThrow();samples++;}
        }
        return result;
    }
    static String header(){return "program,mask,frozen,dt,seconds,height_ratio,width_ratio,shape_R,late_speed,mean_radius,attractive_fraction,mean_signed_force,mean_absolute_force,min_force,max_force,original_contacts\n";}
    static String csv(String label,double seconds,Result r) {
        return String.format(Locale.ROOT,"%s,%.6f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",label,seconds,
                r.height,r.width,r.shape,r.speed,r.radius,r.attractiveFraction,r.meanSignedForce,
                r.meanAbsoluteForce,r.minForce,r.maxForce,r.contacts);
    }
    private static Result measure(List<Cell> body,double[][] reference,double h,double w,Set<Long> initial,double speed) {
        var current=contacts(body); long retained=initial.stream().filter(current::contains).count();
        var actors=body.stream().flatMap(c->c.getSensorActors().stream()).toList();
        return new Result(MechanicsLab.height(body)/h,width(body)/w,MechanicsLab.shapeError(reference,body,20),speed,
                body.stream().mapToDouble(Cell::getRadiusSize).average().orElseThrow(),
                actors.stream().filter(a->a.getForceStrength()<0).count()/(double)actors.size(),
                actors.stream().mapToDouble(SensorActor::getForceStrength).average().orElseThrow(),
                actors.stream().mapToDouble(a->Math.abs(a.getForceStrength())).average().orElseThrow(),
                actors.stream().mapToDouble(SensorActor::getForceStrength).min().orElseThrow(),
                actors.stream().mapToDouble(SensorActor::getForceStrength).max().orElseThrow(),retained/(double)initial.size());
    }
    private static double width(List<Cell> body) {
        return body.stream().mapToDouble(c->c.getPosition().getX()+c.getRadiusSize()).max().orElseThrow()
                -body.stream().mapToDouble(c->c.getPosition().getX()-c.getRadiusSize()).min().orElseThrow();
    }
    private static Set<Long> contacts(List<Cell> body) {
        Set<Long> result=new HashSet<>();
        for(int i=0;i<body.size();i++) for(int j=i+1;j<body.size();j++) {
            Cell a=body.get(i),b=body.get(j);
            if(a.getPosition().distance(b.getPosition())<=a.getRadiusSize()+b.getRadiusSize()+.1*Math.min(a.getRadiusSize(),b.getRadiusSize()))
                result.add((long)i*body.size()+j);
        }
        return result;
    }
}
