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
  @date   16 November 2022
  @version 1.1
  @ brief
        Class application that encapsulates a set of actors communicated via 
	a set of fifos
--------------------------------------------------------------------------
*/
package multitile.application;

import java.util.*;

import integration.MyEntry;
import multitile.architecture.Processor;
import multitile.architecture.Tile;
import multitile.mapping.Bindings;
import net.sf.opendse.model.Task;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;
import multitile.architecture.Architecture;

public class Application{
  private Map<Integer,Actor> actors;
  private Map<Integer,Fifo> fifos;

  public Application(){
    this.actors = new HashMap<>();
    this.fifos  = new HashMap<>();
  }
  public Application(Application appSpec){
		actors = new HashMap<>();
		fifos =  new HashMap<>();
		for(Map.Entry<Integer, Actor> actor : appSpec.getActors().entrySet()) {
			Actor clonedActor = new Actor(actor.getValue());
			actors.put(clonedActor.getId(), clonedActor);
		}
		// cloning the fifos
		for(Map.Entry<Integer, Fifo> fifo: appSpec.getFifos().entrySet()) {
			Fifo clonedFifo = new Fifo(fifo.getValue());
		        fifos.put(clonedFifo.getId(), clonedFifo);
		}
  }


  public void resetApplication(){
    for(Map.Entry<Integer,Fifo> fifo : fifos.entrySet()){
      fifo.getValue().resetFifo();
    }
  }

  public void resetApplication(Architecture architecture, Bindings bindings, Application application){
    for(Map.Entry<Integer,Fifo> fifo : fifos.entrySet()){
      fifo.getValue().resetFifo(architecture, bindings, application);
    }
  }

  public void setActors(Map<Integer,Actor> actorsN){
    actors = actorsN;
  }

  public void setActorsFromList(List<Actor> actorsList){
    actors = new HashMap<>();
    for(Actor actor:actorsList){
      actors.put(actor.getId(),actor);
    }
  }

  public void setFifos(Map<Integer,Fifo> fifosN){
    fifos = fifosN;
  }

  public Map<Integer,Fifo> getFifos(){
    return fifos;
  }

  public Map<Integer,Actor> getActors(){
    return actors;
  }

  public List<Actor> getListActors(){
    List<Actor> listActors = new ArrayList<>();
//   System.out.println("Getting list ");
    for(Map.Entry<Integer,Actor> actor : actors.entrySet() ){
//      System.out.println("Adding: "+actor.getValue().getName());
      listActors.add(actor.getValue());
    }
//    System.out.println("done Getting list ");
    return listActors;
  }

  public void printActors(){
	for(Map.Entry<Integer,Actor> actorEntry : actors.entrySet()){   
	  System.out.println("Actor:"+actorEntry.getValue().getName()+" is multicast:"+actorEntry.getValue().isMulticastActor()+" is mergeable: "+actorEntry.getValue().isMergeMulticast());
	}
  }

  public void printActorsState(Bindings bindings){
		for(Map.Entry<Integer,Actor> actorEntry : actors.entrySet()){   
			Processor map = bindings.getActorProcessorBindings().get(actorEntry.getValue().getId()).getTarget();
			System.out.println("Actor:"+actorEntry.getValue().getName()+" is multicast:"+actorEntry.getValue().isMulticastActor()+" is mergeable: "+actorEntry.getValue().isMergeMulticast()+" mapped to "+map.getName());
		}
	  }
  
  public void printActorsTilesState(Bindings bindings){
	for(Map.Entry<Integer,Actor> actorEntry : actors.entrySet()){   
			Tile map = bindings.getActorTileBindings().get(actorEntry.getKey()).getTarget();
			System.out.println("Id "+actorEntry.getKey()+" Actor:"+actorEntry.getValue().getName()+" is multicast:"+actorEntry.getValue().isMulticastActor()+" is mergeable: "+actorEntry.getValue().isMergeMulticast()+" mapped to tile "+map.getName());
	}
  }
  
