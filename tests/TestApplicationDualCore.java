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
  @date  27 November 2022
  @version 1.1
  @ brief
	Sample sobel test application used for testing, mapped to a dual
        core architecture 
--------------------------------------------------------------------------
*/

package multitile.tests;

import multitile.architecture.Tile;
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.architecture.Processor;

import multitile.application.Application;
import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;

import java.util.*;

public class TestApplicationDualCore{
  private Application sampleApplication;

  public TestApplicationDualCore(Tile t1, Bindings bindings){
      Processor cpu1 = t1.getProcessors().get(0);
      Processor cpu2 = t1.getProcessors().get(1);

      Actor a1 = new Actor("a1");
      a1.setId(1) ;
      a1.setInputs(0);
      a1.setOutputs(1);
      
      Actor a2 = new Actor("a2");  // is a multicast actor
      a2.setId(2) ;
      a2.setInputs(1);
      a2.setOutputs(2);

      Actor a3 = new Actor("a3");
      a3.setId(3) ;
      a3.setInputs(1);
      a3.setOutputs(1);

      Actor a4 = new Actor("a4");
      a4.setId(4) ;
      a4.setInputs(1);
      a4.setOutputs(1);

      Actor a5 = new Actor("a5:sink");
      a5.setId(5) ;
      a5.setInputs(2);
      a5.setOutputs(0);

      Fifo c1 = new Fifo("c1",0,1,1000000,1,1,a1,a2,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);  // channel connected to writer
      Fifo c2 = new Fifo("c2",0,1,1000000,1,1,a2,a3,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);  // channels connected to readers
      Fifo c3 = new Fifo("c3",0,1,1000000,1,1,a2,a4,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);  // channels connected to readers
      Fifo c4 = new Fifo("c4",0,1,1000000,1,1,a3,a5,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
      Fifo c5 = new Fifo("c5",0,1,1000000,1,1,a4,a5,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);

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

      // actor binding
      bindings.getActorProcessorBindings().put(a1.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(a2.getId(), new Binding<Processor>(cpu2));
      bindings.getActorProcessorBindings().put(a3.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(a4.getId(), new Binding<Processor>(cpu2));
      bindings.getActorProcessorBindings().put(a5.getId(), new Binding<Processor>(cpu1));
      
      bindings.getActorTileBindings().put(a1.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(a2.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(a3.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(a4.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(a5.getId(), new Binding<Tile>(t1));
      
      bindings.getActorProcessorBindings().get(a1.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(a2.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(a3.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(a4.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(a5.getId()).getProperties().put("runtime", 10000.0);
      
      
  }

  public Application getSampleApplication(){
    return this.sampleApplication;
  }

}
