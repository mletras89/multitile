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
  @date   14 November 2022
  @version 1.1
  @ brief
     This class describes a composite communicate channel.
     This Channel communicate actors in an application graph as well as 
     regular Fifo class, however it allows not destructive reads of tokens
--------------------------------------------------------------------------
*/

package multitile.application;

import multitile.Transfer;
import multitile.architecture.Architecture;
import multitile.architecture.Memory;
import multitile.mapping.Bindings;

import java.util.*;

public class CompositeFifo extends Fifo {
  private Map<Integer,Fifo> readers; // the key is the id of the actor reading the fifo
  private List<Actor> destinations;
  private Actor multicastActor;
  private int readsMRB;
  
  public CompositeFifo(String name, int tokens, int capacity, int tokenSize,int consRate, int prodRate, Actor src, List<Fifo> destinationFifos,Actor multicastActor){
    super(name,tokens,capacity,tokenSize,consRate,prodRate);
    this.setSource(src);
    this.setDestinations(destinationFifos);
    this.setMulticastActor(multicastActor);
    readsMRB = 0;
  }

  public void setDestinations(List<Fifo> destinationFifos){
    this.readers = new HashMap<>();
    this.destinations = new ArrayList<>();

    for(Fifo reader : destinationFifos){
      this.readers.put(reader.getDestination().getId(),reader);
      this.destinations.add(reader.getDestination());
    }
  }

  public List<Actor> getDestinations(){
    return this.destinations;
  }
  
  @Override
  public void reset(){
	    this.resetFifo();
	    for(Map.Entry<Integer,Fifo> fifo : this.readers.entrySet()) {
	    	fifo.getValue().resetFifo();
	    }
	    readsMRB = 0;
  }
  
  @Override
  public void reset(Architecture architecture, Bindings bindings, Application application){
	  resetFifo(architecture,bindings, application);
	  for(Map.Entry<Integer, Fifo> fifo: this.readers.entrySet()) {
		  fifo.getValue().resetFifo(architecture,bindings, application);
	  }
	  readsMRB = 0;
  }
  
  @Override
  public Actor getDestination() {
	  assert this.destinations.size() > 0: "ERROR HERE!";
	  return destinations.get(0);
  }
  
  @Override
  public void fifoWrite(){
	
	this.set_tokens(this.get_tokens()+this.getProdRate());  
	assert this.get_tokens() <= this.get_capacity()  : "Error in writing composite fifo!!!";
	/*
    for(Map.Entry<Integer,Fifo> fifo : this.readers.entrySet()) {
      int new_tokens = fifo.getValue().get_tokens()+fifo.getValue().getProdRate();
      fifo.getValue().set_tokens(new_tokens);
      assert (fifo.getValue().get_tokens()<=fifo.getValue().get_capacity()): "Error in writing composite fifo!!!";
    }*/
  }
  @Override
  public boolean canFifoReadFromMemory(Bindings bindings){
	  Memory mapping = bindings.getFifoMemoryBindings().get(this.getId()).getTarget();
      return mapping.canRemoveDataFromMemory(this.getConsRate()*this.getTokenSize());
  }
  @Override
  public void fifoReadFromMemory(Transfer transfer,Bindings bindings){
    this.numberOfReads++;
    if(this.canFlushData()){
    	Memory mapping = bindings.getFifoMemoryBindings().get(this.getId()).getTarget();
    	mapping.readDataInMemory(this.getConsRate()*this.getTokenSize(),transfer.getDue_time());
    }
  }
  
  @Override
  public void fifoRead(int idActorReader){
	
	Fifo fifo = readers.get(idActorReader);
    int consTokens = fifo.getConsRate();
    
    for(int i=0; i < consTokens; i++) {
    	this.readsMRB++;
    	if(this.readsMRB % readers.size() == 0) {
    		this.set_tokens(this.get_tokens() - 1);
    		assert this.get_tokens() >= 0 :  "Error reading composite Fifo!!!"; 
        }
    }
    /*Fifo fifo = readers.get(idActorReader);
    int new_tokens = fifo.get_tokens() - fifo.getConsRate();
    fifo.set_tokens(new_tokens);
    assert (fifo.get_tokens()>=0) :  "Error reading composite Fifo!!!";*/
  }
  
  
  @Override
  public boolean canFlushData(){
    if(this.numberOfReads % readers.size() == 0)
      return true;
    return false; 
  }
  
  @Override
  public boolean fifoCanBeRead(int idWhoIsReading){
    // first get the FIFO
    Fifo fifo = readers.get(idWhoIsReading);
    /*if(fifo.get_tokens() - fifo.getConsRate() < 0)
      return false; 
    return true;*/
    if (this.get_tokens() - fifo.getConsRate() < 0)
    	return false;
    return true;
    		
  }
  
  @Override
  public boolean fifoCanBeWritten(){
    //System.out.println("Checking composite fifo");
	if (this.get_capacity() < this.get_tokens()+this.getProdRate())
		return false;
	return true;
	/*
    for(Map.Entry<Integer,Fifo> reader : readers.entrySet()){
      if(reader.getValue().get_capacity() < reader.getValue().get_tokens() + reader.getValue().getProdRate())
        return false;
    }
    return true;*/
  }
  
  @Override
  public double readTimeProducedToken(int n, int idWhoIsReading){
	  //Fifo fifo = readers.get(idWhoIsReading);
	  //return fifo.readTimeProducedToken(n,idWhoIsReading);
	  
	  List<Double> reads = new ArrayList<>();
	  for(int i=0;i<n;i++){
		  reads.add(this.readTimeProducedToken());
	  }
	  return Collections.max(reads);
	}
  
  @Override
  public double readTimeProducedToken() {
    Transfer status;
    //System.out.println("FIFO: "+this.getName()+" capacity "+this.get_capacity());
    this.numberOfReadsTimeProduced++;
    int currentNumberOfReads = this.numberOfReadsTimeProduced;
	  
    if (currentNumberOfReads % readers.size()==0)
      status = this.getTimeProducedToken().remove();
    else 
      status = this.getTimeProducedToken().peek(); //  .peekTimeProducedToken(); 
    if (status == null)
    		return 0;
	assert status != null : "ERROR: FIFO"+this.getName()+" actor";
    return status.getDue_time();
  }
  @Override
  public boolean isCompositeChannel(){
    return true;
  }

  public Actor getMulticastActor(){
    return this.multicastActor;
  }

  public void setMulticastActor(Actor multicastActor){
    this.multicastActor = multicastActor;
  }
}
