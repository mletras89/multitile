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
  @date   03 December 2022
  @version 1.1
  @ brief
     Example of a single tile architecture with four processors with local
     memory, crossbar and tile local memory running a modulo scheduluer
--------------------------------------------------------------------------
*/
package multitile.tests;

import multitile.scheduler.ModuloScheduler;

import multitile.architecture.Architecture;
import multitile.architecture.Tile;
import multitile.architecture.Memory;
import multitile.architecture.Processor;

import multitile.application.Application;
import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.application.CompositeFifo;
import multitile.application.FifoManagement;
import multitile.application.ApplicationManagement;
import multitile.application.ActorManagement;
import multitile.application.FifoManagement;
import multitile.architecture.ArchitectureManagement;


import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class testQuadCoreModuloScheduling {
    public static void main(String[] args) throws IOException {

      System.out.println("Testing singlecore implementation testcase and modulo scheduling!");

      Architecture singleCoreArchitecture = new Architecture("architecture","ModuloSchedulingSingle", 1, 1.0, 2);
      TestApplication testApplication = new TestApplication(singleCoreArchitecture.getTiles().get(0));  
      Application singleCoreApplication = testApplication.getSampleApplication();

      ModuloScheduler singleCoreScheduler = new ModuloScheduler();
      singleCoreScheduler.setApplication(singleCoreApplication);
      singleCoreScheduler.setArchitecture(singleCoreArchitecture);

      singleCoreScheduler.setMaxIterations(5);
      singleCoreScheduler.calculateModuloSchedule();
      singleCoreScheduler.printKernelBody();
      singleCoreScheduler.findSchedule();
      singleCoreScheduler.schedule();

      System.out.println("Single iteration delay: "+singleCoreScheduler.getDelaySingleIteration());

      System.out.println("The MMI is: "+singleCoreScheduler.getMII());

      singleCoreArchitecture.getTiles().get(0).getProcessors().get(0).getScheduler().saveScheduleStats(".");
      singleCoreArchitecture.getTiles().get(0).getCrossbar().saveCrossbarUtilizationStats(".");

      // dumping memory utilization
      singleCoreArchitecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().saveMemoryUtilizationStats(".");
      singleCoreArchitecture.getTiles().get(0).getTileLocalMemory().saveMemoryUtilizationStats(".");;

      System.out.println("Testing singlecore implementation testcase done and modulo scheduling!");

      System.out.println("Testing dualcore implementation testcase and modulo scheduling!");

      ActorManagement.resetCounters();
      FifoManagement.resetCounters();
      ArchitectureManagement.resetCounters();

      Architecture dualCoreArchitecture = new Architecture("architecture","ModuloSchedulingDual", 2, 1.0, 2);
      TestApplicationDualCore testDualApplication = new TestApplicationDualCore(dualCoreArchitecture.getTiles().get(0));
      Application dualCoreApplication = testDualApplication.getSampleApplication();
      ApplicationManagement.assignFifoMapping(dualCoreApplication,dualCoreArchitecture); 
  
      ModuloScheduler dualCoreScheduler = new ModuloScheduler();
      dualCoreScheduler.setApplication(dualCoreApplication);
      dualCoreScheduler.setArchitecture(dualCoreArchitecture);

      dualCoreScheduler.setMaxIterations(5);
      dualCoreScheduler.calculateModuloSchedule();
      dualCoreScheduler.printKernelBody();
      dualCoreScheduler.findSchedule();
      dualCoreScheduler.schedule();

      System.out.println("Single iteration delay: "+dualCoreScheduler.getDelaySingleIteration());
      System.out.println("The MMI is: "+dualCoreScheduler.getMII());

      dualCoreArchitecture.getTiles().get(0).getProcessors().get(0).getScheduler().saveScheduleStats(".");
      dualCoreArchitecture.getTiles().get(0).getProcessors().get(1).getScheduler().saveScheduleStats(".");
      dualCoreArchitecture.getTiles().get(0).getCrossbar().saveCrossbarUtilizationStats(".");

      // dumping memory utilization
      dualCoreArchitecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().saveMemoryUtilizationStats(".");
      dualCoreArchitecture.getTiles().get(0).getProcessors().get(1).getLocalMemory().saveMemoryUtilizationStats(".");
      dualCoreArchitecture.getTiles().get(0).getTileLocalMemory().saveMemoryUtilizationStats(".");

      System.out.println("Testing dualcore implementation testcase done and modulo scheduling!");

      System.out.println("Testing quadcore implementation testcase!");

      ActorManagement.resetCounters();
      FifoManagement.resetCounters();
      ArchitectureManagement.resetCounters();

      Architecture architecture = new Architecture("architecture","ModuloSchedulingQuad", 4, 1.0, 2);
      // set the memory sizes
      architecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().setCapacity(1000000);
      architecture.getTiles().get(0).getProcessors().get(1).getLocalMemory().setCapacity(2000000);
      architecture.getTiles().get(0).getProcessors().get(2).getLocalMemory().setCapacity(2000000);
      architecture.getTiles().get(0).getProcessors().get(3).getLocalMemory().setCapacity(2000000);

      TestApplicationQuadCoreMemoryBound sampleApplication = new TestApplicationQuadCoreMemoryBound(architecture.getTiles().get(0));  
      Application app = sampleApplication.getSampleApplication();
      ApplicationManagement.assignFifoMapping(app,architecture); 

      for(Map.Entry<Integer,Fifo> f : app.getFifos().entrySet()){
        System.out.println("Fifo "+f.getValue().getName()+" mapped to proc "+f.getValue().getMapping().getEmbeddedToProcessor().getName()+" on memory "+f.getValue().getMapping().getName());
      }

      ModuloScheduler scheduler = new ModuloScheduler();
      scheduler.setApplication(app);
      scheduler.setArchitecture(architecture);
			
      scheduler.setMaxIterations(5);
      scheduler.calculateModuloSchedule();
      //scheduler.printKernelBody();
      scheduler.findSchedule();
      scheduler.schedule();

      System.out.println("Single iteration delay: "+scheduler.getDelaySingleIteration());

      System.out.println("The MMI is: "+scheduler.getMII());
      
      for(HashMap.Entry<Integer,Processor> p: architecture.getTiles().get(0).getProcessors().entrySet()){
        p.getValue().getScheduler().saveScheduleStats(".");
      }
      architecture.getTiles().get(0).getCrossbar().saveCrossbarUtilizationStats(".");

      // dumping memory utilization
      architecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().saveMemoryUtilizationStats(".");
      architecture.getTiles().get(0).getProcessors().get(1).getLocalMemory().saveMemoryUtilizationStats(".");
      architecture.getTiles().get(0).getProcessors().get(2).getLocalMemory().saveMemoryUtilizationStats(".");
      architecture.getTiles().get(0).getProcessors().get(3).getLocalMemory().saveMemoryUtilizationStats(".");
      architecture.getTiles().get(0).getTileLocalMemory().saveMemoryUtilizationStats(".");

      System.out.println("Testing quadcore implementation testcase done!");
    }
}

