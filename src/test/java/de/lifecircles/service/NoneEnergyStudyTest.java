package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.DefaultTrainStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.DoubleAdder;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in paired continuations with real NONE metabolism, sunlight and biological births. */
@EnabledIfSystemProperty(named="none.energy.file", matches=".+")
class NoneEnergyStudyTest {
    static final double DT = 1.0/60;
    static final BrainEnergyCosts[] PRICES = {
            BrainEnergyCosts.DEFAULT, new BrainEnergyCosts(1,.1), new BrainEnergyCosts(100,.1)};

    static final class Policy implements SimulationStepPolicy {
        final BrainEnergyCosts costs;
        final Random births;
        final DoubleAdder charged = new DoubleAdder(), decisions = new DoubleAdder(), executed = new DoubleAdder();
        final IdentityHashMap<Cell,Integer> lineage = new IdentityHashMap<>();
        long born, dead;
        Policy(BrainEnergyCosts costs, long seed, List<Cell> founders) {
            this.costs=costs; births=new Random(seed);
            for(int i=0;i<founders.size();i++) lineage.put(founders.get(i),i);
        }
        public BrainEnergyCosts brainEnergyCosts(){return costs;}
        public void afterThinking(Cell cell) {
            charged.add(costs.cost(cell.getBrain().getSynapseCount(),cell.getBrain().getProccessedSynapses(),DT));
            decisions.add(1);executed.add(cell.getBrain().getProccessedSynapses());
        }
        public boolean isDead(Cell cell) {
            boolean result=SimulationStepPolicy.super.isDead(cell);
            if(result){dead++;lineage.remove(cell);} return result;
        }
        public Cell reproduce(Environment world, Cell parent) {
            return ReproductionManagerService.reproduce(SimulationConfig.getInstance(),world,parent,births);
        }
        public void onBirth(Cell parent,Cell child){born++;lineage.put(child,lineage.get(parent));}
    }

