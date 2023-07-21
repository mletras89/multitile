package multitile.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import multitile.application.Actor;

public class BindingAssignment {
	HashMap<Integer,Actor> actors;
	HashMap<Integer,Integer> startTime;
	HashMap<Integer,Integer> endTime;
	
	HashMap<Integer,Integer> initStartTime;
	HashMap<Integer,Integer> initEndTime;
	
	HashMap<Integer, Integer> length;
	int P;
	int R;
	
	public BindingAssignment(HashMap<Integer,Actor> actors, HashMap<Integer,Integer> startTime, HashMap<Integer,Integer> endTime,HashMap<Integer, Integer> length, int R) {
		this.actors		= actors;
		this.initStartTime 	= new HashMap<Integer,Integer>(startTime);
		this.initEndTime 	= new HashMap<Integer,Integer>(endTime);
		this.length		= new HashMap<Integer,Integer>(length);
		this.R			= R;
		int totalUse = 0;
		for(Map.Entry<Integer, Integer> e : length.entrySet()) {
			totalUse += e.getValue();
		}
		
		// initial period that might be adjusted
		this.P			=	(int)Math.ceil((double)totalUse/(double)R);
		//System.out.println("Period "+P);
	}
	
	public void getScheduleAndValidPeriod() {
		
		HashMap<Integer,Integer> binding = new HashMap<Integer,Integer>(); 
		do {
			//System.out.println("WORKING WITH PERIOD "+P);
			startTime = new HashMap<>(initStartTime);
			endTime = new HashMap<>(initEndTime);
			binding = solveBinding();
			P++;
		}while(binding == null);
		P = P-1;
		System.out.println("Solved with valid P "+P);
		for(Map.Entry<Integer, Integer> b : binding.entrySet()) {
			System.out.println("Actor "+actors.get(b.getKey()).getName()+ " is bound to resource "+b.getValue()+" starts: "+startTime.get(b.getKey())+ " ends "+endTime.get(b.getKey()));
		}
	}
	
	public HashMap<Integer,Integer> solveBinding(){
		// adjusting the start time and the end time taking into account the period		
		for(Map.Entry<Integer, Integer> s : startTime.entrySet()) {
			int key = s.getKey();
			startTime.put(key, startTime.get(key) % P );
			endTime.put(key, endTime.get(key) % P );
			if (endTime.get(key) == 0)
				endTime.put(key,P);
			//System.out.println("actor "+actors.get(key).getName()+" starts: "+startTime.get(key)+" ends "+endTime.get(key));
		}
		
		HashMap<Integer,Actor> scheduledActors = new HashMap<>();
		// iterate over the set of actors and try to construct the schedule
		ArrayList<Integer> keys = new ArrayList<>(actors.keySet());
		HashMap<Integer,Integer>  binding = new HashMap<>();
		int counter = 0;
		while(counter < keys.size()) {
			scheduledActors.put(keys.get(counter), actors.get(keys.get(counter)));
			
			HashMap<Integer,HashMap<Integer,Integer>> ov =  getOverlappingMatrix(scheduledActors, startTime, endTime, P);
			binding = solveILP(scheduledActors, R, P,ov,startTime,endTime,length);
			if (binding != null)
				counter++;
			else {
				int initStartTime = startTime.get(keys.get(counter));
				int initEndTime = endTime.get(keys.get(counter));
				while(true) {
					startTime.put(keys.get(counter), (startTime.get(keys.get(counter)) + 1) % P );
					endTime.put(keys.get(counter), (endTime.get(keys.get(counter)) + 1) % P );
					if (endTime.get(keys.get(counter)) == 0)
						endTime.put(keys.get(counter), P);
					
					//System.out.println("TESTING "+startTime.get(keys.get(counter))+ " "+endTime.get(keys.get(counter)));
					
					if(endTime.get(keys.get(counter)) == initEndTime &&  startTime.get(keys.get(counter)) == initStartTime)
						return null;
					
					ov =  getOverlappingMatrix(scheduledActors, startTime, endTime, P);
					binding = solveILP(scheduledActors, R, P,ov,startTime,endTime,length);
					if (binding != null) {
						counter++;
						break;
					}
				}
				//break;
			}
				
		}
			
			
			
		return binding;
	}
	
