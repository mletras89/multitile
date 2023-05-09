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
import java.util.*;

import multitile.application.MyEntry;

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
  
  public void setMemoryVerboseDebug(boolean val) {
	  for(Map.Entry<Integer, Tile> t : this.tiles.entrySet()) {
		  t.getValue().getTileLocalMemory().setVerboseDebug(val);
		  for(Map.Entry<Integer, Processor> p :  t.getValue().getProcessors().entrySet()) {
			  p.getValue().getLocalMemory().setVerboseDebug(val);
		  }
	  }
	  this.globalMemory.setVerboseDebug(val);
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
  
  // clone the architecture but only a subset of the given architecture
  public Architecture(Architecture another,HashMap<Integer,Integer> tileIndexToId, ArrayList<Integer> actorToTileMapping, ArrayList<Integer> actorToCoreTypeMapping,ArrayList<Integer> nCoresPerTypeMapping,int numberOfTiles,ArrayList<String> procTypes) {
	  assert numberOfTiles == 3;
	  Set<Integer> mappedTiles  = new HashSet<Integer>(actorToTileMapping);
	  // real tile id. core type index, and count
	  HashMap<ArrayList<Integer>,Integer> countCoresPerTile = new HashMap<ArrayList<Integer>,Integer>();
	  
	  for(Map.Entry<Integer,Tile> t : another.getTiles().entrySet()) {
		  for(String pType : procTypes) {
			  ArrayList<Integer> entryKey = new ArrayList<>();
			  entryKey.add(t.getKey()); // tile first
			  entryKey.add(procTypes.indexOf(pType));
			  countCoresPerTile.put(entryKey, 0);
		  }
		  
	  }
	  
	  for(int i = 0 ; i < actorToTileMapping.size(); i++) {
		  int tile = actorToTileMapping.get(i);
		  int coreType = actorToCoreTypeMapping.get(i);
		  ArrayList<Integer> entryKey = new ArrayList<>();
		  entryKey.add(tileIndexToId.get(tile)); // tile first
		  entryKey.add(coreType);
		  countCoresPerTile.put(entryKey,nCoresPerTypeMapping.get(numberOfTiles*tile + coreType));
		  
	  }
	  this.name = another.getName();
	  this.tiles = new HashMap<>();
	  
	  // key tile id
	  // value procesor id
	  ArrayList<MyEntry<Integer,Integer>> coresToRemove = new ArrayList<>();
	  
	  for(Integer mappedTile : mappedTiles) {
		  int tileId = tileIndexToId.get(mappedTile);
		  Tile clonedTile = new Tile(new Tile(another.getTiles().get(tileId)));
		  
		  
		  assert clonedTile.getId() == another.getTiles().get(tileId).getId();
		  tiles.put(clonedTile.getId(), clonedTile);
	  }	
	  
	  
	  for(Map.Entry<Integer, Tile > t : this.tiles.entrySet()) {
		  for(String pType : procTypes) {
			  ArrayList<Integer> key = new ArrayList<Integer>(); 
			  key.add(t.getKey());
			  key.add(procTypes.indexOf(pType));
			  if(countCoresPerTile.get(key)>0) {
				  int nAllocatedCores = countCoresPerTile.get(key);
				  int countAllocatedCores = 0;
				  for(Map.Entry<Integer, Processor> p : t.getValue().getProcessors().entrySet()) {
					  if (p.getValue().getProcesorType().compareTo(pType) == 0) {
						  if (countAllocatedCores < nAllocatedCores) {
							  //System.err.println("Allocating "+t.getValue().getName()+ " core "+p.getValue().getName()+" type "+p.getValue().getProcesorType() );
							  countAllocatedCores++;
						  }
						  else {
							  coresToRemove.add( new MyEntry<Integer,Integer>(t.getKey(), p.getKey()));
							  //coresToRemove.put(t.getKey(), p.getKey() );
							  //System.err.println("REMOVE "+t.getValue().getName()+ " core "+p.getValue().getName()+" type "+p.getValue().getProcesorType() );
						  }  
					  }
				  }  
			  }else{
				  for(Map.Entry<Integer, Processor> p : t.getValue().getProcessors().entrySet()) {
					  if (p.getValue().getProcesorType().compareTo(pType) == 0) {
						  //coresToRemove.put(t.getKey(), p.getKey() ); 
						  coresToRemove.add( new MyEntry<Integer,Integer>(t.getKey(), p.getKey()));
						  //System.err.println("REMOVE "+t.getValue().getName()+ " core "+p.getValue().getName()+" type "+p.getValue().getProcesorType() );
					  }
				  }  
			  }
		  }
	  }

	  // remove cores
	  for(MyEntry<Integer, Integer> entry : coresToRemove) {
		  tiles.get(entry.getKey()).getProcessors().remove(entry.getValue());
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
	        System.out.println("\tProcessor "+p.getValue().getName()+" type "+p.getValue().getProcesorType()+" core id "+p.getKey());
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

  
  public HashMap<ArrayList<Integer>,Queue<Processor>> getMapTileCoreTypeCores(ArrayList<String> coreTypes) {
	  
	  // key Int is tile id of tile T, String core type s
	  // content a list of cores allocated in tile T of type s
	  HashMap<ArrayList<Integer>,Queue<Processor>> mapTileAndCoreAndTypeCores = new HashMap<>();
	  
	  
	  for(Map.Entry<Integer, Tile > t: this.getTiles().entrySet()) {
		  for(int i = 0; i < coreTypes.size(); i++) {
			  ArrayList<Integer> key = new ArrayList<Integer>();
			  key.add(t.getKey());
			  key.add(i);
			  mapTileAndCoreAndTypeCores.put(key, new LinkedList<> ());
		  }
	  }
	  for(Map.Entry<Integer, Tile > t: this.getTiles().entrySet()) {
		  for(Map.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()) {
			  String coreType = p.getValue().getProcesorType();
			  ArrayList<Integer> key = new ArrayList<Integer>();
			  key.add(t.getKey());
			  key.add(coreTypes.indexOf(coreType));
			  Queue<Processor> currentList = mapTileAndCoreAndTypeCores.get(key);
			  
			  currentList.add(new Processor(p.getValue()));
			  
			  mapTileAndCoreAndTypeCores.put(key, currentList);
		  }
	  }
	  return mapTileAndCoreAndTypeCores;
  }
  
}
