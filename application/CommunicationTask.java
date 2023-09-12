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
     Actor that can be mapped to any processor which is part of an application
--------------------------------------------------------------------------
*/
package multitile.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;

import multitile.Transfer;
import multitile.architecture.Crossbar;
import multitile.architecture.LocalMemory;
import multitile.architecture.NoC;
import multitile.scheduler.PassTransferOverArchitecture;

public class CommunicationTask extends Actor{

	private Fifo fifo;
	private Fifo fifoFromMRB = null;
	private int discretizedRuntime;
	private double runtime;
	private Transfer transfer = null;
	private Queue<PassTransferOverArchitecture> usedInterconnects;
	
	private ArrayList<Crossbar> usedCrossbar;
	private NoC usedNoC = null;
	private LocalMemory usedLocalMemory = null;
	
	public CommunicationTask(
             String name,
             int priority,
             int inputs, 
             int outputs){
		 super(name,priority,inputs,outputs);
      
	}

	public CommunicationTask(Actor another){
		 super(another);
  
	}
	
	public LocalMemory getUsedLocalMemory() {
		return this.usedLocalMemory;
	}
	
	public NoC getUsedNoc() { return this.usedNoC;};
	
	public  ArrayList<Crossbar> getUsedCrossbars(){
		return this.usedCrossbar;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
	public void setRuntimeFromInterconnects(double scaleFactor) {
		// this method set the runtime and the interconnects
		assert this.usedInterconnects != null : "Must calculate the pass beforehand...";
		this.runtime = 0;
		this.discretizedRuntime = 0 ;
		this.usedCrossbar = new ArrayList<>();
		
		for(PassTransferOverArchitecture p : this.usedInterconnects ) {
			if (p.getCrossbar() != null)
				usedCrossbar.add(p.getCrossbar());
			if (p.getNoC() != null)
				usedNoC = p.getNoC();
			if (p.getLocalMemory() != null)
				usedLocalMemory = p.getLocalMemory();
		}
		
		ArrayList<Double> transferTimes = new ArrayList<>();
		
		assert transfer != null : "Transfer must be different to null";
		
		for(Crossbar c : usedCrossbar) {
			transferTimes.add(c.calculateTransferTime(this.transfer));
		}
		if (this.usedNoC != null)
			transferTimes.add( usedNoC.calculateTransferTime(this.transfer));
		
		if (transferTimes.size() > 0) {
			double lengthTransfer = Collections.max(transferTimes);
			this.runtime = lengthTransfer;
			this.discretizedRuntime = (int)Math.ceil(lengthTransfer/scaleFactor);
			if (this.discretizedRuntime < 1)
				this.discretizedRuntime = 1;
		}
		
	}
  
	public CommunicationTask(String name){
		 super(name);
	}
	
	public void setFifo(Fifo fifo) {
		this.fifo =fifo;
	}
	
	public Fifo getFifo() {
		return fifo;
	}
	
	public void setDiscretizedRuntime(int discretizedRuntime) {
		this.discretizedRuntime = discretizedRuntime;
	}
	
	public int getDiscretizedRuntime() {
		return this.discretizedRuntime;
	}
	
	public void setRuntime(double runtime) {
		this.runtime = runtime;
	}
	
	public double getRuntime() {
		return this.runtime;
	}
	
	public Queue<PassTransferOverArchitecture> getUsedInterconnects(){
		return this.usedInterconnects;
	}
	
	public void setUsedInterconnects(Queue<PassTransferOverArchitecture> interconnects) {
		this.usedInterconnects = interconnects;
	}
	
	public void setTransfer(Transfer transfer) {
		this.transfer = transfer;
	}
	
	public Transfer getTransfer() {
		return this.transfer;
	}

	public Fifo getFifoFromMRB() {
		return fifoFromMRB;
	}

	public void setFifoFromMRB(Fifo _fifoFromMRB) {
		fifoFromMRB = _fifoFromMRB;
	}

}
