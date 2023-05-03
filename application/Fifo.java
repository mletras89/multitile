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
     This class describes a communication channel implemented as a FIFO 
     buffer. Channels communicate actors in an application graph.
--------------------------------------------------------------------------
*/
package multitile.application;

import multitile.mapping.Bindings;
import multitile.Transfer;
import multitile.architecture.Memory;
import multitile.architecture.Architecture;
import multitile.architecture.ArchitectureManagement;
import java.util.*;

public class Fifo implements Buffer{
  private int             id;
  private String          name;
  private int             tokens;    // current number of tokens
  private int             initial_tokens; // the number of initial tokens
  private int             capacity;  // maximal number of tokens
  private int             tokenSize; // token size in bytes
  private int             consRate;
  private int             prodRate;
  private Queue<Transfer> timeProducedToken;  // each transfer transport a token, thus the due time is the produced token time
  
  private FIFO_MAPPING_TYPE mappingType;

  private Actor source; // source actor
  private Actor destination; // destination actor

  public int numberOfReadsTimeProduced;
  public int numberOfReads;

  //private Vector<Integer> memory_footprint;

  private boolean isRecurrence;

  public static enum FIFO_MAPPING_TYPE {
    SOURCE,
    DESTINATION,
    TILE_LOCAL_SOURCE,
    TILE_LOCAL_DESTINATION,
    GLOBAL
  }

  public Fifo(String name, int tokens, int capacity, int tokenSize,int consRate, int prodRate, Actor src, Actor dst){
    //this.id                          = id;
    this.id                          = FifoManagement.getFifoId();
    this.name                        = name;
    this.tokens                      = tokens;
    this.capacity                    = capacity;
    this.setTokenSize(tokenSize);
    this.initial_tokens              = tokens;
    this.setConsRate(consRate);
    this.setProdRate(prodRate);
    this.setSource(src);
    this.setDestination(dst);
    this.setNumberOfReadsTimeProduced(0);
    this.timeProducedToken = new LinkedList<>();
    this.numberOfReads = 0;
    //this.ReMapping = new LinkedList<>();
    //this.setNumberOfReadsReMapping(0);
    this.isRecurrence = false;
  }
  
  public Fifo(String name, int tokens, int capacity, int tokenSize,int consRate, int prodRate, Actor src, Actor dst,FIFO_MAPPING_TYPE mappingType){
    //this.id                          = id;
    this.id                          = FifoManagement.getFifoId();
    this.name                        = name;
    this.tokens                      = tokens;
    this.capacity                    = capacity;
    this.setTokenSize(tokenSize);
    this.setMappingType(mappingType);
    this.initial_tokens              = tokens;
    this.setConsRate(consRate);
    this.setProdRate(prodRate);
    this.setSource(src);
    this.setDestination(dst);
    this.setNumberOfReadsTimeProduced(0);
    this.timeProducedToken = new LinkedList<>();
    this.numberOfReads = 0;
    //this.ReMapping = new LinkedList<>();
    //this.setNumberOfReadsReMapping(0);
    this.isRecurrence = false;
  }

  public Fifo(String name, int tokens, int capacity, int tokenSize,int consRate, int prodRate){
  //  this.id                          = id;
    this.id                          = FifoManagement.getFifoId();
    this.name                        = name;
    this.tokens                      = tokens;
    this.capacity                    = capacity;
    this.setTokenSize(tokenSize);
    this.initial_tokens              = tokens;
    this.setConsRate(consRate);
    this.setProdRate(prodRate);

    this.setNumberOfReadsTimeProduced(0);
    this.timeProducedToken = new LinkedList<>();
    this.numberOfReads = 0;

    //this.ReMapping = new LinkedList<>();
    //this.setNumberOfReadsReMapping(0);
    this.isRecurrence = false;
  }

  public Fifo(String name, int tokens, int capacity, int tokenSize,int consRate, int prodRate,FIFO_MAPPING_TYPE mappingType){
    this.id                          = FifoManagement.getFifoId(); 
    this.name                        = name;
    this.tokens                      = tokens;
    this.capacity                    = capacity;
    this.setTokenSize(tokenSize);
    this.setMappingType(mappingType);
    this.initial_tokens              = tokens;
    this.setConsRate(consRate);
    this.setProdRate(prodRate);

    this.setNumberOfReadsTimeProduced(0);
    this.timeProducedToken = new LinkedList<>();
    this.numberOfReads = 0;

    //this.ReMapping = new LinkedList<>();
    //this.setNumberOfReadsReMapping(0);
    this.isRecurrence = false;
  }

