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
  @date   07 December 2022
  @version 1.1
  @ brief
     This class describes an architecture. Each architecture contains:
        - tiles: a map of tiles in the architecture
        - NoC: NoC that communicates the tiles
        - globalMemory: is the memory global to all the tiles
--------------------------------------------------------------------------
*/
package multitile.architecture;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.*;

public class Architecture{
  // the key is the id
  private String name;
  private HashMap<Integer,Tile> tiles;
  private NoC noc;
  private GlobalMemory globalMemory;
  
  public Architecture(String name){
    this.name = name;
    // creaate one tile in the architecture
    Tile t1 = new Tile("Tile1",4,1.0,2);
    
    tiles = new HashMap<>();	
    tiles.put(t1.getId(),t1);
    
    globalMemory = new GlobalMemory("GLOBAL_MEMORY");
    noc = new NoC();
  }

  public Architecture(String name, int nTiles, int nProcPerTile, double BWCrossbars, int channelsCrossbar){
    this.name = name;
    tiles = new HashMap<>(); 
    // declaring NoC for multitile architecture
    noc = new NoC();      
	for(int i=0; i < nTiles; i++){
      Tile t = new Tile("Tile"+(i+1), nProcPerTile, BWCrossbars, channelsCrossbar);
      tiles.put(t.getId(),t);  
    }
    globalMemory = new GlobalMemory("GLOBAL_MEMORY");
  }

  public Architecture(String name, String nameTile, int nProcPerTile, double BWCrossbars, int channelsCrossbar){
    // for test purposes, single tile with name tile
    this.name = name;
    tiles = new HashMap<>(); 

    Tile t = new Tile(nameTile, nProcPerTile, BWCrossbars, channelsCrossbar);
    tiles.put(t.getId(),t);  

    globalMemory = new GlobalMemory("GLOBAL_MEMORY");
    noc = new NoC();
  }
  
  // a new constructor that creates an architecture from a set tiles
  public Architecture(String name, List<Tile> tilesL) {
	  this.name = name;
	  tiles = new HashMap<>();
	  globalMemory = new GlobalMemory("GLOBAL_MEMORY");
	  noc = new NoC();
	  for(Tile t : tilesL) {
		  tiles.put(t.getId(), t);
	  }
  }
  
  // clone architecture from another architecture
  public Architecture(Architecture another) {
	  this.name = another.getName();
	  this.tiles = new HashMap<>();
	  for(Map.Entry<Integer, Tile> t : another.getTiles().entrySet()) {
		  Tile clonedTile = new Tile(t.getValue());
		  tiles.put(clonedTile.getId(), clonedTile);
	  }
	  this.noc = new NoC(another.getNoC());
	  this.globalMemory = new GlobalMemory(another.getGlobalMemory()); 
  }
  
  public double getEndTime(){
    double endTime = 0.0;
    for(Map.Entry<Integer,Tile> t : tiles.entrySet()){
      for(Map.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
        if(p.getValue().getScheduler().getScheduledActions().size() > 0){
          double last = p.getValue().getScheduler().getScheduledActions().getLast().getDue_time();
          if (last > endTime)
            endTime = last;
        }
      }
    }
    return endTime;
  }

  public Crossbar getCrossbar(int crossbarId){
    for(Map.Entry<Integer,Tile> entry : this.tiles.entrySet()){
      if (entry.getValue().getCrossbar().getId() == crossbarId)
        return entry.getValue().getCrossbar();
    }
    return null;
  } 
  
  public NoC getNoC(){
    return this.noc;
  }
 
  public String getName(){
  	return this.name;
  }
  
  public void setName(String name){
  	this.name = name;
  }
  
  public Tile getTileByName(String tileName) {
	  for(Map.Entry<Integer, Tile> entry : this.tiles.entrySet()){
		  if (entry.getValue().getName() == tileName)
			  return entry.getValue();
	  }
	  return null;
  }
  
  public HashMap<Integer,Tile> getTiles(){
  	return this.tiles;
  }
  
  public GlobalMemory getGlobalMemory(){
  	return this.globalMemory;
  }
  
  public void resetArchitecture(){
    for(HashMap.Entry<Integer,Tile> t: tiles.entrySet()){
      t.getValue().resetTile();
    }
    // refresh the global memory
    this.globalMemory.resetMemoryUtilization();
    // reset the NoC
    this.noc.restartNoC();
  }

