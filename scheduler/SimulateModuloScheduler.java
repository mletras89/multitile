/*
--------------------------------------------------------------------------
 Copyright (c) 2022 Hardware-Software-Co-Design, Friedrich-
 Alexander-Universitaet Erlangen-Nuernberg (FAU), Germany. 
 All rights reserved.
 
 This code and any associated documentation is provided "as is"
 
 IN NO EVENT SHALL HARDWARE-SOFTWARE-CO-DESIGN, FRIEDRICH-ALEXANDER-
 UNIVERSITAET ERLANGEN-NUERNBERG (FAU) BE LIABLE TO ANY PARTY FOR DIRECT,
 INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 OF THE USE OF THIS CODE AND ITS DOCUMENTATION, EVEN IF HARDWARE-
 SOFTWARE-CO-DESIGN, FRIEDRICH-ALEXANDER-UNIVERSITAET ERLANGEN-NUERNBERG
 (FAU) HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. THE
 AFOREMENTIONED EXCLUSIONS OF LIABILITY DO NOT APPLY IN CASE OF INTENT
 BY HARDWARE-SOFTWARE-CO-DESIGN, FRIEDRICH-ALEXANDER-UNIVERSITAET
 ERLANGEN-NUERNBERG (FAU).
 
 HARDWARE-SOFTWARE-CO-DESIGN, FRIEDRICH-ALEXANDER-UNIVERSITAET ERLANGEN-
 NUERNBERG (FAU), SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE.
 
 THE CODE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND HARDWARE-
 SOFTWARE-CO-DESIGN, FRIEDRICH-ALEXANDER-UNIVERSITAET ERLANGEN-
 NUERNBERG (FAU) HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 -------------------------------------------------------------------------
 
  @author Martin Letras
  @date   29 November 2022
  @version 1.1
  @ brief
     This class describes the Modulo scheduler implemented as presented in:
     Kejariwal, A., Nicolau, A. (2011). Modulo Scheduling and Loop Pipelining. 
     In: Padua, D. (eds) Encyclopedia of Parallel Computing. 
     Springer, Boston, MA. https://doi.org/10.1007/978-0-387-09766-4_65
  
  NOTE: This is only valid for single-rate dataflows
--------------------------------------------------------------------------
*/
package multitile.scheduler;

