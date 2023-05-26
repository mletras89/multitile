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
  @date   15 December 2022
  @version 1.1
  @ brief
     Example of a single tile architecture with four processors with local
     memory, crossbar and tile local memory running a modulo scheduluer
     and implementing memory relocation
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
import multitile.architecture.ArchitectureManagement;

import java.io.*;
import java.util.*;

public class testModuloSchedulingWithNoCMergeMulticast {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing two tiles implementation, each tile has 2 cores and connected with a NoC and merging the multicast actor!");

      ActorManagement.resetCounters();
      FifoManagement.resetCounters();
      ArchitectureManagement.resetCounters();

      Architecture architecture = new Architecture("architecture",2,2, 1.0, 2);
      //architecture.printArchitecture();
      // set the memory sizes
      architecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().setCapacity(5000000);
      architecture.getTiles().get(0).getProcessors().get(1).getLocalMemory().setCapacity(5000000);
      architecture.getTiles().get(1).getProcessors().get(2).getLocalMemory().setCapacity(5000000);
      architecture.getTiles().get(1).getProcessors().get(3).getLocalMemory().setCapacity(5000000);
      architecture.getTiles().get(0).getTileLocalMemory().setCapacity(10000000);
      architecture.getTiles().get(1).getTileLocalMemory().setCapacity(10000000);
      architecture.getGlobalMemory().setCapacity(50000000);
      
      Map<Integer,ArrayList<Integer>> indexCoreTypes = new HashMap<>();
      indexCoreTypes.put(0, new ArrayList<Integer>());
      
      for(HashMap.Entry<Integer,Tile> t: architecture.getTiles().entrySet()){
        System.out.println("Tile name:"+t.getValue().getName());
        for(HashMap.Entry<Integer,Processor> p: t.getValue().getProcessors().entrySet()){
        	p.getValue().setProcesorType("P0");
        	ArrayList<Integer> tmp = indexCoreTypes.get(0);
        	tmp.add(p.getKey());
        	indexCoreTypes.put(0, tmp);
        	System.out.println("\tProcessor name:"+p.getValue().getName()+" with id "+p.getValue().getId());
	  System.out.println("\tOwner tile:"+p.getValue().getOwnerTile().getName());
     	  System.out.println("\t\tAttached to local memory:"+p.getValue().getLocalMemory().getName());
        }			
      }
      
      
      Bindings bindings = new Bindings();
      Mappings mappings = new Mappings();
      
      TestApplicationQuadCoreMemoryBound sampleApplication = new TestApplicationQuadCoreMemoryBound(architecture.getTiles().get(0), architecture.getTiles().get(1),architecture.getGlobalMemory(),bindings,mappings);  
      Application app = sampleApplication.getSampleApplication();
      ApplicationManagement.setFifosToActors(app);
      ApplicationManagement.setAllMulticastActorsAsMergeable(app);
      //app.printActors();
      //app.printFifosState();
      ApplicationManagement.collapseMergeableMulticastActors(app,1);
      //app.printActorsState(bindings);
      //app.printFifosState();
      
      ArrayList<Integer> actorToCoreTypeMapping = sampleApplication.getActorToCoreTypeMapping();
      
      Set<String> coreTypes = new HashSet<>();
      coreTypes.add("P0");
      
      HashMap<Integer,Integer> actorIdToIndex = sampleApplication.getActorIdToIndex();
      
      ModuloScheduler scheduler = new ModuloScheduler(architecture,app,indexCoreTypes,actorToCoreTypeMapping,coreTypes);
      // calculate modulo schedule but do not consider cycles to make it faster
      scheduler.calculateModuloSchedule(actorIdToIndex,false);
      
      scheduler.findSchedule();
  	  assert scheduler.checkValidKernel(); // check if the kernel is valid
      
      
  	  // assign the actor binding
      scheduler.assingActorBinding(mappings,bindings,actorIdToIndex);
      
      ApplicationManagement.assignFifoMapping(app,architecture,bindings);
      scheduler.scheduleSingleIteration(bindings);
      
      System.out.println("Single iteration delay: "+scheduler.getOverallDelay());

      System.out.println("The MMI is: "+scheduler.getMII());
      
      scheduler.getArchitecture().printArchitecture();
      // dumping system utilization statistics
      for(HashMap.Entry<Integer,Tile> t: scheduler.getArchitecture().getTiles().entrySet()){
        for(HashMap.Entry<Integer,Processor> p: t.getValue().getProcessors().entrySet()){
          p.getValue().getScheduler().saveScheduleStats(".");
          p.getValue().getLocalMemory().saveMemoryUtilizationStats(".");
        }
        t.getValue().getCrossbar().saveCrossbarUtilizationStats(".");
        t.getValue().getTileLocalMemory().saveMemoryUtilizationStats(".");
      }
      scheduler.getArchitecture().getNoC().saveNoCUtilizationStats(".");
      scheduler.getArchitecture().getGlobalMemory().saveMemoryUtilizationStats(".");
      
      // get the end time
      double endTime=scheduler.getArchitecture().getEndTime();
      System.out.println("End time: "+endTime);
      // print the utilization of each processor and each crossbar
      for(HashMap.Entry<Integer,Tile> t: scheduler.getArchitecture().getTiles().entrySet()){
        for(HashMap.Entry<Integer,Processor> p: t.getValue().getProcessors().entrySet()){
          System.out.println("Processor "+p.getValue().getName()+" Utilization: "+p.getValue().calculateOverallProcessorUtilization(endTime));
          System.out.println("Procesor Local Memory: "+p.getValue().getLocalMemory().getName()+" utilization "+p.getValue().getLocalMemory().getUtilization(endTime)); 
        }
        System.out.println("Tile "+t.getValue().getName()+" avg. utilization: "+t.getValue().averageProcessorUtilization(endTime));
        System.out.println("Crossbar "+t.getValue().getCrossbar().getName()+ " crossbar util. "+t.getValue().getCrossbar().calculateCrossbarOverallUtilization(endTime));
        System.out.println("Tile local memory: "+t.getValue().getTileLocalMemory().getName()+ " utilization "+t.getValue().getTileLocalMemory().getUtilization(endTime));
      }
      //System.out.println("NoC Utilization: "+architecture.getNoC().calculateNoCOverallUtilization(endTime));
      System.out.println("Global memory: "+scheduler.getArchitecture().getGlobalMemory().getName()+ " utilization "+architecture.getGlobalMemory().getUtilization(endTime));

      System.out.println("Testing quadcore implementation testcase done!");
      scheduler.getArchitecture().saveArchitectureUtilizationStats(".");
    }
}
