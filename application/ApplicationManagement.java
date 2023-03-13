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
  @date  16 November 2022
  @version 1.1
  @ brief
        This class implements methods for application management
 
--------------------------------------------------------------------------
*/

package multitile.application;

import java.util.*;

import multitile.architecture.Architecture;
import multitile.architecture.Tile;
import multitile.architecture.Processor;
import multitile.architecture.Memory;

import multitile.application.Actor;
import multitile.application.Application;
import multitile.application.Fifo;
import multitile.application.CompositeFifo;

import multitile.scheduler.ModuloScheduler;

public class ApplicationManagement{

	public static void remapFifo(Fifo fifo,Application application, Memory newMapping){
			application.getFifos().get(fifo.getId()).setMapping(newMapping);
	}

  public static void assignActorMapping(Application application,Architecture architecture, ModuloScheduler scheduler){
    HashMap<Integer,List<Integer>> kernel = scheduler.getKernel();
    // hashmap tile ID, list of actors ID
    HashMap<Integer,List<Integer>> tilesToActors = new HashMap<>();
    for(Map.Entry<Integer,Tile> entry : architecture.getTiles().entrySet()){
      List<Integer> lActors = new ArrayList<Integer>();
      tilesToActors.put(entry.getKey(),lActors);
    }
    for(int step=scheduler.getStepStartKernel(); step <= scheduler.getStepEndKernel(); step++){
      // clean list
      for(Map.Entry<Integer,List<Integer>> entry: tilesToActors.entrySet())
        entry.getValue().clear();
      for(int actorId : kernel.get(step)){
        int mapTile = application.getActors().get(actorId).getMappingToTile().getId();
        tilesToActors.get(mapTile).add(actorId);
      }
      // now, reasign the mapping to the actor
      for(Map.Entry<Integer,List<Integer>> entry: tilesToActors.entrySet()){
        // List of processor in tile
        ArrayList<Processor> processors = new ArrayList<>(architecture.getTiles().get(entry.getKey()).getProcessors().values());
        int countProcessor = 0;
        for(int actorId : entry.getValue()){
          // assigning the new mapping according to the Modulo Scheduler
          application.getActors().get(actorId).setMapping(processors.get(countProcessor));
          countProcessor++;
        }
      }
    }
  }

  // this method assign the mapping of each fifo according the type
  public static void assignFifoMapping(Application application, Architecture architecture){
    for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
      Fifo.FIFO_MAPPING_TYPE type = f.getValue().getMappingType();
      Processor p;
      switch(type){
        case SOURCE:
          p = f.getValue().getSource().getMapping();
          //f.getValue().setMapping(p.getLocalMemory());
          application.getFifos().get(f.getKey()).setMapping(p.getLocalMemory());
          break;
        case DESTINATION:
          p = f.getValue().getDestination().getMapping();
          //f.getValue().setMapping(p.getLocalMemory());
          application.getFifos().get(f.getKey()).setMapping(p.getLocalMemory());
          break;
        case TILE_LOCAL:
          //System.out.println("Type:"+type);
          Tile t = f.getValue().getMappingToTile();
          f.getValue().setMapping(t.getTileLocalMemory());
          application.getFifos().put(f.getKey(),f.getValue());
          break;
        case GLOBAL:
          application.getFifos().get(f.getKey()).setMapping(architecture.getGlobalMemory());
          break;
      }
    }
    // update actors
    for(Map.Entry<Integer,Actor> e: application.getActors().entrySet()){
      Vector<Fifo> inputs = e.getValue().getInputFifos();
      Vector<Fifo> outputs = e.getValue().getOutputFifos();
      Vector<Fifo> newInputs = new Vector<Fifo>();
      Vector<Fifo> newOutputs = new Vector<Fifo>();

      for(Fifo i : inputs){
        newInputs.add(application.getFifos().get(i.getId()));
      }
      for(Fifo o : outputs){
        newOutputs.add(application.getFifos().get(o.getId()));
      }
      e.getValue().setInputFifos(newInputs);
      e.getValue().setOutputFifos(newOutputs);
    }
  
  }

  public static Map<Integer,Actor> getMulticastActors(Application app){
    //System.out.println("getMulticastActors");
    Map<Integer,Actor> multicastActors = new HashMap<>();
    for(Map.Entry<Integer,Actor> actor : app.getActors().entrySet()){
      //System.out.println("Name: "+actor.getValue().getName());
      //System.out.println("\tType: "+actor.getValue().getType());
      if (actor.getValue().isMulticastActor() == true){
        //System.out.println("IS Multicast actor "+actor.getValue().getName());
        multicastActors.put(actor.getKey(),actor.getValue());
      }
    }
    return multicastActors;
  }

  // this method set to true all the multicast actors in the application
  public static void setAllMulticastActorsAsMergeable(Application app){
    //System.out.println("setAllMulticastActorsAsMergeable");
    // get all the multicast actors
    Map<Integer,Actor> multicastActors = getMulticastActors(app);
    
    for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
      Actor selectedActor = multicastActor.getValue();
      //System.out.println("Setting: "+selectedActor.getName());
      app.getActors().get(selectedActor.getId()).setMergeMulticast(true);
    }
  }

  // these method receives an application and returns a modified application removing 
  // all the mergeable multicast actors from the application and replace them by composite channels
  public static void collapseMergeableMulticastActors(Application app){
    // get all the multicast actors
    Map<Integer,Actor> multicastActors = getMulticastActors(app);
    
    for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
      Actor selectedActor = multicastActor.getValue();
      
      if(selectedActor.isMergeMulticast() == true){
        // if the actor is mergeable, we remove it and replace it by a composite channel
        Vector<Fifo> inputFifos  = selectedActor.getInputFifos(); // it should be only one writer
        Vector<Fifo> outputFifos = selectedActor.getOutputFifos(); // it might be multiple readers, more that one
        Fifo writer = inputFifos.get(0);
        List<Fifo>  readerFifos = new ArrayList<Fifo>(outputFifos);

        CompositeFifo compositeFifo = FifoManagement.createCompositeChannel(writer,readerFifos,selectedActor); 
        // once created the compositefifo, we have to connected into the application
        int idWriterActor = writer.getSource().getId();
        app.getActors().get(idWriterActor).removeOutputFifo(writer.getId());
        // connecting the input of the composite fifo
        app.getActors().get(idWriterActor).getOutputFifos().add(compositeFifo);

        // now connect the readers to the composite fifo
        for(Fifo dstFifo : readerFifos){
          int idReaderActor = dstFifo.getDestination().getId();
          app.getActors().get(idReaderActor).removeInputFifo(dstFifo.getId());
          // connectinf the outputs of the composite fifo
          app.getActors().get(idReaderActor).getInputFifos().add(compositeFifo);
        }
        // remove the merged multicast actor from the map of actors
        app.getActors().remove(multicastActor.getKey());
        // add the new composite fifo into the app fifo map
        app.getFifos().put(compositeFifo.getId(),compositeFifo);
      }
    }
  }

}
