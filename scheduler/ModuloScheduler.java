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
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.mapping.Mapping;
import multitile.mapping.Mappings;
import multitile.architecture.Architecture;
import multitile.architecture.ArchitectureManagement;
import multitile.architecture.Memory;

import multitile.application.ApplicationManagement;
import multitile.application.CompositeFifo;
import multitile.application.Actor;
import multitile.application.Application;
import multitile.application.Fifo;
import multitile.application.MyEntry;
//import multitile.application.MyEntry;
import multitile.application.Cycles;

import java.util.*;

public class ModuloScheduler extends BaseScheduler implements Schedule{

  // first level key is the step,
  // the second level map, key is the id of the tile, 
  // the third level of the map, the key is the processor and bool the state of the occupation
  //private HashMap<Integer,HashMap<Integer,HashMap<Integer,Boolean>>> resourceOcupation;

  // key is the actor id and the value is the scheduled step
  private HashMap<Integer,Integer> l;
  private int MII;
  //private int lastStep;
  private HashMap<Integer,List<Integer>> kernel;
  private HashMap<Integer,List<Action>> kernelActions;
  private int stepStartKernel = 0;
  private int stepEndKernel = 0;

  // key is the core type
  private HashMap<Integer,Integer> countCoresPerType;
  
  private ArrayList<Integer> actorToCoreTypeMapping;
  //private ArrayList<Integer> nCoresPerTypeMapping;
  
  Map<Integer,ArrayList<Integer>> indexCoreTypes;
  
  private ArrayList<String> coreTypes;
  //private HashMap<Integer,Integer> tileIndexToId;
  
  //key is the <tileid, processor id>, and the val is the next schedulable actors
  private HashMap<MyEntry<Integer,Integer>,Action> nextSchedulableActors;
  
  public ModuloScheduler(Architecture architecture, Application application, Map<Integer,ArrayList<Integer>> indexCoreTypes, ArrayList<Integer> actorToCoreTypeMapping,Set<String> coreTypes){
    super();
    this.l = new HashMap<>();
    //this.resourceOcupation = new HashMap<>();
    this.indexCoreTypes =indexCoreTypes;
    countCoresPerType = new HashMap<>();
    this.actorToCoreTypeMapping = actorToCoreTypeMapping;
    this.setApplication(application);
    this.setArchitecture(architecture);
    //this.nCoresPerTypeMapping = nCoresPerTypeMapping;
    this.coreTypes = new ArrayList<>(coreTypes);
    
    /*for(int i = 0; i < actorToCoreTypeMapping.size(); i++){
    	//int coreType = actorToCoreTypeMapping.get(i);
    	//ArrayList<Integer> key = indexCoreTypes.get(coreType);
    }*/
    for(Map.Entry<Integer, Tile>  t : architecture.getTiles().entrySet()) {
    	for(Map.Entry<Integer, Processor> p : t.getValue().getProcessors().entrySet()) {
    		String coreType = p.getValue().getProcesorType();
    		int index = this.coreTypes.indexOf(coreType);
    		if( countCoresPerType.containsKey(index) ) {
    			countCoresPerType.put(index,  countCoresPerType.get(index) + 1 );
    		}else {
    			countCoresPerType.put(index, 1 );
    		}
    	}
    }
      //this.tileIndexToId = tileIndexToId;
  }

  public HashMap<Integer,List<Integer>> getKernel(){
    return this.kernel;
  }

  public int getStepStartKernel(){
    return this.stepStartKernel;
  }

  public int getStepEndKernel(){
    return this.stepEndKernel;
  }

  public void assingActorFifoMapping(Bindings bindings) {
	  ApplicationManagement.assignActorMapping(application,architecture,this,bindings);
      ApplicationManagement.assignFifoMapping(application,architecture,bindings);
  }
  
  public void assingActorBinding(Mappings mappings,Bindings bindings,HashMap<Integer,Integer> actorIdToIndex) {
	  for(int i = this.stepStartKernel; i < this.stepEndKernel; i++) {
		  // key is core type
		  HashMap<Integer,Queue<Processor>> availableCores = this.architecture.getMapTileCoreTypeCores(coreTypes);
		  /*System.out.println("Kernel step "+kernel.get(i));
		  for(int actorId : kernel.get(i)) {
			  int actorIndex = actorIdToIndex.get(actorId);
			  int coreTypeBinding = actorToCoreTypeMapping.get(actorIndex);
			  
			  //System.out.println("Actor "+application.getActors().get(actorId).getName()+" mapped to TIle "+  architecture.getTiles().get( tileIndexToId.get(actorToTileMapping.get(actorIndex)) ).getName() +" actor index "+actorIndex);
			  //System.out.println("Core type "+coreTypes.get(coreTypeBinding));
		  }*/
		  
		  for(int actorId : kernel.get(i)) {
			  //System.out.println("\tActor "+application.getActors().get(actorId).getName());
			  int actorIndex = actorIdToIndex.get(actorId);
			  int coreTypeBinding = actorToCoreTypeMapping.get(actorIndex);
			  
			  Queue<Processor> queueCores = availableCores.get(coreTypeBinding);
			  Processor selectedCore = queueCores.peek();
			  
			  // do the binding
			  // binding to tile
			  int tileId = selectedCore.getOwnerTile().getId();
			  bindings.getActorTileBindings().put(actorId, new Binding<Tile>(architecture.getTiles().get(tileId)));
	          // assign also the properties to the tile mapping!
	          bindings.getActorTileBindings().get(actorId).setProperties( mappings.getActorTileMappings().get(actorId).get(tileId).getProperties() );
			  
			  // binding to core
			  Binding<Processor> bindingToCore = new Binding<Processor>(new Processor(selectedCore));
			  // get the mapping
			  Mapping<Processor> mappingToCore = mappings.getActorProcessorMappings().get(actorId).get(selectedCore.getId());
			  bindingToCore.setProperties(mappingToCore.getProperties());
			  bindings.getActorProcessorBindings().put(actorId, bindingToCore);
			  
			  queueCores.remove();
			  availableCores.put(coreTypeBinding,queueCores);
			  
			  //System.out.println("Actor "+application.getActors().get(actorId).getName() +" bound to "+ selectedCore.getName() +" on tile "+selectedCore.getOwnerTile().getName());
		  }
	  }
  }
  
  
  
