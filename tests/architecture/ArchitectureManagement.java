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
  @date  29 November 2022
  @version 1.1
  @ brief
        This class implements methods for architecture management
 
--------------------------------------------------------------------------
*/

package multitile.architecture;

import multitile.application.Fifo;
import java.util.*;

public class ArchitectureManagement{
  private static int processorIdCounter;
  private static int memoryIdCounter;
  private static int crossbarIdCounter;  
  private static int tileIdCounter;
  private static int nocIdCounter;  

  static{
    processorIdCounter = 0;
    memoryIdCounter = 0;
    crossbarIdCounter = 0;
    tileIdCounter = 0;
    nocIdCounter = 0;
  }

  public static void updateLastEventInProcessor(Architecture architecture, Processor processor, double time){
    double timeEvent = architecture.getTiles().get(processor.getOwnerTile().getId()).getProcessors().get(processor.getId()).getScheduler().getLastEventinProcessor(); 
    if (time>timeEvent)
      timeEvent = time;
    architecture.getTiles().get(processor.getOwnerTile().getId()).getProcessors().get(processor.getId()).getScheduler().setLastEventinProcessor(timeEvent);
  }

  public static Memory getMemoryToBeRelocated(Fifo fifo,Architecture architecture){
    Memory mappedMemory = fifo.getMapping();
    Tile mappedTile = fifo.getMappingToTile();
    Memory newMapping;
    switch(mappedMemory.getType()){
      case LOCAL_MEM:
        newMapping = architecture.getTiles().get(mappedTile.getId()).getTileLocalMemory();
        break;
      default:
        newMapping = architecture.getGlobalMemory();
        break;
    }
    return newMapping;
  }
  

  public static void resetCounters(){
    processorIdCounter = 0;
    memoryIdCounter = 0;
    crossbarIdCounter = 0;
    tileIdCounter = 0;
    nocIdCounter = 0;
  }

  public static int getTileId(){
    return tileIdCounter++;
  }

  public static int getProcessorId(){
    return processorIdCounter++;
  }

  public static int getMemoryId(){
    return memoryIdCounter++;
  }

  public static int getCrossbarId(){
    return crossbarIdCounter++;
  }

  public static int getNoCId(){
    return nocIdCounter++;
  }
}
