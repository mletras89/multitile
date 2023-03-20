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
import multitile.mapping.Bindings;
import multitile.application.Application;
import multitile.application.ApplicationManagement;

import java.io.*;

public class testCompositeChannel {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing composite channel Implementation!");

      Architecture architecture = new Architecture("Arch","Tile_testComposite",1,1.0,2);
      Tile t1 = architecture.getTiles().get(0); 
      
      Bindings bindings = new Bindings();
      TestApplication sampleApplication = new TestApplication(t1,bindings);  
      Application app = sampleApplication.getSampleApplication();

      FCFS scheduler = new FCFS();
      scheduler.setApplication(app);
      scheduler.setArchitecture(architecture);

      scheduler.setMaxIterations(3);
      scheduler.schedule(bindings,null);

      architecture.getTiles().get(0).getProcessors().get(0).getScheduler().saveScheduleStats(".");
      architecture.getTiles().get(0).getCrossbar().saveCrossbarUtilizationStats(".");

      // merge all the multicast actors in the application
      ApplicationManagement.setAllMulticastActorsAsMergeable(app);
      ApplicationManagement.collapseMergeableMulticastActors(app,bindings);
      app.resetApplication();
      architecture.resetArchitecture();
      
      architecture.getTiles().get(0).setName("Tile_testCompositeAfterMerging");

      scheduler.setApplication(app);
      scheduler.setArchitecture(architecture);
      scheduler.setMaxIterations(3);
      scheduler.schedule(bindings,null);

      architecture.getTiles().get(0).getProcessors().get(0).getScheduler().saveScheduleStats(".");
      architecture.getTiles().get(0).getCrossbar().saveCrossbarUtilizationStats(".");

      System.out.println("Testing composite channel Implementation done!");
    }
}

