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
import multitile.architecture.Memory;
import java.util.*;

public class CompositeFifo extends Fifo implements Buffer{
  private Map<Integer,Fifo> readers; // the key is the id of the actor reading the fifo
  private List<Actor> destinations;
  private Actor multicastActor;

  public CompositeFifo(String name, int tokens, int capacity, int tokenSize,Memory mapping,int consRate, int prodRate, Actor src, List<Fifo> destinationFifos,Actor multicastActor){
    super(name,tokens,capacity,tokenSize,mapping,consRate,prodRate);
    this.setSource(src);
    this.setDestinations(destinationFifos);
    this.setMulticastActor(multicastActor);
  }

//  public CompositeFifo(CompositeFifo another){
//    this.id                          = another.getId();
//    this.name                        = another.getName();
//    this.tokens                      = another.get_tokens();
//    this.capacity                    = another.get_capacity();
//    this.setTokenSize(another.getTokenSize());
//    this.setMapping(another.getMapping());
//    this.setInitialTokens(another.getInitialTokens());
//    this.setConsRate(another.getConsRate());
//    this.setProdRate(another.getProdRate());
//
//    this.setSource(another.getSource());
//    this.setDestinations(another.getDestinations());
//  }

  public void setDestinations(List<Fifo> destinationFifos){
    this.readers = new HashMap<>();
    this.destinations = new ArrayList<>();

    for(Fifo reader : destinationFifos){
      this.readers.put(reader.getDestination().getId(),reader);
      this.destinations.add(reader.getDestination());
    }
  }

//  public List<Actor> getDestinations(){
//    return this.destinations;
//  }

  public boolean removeReMapping(){
    this.setNumberOfReadsReMapping(this.getNumberOfReadsReMapping()+1);
    int currentNumberOfReads = this.getNumberOfReadsReMapping();
    boolean status = false;
	  
    if(currentNumberOfReads % readers.size() == 0)
      status =  this.removeReMapping();
    else
      status = this.peekReMapping();
	  
    return status;
  }

  public void fifoWrite(){
    for(Map.Entry<Integer,Fifo> fifo : this.readers.entrySet()) {
      int new_tokens = fifo.getValue().get_tokens()+fifo.getValue().getProdRate();
      fifo.getValue().set_tokens(new_tokens);
      assert (fifo.getValue().get_tokens()<=fifo.getValue().get_capacity()): "Error in writing composite fifo!!!";
    }
  }

  public boolean canFifoReadFromMemory(){
    return this.getMapping().canRemoveDataFromMemory(this.getConsRate()*this.getTokenSize());
  }

  public void fifoReadFromMemory(Transfer transfer){
    this.numberOfReads++;
    if(this.canFlushData()){
      this.getMapping().readDataInMemory(this.getConsRate()*this.getTokenSize(),transfer.getDue_time());
    }
  }

  public void fifoRead(int idActorReader){
    Fifo fifo = readers.get(idActorReader);
    int new_tokens = fifo.get_tokens() - fifo.getConsRate();
    fifo.set_tokens(new_tokens);
    assert (fifo.get_tokens()>=0) :  "Error reading composite Fifo!!!";
  }

  public boolean canFlushData(){
    if(this.numberOfReads % readers.size() == 0)
      return true;
    return false; 
  }

  public boolean fifoCanBeRead(int idWhoIsReading){
    // first get the FIFO
    Fifo fifo = readers.get(idWhoIsReading);
    if(fifo.get_tokens() - fifo.getConsRate() < 0)
      return false; 
    return true;
  }

  public boolean fifoCanBeWritten(){
    //System.out.println("Checking composite fifo");
    for(Map.Entry<Integer,Fifo> reader : readers.entrySet()){
      if(reader.getValue().get_capacity() < reader.getValue().get_tokens() + reader.getValue().getProdRate())
        return false;
    }
    return true;
  }

  public double readTimeProducedToken() {
    Transfer status;
    //System.out.println("FIFO: "+this.getName());
    this.numberOfReadsTimeProduced++;
    int currentNumberOfReads = this.numberOfReadsTimeProduced;
	  
    if (currentNumberOfReads % readers.size()==0)
      status = this.removeTimeProducedToken();
    else 
      status = this.peekTimeProducedToken(); 
	  
    return status.getDue_time();
  }

  public boolean isCompositeChannel(){
    return true;
  }

  public Actor getMulticastActor(){
    return this.multicastActor;
  }

  public void setMulticastActor(Actor multicastActor){
    this.multicastActor = multicastActor;
  }

//  public void setReaders(List<Fifo> readers){
//    this.readers = readers;
//  }
//
//  public List<Fifo> getReaders(){
//    return this.readers;
//  }

}