import multitile.Action;
import multitile.architecture.Processor;
import multitile.architecture.Tile;
import multitile.mapping.Bindings;
import multitile.scheduler.UtilizationTable.TimeSlot;
import multitile.architecture.Architecture;
import multitile.architecture.Crossbar;
import multitile.architecture.NoC;
import multitile.application.Actor;
import multitile.application.Actor.ACTOR_TYPE;
import multitile.application.Application;
import multitile.application.CommunicationTask;
import multitile.application.CompositeFifo;
import multitile.application.Fifo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SimulateModuloScheduler extends BaseScheduler implements Schedule{
	
	//private HeuristicModuloSchedulerWithCommunications heuristic;
	private HeuristicModuloSchedulerConstrained heuristic;
	// key: core type
	// value:
	//		Key: processor index
	//		Value: queue of actions
	//private HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule;
	private HashMap<Integer,HashMap<Integer,Queue<TimeSlot>>> scheduleAnalysis;
	// the key is the core, crossbar or noc id
	private Queue<TimeSlot> schedulePipelinedActions;
	private int startKernel;
	
	//public SimulateModuloScheduler(Architecture architecture, Application application,HeuristicModuloSchedulerWithCommunications heuristic){
	public SimulateModuloScheduler(Architecture architecture, Application application,HeuristicModuloSchedulerConstrained heuristic){
		super();
		this.setApplication(application);
		this.setArchitecture(architecture);
		this.heuristic = heuristic;
	}

	@Override
	public void schedule(Bindings bindings) {
		// here we perform the scheduling including the communication overheads
		/*architecture.resetArchitecture();
		application.resetApplication(architecture, bindings, application);
		while(!scheduleHeuristicModulo(scheduleActionPrologue, bindings)) {
			architecture.resetArchitecture();
			application.resetApplication(architecture, bindings, application);
		}*/
	}	

	public int getMakeSpan() {
		ArrayList<Integer> endTimes = new ArrayList<>();
		for(TimeSlot t : schedulePipelinedActions) {
			if (t.getIteration() == 0)
				endTimes.add(t.getEndTime());
		}
		if (endTimes.size() == 0)
			return 0;
		return Collections.max(endTimes);
	}
	
	//public HeuristicModuloSchedulerWithCommunications getHeuristic() {
	public HeuristicModuloSchedulerConstrained getHeuristic() {
		return heuristic;
	}
	
	public void createScheduleForAnalysis(double scaleFactor) {
		// this only works for hSDFG
		int latency = heuristic.getLantency();
		assert latency != -1 : "First calculate the schedule";
		this.startKernel = ((int)Math.ceil((double)latency/(double)heuristic.getPeriod()))*heuristic.getPeriod();
		int nIterations = (startKernel / heuristic.getPeriod()) + 4;
		
		createPipelinedScheduler(nIterations);
		// once I have the schedule, I have to calculate the FIFO capacities
		// key: is the FIFO id
		// value: 
		//		key : time
		//		value : number tokens
		HashMap<Integer, TreeMap<Double,Integer> > mapTokensCounting = new HashMap<>();
		
		// count of reads of a MRB
		// key: is the MRB FIFO
		// value: number of reads
		HashMap<Integer,Integer> readsMRB = new HashMap<>();
		HashMap<Integer,Integer> capacityFifo = new HashMap<>();
		
		for(Map.Entry<Integer, Fifo> f: application.getFifos().entrySet()) {
			TreeMap<Double,Integer> tokensCounting = new TreeMap<>();
			tokensCounting.put(0.0, f.getValue().getInitialTokens());
			mapTokensCounting.put(f.getKey(), tokensCounting);
			capacityFifo.put(f.getKey(), 1);
			if (f.getValue().isCompositeChannel())
				readsMRB.put(f.getKey(), 0);
		}
		//System.out.println("MapTokensCounting "+mapTokensCounting);
		//ArrayList<TimeSlot> l = new ArrayList<>();
		for(TimeSlot t :  schedulePipelinedActions) {
			if(application.getActors().containsKey(t.getActorId())) {
				// it is an actor
				Actor actor = application.getActors().get(t.getActorId());
				int currentIteration = t.getIteration();
				HashMap<String,TimeSlot> reads = new HashMap<>();
				HashMap<String,TimeSlot> writes = new HashMap<>();
				
				//System.out.println("Analysis actor "+actor.getName()+" from "+t.getStartTime()+" to "+t.getEndTime()+" current iteration "+currentIteration);
				
				for(CommunicationTask ct : heuristic.getActorReads().get(actor.getId())) {
					for(TimeSlot tp : schedulePipelinedActions) {
						if (currentIteration == tp.getIteration() && ct.getId() == tp.getActorId()) { //       !application.getActors().containsKey(tp.getActorId())) {
							reads.put(ct.getName(), tp);
							//System.out.println("CTask read "+ct.getName());
						}
					}
				}
				for(CommunicationTask ct :heuristic.getActorWrites().get(actor.getId())) {
					for(TimeSlot tp : schedulePipelinedActions) {
						if (currentIteration == tp.getIteration() && ct.getId() == tp.getActorId()) { // !application.getActors().containsKey(tp.getActorId())) {
							writes.put(ct.getName(), tp);
							//System.out.println("CTask write "+ct.getName());
						}
					}
				}
				// smallest startTime of reads
				int minStartReads = Integer.MAX_VALUE;
				for(TimeSlot tp : reads.values()) {
					minStartReads = (tp.getStartTime() < minStartReads) ? tp.getStartTime() : minStartReads;
					//System.out.println("\tRead  "+heuristic.getSetCommunicationTasks().get(tp.getActorId()).getName()+" iteration "+tp.getIteration());
				}
				if (minStartReads == Integer.MAX_VALUE)
					minStartReads = t.getStartTime();
				
				int maxEndWrites = Integer.MIN_VALUE;
				for(TimeSlot tp : writes.values()) {
					maxEndWrites = (tp.getEndTime() > maxEndWrites) ? tp.getEndTime() : maxEndWrites;
					//System.out.println("\tWrite  "+heuristic.getSetCommunicationTasks().get(tp.getActorId()).getName()+" iteration "+tp.getIteration());
				}
				if (maxEndWrites == Integer.MIN_VALUE)
					maxEndWrites = t.getEndTime();
				
				// now check that at the beginning of the first read, there exists enough spaces at the outputs
				checkAndIncreseTargetFifos(actor, capacityFifo,mapTokensCounting, minStartReads);
				// remove all the reads after the execution of the actor
				for(TimeSlot tp : reads.values()) {
					CommunicationTask cTask = heuristic.getSetCommunicationTasks().get(tp.getActorId());
					int insertTime =  maxEndWrites; //;t.getEndTime();
					Fifo fifo = cTask.getFifo();
					int nTokens = -1*fifo.getProdRate();
					//if(t.getLength() == 0)
					//	insertTime += 0000000000000000000000001;  // this trick to track the reads from local memories
					if(fifo.isCompositeChannel())
						readsMRB.put(fifo.getId(), readsMRB.get(fifo.getId())+1);
					insertCommunicationsInSchedule(mapTokensCounting, fifo, nTokens, insertTime, readsMRB);
					//update the capacity
					TreeMap<Double,Integer> tokensCounting = mapTokensCounting.get(fifo.getId());
					int fifoCapacity = Collections.max(tokensCounting.values());
					if (capacityFifo.get(fifo.getId()) < fifoCapacity)
						capacityFifo.put(fifo.getId(), fifoCapacity);	
				}
				for(TimeSlot tp : writes.values()) {
					CommunicationTask cTask = heuristic.getSetCommunicationTasks().get(tp.getActorId());
					int insertTime = tp.getEndTime();
					Fifo fifo = cTask.getFifo();
					int nTokens = fifo.getConsRate();
					insertCommunicationsInSchedule(mapTokensCounting, fifo, nTokens, insertTime, readsMRB);
					//update the capacity
					TreeMap<Double,Integer> tokensCounting = mapTokensCounting.get(fifo.getId());
					int fifoCapacity = Collections.max(tokensCounting.values());
					if (capacityFifo.get(fifo.getId()) < fifoCapacity)
						capacityFifo.put(fifo.getId(), fifoCapacity);
				}
				//System.out.println("mapTokensCounting "+mapTokensCounting);
				//System.out.println("Fifo capacity "+capacityFifo);
			}
		}

		//printCommunicationsInSchedule(mapTokensCounting);
		// then set the FIFO capacities
		for(Map.Entry<Integer, Integer> f: capacityFifo.entrySet()) {
			Fifo fifo = application.getFifos().get(f.getKey());
			TreeMap<Double,Integer> tokensCounting = mapTokensCounting.get(fifo.getId());
			int fifoCapacity = f.getValue();
			assert fifoCapacity > 0 : "Capacity must not be negative or zero, Capacity="+fifoCapacity+" fifo: "+fifo.getName();
			
			int fifoMinVal = Collections.min(tokensCounting.values());
			//assert fifoMinVal >=0: "Minimum number of stored tokens must be bigger than 0 fifo: "+fifo.getName()+" fifoMinVal "+fifoMinVal;
			
			if (fifoCapacity == 0)
				fifoCapacity = (fifo.getConsRate() >= fifo.getProdRate()) ? fifo.getConsRate() : fifo.getProdRate();
				
			application.getFifos().get(f.getKey()).set_capacity(fifoCapacity);
		}
	}
	

	
	public void printCommunicationsInSchedule(HashMap<Integer, TreeMap<Double,Integer> > mapTokensCounting) {
		for(Map.Entry<Integer, TreeMap<Double,Integer>> m : mapTokensCounting.entrySet()) {
			System.err.println("FIFO "+application.getFifos().get(m.getKey()).getName());
			TreeMap<Double,Integer> tokensCounting = m.getValue();
			for(Map.Entry<Double, Integer> t: tokensCounting.entrySet()) {
				System.err.println("\tTime "+t.getKey()+" count: "+t.getValue());
			}
		}
	}
	
	public void checkAndIncreseTargetFifos(Actor actor, HashMap<Integer,Integer> capacityFifo,HashMap<Integer, TreeMap<Double,Integer> > mapTokensCounting, double time){
		for(Fifo f : actor.getOutputFifos()) {
			int cons = f.getConsRate();
			int storedTokens = checkCurrentStoredTokens(mapTokensCounting, f, time);
			if (capacityFifo.get(f.getId()) <  storedTokens + cons)
				capacityFifo.put(f.getId() ,  storedTokens + cons);
		}
	}
	/*
	public void checkAndIncreseTargetFifos(Fifo fifo, HashMap<Integer,Integer> capacityFifo,HashMap<Integer, TreeMap<Double,Integer> > mapTokensCounting, double time){
		Actor destination = fifo.getDestination();
		for(Fifo f : destination.getOutputFifos()) {
			int prod = f.getProdRate();
			int storedTokens = checkCurrentStoredTokens(mapTokensCounting, f, time);
			if (capacityFifo.get(f.getId()) <  storedTokens + prod)
				capacityFifo.put(f.getId() ,  storedTokens + prod);
		}
	}*/
	
	public int checkCurrentStoredTokens(HashMap<Integer, TreeMap<Double,Integer> > mapTokensCounting, Fifo fifo, double time) {
		// tokens might be positive or negative
		// in case of positive, tokens have been produced
		// in case of negative, tokens must be consumed
		//HashMap<Integer, TreeMap<Integer,Integer> > result = mapTokensCounting;
		TreeMap<Double,Integer> tokensCounting = mapTokensCounting.get(fifo.getId());
		int count =0 ;
		for(Map.Entry<Double, Integer> t : tokensCounting.entrySet()) {
			if( t.getKey() <= time )
				count = t.getValue();
		}

		return count;
	}
	
	
	public void insertCommunicationsInSchedule(HashMap<Integer, TreeMap<Double,Integer> > mapTokensCounting, Fifo fifo, int tokens, double time, HashMap<Integer,Integer> readsMRB) {
		// tokens might be positive or negative
		// in case of positive, tokens have been produced
		// in case of negative, tokens must be consumed
		//HashMap<Integer, TreeMap<Integer,Integer> > result = mapTokensCounting;
		TreeMap<Double,Integer> tokensCounting = mapTokensCounting.get(fifo.getId());
		if(tokensCounting.isEmpty()) {
			tokensCounting.put(time, tokens);
		}else {
			if(!tokensCounting.containsKey(time)) {
				ArrayList<Double> smallIndices = new ArrayList<>();
				for(Map.Entry<Double, Integer> t : tokensCounting.entrySet()) {
					if (t.getKey() < time)
						smallIndices.add(t.getKey());
				}
				double max = Collections.max(smallIndices);
				int count = tokensCounting.get(max);
				tokensCounting.put(time, count);
			}
			for(Map.Entry<Double, Integer> t : tokensCounting.entrySet()) {
				if(t.getKey() >= time) {
					int nTokens = tokens;
					int currentTokens = tokensCounting.get(t.getKey());
					if (tokens < 0 && fifo.isCompositeChannel()) { // it is a read
						CompositeFifo mrb = (CompositeFifo) fifo;
						int nReaders = mrb.getReaders().size();
						if (readsMRB.get(mrb.getId()) % nReaders != 0)
							nTokens = 0;			
					}
					tokensCounting.put(t.getKey(), currentTokens + nTokens );
				}
			}
		}
		mapTokensCounting.put(fifo.getId(), tokensCounting);
		//return result;
	}
	
	
	// the bindings must be given
	public void createPipelinedScheduler(int nIterations) {
		HashMap<Integer,TimeSlot> timeInfoActors = heuristic.getTimeInfoActors();
		//heuristic.printTimeInfoActors();
		assert timeInfoActors != null: "First generate the timeInfoActors";
		assert timeInfoActors.size() != 0 : "First generate the timeInfoActors";
		
		UtilizationTable scheduler = heuristic.getScheduler();
		Map<Integer,Map<Integer,LinkedList<TimeSlot>>> U = scheduler.getUtilizationTable();
		
		
		// key:   actor id
		// value: core id
		HashMap<Integer,ArrayList<Integer>> actorToResourceId = new HashMap<>();
		// filling auxiliary maps and initializing schedule
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>> u : U.entrySet()) {
			for(Map.Entry<Integer,LinkedList<TimeSlot>> ts : u.getValue().entrySet()) {
				for(TimeSlot t : ts.getValue()) {
					if (actorToResourceId.containsKey(t.getActorId())){
						ArrayList<Integer> tmp = actorToResourceId.get(t.getActorId());
						tmp.add(u.getKey());
						actorToResourceId.put(t.getActorId(), tmp);
					}else {
						ArrayList<Integer> tmp = new ArrayList<Integer>();
						tmp.add(u.getKey());
						actorToResourceId.put(t.getActorId(), tmp);
					}
				}
			}
		}
		//System.out.println("ACTORTORESOURCEID "+actorToResourceId);
		schedulePipelinedActions = new LinkedList<>();
		// filling the scheduler
		for(int i = 0; i < nIterations; i++) {
			for(Map.Entry<Integer, TimeSlot> t : timeInfoActors.entrySet()) {
				//if(t.getValue().getLength() >0) {
					int actorId		= t.getValue().getActorId();
					//TimeSlot nTs = new TimeSlot(actorId, t.getValue().getStartTime() + heuristic.getPeriod()*i,t.getValue().getEndTime() + heuristic.getPeriod()*i);
					//nTs.setIteration(i);
					Set<Integer> tmp = new HashSet<>(actorToResourceId.get(actorId));
					for(int resource : tmp) {
						TimeSlot clone =  new TimeSlot(actorId, t.getValue().getStartTime() + heuristic.getPeriod()*i,t.getValue().getEndTime() + heuristic.getPeriod()*i); //  new TimeSlot(t.getValue());
						clone.setIteration(i);
						clone.setResourceId(resource);
						schedulePipelinedActions.add(clone);
					}
					
				//}
			}
		}
		
		sortSchedulePipelinedActions();
	}	
    public void	sortSchedulePipelinedActions() {
    	// order queue
    			ArrayList<TimeSlot> q = new ArrayList<TimeSlot>(schedulePipelinedActions);
    			q.sort((o1,o2) -> {
    				int result = o1.getStartTime() - o2.getStartTime();
    				if (result == 0) {
    					if (o1.getLength() == 0 && o2.getLength()==0) {
    						// both are communication tasks
    						CommunicationTask c1 = heuristic.getSetCommunicationTasks().get(o1.getActorId());
    						assert c1 != null : "C1 must never be null!";
    						CommunicationTask c2 = heuristic.getSetCommunicationTasks().get(o2.getActorId());
    						assert c2 != null : "C2 must never be null!";
    						if (c1.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK && c2.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK) {
    							return 0;
    						}else if (c1.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK && c2.getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK) {
    							return 1;
    						}else if (c1.getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK && c2.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK) {
    							return -1;
    						}else
    							return 0;
    					}
    					result = o1.getLength() - o2.getLength();
    				}
    						return result;
    				});
    			
    			
    			schedulePipelinedActions = new LinkedList<TimeSlot>(q);
    }	
	public void savePipelinedSchedule(String path, String fileName, double scaleFactor) throws IOException{
		try{
			File memUtilStatics = new File(path+"/"+fileName+".csv");
			if (memUtilStatics.createNewFile()) {
				System.out.println("File created: " + memUtilStatics.getName());
			} else {
				System.out.println("File already exists.");
			}
		}
		catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	
		FileWriter myWriter = new FileWriter(path+"/"+fileName+".csv"); 
		myWriter.write("Job\tStart\tFinish\tResource\n");
		savePipelinedScheduleStats(myWriter,scaleFactor);
	
	    myWriter.close();
	}
	
	public Queue<TimeSlot> getSchedulePipelinedActions(){
		// order queue
		sortSchedulePipelinedActions();
		
		return this.schedulePipelinedActions;
	}
	
	public void savePipelinedScheduleStats(FileWriter myWriter, double scaleFactor) throws IOException{
		for(TimeSlot  t : schedulePipelinedActions) {
			//System.out.println("Core Type "+e.getKey());
			// e.getKey() resource id
			String resourceName = "SP";
			Processor p = architecture.isProcessor(t.getResourceId());
			Crossbar  c = architecture.isCrossbar(t.getResourceId());
			NoC		noc = architecture.isNoC(t.getResourceId()); 
			if (p!=null) {
				resourceName = p.getName();
			}
			if(c!=null) {
				resourceName = c.getName();
			}
			if(noc != null) {
				resourceName = noc.getName();
			}
			Actor actor = null;
			if (heuristic.getApplication().getActors().containsKey(t.getActorId()))
				actor = heuristic.getApplication().getActors().get(t.getActorId());  
			if (heuristic.getSetCommunicationTasks().containsKey(t.getActorId()))
				actor = heuristic.getSetCommunicationTasks().get(t.getActorId());
			assert actor != null : "This should never happen!!!!!";
			if ((resourceName.compareTo("SP") != 0) )
				myWriter.write(actor.getName()+"\t"+t.getStartTime()*scaleFactor+"\t"+t.getEndTime()*scaleFactor+"\t"+resourceName+"\n");
		}
	}
	
	// here we calculate the distance between two iterations to determine the actual period
	public void calculatePeriod() {
		// key :actor id
		// value : start time
		HashMap<Integer,Double> iterationN  = new HashMap<>();
		HashMap<Integer,Double> iterationN1 = new HashMap<>();
		
		int startIteration = (this.startKernel / heuristic.getPeriod())+1;
		System.out.println("Start iteration "+startIteration);
		for(Map.Entry<Integer, Tile> t : architecture.getTiles().entrySet()) {
			Tile tile = t.getValue();
			for(Map.Entry<Integer, Processor> p : tile.getProcessors().entrySet()) {
				Processor processor = p.getValue();
				LinkedList<Action> scheduledActions = processor.getScheduler().getScheduledActions();
				for(Action action : scheduledActions) {
					if (action.getIteration() == startIteration)
						iterationN.put(action.getActor().getId(), action.getStart_time());
					if (action.getIteration()  == startIteration+1)
						iterationN1.put(action.getActor().getId(), action.getStart_time());
				}
			}
		}
		for(Map.Entry<Integer, Double> i : iterationN.entrySet()) {
			System.out.println("Actor "+application.getActors().get(i.getKey()).getName()+" n: "+i.getValue()+" n+1: "+iterationN1.get(i.getKey())+" diff "+(iterationN1.get(i.getKey())- i.getValue()));
		}
		
		
		
	}
	
	public void saveScheduleAnalysis(String path, ArrayList<String> coreTypes) throws IOException{
		try{
			File memUtilStatics = new File(path+"/heuristicSchedule-ScheduleAnalysis.csv");
			if (memUtilStatics.createNewFile()) {
				System.out.println("File created: " + memUtilStatics.getName());
			} else {
				System.out.println("File already exists.");
			}
		}
		catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	
		FileWriter myWriter = new FileWriter(path+"/heuristicSchedule-ScheduleAnalysis.csv"); 
		myWriter.write("Job\tStart\tFinish\tResource\n");
		saveScheduleAnalysisStats(myWriter, coreTypes);
	
	    myWriter.close();
	}
	
	public void saveScheduleAnalysisStats(FileWriter myWriter, ArrayList<String> coreTypes) throws IOException{
		
		// print the prologue
		for(Map.Entry<Integer,HashMap<Integer, Queue<TimeSlot>>>  s : scheduleAnalysis.entrySet()) {
			HashMap<Integer, Queue<TimeSlot>> mapCore = s.getValue();
			for(Map.Entry<Integer, Queue<TimeSlot>>  m : mapCore.entrySet()) {
				ArrayList<TimeSlot> q = new ArrayList<TimeSlot>(m.getValue());
				for(TimeSlot t: q) {
					myWriter.write(application.getActors().get(t.getActorId()).getName()+"\t"+t.getStartTime()+"\t"+t.getEndTime()+"\t"+coreTypes.get(s.getKey())+","+m.getKey()+"\n");
				}
			}
		}
	}
}
