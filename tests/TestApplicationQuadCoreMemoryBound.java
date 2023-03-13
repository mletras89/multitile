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
  @date 28 November 2022
  @version 1.1
  @ brief
	Sample sobel test application used for testing, mapped to a quad
        core architecture with bound memories
--------------------------------------------------------------------------
*/

package multitile.tests;

import multitile.architecture.Tile;
import multitile.architecture.Memory;
import multitile.architecture.LocalMemory;
import multitile.architecture.TileLocalMemory;
import multitile.architecture.Processor;
import multitile.architecture.GlobalMemory;

import multitile.application.Application;
import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.application.CompositeFifo;
import multitile.application.FifoManagement;
import multitile.application.ApplicationManagement;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;

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

public class TestApplicationQuadCoreMemoryBound{
  private Application sampleApplication;

  public TestApplicationQuadCoreMemoryBound(Tile t1,Tile t2, GlobalMemory globalMemory){
      TileLocalMemory tileLocalMemory1 	= t1.getTileLocalMemory();
      LocalMemory tile1LocalMemory1 	= t1.getProcessors().get(0).getLocalMemory();
      LocalMemory tile1LocalMemory2 	= t1.getProcessors().get(1).getLocalMemory();

      TileLocalMemory tileLocalMemory2 	= t2.getTileLocalMemory();
      LocalMemory tile2LocalMemory1 	= t2.getProcessors().get(2).getLocalMemory();
      LocalMemory tile2LocalMemory2 	= t2.getProcessors().get(3).getLocalMemory();


      Processor cpu1 = t1.getProcessors().get(0);
      Processor cpu2 = t1.getProcessors().get(1);

      Processor cpu3 = t2.getProcessors().get(2);
      Processor cpu4 = t2.getProcessors().get(3);

      Actor a1 = new Actor("a1");
      a1.setId(1) ;
      a1.setExecutionTime(10000);
      a1.setInputs(0);
      a1.setOutputs(1);
      a1.setMapping(cpu1);
      a1.setMappingToTile(cpu1.getOwnerTile());

      Actor a2 = new Actor("a2");  // is a multicast actor
      a2.setId(2) ;
      a2.setExecutionTime(10000);
      a2.setInputs(1);
      a2.setOutputs(2);
      a2.setMapping(cpu2);
      a2.setMappingToTile(cpu2.getOwnerTile());


      Actor a3 = new Actor("a3");
      a3.setId(3) ;
      a3.setExecutionTime(10000);
      a3.setInputs(1);
      a3.setOutputs(1);
      a3.setMapping(cpu3);
      a3.setMappingToTile(cpu3.getOwnerTile());

      Actor a4 = new Actor("a4");
      a4.setId(4) ;
      a4.setExecutionTime(10000);
      a4.setInputs(1);
      a4.setOutputs(1);
      a4.setMapping(cpu3);
      a4.setMappingToTile(cpu3.getOwnerTile());

      Actor a5 = new Actor("a5:sink");
      a5.setId(5) ;
      a5.setExecutionTime(10000);
      a5.setInputs(2);
      a5.setOutputs(0);
      a5.setMapping(cpu4);
      a5.setMappingToTile(cpu4.getOwnerTile());

      Fifo c1 = new Fifo("c1",0,1,1000000,1,1,a1,a2,FIFO_MAPPING_TYPE.TILE_LOCAL);  // channel connected to writer
      Fifo c2 = new Fifo("c2",0,1,1000000,1,1,a2,a3,FIFO_MAPPING_TYPE.GLOBAL);      // channels connected to readers
      Fifo c3 = new Fifo("c3",0,1,1000000,1,1,a2,a4,FIFO_MAPPING_TYPE.GLOBAL);      // channels connected to readers

      Fifo c4 = new Fifo("c4",0,1,1000000,1,1,a3,a5,FIFO_MAPPING_TYPE.SOURCE);
      Fifo c5 = new Fifo("c5",0,1,1000000,1,1,a4,a5,FIFO_MAPPING_TYPE.SOURCE);

      c1.setMappingToTile(t1);
      c2.setMappingToTile(t1);
      c3.setMappingToTile(t1);

      c4.setMappingToTile(t2);
      c5.setMappingToTile(t2);

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
      fifoMap.put(c1.getId(),c1);
      fifoMap.put(c2.getId(),c2);
      fifoMap.put(c3.getId(),c3);
      fifoMap.put(c4.getId(),c4);
      fifoMap.put(c5.getId(),c5);

      List<Actor> actors = Arrays.asList(a1,a2,a3,a4,a5);
      sampleApplication = new Application();

      sampleApplication.setActorsFromList(actors);
      sampleApplication.setFifos(fifoMap);
  }



