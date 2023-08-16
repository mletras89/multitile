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
  @date   07 December
  @version 1.1
  @ brief
     This class is the basis of an scheduler implementation
--------------------------------------------------------------------------
*/

package multitile.scheduler;

import multitile.Action;
import multitile.Transfer;

import multitile.architecture.Processor;
import multitile.architecture.Tile;
import multitile.architecture.Architecture;
import multitile.architecture.ArchitectureManagement;
import multitile.architecture.LocalMemory;
import multitile.architecture.Memory;
import multitile.architecture.TileLocalMemory;
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.application.Application;
import multitile.application.ApplicationManagement;
import multitile.application.CompositeFifo;
import multitile.application.Fifo;

import java.util.*;

public class BaseScheduler{
  // key is the step and the list are the actions scheduled in the step
  private HashMap<Integer,List<Action>> scheduledStepActions;
  private Map<Integer,Integer> fifoCapacities;
  
  private int maxIterations;
  public Architecture architecture;
  public Application application;
  public Queue<Action> queueActions;

  public BaseScheduler(){
    this.queueActions = new LinkedList<>();
    this.scheduledStepActions = new HashMap<>();
  }

  public HashMap<Integer,List<Action>> getScheduledStepActions(){
    return this.scheduledStepActions;
  }

  public Application getApplication() {
	  return application;
  }
  
  public void setMaxIterations(int maxIterations){
    this.maxIterations = maxIterations;
  }

  public int getMaxIterations(){
    return this.maxIterations;
  }

  public void setApplication(Application application){
    this.application = application; //new Application(application);
  }

  public void setArchitecture(Architecture architecture){
    this.architecture = architecture; //new Architecture(architecture);
  }
  
  public Architecture getArchitecture(){
	    return this.architecture;
  }
  
  public void insertAction(Action a){
    queueActions.add(new Action(a));
  }

  public void cleanQueue(){
    this.queueActions.clear();
  }

  public Transfer schedulePassOfTransfer(Transfer t, PassTransferOverArchitecture routing){
    Transfer schedTransfer = null;
    if (routing.getType() == PassTransferOverArchitecture.PASS_TYPE.NOC)
      // schedule transfer in the NoC
      schedTransfer = architecture.getNoC().putTransferInNoC(t);
    else if(routing.getType() == PassTransferOverArchitecture.PASS_TYPE.CROSSBAR)
      // schedule transfer in the crossbar
      schedTransfer = architecture.getCrossbar(routing.getCrossbar().getId()).putTransferInCrossbar(t);

    return schedTransfer;
  }

