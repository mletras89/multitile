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
     This class describes a Processor element that performs the execution
     of tasks. It encapsulates:
        - scheduler: that performs the scheduling of actions mapped
        - localMemory: the local memory of the processor
--------------------------------------------------------------------------
*/
package multitile.architecture;

import multitile.Action;

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

public class Processor {
  private int id;
  private String name;
  private Scheduler scheduler;
  // each processors has a local memory
  private LocalMemory localMemory;
  private Tile ownerTile;


  public Processor(String name) {
    this.setName(name);
    this.setId(ArchitectureManagement.getProcessorId());
    localMemory = new LocalMemory(this.name+"_localMemory");
    scheduler = new Scheduler(name,this);
    scheduler.setNumberIterations(1); 
    // connecting local memory to processor
    this.localMemory.setEmbeddedToProcessor(this);
  }
    
  public Processor(Processor other) {
    this.setName(other.getName());
    this.setId(other.getId());
    this.scheduler = other.scheduler;
    this.localMemory.setEmbeddedToProcessor(this);
  }

  public double calculateOverallProcessorUtilization(double endTime){
    double processorUtilization = 0.0;
    for(Action action : this.scheduler.getScheduledActions()){
      processorUtilization += action.getDue_time() - action.getStart_time(); 
    }
    return processorUtilization/endTime;
  }


  public boolean equals(Processor processor){
    return this.getId() == processor.getId() && this.getName().equals(processor.getName());
  }

  public Tile getOwnerTile(){
    return this.ownerTile;
  }

  public void setOwnerTile(Tile owner){
    this.ownerTile = owner;
  }

  public LocalMemory getLocalMemory(){
    return this.localMemory;
  }

  public void setLocalMemory(LocalMemory localMemory){
    this.localMemory = localMemory;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void restartProcessor() {
    this.scheduler.restartScheduler();
    this.localMemory.resetMemoryUtilization();
  }

  public int getRunIterations(){
    return scheduler.getRunIterations();
  }

  public Scheduler getScheduler(){
    return this.scheduler;
  }
}
