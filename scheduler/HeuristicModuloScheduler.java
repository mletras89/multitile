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

import multitile.architecture.Processor;
import multitile.architecture.Tile;
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.mapping.Mapping;
import multitile.mapping.Mappings;
import multitile.scheduler.UtilizationTable.TimeSlot;
import multitile.architecture.Architecture;
import multitile.application.CompositeFifo;
import multitile.application.Actor;
import multitile.application.Actor.ACTOR_TYPE;
import multitile.application.Application;
import multitile.application.Fifo;
import java.util.*;

public class HeuristicModuloScheduler extends BaseScheduler implements Schedule{
  private HashMap<Integer,TimeSlot> timeInfoActors;
  
  private int MII;
  private int P;
  
  // key is the core type
  private HashMap<Integer,Integer> countCoresPerType;
  private ArrayList<Integer> actorToCoreTypeMapping;
  //private Map<Integer,ArrayList<Integer>> indexCoreTypes;
  private ArrayList<String> coreTypes;
  private HashMap<Integer,Integer> actorIdToIndex;
  private UtilizationTable U;
  
  public HeuristicModuloScheduler(Architecture architecture, Application application, ArrayList<Integer> actorToCoreTypeMapping,Set<String> coreTypes,HashMap<Integer,Integer> actorIdToIndex){
	  super();
	  //this.resourceOcupation = new HashMap<>();
	  //this.indexCoreTypes =indexCoreTypes;
	  countCoresPerType = new HashMap<>();
	  this.actorToCoreTypeMapping = actorToCoreTypeMapping;
	  this.setApplication(application);
	  this.setArchitecture(architecture);
	  this.coreTypes = new ArrayList<>(coreTypes);
    
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
	  System.out.println("countCoresPerType "+countCoresPerType);
	  this.actorIdToIndex = new HashMap<Integer,Integer>(actorIdToIndex);

  }
  
  public int getPeriod() {
	  return this.P;
  }
  
  public void calculateFifosCapacities() {
	  assert timeInfoActors != null || timeInfoActors.size() == 0 : "First generate the timeInfoActors";
	  int latency = this.getLantency();
	  assert latency != -1 : "First calculate the schedule";
	  
	  
	  int nIterations = Math.floorDiv(latency, this.P);
	  int startKernel = nIterations * this.P;
	  int endKernel = startKernel + this.P;
	  
	  
	  
	  System.out.println("\t\tITERATIONS :"+nIterations);
	  System.out.println("\t\tKernel starts at :"+startKernel);
	  
	  System.out.println("\t\tKernel ends at :"+endKernel);
  }
  
  
  public HashMap<Integer,TimeSlot> getTimeInfoActors() {
	  return this.timeInfoActors;
  }
  
  public int getLantency() {
	  // get the max end time in the timeInfoActors to determine the latency
	  int latency = Integer.MIN_VALUE;
	  if (timeInfoActors == null || timeInfoActors.size() == 0)
		  return -1;  // the schedule has not been calculated
	  for(Map.Entry<Integer, TimeSlot> t : timeInfoActors.entrySet())
		latency = latency <= t.getValue().getEndTime() ? t.getValue().getEndTime() : latency; 
	  
	  return latency;
  }
  
  public void tryToSchedule(Mappings mappings) {
	  // calculate the MII
	  calculateMII(mappings);
	  // set the initial P as MII
	  this.P = this.MII;
	  
	  while(!calculateStartTimes(mappings)) {
		  // we increase the period
		  this.P++;
	  }
	  System.out.println("ACTUAL PERIOD "+P);
	  System.out.println("ACTUAL LATENCY "+this.getLantency());
	  U.printUtilizationTable(application.getActors(), coreTypes);
	  printTimeInfoActors();
  }
  
  public void printTimeInfoActors() {
	  for(Map.Entry<Integer,TimeSlot> t : timeInfoActors.entrySet()) {
		  System.out.println("Actor "+application.getActors().get(t.getValue().getActorId()).getName()+" STARTS AT "+t.getValue().getStartTime()+" ENDS AT "+t.getValue().getEndTime());
	  }
  }
  
