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
import multitile.mapping.Bindings;
import multitile.scheduler.UtilizationTable.TimeSlot;
import multitile.architecture.Architecture;
import multitile.architecture.Crossbar;
import multitile.application.CompositeFifo;
import multitile.Transfer;
import multitile.Transfer.TRANSFER_TYPE;
import multitile.application.Actor;
import multitile.application.Actor.ACTOR_TYPE;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;
import multitile.application.MyEntry;
import multitile.application.Application;
import multitile.application.CommunicationTask;
import multitile.application.Fifo;
import java.util.*;

public class HeuristicModuloSchedulerWithCommunications extends BaseScheduler implements Schedule{
  private HashMap<Integer,TimeSlot> timeInfoActors;
  private int MII;
  private int P;
  // key is the core type
  //private HashMap<Integer,Integer> countCoresPerType;
  private ArrayList<String> coreTypes;
  private UtilizationTable U;
  private double scaleFactor;
  private Application applicationWithMessages;
  
	  public HeuristicModuloSchedulerWithCommunications(Architecture architecture, Application application, ArrayList<String> coreTypes, double scaleFactor){
		  super();
		  //this.resourceOcupation = new HashMap<>();
		  //this.indexCoreTypes =indexCoreTypes;
		  //countCoresPerType = new HashMap<>();
		  this.setApplication(application);
		  this.setArchitecture(architecture);
		  this.coreTypes = new ArrayList<>(coreTypes);
/*	    
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
		  System.out.println("countCoresPerType "+countCoresPerType);*/
		  this.scaleFactor = scaleFactor;
	  }
  
	  public void setApplicationWithMessages(Bindings bindings) {
		  this.applicationWithMessages = new Application();
		  HashMap<Integer,Actor> actors = new HashMap<>();
		  HashMap<Integer,Fifo> fifos = new HashMap<>();
		  
		  // map <NameActor,idActor
		  HashMap<String,Integer> mapActors = new HashMap<>();
		  
		  // key: fifo id
		  // value: communicationWriteTask
		  HashMap<Integer,CommunicationTask> mapWriteTasks = new HashMap<>();
		  // key: fifo id
		  // value: communicationReadTask
		  HashMap<Integer,CommunicationTask> mapReadTasks = new HashMap<>();
		  
		  // copy the actors
		  for(Map.Entry<Integer, Actor> a : application.getActors().entrySet()) {
			  // create a new actor
			  Actor actor = new Actor(a.getValue().getName());
			  actor.setType(a.getValue().getType());  // it can be regular actor or multicast
			  int nInputs = a.getValue().getInputs();
			  int nOutputs = a.getValue().getOutputs();
			  actor.setInputs(nInputs);
			  actor.setOutputs(nOutputs);
			  actors.put(actor.getId(), actor);
			  mapActors.put(a.getValue().getName(), actor.getId() );
		  }
		  // generate the communication tasks
		  for(Map.Entry<Integer, Fifo> f : application.getFifos().entrySet()) {
			  String fifoName = f.getValue().getName();
			  // create write communication task
			  Transfer writeTransfer = new Transfer(f.getValue().getSource(),f.getValue());
			  writeTransfer.setType(TRANSFER_TYPE.WRITE);
			  Queue<PassTransferOverArchitecture> writeInterconnects = this.calculatePathOfTransfer(writeTransfer, bindings);
			  CommunicationTask actorW = new CommunicationTask("writeTask::"+fifoName);
			  actorW.setType(ACTOR_TYPE.WRITE_COMMUNICATION_TASK);
			  actorW.setInputs(1);
			  actorW.setFifo(f.getValue());
			  actorW.setUsedInterconnects(writeInterconnects);
			  actorW.setTransfer(writeTransfer);
			  actorW.setRuntimeFromInterconnects(this.scaleFactor);
			  actors.put(actorW.getId(), actorW);
			  mapActors.put(actorW.getName(), actorW.getId());
			  mapWriteTasks.put(f.getKey(), actorW);
			  // create read communication task
			  Transfer readTransfer = new Transfer(f.getValue().getDestination(),f.getValue());
			  readTransfer.setType(TRANSFER_TYPE.READ);
			  Queue<PassTransferOverArchitecture> readInterconnects = this.calculatePathOfTransfer(readTransfer, bindings);
			  CommunicationTask actorR = new CommunicationTask("readTask::"+fifoName);
			  actorR.setType(ACTOR_TYPE.READ_COMMUNICATION_TASK);
			  actorR.setInputs(1);
			  actorR.setFifo(f.getValue());
			  actorR.setUsedInterconnects(readInterconnects);
			  actorR.setTransfer(readTransfer);
			  actorR.setRuntimeFromInterconnects(this.scaleFactor);
			  actors.put(actorR.getId(), actorR);
			  mapActors.put(actorR.getName(), actorR.getId());
			  mapReadTasks.put(f.getKey(), actorR);
			  // create the FIFO connecting the communication tasks
			  Fifo fifo = new Fifo( actorW.getName()+"->"+actorR.getName(),  0,   1, 1, 1 , 1 , actorW , actorR ,FIFO_MAPPING_TYPE.SOURCE);
			  fifos.put(fifo.getId(), fifo);
			  
			  // copy the Fifos
			  int actorSrcId = mapActors.get( f.getValue().getSource().getName());
			  Actor actorSrc = actors.get(actorSrcId);
			  int actorDstId = mapActors.get(f.getValue().getDestination().getName());
			  Actor actorDst = actors.get(actorDstId);
			  
			  //Fifo fifo = new Fifo( t.getId(),  initialTokens,   tokenCapacity, tokenSize, consRate , prodRate , actorSrc , actorDst ,FIFO_MAPPING_TYPE.SOURCE);
			  Fifo fifoInputWrite = new Fifo("fifo::"+actorW.getName(), f.getValue().getInitialTokens(), f.getValue().get_capacity(), f.getValue().getTokenSize(), 1, 1, actorSrc, actorW, FIFO_MAPPING_TYPE.SOURCE);
			  Fifo fifoInputRead  = new Fifo("fifo::"+actorR.getName(), 0, f.getValue().get_capacity(), f.getValue().getTokenSize(), 1, 1, actorR, actorDst, FIFO_MAPPING_TYPE.SOURCE);
			  
			  fifos.put(fifoInputWrite.getId(), fifoInputWrite);
			  fifos.put(fifoInputRead.getId(), fifoInputRead);
		  }
		  
		  applicationWithMessages.setActors(actors);
		  applicationWithMessages.setFifos(fifos);
		  applicationWithMessages.setFifosToActors();
	  }