  public void calculateModuloSchedule(HashMap<Integer,Integer> actorIdToIndex, boolean checkRECII){
    // number of actors mapped there
	// key is only the core type
    HashMap<Integer,Integer> usage = new HashMap<>();
    // initialize
    //for(int i=0; i <actorToTileMapping.size();i++) {
    for(Map.Entry<Integer, Actor > actor : application.getActors().entrySet()) {
    	int indexActor = actorIdToIndex.get( actor.getKey() );
    	int coreTypeBinding = actorToCoreTypeMapping.get(indexActor);
    	//ArrayList<Integer> key = this.indexCoreTypes.get(coreTypeBinding);
    	//ArrayList<Integer> key = new ArrayList<Integer>();
    	//int indexActor = actorIdToIndex.get( actor.getKey() );
    	//key.add(tileIndexToId.get(actorToTileMapping.get(indexActor)));
    	//key.add(actorToCoreTypeMapping.get(indexActor));
    	usage.put(coreTypeBinding, 0);
	}
    List<Integer> V = new ArrayList<>();
    for(Map.Entry<Integer,Actor> v : application.getActors().entrySet()){
      V.add(v.getKey());
    }
    // 1 [Compute resource usage]
    // Examine the loop body to determine the usage, usage(i), of each resource class R(i) by the loop body
    // <K,V> here the key is the id of the tile and the value is the usage of cpus in the tile
    // update the usage
    for(Map.Entry<Integer, Actor > actor : application.getActors().entrySet()) {
        //ArrayList<Integer> key = new ArrayList<Integer>();
        int indexActor = actorIdToIndex.get( actor.getKey() );
        int coreTypeBinding = actorToCoreTypeMapping.get(indexActor);
        //ArrayList<Integer> key = this.indexCoreTypes.get(coreTypeBinding);
        //key.add(tileIndexToId.get(actorToTileMapping.get(indexActor)));
    	//key.add(actorToCoreTypeMapping.get(indexActor));
    	int val = usage.get(coreTypeBinding); 
  	    usage.put(coreTypeBinding, val + 1);
    }
    // 2 [Determine recurrencies]
    // 		Enumerate all the recurrences in the dependence graph.
    // 		Let C be the set of all recurrences in the dependence graph. Compute len(c) \forall c \in C
    // 	Key -> the actor X id
    // 	Val -> the legth of the shortest cycle from X -> X
    int RECII = 0;
//    HashMap<Integer,Integer> len = new HashMap<>();
//    HashMap<Integer,Integer> del = new HashMap<>();
    // the cycles must be previously calculated
    Cycles cycles = new Cycles();
    if(checkRECII) {
    	cycles.calculateCycles(application);
    	cycles.calculateRecII();
    	RECII = cycles.getRecII();
    }
    // 	3 [Compute the lower bound of minimum initiation interval]
    // 		a) [Compute the resource-constrained initiation interval]
    List<Integer> tmpL = new ArrayList<>();
    
    //System.out.println("countCoresPerTile "+countCoresPerTile);
    //System.out.println("usage "+usage);
    
    for(HashMap.Entry<Integer,Integer> u :usage.entrySet()){
      // have to modifiy this part for multitile, here I am assuming that all the actors are mapped to the same tile
      tmpL.add((int)Math.ceil((double)(u.getValue()) / (double)countCoresPerType.get(u.getKey())));
    }
    int RESII = Collections.max(tmpL);
    //          b) [Compute the recurrence-constrained initiation interval]
    // 		   I do not have to calcualte this because there are not cycles
    // 		c) [Compute the minimum initiation interval]
    MII = ((RESII >= RECII) ? RESII : RECII);
    double IIprime = Double.NEGATIVE_INFINITY;
    while(true){
      // [Modulo schedule the loop]
      // 		a) [Schedule operations in G(V, E) taking only intra-iteration dependences into account]
      // 		   Let U(i, j) denote the usage of the i-th resource class in control step j
      //             In this implementation, U(i, j) denote the usage of the i-th tile class in control step j
      //             i and j are stored in a list which serves as key in a map
      //Map<ArrayList<Integer>, Integer> U = new HashMap<>();
      
      // key core type - step
      Map<ArrayList<Integer>,Integer> U = new HashMap<>();
      
      //for(HashMap.Entry<Integer,Tile> t: tiles.entrySet()) {
      for(HashMap.Entry<Integer,Integer>  u : usage.entrySet()) {
    	for (int i=0; i<application.getActors().size()*3;i++) {
    		ArrayList<Integer> key = new ArrayList<Integer>();
    		key.add(u.getKey());
    		key.add(i);
    		//p.add(i); // adding the control step
    		U.put(key, 0); 
        }
      }
      // compute PCOUNT and SUCC
      // PCOUNT: is the number of immediate predecessors of v not yet scheduled  
      // SUCC: is the set of all immediate successors of v
      // Map<ActorId, Value>
      l 		                        = new HashMap<>();
      HashMap<Integer,Integer> PCOUNT	= new HashMap<>();
      HashMap<Integer,Set<Integer>> SUCC 	= new HashMap<>();
      for(Map.Entry<Integer, Actor> actor : application.getActors().entrySet()) {
      	//l.put(actor.getKey(), 1);
    	  l.put(actor.getKey(), 0);
      	PCOUNT.put(actor.getKey(), getPCOUNT(actor.getValue()));
      	SUCC.put(actor.getKey(), getSUCC(actor.getValue()));
      }

      while(!V.isEmpty()) {
        List<Integer> removeV = new ArrayList<>();
        for (int k = 0 ; k < V.size();k++) {
        	int v = V.get(k);
        	
        	int vIndex = actorIdToIndex.get(v);
        	int coreTypeBinding = actorToCoreTypeMapping.get(vIndex);
        	
            //ArrayList<Integer> mappingV = this.indexCoreTypes.get(coreTypeBinding);
        	
        	
        	//int mappingToTile = tileIndexToId.get(actorToTileMapping.get(vIndex));
        	//int mappingToCoreType = actorToCoreTypeMapping.get(vIndex);
        	
        	//ArrayList<Integer> mappingV = new ArrayList<>();
        	//mappingV.add(mappingToTile);
        	//mappingV.add(mappingToCoreType);
        	
			/* Check whether data dependences are satisfied */
          if (PCOUNT.get(v) == 0) {
            //System.out.println("TRY Scheduling "+actors.get(v).getName()+" on control step "+l.get(v)+ " on resource "+cpus.get(actors.get(v).getMapping()).getName());
            /* Check that no more than num(r(v)) operations are scheduled on the
               resources corresponding to *R(r(v)) at the same time modulo MII */
            int BU = calcU(l,MII,U,v,coreTypeBinding);
            //int mappingV =  bindings.getActorTileBindings().get(v).getTarget().getId();   //application.getActors().get(v).getMappingToTile().getId();
            //while(BU>=tiles.get(mappingV).getProcessors().size()) {
            while(!(BU<  countCoresPerType.get(coreTypeBinding))) {
               l.put(v, l.get(v)+1);
               BU = calcU(l,MII,U,v,coreTypeBinding);  
            }
            ArrayList<Integer> key = new ArrayList<>();
            //MyEntry<Integer> pair = new MyEntry<>( mappingV, l.get(v)  );
            key.add(coreTypeBinding);
            key.add(l.get(v));
            U.put(key, U.get(key) + 1);
            //System.out.println(U);
            for (int w : SUCC.get(v)) {
              PCOUNT.put(w, PCOUNT.get(w) -1 );
              int maxVal = l.get(w) > l.get(v)+1 ? l.get(w) : l.get(v)+1;
              l.put(w,maxVal);
            }
            removeV.add(v);
          }
        }
        V.removeAll(removeV);
      }
      // calculate the IIprime
      if(checkRECII) {
    	  IIprime = cycles.calculateIIPrime(l);
    	  if(MII < IIprime){
    		  MII++;
    		  // 	fill again V
    		  for(Map.Entry<Integer,Actor> v : application.getActors().entrySet()){
    			  V.add(v.getKey());
    		  }
    	  }
    	  else
    		  break;
      }else
    	  break;
    //System.out.println("L-> "+l);
    }

  }
 