	private HashMap<Integer,Integer> solveILP(HashMap<Integer,Actor> scheduledActors, int R, int P,HashMap<Integer,HashMap<Integer,Integer>> ov,HashMap<Integer,Integer> startTime, HashMap<Integer,Integer> endTime, HashMap<Integer, Integer> length) {
		try {
			// define the new model
			IloCplex cplex = new IloCplex();
			// Set ILP timeout in seconds
            cplex.setParam(IloCplex.Param.TimeLimit, 60);

            // -1 More parallel, less deterministic
            //  0 Automatic: let CPLEX decide whether to invoke deterministic or opportunistic search
            //  1 Enable deterministic parallel search mode
            cplex.setParam(IloCplex.Param.Parallel, 0);

            // 0   Automatic: let CPLEX decide; default
            // 1   Sequential; single threaded
            // N   Uses up to N threads; N is limited by available cores.
            cplex.setParam(IloCplex.Param.Threads, 0);
			
            // display option
            cplex.setParam(IloCplex.Param.MIP.Display, 0);
            
			// alpha is the binding
			Map<Integer,Map<Integer,IloNumVar>> alpha = new HashMap<>();
			
			for (int r=0; r<R; r++) {
				Map<Integer,IloNumVar> mapBoolVar = new HashMap<>();
				for(Map.Entry<Integer, Actor> a : scheduledActors.entrySet()) {
					mapBoolVar.put(a.getKey(), cplex.boolVar());
				}
				alpha.put(r, mapBoolVar);
			}

			IloLinearNumExpr objective = cplex.linearNumExpr();
			objective.setConstant(R*P);
			for(int r=0; r < R; r++) {
				for(Map.Entry<Integer, Actor> a : scheduledActors.entrySet()) {
					objective.addTerm(-1*length.get(a.getKey()),  alpha.get(r).get(a.getKey()));
				}
			}
			cplex.addMinimize(objective);
			
			// constraint, each actor bound to exactly one core
			for(Map.Entry<Integer, Actor> a : scheduledActors.entrySet()) {
				IloLinearNumExpr exp = cplex.linearNumExpr();
				for(int r=0; r<R;r++) {
					exp.addTerm(1, alpha.get(r).get(a.getKey()));
				}
				cplex.addEq(exp, 1);  
			}
			// constraint for each resource, that the sum of bound actors is not larger than the period
			for(int r=0; r < R ; r++) {
				IloLinearNumExpr exp = cplex.linearNumExpr();
				for(Map.Entry<Integer, Actor> a : scheduledActors.entrySet()) {
					exp.addTerm(length.get(a.getKey()), alpha.get(r).get(a.getKey()));
				}
				//
				cplex.addLe(exp, P);
			}
			// constraint that handles the overlappings of actions
			for(int r=0; r<R ; r++) {
				for(Map.Entry<Integer, Actor> ai : scheduledActors.entrySet()) {
					for(Map.Entry<Integer, Actor> aj : scheduledActors.entrySet()) {
						IloNumExpr exp = cplex.numExpr();
						if (ov.get(ai.getKey()).get(aj.getKey()) == 1) {
							exp = cplex.sum(alpha.get(r).get(ai.getKey()),alpha.get(r).get(aj.getKey()));
							cplex.addLe(exp,1);
						}
					}
				}
			}
			// display option
			//cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			
			// solve
			if (cplex.solve()) {
				//System.out.println("AREA = "+P*R);
				//System.out.println("OBJ = "+cplex.getObjValue());
				//System.out.println("printing bindings ...");
				
				/*for(int r=0; r<R; r++) {
					for(Map.Entry<Integer, Actor> ai : scheduledActors.entrySet()) {
						int binding = (int)cplex.getValue(alpha.get(r).get(ai.getKey()));
						if (binding == 1)
							System.out.println("Actor "+ai.getValue().getName()+" is bound to resource "+r+" starts: "+startTime.get(ai.getKey())+" ends: "+endTime.get(ai.getKey()));
					}
				}*/
				HashMap<Integer,Integer> binding = new HashMap<>();
				for(int r=0; r<R; r++) {
					for(Map.Entry<Integer, Actor> ai : scheduledActors.entrySet()) {
						int singleBinding = (int)cplex.getValue(alpha.get(r).get(ai.getKey()));
						if (singleBinding == 1)
							binding.put(ai.getKey(), r);
							//System.out.println("Actor "+ai.getValue().getName()+" is bound to resource "+r+" starts: "+startTime.get(ai.getKey())+" ends: "+endTime.get(ai.getKey()));
					}
				}
				return binding;
			}
			else {
				System.out.println("Model not solved");
			}
			
			cplex.end();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
		return null;
	}
	
	
	private HashMap<Integer,HashMap<Integer,Integer>> getOverlappingMatrix(HashMap<Integer,Actor> scheduledActors, HashMap<Integer,Integer> startTime, HashMap<Integer,Integer> endTime, int P){
		
		HashMap<Integer,HashMap<Integer,Integer>> ov = new HashMap<>();
		
		//System.out.println("OV:");
		for(Map.Entry<Integer, Actor> i : scheduledActors.entrySet()) {
			HashMap<Integer,Integer> local = new HashMap<>();
			for(Map.Entry<Integer, Actor> j : scheduledActors.entrySet()){
				local.put(j.getKey(), 0);
				if (i!=j) {
					if (!(startTime.get(j.getKey()) >= endTime.get(i.getKey()) || startTime.get(i.getKey()) >= endTime.get(j.getKey())))  
						local.put(j.getKey(), 1);
					
					if(startTime.get(i.getKey()) > endTime.get(i.getKey())){
						if (!(startTime.get(j.getKey()) >= P || startTime.get(i.getKey()) >= endTime.get(j.getKey()))) {
							local.put(j.getKey(), 1);
						}
						if (!(startTime.get(j.getKey()) >= endTime.get(i.getKey()) || 0 >= endTime.get(j.getKey()))) {
							local.put(j.getKey(), 1);
						}
					}
					if(startTime.get(j.getKey()) > endTime.get(j.getKey())){
						if (!(startTime.get(j.getKey()) >= endTime.get(i.getKey()) || startTime.get(i.getKey()) >= P )) {
							local.put(j.getKey(), 1);
						}
						if (!(0 >= endTime.get(i.getKey()) || startTime.get(i.getKey()) >= endTime.get(j.getKey()))) {
							local.put(j.getKey(), 1);
						}
					}
				}
				//System.out.print(local.get(j.getKey())+", ");
			}
			ov.put(i.getKey(), local);
			//System.out.println("");
		}
		//System.out.println("OV "+ov);
		return ov;
	} 
	
}