  public Queue<PassTransferOverArchitecture> calculatePathOfTransfer(Transfer transfer,Bindings bindings){
    // this function returns a list of interconnect sequences
    Queue<PassTransferOverArchitecture> sequence = new LinkedList<>();

    if (transfer.getType() == Transfer.TRANSFER_TYPE.READ){
      // then here the source is the memory and the destination is the processor
      Memory source           = bindings.getFifoMemoryBindings().get(transfer.getFifo().getId()).getTarget();

      Processor destination   = bindings.getActorProcessorBindings().get(transfer.getActor().getId()).getTarget();
      Tile destinationTile    = bindings.getActorTileBindings().get(transfer.getActor().getId()).getTarget();
      
      switch(source.getType()){
        case GLOBAL_MEM:
          // this is the easiest case, the sequence es GlobalMemory -> NoC -> Tile local crossbar -> processor
          sequence.add(new PassTransferOverArchitecture(architecture.getNoC()));
          PassTransferOverArchitecture crossbarOverNoC = new PassTransferOverArchitecture(destinationTile.getCrossbar());
          crossbarOverNoC.setNoC(architecture.getNoC());
          sequence.add( crossbarOverNoC );
        break;
    
        case TILE_LOCAL_MEM:
          Tile sourceTile         = ((TileLocalMemory)source).getOwnerTile();
          // here is a bit more complex, if both the source and the destination are in the same tile
          // TILE_LOCAL_MEM -> CROSSBAR TILE LOCAL -> processor
          if (sourceTile.equals(destinationTile)){
            sequence.add(new PassTransferOverArchitecture(destinationTile.getCrossbar()));  
          }else{
          // if source and destination are not in the same tile
          // TILE_LOCAL_MEM_T1 -> CROSSBAR T1 -> NoC -> CROSSBAR T2 -> processor
        	sequence.add(new PassTransferOverArchitecture(architecture.getNoC()));
        	PassTransferOverArchitecture srcCrossbarOverNoC = new PassTransferOverArchitecture(sourceTile.getCrossbar());
            PassTransferOverArchitecture dstDrossbarOverNoC = new PassTransferOverArchitecture(destinationTile.getCrossbar());
            srcCrossbarOverNoC.setNoC(architecture.getNoC());
            dstDrossbarOverNoC.setNoC(architecture.getNoC());
            sequence.add(srcCrossbarOverNoC);
            sequence.add(dstDrossbarOverNoC);
          }
        break;

        case LOCAL_MEM:
          Processor localMemOwner = source.getEmbeddedToProcessor();
          Tile tileSource = localMemOwner.getOwnerTile();
          // mapped to different processors but in the same tile
          if (!localMemOwner.equals(destination) && destinationTile.equals(tileSource)){
            // the sequence must be MEM_SOURCE -> CROSSBAR -> processor
            sequence.add(new PassTransferOverArchitecture(destinationTile.getCrossbar()));
          }else if(!localMemOwner.equals(destination) && !destinationTile.equals(tileSource)){
            // the sequence must be MEM_SOURCE -> CROSSBAR_SOURCE -> NoC -> CROSSBAR_DEST -> processor
        	sequence.add(new PassTransferOverArchitecture(architecture.getNoC()));
        	PassTransferOverArchitecture srcCrossbarOverNoC = new PassTransferOverArchitecture(tileSource.getCrossbar());
            PassTransferOverArchitecture dstDrossbarOverNoC = new PassTransferOverArchitecture(destinationTile.getCrossbar());
            srcCrossbarOverNoC.setNoC(architecture.getNoC());
            dstDrossbarOverNoC.setNoC(architecture.getNoC());
            sequence.add(srcCrossbarOverNoC);
            sequence.add(dstDrossbarOverNoC);
          }else {
        	  // the memory is the scratchpad memory
        	  LocalMemory local = (LocalMemory)source;
        	  PassTransferOverArchitecture sp = new PassTransferOverArchitecture(localMemOwner,local);
        	  sequence.add(sp);
          }
        break;
      } 
    }
    if(transfer.getType() == Transfer.TRANSFER_TYPE.WRITE){
      // here the source is the processor and the destination is the memory
      Processor source    = bindings.getActorProcessorBindings().get(transfer.getActor().getId()).getTarget();
      Tile sourceTile     = bindings.getActorTileBindings().get(transfer.getActor().getId()).getTarget();
      
      Memory destination  = bindings.getFifoMemoryBindings().get(transfer.getFifo().getId()).getTarget();
      Tile destinationTile;
      switch(destination.getType()){
        case GLOBAL_MEM:
          // SOURCE_CROSSBAR -> Noc -> GLOBAL MEMORY
          sequence.add(new PassTransferOverArchitecture(architecture.getNoC()) );
          PassTransferOverArchitecture crossbarOverNoC = new PassTransferOverArchitecture(sourceTile.getCrossbar());
          crossbarOverNoC.setNoC(architecture.getNoC());
          sequence.add( crossbarOverNoC );
        break;

        case TILE_LOCAL_MEM:
          destinationTile = ((TileLocalMemory)destination).getOwnerTile();
          // here is a bit more complex, if both source and destination are in the same tile
          // source -> source CROSSBAR -> TILE_LOCAL_MEM
          if(sourceTile.equals(destinationTile)){
            sequence.add(new PassTransferOverArchitecture(destinationTile.getCrossbar()));
          }else{
            // if source and destination are not in the same tile
            // processor -> CROSSBAR SOURCE -> NoC -> CROSSBAR DESTINATION -> TILE_LOCAL_MEM
        	sequence.add(new PassTransferOverArchitecture(architecture.getNoC()));
        	PassTransferOverArchitecture srcCrossbarOverNoC = new PassTransferOverArchitecture(sourceTile.getCrossbar());
            PassTransferOverArchitecture dstDrossbarOverNoC = new PassTransferOverArchitecture(destinationTile.getCrossbar());
            srcCrossbarOverNoC.setNoC(architecture.getNoC());
            dstDrossbarOverNoC.setNoC(architecture.getNoC());
            sequence.add(srcCrossbarOverNoC);
            sequence.add(dstDrossbarOverNoC);     
          }
        break;

        case LOCAL_MEM:
          Processor localMemOwner = destination.getEmbeddedToProcessor();
          destinationTile    = localMemOwner.getOwnerTile();
          //mapped to different processors but in the same tile          
          if (!localMemOwner.equals(source) && destinationTile.equals(sourceTile)){
            // the sequence must be MEM_SOURCE -> CROSSBAR -> processor
            sequence.add(new PassTransferOverArchitecture(destinationTile.getCrossbar()));
          }else if(!localMemOwner.equals(source) && !destinationTile.equals(sourceTile)){
            // the sequence must be MEM_SOURCE -> CROSSBAR_SOURCE -> NoC -> CROSSBAR_DEST -> processor
        	sequence.add(new PassTransferOverArchitecture(architecture.getNoC()));
        	PassTransferOverArchitecture srcCrossbarOverNoC = new PassTransferOverArchitecture(sourceTile.getCrossbar());
            PassTransferOverArchitecture dstDrossbarOverNoC = new PassTransferOverArchitecture(destinationTile.getCrossbar());
            srcCrossbarOverNoC.setNoC(architecture.getNoC());
            dstDrossbarOverNoC.setNoC(architecture.getNoC());
            sequence.add(srcCrossbarOverNoC);
            sequence.add(dstDrossbarOverNoC);   
          }else {
        	// the memory is the scratchpad memory
        	LocalMemory local = (LocalMemory)destination;
        	PassTransferOverArchitecture sp = new PassTransferOverArchitecture(localMemOwner,local);
        	sequence.add(sp);
          }
        break;
      }
    }
    return sequence;
  }
  
