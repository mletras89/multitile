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
  @date   28 November 2022
  @version 1.1
  @ brief
     Example of a single tile architecture with a single processor with local
     memory, crossbar and tile local memory
--------------------------------------------------------------------------
*/
package multitile.tests;

import multitile.scheduler.FCFS;

import multitile.architecture.Architecture;
import multitile.architecture.Tile;
import multitile.architecture.Memory;
import multitile.architecture.LocalMemory;
import multitile.architecture.TileLocalMemory;
import multitile.architecture.Processor;

import multitile.application.Application;
import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.application.CompositeFifo;
import multitile.application.FifoManagement;
import multitile.application.ApplicationManagement;

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

public class testMemoryBoundQuadCore {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing quadcore implementation testcase!");

      Architecture architecture = new Architecture("architecture","Tile_testQuadCoreMemoryBound",4,1.0,2);

      // set the memory sizes
      architecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().setCapacity(2000000);
      architecture.getTiles().get(0).getProcessors().get(1).getLocalMemory().setCapacity(2000000);
      architecture.getTiles().get(0).getProcessors().get(2).getLocalMemory().setCapacity(2000000);
      architecture.getTiles().get(0).getProcessors().get(3).getLocalMemory().setCapacity(2000000);

      TestApplicationQuadCoreMemoryBound sampleApplication = new TestApplicationQuadCoreMemoryBound(architecture.getTiles().get(0));  
      Application app = sampleApplication.getSampleApplication();
      ApplicationManagement.assignFifoMapping(app,architecture); 

      FCFS scheduler = new FCFS();
      scheduler.setApplication(app);
      scheduler.setArchitecture(architecture);

      scheduler.setMaxIterations(10);
      scheduler.schedule();

      architecture.getTiles().get(0).getProcessors().get(0).getScheduler().saveScheduleStats(".");
      architecture.getTiles().get(0).getProcessors().get(1).getScheduler().saveScheduleStats(".");
      architecture.getTiles().get(0).getProcessors().get(2).getScheduler().saveScheduleStats(".");
      architecture.getTiles().get(0).getProcessors().get(3).getScheduler().saveScheduleStats(".");      
      architecture.getTiles().get(0).getCrossbar().saveCrossbarUtilizationStats(".");

      // print the memory utilization stats
      architecture.getTiles().get(0).getProcessors().get(0).getLocalMemory().saveMemoryUtilizationStats(".");
      architecture.getTiles().get(0).getProcessors().get(1).getLocalMemory().saveMemoryUtilizationStats(".");
      architecture.getTiles().get(0).getProcessors().get(2).getLocalMemory().saveMemoryUtilizationStats(".");
      architecture.getTiles().get(0).getProcessors().get(3).getLocalMemory().saveMemoryUtilizationStats(".");      

      System.out.println("Testing quadcore implementation testcase done!");
    }
}

