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
import multitile.application.Fifo;
import multitile.application.Application;
import multitile.application.GraphManagement;

public class Cycle{
        private Actor src;
        private List<Integer> cycle;
	// here, dist is assumed to be 1
	private final int dist=1; 
        public Cycle(Actor src){
                this.src = src;
                this.cycle = new ArrayList<Integer>();
        }

        @Override
        public boolean equals(Object obj){
                if(obj==null)
                        return false;
                if(obj.getClass() != this.getClass())
                        return false;

                final Cycle other = (Cycle) obj;
                if (this.cycle.size() != other.getCycle().size())
                        return false;

                List<Integer> sortedCycle      = new ArrayList<Integer>(this.cycle);
                List<Integer> otherSortedCycle = new ArrayList<Integer>(other.getCycle());
                Collections.sort(sortedCycle);
                Collections.sort(otherSortedCycle);
                for(int i=0; i<sortedCycle.size();i++){
                        if(sortedCycle.get(i) != otherSortedCycle.get(i))
                              return false;
                }
                return true;
        }

	public int getDist(){
		return dist;
	}

        public Actor getSrc(){
                return this.src;
        }

        public int getCycleLength(){
                return cycle.size();
        }
        
        public List<Integer> getCycle(){
                return cycle;
        }

        public boolean existsCycle(){
                if (cycle.size() == 0)
                        return false;
                return true;
        }

        public void calculateCycle(Application app){
                // has no inputs, and cannot be a cycle
                if (src.getInputFifos().size() == 0 ){
                        return;
                }
                HashMap<Integer,Integer> dist =  GraphManagement.BellmanFordCycleDistance(app,src);
                // has inputs, but they are not accesible to src
                ArrayList<Integer> accesibleInputs = getAccesibleInputs(dist,app,src.getId());
                if(accesibleInputs.size() == 0){
                        return;
                }
                // then proceed to calculate the longest cycle path                
                int val = getNextStepCycle(dist,app,accesibleInputs);                
                cycle.add(val);
        }
        
        public ArrayList<Integer> getAccesibleInputs(HashMap<Integer,Integer> dist, Application app, int srcId  ){
                ArrayList<Integer> accesibleInputs = new ArrayList<>();
              	for(Fifo f : app.getActors().get(srcId).getInputFifos()){
        		int idActor = f.getSource().getId();
        		if (dist.get(idActor) != Integer.MAX_VALUE){
                		accesibleInputs.add(idActor);
        		}
		} 
		return accesibleInputs;
        }
	
	// recursive call to do the cycle
        public int getNextStepCycle(HashMap<Integer,Integer> dist, Application app, ArrayList<Integer> accesibleInputs){
		if (accesibleInputs.size()==1 && accesibleInputs.get(0) == this.src.getId())
			return src.getId();
		int maxWeight = Integer.MIN_VALUE; 
		int maxId = 0;
                for(Integer val : accesibleInputs ){
			if(dist.get(val) >= maxWeight){
				maxWeight = dist.get(val);
				maxId =  val;
			}
                }
                // once I get the max input, then proceed to recursive call
                ArrayList<Integer> inputs = getAccesibleInputs(dist, app, maxId);
                int val = getNextStepCycle(dist, app, inputs);
                cycle.add(val);
                return maxId;
        }
}