  // method to initialize the initial startTimes, endTimes and lengthTimes
  public boolean calculateStartTimes(Mappings mappings) {
	  HashMap<Integer,HashMap<String,Integer>> runtimePerType 	=  mappings.getDiscreteRuntimeFromType();
	  timeInfoActors											= new HashMap<>();

	  HashMap<Integer,Integer> startTime = new HashMap<>();
	  List<Integer> V = new ArrayList<>();
	  for(Map.Entry<Integer,Actor> v : application.getActors().entrySet()){
		  V.add(v.getKey());
		  startTime.put(v.getKey(), 0);
	  }
	  
	  // [Modulo schedule the loop]
	  // 		a) [Schedule operations in G(V, E) taking only intra-iteration dependences into account]
	  // 		   Let U(i, j) denote the usage of the i-th resource class in control step j
	  //             In this implementation, U(i, j) denote the usage of the i-th tile class in control step j
	  //             i and j are stored in a list which serves as key in a map
	  // key core type - step
	  U = new UtilizationTable(countCoresPerType,P);
	  // compute PCOUNT and SUCC
	  // PCOUNT: is the number of immediate predecessors of v not yet scheduled  
	  // SUCC: is the set of all immediate successors of v
	  // Map<ActorId, Value>
	  //predecessor count
	  HashMap<Integer,Integer> PCOUNT	= new HashMap<>();
	  // succesors
	  HashMap<Integer,Set<Integer>> SUCC 	= new HashMap<>();
	  for(Map.Entry<Integer, Actor> actor : application.getActors().entrySet()) {
		  PCOUNT.put(actor.getKey(), getPCOUNT(actor.getValue()));
		  SUCC.put(actor.getKey(), getSUCC(actor.getValue()));
	  }
	  
	  while(!V.isEmpty()) {
		  List<Integer> removeV = new ArrayList<>();
		  for (int k = 0 ; k < V.size();k++) {
			  int v = V.get(k);
			  int vIndex = actorIdToIndex.get(v);
			  int coreTypeBinding = actorToCoreTypeMapping.get(vIndex);
			  int discreteRuntime =  runtimePerType.get(v).get(coreTypes.get(coreTypeBinding));
			  
			  /* Check whether data dependences are satisfied */
			  if (PCOUNT.get(v) == 0) {
					/* Check that no more than num(r(v)) operations are scheduled on the
           			resources corresponding to *R(r(v)) at the same time modulo MII */
				  int start = startTime.get(v);
				  int upperBound = (Math.floorDiv(start,this.P) + 1) * P; 
				  
				  //System.out.println("actor "+application.getActors().get(v).getName()+ " lenght "+discreteRuntime);
				  while(!U.insertIntervalUtilizationTable(v, coreTypeBinding, startTime.get(v), startTime.get(v)+discreteRuntime ,discreteRuntime)) {
					  //System.out.println("Trying to insert"+application.getActors().get(v).getName()+" at "+startTime.get(v)+" to "+((startTime.get(v) + discreteRuntime) % this.P ));
					  startTime.put(v, startTime.get(v)+1 );
					  if (upperBound == startTime.get(v) ) {
						  // if it not possible to schedule with this P, you have to increase P
						  return false;
						  //System.exit(1);  // here I have to increase the MII
					  }  
				  }
				  
				  //U.printUtilizationTable(application.getActors(), coreTypes);
				  timeInfoActors.put(v, new TimeSlot(v, startTime.get(v),startTime.get(v) + discreteRuntime ));
				  
				  for (int w : SUCC.get(v)) {
					  PCOUNT.put(w, PCOUNT.get(w) -1 );
					  //int maxVal = startTime.get(w) > (startTime.get(v)+ discreteRuntime) % this.P   ? startTime.get(w) : (startTime.get(v)+discreteRuntime) % this.P;
					  int maxVal = startTime.get(w) > (startTime.get(v)+ discreteRuntime)  ? startTime.get(w) : (startTime.get(v)+discreteRuntime);
					  startTime.put(w,maxVal);
				  }
				  
				  removeV.add(v);
			  }
		  }
		  V.removeAll(removeV);
	  }
	  /*
	  System.out.println("P="+this.P);
	  //U.printUtilizationTable(application.getActors(), coreTypes,this.P);
	  // update the map of end times
	  for(Map.Entry<Integer, Integer> s : startTime.entrySet()) {
		  endTime.put(s.getKey(), ( s.getValue() + lengthTime.get(s.getKey())) % P );
	  }*/
	  return true;
	 
  }


  @Override
  public void schedule(Bindings bindings) {
	// TODO Auto-generated method stub
	
  }
  
	// useful functions for scheduling
	// PCOUNT: is the number of immediate predecessors of v not yet scheduled  
	private int getPCOUNT(Actor v) {
	  Set<Integer> predecessors = new HashSet<Integer>();
	  
	  if (v.getType() == ACTOR_TYPE.READ_ACTION || v.getType() == ACTOR_TYPE.WRITE_ACTION)
		  return 1;
	  
	  for(Fifo fifo : v.getInputFifos()) {
		  if(!fifo.isFifoRecurrence()) {
			  predecessors.add( fifo.getSource().getId() );
		  }
	  }
	  return predecessors.size();
	}
  