  public void printKernelBody(){
    int scheduled = 0;
    int step = 0;
    while(scheduled < application.getActors().size()){
      System.out.println("Scheduling Step: "+step);
      for(Map.Entry<Integer,Integer> entryL : l.entrySet()){
        if(entryL.getValue() == step){
          System.out.println("Actor: "+application.getActors().get(entryL.getKey()).getName());
          scheduled++;
        }
      }
      step++;
    }
  }

  public void printPipelinedSteps(){
    for(Map.Entry<Integer,List<Integer>> k : kernel.entrySet()  ){
      System.out.println("Step: "+k.getKey());
      for(Integer actorId :k.getValue()){
        System.out.println("\t"+application.getActors().get(actorId).getName());
      }
    }
  }
  
  public void getKernelActions(Bindings bindings){
    kernelActions  = new HashMap<>();
    for(Map.Entry<Integer,List<Integer>> k : kernel.entrySet()  ){
	List<Action> actions = new ArrayList<Action>();
	
	for(Integer a : k.getValue()){
	  Binding<Processor> b = bindings.getActorProcessorBindings().get(a);
	  
      Action action = new Action(application.getActors().get(a));
      action.setStep(k.getKey());
      action.setProcessingTime((double)b.getProperties().get("runtime"));
	  actions.add(action);
	  
	}
	kernelActions.put(k.getKey(),actions);
    }
  }

