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


// this modulo scheduler heuristic glues the communications together with the actor execution

import multitile.architecture.Processor;
import multitile.architecture.Tile;
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

public class HeuristicModuloSchedulerConstrained extends BaseScheduler implements Schedule{
  private HashMap<Integer,TimeSlot> timeInfoActors;
  private int MII;
  private int P;
  // key is the core type
  private HashMap<Integer,Integer> countCoresPerType;
  private ArrayList<String> coreTypes;
  private UtilizationTable U;
  private double scaleFactor;
  
  // set of reads of each actor
  // key : id of actor
  // value : list of read tasks
  
  private HashMap<Integer, ArrayList<CommunicationTask>> actorReads;
  private HashMap<Integer, ArrayList<CommunicationTask>> actorWrites;
  private HashMap<Integer, CommunicationTask> setCommunicationTasks;
  
	  public HeuristicModuloSchedulerConstrained(Architecture architecture, Application application, ArrayList<String> coreTypes, double scaleFactor){
		  super();
		  //this.resourceOcupation = new HashMap<>();
		  //this.indexCoreTypes =indexCoreTypes;
		  this.countCoresPerType = new HashMap<>();
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
		  /*System.out.println("countCoresPerType "+countCoresPerType);*/
		  this.scaleFactor = scaleFactor;
	  }
  
	  public void setApplicationWithMessages() {
		  setCommunicationTasks = new HashMap<>(); 
		  actorReads = new HashMap<>();
		  actorWrites = new HashMap<>();
		  
		  // copy the actors
		  for(Map.Entry<Integer, Actor> a : application.getActors().entrySet()) {
			  // create a new actor
			  actorReads.put(a.getKey(), new ArrayList<>());
			  actorWrites.put(a.getKey(), new ArrayList<>());
		  }
		  // generate the communication tasks
		  for(Map.Entry<Integer, Fifo> f : application.getFifos().entrySet()) {
			  String fifoName = f.getValue().getName();
			  // create write communication task
			  Transfer writeTransfer = new Transfer(f.getValue().getSource(),f.getValue());
			  writeTransfer.setType(TRANSFER_TYPE.WRITE);
			  //Queue<PassTransferOverArchitecture> writeInterconnects = this.calculatePathOfTransfer(writeTransfer, bindings);
			  CommunicationTask actorW = new CommunicationTask("writeTask::"+fifoName);
			  actorW.setType(ACTOR_TYPE.WRITE_COMMUNICATION_TASK);
			  actorW.setInputs(1);
			  actorW.setFifo(f.getValue());
			  //actorW.setUsedInterconnects(writeInterconnects);
			  actorW.setTransfer(writeTransfer);
			  
			  actorW.setPriority( application.getActors().get(f.getValue().getSource().getId()).getPriority());
			  ArrayList<CommunicationTask> list = actorWrites.get(f.getValue().getSource().getId());
			  list.add(actorW);
			  setCommunicationTasks.put(actorW.getId(),actorW);
			  actorWrites.put(f.getValue().getSource().getId(), list);
			  
			  if (!f.getValue().isCompositeChannel()) {
				  Transfer readTransfer = new Transfer(f.getValue().getDestination(),f.getValue());
				  readTransfer.setType(TRANSFER_TYPE.READ);
				  //Queue<PassTransferOverArchitecture> readInterconnects = this.calculatePathOfTransfer(readTransfer, bindings);
				  CommunicationTask actorR = new CommunicationTask("readTask::"+fifoName);
				  actorR.setType(ACTOR_TYPE.READ_COMMUNICATION_TASK);
				  actorR.setInputs(1);
				  actorR.setFifo(f.getValue());
				  //actorR.setUsedInterconnects(readInterconnects);
				  actorR.setTransfer(readTransfer);
				  //actorR.setRuntimeFromInterconnects(this.scaleFactor);
				  actorR.setPriority( application.getActors().get(f.getValue().getDestination().getId()).getPriority());
				  
				  ArrayList<CommunicationTask> listReads = actorReads.get(f.getValue().getDestination().getId());
				  listReads.add(actorR);
				  setCommunicationTasks.put(actorR.getId(),actorR);
				  actorReads.put(f.getValue().getDestination().getId(), listReads);
			  }
			  else {
				  CompositeFifo mrb = (CompositeFifo)f.getValue();
				  int counter = 0;
				  for(Map.Entry<Integer, Fifo> fs : mrb.getReaders().entrySet()) {
					  
					  Transfer readTransfer = new Transfer(fs.getValue().getDestination(),f.getValue());
					  readTransfer.setType(TRANSFER_TYPE.READ);
					  //Queue<PassTransferOverArchitecture> readInterconnects = this.calculatePathOfTransfer(readTransfer, bindings);
					  CommunicationTask actorR = new CommunicationTask("readTask::"+fifoName+"::"+counter++);
					  actorR.setType(ACTOR_TYPE.READ_COMMUNICATION_TASK);
					  actorR.setInputs(1);
					  actorR.setFifo(f.getValue());
					  //actorR.setUsedInterconnects(readInterconnects);
					  actorR.setTransfer(readTransfer);
					  //actorR.setRuntimeFromInterconnects(this.scaleFactor);
					  actorR.setPriority( application.getActors().get(fs.getValue().getDestination().getId()).getPriority());
					  actorR.setFifoFromMRB(fs.getValue());
					  
					  ArrayList<CommunicationTask> listReads = actorReads.get(fs.getValue().getDestination().getId());
					  list.add(actorR);
					  setCommunicationTasks.put(actorR.getId(),actorR);
					  actorReads.put(fs.getValue().getSource().getId(), listReads);
				  }
			  }
			  
		  }
	  }

