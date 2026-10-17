package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import java.util.*;

/** Controlled food acquisition experiment; injected food is an explicit external energy source. */
public final class CarrionRainTrainStrategy implements TrainStrategy, SimulationStepPolicy {
    static final int CANDIDATES=8, FOOD_LIMIT=12;
    static final double WIDTH=400, HEIGHT=600, EPISODE_SECONDS=120;
    static final double INITIAL_ENERGY=.18, FOOD_ENERGY=.12, RAIN_SECONDS=6, FOOD_LIFETIME=60;
    private final SimulationConfig config=SimulationConfig.getInstance();
    private final Random random;
    private final EnergyType foodEnergyType;
    private final List<Candidate> candidates=new ArrayList<>();
    private final List<Food> food=new ArrayList<>();
    private double elapsed, nextRain;
    private int generation;
    private double injected, expired;
    private volatile String status="";
    private volatile Evaluation lastEvaluation;
    private static final class Candidate {
        final Cell founder;
        double survival, harvested;
        boolean invalid;
        Candidate(Cell founder) { this.founder=founder; }
        double score() { return invalid?0:survival/EPISODE_SECONDS + .1*harvested; }
    }
    private record Food(Cell cell, int slot, double born) {}
    public record Evaluation(int generation, double score, double survivalSeconds,
                             double harvested, double injected, double expired,
                             double remainingFoodEnergy, double totalHarvested) {}

    public CarrionRainTrainStrategy() { this(new Random(),EnergyType.PRIMARY); }
    public CarrionRainTrainStrategy(EnergyType foodEnergyType) { this(new Random(),foodEnergyType); }
    CarrionRainTrainStrategy(Random random) { this(random,EnergyType.PRIMARY); }
    CarrionRainTrainStrategy(Random random,EnergyType foodEnergyType) {
        this.random=Objects.requireNonNull(random); this.foodEnergyType=Objects.requireNonNull(foodEnergyType);
    }
    public SimulationStepPolicy stepPolicy() { return this; }
    public boolean useSunlight() { return false; }
    public boolean allowReproduction(Cell cell) { return false; }
    public String getStatus() { return status; }
    public Evaluation getLastEvaluation() { return lastEvaluation; }

