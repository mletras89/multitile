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
     This class offers the basic methods for an scheduler, to implement
     a specific scheduler, extends using this classes and implement the 
     interface Schedule

        - scheduledActions is the list of scheduled actions in a given processor
        - queueActions queue of actions to be scheduled
        - readTransfers read transfers executed in each simulation step
        - writeTransfers write transfers executed in each simulation step
--------------------------------------------------------------------------
*/
package multitile.architecture;

import multitile.Action;
import multitile.Transfer;
import multitile.MapManagement;

import multitile.scheduler.SchedulerManagement;

import multitile.application.Actor;
import multitile.application.Fifo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.Queue;
import java.util.Arrays;

public class Scheduler{
  private LinkedList<Action> scheduledActions;
  private Queue<Action> queueActions;
  private Map<Actor,List<Transfer>> readTransfers;
  private Map<Actor,List<Transfer>> writeTransfers;
  // this map is to 
  private List<Transfer> readTransfersToMemory;
  private List<Transfer> writeTransfersToMemory;
  private List<Transfer> transfersToMemory;

  // including also the processor owner of this scheduler
  private Processor owner;

  private double lastEventinProcessor;
  private int numberIterations;
  private int runIterations;
  private String name;

  public Scheduler(String name,Processor owner){
    this.scheduledActions = new LinkedList<Action>();
    this.queueActions = new LinkedList<>();
    this.readTransfers = new HashMap<>();
    this.writeTransfers = new HashMap<>();
    this.lastEventinProcessor = 0.0;
    this.numberIterations = 1;
    this.runIterations = 0;
    this.name = name;
    this.owner = owner;
    this.readTransfersToMemory = new ArrayList<>();
    this.writeTransfersToMemory = new ArrayList<>();
    this.transfersToMemory = new ArrayList<>();
  }

  public void restartScheduler() {
    this.scheduledActions.clear();
    this.queueActions.clear();
    this.readTransfers.clear();
    this.writeTransfers.clear();
    this.lastEventinProcessor = 0.0;
    this.runIterations = 0;
    this.readTransfersToMemory.clear();
    this.writeTransfersToMemory.clear();
    this.transfersToMemory.clear();
  } 

  public Processor getOwner(){
    return this.owner;
  }
  
