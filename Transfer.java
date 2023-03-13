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
  @date   02 November 2022
  @version 1.1
  @ brief
     This class describes a transfer to be scheduled in a crossbar or in the
     NoC. 
     A transfer can be:
        - READ
        - WRITE
     A transfer involves an actor that writes/reads and a channel to be 
     read/written.
     A transfer has an start_time and a due_time
--------------------------------------------------------------------------
*/
package multitile;

import multitile.application.Actor;
import multitile.application.Fifo;

public class Transfer {
  private double start_time;
  private double due_time;
  private double endOverall;
  // in a transfer, an actor and a Fifo are involved
  private Actor actor;
  private Fifo fifo;
  // depending on the type of the operation: {READ, WRITE}
  private TRANSFER_TYPE type;

  private int step;

  public static enum TRANSFER_TYPE {
          READ,
          WRITE
  }
  private boolean occupied;

  public Transfer(Actor actor,Fifo fifo) {
      this.setOccupied(false);
      this.setStart_time(0.0);
      this.setDue_time(0.0);
      this.setType(TRANSFER_TYPE.WRITE);
      this.fifo = fifo;
      this.actor = actor;
      this.step = 0;
  }

  public Transfer(Actor actor,Fifo fifo,Double startTime, TRANSFER_TYPE typeTransfer) {
      this.setOccupied(false);
      this.setStart_time(startTime);
      this.setDue_time(0.0);
      this.setType(typeTransfer);
      this.fifo = fifo;
      this.actor = actor;
      this.step = 0;
  }


  public Transfer(Transfer other) {
    this.setOccupied(other.isOccupied());
    this.setStart_time(other.getStart_time());
    this.setDue_time(other.getDue_time());
    this.setActor(other.getActor());
    this.setFifo(other.getFifo());
    this.setType(other.getType());
    this.setStep(other.getStep());
  }

  public double getEndOverall(){
    return this.endOverall;
  }

  public void setEndOverall(double end){
    this.endOverall = end;
  }

  // Overriding toStrin() method of String class
  @Override
  public String toString(){
    return "Transfer: starts["+this.start_time+"] ends ["+this.due_time+"] actor ["+this.actor.getName()+"] fifo["+this.fifo.getName()+"] type["+this.type+"]";
  }

  public int getStep(){
    return this.step;
  }

  public void setStep(int step){
    this.step = step;
  }

  public boolean isOccupied() {
    return occupied;
  }

  public void setOccupied(boolean occupied) {
    this.occupied = occupied;
  }

  public double getStart_time() {
    return start_time;
  }
  
  public void setStart_time(double start_time) {
    this.start_time = start_time;
  }

  public double getDue_time() {
    return due_time;
  }
  
  public void setDue_time(double due_time) {
    this.due_time = due_time;
  }

  public int getBytes() {
    // here calculate the number of bytes in the transaction, depending on the type of operation
    if (this.type == TRANSFER_TYPE.READ)
      return fifo.getProdRate() * fifo.getTokenSize();
    if (this.type == TRANSFER_TYPE.WRITE)
      return fifo.getConsRate() * fifo.getTokenSize();
    return 0;
  }

  public Actor getActor() {
    return actor;
  }
  
  public void setActor(Actor actor) {
    this.actor = actor;
  }

  public Fifo getFifo() {
    return fifo;
  }
  
  public void setFifo(Fifo fifo) {
    this.fifo = fifo;
  }

  public TRANSFER_TYPE getType() {
    return type;
  }
  
  public void setType(TRANSFER_TYPE type) {
    this.type = type;
  }
    
}
