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
  @date 10 of April 2023
  @version 1.1
  @ brief
        This class saves the actors belonging to a cycle, the src must be specified
--------------------------------------------------------------------------
*/

package multitile.application;

import java.util.*;
import multitile.application.Actor;
import multitile.application.Application;

public class Cycles{
        private ArrayList<Cycle> cycles; 
	private int recII;
	public Cycles(){
		cycles = new ArrayList<>();
	}

	public int getRecII(){
		return recII;
	}

	public void clearCycles(){
		cycles.clear();
	}

        public void addCycle(Cycle cycle){
              // check if cycle is in the list
              boolean isInList = false;
              for(int i=0; i < cycles.size(); i++){
                  if(cycles.get(i).equals(cycle) == true ){
                      isInList = true;
                      break;
                  }
              }
              if(!isInList)
                  cycles.add(cycle);
        }

        public ArrayList<Cycle> getCycles(){
              return cycles;
        }

	public void calculateCycles(Application app){
		for(Map.Entry<Integer,Actor> a : app.getActors().entrySet()){
			Cycle cycle = new Cycle(a.getValue());
			cycle.calculateCycle(app);
			if (cycle.existsCycle())
				this.addCycle(cycle);
		}
	}

	public void calculateRecII(){
		if (cycles.size() == 0){
			recII = 0;
			return;
		}
		int maxVal = Integer.MIN_VALUE;
		for(Cycle c : cycles){
			int len = c.getCycleLength();
			int dist = c.getDist();
			int rec = (int) Math.ceil((double)len/(double)dist);
			if (rec>=maxVal)
				maxVal = rec;
		}
		recII =  maxVal;
	}
	// l
	// key -> actor id
	// value -> scheduled step
	public double calculateIIPrime(HashMap<Integer,Integer> l){
		double IIprime = 0.0;
		if(cycles.size() == 0)
			return Double.NEGATIVE_INFINITY;
		double maxVal = Double.NEGATIVE_INFINITY;
		for(Cycle c : cycles){
			int del = c.getDel(l);
			int dis = c.getDist();
			double res = (double)del/(double)dis;
			if (res>=maxVal)
				maxVal=res;
		}
		IIprime = maxVal;
		return IIprime;
	}

}
