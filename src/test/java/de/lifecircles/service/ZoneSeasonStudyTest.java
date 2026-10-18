package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.NeuralNetwork;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategyFactory;
import de.lifecircles.service.trainStrategy.DefaultTrainStrategy;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opt-in long NONE run with spectral zones, optionally seasons. Example:
 * mvn test -Dtest=ZoneSeasonStudyTest -Dzone.study.seconds=2700 -Dzone.study.year=600 -Dzone.study.name=seasons
 * Writes target/zone-season-study/NAME.csv, NAME-summary.txt and the final population NAME.cells.
 * -Dzone.study.load=PATH continues a saved population (e.g. an earlier NAME.cells) instead of new founders;
 * the year then starts again at mid-summer. -Dzone.study.lifespan=SECONDS overrides the maximum cell age.
 * -Dzone.study.adhesion=false runs the colour-blind control without force tonus (ZELLVERBINDUNGEN.md);
 * ablations: -Dzone.study.tonus=SECONDS (0 = none), -Dzone.study.hold=false, -Dzone.study.recognition=false.
 */
@EnabledIfSystemProperty(named="zone.study.seconds", matches="\\d+")
class ZoneSeasonStudyTest {
    static final double DT=1.0/60;
    static final int ZONES=SunSpectrum.ZONES.size();

    @Test void run() throws Exception {
        int seconds=Integer.getInteger("zone.study.seconds");
        double year=Double.parseDouble(System.getProperty("zone.study.year","0"));
        double winter=Double.parseDouble(System.getProperty("zone.study.winter",".25"));
        long seed=Long.getLong("zone.study.seed",4711);
        String name=System.getProperty("zone.study.name","run");
        var config=SimulationConfig.getInstance();
        config.setSunSpectralZones(true);config.setSeasonDuration(year);config.setWinterIntensity(winter);
        String lifespan=System.getProperty("zone.study.lifespan");
        if(lifespan!=null && !lifespan.isBlank())config.setCellDeathAge(Double.parseDouble(lifespan));
        config.setCellAdhesion(Boolean.parseBoolean(System.getProperty("zone.study.adhesion","true")));
        config.setActorForceTonusSeconds(Double.parseDouble(System.getProperty("zone.study.tonus",".5")));
        config.setAdhesionHoldForces(Boolean.parseBoolean(System.getProperty("zone.study.hold","true")));
        config.setAdhesionRecognition(Boolean.parseBoolean(System.getProperty("zone.study.recognition","true")));
        NeuralNetwork.setRandom(new Random(seed));
        var strategy=new DefaultTrainStrategy();
        Environment world=strategy.initializeEnvironment();strategy.initialize(world);
        String load=System.getProperty("zone.study.load");
        if(load!=null && !load.isBlank()) {
            List<Cell> loaded=FileService.getInstance().loadCellsFromFile(load);
            for(Cell cell:loaded)cell.migrateEnergyUsage();
            world.resetCells(loaded);
            System.out.println("ZONESTUDY "+name+" continues "+loaded.size()+" cells from "+load);
        }
        double radius=(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius())*2;
        var grid=PartitioningStrategyFactory.createStrategy(world.getWidth(),world.getHeight(),radius);
        var policy=strategy.stepPolicy();
        Path dir=Paths.get("target","zone-season-study");Files.createDirectories(dir);
        // The service owns the season phase; read it reflectively instead of widening the model API for a study.
        var sunField=Environment.class.getDeclaredField("energySunCalcService");sunField.setAccessible(true);
        var sun=(EnergySunCalcService)sunField.get(world);
        long start=System.nanoTime();
        try(var csv=new PrintWriter(Files.newBufferedWriter(dir.resolve(name+".csv")))) {
            csv.println(header());
            for(int s=1;s<=seconds;s++) {
                for(int i=0;i<60;i++){strategy.beforeStep(world,DT);grid.build(world.getCellList());world.update(DT,grid,policy);strategy.selectAndMutate(world,DT);}
                if(s%60==0) {
                    String row=sample(world,s,year>0?EnergySunCalcService.seasonFactor(sun.getSeasonPhase(),winter):1);
                    csv.println(row);csv.flush();
                    System.out.printf("ZONESTUDY %s t=%ds wall=%.0fs %s%n",name,s,(System.nanoTime()-start)/1e9,row);
                    if(world.getCellList().stream().noneMatch(c->!c.isRemains())){System.out.println("ZONESTUDY "+name+" extinct");break;}
                }
            }
        }
        List<Cell> living=world.getCellList().stream().filter(c->!c.isRemains()).toList();
        FileService.getInstance().saveCellsToFile(dir.resolve(name+".cells").toString(),new ArrayList<>(living));
        Files.writeString(dir.resolve(name+"-summary.txt"),summary(world,name,seconds,year,winter,seed));
    }