  public TestApplicationQuadCoreMemoryBound(Tile t1){
      TileLocalMemory tileLocalMemory = t1.getTileLocalMemory();
      LocalMemory localMemory1 = t1.getProcessors().get(0).getLocalMemory();
      LocalMemory localMemory2 = t1.getProcessors().get(1).getLocalMemory();
      LocalMemory localMemory3 = t1.getProcessors().get(2).getLocalMemory();

      Processor cpu1 = t1.getProcessors().get(0);
      Processor cpu2 = t1.getProcessors().get(1);
      Processor cpu3 = t1.getProcessors().get(2);
      Processor cpu4 = t1.getProcessors().get(3);

      Actor a1 = new Actor("a1");
      a1.setId(1) ;
      a1.setExecutionTime(10000);
      a1.setInputs(0);
      a1.setOutputs(1);
      a1.setMapping(cpu1);
      a1.setMappingToTile(t1);

      Actor a2 = new Actor("a2");  // is a multicast actor
      a2.setId(2) ;
      a2.setExecutionTime(10000);
      a2.setInputs(1);
      a2.setOutputs(2);
      a2.setMapping(cpu2);
      a2.setMappingToTile(t1);


      Actor a3 = new Actor("a3");
      a3.setId(3) ;
      a3.setExecutionTime(10000);
      a3.setInputs(1);
      a3.setOutputs(1);
      a3.setMapping(cpu3);
      a3.setMappingToTile(t1);

      Actor a4 = new Actor("a4");
      a4.setId(4) ;
      a4.setExecutionTime(10000);
      a4.setInputs(1);
      a4.setOutputs(1);
      a4.setMapping(cpu3);
      a4.setMappingToTile(t1);

      Actor a5 = new Actor("a5:sink");
      a5.setId(5) ;
      a5.setExecutionTime(10000);
      a5.setInputs(2);
      a5.setOutputs(0);
      a5.setMapping(cpu4);
      a5.setMappingToTile(t1);

      Fifo c1 = new Fifo("c1",0,1,1000000,1,1,a1,a2,FIFO_MAPPING_TYPE.SOURCE);  // channel connected to writer
      Fifo c2 = new Fifo("c2",0,1,1000000,1,1,a2,a3,FIFO_MAPPING_TYPE.SOURCE);  // channels connected to readers
      Fifo c3 = new Fifo("c3",0,1,1000000,1,1,a2,a4,FIFO_MAPPING_TYPE.SOURCE);  // channels connected to readers
      Fifo c4 = new Fifo("c4",0,1,1000000,1,1,a3,a5,FIFO_MAPPING_TYPE.SOURCE);
      Fifo c5 = new Fifo("c5",0,1,1000000,1,1,a4,a5,FIFO_MAPPING_TYPE.SOURCE);

      c1.setMappingToTile(t1);
      c2.setMappingToTile(t1);
      c3.setMappingToTile(t1);
      c4.setMappingToTile(t1);
      c5.setMappingToTile(t1);

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
      fifoMap.put(c1.getId(),c1);
      fifoMap.put(c2.getId(),c2);
      fifoMap.put(c3.getId(),c3);
      fifoMap.put(c4.getId(),c4);
      fifoMap.put(c5.getId(),c5);

      List<Actor> actors = Arrays.asList(a1,a2,a3,a4,a5);
      sampleApplication = new Application();

      sampleApplication.setActorsFromList(actors);
      sampleApplication.setFifos(fifoMap);
  }

  public Application getSampleApplication(){
    return this.sampleApplication;
  }

}