  public void findSchedule(){
    //List<List<Integer>>	singleIteration     = new ArrayList<>();
    this.kernel  = new HashMap<>();
    
    int latency = Collections.max(new ArrayList<>(l.values()))+1;
    //System.out.println("LATENCY "+latency);
    
    this.stepStartKernel =  ((int)Math.floor((double)latency/(double)this.MII))*MII; 
    this.stepEndKernel = this.stepStartKernel  + this.MII;
    
    // initialize kernel
    for(int i=0; i <= this.stepEndKernel; i++) {
    	kernel.put(i, new ArrayList<Integer>());
    }
    for(Map.Entry<Integer, Actor> a  :  application.getActors().entrySet()) {
    	// key is the actor id and the value is the scheduled step
    	//private HashMap<Integer,Integer> l;
    	int stepAction = l.get(a.getKey());
    	// put step at intervals MII from 0 to end step
    	do{
    		List<Integer> actionsAtStep = kernel.get(stepAction);
    		if (actionsAtStep != null){
    			actionsAtStep.add(a.getKey());
    			kernel.put(stepAction, actionsAtStep);
    		}
    		stepAction += (MII); 
    	}while(stepAction <= this.stepEndKernel);
    }
  }

  public boolean checkValidKernel() {
	  // check that start and end are the same
	  if (kernel.get(this.stepStartKernel).size() != kernel.get(this.stepEndKernel).size())
		  return false;
	  else {
		  for(int i=0; i < kernel.get(this.stepStartKernel).size();i++) {
			  if (kernel.get(this.stepStartKernel).get(i) != kernel.get(this.stepEndKernel).get(i))
				  return false;
		  }
	  }
	// map id, id actor, scheduled 
     HashMap<Integer,Boolean> scheduled = new HashMap<>();
     
     for(Map.Entry<Integer, Actor> a : application.getActors().entrySet()) {
    	 scheduled.put(a.getKey(), false);
     }
     for(int i=this.stepStartKernel; i<this.stepEndKernel;i++) {
    	 List<Integer> list = kernel.get(i);
    	 if(list!=null) {
    		 for(int e : list) {
    			 scheduled.put(e, true);
    		 }
    	 }
     }
     // check
     for(Map.Entry<Integer,Boolean> s : scheduled.entrySet()) {
    	 if (s.getValue() == false)
    		 return false;
     }
     return true;
  }
  
  public void schedule(Bindings bindings,Mappings mappings){
    architecture.resetArchitecture();
    //application.resetApplication(architecture, bindings, application);
    application.resetApplication();
    /*while(!scheduleModulo(bindings,mappings)){
      architecture.resetArchitecture();
      application.resetApplication();
    }*/
  }
  
  public void schedule(Bindings bindings){
    architecture.resetArchitecture();
    application.resetApplication(architecture, bindings, application);
    //while( ! scheduleModulo(bindings)) {
    while( ! scheduleModuloFCFS(bindings)) {
    	architecture.resetArchitecture();
        application.resetApplication(architecture, bindings, application);
    }
  }

  public boolean resizeFifos(HashMap<Integer,Integer> stateChannels) {
	  boolean resize = false; 
	  for(Map.Entry<Integer, Fifo> f : application.getFifos().entrySet()) {
		  // new capacity
		  if (resizeFifo(stateChannels,f.getValue()))
			  resize = true;
	  }
	  return resize;
  }
  
  public boolean resizeFifo(HashMap<Integer,Integer> stateChannels,Fifo fifo) {
	  boolean resize = false;
	  int tokens = stateChannels.get(fifo.getId());
	  if (fifo.get_capacity() < tokens) {
		  application.getFifos().get(fifo.getId()).set_capacity(tokens);
		  resize = true;
	  }
	  return resize;
  }

  public boolean checkandReMapSingleFifo(Bindings bindings,Fifo fifo) {
	  Map<Integer, Binding<Memory>>  memBindings = bindings.getFifoMemoryBindings();
	  Binding<Memory>  bindingFifo = bindings.getFifoMemoryBindings().get(fifo.getId());
	  Memory boundMemory = bindingFifo.getTarget();
	  
	  double capacityMem =boundMemory.getCapacity();
	  double currentFix = 0.0;
	  for(Map.Entry<Integer, Binding<Memory>> m : memBindings.entrySet() ) { 
		  if (boundMemory.getId() == m.getValue().getTarget().getId()) {
			  // check if I can write
			  int bytesFifo = fifo.get_capacity() * fifo.getTokenSize();
			  if(currentFix + bytesFifo <= capacityMem ) {
				  currentFix += bytesFifo;
			  }else {
				  // ignore the binding and do the remap if a fifo does not fit 
				  Memory reMappingMemory = ArchitectureManagement.getMemoryToBeRelocated(fifo,architecture,bindings);
	              ApplicationManagement.remapFifo(fifo, reMappingMemory,bindings);
				  return true;
			  }
		  }
	  }
	  return false;
  }
  
  
  public HashMap<Integer,Integer> getReadsAfterPrologue(HashMap<Integer,Integer> firingActorsPrologue) {
	  HashMap<Integer,Integer> channelsNumberReads = new HashMap<>();
	  for(Map.Entry<Integer, Fifo> c : application.getFifos().entrySet()) {
		  channelsNumberReads.put(c.getKey(), 0);
	  }
	  for(Map.Entry<Integer, Integer> actorEntry : firingActorsPrologue.entrySet() ) {
		  Actor actor = application.getActors().get(actorEntry.getKey());
		  int nFirings = actorEntry.getValue();
		  // update the channels connected as inputs (reads)
		  for(Fifo fifo : actor.getInputFifos()) {
			  int nTokens = fifo.getProdRate();
			  int newCount = channelsNumberReads.get(fifo.getId()) + nTokens * nFirings;
			  channelsNumberReads.put(fifo.getId(), newCount);
		  }
	  }
	  return channelsNumberReads;
  }
  