	  public void setInterconnects(Bindings bindings) {
		  // generate the communication tasks
		  for(Map.Entry<Integer, Actor> a : application.getActors().entrySet()) {
			  if(a.getValue().getType() == ACTOR_TYPE.ACTOR || a.getValue().getType() == ACTOR_TYPE.MULTICAST) {
				  for(CommunicationTask c : actorWrites.get(a.getKey())) {
					  Transfer transfer = c.getTransfer();
					  Queue<PassTransferOverArchitecture> setInterconnects = this.calculatePathOfTransfer(transfer, bindings);
					  c.setUsedInterconnects(setInterconnects);
					  c.setRuntimeFromInterconnects(this.scaleFactor);
					  setCommunicationTasks.put(c.getId(), c);
				  }
				  for(CommunicationTask c : actorReads.get(a.getKey())) {
					  Transfer transfer = c.getTransfer();
					  Queue<PassTransferOverArchitecture> setInterconnects = this.calculatePathOfTransfer(transfer, bindings);
					  c.setUsedInterconnects(setInterconnects);
					  c.setRuntimeFromInterconnects(this.scaleFactor);
					  setCommunicationTasks.put(c.getId(), c);
				  }
			  }
		  
		  }
	  }
	  
	  @Override
	  public void schedule(Bindings bindings) {
		// TODO Auto-generated method stub
		
	  }
	  