  public Fifo(Fifo another){
    this.id                          = another.getId();
    this.name                        = another.getName();
    this.tokens                      = another.get_tokens();
    this.capacity                    = another.get_capacity();
    this.setTokenSize(another.getTokenSize());
    this.setMappingType(another.getMappingType());
    this.initial_tokens              = another.initial_tokens;
    this.setConsRate(another.getConsRate());
    this.setProdRate(another.getProdRate());

    this.setSource(new Actor(another.getSource()));
    this.setDestination(new Actor(another.getDestination()));

    this.timeProducedToken = new LinkedList<>();
    this.setNumberOfReadsTimeProduced(another.getNumberOfReadsTimeProduced());
    this.numberOfReads = 0;
    //this.setNumberOfReadsReMapping(another.getNumberOfReadsReMapping());
    //this.ReMapping = new LinkedList<>();
    this.setIsRecurrence(another.isFifoRecurrence());
  }

  
  public void insertTimeProducedToken(Transfer transfer) {
	    this.timeProducedToken.add(new Transfer(transfer));
  }  
  
  
  
  
  public void setIsRecurrence(boolean val){
    this.isRecurrence = val;
  }

  public boolean isFifoRecurrence(){
    return isRecurrence;
  }

  
  
  public Queue<Transfer> getTimeProducedToken(){
    return this.timeProducedToken;
  }

  public FIFO_MAPPING_TYPE getMappingType(){
    return this.mappingType;
  }

  public void setMappingType(FIFO_MAPPING_TYPE mappingType){
    this.mappingType = mappingType;
  }

  public int getInitialTokens(int initial_tokens){
    return this.initial_tokens;
  }

  public void setInitialTokens(int initial_tokens){
    this.initial_tokens = initial_tokens;
  }

  //public void setNumberOfReadsReMapping(int numberOfReads) {
  //  this.numberOfReadsReMapping = numberOfReads;
  //}

 //public int getNumberOfReadsReMapping(){
 //   return this.numberOfReadsReMapping;
 //}

  //public void insertReMapping(boolean value) {
  //  this.ReMapping.add(value);
  //}

  //public boolean peekReMapping(){
  //  return this.ReMapping.peek();
  //}


  public Transfer removeTimeProducedToken(){
    assert this.timeProducedToken.size() > 0;
    return this.timeProducedToken.remove();
  }

  /*public Transfer peekTimeProducedToken(){
	assert this.timeProducedToken.size() > 0;
    return this.timeProducedToken.peek();
  }*/

  public boolean equals(Fifo fifo){
    return this.getId()==fifo.getId() && this.getName().equals(fifo.getName());
  }

  // fifo to memory functions, used to write or read to memory
  
  public boolean canFifoWriteToMemory(Bindings bindings){
	Memory mapping = bindings.getFifoMemoryBindings().get(this.getId()).getTarget();
    return mapping.canPutDataInMemory(this.prodRate*this.tokenSize);
  }

  public boolean canFifoReadFromMemory(Bindings bindings){
	  Memory mapping = bindings.getFifoMemoryBindings().get(this.getId()).getTarget();
	  return mapping.canRemoveDataFromMemory(this.consRate*this.tokenSize);
  }

  public void fifoWriteToMemory(Transfer transfer,Bindings bindings){
	  Memory mapping = bindings.getFifoMemoryBindings().get(this.getId()).getTarget();
      mapping.writeDataInMemory(this.prodRate*this.tokenSize,transfer.getDue_time());
  }

  public void fifoReadFromMemory(Transfer transfer, Bindings bindings){
	  Memory mapping = bindings.getFifoMemoryBindings().get(this.getId()).getTarget();
	  mapping.readDataInMemory(this.consRate*this.tokenSize,transfer.getDue_time());  
  }

  public void fifoReadFromMemory(Transfer transfer,double time,Bindings bindings){
	  Memory mapping = bindings.getFifoMemoryBindings().get(this.getId()).getTarget();
	  mapping.readDataInMemory(this.consRate*this.tokenSize,time);  
  }

  
  // functions to update the count of tokens in the fifo
  
  public void fifoWrite(){
    this.set_tokens(this.get_tokens()+this.getProdRate()); 
    //assert (this.get_tokens()<= this.get_capacity()): "Error in writing!!!";
  }

  public void fifoRead(int idActorReader){
    assert idActorReader == this.getDestination().getId(): "Wrong reading attemp!!!";
    this.set_tokens(this.get_tokens() - this.getConsRate());
    assert (this.get_tokens()>=0) :  "Error in reading!!!";
  }

  // functions to check the guards of the fifo
  
  public boolean fifoCanBeWritten(){
    //System.out.println("Checking regular fifo");
    if(this.get_capacity() < this.get_tokens() + this.getProdRate())
      return false;
    return true;
  }

  public boolean fifoCanBeRead(int idWhoIsReading){
    assert idWhoIsReading == this.getDestination().getId(): "Wrong guard reading attemp!!!";
    if(this.get_tokens() - this.getConsRate() < 0)
      return false; 
    return true;
  }


