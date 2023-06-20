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
  @date   02 November 2022
  @version 1.1
  @ brief
     This class describes an FCFS scheduler
--------------------------------------------------------------------------
*/
package multitile.scheduler;

import multitile.Action;
import multitile.Transfer;
//import multitile.architecture.Tile;
import multitile.mapping.Bindings;
import multitile.mapping.Mappings;
import multitile.architecture.Processor;
import multitile.architecture.Tile;
import multitile.architecture.Architecture;

import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.application.MyEntry;
import multitile.application.Application;
import multitile.application.CompositeFifo;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.ArrayList; 
import java.util.Collections;  


public class FCFSwithFifoResizing extends BaseScheduler implements Schedule{

  private ArrayList<Integer> schedulableActors;
  // key is the <tileid, processor id>, and the val is the next schedulable actors
  private HashMap<MyEntry<Integer,Integer>,Integer> nextSchedulableActors;
  
  private Map<Integer,Integer> countActorFirings;
  
  // key actor, value list of keys of predecessors
  private Map<Integer,ArrayList<Integer>> predecessors;
  
  
  public FCFSwithFifoResizing(Architecture architecture, Application application){
    super();
    this.setMaxIterations(1); 
    this.setApplication(application);
    this.setArchitecture(architecture);
    this.setPredecessors();
  }

  public void schedule(Bindings bindings,Mappings mappings){

  }

  public void  resetCountActorFirings(){
    countActorFirings = new HashMap<>();
    for(Map.Entry<Integer,Actor> a : application.getActors().entrySet()){
      countActorFirings.put( a.getKey(), 0);
    }
  }


  
  public void setPredecessors() {
	// key actor, value list of keys of predecessors
	predecessors = new HashMap<Integer,ArrayList<Integer>>();
	for(Map.Entry<Integer, Actor> a : application.getActors().entrySet()) {
		ArrayList<Integer> preds = new ArrayList<Integer>();
		for(Map.Entry<Integer, Fifo> fifo : application.getFifos().entrySet()) {
			if (fifo.getValue().isCompositeChannel() == false) {
				if (fifo.getValue().getDestination().getId() == a.getKey()) {
					preds.add(fifo.getValue().getSource().getId() );
				}	
			}else {
				CompositeFifo mrb = (CompositeFifo)fifo.getValue();
				for(Map.Entry<Integer, Fifo> reader : mrb.getReaders().entrySet()) {
					if(reader.getValue().getDestination().getId() == a.getKey()) {
						preds.add(mrb.getSource().getId());
					}
				}
			}
		}
		predecessors.put(a.getKey(), preds);
	}
  }
  
  public int getMinCountOfPredecessors(int actorId) {
	  ArrayList<Integer> preds = predecessors.get(actorId);
	  int min = Integer.MAX_VALUE;
	  for(int pred : preds) {
		  int firingCount = countActorFirings.get(pred);
		  if (firingCount <= min)
			  min = firingCount;
	  }
	  return min;
  }
  
  public boolean canFireActor(int actorId) {
	  if (!(countActorFirings.get(actorId) < this.getMaxIterations()))
	  	return false;
	  if (predecessors.get(actorId).size() == 0)
		  return true;
	  if (getMinCountOfPredecessors(actorId) > countActorFirings.get(actorId))
		  return true;
	  return false;
  }
  


  public int getNumberCurrentIterations(){
     ArrayList<Integer> valueList = new ArrayList<Integer>(countActorFirings.values());
     return Collections.min(valueList);
  }

  public void schedule(Bindings bindings, boolean boundedMemory){
     while(!scheduleFCFS(bindings,boundedMemory));   
  }

