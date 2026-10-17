package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import java.util.*;

/** Physical curriculum. Selection acts on a complete clonal body's founding genome. */
public class HighPosition3TrainStrategy implements TrainStrategy, SimulationStepPolicy {
    public enum Stage {
        HOLD("Halten"), RECOVER("Störung ausgleichen"), SIZE("Größe mitsteuern"), BUILD("Aufbauen");
        final String label;
        Stage(String label) { this.label=label; }
    }
    static final int CANDIDATES=8, CELL_LIMIT=60, REQUIRED_SUCCESSES=3;
    static final double SECTION_WIDTH=1400, EPISODE_SECONDS=30, SAMPLE_SECONDS=.5, ENERGY=.75;
    private static final CellType GREEN=new CellType(0,1,0);
    private final SimulationConfig config=SimulationConfig.getInstance();
    private final Random random;
    private final StructureEpisodeFitness fitness=new StructureEpisodeFitness(CANDIDATES);
    private final List<StructureEpisodeFitness.Body> bodies=new ArrayList<>();
    private final Map<Cell,StructureEpisodeFitness.Body> owners=new IdentityHashMap<>();
    private Stage stage=Stage.HOLD;
    private int generation=1, successes, sampleNumber;
    private double elapsed;
    private boolean firstKick, secondKick;
    private volatile String status="";
    private StructureEpisodeFitness.Summary lastBest;
    public record Evaluation(int generation, Stage stage, double score, double height, double support, double contacts, double form) {}
    private volatile Evaluation lastEvaluation;

    public HighPosition3TrainStrategy() { this(new Random()); }
    HighPosition3TrainStrategy(Random random) { this.random=Objects.requireNonNull(random); }
    public Stage getStage() { return stage; }
    public int getGeneration() { return generation; }
    public String getStatus() { return status; }
    public Evaluation getLastEvaluation() { return lastEvaluation; }
    public SimulationStepPolicy stepPolicy() { return this; }
    public boolean useSunlight() { return false; }
    public boolean useEnergyConversion() { return false; } // Isolate mechanical selection from metabolism.
    public boolean isDead(Cell cell) { return false; } // Fixed membership / secured energy in this experiment.

    public Environment initializeEnvironment() {
        config.setWidth(CANDIDATES*SECTION_WIDTH); config.setHeight(1200);
        config.setScaleSimulation(CANDIDATES*SECTION_WIDTH/1000);
        return new Environment(config.getWidth(),config.getHeight());
    }
    public void initialize(Environment world) {
        world.getBlockerList().clear(); world.addGroundBlocker();
        List<Cell> genomes=new ArrayList<>();
        for (int i=0;i<CANDIDATES;i++) {
            Cell genome=CellFactory.createCell(new Vector2D(0,0),20,.5,.01,.025);
            genome.setType(new CellType(0,1,0)); genomes.add(genome);
        }
        restart(world,genomes);
    }

    /** Imported cells provide genomes, not an uncontrolled continuation of a scored episode. */
    public void onPopulationImported(Environment world) {
        onPopulationImported(world,new ArrayList<>(world.getCellList()),false);
    }
    public void onPopulationImported(Environment world,List<Cell> imported,boolean merge) {
        if (imported.isEmpty()) { if (!merge) initialize(world); return; }
        List<Cell> sourcesToConsider=new ArrayList<>(imported);
        if (merge) sourcesToConsider.addAll(world.getCellList());
        Map<Integer,Cell> representatives=new LinkedHashMap<>();
        for (Cell cell:sourcesToConsider) {
            if (!(cell.getBrain() instanceof CellBrain)) continue;
            int section=(int)(cell.getPosition().getX()/(world.getWidth()/CANDIDATES));
            representatives.putIfAbsent(section,cell);
        }
        if (representatives.isEmpty()) throw new IllegalArgumentException("HIGH_POSITION3 benötigt neuronale Zellgenome");
        List<Cell> sources=new ArrayList<>(representatives.values()), genomes=new ArrayList<>();
        for (int i=0;i<CANDIDATES;i++) genomes.add(sources.get(i%sources.size()));
        restart(world,genomes);
    }
    private void restart(Environment world,List<Cell> genomes) {
        stage=Stage.HOLD;generation=1;successes=0;lastBest=null;lastEvaluation=null;
        startEpisode(world,genomes); updateStatus();
    }

    void startEpisode(Environment world,List<Cell> genomes) {
        bodies.clear(); owners.clear();
        List<Cell> population=new ArrayList<>();
        double floor=world.getHeight()-Environment.GroundBlockerHeight;
        for (int slot=0;slot<CANDIDATES;slot++) {
            Cell genome=genomes.get(slot);
            List<Cell> cells=new ArrayList<>();
            int rows=stage==Stage.BUILD?1:12;
            for (int row=0;row<rows;row++) for (int col=0;col<5;col++) {
                double x=(slot+.5)*world.getWidth()/CANDIDATES+(col-2)*40+(row%2)*20+(row==11?.2:0);
                Cell cell=new Cell(new Vector2D(x,floor-20-row*Math.sqrt(3)*20),20,
                        new CellBrain(genome.getBrain().mutate(0,0)));
                cell.setType(genome.getType()); cell.setCellState(genome.getCellState());
                cell.setGeneration(genome.getGeneration());
                cell.setMutationRateFactor(genome.getMutationRateFactor()); cell.setMutationStrengthFactor(genome.getMutationStrengthFactor());
                cell.setEnergyConversionProgram(genome.getEnergyConversionProgram());
                cell.setEnergy(ENERGY); cells.add(cell);
            }
            var body=new StructureEpisodeFitness.Body(slot,genome,cells); bodies.add(body);
            for (Cell cell:cells) { owners.put(cell,body); population.add(cell); }
        }
        world.resetCells(population);
        // Sensing refers to this episode's world, never to stale neighbors of a donor.
        for (Cell cell:population) {
            CellBrainService.think(cell,CellBrainService.generateInputs(cell,world)); afterThinking(cell);
            for (SensorActor actor:cell.getSensorActors()) actor.updateCachedPosition();
        }
        elapsed=0;sampleNumber=0;firstKick=secondKick=false;
    }

