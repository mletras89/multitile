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
import multitile.Transfer;
import multitile.architecture.Processor;
import multitile.architecture.Tile;
import multitile.mapping.Bindings;
import multitile.scheduler.UtilizationTable.TimeSlot;
import multitile.architecture.Architecture;
import multitile.application.Actor;
import multitile.application.Application;
import multitile.application.MyEntry;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SimulateModuloScheduler extends BaseScheduler implements Schedule{
	
	private HeuristicModuloScheduler heuristic;
	// key: core type
	// value:
	//		Key: processor index
	//		Value: queue of actions
	//private HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule;
	private HashMap<Integer,HashMap<Integer,Queue<TimeSlot>>> scheduleAnalysis;
	private HashMap<Integer,HashMap<Integer,Queue<TimeSlot>>> schedulePrologue;
	private HashMap<Integer,HashMap<Integer,Queue<Action>>> scheduleActionPrologue;
	private int startKernel;
	// key: pair <tileid, processor id>
	// value: next schedulable actor in such core
	private HashMap<MyEntry<Integer,Integer>,Action> nextSchedulableActors;
	
	public SimulateModuloScheduler(Architecture architecture, Application application,HeuristicModuloScheduler heuristic, Bindings bindings){
		super();
		this.setApplication(application);
		this.setArchitecture(architecture);
		this.heuristic = heuristic;
		nextSchedulableActors = new HashMap<>();
	}

	@Override
	public void schedule(Bindings bindings) {
		// here we perform the scheduling including the communication overheads
		architecture.resetArchitecture();
		application.resetApplication(architecture, bindings, application);
		while(!scheduleHeuristicModulo(scheduleActionPrologue, bindings)) {
			architecture.resetArchitecture();
			application.resetApplication(architecture, bindings, application);
		}
	}	

	private boolean scheduleHeuristicModulo(HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule, Bindings bindings) {
		Map<Actor,List<Transfer>> processorReadTransfers = new HashMap<>();
    	Map<Actor,List<Transfer>> processorWriteTransfers = new HashMap<>();
		
		while(!this.isEmptySchedule(schedule)) {
			getNextSchedulableActors(schedule, bindings);
			this.cleanQueueProcessors();
	    	assert this.nextSchedulableActors.size() > 0 : "THIS SHOULD NO HAPPEN!!!";  // paranoia assert
	    	// assign the actions to the processor
	        for(Map.Entry<MyEntry<Integer,Integer>, Action>  n : nextSchedulableActors.entrySet() ) {
	        	// get the processor
	      	  	Tile t = architecture.getTiles().get(n.getKey().getKey());
	      	  	Processor p = t.getProcessors().get(n.getKey().getValue());
	      	  	//double processingTime = (double)bindings.getActorProcessorBindings().get(n.getValue().getActor().getId()).getProperties().get("runtime");
	      	  	Action a = new Action(n.getValue());
	      	  	p.getScheduler().insertAction(a);
	        }
	        
	        // schedule each of the actions
	        // schedule all the reads
			for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
			  for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
				  Processor p = proc.getValue();
				  if (p.getScheduler().getQueueActions().size()==0)
					  continue;
				  for(Action action : p.getScheduler().getQueueActions()) {
					  if (!action.isSplitNoReads()){
						  // sched reads
						  p.getScheduler().commitReads(action,application.getFifos(),application);
				      	  List<Transfer> readTransfers = p.getScheduler().getReadTransfers().get(action.getActor());
				      	  
				      	  /*for(Transfer t: readTransfers) {
				      		  Fifo f = t.getFifo();
				    		  reads.put(f.getId(),  reads.get(f.getId()) + 1 );
				    		  // update the state of the fifos
				    		  stateChannels = updateStateChannels(initialState, reads, writes);
				    		  boolean resize =  resizeFifo(stateChannels,f);
				    		  if(resize) {
				    			  boolean stateRemap = checkandReMapSingleFifo(bindings,f);
				    			  if (stateRemap)
				    				  return false;
				    		  } 
				      	  }*/
				      	  readTransfers = scheduleTransfers(readTransfers,bindings);
				      	  // udpate events in processor
				          for(Transfer t : readTransfers){
				        	  if(t.getProcessor() != null) {
				            	   int procId = t.getProcessor().getId();
				            	   int tileTId = t.getProcessor().getOwnerTile().getId();
				            	   architecture.getTiles().get(tileTId).getProcessors().get(procId).getScheduler().setLastRead(t.getDue_time());
				              }
				          }
				      	  processorReadTransfers.put(action.getActor(), readTransfers);
				      	  // commit the action in the processor
				      	  p.getScheduler().setReadTransfers(processorReadTransfers);
					  }
				  }
			  }
			}
			// schedule actions
			for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
			  for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
				  Processor p = proc.getValue();
			  	  if (p.getScheduler().getQueueActions().size()==0)
			  		  continue;
			  	  for(Action action : p.getScheduler().getQueueActions()) {
			  		  //System.out.println("Scheduling actor "+action.getActor().getName());
			  		  p.getScheduler().commitSingleAction(action,architecture,application, bindings);  
			  	  }
			  }
			}
			// schedule writes
			for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
			  for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
				  Processor p = proc.getValue();
			      if (p.getScheduler().getQueueActions().size()==0)
			    	  continue;
			      for(Action action : p.getScheduler().getQueueActions()) {
			    	  if (!action.isSplitNoWrites()){
				    	  // sched the writes
				          p.getScheduler().commitWrites(action,application);
				          // put writing transfers to crossbar(s) or NoC
				          // get write transfers from the scheduler
				          List<Transfer> writeTransfers = p.getScheduler().getWriteTransfers().get(action.getActor());
				          /*for(Transfer t : writeTransfers) {
				        	  Fifo f = t.getFifo();
				        	  writes.put(f.getId(),  writes.get(f.getId()) + 1 );
				           	  // update the state of the fifos 
				           	  stateChannels = updateStateChannels(initialState, reads, writes);
				           	  boolean resize =  resizeFifo(stateChannels,f);
				           	  if(resize) {
				           		  boolean stateRemap = checkandReMapSingleFifo(bindings,f);
				    			  if (stateRemap)
				    				  return false;
				           	  } 
				           	}*/
				           processorWriteTransfers.put(action.getActor(), scheduleTransfers(writeTransfers,bindings));
				           // update the write transfers of each processor with the correct start and due time
				           p.getScheduler().setWriteTransfers(processorWriteTransfers);
				           // update the last event in processor, taking into the account the processorWriteTransfers
				           p.getScheduler().updateLastEventAfterWrite(action);
				           // insert the time of the produced tokens by action into the correspondent fifos
				           p.getScheduler().produceTokensinFifo(action,application.getFifos());
			    	  }
			      }
			  }
			}
			// update the fifo
			for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
				for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
					Processor p = proc.getValue();
					if (p.getScheduler().getQueueActions().size()==0)
						continue;
					for(Action action : p.getScheduler().getQueueActions()) {
						//System.out.println("Firing actor "+action.getActor().getName()+" starts "+action.getStart_time()+" ends "+action.getDue_time());
						application.getActors().get(action.getActor().getId()).fire( application.getFifos(), action );
						// and remove the action from the scheduler
						boolean state = removeActionFromSchedule(schedule, action);
						assert state : "ERROOOOOOOR";
					}
				}
			}
		}
		return true;
	}
	
	private boolean isEmptySchedule(HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule) {
		
		for(Map.Entry<Integer,HashMap<Integer,Queue<Action>>> e : schedule.entrySet() ) {
			HashMap<Integer,Queue<Action>> localMap = e.getValue();
			for(Map.Entry<Integer, Queue<Action>> t : localMap.entrySet()) {
				if (t.getValue().size() > 0)
					return false;
			}
		}
		
		return true;
	}
	
	public boolean removeActionFromSchedule(HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule, Action action) {
		for(Map.Entry<Integer,HashMap<Integer,Queue<Action>>> e : schedule.entrySet() ) {
			HashMap<Integer,Queue<Action>> localMap = e.getValue();
			for(Map.Entry<Integer, Queue<Action>> t : localMap.entrySet()) {
				if (t.getValue().size() > 0) {
					if (t.getValue().peek().getActor().getId() == action.getActor().getId()) {
						t.getValue().remove();
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public void getNextSchedulableActors(HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule, Bindings bindings){
		nextSchedulableActors.clear();
		// check if the actor in the top of each queue is fire able
		for(Map.Entry<Integer,HashMap<Integer,Queue<Action>>> e : schedule.entrySet() ) {
			// here the key is the core type
			HashMap<Integer,Queue<Action>> localMap = e.getValue();
			for(Map.Entry<Integer, Queue<Action>> t : localMap.entrySet()) {
				// here the key is the core index
				// the value is the list of actions
				if (t.getValue().size() > 0) {
					// only check the top of each list
					Action candidate = t.getValue().peek();
					//System.out.println("\tCandidate "+candidate.getActor().getName());
					Actor candidateActor = application.getActors().get(candidate.getActor().getId());
					if(candidateActor.canFire(application.getFifos(),candidate)) {
						// put in the list of next schedulableActors
						int tileBinding = bindings.getActorTileBindings().get(candidateActor.getId()).getTarget().getId();
						int coreBinding = bindings.getActorProcessorBindings().get(candidateActor.getId()).getTarget().getId();
						MyEntry<Integer,Integer> key = new MyEntry<Integer,Integer>(tileBinding,coreBinding);
						nextSchedulableActors.put(key, candidate);
					}
				}
			}
		}
	}

	// the bindings must be given
	public void createPrologueScheduler(Bindings bindings) {
		HashMap<Integer,TimeSlot> timeInfoActors = heuristic.getTimeInfoActors();
		assert timeInfoActors != null: "First generate the timeInfoActors";
		assert timeInfoActors.size() != 0 : "First generate the timeInfoActors";
		
		int latency = heuristic.getLantency();
		assert latency != -1 : "First calculate the schedule";
		  
		this.startKernel = ((int)Math.ceil((double)latency/(double)heuristic.getPeriod()))*heuristic.getPeriod(); //   .floorDiv(latency, heuristic.getPeriod()) * heuristic.getPeriod();
		int endKernel = startKernel + heuristic.getPeriod();
		int nIterations = (startKernel / heuristic.getPeriod()) + 40;
		
		System.out.println("\t\tPERIOD :"+heuristic.getPeriod());
		System.out.println("\t\tITERATIONS :"+nIterations);
		System.out.println("\t\tKernel starts at :"+startKernel);
		System.out.println("\t\tKernel ends at :"+endKernel);
		
		// key  : actor id
		// value: core type
		HashMap<Integer,Integer> actorToCoreType = new HashMap<>();
		// key:   actor id
		// value: core id
		HashMap<Integer,Integer> actorToCoreId = new HashMap<>();
		
		UtilizationTable scheduler = heuristic.getScheduler();
		Map<Integer,Map<Integer,LinkedList<TimeSlot>>> U = scheduler.getUtilizationTable();
		schedulePrologue 		= new HashMap<>();
		scheduleActionPrologue 	= new HashMap<>();
		
		// filling auxiliary maps and initializing schedule
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>> u : U.entrySet()) {
			HashMap<Integer,Queue<TimeSlot>> mapCoreQueues = new HashMap<>();
			HashMap<Integer,Queue<Action>> mapCoreQueuesActions = new HashMap<>();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> ts : u.getValue().entrySet()) {
				Queue<TimeSlot> listActions = new LinkedList<>();
				Queue<Action> lActions = new LinkedList<>();
				int coreId = -1;
				for(TimeSlot t : ts.getValue()) {
					Processor p = bindings.getActorProcessorBindings().get(t.getActorId()).getTarget();
					coreId = p.getId();
					actorToCoreType.put(t.getActorId(), u.getKey());
					actorToCoreId.put(t.getActorId(), p.getId());
				}
				if (coreId!=-1) {
					mapCoreQueues.put(coreId, listActions);
					mapCoreQueuesActions.put(coreId, lActions);
				}
			}
			schedulePrologue.put(u.getKey(), mapCoreQueues);
			scheduleActionPrologue.put(u.getKey(), mapCoreQueuesActions);
		}
		// filling the scheduler
		for(int i = 0; i < nIterations; i++) {
			for(Map.Entry<Integer, TimeSlot> t : timeInfoActors.entrySet()) {
				int actorId 	= t.getValue().getActorId();
				int coreId 		= actorToCoreId.get(actorId);
				int coreType 	= actorToCoreType.get(actorId);
				
				Queue<TimeSlot> queue = schedulePrologue.get(coreType).get(coreId);
				//System.out.println("ACTOR "+application.getActors().get(actorId)+" STARTS "+t.getValue().getStartTime()+" ENDS "+t.getValue().getEndTime());
				TimeSlot nTs = new TimeSlot(actorId, t.getValue().getStartTime() + heuristic.getPeriod()*i,t.getValue().getEndTime() + heuristic.getPeriod()*i);
				nTs.setIteration(i);
				queue.add(nTs);
				
				schedulePrologue.get(coreType).put(coreId, queue);
			}
		}
		// order map
		for(Map.Entry<Integer,HashMap<Integer, Queue<TimeSlot>>>  s : schedulePrologue.entrySet()) {
			HashMap<Integer, Queue<TimeSlot>> mapCore = s.getValue();
			for(Map.Entry<Integer, Queue<TimeSlot>>  m : mapCore.entrySet()) {
				ArrayList<TimeSlot> q = new ArrayList<TimeSlot>(m.getValue());
				// sort the q
				q.sort((o1,o2) -> o1.getStartTime() - o2.getStartTime());
				Queue<TimeSlot> orderedResult = new LinkedList<TimeSlot>(q);
				schedulePrologue.get(s.getKey()).put(m.getKey(),orderedResult);     // ordering the schedule PARANOIA
			}
		}
		// fill the action map
		for(Map.Entry<Integer,HashMap<Integer, Queue<TimeSlot>>>  s : schedulePrologue.entrySet()) {
			HashMap<Integer, Queue<TimeSlot>> mapCore = s.getValue();
			for(Map.Entry<Integer, Queue<TimeSlot>>  m : mapCore.entrySet()) {
				int coreType = s.getKey();
				int coreId   = m.getKey();
				for(TimeSlot t : m.getValue()) {
					int actorId 	= t.getActorId();
					double runtime = (double)bindings.getActorProcessorBindings().get(actorId).getProperties().get("runtime"); 
					Processor p = bindings.getActorProcessorBindings().get(actorId).getTarget();
					Action action = new Action(application.getActors().get(actorId),runtime);
					action.setProcessor(p);
					action.setIteration(t.getIteration());
					Queue<Action> queue = scheduleActionPrologue.get(coreType).get(coreId);
					queue.add(action);
					scheduleActionPrologue.get(coreType).put(coreId, queue);					
				}
			}
		}
	}	
		
	public void savePrologue(String path, ArrayList<String> coreTypes) throws IOException{
		try{
			File memUtilStatics = new File(path+"/heuristicSchedule-Prologue.csv");
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
	
		FileWriter myWriter = new FileWriter(path+"/heuristicSchedule-Prologue.csv"); 
		myWriter.write("Job\tStart\tFinish\tResource\n");
		savePrologueStats(myWriter, coreTypes);
	
	    myWriter.close();
	}
	
	public void savePrologueStats(FileWriter myWriter, ArrayList<String> coreTypes) throws IOException{
		
		// print the prologue
		for(Map.Entry<Integer,HashMap<Integer, Queue<TimeSlot>>>  s : schedulePrologue.entrySet()) {
			HashMap<Integer, Queue<TimeSlot>> mapCore = s.getValue();
			for(Map.Entry<Integer, Queue<TimeSlot>>  m : mapCore.entrySet()) {
				ArrayList<TimeSlot> q = new ArrayList<TimeSlot>(m.getValue());
				for(TimeSlot t: q) {
					myWriter.write(application.getActors().get(t.getActorId()).getName()+"\t"+t.getStartTime()+"\t"+t.getEndTime()+"\t"+coreTypes.get(s.getKey())+","+m.getKey()+"\n");
				}
			}
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
	
	/*public void createScheduler(HashMap<Integer,HashMap<Integer,Queue<Action>>> schedule, Bindings bindings) {
		// I need a map to store the runtime
		HashMap<Integer,Double> toScheduleRuntime = new HashMap<>();
		for(Map.Entry<Integer, Binding<Processor>>  b :  bindings.getActorProcessorBindings().entrySet()) {
			double runtime =  (double)b.getValue().getProperties().get("runtime");
			toScheduleRuntime.put(b.getKey(), runtime);
		}
		
		
		UtilizationTable scheduler = heuristic.getScheduler();
		Map<Integer,Map<Integer,LinkedList<TimeSlot>>> U = scheduler.getUtilizationTable();
		
		schedule = new HashMap<>();
		
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>> u : U.entrySet()) {
			HashMap<Integer,Queue<Action>> mapCoreQueues = new HashMap<>();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> ts : u.getValue().entrySet()) {
				Queue<Action> listActions = new LinkedList<>();
				for(TimeSlot t : ts.getValue()) {
					Action action = new Action(application.getActors().get(t.getActorId()));
					// if the timeslot is split assign it to the action
					if (t.isSplit()) {
						if(t.getStartTime() == 0) {
							action.setSplitNoReads(true);
							//System.out.println("SPLIT NO READS "+application.getActors().get(t.getActorId()).getName());
						}
						else {
							action.setSplitNoWrites(true);
							//System.out.println("SPLIT NO WRITES "+application.getActors().get(t.getActorId()).getName());
						}
					}
					// get the binding runtime
					double runtime = (double)bindings.getActorProcessorBindings().get(t.getActorId()).getProperties().get("runtime");
					if(!t.isSplit()) {
						action.setProcessingTime(runtime);
						toScheduleRuntime.put(t.getActorId(), 0.0);
					}else {
						// I have to partition the runtime
						if(toScheduleRuntime.get(t.getActorId()) == runtime ) {
							// the split happen a time zero and no reads are required
							double sRuntime = t.getLength() * this.scaleFactor;
							action.setProcessingTime(sRuntime);
							toScheduleRuntime.put(t.getActorId(), runtime - sRuntime);
						}else {
							// the split happen at the end and no writes are required
							action.setProcessingTime(toScheduleRuntime.get(t.getActorId()));
							toScheduleRuntime.put(t.getActorId(), 0.0);
						}
					}
					listActions.add(action);  
				}
				mapCoreQueues.put(ts.getKey(), listActions);
			}
			schedule.put(u.getKey(), mapCoreQueues);
		}
		// paranoia
		for(Map.Entry<Integer, Double> t : toScheduleRuntime.entrySet()) {
			assert t.getValue() == 0: "All values should be zero";
		}
	}*/
	
	// the bindings must be given
	public void createAnalysisScheduler(Bindings bindings) {
		HashMap<Integer,TimeSlot> timeInfoActors = heuristic.getTimeInfoActors();
		assert timeInfoActors != null: "First generate the timeInfoActors";
		assert timeInfoActors.size() != 0 : "First generate the timeInfoActors";
		
		int latency = heuristic.getLantency();
		assert latency != -1 : "First calculate the schedule";
		  
		// key  : actor id
		// value: core type
		HashMap<Integer,Integer> actorToCoreType = new HashMap<>();
		// key:   actor id
		// value: core id
		HashMap<Integer,Integer> actorToCoreId = new HashMap<>();
		
		UtilizationTable scheduler = heuristic.getScheduler();
		Map<Integer,Map<Integer,LinkedList<TimeSlot>>> U = scheduler.getUtilizationTable();
		scheduleAnalysis = new HashMap<>();
		
		// filling auxiliary maps and initializing schedule
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>> u : U.entrySet()) {
			HashMap<Integer,Queue<TimeSlot>> mapCoreQueues = new HashMap<>();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> ts : u.getValue().entrySet()) {
				Queue<TimeSlot> listActions = new LinkedList<>();
				for(TimeSlot t : ts.getValue()) {
					actorToCoreType.put(t.getActorId(), u.getKey());
					actorToCoreId.put(t.getActorId(), ts.getKey());
				}
				mapCoreQueues.put(ts.getKey(), listActions);
			}
			scheduleAnalysis.put(u.getKey(), mapCoreQueues);
		}
		
		// filling the scheduler
		for(int i = this.startKernel/heuristic.getPeriod(); i < ((this.startKernel/heuristic.getPeriod())+2); i++) {
			for(Map.Entry<Integer, TimeSlot> t : timeInfoActors.entrySet()) {
				int actorId 	= t.getValue().getActorId();
				int coreId 		= actorToCoreId.get(actorId);
				int coreType 	= actorToCoreType.get(actorId);
				Queue<TimeSlot> queue = scheduleAnalysis.get(coreType).get(coreId);
				
				//System.out.println("ACTOR "+application.getActors().get(actorId)+" STARTS "+t.getValue().getStartTime()+" ENDS "+t.getValue().getEndTime());
				//double runtime = (double)bindings.getActorProcessorBindings().get(actorId).getProperties().get("runtime");
				TimeSlot action =  new TimeSlot(actorId, t.getValue().getStartTime() + heuristic.getPeriod()*i,t.getValue().getEndTime() + heuristic.getPeriod()*i);// new Action(application.getActors().get(actorId),runtime); 
				queue.add(action);
				scheduleAnalysis.get(coreType).put(coreId, queue);
			}
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
