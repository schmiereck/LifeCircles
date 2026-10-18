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
 * ablations: -Dzone.study.tonus=SECONDS (0 = none), -Dzone.study.hold=false, -Dzone.study.recognition=false;
 * experimental stages: -Dzone.study.bonds=true (lock-in), -Dzone.study.birthbonds=true; ground hold
 * (BODENHALT.md): -Dzone.study.ground=true (surface fields), -Dzone.study.anchors=true, -Dzone.study.groundonly=true.
 * Bond snapshots (PNG overview plus crops of the most protruding bonded bodies) are written at the end and,
 * with -Dzone.study.snapshot=SECONDS, at that interval: NAME-tSECONDS.png, NAME-tSECONDS-bodyN.png.
 */
@EnabledIfSystemProperty(named="zone.study.seconds", matches="\\d+")
public class ZoneSeasonStudyTest {
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
        config.setAdhesionBonds(Boolean.getBoolean("zone.study.bonds"));
        config.setAdhesionBirthBonds(Boolean.getBoolean("zone.study.birthbonds"));
        config.setGroundAdhesion(Boolean.getBoolean("zone.study.ground"));
        config.setGroundAnchors(Boolean.getBoolean("zone.study.anchors"));
        config.setGroundAnchorsGroundOnly(Boolean.getBoolean("zone.study.groundonly"));
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
        int snapshotEvery=Integer.getInteger("zone.study.snapshot",0);
        long start=System.nanoTime();
        try(var csv=new PrintWriter(Files.newBufferedWriter(dir.resolve(name+".csv")))) {
            csv.println(header());
            for(int s=1;s<=seconds;s++) {
                for(int i=0;i<60;i++){strategy.beforeStep(world,DT);grid.build(world.getCellList());world.update(DT,grid,policy);strategy.selectAndMutate(world,DT);}
                if(s%60==0) {
                    String row=sample(world,s,year>0?EnergySunCalcService.seasonFactor(sun.getSeasonPhase(),winter):1);
                    csv.println(row);csv.flush();
                    System.out.printf("ZONESTUDY %s t=%ds wall=%.0fs %s%n",name,s,(System.nanoTime()-start)/1e9,row);
                    if(snapshotEvery>0&&s%snapshotEvery==0&&s<seconds)writeSnapshots(world,dir,name,s);
                    if(world.getCellList().stream().noneMatch(c->!c.isRemains())){System.out.println("ZONESTUDY "+name+" extinct");break;}
                }
            }
        }
        writeSnapshots(world,dir,name,seconds);
        List<Cell> living=world.getCellList().stream().filter(c->!c.isRemains()).toList();
        FileService.getInstance().saveCellsToFile(dir.resolve(name+".cells").toString(),new ArrayList<>(living));
        Files.writeString(dir.resolve(name+"-summary.txt"),summary(world,name,seconds,year,winter,seed));
    }

    static int zone(Cell cell,double width) {
        double u=((cell.getPosition().getX()%width)+width)%width/width*ZONES;
        return Math.min(ZONES-1,(int)u);
    }
    public static String header() {
        StringBuilder h=new StringBuilder("t,season,living,paused,remains,activeEnzyme,meanActivity,throttled,meanAge,lifted,maxLift,attractive,pausedAttractive,largestCluster,clusterRemains,clusterLift,bonds,largestBonded,bondedLift,bondedSpan,bondedRise,riseCells,anchors,anchoredCells,rootedBodies,rootedRise,rootedSpan");
        for(int z=0;z<ZONES;z++){String f=SunSpectrum.ZONES.get(z).getRgbCode();
            h.append(",z").append(f).append("_living,z").append(f).append("_ownStore,z").append(f).append("_toPrimary,z").append(f).append("_activity");}
        for(EnergyType t:EnergyType.ALL)h.append(",remains_").append(t.getRgbCode());
        for(EnergyType t:EnergyType.ALL)h.append(",living_").append(t.getRgbCode());
        return h.toString();
    }
    /** Per zone: living, mean store of the zone's own form, cells expressing zoneForm -> 000, their mean activity. */
    public static String sample(Environment world,int t,double season) {
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
        int[] bonded=largestBondedBody(world);
        row.append(',').append(bonded[0]).append(',').append(bonded[1]).append(',').append(bonded[2]);
        double span=0,rise=0;int riseCells=0;
        for(List<Cell> body:bondedBodies(world)){double[] shape=bodyShape(world,body);span=Math.max(span,shape[0]);
            if(riseCells==0||shape[1]>rise){rise=shape[1];riseCells=body.size();}}
        row.append(',').append(fmt(span)).append(',').append(fmt(rise)).append(',').append(riseCells);
        // Ground anchors (BODENHALT.md): rooted bodies are bonded bodies with at least one anchored member.
        int anchorCount=0,anchoredCells=0;
        for(Cell c:world.getCellList()){int n=0;for(SensorActor a:c.getSensorActors())if(a.getAnchor()!=null)n++;anchorCount+=n;if(n>0)anchoredCells++;}
        int rooted=0;double rootedRise=0,rootedSpan=0;
        for(List<Cell> body:bondedBodies(world)) if(body.stream().anyMatch(c->c.getSensorActors().stream().anyMatch(a->a.getAnchor()!=null))) {
            double[] shape=bodyShape(world,body);
            if(rooted==0||shape[1]>rootedRise)rootedRise=shape[1];rootedSpan=Math.max(rootedSpan,shape[0]);rooted++;
        }
        row.append(',').append(anchorCount).append(',').append(anchoredCells).append(',').append(rooted)
                .append(',').append(fmt(rootedRise)).append(',').append(fmt(rootedSpan));
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
    /** {bonds, cells of the largest bonded body, highest lift of a member}; one cell alone is no body. */
    static int[] largestBondedBody(Environment world) {
        List<Cell> cells=world.getCellList();int n=cells.size();int[] parent=new int[n];int bonds=0;
        Map<Cell,Integer> index=new IdentityHashMap<>();
        for(int i=0;i<n;i++){parent[i]=i;index.put(cells.get(i),i);}
        for(int i=0;i<n;i++)for(SensorActor a:cells.get(i).getSensorActors()) {
            SensorActor p=a.getBondPartner();if(p==null)continue;
            Integer j=index.get(p.getParentCell());if(j==null||j<i)continue;
            bonds++;parent[root(parent,i)]=root(parent,j);
        }
        Map<Integer,int[]> groups=new HashMap<>();
        for(int i=0;i<n;i++){int[] g=groups.computeIfAbsent(root(parent,i),k->new int[2]);g[0]++;g[1]=Math.max(g[1],(int)Math.round(liftAboveTerrain(world,cells.get(i))));}
        int[] best=groups.values().stream().filter(g->g[0]>1).max(Comparator.comparingInt(g->g[0])).orElse(new int[2]);
        return new int[]{bonds,best[0],best[1]};
    }
    /** Bonded bodies of at least two cells; bonds to cells outside the list are ignored. */
    static List<List<Cell>> bondedBodies(Environment world) {
        List<Cell> cells=world.getCellList();int n=cells.size();int[] parent=new int[n];
        Map<Cell,Integer> index=new IdentityHashMap<>();
        for(int i=0;i<n;i++){parent[i]=i;index.put(cells.get(i),i);}
        for(int i=0;i<n;i++)for(SensorActor a:cells.get(i).getSensorActors()) {
            SensorActor p=a.getBondPartner();if(p==null)continue;
            Integer j=index.get(p.getParentCell());if(j!=null)parent[root(parent,i)]=root(parent,j);
        }
        Map<Integer,List<Cell>> groups=new LinkedHashMap<>();
        for(int i=0;i<n;i++)groups.computeIfAbsent(root(parent,i),k->new ArrayList<>()).add(cells.get(i));
        return groups.values().stream().filter(g->g.size()>1).toList();
    }
    /**
     * {span, rise} of a bonded body in px. Span: lowest member bottom to highest member top. Rise: how far the
     * top member reaches above everything else under the body's horizontal extent: other cells (living or
     * remains) and terrain whose bottom reaches at least the body's top. A cell lying on the body counts, so a
     * buried body has negative rise; things wholly above it (ceilings, falling cells) do not. A stalk standing
     * out of a heap has a rise of several cell diameters, a chain lying on top about one diameter.
     */
    static double[] bodyShape(Environment world,List<Cell> body) {
        double w=world.getWidth(),h=world.getHeight();Vector2D anchor=body.get(0).getPosition();
        Set<Cell> members=Collections.newSetFromMap(new IdentityHashMap<>());members.addAll(body);
        double left=Double.POSITIVE_INFINITY,right=Double.NEGATIVE_INFINITY,top=Double.POSITIVE_INFINITY,bottom=Double.NEGATIVE_INFINITY;
        for(Cell c:body){double x=anchor.getShortestDirectionWithWrapAround(c.getPosition(),w,h).getX(),r=c.getRadiusSize(),y=c.getPosition().getY();
            left=Math.min(left,x-r);right=Math.max(right,x+r);top=Math.min(top,y-r);bottom=Math.max(bottom,y+r);}
        double surface=h;
        for(Blocker b:world.getBlockerList()){double x=anchor.getShortestDirectionWithWrapAround(b.getPosition(),w,h).getX();
            if(x+b.getWidth()/2>=left&&x-b.getWidth()/2<=right&&b.getPosition().getY()+b.getHeight()/2>=top-CONTACT_PX)
                surface=Math.min(surface,b.getPosition().getY()-b.getHeight()/2);}
        for(Cell c:world.getCellList()){if(members.contains(c))continue;
            double x=anchor.getShortestDirectionWithWrapAround(c.getPosition(),w,h).getX(),r=c.getRadiusSize(),y=c.getPosition().getY();
            if(x+r>=left&&x-r<=right&&y+r>=top-CONTACT_PX)surface=Math.min(surface,y-r);}
        return new double[]{bottom-top,surface-top};
    }
    /** Overview of the whole world, crops of the three bonded bodies with the largest rise and of the largest body. */
    public static void writeSnapshots(Environment world,Path dir,String name,int t) throws IOException {
        System.setProperty("java.awt.headless","true");
        List<List<Cell>> bodies=new ArrayList<>(bondedBodies(world));
        Map<List<Cell>,double[]> shapes=new IdentityHashMap<>();
        for(List<Cell> b:bodies)shapes.put(b,bodyShape(world,b));
        bodies.sort(Comparator.comparingDouble(b->-shapes.get(b)[1]));
        long bonds=world.getCellList().stream().flatMap(c->c.getSensorActors().stream()).filter(a->a.getBondPartner()!=null).count()/2;
        BondSnapshotImage.writeOverview(world,dir.resolve(name+"-t"+t+".png"),.25,
                String.format(Locale.ROOT,"%s t=%ds cells=%d bonds=%d bodies=%d",name,t,world.getCellList().size(),bonds,bodies.size()));
        for(int i=0;i<Math.min(3,bodies.size());i++){List<Cell> b=bodies.get(i);double[] s=shapes.get(b);
            BondSnapshotImage.writeBody(world,b,dir.resolve(name+"-t"+t+"-body"+(i+1)+".png"),480,1.5,
                    String.format(Locale.ROOT,"%s t=%ds body %d: %d cells, span %.0f px, rise %.0f px",name,t,i+1,b.size(),s[0],s[1]));}
        bodies.stream().max(Comparator.comparingInt(List::size)).ifPresent(b->{double[] s=shapes.get(b);
            try{BondSnapshotImage.writeBody(world,b,dir.resolve(name+"-t"+t+"-largest.png"),480,1.5,
                    String.format(Locale.ROOT,"%s t=%ds largest body: %d cells, span %.0f px, rise %.0f px",name,t,b.size(),s[0],s[1]));}
            catch(IOException e){throw new UncheckedIOException(e);}});
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
                .append(" hold=").append(c.isAdhesionHoldForces()).append(" recognition=").append(c.isAdhesionRecognition())
                .append(" bonds=").append(c.isAdhesionBonds()).append(" birthbonds=").append(c.isAdhesionBirthBonds())
                .append(" ground=").append(c.isGroundAdhesion()).append(" anchors=").append(c.isGroundAnchors())
                .append(" groundonly=").append(c.isGroundAnchorsGroundOnly()).append(' ');
        s.append("run=").append(name).append(" load=").append(System.getProperty("zone.study.load","-")).append(" seconds=").append(seconds).append(" year=").append(year)
                .append(" winter=").append(winter).append(" seed=").append(seed).append('\n');
        s.append("expressed routes of living cells by zone:\n");
        routes.forEach((k,v)->s.append("  ").append(k).append(": ").append(v).append('\n'));
        return s.toString();
    }
}
