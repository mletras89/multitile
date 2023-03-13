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
  @date  15 November 2022
  @version 1.1
  @ brief
        This class implements methods for fifo management such as merging
        the fifos
 
 
--------------------------------------------------------------------------
*/
package multitile.application;

import java.util.*;
import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.application.CompositeFifo;

public class FifoManagement{
  private static int fifoIdCounter;
  private static int compositeCounter;

  static{
    fifoIdCounter=1;
    compositeCounter = 0;
  }

  public static void resetCounters(){
    fifoIdCounter=1;
    compositeCounter = 0;
  }

  public static int getCompositeCounter(){
    return compositeCounter++;
  }

  public static int getFifoId(){
    return fifoIdCounter++;
  }

  public static CompositeFifo createCompositeChannel(Fifo writer,List<Fifo> readerFifos, Actor multicastActor){
    // create a composite channel from a given list of fifos
    // a composite actor has only one writer and multiple readers
    //
    // the capacity of the composite is the addition of the capacity of the writer and the max capacity of readers
    int capacityWriter = writer.get_capacity();
    int capacityReader = 0;

    for(Fifo fifo : readerFifos){
      if(fifo.get_capacity() > capacityReader)
        capacityReader = fifo.get_capacity();
    }
    // updating fifos capacities of readers
    for(Fifo fifo : readerFifos){
      fifo.set_capacity(capacityWriter+capacityReader);
    }

    // when doing the composite channel, we take the mapping of the writer
           
    CompositeFifo compositeFifo = new CompositeFifo("compositeFifo_"+getCompositeCounter(),writer.get_tokens(),capacityWriter+capacityReader,writer.getTokenSize(),writer.getMapping(),writer.getConsRate(),writer.getProdRate(),writer.getSource(),readerFifos,multicastActor);

    return compositeFifo;
  }

}

