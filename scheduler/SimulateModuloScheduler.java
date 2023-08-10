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
import multitile.application.Fifo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SimulateModuloScheduler extends BaseScheduler implements Schedule{
	
	private HeuristicModuloSchedulerWithCommunications heuristic;
	// key: core type
	// value:
	//		Key: processor index
	//		Value: queue of actions
	//private HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule;
	private HashMap<Integer,HashMap<Integer,Queue<TimeSlot>>> scheduleAnalysis;
	// the key is the core, crossbar or noc id
	private Queue<TimeSlot> schedulePipelinedActions;
	private int startKernel;
	
	public SimulateModuloScheduler(Architecture architecture, Application application,HeuristicModuloSchedulerWithCommunications heuristic){
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
			endTimes.add(t.getEndTime());
		}
		if (endTimes.size() == 0)
			return 0;
		return Collections.max(endTimes);
	}
	
	public HeuristicModuloSchedulerWithCommunications getHeuristic() {
		return heuristic;
	}
	
	public void createScheduleForAnalysis() {
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
		HashMap<Integer, TreeMap<Integer,Integer> > mapTokensCounting = new HashMap<>();
		for(Map.Entry<Integer, Fifo> f: application.getFifos().entrySet()) {
			TreeMap<Integer,Integer> tokensCounting = new TreeMap<>();
			tokensCounting.put(0, f.getValue().getInitialTokens());
			mapTokensCounting.put(f.getKey(), tokensCounting);
		}
		
		for(TimeSlot t :  schedulePipelinedActions) {
			Actor actor = heuristic.getApplicationWithMessages().getActors().get(t.getActorId());
			if(actor.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK || actor.getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK) {
				CommunicationTask communication = (CommunicationTask)(actor);
				Fifo fifo = communication.getFifo();
				int nTokens = 0;
				if (communication.getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK) nTokens += fifo.getConsRate();
				if (communication.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK) nTokens -= fifo.getProdRate();
				insertCommunicationsInSchedule(mapTokensCounting, fifo, nTokens, t.getEndTime());
			}
		}
		//printCommunicationsInSchedule(mapTokensCounting);
		// then set the FIFO capacities
		for(Map.Entry<Integer, TreeMap<Integer,Integer>> m : mapTokensCounting.entrySet()) {
			TreeMap<Integer,Integer> tokensCounting = m.getValue();
			int fifoCapacity = Collections.max(tokensCounting.values());
			assert fifoCapacity >= 0 : "Capacity must not be negative";
			
			Fifo fifo = application.getFifos().get(m.getKey());
			
			if (fifoCapacity == 0)
				fifoCapacity = (fifo.getConsRate() >= fifo.getProdRate()) ? fifo.getConsRate() : fifo.getProdRate();
				
			application.getFifos().get(m.getKey()).set_capacity(fifoCapacity);
		}
	}
	
	public void printCommunicationsInSchedule(HashMap<Integer, TreeMap<Integer,Integer> > mapTokensCounting) {
		for(Map.Entry<Integer, TreeMap<Integer,Integer>> m : mapTokensCounting.entrySet()) {
			System.err.println("FIFO "+application.getFifos().get(m.getKey()).getName());
			TreeMap<Integer,Integer> tokensCounting = m.getValue();
			for(Map.Entry<Integer, Integer> t: tokensCounting.entrySet()) {
				System.err.println("\tTime "+t.getKey()+" count: "+t.getValue());
			}
		}
	}
	
	public void insertCommunicationsInSchedule(HashMap<Integer, TreeMap<Integer,Integer> > mapTokensCounting, Fifo fifo, int tokens, int time) {
		// tokens might be positive or negative
		// in case of positive, tokens have been produced
		// in case of negative, tokens must be consumed
		//HashMap<Integer, TreeMap<Integer,Integer> > result = mapTokensCounting;
		TreeMap<Integer,Integer> tokensCounting = mapTokensCounting.get(fifo.getId());
		if(tokensCounting.isEmpty()) {
			tokensCounting.put(time, tokens);
		}else {
			if(!tokensCounting.containsKey(time)) {
				ArrayList<Integer> smallIndices = new ArrayList<>();
				for(Map.Entry<Integer, Integer> t : tokensCounting.entrySet()) {
					if (t.getKey() < time)
						smallIndices.add(t.getKey());
				}
				int max = Collections.max(smallIndices);
				int count = tokensCounting.get(max);
				tokensCounting.put(time, count);
			}
			for(Map.Entry<Integer, Integer> t : tokensCounting.entrySet()) {
				if(t.getKey() >= time) {
					int currentTokens = tokensCounting.get(t.getKey());
					tokensCounting.put(t.getKey(), currentTokens + tokens );
				}
			}
		}
		mapTokensCounting.put(fifo.getId(), tokensCounting);
		//return result;
	}
	
	
	// the bindings must be given
	public void createPipelinedScheduler(int nIterations) {
		HashMap<Integer,TimeSlot> timeInfoActors = heuristic.getTimeInfoActors();
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
		System.out.println("ACTORTORESOURCEID "+actorToResourceId);
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
		
		// order queue
		ArrayList<TimeSlot> q = new ArrayList<TimeSlot>(schedulePipelinedActions);
		q.sort((o1,o2) -> o1.getStartTime() - o2.getStartTime());
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
			Actor actor = heuristic.getApplicationWithMessages().getActors().get(t.getActorId());
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