  public void printArchitectureState(){
    for(HashMap.Entry<Integer,Tile> t: this.tiles.entrySet()){
      for(HashMap.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
        p.getValue().getLocalMemory().printMemoryState();
      }
      t.getValue().getTileLocalMemory().printMemoryState();
    }
  }
  
  public void printArchitecture() {
	  for(HashMap.Entry<Integer,Tile> t: this.tiles.entrySet()){
		  System.out.println("Tile: "+t.getValue().getName());
	      for(HashMap.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()){
	        System.out.println("\tProcessor "+p.getValue().getName());
	        System.out.println("\t\tLocal memory "+p.getValue().getLocalMemory().getName()+" capacity "+p.getValue().getLocalMemory().getCapacity());
	      }
	      System.out.println("\tTile Local memory: "+t.getValue().getTileLocalMemory().getName()+" capacity "+t.getValue().getTileLocalMemory().getCapacity());
	      System.out.println("\tCrossbar: "+t.getValue().getCrossbar().getName()+" bW "+t.getValue().getCrossbar().getBandwidth()+" channels "+t.getValue().getCrossbar().getNumberofParallelChannels()+ " bw per channel "+t.getValue().getCrossbar().getBandwithPerChannel());
	  }
	  System.out.println("NoC: "+this.getNoC().getName()+" bW "+this.getNoC().getBandwidth()+" channels "+this.getNoC().getNumberOfParallelChannels()+ " bw per channel "+this.getNoC().getBandwithPerChannel());
	  System.out.println("Global Memory: "+this.getGlobalMemory().getName()+" capacity "+this.getGlobalMemory().getCapacity());
  }
  

  // DUMPING the architecture utilization stats
  // processor and local memory Utilization
  // average processor utilization per tile
  // average local memory utilization per tile
  // tile local memory utilization
  // NoC utilization
  // global memory utilization

  public void saveArchitectureUtilizationStats(String path) throws IOException{
    double endTime=this.getEndTime();
    try{
        File archUtilStatics = new File(path+"/architecture-utilization-"+this.getName()+".dat");
        if (archUtilStatics.createNewFile()) {
          System.out.println("File created: " + archUtilStatics.getName());
        } else {
          System.out.println("File already exists.");
        }
    }
    catch (IOException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
    }
    FileWriter myWriter = new FileWriter(path+"/architecture-utilization-"+this.getName()+".dat");  
    myWriter.write("Processors:\n");
    for(HashMap.Entry<Integer,Tile> t: tiles.entrySet()){
      for(HashMap.Entry<Integer,Processor> p: t.getValue().getProcessors().entrySet()){
        myWriter.write("Processor "+p.getValue().getName()+" Utilization: "+p.getValue().calculateOverallProcessorUtilization(endTime)+"\n");
      }
    }
    myWriter.write("Memories:\n");
    for(HashMap.Entry<Integer,Tile> t: tiles.entrySet()){
      for(HashMap.Entry<Integer,Processor> p: t.getValue().getProcessors().entrySet()){
        myWriter.write("Procesor Local Memory: "+p.getValue().getLocalMemory().getName()+" utilization "+p.getValue().getLocalMemory().getUtilization(endTime)+"\n");
      }
    }
    myWriter.write("Tiles:\n");
    for(HashMap.Entry<Integer,Tile> t: tiles.entrySet()){ 
      myWriter.write("Tile "+t.getValue().getName()+" avg. processor utilization: "+t.getValue().averageProcessorUtilization(endTime)+"\n");
      myWriter.write("Crossbar "+t.getValue().getCrossbar().getName()+ " crossbar util. "+t.getValue().getCrossbar().calculateCrossbarOverallUtilization(endTime)+"\n");
      myWriter.write("Tile local memory: "+t.getValue().getTileLocalMemory().getName()+ " utilization "+t.getValue().getTileLocalMemory().getUtilization(endTime)+"\n");
    }
    myWriter.write("Global memory: "+this.getGlobalMemory().getName()+ " utilization "+this.getGlobalMemory().getUtilization(endTime)+"\n");
    myWriter.close();
  }

}