	// 	SUCC: is the set of all immediate successors of v
	//  the set is composed of the ids
	private Set<Integer> getSUCC(Actor v) {
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
	
	// calculate MII
	private void calculateMII(Mappings mappings) {
		  HashMap<Integer,HashMap<String,Integer>> runtimePerType 	=  mappings.getDiscreteRuntimeFromType();
		  // create a copy of the application including tasks for communication channels
		  // number of actors mapped there
		  // key is only the core type
		  HashMap<Integer,Integer> usage = new HashMap<>();
		  
		  // initialize
		  for(Map.Entry<Integer, Actor > actor : application.getActors().entrySet()) {
			  //System.out.println("actor "+actor.getValue().getName()+" id "+actor.getKey());
			  int indexActor = actorIdToIndex.get( actor.getKey() );
			  int coreTypeBinding = actorToCoreTypeMapping.get(indexActor);
			  usage.put(coreTypeBinding, 0);
		  }
		  
		  // 1 [Compute resource usage]
		  // Examine the loop body to determine the usage, usage(i), of each resource class R(i) by the loop body
		  // <K,V> here the key is the id of the tile and the value is the usage of cpus in the tile
		  // update the usage
		  int maxExTime = 0;
		  
		  for(Map.Entry<Integer, Actor > actor : application.getActors().entrySet()) {
			  //ArrayList<Integer> key = new ArrayList<Integer>();
			  int indexActor = actorIdToIndex.get( actor.getKey() );
			  int coreTypeBinding = actorToCoreTypeMapping.get(indexActor);
			  //System.out.println("CORETYPEBINDING "+coreTypeBinding);
			  int val = usage.get(coreTypeBinding);
			  int discreteRuntime =  runtimePerType.get(actor.getKey()).get(coreTypes.get(coreTypeBinding));
			  maxExTime = (discreteRuntime > maxExTime) ? discreteRuntime : maxExTime; 
			  //System.out.println("RUNIMTE "+discreteRuntime);
			  usage.put(coreTypeBinding, val +  discreteRuntime );
		  } 
		  //System.out.println("USAGE: "+usage);
		  // 	3 [Compute the lower bound of minimum initiation interval]
		  // 		a) [Compute the resource-constrained initiation interval]
		  List<Integer> tmpL = new ArrayList<>();

		  for(HashMap.Entry<Integer,Integer> u :usage.entrySet()){
			  // have to modifiy this part for multitile, here I am assuming that all the actors are mapped to the same tile
			  tmpL.add((int)Math.ceil((double)(u.getValue()) / (double)countCoresPerType.get(u.getKey())));
		  }
		  this.MII = Collections.max(tmpL);
		  maxExTime = (this.MII > maxExTime) ? this.MII : maxExTime;
		  System.out.println("MII "+MII);
	}

	public UtilizationTable getScheduler() {
		return U;
	}
	
	
	public void assingActorBinding(Mappings mappings,Bindings bindings) {
		// key: is the core type
		// value:
		// 		key: is the core enumeration
		// 		value: is the occupation list of time slots
		Map<Integer,Map<Integer,LinkedList<TimeSlot>>> uTable = U.getUtilizationTable();
		HashMap<Integer,ArrayList<Processor>> availableCores = this.architecture.getMapTileCoreTypeCoresAsList(coreTypes);
		
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>>  e : uTable.entrySet()) {
			int coreType = e.getKey();
			Map<Integer,LinkedList<TimeSlot>> util = e.getValue();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> u : util.entrySet()) {
				LinkedList<TimeSlot> slots = u.getValue();
				//System.out.println("\tCore # "+u.getKey()+" : ");
				int coreIndex = u.getKey();
				// get the actual core
				Processor p  = availableCores.get(coreType).get(coreIndex);
				for(TimeSlot ts : slots) {
					// do the binding
					int actorId = ts.getActorId();
					// binding the tile
					int tileId = p.getOwnerTile().getId();
					bindings.getActorTileBindings().put(actorId, new Binding<Tile>(architecture.getTiles().get(tileId)));
			        // assign also the properties to the tile mapping!
			        bindings.getActorTileBindings().get(actorId).setProperties( mappings.getActorTileMappings().get(actorId).get(tileId).getProperties() );
					// binding to core
					Binding<Processor> bindingToCore = new Binding<Processor>(new Processor(p));
					// get the mapping
					Mapping<Processor> mappingToCore = mappings.getActorProcessorMappings().get(actorId).get(p.getId());
					bindingToCore.setProperties(mappingToCore.getProperties());
					bindings.getActorProcessorBindings().put(actorId, bindingToCore);
			    }
			}
		}
	}
	

  }
