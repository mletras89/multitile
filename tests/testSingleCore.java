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

public class testSingleCore {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing Single Core Implementation!");

      Tile t1 = new Tile("Tile1",1,1.0,4);
      Memory memory1 = t1.getTileLocalMemory();
      Processor cpu1 = t1.getProcessors().get(0);

      Actor a1 = new Actor("a1");
      a1.setId(1) ;
      a1.setExecutionTime(10000);
      a1.setInputs(0);
      a1.setOutputs(1);
      a1.setMapping(cpu1);

      Actor a2 = new Actor("a2");
      a2.setId(2) ;
      a2.setExecutionTime(10000);
      a2.setInputs(1);
      a2.setOutputs(2);
      a2.setMapping(cpu1);

      Actor a3 = new Actor("a3");
      a3.setId(3) ;
      a3.setExecutionTime(10000);
      a3.setInputs(1);
      a3.setOutputs(1);
      a3.setMapping(cpu1);

      Actor a4 = new Actor("a4");
      a4.setId(4) ;
      a4.setExecutionTime(10000);
      a4.setInputs(1);
      a4.setOutputs(1);
      a4.setMapping(cpu1);

      Actor a5 = new Actor("a5:sink");
      a5.setId(5) ;
      a5.setExecutionTime(10000);
      a5.setInputs(2);
      a5.setOutputs(0);
      a5.setMapping(cpu1);

      Fifo c1 = new Fifo("c1",0,1,1000000,memory1,1,1,a1,a2);
      Fifo c2 = new Fifo("c2",0,1,1000000,memory1,1,1,a2,a3);
      Fifo c3 = new Fifo("c3",0,1,1000000,memory1,1,1,a2,a4);
      Fifo c4 = new Fifo("c4",0,1,1000000,memory1,1,1,a3,a5);
      Fifo c5 = new Fifo("c5",0,1,1000000,memory1,1,1,a4,a5);

      Vector<Fifo> v1 = new Vector<Fifo>();
      v1.addElement(c1);
      a1.setOutputFifos(v1);

      Vector<Fifo> v2 = new Vector<Fifo>();
      v2.addElement(new Fifo(c1));
      a2.setInputFifos(v2);

      Vector<Fifo> v3 = new Vector<Fifo>();
      v3.addElement(new Fifo(c2));
      v3.addElement(new Fifo(c3));
      a2.setOutputFifos(v3);

      Vector<Fifo> v4 = new Vector<Fifo>();
      v4.addElement(new Fifo(c2));
      a3.setInputFifos(v4);

      Vector<Fifo> v5 = new Vector<Fifo>();
      v5.addElement(new Fifo(c4));
      a3.setOutputFifos(v5);

      Vector<Fifo> v6 = new Vector<Fifo>();
      v6.addElement(new Fifo(c3));
      a4.setInputFifos(v6);

      Vector<Fifo> v7 = new Vector<Fifo>();
      v7.addElement(new Fifo(c5));
      a4.setOutputFifos(v7);

      Vector<Fifo> v8 = new Vector<Fifo>();
      v8.addElement(new Fifo(c4));
      v8.addElement(new Fifo(c5));
      a5.setInputFifos(v8);



      Map<Integer,Fifo> fifoMap = new HashMap<Integer,Fifo>();
      fifoMap.put(1,c1);
      fifoMap.put(2,c2);
      fifoMap.put(3,c3);
      fifoMap.put(4,c4);
      fifoMap.put(5,c5);

      List<Actor> actors = Arrays.asList(a1,a2,a3,a4,a5);

      t1.setTotalIterations(1);
      t1.runTileActors(actors,fifoMap);
      t1.getProcessors().get(0).getScheduler().saveScheduleStats(".");
      t1.getCrossbar().saveCrossbarUtilizationStats(".");
      System.out.println("Testing Single Core Implementation done!");
    }
}