	  public void tryToSchedule(Bindings bindings,String foldername) {
		  // calculate the MII
		  calculateMII(bindings);
		  //calculateMIISecond(bindings);
		  // set the initial P as MII
		  this.P = this.MII;
		  //System.out.println("MII "+this.P);
		  /**
		   * Instead of increasing one by one, we perform
		   * a binary search to do less evaluations to find P
		   * */ 
		  int _lowerBound = this.MII;
		  int _upperBound = _lowerBound + this.MII;
		  if(!calculateStartTimes(bindings)) {
			  while(true) {
				  boolean state  = false;
				  int lowerBound = _lowerBound;
				  int upperBound = _upperBound;
				  
				  this.P = upperBound;
				  if(!calculateStartTimes(bindings)) {
					  _lowerBound = _upperBound;
					  _upperBound += this.MII;
					  continue;
				  }
				  while(true) {
					  this.P = lowerBound + (upperBound-lowerBound)/2;
					  state = calculateStartTimes(bindings);
					  
					  //System.out.println("P "+this.P+" lower "+lowerBound+" upper "+upperBound+" state "+state);
					  if (lowerBound == upperBound)
						  break;
					  
					  if (state) {
						  upperBound = this.P;
					  }
					  else {
						  lowerBound = this.P+1;
					  }
				  }
			  
				  if (state) 
					  break;
				  _lowerBound = _upperBound;
				  _upperBound += this.MII;
			  }
		  }
		  /*while(!calculateStartTimes(bindings)) {
			  // we increase the period
			  this.P++;
		  }*/
		  //System.out.println("HEURISTIC WITH COMMS:: ACTUAL PERIOD "+P);
		  //System.out.println("HEURISTIC WITH COMMS:: ACTUAL LATENCY "+this.getLantency());
		  //U.printUtilizationTable(applicationWithMessages.getActors(), coreTypes);
		  //printTimeInfoActors();
		  //System.out.println("DONE "+this.P);
	  }
	  
	  public void printTimeInfoActors() {
		  for(Map.Entry<Integer,TimeSlot> t : timeInfoActors.entrySet()) {
			  if (application.getActors().containsKey(t.getValue().getActorId()))
				  System.out.println("Actor "+application.getActors().get(t.getValue().getActorId()).getName()+" STARTS AT "+t.getValue().getStartTime()+" ENDS AT "+t.getValue().getEndTime());
			  else if(setCommunicationTasks.containsKey(t.getValue().getActorId())) {
				  System.out.println("Actor "+setCommunicationTasks.get(t.getValue().getActorId()).getName()+" STARTS AT "+t.getValue().getStartTime()+" ENDS AT "+t.getValue().getEndTime());
			  }else {
				  assert false: "This should never happen!";
			  }
		  }
	  }
  
