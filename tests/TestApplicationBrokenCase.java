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
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.mapping.Mappings;
import multitile.architecture.GlobalMemory;
import multitile.application.Application;
import multitile.application.Actor;
import multitile.application.Actor.ACTOR_TYPE;
import multitile.application.Fifo;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;

import java.util.*;

public class TestApplicationBrokenCase{
  private Application sampleApplication;

  public TestApplicationBrokenCase(Tile t1,Tile t2, GlobalMemory globalMemory, Bindings bindings, Mappings mappings){
      Actor t0_source = new Actor("t0_source");
      t0_source.setId(1) ;
      t0_source.setInputs(0);
      t0_source.setOutputs(1);
      
      Actor t3_hor = new Actor("t3_hor");  
      t3_hor.setId(2) ;
      t3_hor.setInputs(1);
      t3_hor.setOutputs(1);
      
      Actor t4_ver = new Actor("t4_ver");  
      t4_ver.setId(3) ;
      t4_ver.setInputs(1);
      t4_ver.setOutputs(1);
      
      Actor t7_broadcast = new Actor("t7_broadcast");  
      t7_broadcast.setId(4) ;
      t7_broadcast.setInputs(1);
      t7_broadcast.setOutputs(2);
      t7_broadcast.setType(ACTOR_TYPE.MULTICAST);
      
      Actor t5_mag = new Actor("t5_mag");  
      t5_mag.setId(5) ;
      t5_mag.setInputs(2);
      t5_mag.setOutputs(1);
      
      Actor t6_sink = new Actor("t6_sink");  
      t6_sink.setId(6) ;
      t6_sink.setInputs(1);
      t6_sink.setOutputs(0);
      
      Actor t1_grayscale = new Actor("t1_grayscale");  
      t1_grayscale.setId(7) ;
      t1_grayscale.setInputs(1);
      t1_grayscale.setOutputs(1);
      
      Actor t2_convert = new Actor("t2_convert");  
      t2_convert.setId(8) ;
      t2_convert.setInputs(1);
      t2_convert.setOutputs(1);      
      
      Fifo c1 = new Fifo("cf:top.t7_broadcast.o0->top.t4_ver.i0",0,1,1000000,1,1,t7_broadcast,t4_ver,FIFO_MAPPING_TYPE.TILE_LOCAL_DESTINATION);  // channel connected to writer
      Fifo c2 = new Fifo("cf:top.t7_broadcast.o1->top.t3_hor.i0",0,1,1000000,1,1,t7_broadcast,t3_hor,FIFO_MAPPING_TYPE.SOURCE);      // channels connected to readers
      Fifo c3 = new Fifo("cf:top.t2_convert.o0->top.t7_broadcast.i0",0,1,1000000,1,1,t2_convert,t7_broadcast,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);      // channels connected to readers
      Fifo c4 = new Fifo("cf:top.t1_grayscale.o0->top.t2_convert.i0",0,1,1000000,1,1,t1_grayscale,t2_convert,FIFO_MAPPING_TYPE.SOURCE);
      Fifo c5 = new Fifo("cf:top.t0_source.o0->top.t1_grayscale.i0",0,1,1000000,1,1,t0_source,t1_grayscale,FIFO_MAPPING_TYPE.GLOBAL);
      Fifo c6 = new Fifo("cf:top.t3_hor.o0->top.t5_mag.i0",0,1,1000000,1,1,t3_hor,t5_mag,FIFO_MAPPING_TYPE.GLOBAL);
      Fifo c7 = new Fifo("cf:top.t4_ver.o0->top.t5_mag.i1",0,1,1000000,1,1,t4_ver,t5_mag,FIFO_MAPPING_TYPE.TILE_LOCAL_DESTINATION);
      Fifo c8 = new Fifo("cf:top.t5_mag.o0->top.t6_sink.i0",0,1,1000000,1,1,t5_mag,t6_sink,FIFO_MAPPING_TYPE.TILE_LOCAL_DESTINATION);
      
      Vector<Fifo> v1 = new Vector<Fifo>();
      v1.addElement(c5);
      t0_source.setOutputFifos(v1);

      Vector<Fifo> v2 = new Vector<Fifo>();
      v2.addElement(new Fifo(c2));
      t3_hor.setInputFifos(v2);
      Vector<Fifo> v3 = new Vector<Fifo>();
      v3.addElement(new Fifo(c6));
      t3_hor.setOutputFifos(v3);

      Vector<Fifo> v4 = new Vector<Fifo>();
      v4.addElement(new Fifo(c1));
      t4_ver.setInputFifos(v4);
      Vector<Fifo> v5 = new Vector<Fifo>();
      v5.addElement(new Fifo(c7));
      t4_ver.setOutputFifos(v5);

      Vector<Fifo> v6 = new Vector<Fifo>();
      v6.addElement(new Fifo(c3));
      t7_broadcast.setInputFifos(v6);
      Vector<Fifo> v7 = new Vector<Fifo>();
      v7.addElement(new Fifo(c1));
      v7.addElement(new Fifo(c2));
      t7_broadcast.setOutputFifos(v7);

      Vector<Fifo> v8 = new Vector<Fifo>();
      v8.addElement(new Fifo(c6));
      v8.addElement(new Fifo(c7));
      t5_mag.setInputFifos(v8);
      Vector<Fifo> v9 = new Vector<Fifo>();
      v9.addElement(new Fifo(c8));
      t5_mag.setOutputFifos(v9);

      Vector<Fifo> v10 = new Vector<Fifo>();
      v10.addElement(new Fifo(c8));
      t6_sink.setInputFifos(v10);
      
      Vector<Fifo> v11 = new Vector<Fifo>();
      v11.addElement(new Fifo(c5));
      t1_grayscale.setInputFifos(v11);
      Vector<Fifo> v12 = new Vector<Fifo>();
      v12.addElement(new Fifo(c4));
      t1_grayscale.setOutputFifos(v12);
      
      Vector<Fifo> v13 = new Vector<Fifo>();
      v13.addElement(new Fifo(c4));
      t2_convert.setInputFifos(v13);
      Vector<Fifo> v14 = new Vector<Fifo>();
      v14.addElement(new Fifo(c3));
      t2_convert.setOutputFifos(v14);
      
      Map<Integer,Fifo> fifoMap = new HashMap<Integer,Fifo>();
      fifoMap.put(c1.getId(),c1);
      fifoMap.put(c2.getId(),c2);
      fifoMap.put(c3.getId(),c3);
      fifoMap.put(c4.getId(),c4);
      fifoMap.put(c5.getId(),c5);
      fifoMap.put(c3.getId(),c6);
      fifoMap.put(c4.getId(),c7);
      fifoMap.put(c5.getId(),c8);

      List<Actor> actors = Arrays.asList(t0_source,t3_hor,t4_ver,t7_broadcast,t5_mag,t6_sink,t1_grayscale,t2_convert);
      sampleApplication = new Application();
      sampleApplication.setActorsFromList(actors);
      sampleApplication.setFifos(fifoMap);
      
      // actor binding
      bindings.getActorTileBindings().put(t0_source.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(t3_hor.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(t4_ver.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(t7_broadcast.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(t5_mag.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(t6_sink.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(t1_grayscale.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(t2_convert.getId(), new Binding<Tile>(t1));

      bindings.getActorTileBindings().get(t0_source.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorTileBindings().get(t3_hor.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorTileBindings().get(t4_ver.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorTileBindings().get(t7_broadcast.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorTileBindings().get(t5_mag.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorTileBindings().get(t6_sink.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorTileBindings().get(t1_grayscale.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorTileBindings().get(t2_convert.getId()).getProperties().put("runtime", 10000.0);
  }

  public Application getSampleApplication(){
    return this.sampleApplication;
  }

}
