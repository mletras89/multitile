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
import multitile.architecture.Tile;
import multitile.mapping.Bindings;
import multitile.mapping.Mappings;
import multitile.architecture.Processor;
import multitile.architecture.Architecture;

import multitile.application.Actor;
import multitile.application.Fifo;
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
  private Map<Integer,Integer> countActorFirings;
  private Map<Integer,Integer> fifoCapacities;
  // key actor, value list of keys of predecessors
  private Map<Integer,ArrayList<Integer>> predecessors;
  
  
  public FCFSwithFifoResizing(Architecture architecture, Application application){
    super();
    this.setMaxIterations(1); 
    this.setApplication(application);
    this.setArchitecture(architecture);

  }

  public void schedule(Bindings bindings,Mappings mappings){

  }

  public void  resetCountActorFirings(){
    countActorFirings = new HashMap<>();
    for(Map.Entry<Integer,Actor> a : application.getActors().entrySet()){
      countActorFirings.put( a.getKey(), 0);
    }
  }

  public void setFifosForAnaylisis() {
		for(Map.Entry<Integer, Fifo> f: application.getFifos().entrySet()) {
			f.getValue().set_capacity(Integer.MAX_VALUE);  // this is done for analysis purposes and only applies to FCFC with fifo resizing
			if (f.getValue().isCompositeChannel()) {
				CompositeFifo mrb = (CompositeFifo)f.getValue();
				for(Map.Entry<Integer, Fifo> reader: mrb.getReaders().entrySet()) {
					reader.getValue().set_capacity(Integer.MAX_VALUE);
				}
			}
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
	  if (predecessors.get(actorId).size() == 0)
		  return true;
	  if (getMinCountOfPredecessors(actorId) > countActorFirings.get(actorId))
		  return true;
	  return false;
  }
  
  public void assignValidFifoCapacities() {
	for(Map.Entry<Integer, Integer> e : fifoCapacities.entrySet()) {
  		application.getFifos().get(e.getKey()).set_capacity(e.getValue());
  	}
  }
  
  public void resetFifoCapacities(){
    fifoCapacities = new HashMap<>();
    for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
      fifoCapacities.put(f.getKey(), Integer.MIN_VALUE);
    }
  }

  public boolean updateFifoCapacitiesFromStateOfApplication(){
    // retunrs true if there is a change
    boolean change = false;
    for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
     
      int currentTokens;
      if(f.getValue().isCompositeChannel()) {
    	  CompositeFifo mrb  = (CompositeFifo)f.getValue();
    	  currentTokens = mrb.getTokensInMRB();
      }
      else
    	  currentTokens = f.getValue().get_tokens();
      int currentCapacity = fifoCapacities.get(f.getKey());
      if(currentTokens > currentCapacity){
    	  // then update
    	  fifoCapacities.put(f.getKey(), currentTokens);
    	  change = true;
      }
    }
    return change;
  }

  public void updateFifoCapsToApplication(){
    for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
      // then update
      f.getValue().set_tokens(fifoCapacities.get(f.getKey()));
    }
  }


  public void printFifoCapacities(){
    for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
      System.out.println("Fifo "+f.getValue().getName()+" suggested capacity "+fifoCapacities.get(f.getKey()));
    }
  }


  public int getNumberCurrentIterations(){
     ArrayList<Integer> valueList = new ArrayList<Integer>(countActorFirings.values());
     return Collections.min(valueList);
  }

  public void schedule(Bindings bindings, boolean boundedMemory){
     while(!scheduleFCFS(bindings,boundedMemory));   
  }

  public boolean scheduleFCFS(Bindings bindings,boolean boundedMemory){
	  setPredecessors();
	 // do the re-map of the FIFOs in case to be required
    if (boundedMemory)
      checkAndReMapMemories(bindings);
    
    for(HashMap.Entry<Integer,Tile> t : architecture.getTiles().entrySet()){
      // reseting all the tiles in the architecture
      t.getValue().resetTile();
    }
    resetCountActorFirings();
    resetFifoCapacities();
    application.resetApplication();
    architecture.resetArchitecture();
    updateFifoCapacitiesFromStateOfApplication();

    this.schedulableActors = new ArrayList<>();
    Map<Actor,List<Transfer>> processorReadTransfers = new HashMap<>();
    Map<Actor,List<Transfer>> processorWriteTransfers = new HashMap<>();
    
    //application.printFifosState();

    while(getNumberCurrentIterations() < this.getMaxIterations()){
      // get schedulable actions in all the processors in all the tiles
      this.getSchedulableActors();
      // pop the next actor to be scheduled
      assert this.schedulableActors.size() > 0 : "THIS SHOULD NO HAPPEN!!!";
      for(int actorId : this.schedulableActors){
        //int actorId = this.schedulableActors.remove();
        double processingTime = (double) bindings.getActorProcessorBindings().get(actorId).getProperties().get("runtime");

        // create action to be scheduled
        Action action = new Action(application.getActors().get(actorId));
        action.setProcessingTime(processingTime);
        //System.out.println("Scheduling ... "+action.getActor().getName()); 
        // get processor to execute the action
        int processorID = bindings.getActorProcessorBindings().get(action.getActor().getId()).getTarget().getId();
        int tileId = bindings.getActorTileBindings().get(action.getActor().getId()).getTarget().getId();
        Processor p = architecture.getTiles().get(tileId).getProcessors().get(processorID);

        // schedule the reads
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
          
        // sched the action
        p.getScheduler().commitSingleActionFCFS(action,architecture,application, bindings,1 );

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
        // fire and update the state of the fifos
        application.getActors().get(actorId).fire( application.getFifos() );

        boolean isChange = updateFifoCapacitiesFromStateOfApplication();
        // here check if I have to remap       
        if (isChange == true && boundedMemory){
          boolean remap = checkAndReMapMemories(bindings);
          if (remap == true)
        	  return false;
        }
        //System.out.println("Here!");
        //application.printFifosState();
        countActorFirings.put( actorId, countActorFirings.get(actorId) + 1   );
      }
      //application.printFifosState();
//      break; 
    }
    //System.exit(1);
    return true;
  }



  public void getSchedulableActors(){
    schedulableActors.clear();
    // key, id and value number of current firings
    HashMap<Integer,Integer> mapOcurrences = new HashMap<>();

    for(Map.Entry<Integer,Actor> actor : this.application.getActors().entrySet()) {	
      	//if(this.application.getActors().get(actor.getKey()).canFire(application.getFifos())){
      	if(canFireActor(actor.getKey())) {	
       	  schedulableActors.add(actor.getKey());
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
      schedulableActors.add(selectedKey);
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
  		  // proceed to schedule the routing passes
  		  PassTransferOverArchitecture routing = routings.remove();
  		  scheduledTransfer = schedulePassOfTransfer(temporalTransfer,routing);
  		  temporalTransfer = new Transfer(scheduledTransfer);
  		  temporalTransfer.setStart_time(scheduledTransfer.getDue_time());
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