  // functions to read the time of n produced tokens
 
  public double readTimeProducedToken(int n, int idWhoIsReading){
	assert idWhoIsReading == this.getDestination().getId(): "Error "+this.getName();
	  
	  	
    // this method reads n tokens from the fifo and returns the one with
    // the max delay
    List<Double> reads = new ArrayList<>();
    for(int i=0;i<n;i++){
      reads.add(this.readTimeProducedToken());
    }
    return Collections.max(reads);
  }

  public double readTimeProducedToken() {
    Transfer status;
    //System.out.println("FIFOS: "+this.getName());
    this.numberOfReadsTimeProduced++;
    //assert timeProducedToken.size() > 0: "Error here "+this.getName();
    
    if (timeProducedToken.size() > 0)
    {
    	status = this.timeProducedToken.remove();
    	return status.getDue_time();
    }
    return 0;
  }
  
 
  
  public boolean canFlushData() {
    return true;
  }
  
  public void increaseNumberOfReads() {
    this.numberOfReads++;
  }
  
  /*
  public boolean removeReMapping() {
    this.numberOfReadsReMapping++;
    //int currentNumberOfReads = this.numberOfReadsReMapping;
    boolean status = false;
	  
    status = this.ReMapping.remove();
    return status;
  }*/
  public void reset(Architecture architecture, Bindings bindings, Application application){
	  resetFifo(architecture,bindings, application);
  }
  
  public void resetFifo(Architecture architecture, Bindings bindings, Application application){
    this.tokens = this.initial_tokens;
    this.timeProducedToken.clear();
    // and put the initial tokens in the fifo produced at time 0.0
    for(int i=0; i < this.initial_tokens; i++){
        Transfer t = new Transfer(source,this);
        t.setDue_time(0.0);
        
        while(!this.canFifoWriteToMemory(bindings)){
        	// do the remap and return false
        	Memory reMappingMemory = ArchitectureManagement.getMemoryToBeRelocated(this,architecture,application,bindings);
        	ApplicationManagement.remapFifo(this, reMappingMemory,bindings);
        	System.out.println("Stuck here!");
        }
        this.fifoWriteToMemory(t,bindings);
        this.timeProducedToken.add(t); 
    }
    this.numberOfReads = 0;
    this.numberOfReadsTimeProduced = 0;
    //this.numberOfReadsReMapping = 0;
    //this.ReMapping.clear();
  }

  
  public void reset() {
	  resetFifo();
  }
  
  public void resetFifo(){
    this.tokens = this.initial_tokens;
    this.timeProducedToken.clear();
    // and put the initial tokens in the fifo produced at time 0.0
    for(int i=0; i < this.initial_tokens; i++){
        Transfer t = new Transfer(source,this);
        t.setDue_time(0.0);
        this.timeProducedToken.add(t); 
    }
    this.numberOfReads = 0;
    this.numberOfReadsTimeProduced = 0;
    //this.numberOfReadsReMapping = 0;
    //this.ReMapping.clear();
  }

  /*public void update_memory_footprint(){
    this.memory_footprint.add(tokens);
  }*/
  
  
  
  public String getName() {
    return this.name;
  }
  
  public int get_capacity(){
    return this.capacity;
  }

  public void set_capacity(int capacity){
    this.capacity = capacity;
  }

  public int get_tokens(){
    return this.tokens;
  }

  public void set_tokens(int tokens){
    this.tokens = tokens;
  }

  public int getTokenSize() {
    return tokenSize;
  }

  public void setTokenSize(int tokenSize) {
    this.tokenSize = tokenSize;
  }

  public int getConsRate() {
    return consRate;
  }

  public void setConsRate(int consRate) {
    this.consRate = consRate;
  }

  public int getProdRate() {
    return prodRate;
  }

  public void setProdRate(int prodRate) {
    this.prodRate = prodRate;
  }

  public int getId() {
    return this.id;
  }

  public boolean isCompositeChannel(){
      return false;
  }

  public Actor getSource() {
    return source;
  }

  public void setSource(Actor source) {
    this.source = source;
  }

  public Actor getDestination() {
    return destination;
  }

  public void setDestination(Actor destination) {
    this.destination = destination;
  }

/*  public Queue<Boolean> getReMapping() {
    return ReMapping;
  }

  public void setReMapping(Queue<Boolean> reMapping) {
    ReMapping = reMapping;
  }*/

  public int getNumberOfReadsTimeProduced() {
    return numberOfReadsTimeProduced;
  }

  public void setNumberOfReadsTimeProduced(int numberOfReadsTimeProduced) {
    this.numberOfReadsTimeProduced = numberOfReadsTimeProduced;
  }

}
