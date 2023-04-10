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
  @date   10 April 2023
  @version 1.1
  @ brief
     Example of a single tile architecture with four processors with local
     memory, crossbar and tile local memory running a modulo scheduler,
     the DFG application might contain cycles
--------------------------------------------------------------------------
*/
package multitile.tests;

import multitile.scheduler.ModuloScheduler;

import multitile.architecture.Architecture;
import multitile.architecture.Tile;
import multitile.mapping.Bindings;
import multitile.mapping.Mappings;
import multitile.architecture.Processor;

import multitile.application.Application;
import multitile.application.FifoManagement;
import multitile.application.ApplicationManagement;
import multitile.application.ActorManagement;
import multitile.application.Actor;
import multitile.architecture.ArchitectureManagement;
import multitile.application.GraphManagement;

import java.io.*;
import java.util.*;

public class testModuloSchedulingRecurrences {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing two tiles implementation, each tile has 2 cores and connected with a NoC and merging the multicast actor!");

      ActorManagement.resetCounters();
      FifoManagement.resetCounters();
      ArchitectureManagement.resetCounters();

      Architecture architecture = new Architecture("architecture",1,1, 1.0, 2);
      //architecture.printArchitecture();
      // set the memory sizes
      architecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().setCapacity(Integer.MAX_VALUE);
      architecture.getTiles().get(0).getTileLocalMemory().setCapacity(Integer.MAX_VALUE);
      architecture.getGlobalMemory().setCapacity(Integer.MAX_VALUE);
      
      Bindings bindings = new Bindings();
      Mappings mappings = new Mappings();
      
      TestApplicationWithRecurrences sampleApplication = new TestApplicationWithRecurrences(architecture.getTiles().get(0),architecture.getGlobalMemory(),bindings,mappings);  
      Application app = sampleApplication.getSampleApplication();
      ApplicationManagement.setFifosToActors(app);
      ApplicationManagement.setAllMulticastActorsAsMergeable(app);
      app.printActors();
      app.printFifosState();
      ApplicationManagement.collapseMergeableMulticastActors(app,1);
      app.printActorsState(bindings);
      app.printFifosState();
      // chech the calculation of distances and loops
      for(Map.Entry<Integer,Actor> a : app.getActors().entrySet()){
        GraphManagement.BellmanFord(app,a.getValue());
      }
      
      ModuloScheduler scheduler = new ModuloScheduler();
      // I need to update the actor mapping according to the modulo schedule!!!!!
      scheduler.setApplication(app);
      scheduler.setArchitecture(architecture);
			
      scheduler.setMaxIterations(10);
      // comment scheduling
      //scheduler.calculateModuloSchedule(bindings);
      //System.out.println("PRINTING KERNEL: ");
      //scheduler.printKernelBody();
      // once the kernell is done, reassign the actor Mapping and then reassing the fifoMapping
      
      //scheduler.findSchedule();
      //ApplicationManagement.assignActorMapping(app,architecture,scheduler,bindings);
      //ApplicationManagement.assignFifoMapping(app,architecture,bindings); 
      //app.printActors();
      //app.printFifos();
 
      //scheduler.schedule(bindings,mappings);
      //System.out.println("Single iteration delay: "+scheduler.getDelaySingleIteration());
      //System.out.println("The MMI is: "+scheduler.getMII());

      // dumping system utilization statistics
      for(HashMap.Entry<Integer,Tile> t: architecture.getTiles().entrySet()){
        for(HashMap.Entry<Integer,Processor> p: t.getValue().getProcessors().entrySet()){
          p.getValue().getScheduler().saveScheduleStats(".");
	  p.getValue().getLocalMemory().saveMemoryUtilizationStats(".");
        }
        t.getValue().getCrossbar().saveCrossbarUtilizationStats(".");
        t.getValue().getTileLocalMemory().saveMemoryUtilizationStats(".");
      }
      architecture.getNoC().saveNoCUtilizationStats(".");
      architecture.getGlobalMemory().saveMemoryUtilizationStats(".");
      
      // get the end time
      double endTime=architecture.getEndTime();
      System.out.println("End time: "+endTime);
      // print the utilization of each processor and each crossbar
      for(HashMap.Entry<Integer,Tile> t: architecture.getTiles().entrySet()){
        for(HashMap.Entry<Integer,Processor> p: t.getValue().getProcessors().entrySet()){
          System.out.println("Processor "+p.getValue().getName()+" Utilization: "+p.getValue().calculateOverallProcessorUtilization(endTime));
          System.out.println("Procesor Local Memory: "+p.getValue().getLocalMemory().getName()+" utilization "+p.getValue().getLocalMemory().getUtilization(endTime)); 
        }
        System.out.println("Tile "+t.getValue().getName()+" avg. utilization: "+t.getValue().averageProcessorUtilization(endTime));
        System.out.println("Crossbar "+t.getValue().getCrossbar().getName()+ " crossbar util. "+t.getValue().getCrossbar().calculateCrossbarOverallUtilization(endTime));
        System.out.println("Tile local memory: "+t.getValue().getTileLocalMemory().getName()+ " utilization "+t.getValue().getTileLocalMemory().getUtilization(endTime));
      }
      //System.out.println("NoC Utilization: "+architecture.getNoC().calculateNoCOverallUtilization(endTime));
      System.out.println("Global memory: "+architecture.getGlobalMemory().getName()+ " utilization "+architecture.getGlobalMemory().getUtilization(endTime));

      System.out.println("Testing quadcore implementation testcase done!");
      architecture.saveArchitectureUtilizationStats(".");
    }
}