  public void printFifos(){
	for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
	  System.out.println("Fifo:"+fifoEntry.getValue().getName()+" is composite?:"+fifoEntry.getValue().isCompositeChannel());
	}
  }
  
  public void printFifosMapping() {
	for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
		  System.out.println("Fifo:"+fifoEntry.getValue().getName()+" is composite?:"+fifoEntry.getValue().isCompositeChannel()+" mapping type "+fifoEntry.getValue().getMappingType());
	}  
  }
  
  public void printFifosState(){
    for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
    	if(!fifoEntry.getValue().isCompositeChannel()) {
    		System.out.println("Fifo: "+fifoEntry.getValue().getName()+" contains tokens: "+fifoEntry.getValue().get_tokens()+" source "+fifoEntry.getValue().getSource().getName()+" destination "+fifoEntry.getValue().getDestination().getName());
    	}else {
    		CompositeFifo cf = (CompositeFifo) fifoEntry.getValue();
    		for(Actor a : cf.getDestinations()) {
    			System.out.println("Fifo: "+fifoEntry.getValue().getName()+" contains tokens: "+fifoEntry.getValue().get_tokens()+" source "+fifoEntry.getValue().getSource().getName()+" destination "+a.getName());
    		}
    	}
    	System.out.println("\t"+fifoEntry.getValue().getTimeProducedToken());
    }
  }

	public void setFifosToActors() {
		for(Map.Entry<Integer, Actor> actor : actors.entrySet()) {
			Vector<Fifo> inputs= new Vector<Fifo>();
			Vector<Fifo> outputs = new Vector<Fifo>();
			for(Map.Entry<Integer, Fifo> fifo : fifos.entrySet()) {
				if (fifo.getValue().getSource().equals(actor.getValue())){
					outputs.add(fifo.getValue());
				}
				if (fifo.getValue().getDestination().equals(actor.getValue())) {
					inputs.add(fifo.getValue());
				}
			}
			actors.get(actor.getKey()).setInputFifos(inputs);
			actors.get(actor.getKey()).setOutputFifos(outputs);
		}
	}
	
	
	public void setAllFifosToSource() {
		for(Map.Entry<Integer, Fifo> fifo: fifos.entrySet()) {
			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.SOURCE);
		}
	}
	
	public void setAllFifosToDestination() {
		for(Map.Entry<Integer, Fifo> fifo: fifos.entrySet()) {
			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.DESTINATION);
		}
	}
	
	public void setAllFifosToTileLocalMemory(FIFO_MAPPING_TYPE type) {
		for(Map.Entry<Integer, Fifo> fifo: fifos.entrySet()) {
			fifo.getValue().setMappingType(type);
		}
	}
	
	public void setAllFifosToGlobalMemory() {
		for(Map.Entry<Integer, Fifo> fifo: fifos.entrySet()) {
			fifo.getValue().setMappingType(FIFO_MAPPING_TYPE.GLOBAL);
		}
	}
	
	public void setFifosToDecision(ArrayList<Integer> decision) {
		//System.out.println("Decision size "+decision.size());
		//System.out.println("number fifos "+app.getFifos().size());
		assert fifos.size() == decision.size() : "SOMETHING WRONG SIZE OF fifos AND decision MUST BE THE SAME";
		int counter = 0;
        for(Map.Entry<Integer, Fifo> fifo : fifos.entrySet()) {
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
	
	public Map<Integer,Actor> getMulticastActors(){
	    //System.out.println("getMulticastActors");
	    Map<Integer,Actor> multicastActors = new HashMap<>();
	    for(Map.Entry<Integer,Actor> actor : actors.entrySet()){
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
	  public void setAllMulticastActorsAsMergeable(){
	    //System.out.println("setAllMulticastActorsAsMergeable");
	    // get all the multicast actors
	    Map<Integer,Actor> multicastActors = getMulticastActors();
	    
	    for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
	      Actor selectedActor = multicastActor.getValue();
	      //System.out.println("Setting: "+selectedActor.getName());
	      actors.get(selectedActor.getId()).setMergeMulticast(true);
	    }
	  }
	  
	  public void setAllMulticastActorsAsNotMergeable(){
		    //System.out.println("setAllMulticastActorsAsMergeable");
		    // get all the multicast actors
		    Map<Integer,Actor> multicastActors = getMulticastActors();
		    
		    for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
		      Actor selectedActor = multicastActor.getValue();
		      //System.out.println("Setting: "+selectedActor.getName());
		      actors.get(selectedActor.getId()).setMergeMulticast(false);
		    }
	  }
	  
	  // this method selectively set multicast actors as mergeable
	  public void setMulticastActorsAsMergeable(ArrayList<Boolean> mergeable) {
		// get all the multicast actors
		Map<Integer,Actor> multicastActors = getMulticastActors();
		assert multicastActors.size() == mergeable.size() : "SOMETHING WRONG SIZE OF multicastActors AND mergeable MUST BE THE SAME";
		int count = 0;
		for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
		      Actor selectedActor = multicastActor.getValue();
		      actors.get(selectedActor.getId()).setMergeMulticast(mergeable.get(count));
		      count++;
		}
	  }

	  public MyEntry<Fifo, CompositeFifo> collapseMergeableMulticastActorDSE(Actor multicastActor, int index){
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
	        actors.get(idWriterActor).removeOutputFifo(writer.getId());
	        // connecting the input of the composite fifo
	        actors.get(idWriterActor).getOutputFifos().add(compositeFifo);

	        // now connect the readers to the composite fifo
	        for(Fifo dstFifo : readerFifos){
	          int idReaderActor = dstFifo.getDestination().getId();
	          actors.get(idReaderActor).removeInputFifo(dstFifo.getId());
	          // connectinf the outputs of the composite fifo
	          actors.get(idReaderActor).getInputFifos().add(compositeFifo);
	        }
	        // remove the fifos
	        fifos.remove(writer.getId());
	        for(Fifo dstFifo : readerFifos) {
	        	fifos.remove(dstFifo.getId());
	        }
	        // remove the merged multicast actor from the map of actors
	        actors.remove(multicastActor.getId());
	        // add the new composite fifo into the app fifo map
	        fifos.put(compositeFifo.getId(),compositeFifo);
	      }
		  //return writer;
		  MyEntry<Fifo, CompositeFifo> result = new MyEntry<Fifo,CompositeFifo>(writer,compositeFifo);
		  return result;
	  }
	  
		public void collapseMergeableMulticastActorsDSE(int startIndex,HashMap<String,Task> mapOfFifos){
		    // get all the multicast actors
		    Map<Integer,Actor> multicastActors = getMulticastActors();
		    
		    for(Map.Entry<Integer,Actor> multicastActor : multicastActors.entrySet()){
		      Actor selectedActor = multicastActor.getValue();
		      if(selectedActor.isMergeMulticast() == true){
			      MyEntry<Fifo, CompositeFifo> result = collapseMergeableMulticastActorDSE(selectedActor,startIndex++);
			      Fifo writer = result.getKey();
			      CompositeFifo cf = result.getValue();
			      assert cf != null: "THIS MUST NOT HAPPEN!";
			      assert writer !=null : "THIS MUST NOT HAPPEN!";
			      Task r = mapOfFifos.get(writer.getName());
			      mapOfFifos.put(cf.getName(), r);
		      }
		    }
		}
	  
}


