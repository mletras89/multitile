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
import multitile.architecture.Processor;

import multitile.application.Actor;
import multitile.application.Fifo;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.ArrayList; 

public class FCFS extends BaseScheduler implements Schedule{
  
  public FCFS(){
    super();
    this.setMaxIterations(1); 
  }
  
  public void schedule(){
    List<Actor> actors = application.getListActors(); 
    Map<Integer,Fifo> fifoMap = application.getFifos();
    for(HashMap.Entry<Integer,Tile> t : architecture.getTiles().entrySet()){
      // reseting all the tiles in the architecture
      t.getValue().resetTile();
    }
    int runIterations = 0;
    List<Transfer> transfersToMemory = new ArrayList<>();
    while(runIterations < this.getMaxIterations()){
      // get schedulable actions in all the processors in all the tiles
      for(HashMap.Entry<Integer,Tile> t :architecture.getTiles().entrySet()){
        for(HashMap.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
          this.getSchedulableActors(p.getValue());
        }
      }
      // proceed to schedule each of the actions per processor
      for(HashMap.Entry<Integer,Tile> t :architecture.getTiles().entrySet()){
        for(HashMap.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
          Queue<Action> actions = p.getValue().getScheduler().getQueueActions();
          for(Action action : actions){
            // first schedule the reads
            p.getValue().getScheduler().commitReadsToCrossbar(action,application.getFifos());
            Map<Actor,List<Transfer>> readTransfers = p.getValue().getScheduler().getReadTransfers();
            t.getValue().getCrossbar().cleanQueueTransfers();
            for(Map.Entry<Actor,List<Transfer>> entry : readTransfers.entrySet()){
              t.getValue().getCrossbar().insertTransfers(entry.getValue());
            }
            //commit the read transfers
            t.getValue().getCrossbar().commitTransfersinQueue();
            // update the read transfers of each processor with the correct due time
            Map<Actor,List<Transfer>> processorReadTransfers = t.getValue().getCrossbar().getScheduledReadTransfers(p.getValue());
            // commit the action in the processor
            p.getValue().getScheduler().setReadTransfers(processorReadTransfers);
            p.getValue().getScheduler().commitSingleAction(action); // modificar este 
            // finally, schedule the write of tokens
            p.getValue().getScheduler().commitWritesToCrossbar(action);
            // put writing transfers to crossbar
            // get write transfers from the scheduler
            Map<Actor,List<Transfer>> writeTransfers = p.getValue().getScheduler().getWriteTransfers();
            for(Map.Entry<Actor,List<Transfer>> entry: writeTransfers.entrySet()){
              t.getValue().getCrossbar().insertTransfers(entry.getValue());
            }
            // commit write transfers in the crossbar
            t.getValue().getCrossbar().commitTransfersinQueue();
            // update the write transfers of each processor with the correct start and due time
            Map<Actor,List<Transfer>> processorWriteTransfers = t.getValue().getCrossbar().getScheduledWriteTransfers(p.getValue());
            p.getValue().getScheduler().setWriteTransfers(processorWriteTransfers);
            // update the last event in processor, taking into the account the processorWriteTransfers
            p.getValue().getScheduler().updateLastEventAfterWrite(action);
            // insert the time of the produced tokens by acton into the correspondent fifos
            p.getValue().getScheduler().produceTokensinFifo(action,application.getFifos());
            // managing the tracking of the memories
            p.getValue().getScheduler().setTransfersToMemory();
            transfersToMemory.addAll(p.getValue().getScheduler().getTransfersToMemory());

            // update the memories
            // clean the transfers to memories
            p.getValue().getScheduler().getTransfersToMemory().clear();
            p.getValue().getScheduler().getReadTransfers().clear();
            p.getValue().getScheduler().getWriteTransfers().clear();
          }
        }
        // when, I finish the tile update last event in each processor with the maximum of all the processors
        double maxTimeP = 0.0;
        for(HashMap.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
          if (maxTimeP < p.getValue().getScheduler().getLastEventinProcessor())
            maxTimeP = p.getValue().getScheduler().getLastEventinProcessor();
        }
        for(HashMap.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
          p.getValue().getScheduler().setLastEventinProcessor(maxTimeP);
        }
      }
      //fire the actions, updating fifos
      for(HashMap.Entry<Integer,Tile> t :architecture.getTiles().entrySet()){
        for(HashMap.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
          p.getValue().getScheduler().fireCommitedActions(application.getFifos());
          p.getValue().getScheduler().getTransfersToMemory().clear();
        }
      }
      // commit the reads/writes to memory
      SchedulerManagement.sort(transfersToMemory);

      for(Transfer t : transfersToMemory){
        if(t.getType() == Transfer.TRANSFER_TYPE.READ)
          t.getFifo().fifoReadFromMemory(t);
        else
          t.getFifo().fifoWriteToMemory(t);
      }
      transfersToMemory.clear();
      runIterations = this.getRunIterations();
    }
  }


  public int getRunIterations(){
    int max = 0 ;
    for(HashMap.Entry<Integer,Tile> e : architecture.getTiles().entrySet()){
      if(max < e.getValue().getRunIterations())
        max = e.getValue().getRunIterations();
    }
    return max;
  }


  public void getSchedulableActors(Processor processor){
    // from the list of actors in Processor, check which of them can fire
    int processorId = processor.getId();
    int tileId = processor.getOwnerTile().getId();

    architecture.getTiles().get(tileId).getProcessors().get(processorId).getScheduler().cleanQueue();
    for(Actor actor: this.application.getListActors()){
      if(actor.getMapping().equals(processor)){
      	if(actor.canFire(application.getFifos())){
          //System.out.println("Fireable: "+actor.getName());
	  Action action = new Action(actor);
          architecture.getTiles().get(tileId).getProcessors().get(processorId).getScheduler().insertAction(action);
      	}
      }
    }
  }

}