  public HashMap<Integer,Integer> getWritesAfterPrologue(HashMap<Integer,Integer> firingActorsPrologue) {
	  HashMap<Integer,Integer> channelsNumberWrites = new HashMap<>();
	  for(Map.Entry<Integer, Fifo> c : application.getFifos().entrySet()) {
		  channelsNumberWrites.put(c.getKey(), 0);
	  }
	  for(Map.Entry<Integer, Integer> actorEntry : firingActorsPrologue.entrySet() ) {
		  Actor actor = application.getActors().get(actorEntry.getKey());
		  int nFirings = actorEntry.getValue();
		  // update the channels connected as inputs (writes)
		  for(Fifo fifo : actor.getOutputFifos()) {
			  int nTokens = fifo.getConsRate();
			  int newCount = channelsNumberWrites.get(fifo.getId()) + nTokens * nFirings;
			  channelsNumberWrites.put(fifo.getId(), newCount);
		  }
	  }
	  return channelsNumberWrites;
  }
  
  
  public  HashMap<Integer,Integer> getFiringActorsPrologue(){
	  // count of actors during the
	  // key -> actor id
	  // value -> number of firings in prologue
	  HashMap<Integer,Integer> firingActorsPrologue = new HashMap<>();
	// initialize them
	  for(Map.Entry<Integer, Actor> a : application.getActors().entrySet()) {
		  firingActorsPrologue.put(a.getKey(), 0);
	  }
	  for(int i = 0 ; i < this.stepStartKernel; i++) {
		  for(int actorId :kernel.get(i))
			  firingActorsPrologue.put( actorId , firingActorsPrologue.get(actorId) + 1  );
	  }
	  return firingActorsPrologue;
  }
  
  public HashMap<Integer,Integer> getInitialStateChannels(){
	  // calculate the initial state of the fifos
	  // key -> fifo id
	  // value -> number of tokens
	  HashMap<Integer,Integer> stateChannels = new HashMap<>();
	  for(Map.Entry<Integer, Fifo> c : application.getFifos().entrySet()) {
		  stateChannels.put(c.getKey(), c.getValue().getInitialTokens());
	  }
	  return stateChannels;
  }
    
  public HashMap<Integer,Integer> updateStateChannels(HashMap<Integer,Integer> initialStateChannels,HashMap<Integer,Integer> channelsNumberReads,HashMap<Integer,Integer> channelsNumberWrites) {
	  HashMap<Integer,Integer> stateChannels = new HashMap<>();
	  
	  HashMap<Integer,Integer> actualChannelsNumberReads = new HashMap<>();
	  
	  // update the counts of reads for the MRBs
	  for(Map.Entry<Integer, Fifo> c : application.getFifos().entrySet()) {
		 if (c.getValue().isCompositeChannel()) {
			 CompositeFifo MRB = (CompositeFifo)c.getValue();
			 int nReaders = MRB.getDestinations().size();
			 int currentCountReads = channelsNumberReads.get(MRB.getId()) % nReaders;
			 actualChannelsNumberReads.put(MRB.getId(),currentCountReads );
		 }else {
			 int currentCountReads = channelsNumberReads.get(c.getKey());
			 actualChannelsNumberReads.put(c.getKey(),currentCountReads );
		 }
		 
	  }
	  for(Map.Entry<Integer, Fifo> fifo : application.getFifos().entrySet()) {
		  int initState = initialStateChannels.get(fifo.getKey());
		  stateChannels.put(fifo.getKey(), initState + channelsNumberWrites.get(fifo.getKey()) - actualChannelsNumberReads.get(fifo.getKey())  );
	  }
	  // -1 is wrong
	  /*for(HashMap.Entry<Integer,Integer> e : stateChannels.entrySet()) {
		  assert (e.getValue() >= 0);
	  }*/
	  
	  // Once I get the prologue
	  return stateChannels;
  }
  