  public ArrayList<Fifo> checkMemorySize(Bindings bindings) {
	  ArrayList<Fifo> fifosToReMap = new ArrayList<>();
	  
	  Map<Integer, Binding<Memory>>  memBindings = bindings.getFifoMemoryBindings();
	  Set<Memory> setBoundMemories = new HashSet<Memory>(); // set of the ids of the bound memories
	  
	  for(Map.Entry<Integer, Binding<Memory>> m : memBindings.entrySet() ) {
		  setBoundMemories.add(m.getValue().getTarget());
	  }
	  
	  for(Memory mem : setBoundMemories) {
		  double capacityMem = mem.getCapacity();
		  double currentStored = 0.0;
		  for(Map.Entry<Integer, Binding<Memory>> m : memBindings.entrySet() ) { 
			  if (m.getValue().getTarget().getId() == mem.getId()) {
				  Fifo fifo = application.getFifos().get( m.getKey() );
				  int bytesFifo = fifo.get_capacity() * fifo.getTokenSize();
				  if(currentStored + bytesFifo <= capacityMem ) {
					  currentStored += bytesFifo;
				  }else {
					  fifosToReMap.add(fifo);
				  }
			  }
			  
			  
		  }
	  }
	  return fifosToReMap;
  }
  
  
  // check if the memory bounds are safe, if not do the re-mapping until the solution is feasible
  public boolean checkAndReMapMemories(Bindings bindings) {
	  ArrayList<Fifo> fifosToReMap = checkMemorySize(bindings);
	  //System.out.println("fifosToReMap"+fifosToReMap);
	  //System.exit(1);
	  if (fifosToReMap.size() == 0)
		  return false;
	  for(Fifo f : fifosToReMap) {
		// ignore the binding and do the remap if a fifo does not fit 
		  Memory reMappingMemory = this.getMemoryToBeRelocated(application.getFifos().get(f.getId()),bindings);
		  //System.out.println("remapping memory "+reMappingMemory.getName());
		  bindings.getFifoMemoryBindings().put(f.getId(), new Binding<Memory>(reMappingMemory));
          //ApplicationManagement.remapFifo(application.getFifos().get(f.getId()), reMappingMemory,bindings);
	  }
	  return true;
	}
  
  
  public Memory getMemoryToBeRelocated(Fifo fifo,Bindings bindings){
	    Memory mappedMemory = bindings.getFifoMemoryBindings().get(fifo.getId()).getTarget();
	    //System.out.println("Mapped Memory "+mappedMemory.getName());
	    Memory newMapping;
	    switch(mappedMemory.getType()){
	      case LOCAL_MEM:
	    	  Tile mappedTile = this.architecture.getTiles().get(mappedMemory.getEmbeddedToProcessor().getOwnerTile().getId());
	    	  newMapping = this.architecture.getTiles().get(mappedTile.getId()).getTileLocalMemory();
	        break;
	      default:
	        newMapping = this.architecture.getGlobalMemory();
	        break;
	    }
	    return newMapping;
  }
  
  
  
  public double getOverallDelay() {
	  double delay = Double.MIN_VALUE;
	  for(Map.Entry<Integer, Tile> t : architecture.getTiles().entrySet()) {
		  for(Map.Entry<Integer, Processor> p :t.getValue().getProcessors().entrySet()) {
			  if (p.getValue().getScheduler().getScheduledActions().size() > 0) {
				  Action a = p.getValue().getScheduler().getScheduledActions().getLast();
				  if (a.getDue_time() > delay)
					  delay = a.getDue_time();
			  }
		  }
	  }
	  return delay;
  }
  
  public void cleanQueueProcessors() {
	  for(Map.Entry<Integer, Tile> t : architecture.getTiles().entrySet()) {
		  for(Map.Entry<Integer, Processor> p : t.getValue().getProcessors().entrySet()) {
			  p.getValue().getScheduler().cleanQueue();
		  }
	  }
  }
  
  public void setFifosForAnalysis() {
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