    @Test void compareCostsInNoneWorlds() throws Exception {
        Path source=Path.of(System.getProperty("none.energy.file"));
        Path out=Path.of(System.getProperty("none.energy.output","target/none-energy"));
        Files.createDirectories(out);
        byte[] bytes=Files.readAllBytes(source);
        int steps=Integer.getInteger("none.energy.steps",36000), seeds=Integer.getInteger("none.energy.seeds",3);
        int interval=Integer.getInteger("none.energy.interval",600), cap=Integer.getInteger("none.energy.cap",2000);
        int checkpointInterval=Integer.getInteger("none.energy.checkpoint_interval",0);
        long offset=Long.getLong("none.energy.seed_offset",0);
        int foundersPerSection=Integer.getInteger("none.energy.founders_per_section",0);
        double width=Double.parseDouble(System.getProperty("none.energy.width","11200"));
        double height=Double.parseDouble(System.getProperty("none.energy.height","1200"));
        boolean floor=Boolean.getBoolean("none.energy.floor");
        assertTrue(steps>0 && seeds>0 && interval>0 && cap>0 && checkpointInterval>=0);
        SimulationConfig c=SimulationConfig.getInstance();Random original=NeuralNetwork.getRandom();
        int initial=c.getInitialCellCount();double oldW=c.getWidth(),oldH=c.getHeight();
        String header="seed,storage,work,step,seconds,cells,births,deaths,founder_lineages,total_energy,mean_energy,mean_stored,mean_executed,mean_root_neurons,mean_generation,max_generation,states,contact_fraction,mean_abs_force,brain_cost_requested,decisions,step_wall_ms,status,state_hash\n";
        Files.writeString(out.resolve("trajectory.csv"),header);Files.writeString(out.resolve("results.csv"),header);
        Files.writeString(out.resolve("phases.csv"),"seed,storage,work,phase,calls,total_ms\n");
        Files.writeString(out.resolve("settings.txt"),"source="+source+"\nsha256="+hex(MessageDigest.getInstance("SHA-256").digest(bytes))
                +"\nsteps="+steps+"\nseeds="+seeds+"\nseed_offset="+offset+"\ninterval="+interval+"\ncheckpoint_interval="+checkpointInterval+"\ncap_stop="+cap
                +"\nfounders_per_section="+foundersPerSection+"\nfloor_layout="+floor+"\nwidth="+width+"\nheight="+height+"\ndt="+DT+"\nsun_rate="+c.getSunRayRate()+"\nsun_spacing="+c.getSunRaySpacingPx()
                +"\nenergy_per_ray="+c.getEnergyPerRay()+"\nmutation_rate="+c.getMutationRate()+"\nmutation_strength="+c.getMutationStrength()
                +"\ndivision_multiplier="+c.getDivisionMutationRateMultiplier()+"\ninput_normalization="+c.isNormalizeBrainInputs()
                +"\nDefault layout: NONE obstacles and imported coordinates preserved. Floor layout: ground only, founders equally spaced above floor with zero velocity."
                +"\nImported energy/age/genomes preserved, no random founders, no training/refill/reseeding."
                +"\nSun, network mutation and birth traits/meta-mutation seeded independently. Price order rotated by seed."
                +"\nNo UI snapshots; step_wall_ms includes grid and profiling, excludes checkpoints/serialization. CPU comparisons are descriptive."
                +"\nbrain_cost_requested is cumulative before energy-zero clamping; newborn initial thinking is not a billed world step."
                +"\nContact and output diversity are descriptive, not demonstrated biological functions. Cap stops entire trial, never truncates population.\n");
        try {
            c.setWidth(width);c.setHeight(height);c.setInitialCellCount(0);
            for(int s=0;s<seeds;s++) for(int order=0;order<PRICES.length;order++) {
                long seed=offset+s;BrainEnergyCosts price=PRICES[(s+order)%PRICES.length];
                NeuralNetwork.setRandom(new Random(900001+seed));
                Environment world=new Environment(width,height,new Random(700001+seed));
                if(floor) world.addGroundBlocker();else new DefaultTrainStrategy().initialize(world);
                world.resetCells(selectFounders(load(bytes),foundersPerSection));
                if(floor) for(int i=0;i<world.getCellList().size();i++) {
                    Cell cell=world.getCellList().get(i);
                    cell.setPosition(new Vector2D((i+.5)*width/world.getCellList().size(),height-Environment.GroundBlockerHeight-cell.getRadiusSize()-1));
                    cell.setVelocity(new Vector2D(0,0));cell.setAngularVelocity(0);
                    for(var actor:cell.getSensorActors()) actor.updateCachedPosition();
                }
                Policy policy=new Policy(price,800001+seed,world.getCellList());
                var grid=new SpatialGridPartitioningStrategy(width,height,2*(c.getCellMaxRadiusSize()+c.getCellActorMaxFieldRadius()));
                world.getProfiler().setEnabled(true);
                String name="seed-"+seed+"-s"+price.storage()+"-w"+price.work();
                Path folder=out.resolve(name);Files.createDirectories(folder);
                save(folder.resolve("start.cells"),world.getCellList());
                append(out.resolve("trajectory.csv"),row(seed,price,0,world,policy,0,"start"));
                long elapsed=0;int step=0;String status="completed";
                for(step=1;step<=steps;step++) {
                    long begin=System.nanoTime();
                    long phase=world.getProfiler().start();grid.build(world.getCellList());
                    world.getProfiler().finish(SimulationProfiler.Phase.GRID,phase);
                    world.update(DT,grid,policy);elapsed+=System.nanoTime()-begin;
                    if(world.getCellList().isEmpty()) status="extinct";
                    else if(world.getCellList().size()>=cap) status="cap_stop";
                    if(checkpointInterval>0 && step%checkpointInterval==0) {
                        save(folder.resolve("step-"+step+".cells"),world.getCellList());
                        StringBuilder ancestry=new StringBuilder("cell,founder_lineage\n");
                        for(int i=0;i<world.getCellList().size();i++) ancestry.append(i).append(',')
                                .append(policy.lineage.get(world.getCellList().get(i))).append('\n');
                        Files.writeString(folder.resolve("step-"+step+"-lineages.csv"),ancestry);
                    }
                    if(step%interval==0 || !status.equals("completed") || step==steps) {
                        String line=row(seed,price,step,world,policy,elapsed/1e6/step,status);
                        append(out.resolve("trajectory.csv"),line);
                        System.out.printf(Locale.ROOT,"NONE %s step %d/%d cells %d births %d deaths %d %.3f ms/step %s%n",
                                name,step,steps,world.getCellList().size(),policy.born,policy.dead,elapsed/1e6/step,status);
                    }
                    if(!status.equals("completed")) break;
                }
                step=Math.min(step,steps);
                append(out.resolve("results.csv"),row(seed,price,step,world,policy,elapsed/1e6/step,status));
                for(var e:world.getProfiler().snapshot().entrySet()) append(out.resolve("phases.csv"),
                        String.format(Locale.ROOT,"%d,%.1f,%.1f,%s,%d,%.6f%n",seed,price.storage(),price.work(),e.getKey(),e.getValue().calls(),e.getValue().nanos()/1e6));
                save(folder.resolve("end.cells"),world.getCellList());
            }
        } finally {NeuralNetwork.setRandom(original);c.setWidth(oldW);c.setHeight(oldH);c.setInitialCellCount(initial);}
    }

