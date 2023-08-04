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
     Actor that can be mapped to any processor which is part of an application
--------------------------------------------------------------------------
*/
package multitile.application;

import java.util.*;

import multitile.Action;

public class Actor{
  private int id;
  private String name;
  private int priority;    
  
  private int inputs;
  private int outputs;
  
  private Vector<Fifo> inputFifos;
  private Vector<Fifo> outputFifos;

  private ACTOR_TYPE type;
  private boolean mergeMulticast = false;

  public static enum ACTOR_TYPE {
      ACTOR,
      MULTICAST,
      WRITE_COMMUNICATION_TASK,
      READ_COMMUNICATION_TASK
    }
    
  public Actor(
               String name,
               int priority,
               int inputs, 
               int outputs){
    this.setId(ActorManagement.getActorId());
    this.setName(name);
    this.setPriority(priority);
    this.setInputs(inputs);
    this.setOutputs(outputs);
    this.setType(ACTOR_TYPE.ACTOR);
    this.inputFifos  = new Vector<Fifo>();   
    this.outputFifos = new Vector<Fifo>();    
  }
  
  public Actor(Actor another){
    this.setId(another.getId());
    this.setName(another.getName());
    this.setPriority(another.getPriority());
    this.setInputs(another.getInputs());
    this.setOutputs(another.getOutputs());
    this.setType(another.getType());
    this.inputFifos  = new Vector<Fifo>();   
    this.outputFifos = new Vector<Fifo>();
  }
    
  public Actor(String name){
    this.setId(ActorManagement.getActorId());
    this.setName(name);
    this.inputFifos  = new Vector<Fifo>();   
    this.outputFifos = new Vector<Fifo>();   
    this.setType(ACTOR_TYPE.ACTOR);
  }

  public boolean equals(Actor actor){
    return this.getId() == actor.getId() && this.getName().equals(actor.getName());
  }

  public boolean isMulticastActor(){
    if (this.getType() == ACTOR_TYPE.MULTICAST)
      return true;
    return false;
  }

  // method for checking if an actor can FIRE
  public boolean canFire(Map<Integer,Fifo> fifos){
    //System.out.println("Can fire "+this.name+" ?");

    for(Fifo fifo : this.outputFifos){
      Fifo selectedFifo = fifos.get(fifo.getId());
      //System.out.println("Checking fifo "+selectedFifo.getName()+" ?");
      if (!selectedFifo.fifoCanBeWritten())
        return false;
    }
    
    for(Fifo fifo : this.inputFifos){
      Fifo selectedFifo = fifos.get(fifo.getId());
      if(!selectedFifo.fifoCanBeRead(this.getId()))
        return false; 
    }
    return true;
  }
  
  //method for checking if an actor can FIRE
  public boolean canFire(Map<Integer,Fifo> fifos,Action action){
	  assert action.getActor().getId() == this.getId(): "SOMETHING WRONG, ids must be the same";

	  if (!action.isSplitNoWrites()){
		  //System.out.println("Checking reads of "+action.getActor().getName());
		  for(Fifo fifo : this.outputFifos){
			  Fifo selectedFifo = fifos.get(fifo.getId());
			  //System.out.println("Checking fifo "+selectedFifo.getName()+" ?");
			  if (!selectedFifo.fifoCanBeWritten())
				  return false;
		  }
	  }
	  if(!action.isSplitNoReads()) {
		  //System.out.println("Checking writes of "+action.getActor().getName());
		  for(Fifo fifo : this.inputFifos){
			  Fifo selectedFifo = fifos.get(fifo.getId());
			  if(!selectedFifo.fifoCanBeRead(this.getId()))
				  return false; 
		  }
	  }
	  return true;
 }
  
  // method that fires the actor
  public boolean fire(Map<Integer,Fifo> fifos){
    //System.out.println("Firing actor "+this.name);

    for(Fifo fifo: inputFifos){
      fifos.get(fifo.getId()).fifoRead(this.getId());
    }

    for(Fifo fifo : outputFifos){
      fifos.get(fifo.getId()).fifoWrite();
    }                                         
    //System.out.println("Firing done!");
    return true;
  }

  	// method that fires the actor
  	public boolean fire(Map<Integer,Fifo> fifos,Action action){
    //System.out.println("Firing actor "+this.name);
  		assert action.getActor().getId() == this.getId(): "SOMETHING WRONG, ids must be the same";
  		
  		if (!action.isSplitNoReads()){
  			for(Fifo fifo: inputFifos){
  				fifos.get(fifo.getId()).fifoRead(this.getId());
  			}
  		}
  		
  		if (!action.isSplitNoWrites()) {
  			for(Fifo fifo : outputFifos){
  				fifos.get(fifo.getId()).fifoWrite();
  			}                                   
  		}
    //System.out.println("Firing done!");
    return true;
  }
  
  
  public int getInputs() {
    return inputs;
  }
  
  public void setInputs(int inputs) {
    this.inputs = inputs;
  }
  
  public int getOutputs() {
    return outputs;
  }
  
  public void setOutputs(int outputs) {
    this.outputs = outputs;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public int getPriority() {
    return priority;
  }
  
  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int getId() {
    return id;
  }
  
  public void setId(int id) {
    this.id = id;
  }
  
  public Vector<Fifo> getInputFifos(){
    return this.inputFifos;
  }
  public Vector<Fifo> getOutputFifos(){
    return this.outputFifos;
  }

  public void setInputFifos(Vector<Fifo> inputs){
    this.inputFifos =  inputs;
  }
  public void setOutputFifos(Vector<Fifo> outputs){
    this.outputFifos = outputs;
  }

  public ACTOR_TYPE getType() {
    return type;
  }
  
  public void setType(ACTOR_TYPE type) {
    this.type = type;
  }

  public int getNInputs() {
    return this.inputFifos.size();
  }

  public void removeInputFifo(int fifoId){
    int indexRemove=0;
    for(int i=0; i<this.getInputFifos().size();i++){
      if(this.getInputFifos().get(i).getId() == fifoId)
        indexRemove = i;
    }
    this.getInputFifos().remove(indexRemove);
  }
  
  public void removeOutputFifo(int fifoId){
    int indexRemove=0;
    for(int i=0;i<this.getOutputFifos().size();i++){
      if(this.getOutputFifos().get(i).getId() == fifoId)
        indexRemove = i;
    }
    this.getOutputFifos().remove(indexRemove);
  }

  public boolean isMergeMulticast(){
    return this.mergeMulticast;
  }
  
  public void setMergeMulticast(boolean mergeMulticast){
    this.mergeMulticast = mergeMulticast;
  }

}
