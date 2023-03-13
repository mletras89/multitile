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
     This class describes an action to be executed in a Processor
--------------------------------------------------------------------------
*/
package multitile;

import multitile.application.Actor;

public class Action {
  private double start_time;
  private double due_time;
  private Actor actor;
  private int step;

  public Action(Actor actor) {
      this.setStart_time(0.0);
      this.setDue_time(0.0);
      this.actor = actor;
      this.step = 0;
  }
  
  public Action(Action other) {
    this.setStart_time(other.getStart_time());
    this.setDue_time(other.getDue_time());
    this.setActor(other.getActor());
    this.setStep(other.getStep());
  }

  public int getStep(){
    return this.step;
  }

  public void setStep(int step){
    this.step = step;
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
  
  
  public double getProcessing_time() {
      return actor.getExecutionTime();
  }

  public Actor getActor(){
    return this.actor;
  }    

  public void setActor(Actor actor){
    this.actor = actor;
  }
   
}
