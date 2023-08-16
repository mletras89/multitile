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
  public Architecture(Architecture another,  Map<Integer,ArrayList<MyEntry<Integer,Integer>>> procsPerType,  ArrayList<Integer> actorToCoreTypeMapping,ArrayList<Integer> nCoresPerTypeMapping,ArrayList<String> procTypes) {
	  //assert numberOfTiles == 3;
	  
	  // get the list of mapped cores
	  // it is a subset of the core list
	  ArrayList<Integer> nCoresPerTypeInArch = new ArrayList<>();
	  for(int i=0; i < procTypes.size();i++) {
		  nCoresPerTypeInArch.add(0);
	  }
	  Set<Integer> mappedTypes = new HashSet<Integer>(actorToCoreTypeMapping);
	  for(int type : mappedTypes) {
		  nCoresPerTypeInArch.set(type, nCoresPerTypeMapping.get(type)  ); 
	  }
	  
	  Map<Integer,ArrayList<MyEntry<Integer,Integer>>> allocatedProcsPerType = new HashMap<>();
	  
	  for(int type : mappedTypes) {
		  allocatedProcsPerType.put(type, new ArrayList<>());
	  }
	  
	  Set<Integer> mappedTiles = new HashSet<Integer>();
	  ArrayList<Integer> coresToAllocate = new ArrayList<>();  // ids of cores to allocate
	  
	  for(int type : mappedTypes) {
		  int nCores = nCoresPerTypeInArch.get(type);
		  
		  ArrayList<MyEntry<Integer,Integer>> cores = procsPerType.get(type);
		  
		  for(int i=0; i <nCores; i++) {
			  ArrayList<MyEntry<Integer,Integer>> tmpList =allocatedProcsPerType.get(type);
			  mappedTiles.add( cores.get(i).getKey() ); // the key is the tile id
			  coresToAllocate.add(cores.get(i).getValue()); // the value is the core id
			  tmpList.add(cores.get(i));
			  allocatedProcsPerType.put(type, tmpList);
		  }
	  }
	  
	  this.name = another.getName();
	  this.tiles = new HashMap<>();

	  // clone allocated tiles
	  for(Integer mappedTile : mappedTiles) {
		  int tileId = mappedTile;
		  Tile clonedTile = new Tile(new Tile(another.getTiles().get(tileId)));
		  assert clonedTile.getId() == another.getTiles().get(tileId).getId();
		  tiles.put(clonedTile.getId(), clonedTile);
	  }	

  
	  // key tile id
	  // value procesor id
	  ArrayList<MyEntry<Integer,Integer>> coresToRemove = new ArrayList<>();

	  for(Map.Entry<Integer,Tile> t : this.tiles.entrySet()) {
		  for(Map.Entry<Integer, Processor> p : t.getValue().getProcessors().entrySet()) {
			  if(!coresToAllocate.contains(p.getKey())) {
				  coresToRemove.add( new MyEntry<Integer,Integer>(t.getKey(), p.getKey())  );
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
	        System.out.println("\t\tLocal memory "+p.getValue().getLocalMemory().getName()+" capacity "+p.getValue().getLocalMemory().getCapacity()+" type "+p.getValue().getLocalMemory().getType());
	      }
	      System.out.println("\tTile Local memory: "+t.getValue().getTileLocalMemory().getName()+" capacity "+t.getValue().getTileLocalMemory().getCapacity()+" type "+t.getValue().getTileLocalMemory().getType());
	      System.out.println("\tCrossbar: "+t.getValue().getCrossbar().getName()+" bW "+t.getValue().getCrossbar().getBandwidth()+" channels "+t.getValue().getCrossbar().getNumberofParallelChannels()+ " bw per channel "+t.getValue().getCrossbar().getBandwithPerChannel());
	  }
	  System.out.println("NoC: "+this.getNoC().getName()+" bW "+this.getNoC().getBandwidth()+" channels "+this.getNoC().getNumberOfParallelChannels()+ " bw per channel "+this.getNoC().getBandwithPerChannel());
	  System.out.println("Global Memory: "+this.getGlobalMemory().getName()+" capacity "+this.getGlobalMemory().getCapacity()+" type "+this.globalMemory.getType());
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

  
  public HashMap<Integer,Queue<Processor>> getMapTileCoreTypeCores(ArrayList<String> coreTypes) {
	  
	  // key Int is the core type
	  // content a list of cores allocated in tile T of type s
	  HashMap<Integer,Queue<Processor>> mapCoreTypeToCores = new HashMap<>();
	  for(int i = 0; i < coreTypes.size(); i++) {
		  mapCoreTypeToCores.put(i, new LinkedList<> ());
	  }
	  for(Map.Entry<Integer, Tile > t: this.getTiles().entrySet()) {
		  for(Map.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()) {
			  String coreType = p.getValue().getProcesorType();
			  int indexCoreType = coreTypes.indexOf(coreType);
			  
			  Queue<Processor> currentList = mapCoreTypeToCores.get(indexCoreType);
			  currentList.add(new Processor(p.getValue()));
			  mapCoreTypeToCores.put(indexCoreType, currentList);
		  }
	  }
	  return mapCoreTypeToCores;
  }
  
  public HashMap<Integer,ArrayList<Processor>> getMapTileCoreTypeCoresAsList(ArrayList<String> coreTypes) {
	  
	  // key Int is the core type
	  // content a list of cores allocated in tile T of type s
	  HashMap<Integer,ArrayList<Processor>> mapCoreTypeToCores = new HashMap<>();
	  for(int i = 0; i < coreTypes.size(); i++) {
		  mapCoreTypeToCores.put(i, new ArrayList<> ());
	  }
	  
	  for(Map.Entry<Integer, Tile > t: this.getTiles().entrySet()) {
		  for(Map.Entry<Integer,Processor> p : t.getValue().getProcessors().entrySet()) {
			  String coreType = p.getValue().getProcesorType();
			  int indexCoreType = coreTypes.indexOf(coreType);
			  
			  ArrayList<Processor> currentList = mapCoreTypeToCores.get(indexCoreType);
			  currentList.add(new Processor(p.getValue()));
			  mapCoreTypeToCores.put(indexCoreType, currentList);
		  }
	  }
	  return mapCoreTypeToCores;
  }
  
  

  public Processor getProcessor(int processorId) {
	  for(Map.Entry<Integer, Tile> t: this.tiles.entrySet()) {
		  for(Map.Entry<Integer, Processor> p : t.getValue().getProcessors().entrySet()) {
			  if (p.getKey() == processorId)
				  return p.getValue();
		  }
	  }
	  return null;
  }
  
  public Processor isProcessor(int resourceId) {
	  Processor processor = null;
	  for(Map.Entry<Integer,Tile>  t : tiles.entrySet()) {
		  for(Map.Entry<Integer, Processor> p : t.getValue().getProcessors().entrySet()) {
			  if (p.getKey() == resourceId)
				  return p.getValue();
		  }
	  }
	  return processor;
  }
  
  public Crossbar isCrossbar(int resourceId) {
	  Crossbar crossbar = null;
	  for(Map.Entry<Integer,Tile>  t : tiles.entrySet()) {
		  if(t.getValue().getCrossbar().getId() == resourceId)
			  return t.getValue().getCrossbar();		  
	  }
	  return crossbar;
  }
  
  public NoC isNoC(int resourceId) {
	  NoC noc = null;
	  if (this.noc.getId() == resourceId)
		  return this.noc;
	  return noc;
  }
  
}