	  public Application getApplicationWithMessages() {
		  return this.applicationWithMessages;
	  }
	  
	  @Override
	  public void schedule(Bindings bindings) {
		// TODO Auto-generated method stub
		
	  }
	  
	  public void tryToSchedule(Bindings bindings) {
		  // calculate the MII
		  calculateMII(bindings);
		  
		  // set the initial P as MII
		  this.P = this.MII;
		  
		  while(!calculateStartTimes(bindings)) {
			  // we increase the period
			  this.P++;
		  }
		  System.out.println("HEURISTIC WITH COMMS:: ACTUAL PERIOD "+P);
		  System.out.println("HEURISTIC WITH COMMS:: ACTUAL LATENCY "+this.getLantency());
		  U.printUtilizationTable(applicationWithMessages.getActors(), coreTypes);
		  printTimeInfoActors();
	  }
	  
	  public void printTimeInfoActors() {
		  for(Map.Entry<Integer,TimeSlot> t : timeInfoActors.entrySet()) {
			  System.out.println("Actor "+applicationWithMessages.getActors().get(t.getValue().getActorId()).getName()+" STARTS AT "+t.getValue().getStartTime()+" ENDS AT "+t.getValue().getEndTime());
		  }
	  }
  
	  // method to initialize the initial startTimes, endTimes and lengthTimes
	  public boolean calculateStartTimes(Bindings bindings) {
		  timeInfoActors = new HashMap<>();
	
		  HashMap<Integer,Integer> startTime = new HashMap<>();
		  List<Integer> V = new ArrayList<>();
		  for(Map.Entry<Integer,Actor> v : applicationWithMessages.getActors().entrySet()){
			  V.add(v.getKey());
			  startTime.put(v.getKey(), 0);
		  }
		  
		  // [Modulo schedule the loop]
		  // 		a) [Schedule operations in G(V, E) taking only intra-iteration dependences into account]
		  // 		   Let U(i, j) denote the usage of the i-th resource class in control step j
		  //             In this implementation, U(i, j) denote the usage of the i-th tile class in control step j
		  //             i and j are stored in a list which serves as key in a map
		  // key core type - step
		  
		  // key is the resource
		  HashMap<Integer,Integer> countResourcesPerType = new HashMap<>(); // here all the resources are treated as different types, so I use the id of the resources as Key
		  for(Map.Entry<Integer, Actor> a : applicationWithMessages.getActors().entrySet()) {
			  Actor actor = a.getValue();
			  Actor origActor = application.getActor(actor.getName());
			  if (actor.getType() == ACTOR_TYPE.ACTOR || actor.getType() == ACTOR_TYPE.MULTICAST) {
				  // mapped to core
				  System.out.println("actor "+actor.getName());
				  int coreId = bindings.getActorProcessorBindings().get(origActor.getId()).getTarget().getId();
				  countResourcesPerType.put(coreId, 1);
			  }
			  if (actor.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK || actor.getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK) {
				  System.out.println("actor comm "+actor.getName());
				  // cast to communication task
				  CommunicationTask comm = (CommunicationTask)actor;
				  // mapped to interconnect
				  if(comm.getUsedNoc() != null) {
					  countResourcesPerType.put(comm.getUsedNoc().getId(), 1);
				  }
				  if(comm.getUsedLocalMemory() != null) {
					  countResourcesPerType.put(comm.getUsedLocalMemory().getId(), 1);
				  }
				  for(Crossbar c : comm.getUsedCrossbars()) {
					  countResourcesPerType.put(c.getId(), 1);
				  }
				  
			  }
		  }
		  
		  U = new UtilizationTable(countResourcesPerType,P);
		  // compute PCOUNT and SUCC
		  // PCOUNT: is the number of immediate predecessors of v not yet scheduled  
		  // SUCC: is the set of all immediate successors of v
		  // Map<ActorId, Value>
		  //predecessor count
		  HashMap<Integer,Integer> PCOUNT	= new HashMap<>();
		  // succesors
		  HashMap<Integer,Set<Integer>> SUCC 	= new HashMap<>();
		  for(Map.Entry<Integer, Actor> actor : applicationWithMessages.getActors().entrySet()) {
			  PCOUNT.put(actor.getKey(), getPCOUNT(actor.getValue()));
			  SUCC.put(actor.getKey(), getSUCC(actor.getValue()));
		  }
		  
		  while(!V.isEmpty()) {
			  List<Integer> removeV = new ArrayList<>();
			  for (int k = 0 ; k < V.size();k++) {
				  int v = V.get(k);
				  MyEntry<Integer,ArrayList<Integer>> infoBoundResources = this.getBoundResources(bindings, v);
				  ArrayList<Integer> boundResources = infoBoundResources.getValue();
				  int discreteRuntime = infoBoundResources.getKey();
				  /* Check whether data dependences are satisfied */
				  if (PCOUNT.get(v) == 0) {
					  System.out.println("scheduling "+applicationWithMessages.getActors().get(v).getName()+" runtime "+discreteRuntime);
						/* Check that no more than num(r(v)) operations are scheduled on the
	           			resources corresponding to *R(r(v)) at the same time modulo MII */
					  int start = startTime.get(v);
					  //int upperBound = (Math.floorDiv(start,this.P) + 1) * P; 
					  int upperBound = start % this.P;
					  
					  //System.out.println("actor "+application.getActors().get(v).getName()+ " lenght "+discreteRuntime);
					  while(!U.insertIntervalUtilizationTable(v, boundResources, startTime.get(v), startTime.get(v)+discreteRuntime ,discreteRuntime)) {
						  //System.out.println("Trying to insert"+application.getActors().get(v).getName()+" at "+startTime.get(v)+" to "+((startTime.get(v) + discreteRuntime) % this.P ));
						  startTime.put(v, startTime.get(v)+1 );
						  if (upperBound == startTime.get(v) % P ) {
							  // if it not possible to schedule with this P, you have to increase P
							  return false;
							  //System.exit(1);  // here I have to increase the MII
						  }  
					  }
					  
					  //U.printUtilizationTable(applicationWithMessages.getActors(), coreTypes);
					  timeInfoActors.put(v, new TimeSlot(v, startTime.get(v),startTime.get(v) + discreteRuntime ));
					  
					  for (int w : SUCC.get(v)) {
						  PCOUNT.put(w, PCOUNT.get(w) -1 );
						  int maxVal = startTime.get(w) > (startTime.get(v)+ discreteRuntime)  ? startTime.get(w) : (startTime.get(v)+discreteRuntime);
						  startTime.put(w,maxVal);
					  }
					  
					  removeV.add(v);
				  }
			  }
			  V.removeAll(removeV);
		  }
		  return true;
	  }

