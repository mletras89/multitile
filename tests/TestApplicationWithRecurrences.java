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

import multitile.architecture.Architecture;
import multitile.architecture.Tile;
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.mapping.Mapping;
import multitile.mapping.Mappings;
import multitile.architecture.Processor;
import multitile.architecture.GlobalMemory;

import multitile.application.Application;
import multitile.application.Actor;
import multitile.application.Actor.ACTOR_TYPE;
import multitile.application.Fifo;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;

import java.util.*;

public class TestApplicationWithRecurrences{
  private Application sampleApplication;

  public TestApplicationWithRecurrences(Tile t1, GlobalMemory globalMemory, Bindings bindings, Mappings mappings){
      // one tile with single processor
      Processor cpu1 = t1.getProcessors().get(0);

      Actor v1 = new Actor("v1");
      v1.setId(1) ;
      v1.setInputs(0);
      v1.setOutputs(2);
      
      Actor v2 = new Actor("v2");  // is a multicast actor
      v2.setId(2) ;
      v2.setInputs(2);
      v2.setOutputs(1);
      
      Actor v3 = new Actor("v3");
      v3.setId(3) ;
      v3.setInputs(1);
      v3.setOutputs(1);
      
      Actor v4 = new Actor("v4");
      v4.setId(4) ;
      v4.setInputs(1);
      v4.setOutputs(1);
      
      Actor v5 = new Actor("v5");
      v5.setId(5) ;
      v5.setInputs(2);
      v5.setOutputs(1);

      Actor v6 = new Actor("v6");
      v6.setId(6);
      v6.setInputs(1);
      v6.setOutputs(1);

      Actor v7 = new Actor("v7");
      v7.setId(7);
      v7.setInputs(2);
      v7.setOutputs(1);
      
      Fifo c1 = new Fifo("c1",0,1,1000000,1,1,v1,v2,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE); 
      Fifo c2 = new Fifo("c2",0,1,1000000,1,1,v1,v3,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);      
      Fifo c3 = new Fifo("c3",0,1,1000000,1,1,v2,v5,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);      
      Fifo c4 = new Fifo("c4",0,1,1000000,1,1,v3,v4,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
      Fifo c5 = new Fifo("c5",0,1,1000000,1,1,v4,v5,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
      Fifo c6 = new Fifo("c6",0,1,1000000,1,1,v5,v6,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
      Fifo c7 = new Fifo("c7",0,1,1000000,1,1,v6,v7,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
      Fifo c8 = new Fifo("c8",0,1,1000000,1,1,v4,v7,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
      Fifo c9 = new Fifo("c9",0,1,1000000,1,1,v7,v2,FIFO_MAPPING_TYPE.TILE_LOCAL_SOURCE);
      
      // outputs v1
      Vector<Fifo> vec1 = new Vector<Fifo>();
      vec1.addElement(new Fifo(c1));
      vec1.addElement(new Fifo(c2));
      v1.setOutputFifos(vec1);

      // inputs v2
      Vector<Fifo> vec2 = new Vector<Fifo>();
      vec2.addElement(new Fifo(c1));
      vec2.addElement(new Fifo(c9));
      v2.setInputFifos(vec2);
      
      // outputs v2
      Vector<Fifo> vec3 = new Vector<Fifo>();
      vec3.addElement(new Fifo(c3));
      v2.setOutputFifos(vec3);
      
      // inputs v3
      Vector<Fifo> vec4 = new Vector<Fifo>();
      vec4.addElement(new Fifo(c2));
      v3.setInputFifos(vec4);

      // outputs v3
      Vector<Fifo> vec5 = new Vector<Fifo>();
      vec5.addElement(new Fifo(c4));
      v3.setOutputFifos(vec5);

      // inputs v4
      Vector<Fifo> vec6 = new Vector<Fifo>();
      vec6.addElement(new Fifo(c4));
      v4.setInputFifos(vec6);

      // outputs v4
      Vector<Fifo> vec7 = new Vector<Fifo>();
      vec7.addElement(new Fifo(c5));
      vec7.addElement(new Fifo(c8));
      v4.setOutputFifos(vec7);

      // inputs v5
      Vector<Fifo> vec8 = new Vector<Fifo>();
      vec8.addElement(new Fifo(c3));
      vec8.addElement(new Fifo(c5));
      v5.setInputFifos(vec8);

      // outputs v5
      Vector<Fifo> vec9 = new Vector<Fifo>();
      vec9.addElement(new Fifo(c6));
      v5.setOutputFifos(vec9);

      // inputs v6
      Vector<Fifo> vec10 = new Vector<Fifo>();
      vec10.addElement(new Fifo(c6));
      v6.setInputFifos(vec10);

      // outputs v6
      Vector<Fifo> vec11 = new Vector<Fifo>();
      vec11.addElement(new Fifo(c7));
      v6.setOutputFifos(vec11);

      // inputs v7
      Vector<Fifo> vec12 = new Vector<Fifo>();
      vec12.addElement(new Fifo(c7));
      vec12.addElement(new Fifo(c8));
      v7.setInputFifos(vec12);

      // outputs v7
      Vector<Fifo> vec13 = new Vector<Fifo>();
      vec13.addElement(new Fifo(c9));
      v7.setOutputFifos(vec13);

      Map<Integer,Fifo> fifoMap = new HashMap<Integer,Fifo>();
      fifoMap.put(c1.getId(),c1);
      fifoMap.put(c2.getId(),c2);
      fifoMap.put(c3.getId(),c3);
      fifoMap.put(c4.getId(),c4);
      fifoMap.put(c5.getId(),c5);
      fifoMap.put(c6.getId(),c6);
      fifoMap.put(c7.getId(),c7);
      fifoMap.put(c8.getId(),c8);
      fifoMap.put(c9.getId(),c9);

      List<Actor> actors = Arrays.asList(v1,v2,v3,v4,v5,v6,v7);
      sampleApplication = new Application();

      sampleApplication.setActorsFromList(actors);
      sampleApplication.setFifos(fifoMap);
      
      // actor mappings
      HashMap<Integer,Mapping<Processor>> a1Mappings = new HashMap<>();
      a1Mappings.put(cpu1.getId(), new Mapping<Processor>( cpu1 ));
      a1Mappings.get(cpu1.getId()).getProperties().put("runtime", 10000.0);
      mappings.getActorProcessorMappings().put(v1.getId(), a1Mappings);
      
      HashMap<Integer,Mapping<Processor>> a2Mappings = new HashMap<>();
      a2Mappings.put(cpu1.getId(), new Mapping<Processor>( cpu1));
      a2Mappings.get(cpu1.getId()).getProperties().put("runtime", 10000.0);
      mappings.getActorProcessorMappings().put(v2.getId(), a2Mappings);
      
      HashMap<Integer,Mapping<Processor>> a3Mappings = new HashMap<>();
      a3Mappings.put(cpu1.getId(), new Mapping<Processor>(cpu1));
      a3Mappings.get(cpu1.getId()).getProperties().put("runtime", 10000.0);
      mappings.getActorProcessorMappings().put(v3.getId(), a3Mappings);
      
      HashMap<Integer,Mapping<Processor>> a4Mappings = new HashMap<>();
      a4Mappings.put(cpu1.getId(), new Mapping<Processor>(cpu1));
      a4Mappings.get(cpu1.getId()).getProperties().put("runtime", 10000.0);
      mappings.getActorProcessorMappings().put(v4.getId(), a4Mappings);
      
      HashMap<Integer,Mapping<Processor>> a5Mappings = new HashMap<>();
      a5Mappings.put(cpu1.getId(), new Mapping<Processor>(cpu1));
      a5Mappings.get(cpu1.getId()).getProperties().put("runtime", 10000.0);
      mappings.getActorProcessorMappings().put(v5.getId(), a5Mappings);

      HashMap<Integer,Mapping<Processor>> a6Mappings = new HashMap<>();
      a6Mappings.put(cpu1.getId(), new Mapping<Processor>(cpu1));
      a6Mappings.get(cpu1.getId()).getProperties().put("runtime", 10000.0);
      mappings.getActorProcessorMappings().put(v6.getId(), a6Mappings);

      HashMap<Integer,Mapping<Processor>> a7Mappings = new HashMap<>();
      a7Mappings.put(cpu1.getId(), new Mapping<Processor>(cpu1));
      a7Mappings.get(cpu1.getId()).getProperties().put("runtime", 10000.0);
      mappings.getActorProcessorMappings().put(v7.getId(), a7Mappings);
      
      // tile mappings
      HashMap<Integer,Mapping<Tile>> a1TMappings = new HashMap<>();
      a1TMappings.put(t1.getId(), new Mapping<Tile>(t1));
      mappings.getActorTileMappings().put(v1.getId(), a1TMappings);
      
      HashMap<Integer,Mapping<Tile>> a2TMappings = new HashMap<>();
      a2TMappings.put(t1.getId(), new Mapping<Tile>(t1));
      mappings.getActorTileMappings().put(v2.getId(), a2TMappings);
      
      HashMap<Integer,Mapping<Tile>> a3TMappings = new HashMap<>();
      a3TMappings.put(t1.getId(), new Mapping<Tile>(t1));
      mappings.getActorTileMappings().put(v3.getId(), a3TMappings);
      
      HashMap<Integer,Mapping<Tile>> a4TMappings = new HashMap<>();
      a4TMappings.put(t1.getId(), new Mapping<Tile>(t1));
      mappings.getActorTileMappings().put(v4.getId(), a4TMappings);
      
      HashMap<Integer,Mapping<Tile>> a5TMappings = new HashMap<>();
      a5TMappings.put(t1.getId(), new Mapping<Tile>(t1));
      mappings.getActorTileMappings().put(v5.getId(), a5TMappings);

      HashMap<Integer,Mapping<Tile>> a6TMappings = new HashMap<>();
      a6TMappings.put(t1.getId(), new Mapping<Tile>(t1));
      mappings.getActorTileMappings().put(v6.getId(), a6TMappings);

      HashMap<Integer,Mapping<Tile>> a7TMappings = new HashMap<>();
      a7TMappings.put(t1.getId(), new Mapping<Tile>(t1));
      mappings.getActorTileMappings().put(v7.getId(), a7TMappings);
      
      // actor binding
      bindings.getActorProcessorBindings().put(v1.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(v2.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(v3.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(v4.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(v5.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(v6.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(v7.getId(), new Binding<Processor>(cpu1));
      
      bindings.getActorTileBindings().put(v1.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(v2.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(v3.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(v4.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(v5.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(v6.getId(), new Binding<Tile>(t1));
      bindings.getActorTileBindings().put(v7.getId(), new Binding<Tile>(t1));
      
      bindings.getActorProcessorBindings().get(v1.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(v2.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(v3.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(v4.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(v5.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(v6.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(v7.getId()).getProperties().put("runtime", 10000.0);     
  }

  public Application getSampleApplication(){
    return this.sampleApplication;
  }

}
