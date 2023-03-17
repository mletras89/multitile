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
  @date   16 November 2022
  @version 1.1
  @ brief
        Class application that encapsulates a set of actors communicated via 
	a set of fifos
--------------------------------------------------------------------------
*/
package multitile.application;

import java.util.*;

public class Application{
  private Map<Integer,Actor> actors;
  private Map<Integer,Fifo> fifos;

  public Application(){
    this.actors = new HashMap<>();
    this.fifos  = new HashMap<>();
  }

  public void resetApplication(){
    for(Map.Entry<Integer,Fifo> fifo : fifos.entrySet()){
      fifo.getValue().resetFifo();
    }
  }

  public void setActors(Map<Integer,Actor> actorsN){
    actors = actorsN;
  }

  public void setActorsFromList(List<Actor> actorsList){
    actors = new HashMap<>();
    for(Actor actor:actorsList){
      actors.put(actor.getId(),actor);
    }
  }

  public void setFifos(Map<Integer,Fifo> fifosN){
    fifos = fifosN;
  }

  public Map<Integer,Fifo> getFifos(){
    return fifos;
  }

  public Map<Integer,Actor> getActors(){
    return actors;
  }

  public List<Actor> getListActors(){
    List<Actor> listActors = new ArrayList<>();
//   System.out.println("Getting list ");
    for(Map.Entry<Integer,Actor> actor : actors.entrySet() ){
//      System.out.println("Adding: "+actor.getValue().getName());
      listActors.add(actor.getValue());
    }
//    System.out.println("done Getting list ");
    return listActors;
  }

  public void printActors(){
    for(Map.Entry<Integer,Actor> actorEntry : actors.entrySet()){   
      System.out.println("Actor:"+actorEntry.getValue().getName()+" is multicast:"+actorEntry.getValue().isMulticastActor()+" is mergeable: "+actorEntry.getValue().isMergeMulticast()+" mapped to "+actorEntry.getValue().getMapping().getName());
    }
  }

  public void printActorsShort(){
	for(Map.Entry<Integer,Actor> actorEntry : actors.entrySet()){   
	  System.out.println("Actor:"+actorEntry.getValue().getName()+" is multicast:"+actorEntry.getValue().isMulticastActor()+" is mergeable: "+actorEntry.getValue().isMergeMulticast());
	}
  }

  public void printFifos(){
    for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
      System.out.println("Fifo:"+fifoEntry.getValue().getName()+" is composite?:"+fifoEntry.getValue().isCompositeChannel()+" mapped to: "+fifoEntry.getValue().getMapping().getName());
    }
  }

  public void printFifosShort(){
	for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
	  System.out.println("Fifo:"+fifoEntry.getValue().getName()+" is composite?:"+fifoEntry.getValue().isCompositeChannel());
	}
  }
  
  
  public void printFifosState(){
    for(Map.Entry<Integer,Fifo> fifoEntry : fifos.entrySet()){
      System.out.println("Fifo: "+fifoEntry.getValue().getName()+" contains tokens: "+fifoEntry.getValue().get_tokens());
      System.out.println("\t"+fifoEntry.getValue().getTimeProducedToken());
    }
  }


}