  public void scheduleUpperBound(Bindings bindings, boolean boundedMemory){
	  while(!upperBound(bindings,boundedMemory));
  }
  
  
  public boolean upperBound(Bindings bindings,boolean boundedMemory){
	  
	 // do the re-map of the FIFOs in case to be required
    if (boundedMemory)
      checkAndReMapMemories(bindings);
    
    resetCountActorFirings();
    resetFifoCapacities();
    application.resetApplication();
    architecture.resetArchitecture();
    updateFifoCapacitiesFromStateOfApplication();

    this.schedulableActors = new ArrayList<>();
    this.nextSchedulableActors = new HashMap<>();
    Map<Actor,List<Transfer>> processorReadTransfers = new HashMap<>();
    Map<Actor,List<Transfer>> processorWriteTransfers = new HashMap<>();
    //application.printFifosState();
    while(getNumberCurrentIterations() < this.getMaxIterations()){
      // get schedulable actions in all the processors in all the tiles
      this.getSchedulableActors(bindings);
      this.cleanQueueProcessors();
      // pop the next actor to be scheduled
      assert this.nextSchedulableActors.size() > 0 : "THIS SHOULD NO HAPPEN!!!";
      // assign the actions to the processor
      for(Map.Entry<MyEntry<Integer,Integer>, Integer>  n : nextSchedulableActors.entrySet() ) {
    	  // get the processor
    	  Tile t = architecture.getTiles().get(n.getKey().getKey());
    	  Processor p = t.getProcessors().get(n.getKey().getValue());
    	  double processingTime = (double)bindings.getActorProcessorBindings().get(n.getValue()).getProperties().get("runtime");
    	  Action a = new Action(application.getActors().get(n.getValue()),processingTime);
    	  p.getScheduler().insertAction(a);
      }
      //schedule all the reads
      for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
    	  for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
    		  Processor p = proc.getValue();
    		  if (p.getScheduler().getQueueActions().size()==0)
    			  continue;
    		  for(Action action : p.getScheduler().getQueueActions()) {
    			  p.getScheduler().commitReadsFCFS(action,application.getFifos(),application,architecture);
    			  List<Transfer> readTransfers = p.getScheduler().getReadTransfers().get(action.getActor());
    			  readTransfers = scheduleTransfers(readTransfers,bindings);
    			  // udpate events in processor
    		      for(Transfer t : readTransfers){
    		    	  if (t.getProcessor() != null) {
    		    		  int procId = t.getProcessor().getId();
    		    		  int tileTId = t.getProcessor().getOwnerTile().getId();
    		    		  architecture.getTiles().get(tileTId).getProcessors().get(procId).getScheduler().setLastRead(t.getDue_time());
    		          }
    		      }

    		      processorReadTransfers.put(action.getActor(), readTransfers);
    		      // commit the reads in the processor
    		      p.getScheduler().setReadTransfers(processorReadTransfers);
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
    			  // sched the action
    			  p.getScheduler().commitSingleActionFCFS(action,architecture,application, bindings,1 );
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
    			  // sched the writes
    		      p.getScheduler().commitWrites(action,application);
    		      // put writing transfers to crossbar(s) or NoC
    		      // get write transfers from the scheduler
    		      List<Transfer> writeTransfers = p.getScheduler().getWriteTransfers().get(action.getActor());
    		      processorWriteTransfers.put(action.getActor(), scheduleTransfers(writeTransfers,bindings));
    		      // update the write transfers of each processor with the correct start and due time
    		      p.getScheduler().setWriteTransfers(processorWriteTransfers);
    		      // update the last event in processor, taking into the account the processorWriteTransfers
    		      p.getScheduler().updateLastEventAfterWrite(action);
    		      // insert the time of the produced tokens by action into the correspondent fifos
    		      p.getScheduler().produceTokensinFifo(action,application.getFifos());
    		      // update the state of the application
    		      application.getActors().get(action.getActor().getId()).fire( application.getFifos() );
    		      boolean isChange = updateFifoCapacitiesFromStateOfApplication();
    		      // here check if I have to remap       
    		      if (isChange == true && boundedMemory){
    		    	  boolean remap = checkAndReMapMemories(bindings);
    		          if (remap == true)
    		        	  return false;
    		        	}
    		      countActorFirings.put( action.getActor().getId(), countActorFirings.get(action.getActor().getId()) + 1   );
    		  }
    	  }
      }
    }
    return true;
  }
  
  
  
  public boolean scheduleFCFS(Bindings bindings,boolean boundedMemory){
	  
	 // do the re-map of the FIFOs in case to be required
    if (boundedMemory)
      checkAndReMapMemories(bindings);
    
    resetCountActorFirings();
    resetFifoCapacities();
    application.resetApplication();
    architecture.resetArchitecture();
    updateFifoCapacitiesFromStateOfApplication();

    this.schedulableActors = new ArrayList<>();
    this.nextSchedulableActors = new HashMap<>();
    Map<Actor,List<Transfer>> processorReadTransfers = new HashMap<>();
    Map<Actor,List<Transfer>> processorWriteTransfers = new HashMap<>();
    //application.printFifosState();
    while(getNumberCurrentIterations() < this.getMaxIterations()){
      // get schedulable actions in all the processors in all the tiles
      this.getSchedulableActors(bindings);
      this.cleanQueueProcessors();
      // pop the next actor to be scheduled
      assert this.nextSchedulableActors.size() > 0 : "THIS SHOULD NO HAPPEN!!!";
      // assign the actions to the processor
      for(Map.Entry<MyEntry<Integer,Integer>, Integer>  n : nextSchedulableActors.entrySet() ) {
    	  // get the processor
    	  Tile t = architecture.getTiles().get(n.getKey().getKey());
    	  Processor p = t.getProcessors().get(n.getKey().getValue());
    	  double processingTime = (double)bindings.getActorProcessorBindings().get(n.getValue()).getProperties().get("runtime");
    	  Action a = new Action(application.getActors().get(n.getValue()),processingTime);
    	  p.getScheduler().insertAction(a);
      }
      //schedule all the reads
      for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
    	  for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
    		  Processor p = proc.getValue();
    		  if (p.getScheduler().getQueueActions().size()==0)
    			  continue;
    		  for(Action action : p.getScheduler().getQueueActions()) {
    			  p.getScheduler().commitReadsFCFS(action,application.getFifos(),application,architecture);
    			  List<Transfer> readTransfers = p.getScheduler().getReadTransfers().get(action.getActor());
    			  readTransfers = scheduleTransfers(readTransfers,bindings);
    			  // udpate events in processor
    		      for(Transfer t : readTransfers){
    		    	  if (t.getProcessor() != null) {
    		    		  int procId = t.getProcessor().getId();
    		    		  int tileTId = t.getProcessor().getOwnerTile().getId();
    		    		  architecture.getTiles().get(tileTId).getProcessors().get(procId).getScheduler().setLastRead(t.getDue_time());
    		          }
    		      }

    		      processorReadTransfers.put(action.getActor(), readTransfers);
    		      // commit the reads in the processor
    		      p.getScheduler().setReadTransfers(processorReadTransfers);
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
    			  // sched the action
    			  p.getScheduler().commitSingleActionFCFS(action,architecture,application, bindings,1 );
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
    			  // sched the writes
    		      p.getScheduler().commitWrites(action,application);
    		      // put writing transfers to crossbar(s) or NoC
    		      // get write transfers from the scheduler
    		      List<Transfer> writeTransfers = p.getScheduler().getWriteTransfers().get(action.getActor());
    		      processorWriteTransfers.put(action.getActor(), scheduleTransfers(writeTransfers,bindings));
    		      // update the write transfers of each processor with the correct start and due time
    		      p.getScheduler().setWriteTransfers(processorWriteTransfers);
    		      // update the last event in processor, taking into the account the processorWriteTransfers
    		      p.getScheduler().updateLastEventAfterWrite(action);
    		      // insert the time of the produced tokens by action into the correspondent fifos
    		      p.getScheduler().produceTokensinFifo(action,application.getFifos());
    		      // update the state of the application
    		      application.getActors().get(action.getActor().getId()).fire( application.getFifos() );
    		      boolean isChange = updateFifoCapacitiesFromStateOfApplication();
    		      // here check if I have to remap       
    		      if (isChange == true && boundedMemory){
    		    	  boolean remap = checkAndReMapMemories(bindings);
    		          if (remap == true)
    		        	  return false;
    		        	}
    		      countActorFirings.put( action.getActor().getId(), countActorFirings.get(action.getActor().getId()) + 1   );
    		  }
    	  }
      }
    }
    return true;
  }

  public void getSchedulableActors(Bindings bindings){
    schedulableActors.clear();
    nextSchedulableActors.clear();
    
    // key, id and value number of current firings
    HashMap<Integer,Integer> mapOcurrences = new HashMap<>();
    ArrayList<Integer> potentialSched = new ArrayList<>();
    for(Map.Entry<Integer,Actor> actor : this.application.getActors().entrySet()) {	
      	//if(this.application.getActors().get(actor.getKey()).canFire(application.getFifos()) && countActorFirings.get(actor.getKey()) < this.getMaxIterations()){
      	//if(canFireActor(actor.getKey()) && countActorFirings.get(actor.getKey()) < this.getMaxIterations() ) {
    	if(canFireActor(actor.getKey())  ) {
      	  //Processor core = bindings.getActorProcessorBindings().get(actor.getKey()).getTarget();
      	  //if (!nextSchedulableActors.containsKey( core.getId() )) {
      	  //	  nextSchedulableActors.put(new MyEntry<Integer,Integer>(core.getOwnerTile().getId(),core.getId()), actor.getKey());
      	  //}
       	  //schedulableActors.add(actor.getKey());
          mapOcurrences.put(actor.getKey(), countActorFirings.get(actor.getKey()));
      	}
    }
    // the order: first those with less number of firings
    int nEntries = mapOcurrences.size();
    for(int i=0; i < nEntries; i++){
      int selectedKey=0;
      int minVal = Integer.MAX_VALUE;
      for(Map.Entry<Integer,Integer> e : mapOcurrences.entrySet()){
        if (e.getValue() <= minVal){
          minVal = e.getValue();
          selectedKey = e.getKey();
        }
      }
      // remove val and key from map
      mapOcurrences.remove(selectedKey);
      // update schedulable actors
      potentialSched.add(selectedKey);
    }
    for(int s : potentialSched) {
    	Processor core = bindings.getActorProcessorBindings().get(s).getTarget();
    	if (!nextSchedulableActors.containsKey(new MyEntry<Integer,Integer>(core.getOwnerTile().getId(),core.getId())) ) {
    		  nextSchedulableActors.put(new MyEntry<Integer,Integer>(core.getOwnerTile().getId(),core.getId()), s);
    	}
    }
  }

  


  

  public List<Transfer> scheduleTransfers(List<Transfer> transfers,Bindings bindings){
    List<Transfer> listSchedTransfers = new ArrayList<Transfer>();
    //    for each transfer calculate the path that has to travel, might be comming from the tile local crossbar,
    //    or the transfer has to travel across several interconnect elements, a read comming from NoC has to travel 
    //    NoC -> TileLocal Crossbar -> Processor
    //    other example es when the transfer source is a local memory of other processor placed in a different tile
    //    Processor1 -> Tile local Crossbar of Processor 1 -> NoC -> TileLocal Crossbar of Processor 2 -> Processor 2  
    for(Transfer transfer : transfers){
  	  Queue<PassTransferOverArchitecture> routings = calculatePathOfTransfer(transfer,bindings);
  	  int routingsLength = routings.size();
  	  Transfer scheduledTransfer = null;
  	  Transfer temporalTransfer = new Transfer(transfer);
  	  
  	  for(int m=0; m<routingsLength;m++){
  		  if (routingsLength > 1)
  			  temporalTransfer.setTransferWithNoC(architecture.getNoC());
  		  // proceed to schedule the routing passes
  		  PassTransferOverArchitecture routing = routings.remove();
  		  scheduledTransfer = schedulePassOfTransfer(temporalTransfer,routing);
  		  temporalTransfer = new Transfer(scheduledTransfer);
  		  temporalTransfer.setStart_time(scheduledTransfer.getStart_time());
  	  }

  	  if(scheduledTransfer == null){
  		  // if we reach this part, means that the transfer does not cost and is a writing to processor local memory
  		  scheduledTransfer = new Transfer(transfer);
  		  scheduledTransfer.setDue_time(scheduledTransfer.getStart_time());
  	  }
	  listSchedTransfers.add(scheduledTransfer);
    }
    return listSchedTransfers;

  } 

}
