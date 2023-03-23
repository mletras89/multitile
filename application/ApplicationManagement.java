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
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.architecture.Processor;
import multitile.architecture.Memory;

import multitile.application.Actor;
import multitile.application.Application;
import multitile.application.Fifo;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;
import multitile.application.CompositeFifo;

import multitile.scheduler.ModuloScheduler;

public class ApplicationManagement{

	public static Application cloneApplication(Application appSpec){
		Application appImplementation = new Application();
		Map<Integer,Actor> clonedActors = new HashMap<>();
		Map<Integer,Fifo> clonedFifos =  new HashMap<>();
		
		for(Map.Entry<Integer, Actor> actor : appSpec.getActors().entrySet()) {
			Actor clonedActor = new Actor(actor.getValue());
			clonedActors.put(clonedActor.getId(), clonedActor);
		}
		// cloning the fifos
		for(Map.Entry<Integer, Fifo> fifo: appSpec.getFifos().entrySet()) {
			Fifo clonedFifo = new Fifo(fifo.getValue());
			clonedFifos.put(clonedFifo.getId(), clonedFifo);
		}
		appImplementation.setActors(clonedActors);
		appImplementation.setFifos(clonedFifos);
		return appImplementation;
	}
	
	public static void setAllFifosToSource(Application app) {
		for(Map.Entry<Integer, Fifo> fifo: app.getFifos().entrySet()) {
			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.SOURCE);
		}
	}
	
	public static void setAllFifosToDestination(Application app) {
		for(Map.Entry<Integer, Fifo> fifo: app.getFifos().entrySet()) {
			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.DESTINATION);
		}
	}
	
	public static void setAllFifosToTileLocalMemory(Application app, FIFO_MAPPING_TYPE type) {
		for(Map.Entry<Integer, Fifo> fifo: app.getFifos().entrySet()) {
			fifo.getValue().setMappingType(type);
		}
	}
	
	public static void setAllFifosToGlobalMemory(Application app) {
		for(Map.Entry<Integer, Fifo> fifo: app.getFifos().entrySet()) {
			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.GLOBAL);
		}
	}
	
	public static void setFifosToDecision(Application app,ArrayList<Integer> decision) {
		//System.out.println("Decision size "+decision.size());
		//System.out.println("number fifos "+app.getFifos().size());
		assert app.getFifos().size() == decision.size() : "SOMETHING WRONG SIZE OF fifos AND decision MUST BE THE SAME";
		int counter = 0;
        for(Map.Entry<Integer, Fifo> fifo : app.getFifos().entrySet()) {
        	int fifoMapping = decision.get(counter);
        	switch(fifoMapping) {
        		case 0:
        			// fifo mapped to produced
        			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.SOURCE);
        			break;
        		case 1:
        			// fifo mapped to consumer
        			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.DESTINATION);
        			break;
        		case 2:
        			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
        			// fifo mapped to tile local memory
        			break;
        		case 3:
        			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.TILE_LOCAL_DESTINATION);
        			// fifo mapped to tile local memory
        			break;
        		default:
        			// fifo mapped to global memory
        			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.GLOBAL);
        	}
        	counter++;
        }
	}
	
	public static void setFifosToActors(Application app) {
		for(Map.Entry<Integer, Actor> actor : app.getActors().entrySet()) {
			Vector<Fifo> inputs= new Vector<Fifo>();
			Vector<Fifo> outputs = new Vector<Fifo>();
			for(Map.Entry<Integer, Fifo> fifo : app.getFifos().entrySet()) {
				if (fifo.getValue().getSource().equals(actor.getValue())){
					outputs.add(fifo.getValue());
				}
				if (fifo.getValue().getDestination().equals(actor.getValue())) {
					inputs.add(fifo.getValue());
				}
			}
			app.getActors().get(actor.getKey()).setInputFifos(inputs);
			app.getActors().get(actor.getKey()).setOutputFifos(outputs);
		}
	}
		
	public static void remapFifo(Fifo fifo, Memory newBinding, Bindings bindings){
		bindings.getFifoMemoryBindings().put(fifo.getId(), new Binding<Memory>(newBinding));
	}

  public static void assignActorMapping(Application application,Architecture architecture, ModuloScheduler scheduler,Bindings bindings){
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
        int mapTile =  bindings.getActorTileBindings().get(actorId).getTarget().getId();   // application.getActors().get(actorId).getMappingToTile().getId();
        tilesToActors.get(mapTile).add(actorId);
      }
      // now, reasign the mapping to the actor
      for(Map.Entry<Integer,List<Integer>> entry: tilesToActors.entrySet()){
        // List of processor in tile
        ArrayList<Processor> processors = new ArrayList<>(architecture.getTiles().get(entry.getKey()).getProcessors().values());
        int countProcessor = 0;
        for(int actorId : entry.getValue()){
          // assigning the new mapping according to the Modulo Scheduler
          Processor p = processors.get(countProcessor);
          bindings.getActorProcessorBindings().put(actorId, new Binding<Processor>( p ));
          //application.getActors().get(actorId).setMapping(processors.get(countProcessor));
          countProcessor++;
        }
      }
    }
  }

  // this method assign the mapping of each fifo according the type
  public static void assignFifoMapping(Application application, Architecture architecture,Bindings bindings){
    for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
      //System.out.println("Fifo: "+f.getValue().getName()+" mapping type "+f.getValue().getMappingType());
      Fifo.FIFO_MAPPING_TYPE type = f.getValue().getMappingType();
      int processorId, tileId, sourceActorId,destinationActorId;
      switch(type){
        case SOURCE:
          sourceActorId = f.getValue().getSource().getId();
          processorId 	=  bindings.getActorProcessorBindings().get(sourceActorId).getTarget().getId(); //  application.getActors().get(sourceActorId).getMapping().getId();
          tileId 		= bindings.getActorTileBindings().get(sourceActorId).getTarget().getId();    // application.getActors().get(sourceActorId).getMappingToTile().getId();
          //System.out.println("Source "+f.getValue().getSource().getName()+" mapped to "+architecture.getTiles().get(tileId).getProcessors().get(processorId).getName());
          bindings.getFifoMemoryBindings().put(f.getKey(), new Binding<Memory>( architecture.getTiles().get(tileId).getProcessors().get(processorId).getLocalMemory() ) );
          //application.getFifos().get(f.getKey()).setMapping( architecture.getTiles().get(tileId).getProcessors().get(processorId).getLocalMemory()  );
          break;
        case DESTINATION:
          destinationActorId 	= f.getValue().getDestination().getId();
          processorId 			= bindings.getActorProcessorBindings().get(destinationActorId).getTarget().getId(); // application.getActors().get(destinationActorId).getMapping().getId();
          tileId  				= bindings.getActorTileBindings().get(destinationActorId).getTarget().getId();  //application.getActors().get(destinationActorId).getMappingToTile().getId();
          //application.getFifos().get(f.getKey()).setMapping( architecture.getTiles().get(tileId).getProcessors().get(processorId).getLocalMemory());
          bindings.getFifoMemoryBindings().put(f.getKey(), new Binding<Memory>(architecture.getTiles().get(tileId).getProcessors().get(processorId).getLocalMemory() ));
          break;
        case TILE_LOCAL_SOURCE:
        	sourceActorId 	= f.getValue().getSource().getId();
            processorId 	= bindings.getActorProcessorBindings().get(sourceActorId).getTarget().getId(); // application.getActors().get(sourceActorId).getMapping().getId();
            tileId 			= bindings.getActorTileBindings().get(sourceActorId).getTarget().getId(); //   application.getActors().get(sourceActorId).getMappingToTile().getId();
            //application.getFifos().get(f.getKey()).setMapping( architecture.getTiles().get(tileId).getTileLocalMemory()  );
            bindings.getFifoMemoryBindings().put(f.getKey(), new Binding<Memory>( architecture.getTiles().get(tileId).getTileLocalMemory() ));
        	break;
        case TILE_LOCAL_DESTINATION:
        	destinationActorId 	= f.getValue().getDestination().getId();
            processorId			= bindings.getActorProcessorBindings().get(destinationActorId).getTarget().getId();  //application.getActors().get(destinationActorId).getMapping().getId();
            tileId  			= bindings.getActorTileBindings().get(destinationActorId).getTarget().getId();  //application.getActors().get(destinationActorId).getMappingToTile().getId();
            //application.getFifos().get(f.getKey()).setMapping( architecture.getTiles().get(tileId).getTileLocalMemory()  );
            bindings.getFifoMemoryBindings().put(f.getKey(), new Binding<Memory>( architecture.getTiles().get(tileId).getTileLocalMemory() ) );
            break;
        case GLOBAL:
          //application.getFifos().get(f.getKey()).setMapping(architecture.getGlobalMemory());
          bindings.getFifoMemoryBindings().put(f.getKey(), new Binding<Memory>( architecture.getGlobalMemory() ));
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
  
  public static void setAllMulticastActorsAsNotMergeable(Application app){
	    //System.out.println("setAllMulticastActorsAsMergeable");
	    // get all the multicast actors
	    Map<Integer,Actor> multicastActors = getMulticastActors(app);
	    
	    for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
	      Actor selectedActor = multicastActor.getValue();
	      //System.out.println("Setting: "+selectedActor.getName());
	      app.getActors().get(selectedActor.getId()).setMergeMulticast(false);
	    }
  }
  
  // this method selectively set multicast actors as mergeable
  public static void setMulticastActorsAsMergeable(Application app, ArrayList<Boolean> mergeable) {
	// get all the multicast actors
	Map<Integer,Actor> multicastActors = getMulticastActors(app);
	assert multicastActors.size() == mergeable.size() : "SOMETHING WRONG SIZE OF multicastActors AND mergeable MUST BE THE SAME";
	int count = 0;
	for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
	      Actor selectedActor = multicastActor.getValue();
	      app.getActors().get(selectedActor.getId()).setMergeMulticast(mergeable.get(count));
	      count++;
	}
  }
  
  // these method receives an application and returns a modified application removing 
  // all the mergeable multicast actors from the application and replace them by composite channels
  public static void collapseMergeableMulticastActors(Application app,Bindings bindings){
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

        CompositeFifo compositeFifo = FifoManagement.createCompositeChannel(writer,readerFifos,selectedActor,bindings); 
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

  public static void collapseMergeableMulticastActors(Application app,int startIndex,Bindings bindings){
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

	        CompositeFifo compositeFifo = FifoManagement.createCompositeChannel(writer,readerFifos,selectedActor,startIndex++,bindings); 
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
	        // remove the fifos
	        app.getFifos().remove(writer.getId());
	        for(Fifo dstFifo : readerFifos) {
	        	app.getFifos().remove(dstFifo.getId());
	        }
	        // remove the merged multicast actor from the map of actors
	        app.getActors().remove(multicastActor.getKey());
	        // add the new composite fifo into the app fifo map
	        app.getFifos().put(compositeFifo.getId(),compositeFifo);
	      }
	    }
  }
  
  public static void collapseMergeableMulticastActors(Application app,int startIndex){
	    // get all the multicast actors
	    Map<Integer,Actor> multicastActors = getMulticastActors(app);
	    
	    for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
	      Actor selectedActor = multicastActor.getValue();
	      collapseMergeableMulticastActor(app,selectedActor,startIndex++);
	    }
  }

  public static void collapseMergeableMulticastActor(Application app, Actor multicastActor, int index){
	  // returns the writer fifo
	  Fifo writer=null;
	  CompositeFifo compositeFifo = null;
	  if(multicastActor.isMergeMulticast() == true){
        // if the actor is mergeable, we remove it and replace it by a composite channel
        Vector<Fifo> inputFifos  = multicastActor.getInputFifos(); // it should be only one writer
        Vector<Fifo> outputFifos = multicastActor.getOutputFifos(); // it might be multiple readers, more that one
        writer = inputFifos.get(0);
        List<Fifo>  readerFifos = new ArrayList<Fifo>(outputFifos);

        compositeFifo = FifoManagement.createCompositeChannel(writer,readerFifos,multicastActor,index); 
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
        // remove the fifos
        app.getFifos().remove(writer.getId());
        for(Fifo dstFifo : readerFifos) {
        	app.getFifos().remove(dstFifo.getId());
        }
        // remove the merged multicast actor from the map of actors
        app.getActors().remove(multicastActor.getId());
        // add the new composite fifo into the app fifo map
        app.getFifos().put(compositeFifo.getId(),compositeFifo);
      }
  }
  
}
