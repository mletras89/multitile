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

public class Bindings{
  // Key is the actor id
  // Value is the binding
	private HashMap<Integer,Binding> actorTileBindings;
	private HashMap<Integer,Binding> actorProcessorBindings;
	private HashMap<Integer,Binding> fifoMemoryBindings;
	
	public Bindings() {
		actorTileBindings = new HashMap<>();
		actorProcessorBindings = new HashMap<>();
		fifoMemoryBindings = new HashMap<>();
	}
	
	public HashMap<Integer,Binding> getActorTileBindings(){
		return actorTileBindings;
	}
	
	public HashMap<Integer,Binding> getActorProcessorBindings(){
		return actorProcessorBindings;
	}
	
	public HashMap<Integer,Binding> getFifoMemoryBindings(){
		return fifoMemoryBindings;
	}
}