	  //useful functions for scheduling
	  // PCOUNT: is the number of immediate predecessors of v not yet scheduled  
	  private int getPCOUNT(Actor v) {
		  Set<Integer> predecessors = new HashSet<Integer>();
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
	  private void calculateMII(Bindings bindings) {
		  // key: pair <core type, core id>
		  // Value: count
		  HashMap<MyEntry<Integer,Integer>,Integer> usageCores = new HashMap<>();
			  
		  // key: is the noc id
		  // value: is the count
		  HashMap<Integer,Integer> usageNoC 	 = new HashMap<>();
		  // key: is the crossover id
		  // value: is the count
		  HashMap<Integer,Integer> usageCrossbar = new HashMap<>();
		  // key: is the local memory id
		  // value: is the count
		  HashMap<Integer,Integer> usageLocalMemory = new HashMap<>();
		  // initialize
		  for(Map.Entry<Integer, Actor > actor : applicationWithMessages.getActors().entrySet()) {
			  Actor origActor = application.getActor(actor.getValue().getName());
			  //System.out.println("actor "+actor.getValue().getName()+" id "+actor.getKey());
			  if(actor.getValue().getType() == ACTOR_TYPE.ACTOR || actor.getValue().getType() == ACTOR_TYPE.MULTICAST) {
				  Processor p = bindings.getActorProcessorBindings().get(origActor.getId()).getTarget();
				  int coreTypeIx = coreTypes.indexOf(p.getProcesorType());
				  MyEntry<Integer,Integer> key = new MyEntry<Integer,Integer>(coreTypeIx, p.getId());
				  usageCores.put(key, 0);
			  }
			  if(actor.getValue().getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK || actor.getValue().getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK) {
				  // first init the crossbar
				  CommunicationTask comm = (CommunicationTask)(actor.getValue());
				  for(Crossbar c : comm.getUsedCrossbars()) {
					  usageCrossbar.put(c.getId(), 0);
				  }
				  // init the noc
				  if (comm.getUsedNoc() != null) {
					  usageNoC.put(comm.getUsedNoc().getId(), 0);
				  }
				  if(comm.getUsedLocalMemory() != null) {
					  usageLocalMemory.put(comm.getUsedLocalMemory().getId(), 0);
				  }
			  }
		  }
		  // 1 [Compute resource usage]
		  // Examine the loop body to determine the usage, usage(i), of each resource class R(i) by the loop body
		  // <K,V> here the key is the id of the tile and the value is the usage of cpus in the tile
		  // update the usage
		  int maxExTime = 0;
		  for(Map.Entry<Integer, Actor > actor : applicationWithMessages.getActors().entrySet()) {
			  Actor origActor = application.getActor(actor.getValue().getName());
			  if(actor.getValue().getType() == ACTOR_TYPE.ACTOR || actor.getValue().getType() == ACTOR_TYPE.MULTICAST) {
				  Processor p = bindings.getActorProcessorBindings().get(origActor.getId()).getTarget();
				  int coreTypeIx = coreTypes.indexOf(p.getProcesorType());
				  MyEntry<Integer,Integer> key = new MyEntry<Integer,Integer>(coreTypeIx, p.getId());
				  int val = usageCores.get(key);
				  int discreteRuntime = (int)bindings.getActorProcessorBindings().get(origActor.getId()).getProperties().get("discrete-runtime");
				  maxExTime = (discreteRuntime > maxExTime) ? discreteRuntime : maxExTime;
				  usageCores.put(key, val+discreteRuntime);
			  }
			  if(actor.getValue().getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK || actor.getValue().getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK) {
				  // first init the crossbar
				  CommunicationTask comm = (CommunicationTask)(actor.getValue());
				  for(Crossbar c : comm.getUsedCrossbars()) {
					  int crossbarId = c.getId();
					  int val = usageCrossbar.get(crossbarId);
					  int discreteRuntime = comm.getDiscretizedRuntime();
					  maxExTime = (discreteRuntime > maxExTime) ? discreteRuntime : maxExTime;
					  usageCrossbar.put(crossbarId, val+discreteRuntime);
				  }
				  // init the noc
				  if (comm.getUsedNoc() != null) {
					  int nocId = comm.getUsedNoc().getId();
					  int val = usageNoC.get(nocId);
					  int discreteRuntime = comm.getDiscretizedRuntime();
					  maxExTime = (discreteRuntime > maxExTime) ? discreteRuntime : maxExTime;
					  usageNoC.put(nocId, val+discreteRuntime);
				  }
				  if(comm.getUsedLocalMemory() !=null) {
					  int memoryId = comm.getUsedLocalMemory().getId();
					  usageLocalMemory.put(memoryId, 0); // it is 0 because is communication over the scratchpad memory
				  }
				  // need this to track the usage of the communications in the cores
				  Actor actorInCommunication;
				  if (actor.getValue().getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK) 
					  actorInCommunication = application.getActor( comm.getFifo().getDestination().getName() );
				  else
					  actorInCommunication = application.getActor( comm.getFifo().getSource().getName() );
				  
				  Processor p = bindings.getActorProcessorBindings().get(actorInCommunication.getId()).getTarget();
				  int coreTypeIx = coreTypes.indexOf(p.getProcesorType());
				  MyEntry<Integer,Integer> key = new MyEntry<Integer,Integer>(coreTypeIx, p.getId());
				  int val = usageCores.get(key);
				  int discreteRuntime = comm.getDiscretizedRuntime();
				  maxExTime = (discreteRuntime > maxExTime) ? discreteRuntime : maxExTime;
				  usageCores.put(key, val + discreteRuntime);
				  
			  }
		  } 
		  // System.out.println("USAGE: "+usage);
		  // 	3 [Compute the lower bound of minimum initiation interval]
		  // 		a) [Compute the resource-constrained initiation interval]
		  List<Integer> tmpL = new ArrayList<>();
		  for(HashMap.Entry<MyEntry<Integer,Integer>,Integer> u :usageCores.entrySet()){
			  tmpL.add(u.getValue());
		  }
		  for(HashMap.Entry<Integer,Integer> c : usageCrossbar.entrySet()) {
			  tmpL.add(c.getValue());
		  }
		  for(HashMap.Entry<Integer,Integer> n : usageNoC.entrySet()) {
			  tmpL.add(n.getValue());
		  }
		  for(HashMap.Entry<Integer,Integer> m : usageLocalMemory.entrySet()) {
			  tmpL.add(m.getValue());
		  }
		  this.MII = Collections.max(tmpL);
		  maxExTime = (this.MII > maxExTime) ? this.MII : maxExTime;
		  System.out.println("Heuristic with Communications MII "+MII);
	  }

	
	  public MyEntry<Integer,ArrayList<Integer>> getBoundResources(Bindings bindings, int actorId) {
		  Actor actor = applicationWithMessages.getActors().get(actorId);
		  ArrayList<Integer> boundResources = new ArrayList<>();
		  int discreteRuntime = -1;
		  Actor origActor = application.getActor(actor.getName());
		
		  if (actor.getType() == ACTOR_TYPE.ACTOR || actor.getType() == ACTOR_TYPE.MULTICAST) {
			  // mapped to core
			  int coreId = bindings.getActorProcessorBindings().get(origActor.getId()).getTarget().getId();
			  boundResources.add(coreId);
			  discreteRuntime = (int)bindings.getActorProcessorBindings().get(origActor.getId()).getProperties().get("discrete-runtime");
		  }
		  if (actor.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK || actor.getType() == ACTOR_TYPE.WRITE_COMMUNICATION_TASK) {
			  // cast to communication task
			  CommunicationTask comm = (CommunicationTask)actor;
			  discreteRuntime = comm.getDiscretizedRuntime();
			  // mapped to interconnect
			  if(comm.getUsedNoc() != null) {
				  boundResources.add(comm.getUsedNoc().getId());
			  }
			  for(Crossbar c : comm.getUsedCrossbars()) {
				  boundResources.add(c.getId());
			  }
			  if(comm.getUsedLocalMemory() != null) {
				  boundResources.add(comm.getUsedLocalMemory().getId());
			  }
			  // including in the bound resources list, the core triggering the read of the write
			  Actor operator = null;
			  if (actor.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK) 
				  operator = application.getActors().get( comm.getFifo().getDestination().getId() ) ;
			  else
				  operator = application.getActors().get( comm.getFifo().getSource().getId() ) ;
			  
			  assert operator != null;
			  int coreId = bindings.getActorProcessorBindings().get(operator.getId()).getTarget().getId();
			  boundResources.add(coreId);
			  
			  
		  }
		  MyEntry<Integer,ArrayList<Integer>> result = new MyEntry<Integer,ArrayList<Integer>>(discreteRuntime,boundResources);
		  return result;
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
		  
	  public UtilizationTable getScheduler() {
		  return U;
	  }
	  
	  public HashMap<Integer,TimeSlot> getTimeInfoActors() {
		  return this.timeInfoActors;
	  }
	  
	  public int getPeriod() {
		  return this.P;
	  }
	  
	}