    public void beforeStep(Environment world,double dt) {
        if (!Double.isFinite(dt) || dt<=0 || dt>SAMPLE_SECONDS) throw new IllegalArgumentException("HIGH_POSITION3 benötigt 0 < dt <= 0.5 s");
        invalidateEscapes(world);
        for (Cell cell:world.getCellList()) { cell.setEnergy(ENERGY); afterThinking(cell); }
        if (stage==Stage.RECOVER || stage==Stage.SIZE) {
            if (!firstKick && elapsed>=10-1e-9) { kick(20);firstKick=true; }
            if (!secondKick && elapsed>=20-1e-9) { kick(-20);secondKick=true; }
        }
    }
    private void kick(double speed) {
        for (var body:bodies) for (int i=55;i<60;i++) {
            Cell cell=body.cells.get(i); cell.setVelocity(cell.getVelocity().add(new Vector2D(speed,0)));
        }
    }
    public void afterThinking(Cell cell) {
        if (stage==Stage.HOLD || stage==Stage.RECOVER) cell.setRadiusSize(20);
        for (SensorActor actor:cell.getSensorActors()) {
            // Keep force outputs live in every stage. Release actor colors with size control.
            if (stage==Stage.HOLD || stage==Stage.RECOVER) actor.setType(GREEN);
            actor.setEnergyAbsorption(0); actor.setEnergyDelivery(0);
            if (stage!=Stage.BUILD) actor.setReproductionDesire(0);
        }
    }
    public boolean allowReproduction(Cell cell) {
        var body=owners.get(cell);
        return stage==Stage.BUILD && body!=null && !body.invalid && body.cells.size()<CELL_LIMIT;
    }
    public Cell reproduce(Environment world,Cell parent) { return ReproductionManagerService.reproduceClonally(config,world,parent); }
    public void onBirth(Cell parent,Cell child) {
        var body=owners.get(parent); body.cells.add(child);owners.put(child,body);
        child.setEnergy(ENERGY); afterThinking(child);
    }

    private void invalidateEscapes(Environment world) {
        double width=world.getWidth()/CANDIDATES;
        double guard=2*(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius());
        for (var body:bodies) for (Cell cell:body.cells) {
            double x=cell.getPosition().getX(), y=cell.getPosition().getY();
            if (!Double.isFinite(x) || !Double.isFinite(y) || x<0 || x>=world.getWidth() || y<0 || y>=world.getHeight()) {
                body.invalid=true; continue;
            }
            int section=(int)(x/width);
            if (section!=body.slot) { body.invalid=true;bodies.get(section).invalid=true; }
            if (x-cell.getRadiusSize()<body.slot*width+guard || x+cell.getRadiusSize()>(body.slot+1)*width-guard) body.invalid=true;
        }
    }
    public void selectAndMutate(Environment world) { selectAndMutate(world,config.getCalcTimeStep()); }
    public void selectAndMutate(Environment world,double dt) {
        elapsed+=dt;
        if (elapsed+1e-9>=(sampleNumber+1)*SAMPLE_SECONDS) {
            invalidateEscapes(world);
            fitness.sample(world,bodies,elapsed,stage==Stage.BUILD);sampleNumber++;updateStatus();
        }
        if (elapsed+1e-9<EPISODE_SECONDS) return;
        List<StructureEpisodeFitness.Body> ranked=new ArrayList<>(bodies);
        ranked.sort(Comparator.comparingDouble((StructureEpisodeFitness.Body b)->b.summary(stage==Stage.BUILD).score()).reversed()
                .thenComparingInt(b->b.slot));
        lastBest=ranked.getFirst().summary(stage==Stage.BUILD);
        lastEvaluation=new Evaluation(generation,stage,lastBest.score(),lastBest.height(),lastBest.support(),lastBest.contacts(),lastBest.form());
        System.out.printf(Locale.ROOT,"HIGH_POSITION3 generation=%d stage=%s score=%.6f height=%.6f support=%.6f contacts=%.6f form=%.6f%n",
                generation,stage,lastBest.score(),lastBest.height(),lastBest.support(),lastBest.contacts(),lastBest.form());
        advanceStage(lastBest.mastered());
        List<Cell> next=new ArrayList<>();
        for (int i=0;i<CANDIDATES;i++) {
            Cell parent=ranked.get(i==0?0:random.nextInt(3)).genome;
            // One elite; all other candidates mutate once, before cloning the body's cells.
            Cell founder=ReproductionManagerService.createTrainingFounder(config,world,parent,new Vector2D(0,0),parent.getCellState(),i!=0);
            next.add(founder);
        }
        generation++; startEpisode(world,next); updateStatus();
    }
    void advanceStage(boolean mastered) {
        if (stage==Stage.BUILD) return;
        successes=mastered?successes+1:0;
        if (successes>=REQUIRED_SUCCESSES) { stage=Stage.values()[stage.ordinal()+1];successes=0; }
    }
    private void updateStatus() {
        status=String.format(Locale.ROOT,"HIGH_POSITION3 · %s · G%d · %.0f/30 s · Bestwert %s · %d/3",
                stage.label,generation,elapsed,lastBest==null?"—":String.format(Locale.ROOT,"%.3f",lastBest.score()),successes);
    }
}
