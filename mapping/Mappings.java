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
  @date   19 March 2023
  @version 1.1
  @ brief
     Mappings for all actor  ->  for all processor
     Mappings for all fifo   ->  for all memory
--------------------------------------------------------------------------
*/
package multitile.mapping;

import java.util.HashMap;
import java.util.Map;

import multitile.application.Application;
import multitile.architecture.Architecture;
import multitile.architecture.Memory;
import multitile.architecture.Processor;
import multitile.architecture.Tile;

public class Mappings{
  // Key is the actor id
  // Annidated map
  //    -> Key is processor ID
  //    -> Value is the property mapping class
  //<IdActor, Map <processorId,ExecutionTime>
  private HashMap<Integer,HashMap<Integer,Mapping<Processor>>> actorProcessorMappings;
  // Key is the actor id
  // Annidated map
  //    -> Key is tile id
  //    -> Value is the property mapping class
  //<IdActor, Map <tileId,ExecutionTime>
  private HashMap<Integer,HashMap<Integer,Mapping<Tile>>> actorTileMappings;
  // Key is the fifo id
  // Annidated map
  //    -> Key is memory ID
  //    -> Value is the property mapping class
  //<IdFifo, Map <tileId,ExecutionTime>
  private HashMap<Integer,HashMap<Integer,Mapping<Memory>>> fifoMemoryMappings;

  
  HashMap<Integer,HashMap<String,Integer>> discreteRuntime;
  
  public Mappings(){
	this.actorProcessorMappings = new HashMap<>();
	this.actorTileMappings = new HashMap<>();
	this.fifoMemoryMappings = new HashMap<>();
  }

  public HashMap<Integer,HashMap<Integer,Mapping<Processor>>> getActorProcessorMappings(){
	return actorProcessorMappings;
  }

  public HashMap<Integer,HashMap<Integer,Mapping<Tile>>> getActorTileMappings(){
	return actorTileMappings;
  }

  public HashMap<Integer,HashMap<Integer,Mapping<Memory>>> getFifoMemoryMappings(){
	return fifoMemoryMappings;
  }
  
  
  public HashMap<Integer,HashMap<String,Integer>>  getDiscreteRuntimeFromType() {
	  return discreteRuntime;
  }
  
  public void fillUsefulMaps() {
	  // key: actor
	  // value:
	  //	key: type
	  //	value: discrete runtime
	  discreteRuntime = new HashMap<>();
	  
	  for(Map.Entry<Integer, HashMap<Integer,Mapping<Processor>>> e :  actorProcessorMappings.entrySet()) {
		  HashMap<String,Integer> typeRuntime = new HashMap<>();
		  HashMap<Integer,Mapping<Processor>> mappingsToProcessor = e.getValue();
		  for(Map.Entry<Integer, Mapping<Processor>> m : mappingsToProcessor.entrySet()) {
			  int d = (int)m.getValue().getProperties().get("discrete-runtime");
			  typeRuntime.put(m.getValue().getTarget().getProcesorType(), d);
		  }
		  discreteRuntime.put(e.getKey(), typeRuntime);
	  }
	  
  }
  
  public double getExecTimeToProcMapping(String type, int actorId, Architecture architecture) {
	  
	  HashMap<Integer,Mapping<Processor>> actorMappings = actorProcessorMappings.get(actorId);
	  
	  for(Map.Entry<Integer, Mapping<Processor>> mp : actorMappings.entrySet()) {
		  int procId = mp.getKey();
		  Processor proc = architecture.getProcessor(procId);
		  if (proc.getProcesorType().compareTo(type) == 0)
		     return (double)mp.getValue().getProperties().get("runtime-discrete");
	  }
	  
	  return -1;
  }
  
  public void printMappings(Application application) {
	  System.out.println("actorProcessorMappings...");
	  for(Map.Entry<Integer, HashMap<Integer,Mapping<Processor>>> e :  actorProcessorMappings.entrySet()) {
		  System.out.println("Mappings of "+application.getActors().get(e.getKey()).getName() );
		  HashMap<Integer,Mapping<Processor>> mappingsToProcessor = e.getValue();
		  for(Map.Entry<Integer, Mapping<Processor>> m : mappingsToProcessor.entrySet()) {
			  System.out.println("\t To processor "+m.getValue().getTarget().getName()+" runtime "+m.getValue().getProperties().get("runtime")+" discrete runtime "+m.getValue().getProperties().get("discrete-runtime"));
		  }
	  }
	  System.out.println("fifoMemoryMappings...");
	  for(Map.Entry<Integer, HashMap<Integer,Mapping<Memory>>> e :  fifoMemoryMappings.entrySet()) {
		  System.out.println("Mappings of "+application.getFifos().get(e.getKey()).getName() );
		  HashMap<Integer,Mapping<Memory>> mappingsToMemory = e.getValue();
		  for(Map.Entry<Integer, Mapping<Memory>> m : mappingsToMemory.entrySet()) {
			  System.out.println("\t To memory "+m.getValue().getTarget().getName());
		  }
	  }
  }
  
}