  public void setOwner(Processor owner){
    this.owner = owner;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void cleanQueue(){
    this.queueActions.clear();
  }

  public void setNumberIterations(int numberIterations){
    this.numberIterations = numberIterations;
  }

  public int getNumberIterations(){
    return this.numberIterations;
  }

  public int getRunIterations(){
    return this.runIterations;
  }

  public void setScheduledActions(LinkedList<Action> scheduledActions) {
    this.scheduledActions = scheduledActions;
  }
  
  public LinkedList<Action> getScheduledActions(){
    return this.scheduledActions;
  }

  public void setQueueActions(Queue<Action> queueActions) {
    this.queueActions = queueActions;
  }
 
  public Map<Actor,List<Transfer>> getReadTransfers(){
    return this.readTransfers;
  } 

  public List<Transfer> getWriteTransfersToMemory(){
    return this.writeTransfersToMemory;
  }

  public List<Transfer> getReadTransfersToMemory(){
    return this.readTransfersToMemory;
  }

  public List<Transfer> getTransfersToMemory(){
    return this.transfersToMemory;
  }

  public void updateWritesStateMemory(){
    for(Transfer t : this.writeTransfersToMemory){
      // now we update the memory of this transfer
      int numberTokens = t.getFifo().getProdRate();
      int numberBytesperToken  = t.getFifo().getTokenSize();
      t.getFifo().getMapping().writeDataInMemory(numberTokens*numberBytesperToken,t.getDue_time());
    }
  }

  public void updateReadsStateMemory(){
    for(Transfer t : this.readTransfersToMemory){
      // now we update the memory of this transfer
      int numberTokens = t.getFifo().getConsRate();
      int numberBytesperToken  = t.getFifo().getTokenSize();
      t.getFifo().getMapping().readDataInMemory(numberTokens*numberBytesperToken,t.getDue_time());
    }
  }

  public void updateStateMemory(){
    SchedulerManagement.sort(transfersToMemory);
    for(Transfer t: this.transfersToMemory){
      // now we update the memory of this transfer
      int numberTokens = t.getFifo().getProdRate();
      int numberBytesperToken  = t.getFifo().getTokenSize();
      if(t.getType() == Transfer.TRANSFER_TYPE.READ)
        t.getFifo().fifoReadFromMemory(t);
      else
        t.getFifo().fifoWriteToMemory(t);
    }
  }

  public void setReadTransfersToMemory(){
    for(Map.Entry<Actor,List<Transfer>> entry : this.readTransfers.entrySet()){
      // setting the read Transfers
      for(Transfer t: entry.getValue()){
        transfersToMemory.add(t);
      }
    }   
  }

  public void setWriteTransfersToMemory(){
    for(Map.Entry<Actor,List<Transfer>> entry : this.writeTransfers.entrySet()){
      // setting the read Transfers
      for(Transfer t: entry.getValue()){
        transfersToMemory.add(t);
      }
    }
  }

  public void setTransfersToMemory(){
    for(Map.Entry<Actor,List<Transfer>> entry : this.readTransfers.entrySet()){
      // setting the read Transfers
      for(Transfer t: entry.getValue()){
        transfersToMemory.add(t);
      }
    }   

    for(Map.Entry<Actor,List<Transfer>> entry : this.writeTransfers.entrySet()){
      // setting the read Transfers
      for(Transfer t: entry.getValue()){
        transfersToMemory.add(t);
      }
    }
  }

  public void setReadTransfers(Map<Actor,List<Transfer>> readTransfers){
    this.readTransfers = readTransfers;
  } 

  public void setWriteTransfers(Map<Actor,List<Transfer>> writeTransfers){
      this.writeTransfers = writeTransfers;
  } 

  public double getTimeLastReadofActor(Actor actor){
    //if(readTransfers.containsKey(actor)){
     if(MapManagement.isActorIdinMap(readTransfers.keySet(),actor.getId())){
      double max = 0.0;
      for(Transfer transfer : readTransfers.get(actor)){
        if (transfer.getDue_time() > max){
          max = transfer.getDue_time();
        }
      }
      return max;
    }
    return 0.0;
  }

  public double getTimeLastWriteofActor(Actor actor){
    //if(writeTransfers.containsKey(actor)){
    if(MapManagement.isActorIdinMap(writeTransfers.keySet(),actor.getId())){
      double max = 0.0;
      for(Transfer transfer : writeTransfers.get(actor)){
        if (transfer.getDue_time() > max){
          max = transfer.getDue_time();
        }
      }
      return max;
    }
    return 0.0;
  }

  public Map<Actor,List<Transfer>> getWriteTransfers(){
    return this.writeTransfers;
  } 

  public Queue<Action> getQueueActions(){
    return this.queueActions;
  }

  public void setLastEventinProcessor(double lastEventinProcessor){
    this.lastEventinProcessor  = lastEventinProcessor;
  }

  public double getLastEventinProcessor(){
    return this.lastEventinProcessor;
  }

  public void insertAction(Action a){
    queueActions.add(new Action(a));
  }

  public void produceTokensinFifo(Map<Integer,Fifo> fifoMap){
    for(Action commitAction : this.queueActions){
      if(MapManagement.isActorIdinMap(writeTransfers.keySet(),commitAction.getActor().getId())){
        List<Transfer> writes = writeTransfers.get(commitAction.getActor());
        for(Transfer transfer: writes){
          fifoMap.get(transfer.getFifo().getId()).insertTimeProducedToken(transfer);
        }
      }
    }
  }

  public void produceTokensinFifo(Action commitAction,Map<Integer,Fifo> fifoMap){
    if(MapManagement.isActorIdinMap(writeTransfers.keySet(),commitAction.getActor().getId())){
      List<Transfer> writes = writeTransfers.get(commitAction.getActor());
      for(Transfer transfer: writes){
        fifoMap.get(transfer.getFifo().getId()).insertTimeProducedToken(transfer);
      }
    }
  }
  
  public void updateLastEventAfterWrite(Action action){
    double lastWrite = this.getTimeLastWriteofActor(action.getActor());
    //System.out.println("Last Write:"+lastWrite);
    if (lastWrite > this.lastEventinProcessor)
      this.lastEventinProcessor = lastWrite;
  }

  public void syncTimeOfSrcActors(Action commitAction, Architecture architecture){
    List<Transfer>  transfers = this.readTransfers.get(commitAction.getActor());
    if(transfers != null){
      //System.out.println("Number of read transfers: "+transfers.size());
      for(Transfer t: transfers){
        //System.out.println(t);
        Actor actorWriting = t.getFifo().getSource();
        if(actorWriting.getInputFifos().size() == 0){
          // update the processor after reading the token
          //System.out.println("ACTOR ACTOR: "+actorWriting.getName());
          //assert true;
          double timeProcessor = t.getDue_time();
          ArchitectureManagement.updateLastEventInProcessor(architecture,actorWriting.getMapping(),timeProcessor);
        }
      }
    }
  }

  public void commitSingleAction(Action commitAction){
    // proceed to schedule the Action
    double ActionTime = commitAction.getProcessing_time();
    double startTime = Collections.max(Arrays.asList(this.lastEventinProcessor,commitAction.getStart_time(),this.getTimeLastReadofActor(commitAction.getActor())));
    double endTime = startTime + ActionTime;
    // update now the commit Action
    commitAction.setStart_time(startTime);
    commitAction.setDue_time(endTime);
    // update the last event in processor
    this.lastEventinProcessor = endTime;
    // commit the Action
    this.scheduledActions.addLast(commitAction);
    //System.out.println("\tScheduling actor "+commitAction.getActor().getName()+ " start time "+commitAction.getStart_time()+" due time "+commitAction.getDue_time());
  }

  public void commitSingleAction(Action commitAction,Architecture architecture){
    // proceed to schedule the Action
    double ActionTime = commitAction.getProcessing_time();
    double startTime = Collections.max(Arrays.asList(this.lastEventinProcessor,commitAction.getStart_time(),this.getTimeLastReadofActor(commitAction.getActor())));
    double endTime = startTime + ActionTime;
    // update now the commit Action
    commitAction.setStart_time(startTime);
    commitAction.setDue_time(endTime);
    // update the last event in processor
    this.lastEventinProcessor = endTime;
    // commit the Action
    this.scheduledActions.addLast(commitAction);
    System.out.println("COMITTING:"+commitAction.getActor().getName());
    this.syncTimeOfSrcActors(commitAction,architecture);
  }


  public void commitActionsinQueue(){
    // then commit all the schedulable Actions in the queue
    for(Action commitAction : this.queueActions){
        commitSingleAction(commitAction);
//      // proceed to schedule the Action
//      double ActionTime = commitAction.getProcessing_time();
//      double startTime = Collections.max(Arrays.asList(this.lastEventinProcessor,commitAction.getStart_time(),this.getTimeLastReadofActor(commitAction.getActor())));
//      double endTime = startTime + ActionTime;
//      // update now the commit Action
//      commitAction.setStart_time(startTime);
//      commitAction.setDue_time(endTime);
//      // update the last event in processor
//      this.lastEventinProcessor = endTime;
//      // commit the Action
//      this.scheduledActions.addLast(commitAction);
//      System.out.println("\tScheduling actor "+commitAction.getActor().getName()+ " start time "+commitAction.getStart_time()+" due time "+commitAction.getDue_time());
    }
  }

  public void commitReadsToCrossbar(Action commitAction,Map<Integer,Fifo> fifos){
    List<Transfer> reads = new ArrayList<>();
    //System.out.println("Actor "+commitAction.getActor().getName());
    for(Fifo fifo : commitAction.getActor().getInputFifos()){
      int cons      = fifo.getProdRate();
      //System.out.println("1)Actor: "+commitAction.getActor().getName()+" reading from "+fifo.getName());
      double timeLastReadToken = fifos.get(fifo.getId()).readTimeProducedToken(cons);
      // I scheduled read of data by token reads
      for(int n = 0 ; n<cons;n++) {
//        if(fifo.getMapping().getType() == Memory.MEMORY_TYPE.TILE_LOCAL_MEM ||
//          (fifo.getMapping().getType() == Memory.MEMORY_TYPE.LOCAL_MEM &&
//          !fifo.getMapping().getEmbeddedToProcessor().equals(commitAction.getActor().getMapping()))){
          // then the read must be scheduled in the crossbar
          Transfer readTransfer = new Transfer(commitAction.getActor(),fifo,Collections.max(Arrays.asList(this.lastEventinProcessor,timeLastReadToken)),Transfer.TRANSFER_TYPE.READ);
          reads.add(readTransfer);
          //System.out.println("2)Actor: "+commitAction.getActor().getName()+" reading from "+fifo.getName()); 
//        }
      }
    }
    readTransfers.put(commitAction.getActor(),reads);
  }

  public void commitReadsToCrossbar(Map<Integer,Fifo> fifos){
    for(Action commitAction : this.queueActions){
      List<Transfer> reads = new ArrayList<>();
      for(Fifo fifo : commitAction.getActor().getInputFifos()){
        //System.out.println("1)Actor: "+commitAction.getActor().getName()+" reading from "+fifo.getName());
        int cons      = fifo.getProdRate();
        double timeLastReadToken = fifos.get(fifo.getId()).readTimeProducedToken(cons);
        // I scheduled read of data by token reads
        for(int n = 0 ; n<cons;n++) {
//          if(fifo.getMapping().getType() == Memory.MEMORY_TYPE.TILE_LOCAL_MEM ||
//            (fifo.getMapping().getType() == Memory.MEMORY_TYPE.LOCAL_MEM &&
//            !fifo.getMapping().getEmbeddedToProcessor().equals(commitAction.getActor().getMapping()))){
            // then the read must be scheduled in the crossbar
            Transfer readTransfer = new Transfer(commitAction.getActor(),fifo,Collections.max(Arrays.asList(this.lastEventinProcessor,timeLastReadToken)),Transfer.TRANSFER_TYPE.READ);
            reads.add(readTransfer);
            //System.out.println("2)Actor: "+commitAction.getActor().getName()+" reading from "+fifo.getName());
//          }
        }
      }
      readTransfers.put(commitAction.getActor(),reads);
    }
  }
  
  public void commitWritesToCrossbar(){
    for(Action commitAction : this.queueActions){
      List<Transfer> writes = new ArrayList<>();
      for(Fifo fifo : commitAction.getActor().getOutputFifos()){
      //System.out.println("1)Actor: "+commitAction.getActor().getName()+" writing to "+fifo.getName());
        int prod    = fifo.getProdRate();
        for(int n=0; n<prod; n++){
          if(fifo.getMapping().getType() == Memory.MEMORY_TYPE.TILE_LOCAL_MEM ||
            (fifo.getMapping().getType() == Memory.MEMORY_TYPE.LOCAL_MEM &&
            !fifo.getMapping().getEmbeddedToProcessor().equals(commitAction.getActor().getMapping()))){
              // Then the write must be scheduled in the crossbar
              Transfer writeTransfer = new Transfer(commitAction.getActor(),fifo,this.lastEventinProcessor,Transfer.TRANSFER_TYPE.WRITE);
              writes.add(writeTransfer);
              //System.out.println("2)Actor: "+commitAction.getActor().getName()+" writing to "+fifo.getName());
          }
        }
      }
      writeTransfers.put(commitAction.getActor(),writes);
    }
  }

  public void commitWritesToCrossbar(Action commitAction){
    List<Transfer> writes = new ArrayList<>();
    for(Fifo fifo : commitAction.getActor().getOutputFifos()){
      //System.out.println("1)Actor: "+commitAction.getActor().getName()+" writing to "+fifo.getName());
      int prod    = fifo.getProdRate();
      for(int n=0; n<prod; n++){
//        if(fifo.getMapping().getType() == Memory.MEMORY_TYPE.TILE_LOCAL_MEM ||
//          (fifo.getMapping().getType() == Memory.MEMORY_TYPE.LOCAL_MEM &&
//          !fifo.getMapping().getEmbeddedToProcessor().equals(commitAction.getActor().getMapping()))){
          // Then the write must be scheduled in the crossbar
          Transfer writeTransfer = new Transfer(commitAction.getActor(),fifo,this.lastEventinProcessor,Transfer.TRANSFER_TYPE.WRITE);
          writes.add(writeTransfer);
          //System.out.println("2)Actor: "+commitAction.getActor().getName()+" writing to "+fifo.getName());
//        }
      }
    }
    writeTransfers.put(commitAction.getActor(),writes);
  }

  public void fireCommitedActions(Map<Integer,Fifo> fifos){
    // update the fifos after the firing of the action
    int elementsinQueue = this.queueActions.size();
    for(int i=0;i<elementsinQueue;i++){
      Action firingAction = this.queueActions.remove();
      firingAction.getActor().fire(fifos);
      if (firingAction.getActor().getName().contains("sink") == true){
       this.runIterations++; 
      }
    }
  }

  // DUMPING the processor utilzation locally
  public void saveScheduleStats(String path) throws IOException{
    try{
        File memUtilStatics = new File(path+"/processor-utilization-"+this.getName()+".csv");
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

    FileWriter myWriter = new FileWriter(path+"/processor-utilization-"+this.getName()+".csv"); 
    myWriter.write("Job\tStart\tFinish\tResource\n");
    saveScheduleStats(myWriter);

    myWriter.close();
  }

  public void saveScheduleStats(FileWriter myWriter) throws IOException{
    for(Action a : scheduledActions){
      myWriter.write(a.getActor().getName()+"\t"+a.getStart_time()+"\t"+a.getDue_time()+"\t"+this.getName()+"\n");
    }
  }

  // printing info for debug purposes
  public void printSchedulableActors(){
    for(Action action : this.getQueueActions()){
      System.out.println("Schedulable action:"+action.getActor().getName());
    } 
  }
}