    static int zone(Cell cell,double width) {
        double u=((cell.getPosition().getX()%width)+width)%width/width*ZONES;
        return Math.min(ZONES-1,(int)u);
    }
    static String header() {
        StringBuilder h=new StringBuilder("t,season,living,paused,remains,activeEnzyme,meanActivity,throttled,meanAge,lifted,maxLift,attractive,pausedAttractive,largestCluster,clusterRemains,clusterLift");
        for(int z=0;z<ZONES;z++){String f=SunSpectrum.ZONES.get(z).getRgbCode();
            h.append(",z").append(f).append("_living,z").append(f).append("_ownStore,z").append(f).append("_toPrimary,z").append(f).append("_activity");}
        for(EnergyType t:EnergyType.ALL)h.append(",remains_").append(t.getRgbCode());
        for(EnergyType t:EnergyType.ALL)h.append(",living_").append(t.getRgbCode());
        return h.toString();
    }
    /** Per zone: living, mean store of the zone's own form, cells expressing zoneForm -> 000, their mean activity. */
    static String sample(Environment world,int t,double season) {
        double width=world.getWidth();
        int living=0,paused=0,remains=0,active=0,throttled=0;double activity=0;
        int[] zoneLiving=new int[ZONES],toPrimary=new int[ZONES];double[] own=new double[ZONES],zoneActivity=new double[ZONES];
        double[] remainsEnergy=new double[8],livingEnergy=new double[8];
        for(Cell c:world.getCellList()) {
            if(c.isRemains()){remains++;for(EnergyType e:EnergyType.ALL)remainsEnergy[e.getCode()]+=c.getEnergy(e);continue;}
            living++;if(c.isBrainPaused())paused++;
            for(EnergyType e:EnergyType.ALL)livingEnergy[e.getCode()]+=c.getEnergy(e);
            int z=zone(c,width);EnergyType form=SunSpectrum.ZONES.get(z);
            zoneLiving[z]++;own[z]+=c.getEnergy(form);
            EnergyConversion r=c.getEnergyConversion();
            if(r.active()){active++;activity+=c.getEnzymeActivity();if(c.getEnzymeActivity()<.99)throttled++;}
            if(r.active()&&r.target()==EnergyType.PRIMARY&&r.source()==form&&form!=EnergyType.PRIMARY){toPrimary[z]++;zoneActivity[z]+=c.getEnzymeActivity();}
        }
        StringBuilder row=new StringBuilder();
        row.append(t).append(',').append(fmt(season)).append(',').append(living).append(',').append(paused).append(',').append(remains)
                .append(',').append(active).append(',').append(fmt(active==0?0:activity/active)).append(',').append(throttled);
        double age=0;int lifted=0;double maxLift=0;
        for(Cell c:world.getCellList()) if(!c.isRemains()) {
            age+=c.getAge();double lift=liftAboveTerrain(world,c);
            if(lift>LIFTED_PX)lifted++;maxLift=Math.max(maxLift,lift);
        }
        row.append(',').append(fmt(living==0?0:age/living)).append(',').append(lifted).append(',').append(fmt(maxLift));
        int actors=0,attracting=0,pausedActors=0,pausedAttracting=0;
        for(Cell c:world.getCellList()) if(!c.isRemains()) for(SensorActor a:c.getSensorActors()) {
            actors++;if(a.getForceStrength()<0)attracting++;
            if(c.isBrainPaused()){pausedActors++;if(a.getForceStrength()<0)pausedAttracting++;}
        }
        int[] cluster=largestContactCluster(world);
        row.append(',').append(fmt(actors==0?0:(double)attracting/actors)).append(',').append(fmt(pausedActors==0?0:(double)pausedAttracting/pausedActors))
                .append(',').append(cluster[0]).append(',').append(cluster[1]).append(',').append(cluster[2]);
        for(int z=0;z<ZONES;z++) row.append(',').append(zoneLiving[z]).append(',').append(fmt(zoneLiving[z]==0?0:own[z]/zoneLiving[z]))
                .append(',').append(toPrimary[z]).append(',').append(fmt(toPrimary[z]==0?0:zoneActivity[z]/toPrimary[z]));
        for(double e:remainsEnergy)row.append(',').append(fmt(e));
        for(double e:livingEnergy)row.append(',').append(fmt(e));
        return row.toString();
    }
    /** One maximum-size cell: a lifted cell must rest on other cells, not on ground or a wall top. */
    static final double LIFTED_PX=40;
    /** Gap between the cell's bottom and the highest ground/blocker surface directly below its centre (y grows downward). */
    static double liftAboveTerrain(Environment world,Cell cell) {
        double x=cell.getPosition().getX(),bottom=cell.getPosition().getY()+cell.getRadiusSize();
        double surface=world.getHeight();
        for(Blocker b:world.getBlockerList()) {
            double top=b.getPosition().getY()-b.getHeight()/2;
            if(Math.abs(x-b.getPosition().getX())<=b.getWidth()/2 && top>=bottom-1e-6)surface=Math.min(surface,top);
        }
        return Math.max(0,surface-bottom);
    }
    /** Contact gap that still counts as connected: actor fields reach beyond touching surfaces. */
    static final double CONTACT_PX=2;
    /** Largest group of touching cells (living and remains): size, remains in it, highest lift of a member. */
    static int[] largestContactCluster(Environment world) {
        List<Cell> cells=world.getCellList();int n=cells.size();int[] parent=new int[n];
        for(int i=0;i<n;i++)parent[i]=i;
        for(int i=0;i<n;i++)for(int j=i+1;j<n;j++) {
            Cell a=cells.get(i),b=cells.get(j);double reach=a.getRadiusSize()+b.getRadiusSize()+CONTACT_PX;
            if(a.getPosition().getShortestDirectionWithWrapAround(b.getPosition(),world.getWidth(),world.getHeight()).lengthSquared()<=reach*reach)
                parent[root(parent,i)]=root(parent,j);
        }
        Map<Integer,int[]> groups=new HashMap<>();
        for(int i=0;i<n;i++) {
            int[] g=groups.computeIfAbsent(root(parent,i),k->new int[3]);Cell c=cells.get(i);
            g[0]++;if(c.isRemains())g[1]++;g[2]=Math.max(g[2],(int)Math.round(liftAboveTerrain(world,c)));
        }
        return groups.values().stream().max(Comparator.comparingInt(g->g[0])).orElse(new int[3]);
    }
    static int root(int[] parent,int i){while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}return i;}
    static String fmt(double v){return String.format(Locale.ROOT,"%.4f",v);}

    static String summary(Environment world,String name,int seconds,double year,double winter,long seed) {
        Map<String,Integer> routes=new TreeMap<>();
        for(Cell c:world.getCellList()) if(!c.isRemains()) {
            EnergyConversion r=c.getEnergyConversion();
            String key=SunSpectrum.ZONES.get(zone(c,world.getWidth())).getRgbCode()+" zone: "+(r.active()?r.source().getRgbCode()+"->"+r.target().getRgbCode():"none");
            routes.merge(key,1,Integer::sum);
        }
        StringBuilder s=new StringBuilder();
        var c=SimulationConfig.getInstance();
        s.append("adhesion=").append(c.isCellAdhesion()).append(" tonus=").append(c.getActorForceTonusSeconds())
                .append(" hold=").append(c.isAdhesionHoldForces()).append(" recognition=").append(c.isAdhesionRecognition()).append(' ');
        s.append("run=").append(name).append(" load=").append(System.getProperty("zone.study.load","-")).append(" seconds=").append(seconds).append(" year=").append(year)
                .append(" winter=").append(winter).append(" seed=").append(seed).append('\n');
        s.append("expressed routes of living cells by zone:\n");
        routes.forEach((k,v)->s.append("  ").append(k).append(": ").append(v).append('\n'));
        return s.toString();
    }
}