    public Environment initializeEnvironment() {
        config.setWidth(CANDIDATES*WIDTH); config.setHeight(HEIGHT); config.setScaleSimulation(3.3);
        return new Environment(config.getWidth(),config.getHeight());
    }
    public void initialize(Environment world) {
        world.getBlockerList().clear(); world.addGroundBlocker();
        for(int i=0;i<=CANDIDATES;i++) world.addWallBlocker(i*WIDTH-12.5,HEIGHT,Environment.GroundBlockerHeight);
        List<Cell> genomes=new ArrayList<>();
        for(int i=0;i<CANDIDATES;i++) genomes.add(CellFactory.createCell(new Vector2D(0,0),20));
        generation=1; lastEvaluation=null; startEpisode(world,genomes,false);
    }
    public void onPopulationImported(Environment world,List<Cell> imported,boolean merge) {
        List<Cell> genomes=imported.stream().filter(c->!c.isRemains() && c.getBrain() instanceof CellBrain).toList();
        if(genomes.isEmpty()) throw new IllegalArgumentException("Aasregen benötigt lebende neuronale Zellgenome.");
        List<Cell> seeds=new ArrayList<>();
        for(int i=0;i<CANDIDATES;i++) seeds.add(genomes.get(i%genomes.size()));
        generation=1; lastEvaluation=null; startEpisode(world,seeds,false);
    }
    private void startEpisode(Environment world,List<Cell> genomes,boolean evolve) {
        candidates.clear(); food.clear(); world.resetCells(); elapsed=0; nextRain=RAIN_SECONDS; injected=expired=0;
        for(int i=0;i<CANDIDATES;i++) {
            Cell parent=genomes.get(i);
            Cell cell=ReproductionManagerService.createTrainingFounder(config,world,parent,
                    new Vector2D((i+.5)*WIDTH,HEIGHT-Environment.GroundBlockerHeight-20),parent.getCellState(),evolve && i!=0);
            cell.setEnergy(INITIAL_ENERGY);
            ((CellBrain)cell.getBrain()).getNeuralNetwork().rnnClearPreviousState();
            candidates.add(new Candidate(cell)); world.addCell(cell);
            CellBrainService.think(cell,CellBrainService.generateInputs(cell,world));
        }
        rain(world,true); updateStatus();
    }
    private void rain(Environment world,boolean initial) {
        // Identical relative positions in every chamber; no candidate gets a luckier rain pattern.
        double offset=initial?0:random.nextDouble(-65,65);
        for(int i=0;i<CANDIDATES;i++) {
            final int slot=i;
            if(food.stream().filter(f->f.slot==slot).count()>=FOOD_LIMIT) continue;
            Cell corpse=new Cell(new Vector2D((i+.5)*WIDTH+offset,initial?HEIGHT-Environment.GroundBlockerHeight-65:80),
                    8,RemainsBrain.INSTANCE);
            corpse.setEnergy(0); corpse.setEnergy(foodEnergyType,FOOD_ENERGY);
            corpse.setType(new CellType(1,0,0)); corpse.becomeRemains();
            world.addCell(corpse); food.add(new Food(corpse,i,elapsed)); injected+=FOOD_ENERGY;
        }
    }
    public void beforeStep(Environment world,double dt) {
        if(!Double.isFinite(dt) || dt<=0 || dt>1) throw new IllegalArgumentException("Aasregen benötigt 0 < dt <= 1 s");
        invalidateEscapes(world);
        if(elapsed+1e-9>=nextRain) { rain(world,false); nextRain+=RAIN_SECONDS; }
    }
    private void invalidateEscapes(Environment world) {
        for(int i=0;i<candidates.size();i++) {
            Candidate candidate=candidates.get(i); Vector2D position=candidate.founder.getPosition();
            double x=position.getX(), y=position.getY();
            if(!Double.isFinite(x) || !Double.isFinite(y) || x<i*WIDTH+12.5 || x>(i+1)*WIDTH-12.5 || y<8 || y>=HEIGHT)
                candidate.invalid=true;
            if(candidate.invalid) world.getCellList().remove(candidate.founder);
        }
    }
    public void selectAndMutate(Environment world) { selectAndMutate(world,config.getCalcTimeStep()); }
    public void selectAndMutate(Environment world,double dt) {
        elapsed+=dt;
        invalidateEscapes(world);
        for(int i=0;i<candidates.size();i++) {
            Candidate candidate=candidates.get(i); Cell cell=candidate.founder;
            if(world.getCellList().contains(cell) && !cell.isRemains()) candidate.survival+=dt;
        }
        for(Iterator<Food> iterator=food.iterator();iterator.hasNext();) {
            Food item=iterator.next();
            // Only ordinary conservative absorption can reduce these inert reserves.
            if(item.cell.getEnergy(foodEnergyType)<=0 || elapsed-item.born>=FOOD_LIFETIME) {
                candidates.get(item.slot).harvested+=FOOD_ENERGY-item.cell.getEnergy(foodEnergyType);
                expired+=item.cell.getEnergy(foodEnergyType); world.getCellList().remove(item.cell); iterator.remove();
            }
        }
        if(elapsed+1e-9>=EPISODE_SECONDS) {
            for(Food item:food) candidates.get(item.slot).harvested+=FOOD_ENERGY-item.cell.getEnergy(foodEnergyType);
            List<Candidate> ranked=new ArrayList<>(candidates);
            ranked.sort(Comparator.comparingDouble(Candidate::score).reversed());
            Candidate best=ranked.getFirst();
            double remaining=food.stream().mapToDouble(f->f.cell.getEnergy(foodEnergyType)).sum();
            double totalHarvested=candidates.stream().mapToDouble(c->c.harvested).sum();
            lastEvaluation=new Evaluation(generation,best.score(),best.survival,best.harvested,injected,expired,remaining,totalHarvested);
            System.out.printf(Locale.ROOT,"CARRION_RAIN food=%s G%d score=%.4f survival=%.2fs harvested=%.4f injected=%.4f expired=%.4f remaining=%.4f totalHarvested=%.4f%n",
                    foodEnergyType.getRgbCode(),generation,best.score(),best.survival,best.harvested,injected,expired,remaining,totalHarvested);
            List<Cell> genomes=new ArrayList<>();
            for(int i=0;i<CANDIDATES;i++) genomes.add(ranked.get(i==0?0:random.nextInt(3)).founder);
            generation++; startEpisode(world,genomes,true);
        }
        updateStatus();
    }
    private void updateStatus() {
        double harvested=candidates.stream().mapToDouble(c->c.harvested).sum()
                +food.stream().mapToDouble(f->FOOD_ENERGY-f.cell.getEnergy(foodEnergyType)).sum();
        status=String.format(Locale.ROOT,"Aasregen %s · Sonne AUS · G%d · %.0f/120 s · aufgenommen %.3f · Zufuhr %.3f · Best %s",
                foodEnergyType.getRgbCode(),generation,elapsed,harvested,injected,lastEvaluation==null?"—":String.format(Locale.ROOT,"%.3f",lastEvaluation.score));
    }
}