    @SuppressWarnings("unchecked") static List<Cell> load(byte[] bytes) throws Exception {
        try(var in=new ObjectInputStream(new ByteArrayInputStream(bytes))){return (List<Cell>)in.readObject();}
    }
    /** Deterministic bottleneck: evenly spaced file-order representatives of each former 1400px training section. */
    static List<Cell> selectFounders(List<Cell> cells,int perSection) {
        if(perSection<0) throw new IllegalArgumentException("Negative founder count");
        if(perSection==0) return cells;
        Map<Integer,List<Cell>> groups=new TreeMap<>();
        for(Cell cell:cells) groups.computeIfAbsent((int)(cell.getPosition().getX()/1400),k->new ArrayList<>()).add(cell);
        List<Cell> selected=new ArrayList<>();
        for(var group:groups.values()) {
            int count=Math.min(perSection,group.size());
            for(int i=0;i<count;i++) selected.add(group.get(i*group.size()/count));
        }
        return selected;
    }
    static void save(Path path,List<Cell> cells) throws Exception {
        try(var out=new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))){out.writeObject(new ArrayList<>(cells));}
    }
    static void append(Path path,String line) throws Exception {Files.writeString(path,line,StandardOpenOption.APPEND);}
    static String hex(byte[] bytes){return HexFormat.of().formatHex(bytes);}
    static String row(long seed,BrainEnergyCosts price,int step,Environment world,Policy policy,double ms,String status) throws Exception {
        var cells=world.getCellList();int n=cells.size(),maxGen=0,contact=0;double energy=0,stored=0,exec=0,neurons=0,gen=0,force=0;
        Set<Integer> states=new HashSet<>(),lineages=new HashSet<>();StringBuilder state=new StringBuilder();
        for(Cell cell:cells) {
            assertTrue(Double.isFinite(cell.getPosition().getX()) && Double.isFinite(cell.getPosition().getY())
                    && Double.isFinite(cell.getEnergy()) && Double.isFinite(cell.getRadiusSize()),"Nonfinite world state");
            energy+=cell.getEnergy();stored+=cell.getBrain().getSynapseCount();exec+=cell.getBrain().getProccessedSynapses();
            neurons+=((CellBrain)cell.getBrain()).getNeuralNetwork().getNeuralNet().getAllNeuronsSize();
            gen+=cell.getGeneration();maxGen=Math.max(maxGen,cell.getGeneration());states.add(cell.getCellState());lineages.add(policy.lineage.get(cell));
            if(cell.getSensorActors().stream().anyMatch(a->a.getSensedCell() instanceof Cell)) contact++;
            for(var a:cell.getSensorActors()) force+=Math.abs(a.getForceStrength());
            state.append(cell.getPosition().getX()).append('/').append(cell.getPosition().getY()).append('/').append(cell.getEnergy())
                    .append('/').append(cell.getRotation()).append('/').append(cell.getRadiusSize()).append('/').append(cell.getCellState())
                    .append('/').append(cell.getGeneration()).append('/').append(cell.getBrain().getSynapseCount()).append(';');
        }
        double denominator=Math.max(1,n);
        return String.format(Locale.ROOT,"%d,%.1f,%.1f,%d,%.6f,%d,%d,%d,%d,%.9f,%.9f,%.6f,%.6f,%.6f,%.6f,%d,%d,%.6f,%.6f,%.9f,%.0f,%.6f,%s,%s%n",
                seed,price.storage(),price.work(),step,step*DT,n,policy.born,policy.dead,lineages.size(),energy,energy/denominator,stored/denominator,
                exec/denominator,neurons/denominator,gen/denominator,maxGen,states.size(),contact/denominator,force/denominator/12,policy.charged.sum(),
                policy.decisions.sum(),ms,status,hex(MessageDigest.getInstance("SHA-256").digest(state.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
}
