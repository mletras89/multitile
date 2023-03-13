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
  @date   04 November 2022
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
import multitile.architecture.Processor;

import multitile.application.Application;
import multitile.application.Actor;
import multitile.application.Fifo;

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

public class testWriteReadTransfers {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing testWriteReadTransfers!");
      
      Architecture architecture = new Architecture("Arch","TileReadWrite",1,1.0,2); 
      Tile t1 = architecture.getTiles().get(0);
      Memory memory1 = t1.getTileLocalMemory();
      Processor cpu1 = t1.getProcessors().get(0);

      Actor a1 = new Actor("a1");
      a1.setId(1) ;
      a1.setExecutionTime(10000);
      a1.setInputs(0);
      a1.setOutputs(1);
      a1.setMapping(cpu1);

      Actor a5 = new Actor("a5:sink");
      a5.setId(5) ;
      a5.setExecutionTime(10000);
      a5.setInputs(1);
      a5.setOutputs(0);
      a5.setMapping(cpu1);

      Fifo c1 = new Fifo("c1",0,1,1000000,memory1,1,1,a1,a5);

      Vector<Fifo> v1 = new Vector<Fifo>();
      v1.addElement(c1);
      a1.setOutputFifos(v1);

      Vector<Fifo>  v8 = new Vector<Fifo>();
      v8.addElement(c1);
      a5.setInputFifos(v8);

      Map<Integer,Fifo> fifoMap = new HashMap<Integer,Fifo>();
      fifoMap.put(1,c1);

      List<Actor> actors = Arrays.asList(a1,a5);

      Application application = new Application();
      application.setActorsFromList(actors);
      application.setFifos(fifoMap);

      FCFS scheduler = new FCFS();
      scheduler.setApplication(application);
      scheduler.setArchitecture(architecture);

      scheduler.setMaxIterations(10);
      scheduler.schedule();

      architecture.getTiles().get(0).getProcessors().get(0).getScheduler().saveScheduleStats(".");
      architecture.getTiles().get(0).getCrossbar().saveCrossbarUtilizationStats(".");

      System.out.println("Testing testWriteReadTransfers done!");
    }
}

