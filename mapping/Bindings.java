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
     Bindings for all actor  ->  to a single processor
     Bindings for all fifo   ->  to a single memory
--------------------------------------------------------------------------
*/

package multitile.mapping;

import java.util.HashMap;
import java.util.Map;

import multitile.application.Application;
import multitile.architecture.Memory;
import multitile.architecture.NoC;
import multitile.architecture.Processor;
import multitile.architecture.Tile;

public class Bindings{
  // Key is the actor id
  // Value is the binding
	private HashMap<Integer,Binding<Tile>> actorTileBindings;
	private HashMap<Integer,Binding<Processor>> actorProcessorBindings;
	private HashMap<Integer,Binding<Memory>> fifoMemoryBindings;
	private HashMap<Integer,Binding<NoC>> commTaskToNoC;
	
	public Bindings() {
		actorTileBindings = new HashMap<>();
		actorProcessorBindings = new HashMap<>();
		fifoMemoryBindings = new HashMap<>();
	}
	
	public HashMap<Integer,Binding<NoC>> getCommTaskToNoCBinding(){
		return commTaskToNoC;
	}
	
	public HashMap<Integer,Binding<Tile>> getActorTileBindings(){
		return actorTileBindings;
	}
	
	public HashMap<Integer,Binding<Processor>> getActorProcessorBindings(){
		return actorProcessorBindings;
	}
	
	public HashMap<Integer,Binding<Memory>> getFifoMemoryBindings(){
		return fifoMemoryBindings;
	}
	
	
	public void printBindings(Application application) {
		  System.out.println("actorProcessorBindings...");
		  for(Map.Entry<Integer, Binding<Processor>> e :  actorProcessorBindings.entrySet()) {
			  System.out.println("Binding of "+application.getActors().get(e.getKey()).getName()+" to prorcessor "+e.getValue().getTarget().getName()+" runtime "+e.getValue().getProperties().get("runtime")+" discrete runtime "+e.getValue().getProperties().get("discrete-runtime"));
		  }
		  System.out.println("fifoMemoryBindings...");
		  for(Map.Entry<Integer, Binding<Memory>> e :  fifoMemoryBindings.entrySet()) {
			  System.out.println("Binding of "+application.getFifos().get(e.getKey()).getName()+" to memory "+e.getValue().getTarget().getName() );
		  }
	  }
}
