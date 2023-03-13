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
     This class describes a Memory element in the architecture. There exists
     three types of memories:
        - LOCAL_MEM,
        - TILE_LOCAL_MEM
        - GLOBAL_MEM
--------------------------------------------------------------------------
*/
package multitile.architecture;

import multitile.Transfer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

public class Memory{
  private int id;
  private String name;
  private double capacity;
  private Map<Double,Double> memoryUtilization = new TreeMap<Double, Double>();
  private MEMORY_TYPE type;
  private Processor embeddedToProcessor;

  public static enum MEMORY_TYPE {
    LOCAL_MEM,
    TILE_LOCAL_MEM,
    GLOBAL_MEM
  }

  // Possible constructors

  // initializing empty memory
  public Memory() {
    this.setName("");
    this.setId(ArchitectureManagement.getMemoryId());
    this.resetMemoryUtilization();
    // assume infinite size of memories if not specificed
    this.setCapacity(Double.POSITIVE_INFINITY);
  }
   // cloning memory
  public Memory(Memory other) {
    this.setName(other.getName());
    this.setId(other.getId());
    this.resetMemoryUtilization();
    this.setCapacity(other.getCapacity());
    this.setType(other.getType());
  }
  // creating memory from given parameters
  public Memory(String name, double capacity){
    this.name = name;
    this.id       = ArchitectureManagement.getMemoryId();
    this.resetMemoryUtilization();
    this.capacity = capacity;
  }

  // creating memory from given parameters
  public Memory(String name){
    this.name = name;
    this.id       = ArchitectureManagement.getMemoryId();
    this.resetMemoryUtilization();
    this.capacity = Double.POSITIVE_INFINITY;
  }
  
  public boolean equals(Memory memory){
    return this.getId() == memory.getId() && this.getName().equals(memory.getName());
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public MEMORY_TYPE getType(){
    return this.type;
  }

  public void setType(MEMORY_TYPE type){
    this.type = type;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public double getCapacity() {
    return capacity;
  }
  
  public void setCapacity(double capacity) {
    this.capacity = capacity;
  }

  public void resetMemoryUtilization() {
    // KEY is when and Value is the current utilization
    memoryUtilization.clear();
    this.memoryUtilization.put(0.0, 0.0);
  }

  public Map<Double,Double> getMemoryUtilization() {
    return this.memoryUtilization;
  }

  public Processor getEmbeddedToProcessor(){
    return this.embeddedToProcessor;
  }
 
  public void setEmbeddedToProcessor(Processor embeddedToProcessor){
    this.embeddedToProcessor = embeddedToProcessor;
  }

  // methods for memory managing
  public double getUtilization(double endTime){
    List<Double> listKeys = new ArrayList<>(memoryUtilization.keySet());
    Collections.sort(listKeys);
    double maxUtilization = endTime * capacity;

    double util = 0;
    for (int i=0;i<listKeys.size()-1;i++) {
      util += (listKeys.get(i+1) - listKeys.get(i)) * memoryUtilization.get(listKeys.get(i));
    }
//    System.out.println("maxUtilization "+maxUtilization);
//    System.out.println("util "+util);
    return 1 - (maxUtilization - util)/maxUtilization;
  }

  public void processTransfer(Transfer transfer,double when){
    if(transfer.getType() == Transfer.TRANSFER_TYPE.READ){
      readDataInMemory(transfer.getBytes(),when);
    }
    if(transfer.getType() == Transfer.TRANSFER_TYPE.WRITE){
      writeDataInMemory(transfer.getBytes(),when);
    }
  }

  public void writeDataInMemory(int amountBytes, double when) {
    List<Double> listKeys = new ArrayList<>(memoryUtilization.keySet());
    double last_inserted_key = listKeys.get(listKeys.size()-1);
    // get current amount of bytes
    double currentBytes = memoryUtilization.get(last_inserted_key);
    System.err.println("Writing memory "+this.getName()+ " storing "+currentBytes+" writing "+amountBytes+" at "+when);
    assert this.getCapacity() >= currentBytes+amountBytes;
    // I can only insert events from the last insert element, no insertions in the past
    assert last_inserted_key <= when;
    memoryUtilization.put(when, currentBytes+amountBytes);
  }

  public void readDataInMemory(double amountBytes, double when) {
    List<Double> listKeys = new ArrayList<>(memoryUtilization.keySet());
    double last_inserted_key = listKeys.get(listKeys.size()-1);
    // get current amount of bytes
    double currentBytes = memoryUtilization.get(last_inserted_key);
    System.err.println("Reading memory "+this.getName()+ " storing "+currentBytes+" reading "+amountBytes+" at "+when);
    assert currentBytes-amountBytes >= 0;
    // I can only insert events from the last insert element, no insertions in the past
    //System.out.println("Last inserted key "+last_inserted_key);
    //assert last_inserted_key <= when; => THIS ASSERT MÌGHT BE NECESSARY
    memoryUtilization.put(when, currentBytes-amountBytes);
  }

  public boolean canRemoveDataFromMemory(int amountBytes){
    double currentAmountBytes = this.getCurrentAmountofBytes();
    if(currentAmountBytes - amountBytes >= 0)
      return true;
    return false;
  }

  public boolean canPutDataInMemory(int amountBytes) {
    List<Double> listKeys = new ArrayList<>(memoryUtilization.keySet());
    double last_inserted_key = listKeys.get(listKeys.size()-1);
    // get current amount of bytes
    double currentBytes = memoryUtilization.get(last_inserted_key);
    //System.err.println("Storing "+currentBytes+" amount bytes "+amountBytes);
    if (currentBytes + amountBytes <= this.getCapacity())
      return true;
    return false;
  }

  public double getCurrentAmountofBytes(){
    List<Double> listKeys = new ArrayList<>(memoryUtilization.keySet());
    double last_inserted_key = listKeys.get(listKeys.size()-1);
    // get current amount of bytes
    return memoryUtilization.get(last_inserted_key);
  }

  // DUMPING the memory utilzation locally
  public void saveMemoryUtilizationStats(String path) throws IOException{
    try{
        File memUtilStatics = new File(path+"/memory-utilization-"+this.getName()+".csv");
        if (memUtilStatics.createNewFile()) {
          System.out.println("File created: " + memUtilStatics.getName());
        } else {
          System.out.println("File already exists.");
        }
    }
    catch (IOException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
    }

    FileWriter myWriter = new FileWriter(path+"/memory-utilization-"+this.getName()+".csv"); 
    myWriter.write("Memory\tWhen\tCapacity\n");

    saveMemoryUtilizationStats(myWriter);

    myWriter.close();
  }

  public void saveMemoryUtilizationStats(FileWriter myWriter) throws IOException{
    Map<Double,Double> memoryUtilization = this.getMemoryUtilization();
    List<Double> listKeys = new ArrayList<>(memoryUtilization.keySet());
    
    for (double element : listKeys) {
      myWriter.write(this.getName()+"\t"+element+"\t"+memoryUtilization.get(element)+"\n");
    }
  }

  public void printMemoryState(){
    Map<Double,Double> memoryUtilization = this.getMemoryUtilization();
    System.out.println("PRINTING MEMORY UTILIZATION: "+this.getName());
    List<Double> listKeys = new ArrayList<>(memoryUtilization.keySet());
    for(double e : listKeys){
      System.out.println("\t time ["+e+"] memory util. ["+memoryUtilization.get(e)+"]");
    }
  }


}
