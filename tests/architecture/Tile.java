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
     This class describes a Tile in the architecture. Each Tile contains:
        - processors: a list of processors in the tile.
        - crossbar: crossbar that communicate the processors
        - tileLocalMemory: is the memory local to this tile
--------------------------------------------------------------------------
*/
package multitile.architecture;

import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.application.Application;

//import src.multitile.FCFS;
import multitile.Action;
import multitile.Transfer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.Queue;
import java.util.*;

public class Tile{
  private int id;
  private String name;
  private int numberProcessors;
  // the key is the id
  private HashMap<Integer,Processor> processors;
  private Crossbar crossbar;
  private TileLocalMemory tileLocalMemory;
  private int totalIterations;
  
  public Tile(){
    this.id = ArchitectureManagement.getTileId();
    this.name = "Tile1";
    this.numberProcessors = 1;
    this.processors = new HashMap<>();
    for(int i=0; i<this.numberProcessors;i++){
      Processor processor = new Processor("Processor"+i);
      processor.setOwnerTile(this);
      processors.put(processor.getId(),processor);
    }

    for(HashMap.Entry<Integer,Processor> e: processors.entrySet()){
      // connecting local memory to processor
      e.getValue().getLocalMemory().setEmbeddedToProcessor(e.getValue());
    }
    crossbar = new Crossbar("crossbar_"+this.name, 1,2);
    tileLocalMemory = new TileLocalMemory("TileLocalMemory_"+this.name);
    this.totalIterations = 1;
    tileLocalMemory.setOwnerTile(this);
  }

  public Tile(String name,int numberProcessors,double crossbarBw,int crossbarChannels){
    this.id = ArchitectureManagement.getTileId();
    this.name = name;
    this.numberProcessors = numberProcessors;
    this.processors = new HashMap<>();
    //System.out.println("Here!");
    for(int i=0; i<this.numberProcessors;i++){
      Processor processor = new Processor(this.name+"_Processor"+i);
      processor.setOwnerTile(this);
      processors.put(processor.getId(),processor);
    }
    for(HashMap.Entry<Integer,Processor> e: processors.entrySet()){
      // connecting local memory to processor     
      e.getValue().getLocalMemory().setEmbeddedToProcessor(e.getValue());     
    }
    crossbar = new Crossbar("crossbar_"+this.name, crossbarBw,crossbarChannels);
    tileLocalMemory = new TileLocalMemory("TileLocalMemory_"+this.name);
    this.totalIterations = 1;
    tileLocalMemory.setOwnerTile(this);
  }

  public double averageProcessorUtilization(double endTime){
    double processorUtilization = 0.0;
    double nProcs=0;
    double tempVal;
    for(Map.Entry<Integer,Processor> entry : processors.entrySet()){
      tempVal = entry.getValue().calculateOverallProcessorUtilization(endTime);
      if(tempVal > 0){
        nProcs++;
        processorUtilization += tempVal;
      }
    }
    return processorUtilization/nProcs;
  }

  public void setName(String name){
    this.name = name;
    crossbar.setName("crossbar_"+this.name);
    int i=0;
    for(HashMap.Entry<Integer,Processor> e: processors.entrySet()){
      e.getValue().getScheduler().setName(this.name+"_Processor"+(i++));  
    }
    tileLocalMemory.setName("TileLocalMemory_"+this.name);
  }

  public boolean equals(Tile tile){
    return this.getId() == tile.getId() && this.getName().equals(tile.getName());
  }

  public Crossbar getCrossbar(){
    return this.crossbar;
  }
  
  public void setTotalIterations(int totalIterations){
    this.totalIterations = totalIterations;
  }
  
  public TileLocalMemory getTileLocalMemory(){
    return this.tileLocalMemory;
  }

  public HashMap<Integer,Processor> getProcessors(){
    return this.processors;
  }

  public int getRunIterations(){
    int max = 0 ;
    for(HashMap.Entry<Integer,Processor> e : processors.entrySet()){
      if(max < e.getValue().getRunIterations())
        max = e.getValue().getRunIterations();
    }
    return max;
  }

  public void resetTile(){
    // first reset the processors
    for(HashMap.Entry<Integer,Processor> e: processors.entrySet()){
      e.getValue().restartProcessor();
    }
    // refresh the tile local memory
    tileLocalMemory.resetMemoryUtilization();
    // restart the crossbar
    crossbar.restartCrossbar();
  }
  
  public String getName(){
    return this.name;
  }

  public int getId(){
    return this.id;
  }

  public void setId(int id){
    this.id = id;
  }

}