	  // method to initialize the initial startTimes, endTimes and lengthTimes
	  public boolean calculateStartTimes(Bindings bindings) {
		  timeInfoActors = new HashMap<>();
	
		  HashMap<Integer,Integer> startTime = new HashMap<>();
		  List<Integer> V = new ArrayList<>();
		  
		  ArrayList<Actor> actorsToOrder = new ArrayList<>(application.getActors().values());
		  actorsToOrder.sort((o1,o2) ->  o1.getPriority() - o2.getPriority());
		  for(Actor v : actorsToOrder){
			  V.add(v.getId());
			  startTime.put(v.getId(), 0);
		  }
		  
		  // [Modulo schedule the loop]
		  // 		a) [Schedule operations in G(V, E) taking only intra-iteration dependences into account]
		  // 		   Let U(i, j) denote the usage of the i-th resource class in control step j
		  //             In this implementation, U(i, j) denote the usage of the i-th tile class in control step j
		  //             i and j are stored in a list which serves as key in a map
		  // key core type - step
		  
		  // key is the resource
		  HashMap<Integer,Integer> countResourcesPerType = new HashMap<>(); // here all the resources are treated as different types, so I use the id of the resources as Key
		  for(Map.Entry<Integer, Actor> a : application.getActors().entrySet()) {
			  Actor actor = a.getValue();
			  Actor origActor = application.getActor(actor.getName());
			  if (actor.getType() == ACTOR_TYPE.ACTOR || actor.getType() == ACTOR_TYPE.MULTICAST) {
				  // mapped to core
				  //System.out.println("actor "+actor.getName());
				  int coreId = bindings.getActorProcessorBindings().get(origActor.getId()).getTarget().getId();
				  countResourcesPerType.put(coreId, 1);
			  }
		  }  
		  for(Map.Entry<Integer, CommunicationTask> cs : setCommunicationTasks.entrySet()) {
			  CommunicationTask comm = cs.getValue();
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
			  		  
		  U = new UtilizationTable(countResourcesPerType,this.P);
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
				  
				  /* Check whether data dependences are satisfied */
				  if (PCOUNT.get(v) == 0) {
					  HashMap<CommunicationTask,Integer> startTimes = new HashMap<>();
					  MyEntry<Integer,ArrayList<Integer>> infoBoundResources = this.getBoundResources(bindings, v);
					  ArrayList<Integer> boundResources = infoBoundResources.getValue();
					  int discreteRuntime = infoBoundResources.getKey();
					  
					  int wholeExecTime = discreteRuntime;
					  for(CommunicationTask  c: actorReads.get(v)) {
						  MyEntry<Integer,ArrayList<Integer>> infoBoundResourcesCTask = this.getBoundResources(bindings, c.getId());
						  wholeExecTime += infoBoundResourcesCTask.getKey();
					  }
					  for(CommunicationTask  c: actorWrites.get(v)) {
						  MyEntry<Integer,ArrayList<Integer>> infoBoundResourcesCTask = this.getBoundResources(bindings, c.getId());
						  wholeExecTime += infoBoundResourcesCTask.getKey();
					  }
					  int start = startTime.get(v);
					  
					  // get the candidate start times in the bound core
					  MyEntry<Integer,Integer> asapStartTime = new MyEntry<Integer,Integer>(start,wholeExecTime); 
					  Queue<MyEntry<Integer,Integer>> candidateStartTimes = U.getCandidateStartsInBoundResources(boundResources, startTime.get(v),wholeExecTime);
					  Queue<MyEntry<Integer,Integer>> setCandidateStartTimes = new LinkedList<>();
					  setCandidateStartTimes.add(asapStartTime);
					  setCandidateStartTimes.addAll(candidateStartTimes);
					  boolean state = false;
					  // first check if I can use the suggested start time
					  for(MyEntry<Integer,Integer> q : candidateStartTimes) {
						  if(U.canInsertIntervalUtilizationTable(v, boundResources, q.getKey(), q.getKey()+wholeExecTime ,wholeExecTime)) {
							  // propose a start time for each communication task 
							  startTimes = new HashMap<>();
							  // first set the reads
							  int taskStart = q.getKey();
							  for(CommunicationTask c : actorReads.get(v)) {
								  startTimes.put(c, taskStart);
								  taskStart += c.getDiscretizedRuntime();
							  }
							  taskStart += discreteRuntime;
							  for(CommunicationTask c : actorReads.get(v)) {
								  startTimes.put(c, taskStart);
								  taskStart += c.getDiscretizedRuntime();
							  }
							  ArrayList<Boolean> canSchedule = new ArrayList<>();
							  for(Map.Entry<CommunicationTask, Integer> sp : startTimes.entrySet()) {
								  CommunicationTask c = sp.getKey();
								  MyEntry<Integer,ArrayList<Integer>> infoBoundResourcesCTask = this.getBoundResources(bindings, c.getId());
								  if(U.canInsertIntervalUtilizationTable(c.getId(), infoBoundResourcesCTask.getValue(), sp.getValue(), sp.getValue() +  c.getDiscretizedRuntime(), c.getDiscretizedRuntime()))
									  canSchedule.add(true);
								  else
									  break;
							  }
							  if (canSchedule.size() != startTimes.size())
								  continue;
							  canSchedule = new ArrayList<Boolean>( new HashSet<Boolean>(canSchedule));
							  if (canSchedule.size() == 1) {
								  if (canSchedule.get(0) == false)  // paranoia checks
									  continue;}
							  else
								  continue; // paranoia checks
							  // then I can schedule all the tasks
							  // schedule the core
							  boolean successSchedule = U.insertIntervalUtilizationTable(v, boundResources, q.getKey(), q.getKey()+wholeExecTime , wholeExecTime);
							  assert successSchedule : "This must not happen";
							  for(Map.Entry<CommunicationTask, Integer> sp : startTimes.entrySet()) {
								  CommunicationTask c = sp.getKey();
								  MyEntry<Integer,ArrayList<Integer>> infoBoundResourcesCTask = this.getBoundResources(bindings, c.getId());
								  successSchedule = U.insertIntervalUtilizationTable(c.getId(), infoBoundResourcesCTask.getValue(), sp.getValue(), sp.getValue() + c.getDiscretizedRuntime() , c.getDiscretizedRuntime());
								  assert successSchedule : "This must not happen";
							  }
							  state = true;
							  break;  // succes in scheduling all the tasks
						  }
					  }
					  if(!state)
						  return false;

					  /** THIS CODE BELOGN TO A BACKUP WORKING IMP MORE TIME DEMANDING
					   * int upperBound = start % this.P;
					  //System.out.println("actor "+application.getActors().get(v).getName()+ " lenght "+discreteRuntime);
					  while(!U.insertIntervalUtilizationTable(v, boundResources, startTime.get(v), startTime.get(v)+discreteRuntime ,discreteRuntime)) {
						  //System.out.println("Trying to insert"+application.getActors().get(v).getName()+" at "+startTime.get(v)+" to "+((startTime.get(v) + discreteRuntime) % this.P ));
						  startTime.put(v, startTime.get(v)+1 );
						  if (upperBound == startTime.get(v) % P ) {
							  // if it not possible to schedule with this P, you have to increase P
							  return false;
							  //System.exit(1);  // here I have to increase the MII
						  }  
					  }*/
					  // update info for actor
					  timeInfoActors.put(v, new TimeSlot(v, startTime.get(v),startTime.get(v) + wholeExecTime ));
					  // update info for communication tasks
					  for(Map.Entry<CommunicationTask, Integer> sp : startTimes.entrySet()) {
						  CommunicationTask c = sp.getKey();
						  timeInfoActors.put(c.getId(), new TimeSlot(c.getId(), sp.getValue(), sp.getValue() + c.getDiscretizedRuntime()));
					  }
					  for (int w : SUCC.get(v)) {
						  PCOUNT.put(w, PCOUNT.get(w) -1 );
						  //if (startTime.get(w) <= (startTime.get(v)+ discreteRuntime))
						  //    startTime.put(w,startTime.get(v)+ discreteRuntime);
						  int maxVal = startTime.get(w) > (startTime.get(v)+ wholeExecTime)  ? startTime.get(w) : (startTime.get(v)+wholeExecTime);
						  startTime.put(w,maxVal);
					  }
					  
					  removeV.add(v);
					  break;
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
		  for(Map.Entry<Integer, Actor > actor : application.getActors().entrySet()) {
			  Actor origActor = application.getActor(actor.getValue().getName());
			  //System.out.println("actor "+actor.getValue().getName()+" id "+actor.getKey());
			  if(actor.getValue().getType() == ACTOR_TYPE.ACTOR || actor.getValue().getType() == ACTOR_TYPE.MULTICAST) {
				  Processor p = bindings.getActorProcessorBindings().get(origActor.getId()).getTarget();
				  int coreTypeIx = coreTypes.indexOf(p.getProcesorType());
				  MyEntry<Integer,Integer> key = new MyEntry<Integer,Integer>(coreTypeIx, p.getId());
				  usageCores.put(key, 0);
			  }
		  }
		  for(Map.Entry<Integer, CommunicationTask> cs : setCommunicationTasks.entrySet()) {
			  CommunicationTask comm = cs.getValue();
			  if(comm.getUsedNoc() != null) {
				  usageNoC.put(comm.getUsedNoc().getId(), 0);
			  }
			  if(comm.getUsedLocalMemory() != null) {
				  usageLocalMemory.put(comm.getUsedLocalMemory().getId(), 0);
			  }
			  for(Crossbar c : comm.getUsedCrossbars()) {
				  usageCrossbar.put(c.getId(), 0);
			  }
		  }
		  // 1 [Compute resource usage]
		  // Examine the loop body to determine the usage, usage(i), of each resource class R(i) by the loop body
		  // <K,V> here the key is the id of the tile and the value is the usage of cpus in the tile
		  // update the usage
		  int maxExTime = 0;
		  for(Map.Entry<Integer, Actor > actor : application.getActors().entrySet()) {
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
		  } 
		  for(Map.Entry<Integer, CommunicationTask> cs : setCommunicationTasks.entrySet()) {
		      // first init the crossbar
			  CommunicationTask comm = cs.getValue();
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
			  if (comm.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK) 
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
		  //System.out.println("Heuristic with Communications MII "+MII);
	  }
/**
 * 	I need to determine an accurate lower bound, this might be a hint of a possible solution
 *  A better lower bound is required e.g., in multicamera application to accelerate the search 
 * 
	  private void calculateMIISecond(Bindings bindings) {
		  // key: core type
		  // Value: count
		  HashMap<Integer,Integer> usageCores = new HashMap<>();
			  
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
				  usageCores.put(coreTypeIx, 0);
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
				  int val = usageCores.get(coreTypeIx);
				  int discreteRuntime = (int)bindings.getActorProcessorBindings().get(origActor.getId()).getProperties().get("discrete-runtime");
				  maxExTime = (discreteRuntime > maxExTime) ? discreteRuntime : maxExTime;
				  usageCores.put(coreTypeIx, val+discreteRuntime);
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
				  int val = usageCores.get(coreTypeIx);
				  int discreteRuntime = comm.getDiscretizedRuntime();
				  maxExTime = (discreteRuntime > maxExTime) ? discreteRuntime : maxExTime;
				  usageCores.put(coreTypeIx, val + discreteRuntime);
				  
			  }
		  } 
		  // System.out.println("USAGE: "+usage);
		  // 	3 [Compute the lower bound of minimum initiation interval]
		  // 		a) [Compute the resource-constrained initiation interval]
		  List<Integer> tmpL = new ArrayList<>();
		  for(HashMap.Entry<Integer,Integer> u :usageCores.entrySet()){
			  tmpL.add((int)Math.ceil( (double) u.getValue() / (double)countCoresPerType.get(u.getKey())) );
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
		  //this.MII = Collections.max(tmpL);
		  maxExTime = (this.MII > maxExTime) ? this.MII : maxExTime;
		  System.out.println("MII second "+MII);
	  } */
	  
	  public MyEntry<Integer,ArrayList<Integer>> getBoundResources(Bindings bindings, int actorId) {
		  ArrayList<Integer> boundResources = new ArrayList<>();
		  int discreteRuntime = -1;
		  
		  if (application.getActors().containsKey(actorId)) {
			  Actor origActor = application.getActors().get(actorId);
			  if (origActor.getType() == ACTOR_TYPE.ACTOR || origActor.getType() == ACTOR_TYPE.MULTICAST) {
				  // mapped to core
				  int coreId = bindings.getActorProcessorBindings().get(origActor.getId()).getTarget().getId();
				  boundResources.add(coreId);
				  discreteRuntime = (int)bindings.getActorProcessorBindings().get(origActor.getId()).getProperties().get("discrete-runtime");
			  }
		  }else if(setCommunicationTasks.containsKey(actorId)) {
			  CommunicationTask comm = setCommunicationTasks.get(actorId);
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
			  if (comm.getType() == ACTOR_TYPE.READ_COMMUNICATION_TASK) 
				  operator = application.getActors().get( comm.getFifo().getDestination().getId() ) ;
			  else
				  operator = application.getActors().get( comm.getFifo().getSource().getId() ) ;
			  assert operator != null;
			  int coreId = bindings.getActorProcessorBindings().get(operator.getId()).getTarget().getId();
			  boundResources.add(coreId);
		  }else
			  assert false : "This should not happen!!!!";
		  
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
	  public int getMII() {
		  return this.MII;
	  }
	}