  boolean scheduleModuloFCFS(Bindings bindings) {
		//HashMap<Integer,Tile> tiles = architecture.getTiles();
	    this.getScheduledStepActions().clear();
	    this.getKernelActions(bindings);
	    //HashMap<Integer,Integer> firingActorsPrologue = getFiringActorsPrologue();
	    HashMap<Integer,Integer> initialState = getInitialStateChannels();
	    HashMap<Integer,Integer> writes = new HashMap<>();
	    HashMap<Integer,Integer> reads = new HashMap<>();
	    // initialize read and writes as empty
	    for(Map.Entry<Integer, Fifo> f : application.getFifos().entrySet()) {
	    	reads.put(f.getKey(), 0);
			writes.put(f.getKey(), 0);
		}
	    
	    HashMap<Integer,Integer> stateChannels = new HashMap<>();
	    
	    for(int i = 0 ; i < this.stepStartKernel; i++) {
		  for(int actorId :kernel.get(i)) {
			  // update state of channels connected at it
			  // first set the reads
			  Actor actor = application.getActors().get(actorId);
			  for(Fifo f : actor.getInputFifos()) {
				  reads.put(f.getId(), reads.get(f.getId()) + f.getConsRate() );
			  }
			  // then set the writes
			  for(Fifo f : actor.getOutputFifos()) {
				  writes.put(f.getId(), writes.get(f.getId()) + f.getProdRate() );
			  }
			  // calculate state
			  stateChannels = updateStateChannels(initialState, reads, writes);
			  // resize fifos
			  resizeFifos(stateChannels);
		  }
		  
	    }
	    // check memory constraint binding
	    checkAndReMapMemories(bindings);
	    
	    // insert tokens at FIFOs after prologue
	    application.fillTokensAtState(stateChannels,reads);
	    //application.printFifosState();
	    // proceed to schedule the kernel
	    Map<Actor,List<Transfer>> processorReadTransfers = new HashMap<>();
    	Map<Actor,List<Transfer>> processorWriteTransfers = new HashMap<>();
    	this.nextSchedulableActors = new HashMap<>();
	    for(int j =this.stepStartKernel; j<this.stepEndKernel; j++){  // this.stepEndKernel;j++){
	    	this.getNextSchedulableActors(bindings, j);
	    	this.cleanQueueProcessors();
	    	assert this.nextSchedulableActors.size() > 0 : "THIS SHOULD NO HAPPEN!!!";
	    	// assign the actions to the processor
	        for(Map.Entry<MyEntry<Integer,Integer>, Action>  n : nextSchedulableActors.entrySet() ) {
	      	  // get the processor
	      	  Tile t = architecture.getTiles().get(n.getKey().getKey());
	      	  Processor p = t.getProcessors().get(n.getKey().getValue());
	      	  //double processingTime = (double)bindings.getActorProcessorBindings().get(n.getValue().getActor().getId()).getProperties().get("runtime");
	      	  Action a = new Action(n.getValue());
	      	  p.getScheduler().insertAction(a);
	        }
	        //schedule all the reads
	        for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
	      	  for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
	      		  Processor p = proc.getValue();
	      		  if (p.getScheduler().getQueueActions().size()==0)
	      			  continue;
	      		  for(Action action : p.getScheduler().getQueueActions()) {
	      			  // sched reads
	      			  p.getScheduler().commitReads(action,application.getFifos(),application);
		          	  List<Transfer> readTransfers = p.getScheduler().getReadTransfers().get(action.getActor());
		          	  for(Transfer t: readTransfers) {
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
		          	  }
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
	        // schedule actions
	        for(Map.Entry<Integer, Tile> tile : architecture.getTiles().entrySet()) {
	          for(Map.Entry<Integer, Processor> proc : tile.getValue().getProcessors().entrySet()) {
	        	  Processor p = proc.getValue();
		      	  if (p.getScheduler().getQueueActions().size()==0)
		      		  continue;
		      	  for(Action action : p.getScheduler().getQueueActions()) {
		      		  p.getScheduler().commitSingleAction(action,architecture,application, bindings, j );  
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
		              for(Transfer t : writeTransfers) {
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
		               	}
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
	    
     return true;
  }
  
  
  boolean scheduleModulo(Bindings bindings){
	//HashMap<Integer,Tile> tiles = architecture.getTiles();
	    this.getScheduledStepActions().clear();
	    this.getKernelActions(bindings);
	    //HashMap<Integer,Integer> firingActorsPrologue = getFiringActorsPrologue();
	    HashMap<Integer,Integer> initialState = getInitialStateChannels();
	    HashMap<Integer,Integer> writes = new HashMap<>();
	    HashMap<Integer,Integer> reads = new HashMap<>();
	    // initialize read and writes as empty
	    for(Map.Entry<Integer, Fifo> f : application.getFifos().entrySet()) {
	    	reads.put(f.getKey(), 0);
			writes.put(f.getKey(), 0);
		}
	    
	    HashMap<Integer,Integer> stateChannels = new HashMap<>();
	    
	    for(int i = 0 ; i < this.stepStartKernel; i++) {
		  for(int actorId :kernel.get(i)) {
			  // update state of channels connected at it
			  // first set the reads
			  Actor actor = application.getActors().get(actorId);
			  for(Fifo f : actor.getInputFifos()) {
				  reads.put(f.getId(), reads.get(f.getId()) + f.getConsRate() );
			  }
			  // then set the writes
			  for(Fifo f : actor.getOutputFifos()) {
				  writes.put(f.getId(), writes.get(f.getId()) + f.getProdRate() );
			  }
			  // calculate state
			  stateChannels = updateStateChannels(initialState, reads, writes);
			  // resize fifos
			  resizeFifos(stateChannels);
		  }
		  
	    }
	    // check memory constraint binding
	    checkAndReMapMemories(bindings);
	    
	    // insert tokens at FIFOs after prologue
	    application.fillTokensAtState(stateChannels,reads);
	    //application.printFifosState();
	    // proceed to schedule the kernel
	    for(int j =this.stepStartKernel; j<this.stepEndKernel; j++){  // this.stepEndKernel;j++){
	    		//List<Action> actorsInStep = kernelActions.get(j);
	    		//System.out.println("Working on step "+j);
	    		//while(actorsInStep.size()>0) {
	        	//System.out.println("actors in step: "+actorsInStep.size());
	        	this.getSchedulableActors(j);
	        	Map<Actor,List<Transfer>> processorReadTransfers = new HashMap<>();
	        	Map<Actor,List<Transfer>> processorWriteTransfers = new HashMap<>();
	        	for(Action action : queueActions){
	        		//System.out.println("Firing "+action.getActor().getName());
	        		int processorID = bindings.getActorProcessorBindings().get(action.getActor().getId()).getTarget().getId();
	          	  	int tileId = bindings.getActorTileBindings().get(action.getActor().getId()).getTarget().getId();
	          	  	Processor p = architecture.getTiles().get(tileId).getProcessors().get(processorID);
	          	  	// sched reads
	          	  	p.getScheduler().commitReads(action,application.getFifos(),application);
	          	  	List<Transfer> readTransfers = p.getScheduler().getReadTransfers().get(action.getActor());
	          	  	//if (readTransfers != null)
	        		  //continue;
	          	  	// I have to update the state of the channels according to the reads
	          	  	for(Transfer t: readTransfers) {
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
	          	  	}
	          	  	
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
	          	  	
	          	  	// sched the action
	          	  	p.getScheduler().commitSingleAction(action,architecture,application, bindings, j );
	          	  	
	          	  	// sched the writes
	                p.getScheduler().commitWrites(action,application);
	                // put writing transfers to crossbar(s) or NoC
	                // get write transfers from the scheduler
	                List<Transfer> writeTransfers = p.getScheduler().getWriteTransfers().get(action.getActor());
	                //if (writeTransfers != null)
	               	 //continue;
	                for(Transfer t : writeTransfers) {
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
	               	}
	                
	                processorWriteTransfers.put(action.getActor(), scheduleTransfers(writeTransfers,bindings));
	            	// update the write transfers of each processor with the correct start and due time
	                p.getScheduler().setWriteTransfers(processorWriteTransfers);
	             	// update the last event in processor, taking into the account the processorWriteTransfers
	                p.getScheduler().updateLastEventAfterWrite(action);
	                // insert the time of the produced tokens by action into the correspondent fifos
	                p.getScheduler().produceTokensinFifo(action,application.getFifos());
	            }
	    }
	    
	    return true;
  }
  
  void removeActionFromListOfActions(List<Action> actorsInStep,Action action) {
	  int selectedAction = -1;
	  for(int i = 0 ; i < actorsInStep.size();i++) {
		  if( actorsInStep.get(i).getActor().getId() == action.getActor().getId() ) {
			  selectedAction = i;
			  break;
		  }
	  }
	  if (selectedAction != -1) {
		  actorsInStep.remove(selectedAction);
	  }
  }
  
  
  public double getDelaySingleIteration(){
    // we take into account when k=3 and K=4
    int start  = stepStartKernel; //this.MII*3+1;
    int end = stepEndKernel; //this.MII*4+1;

    double startTime = Double.POSITIVE_INFINITY;
    double endTime  = -1;
    //System.out.println("Start iteration "+start);
    for(Action a : this.getScheduledStepActions().get(start)){
      //System.out.println("ACTION:"+a.getActor().getName());
      if(a.getStart_time() < startTime)
        startTime = a.getStart_time();
    }
    //System.out.println("MMI: "+MII+" start iteration "+start+" End iteration "+end);
    //System.out.println("this.getScheduledStepActions() "+this.getScheduledStepActions());
    //System.out.println("iterations "+this.getMaxIterations());
    for(int i=0; i < 5; i++)
    	if (this.getScheduledStepActions().get(end) == null) 
    		end = end + MII;
    	else
    		break;
    
    for(Action a : this.getScheduledStepActions().get(end)){
      if(a.getDue_time() > endTime)
        endTime = a.getDue_time();
    }
    return endTime - startTime;
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

  public int getNextAvailableProcessor(HashMap<Integer,Boolean> processorUtilization){
    // key is the id of the processor and the boolean is the availability of the processor
    for(HashMap.Entry<Integer,Boolean> e : processorUtilization.entrySet()){
      if (e.getValue() == false)
        return e.getKey();
    }
    return -1;    // only return this, if there are no available processors; probably implemente remapping
  }

  public void getSchedulableActors(Map<Integer,Actor> actors,Map<Integer,Fifo> fifos, 
                                   int step,
                                   // step, actor_ids
                                   HashMap<Integer, List<Integer>> kernel){
                                   
    // from the list of actors in Processor, check which of them can fire
    this.cleanQueue();
    List<Integer> actorsInStep = kernel.get(step);
    if (actorsInStep!=null) {
    	for(int v : actorsInStep){
    		//if (actors.get(v).canFire(fifos)){
    			Action action = new Action(actors.get(v));
    			this.insertAction(action);
    		//}
    	}
    }
  }
  
  public void getNextSchedulableActors(Bindings bindings, int step){
	    nextSchedulableActors.clear();
	    
	    // key, id and value number of current firings
	    HashMap<Integer,Integer> mapOcurrences = new HashMap<>();
	    ArrayList<Integer> potentialSched = new ArrayList<>();
	    
	    List<Action> actorsInStep = kernelActions.get(step);
	    
	    if (actorsInStep!=null) {
	    	for(Action v : actorsInStep){
	    		Processor core = bindings.getActorProcessorBindings().get(v.getActor().getId()).getTarget();
	    		nextSchedulableActors.put(new MyEntry<Integer,Integer>(core.getOwnerTile().getId(),core.getId()), v);
	    	}
	    }
  }


  public void getSchedulableActors(int step){
                                   
    // from the list of actors in Processor, check which of them can fire
    this.cleanQueue();
    List<Action> actorsInStep = kernelActions.get(step);
    if (actorsInStep!=null) {
    	for(Action v : actorsInStep){
    	//	if (application.getActors().get(v.getActor().getId()).canFire(application.getFifos())){
    			Action action = new Action(v);
    			this.insertAction(action);
    		//}
    	}
    }
  }
  
  public void getSchedulableActors(List<Action> actorsInStep){
  // from the list of actors in Processor, check which of them can fire
  this.cleanQueue();
  //List<Action> actorsInStep = kernelActions.get(step);
  	if (actorsInStep!=null) {
	  for(Action v : actorsInStep){
		  if (application.getActors().get(v.getActor().getId()).canFire(application.getFifos())){
			  Action action = new Action(v);
			  this.insertAction(action);
		  }
	  }
  	}
  }
  
  
  // PCOUNT: is the number of immediate predecessors of v not yet scheduled  
  int getPCOUNT(Actor v) {
    int pCount=0;
    for(Fifo fifo : v.getInputFifos()) {
      if(!fifo.isFifoRecurrence())
        pCount++;
    }
    return pCount;
  }
    
  // SUCC: is the set of all immediate successors of v
  //  the set is composed of the ids
  Set<Integer> getSUCC(Actor v) {
    Set<Integer> SUCC = new HashSet<Integer>();
		
    for(Fifo fifo: v.getOutputFifos()) {
      //System.out.println("Fifo "+fifo.getName()+" is composite "+fifo.isCompositeChannel());
      if(fifo.isCompositeChannel()) {
    	  CompositeFifo cf = (CompositeFifo) fifo;
          if (!cf.isFifoRecurrence()){
    	    for(Actor a : cf.getDestinations()) {
    		  Integer targetActor = a.getId();
    		  SUCC.add(targetActor);
    	    }
          }
      }else {
    	  Integer targetActor = fifo.getDestination().getId();
          if (!fifo.isFifoRecurrence())
    	    SUCC.add(targetActor);
      }
    }
    return SUCC;
  }

  int calcU(HashMap<Integer,Integer> l,int MII, Map<ArrayList< Integer >,Integer> U,int v, int coreTypeMapping) { // int mappingToTile, int mappingToCoreType) {
    int BU=0;
    /*System.out.println("Here");
    for(Map.Entry<ArrayList<Integer>, Integer> u : U.entrySet()) {
            System.out.println("key :["+u.getKey().get(0)+","+u.getKey().get(1)+"] val: "+u.getValue());
    }
    System.out.println("==================================");*/
    int maxL = 	Collections.max(new ArrayList<>(l.values()));
    //int mapping = bindings.getActorTileBindings().get(v).getTarget().getId();
    //MyEntry<Integer,String> e1 = new MyEntry<Integer,String>( mappingToTile, mappingToCoreType );
    
    for (int i=0;i<=Math.floor(l.get(v)/MII);i++) {
            ArrayList<Integer> key = new ArrayList<>();
            key.add(coreTypeMapping);
            key.add(l.get(v)-i*MII);
            
            //pair.add(mappingToCoreType);
            //pair.add(l.get(v)-i*MII);
            //MyEntry< MyEntry<Integer,String>,Integer> e2 = new MyEntry<>( e1, l.get(v)-i*MII );
            
            BU += U.get(key);
    }
    int i = l.get(v) + MII;
    while(i<=maxL) {
    	 ArrayList<Integer> key = new ArrayList<>();
    	 key.add(coreTypeMapping);
    	 key.add(i);
    	 
    	//ArrayList<Integer> pair = new ArrayList<>();
        //pair.add(mappingToTile);
        //pair.add(mappingToCoreType);
        //pair.add(i);
        //MyEntry< MyEntry<Integer,String>,Integer> e2 = new MyEntry<>( e1, i);
        BU += U.get(key);
    	i=i+MII;
    }
    
    return BU;
  }
  
  public int getNumberOfIterationsFromSchedule() {
	  int iterations = 0;
	  ArrayList<Integer> key = new ArrayList<>( application.getActors().keySet());
	  Actor actor = application.getActors().get(key.get(0));
	  for(Map.Entry<Integer, Tile> t : architecture.getTiles().entrySet()) {
		  for(Map.Entry<Integer, Processor> p :t.getValue().getProcessors().entrySet()) {
			  for(Action a : p.getValue().getScheduler().getScheduledActions()) {
				  if (a.getActor().getId() == actor.getId())
					  iterations++;
			  }
		  }
	  }
	  return iterations;
  }

  public int getMII(){
    return this.MII;
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
