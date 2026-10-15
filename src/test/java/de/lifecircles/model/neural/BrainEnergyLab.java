package de.lifecircles.model.neural;

import de.lifecircles.service.SimulationConfig;
import java.util.*;

/** Deterministic small-genome experiment; no gradients, crossover or production changes. */
final class BrainEnergyLab {
    enum Task { CONSTANT, AND, OR, XOR, DELAY, COUNTER }
    record Sample(double[] input,double target) {}
    record Data(List<List<Sample>> episodes,double baselineLoss) {}
    record Price(double storage,double work,double reward) {
        Price {
            if(!Double.isFinite(storage) || !Double.isFinite(work) || !Double.isFinite(reward)
                    || storage<0 || work<0 || reward<=0)throw new IllegalArgumentException("Invalid energy price");
        }
        double cost(int stored,double executed) {
            return storage*stored*SimulationConfig.ENERGY_COST_PER_SYNAPSE
                    +work*60*executed*SimulationConfig.ENERGY_COST_PER_PROCESSED_SYNAPSE;
        }
        double utility(Data data,Metrics m) { return reward*(data.baselineLoss-m.loss)-cost(m.stored,m.executed); }
    }
    record Metrics(double loss,double accuracy,int stored,double executed,int neurons,double activations) {
        boolean solved(Task task) {
            return task==Task.CONSTANT?loss<.001:accuracy>=.95 && loss<.02;
        }
    }
    record Individual(NeuralNetwork network,double rate,double strength) {}
    record Result(Individual champion,Metrics metrics,int firstSolved,double nanos,int rejected) {}
    interface Progress { void record(int generation,Metrics metrics,int firstSolved); }
    static long seed(int seed,int generation,int slot) {
        long x=seed*0x9e3779b97f4a7c15L+generation*0x632be59bd9b4e019L+slot;
        x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);
    }
    static Data data(Task task,long seed,boolean validation) {
        Random random=new Random(seed);List<List<Sample>> episodes=new ArrayList<>();
        if(task.ordinal()<=Task.XOR.ordinal()) {
            List<Integer> order=new ArrayList<>(List.of(0,1,2,3));Collections.shuffle(order,random);
            for(int bits:order) {
                int a=bits&1,b=(bits>>1)&1;
                double y=switch(task){case CONSTANT->.7;case AND->a&b;case OR->a|b;case XOR->a^b;default->throw new AssertionError();};
                episodes.add(List.of(new Sample(new double[]{a,b},y)));
            }
        } else {
            int count=validation?8:Integer.getInteger("brain.energy.train_episodes",2),
                    length=validation?64:Integer.getInteger("brain.energy.train_length",24);
            if(count<=0 || length<=0)throw new IllegalArgumentException("Sequence dimensions must be positive");
            for(int episode=0;episode<count;episode++) {
                List<Sample> samples=new ArrayList<>();int previous=0,state=0;
                for(int step=0;step<length;step++) {
                    int a=random.nextBoolean()?1:0,b=task==Task.COUNTER && random.nextDouble()<.125?1:0;
                    double target;
                    if(task==Task.DELAY) {target=previous;previous=a;}
                    else {if(b==1)state=0;else if(a==1)state=(state+1)%4;target=state/3.0;}
                    samples.add(new Sample(new double[]{a,b},target));
                }
                episodes.add(samples);
            }
        }
        double mean=episodes.stream().flatMap(List::stream).mapToDouble(Sample::target).average().orElseThrow();
        double variance=episodes.stream().flatMap(List::stream).mapToDouble(s->Math.pow(s.target-mean,2)).average().orElseThrow();
        return new Data(episodes,variance);
    }
    static Metrics evaluate(NeuralNetwork network,Data data,Task task) {
        int stored=network.getNeuralNet().getStoredSynapseCount(),neurons=network.getNeuralNet().getAllNeuronsSize();
        if(stored>4096 || neurons>128) return new Metrics(100,0,stored,0,neurons,0);
        double error=0,work=0,activations=0;int correct=0,count=0;
        for(var episode:data.episodes) {
            network.rnnClearPreviousState();
            for(Sample sample:episode) {
                double active=network.getOutputLayerSize();
                for(Layer layer:network.getHiddenLayerArr()) active+=layer.isActiveLayer()?layer.size():layer.size()==0?0:1;
                network.setInputs(sample.input);
                double[] outputs=network.getNeuralNet().process(network.getNeuronValueFunction(),false);
                double output=outputs[0];
                if(!Double.isFinite(output)) return new Metrics(100,0,stored,0,neurons,0);
                error+=Math.pow(output-sample.target,2);work+=network.getProccessedSynapses();activations+=active;count++;
                if(task==Task.COUNTER) {if(Math.round(output*3)==Math.round(sample.target*3))correct++;}
                else if(task==Task.CONSTANT) {if(Math.abs(output-sample.target)<.05)correct++;}
                else if((output>=.5)==(sample.target>=.5))correct++;
            }
        }
        return new Metrics(error/count,correct/(double)count,stored,work/count,neurons,activations/count);
    }
    static List<Individual> initial(int seed,int population) {
        NeuralNetwork.setRandom(new Random(seed));List<Individual> result=new ArrayList<>();
        int[][] layouts={new int[0],{4},{8},{4,4}};
        for(int i=0;i<population;i++) {
            var nn=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),2,layouts[i%layouts.length],1,.65,0);
            nn.setEnableNeuronType(true); // Dynamic scheduling remains enabled, as in cells.
            if(i%2==1) for(int edge=0;edge<4;edge++) nn.getNeuralNet().addRandomSynapse(NeuralNetwork.getRandom());
            result.add(new Individual(nn,1,1));
        }
        return result;
    }
    static Result evolve(Task task,Price price,int seed,int generations,int population,NeuralNetwork known,Progress progress) {
        return evolve(task,price,seed,generations,population,known,false,progress);
    }
    static Result evolve(Task task,Price price,int seed,int generations,int population,NeuralNetwork known,boolean competitors,Progress progress) {
        List<Individual> candidates;
        if(known==null) candidates=initial(seed,population);
        else {
            candidates=new ArrayList<>();
            for(int i=0;i<population;i++) {
                NeuralNetwork nn=known.mutate(0,0);
                if(competitors && i>=population*4/5) {
                    nn=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),2,new int[0],1,0,0);
                    double target=new double[]{.25,.375,.5,.625}[i%4];
                    nn.getOutputNeuronArr()[0].setBias(0,Math.log(target/(1-target)));nn.setEnableNeuronType(true);
                }
                candidates.add(new Individual(nn,1,1));
            }
        }
        Data validation=data(task,seed(seed,100000,task.ordinal()),true);
        int first=-1,rejected=0;Individual champion=null;Metrics checked=null;
        for(int generation=0;generation<=generations;generation++) {
            // Fixed for 25 generations, then refreshed independently of mutation and price.
            Data train=data(task,seed(seed,generation/25,task.ordinal()+2000),false);
            record Rated(Individual individual,Metrics metrics,double utility) {}
            List<Rated> ranked=new ArrayList<>();
            for(Individual individual:candidates) {
                Metrics metrics=evaluate(individual.network,train,task);
                if(metrics.loss>=100)rejected++;
                ranked.add(new Rated(individual,metrics,price.utility(train,metrics)));
            }
            ranked.sort(Comparator.comparingDouble(Rated::utility).reversed());
            champion=ranked.getFirst().individual;
            if(generation%25==0 || generation==generations) {
                checked=evaluate(champion.network,validation,task);
                if(first<0 && checked.solved(task))first=generation;
                if(progress!=null)progress.record(generation,checked,first);
            }
            if(generation==generations)break;
            List<Individual> next=new ArrayList<>();int elites=Math.max(1,population/5);
            for(int slot=0;slot<population;slot++) {
                var selection=new Random(seed(seed,generation,slot+10000));
                Individual parent=ranked.get(slot<elites?slot:selection.nextInt(Math.max(1,population/4))).individual;
                NeuralNetwork.setRandom(new Random(seed(seed,generation,slot)));
                NeuralNetwork child=parent.network.mutate(slot<elites?0:Math.min(1,.1*parent.rate),slot<elites?0:.2*parent.strength);
                double rate=parent.rate,strength=parent.strength;
                // Same bounded meta-mutation rule as Cell, with an explicit reproducible RNG.
                Random meta=new Random(seed(seed,generation,slot+20000));
                if(slot>=elites) {
                    if(meta.nextDouble()<.1)rate=Math.clamp(rate+(meta.nextDouble()*2-1)*.2,.1,2);
                    if(meta.nextDouble()<.1)strength=Math.clamp(strength+(meta.nextDouble()*2-1)*.2,.1,2);
                }
                next.add(new Individual(child,rate,strength));
            }
            candidates=next;
        }
        // Timing never enters selection. Median of three warmed, repeated validation evaluations.
        for(int i=0;i<10;i++)evaluate(champion.network,validation,task);
        long[] nanos=new long[3];int decisions=validation.episodes.stream().mapToInt(List::size).sum();
        for(int repeat=0;repeat<3;repeat++) {
            long start=System.nanoTime();for(int i=0;i<20;i++)evaluate(champion.network,validation,task);
            nanos[repeat]=System.nanoTime()-start;
        }
        Arrays.sort(nanos);
        return new Result(champion,checked,first,nanos[1]/(20.0*decisions),rejected);
    }
}
