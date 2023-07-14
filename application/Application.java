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

import multitile.architecture.Processor;
import multitile.architecture.Memory;
import multitile.architecture.NoC;
import multitile.architecture.Tile;
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import net.sf.opendse.model.Task;
import multitile.Transfer;
import multitile.application.Actor.ACTOR_TYPE;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;
import multitile.architecture.Architecture;

public class Application{
  private Map<Integer,Actor> actors;
  private Map<Integer,Fifo> fifos;
  
  // the key is the actor id and the value is the FIFO
  private Map<Integer,Fifo> transferWrites;
  private Map<Integer,Fifo> transferReads;
  
  public Application(){
    this.actors = new HashMap<>();
    this.fifos  = new HashMap<>();
  }
  public Application(Application appSpec){
		actors = new HashMap<>();
		fifos =  new HashMap<>();
		transferWrites = new HashMap<>();
		transferReads = new HashMap<>();
		
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

  
  public void fillTransferActions(Map<Integer, Integer> commsTime,Bindings bindings) {
	  transferWrites = new HashMap<>();
	  transferReads = new HashMap<>();
	  for(Map.Entry<Integer, Fifo> f: fifos.entrySet()) {
		  Actor actorWrite = new Actor("w:"+f.getValue().getName());
		  actorWrite.setType(ACTOR_TYPE.WRITE_ACTION);
		  Actor actorRead  = new Actor("r:"+f.getValue().getName());
		  actorRead.setType(ACTOR_TYPE.READ_ACTION);
		  
		  Binding<NoC> wBinding = new Binding<NoC>();
		  wBinding.getProperties().put("runtime-discrete", commsTime.get(f.getKey()));
		  
		  Binding<NoC> rBinding = new Binding<NoC>();
		  rBinding.getProperties().put("runtime-discrete", commsTime.get(f.getKey()));
		  
		  bindings.getCommTaskToNoCBinding().put(actorWrite.getId(), wBinding);
		  bindings.getCommTaskToNoCBinding().put(actorRead.getId(), rBinding);
		  
		  actors.put(actorRead.getId(), actorRead);
		  actors.put(actorWrite.getId(), actorWrite);
		  
		  transferWrites.put(actorWrite.getId(), f.getValue());
		  transferReads.put(actorRead.getId(), f.getValue());
		  
	  }
  }
  
  public void fillTokensAtState(HashMap<Integer,Integer> stateChannels, HashMap<Integer,Integer> nReads) {
	  for(Map.Entry<Integer,Integer> state : stateChannels.entrySet()) {
		  int fifoId = state.getKey();
		  int countTokens = state.getValue();
		  
		  fifos.get(fifoId).set_tokens(countTokens);
		  
		  for(int i = 0 ; i < countTokens; i++) {
			  // now insert the empty transfers
			  Actor src = fifos.get(fifoId).getSource();
			  Fifo f = fifos.get(fifoId);
			  Transfer t = new Transfer(src,f);
			  fifos.get(fifoId).insertTimeProducedToken(t);
		  }
		  fifos.get(state.getKey()).setNumberOfReads(nReads.get(fifoId));
		  
	  }
  }

  
  public Map<Integer, Fifo> getTransferWrites() {
		return transferWrites;
  }

  public Map<Integer, Fifo> getTransferReads() {
		return transferReads;
  }  
  
  public void resetApplication(){
    for(Map.Entry<Integer,Fifo> fifo : fifos.entrySet()){
      fifo.getValue().reset();
    }
  }

  public void resetApplication(Architecture architecture, Bindings bindings, Application application){
    for(Map.Entry<Integer,Fifo> fifo : fifos.entrySet()){
      fifo.getValue().reset(architecture, bindings, application);
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
  
  public void printActorsTilesState(Bindings bindings,Map<Integer, Tile> tiles){
	for(Map.Entry<Integer,Tile> t : tiles.entrySet()){
	  System.out.println("Tile "+t.getValue().getName());
	  for(Map.Entry<Integer,Actor> actorEntry : actors.entrySet()){
		Tile map = bindings.getActorTileBindings().get(actorEntry.getKey()).getTarget();
		if (map.getId() == t.getKey())
			System.out.println("\t Id "+actorEntry.getKey()+" Actor:"+actorEntry.getValue().getName()+" is multicast:"+actorEntry.getValue().isMulticastActor());
	  }
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

  public void printFifosMapping(Bindings bindings) {
    for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
      Memory memory = bindings.getFifoMemoryBindings().get(fifoEntry.getKey()).getTarget();
      System.out.println("Fifo:"+fifoEntry.getValue().getName()+" is composite?:"+fifoEntry.getValue().isCompositeChannel()+" bound to "+memory.getName());
    }  
  }



  public void printFifosState(){
    for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
    	if(!fifoEntry.getValue().isCompositeChannel()) {
    		System.out.println("Fifo: "+fifoEntry.getValue().getName()+" contains tokens: "+fifoEntry.getValue().get_tokens()+" capacity "+fifoEntry.getValue().get_capacity()+" source "+fifoEntry.getValue().getSource().getName()+" destination "+fifoEntry.getValue().getDestination().getName());
    	}else {
    		CompositeFifo cf = (CompositeFifo) fifoEntry.getValue();
    		for(Map.Entry<Integer, Fifo> f: cf.getReaders().entrySet() ) {
    			System.out.println("Fifo: "+fifoEntry.getValue().getName()+" contains tokens: "+f.getValue().get_tokens()+" source "+fifoEntry.getValue().getSource().getName()+" destination "+f.getValue().getDestination().getName());
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
	  
	  // passing as parameter the index of composite actors no bidings
	  public CompositeFifo createCompositeChannel(Fifo writer,List<Fifo> readerFifos, Actor multicastActor,int index){
		    // create a composite channel from a given list of fifos
		    // a composite actor has only one writer and multiple readers
		    //
		    // the capacity of the composite is the addition of the capacity of the writer and the max capacity of readers
		    int capacityWriter = writer.get_capacity();
		    ArrayList<Integer> capacitiesReader = new ArrayList<>();

		    for(Fifo fifo : readerFifos){
		      capacitiesReader.add( fifo.get_capacity());
		    }
		    
		    int capacityReader = Collections.max(capacitiesReader);
		    
		    // updating fifos capacities of readers
		    for(Fifo fifo : readerFifos){
		      fifo.set_capacity(capacityWriter+capacityReader);
		    }

		    CompositeFifo compositeFifo = new CompositeFifo("compositeFifo_"+index,writer.get_tokens(),capacityWriter+capacityReader,writer.getTokenSize(),writer.getConsRate(),writer.getProdRate(),writer.getSource(),readerFifos,multicastActor);
		    compositeFifo.setMappingType( writer.getMappingType() );
		    return compositeFifo;
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

	        compositeFifo = createCompositeChannel(writer,readerFifos,multicastActor,index); 
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
		
		public void setMulticastCapacity(int capacity) {
			for(Map.Entry<Integer, Fifo> f : fifos.entrySet()) {
				if (f.getValue().isCompositeChannel())
					f.getValue().set_capacity(capacity);
			}
		}
		
	    public double calculateMemoryFootprint() {
	        double memory_footprint = 0.0;
	        //System.err.println("=====================");
	        for (Map.Entry<Integer, Fifo> fifoEntry : fifos.entrySet()) {
	            //System.err.println("fifo "+fifoEntry.getValue().getName()+ " cap "+fifoEntry.getValue().get_capacity() + "mem F " + (fifoEntry.getValue().get_capacity() * fifoEntry.getValue().getTokenSize()));
	            memory_footprint += fifoEntry.getValue().get_capacity() * fifoEntry.getValue().getTokenSize();
	        }
	        //System.err.println("TOTAL MF "+memory_footprint);
	        //System.exit(1);
	        return memory_footprint;
	    }
	  
}


