package multitile.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import multitile.application.Actor;
import multitile.scheduler.BindingAssignment;
import multitile.scheduler.UtilizationTable.TimeSlot;
import ilog.concert.*;
import ilog.cplex.*;


public class testILPBindingAssigment {
	
	
	
	
	public static void main(String[] args) throws IOException {
		System.out.println("Testing here!!");
		
		// List of sorted actions
		ArrayList<TimeSlot> actions = new ArrayList<TimeSlot>(); 
		TimeSlot ta1 = new TimeSlot(1,0,3);
		actions.add(ta1);
		TimeSlot ta2 = new TimeSlot(2,3,4);
		actions.add(ta2);
		TimeSlot ta3 = new TimeSlot(3,4,7);
		actions.add(ta3);
		TimeSlot ta4 = new TimeSlot(4,7,9);
		actions.add(ta4);
		TimeSlot ta5 = new TimeSlot(5,9,12);
		actions.add(ta5);
		TimeSlot ta6 = new TimeSlot(6,9,11);
		actions.add(ta6);
		TimeSlot ta7 = new TimeSlot(7,12,15);
		actions.add(ta7);
		TimeSlot ta8 = new TimeSlot(8,18,21);
		actions.add(ta8);
		
		
		HashMap<Integer,Actor> actors = new HashMap<>();
		Actor a1 = new Actor("a1");
		Actor a2 = new Actor("a2");
		Actor a3 = new Actor("a3");
		Actor a4 = new Actor("a4");
		Actor a5 = new Actor("a5");
		Actor a6 = new Actor("a6");
		Actor a7 = new Actor("a7");
		Actor a8 = new Actor("a8");
		
		actors.put(a1.getId(),a1);
		actors.put(a2.getId(),a2);
		actors.put(a3.getId(),a3);
		actors.put(a4.getId(),a4);
		actors.put(a5.getId(),a5);
		actors.put(a6.getId(),a6);
		actors.put(a7.getId(),a7);
		actors.put(a8.getId(),a8);
		
		
		// now we made the start and end times to fit between 0 and period P
		// in the beginning P is the same as the MII
		int P = 7;
		int R = 3; // n processors
		// the key of the map is the id of the actor
		HashMap<Integer, Integer> startTime = new HashMap<>();
		HashMap<Integer, Integer> endTime = new HashMap<>();
		HashMap<Integer, Integer> length = new HashMap<>();
		
		startTime.put(a1.getId(), 0); // a1
		startTime.put(a2.getId(), 3); // a2
		startTime.put(a3.getId(), 4); // a3
		startTime.put(a4.getId(), 7); // a4
		startTime.put(a5.getId(), 9); // a5
		startTime.put(a6.getId(), 9); // a6
		startTime.put(a7.getId(), 12);// a7
		startTime.put(a8.getId(), 15);// a8
				
		endTime.put(a1.getId(), 3);
		endTime.put(a2.getId(), 4);
		endTime.put(a3.getId(), 7);
		endTime.put(a4.getId(), 9);
		endTime.put(a5.getId(), 12);
		endTime.put(a6.getId(), 11);
		endTime.put(a7.getId(), 15);
		endTime.put(a8.getId(), 18);
				
		length.put(a1.getId(), 3);
		length.put(a2.getId(), 1);
		length.put(a3.getId(), 3);
		length.put(a4.getId(), 2);
		length.put(a5.getId(), 3);
		length.put(a6.getId(), 2);
		length.put(a7.getId(), 3);
		length.put(a8.getId(), 3);
		
		//HashMap<Integer,Actor> actors, HashMap<Integer,Integer> startTime, HashMap<Integer,Integer> endTime,HashMap<Integer, Integer> length, int R
		//BindingAssignment bd = new BindingAssignment(actors, startTime, endTime, length, R,10);
		//bd.getScheduleAndValidPeriod();
		//bd.solveBinding();
		
		
		
		/*
		int A = startTime.size();
		int[][] ov = new int[A][A];
		
		// adjusting the start time and the end time taking into account the period		
		for(Map.Entry<Integer, Integer> s : startTime.entrySet()) {
			int key = s.getKey();
			startTime.put(key, startTime.get(key) % P );
			endTime.put(key, endTime.get(key) % P );
			if (endTime.get(key) == 0)
				endTime.put(key,P);
			System.out.println("actor "+actors.get(key).getName()+" starts: "+startTime.get(key)+" ends "+endTime.get(key));
		}
		
		System.out.println("OV:");
		for(int i=0; i < A; i++) {
			for(int j=0; j< A; j++) {
				ov[i][j] = 0; 
				if (i!=j) {
					if (!(startTime.get(j) >= endTime.get(i) || startTime.get(i) >= endTime.get(j)))  
						ov[i][j] = 1;
					if(startTime.get(i) > endTime.get(i)){
						if (!(startTime.get(j) >= P || startTime.get(i) >= endTime.get(j))) {
							ov[i][j] = 1;
						}
						if (!(startTime.get(j) >= endTime.get(i) || 0 >= endTime.get(j))) {
							ov[i][j] = 1;
						}
					}
					if(startTime.get(j) > endTime.get(j)){
						if (!(startTime.get(j) >= endTime.get(i) || startTime.get(i) >= P )) {
							ov[i][j] = 1;
						}
						if (!(0 >= endTime.get(i) || startTime.get(i) >= endTime.get(j))) {
							ov[i][j] = 1;
						}
					}
				}
				System.out.print(ov[i][j]+", ");
			}
			System.out.println("");
		}
		
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
			
			// alpha is the binding
			IloNumVar[][] alpha = new IloNumVar[R][];
			for (int r=0; r<R; r++) {
        		//alpha[i] = cplex.numVarArray(A, 0, Double.MAX_VALUE);
				// boolean variable to define the binding
				alpha[r] = cplex.boolVarArray(A);
        	}

			IloLinearNumExpr objective = cplex.linearNumExpr();
			objective.setConstant(R*P);
			for(int r=0; r < R; r++) {
				for(int j=0; j< A; j++) {
					objective.addTerm(-1*length.get(j), alpha[r][j]);
				}
			}
			cplex.addMinimize(objective);
			
			// constraint, each actor bound to exactly one core
			for(int j =0; j<A;j++) {
				IloLinearNumExpr exp = cplex.linearNumExpr();
				for(int r=0; r<R;r++) {
					exp.addTerm(1, alpha[r][j]);
				}
				cplex.addEq(exp, 1);  
			}
			// constraint for each resource, that the sum of bound actors is not larger than the period
			for(int r=0; r < R ; r++) {
				IloLinearNumExpr exp = cplex.linearNumExpr();
				for(int j=0; j < A; j++) {
					exp.addTerm(length.get(j), alpha[r][j]);
				}
				//
				cplex.addLe(exp, P);
			}
			// constraint that handles the overlappings of actions
			for(int i=0; i < A; i++) {
				for(int j=0; j < A; j++) {
					IloNumExpr[] exps = new IloNumExpr[R];
					for(int r=0; r<R ; r++) {
						//cplex.linearNumExpr()
						exps[r] = cplex.prod(alpha[r][i],alpha[r][j]);
					}
					IloNumExpr addExp = cplex.sum(exps);
					addExp = cplex.sum(ov[i][j], addExp);
					// add the constraint
					cplex.addLe(addExp,1);
				}
			}
			
			// display option
			//cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			
			// solve
			if (cplex.solve()) {
				System.out.println("AREA = "+P*R);
				System.out.println("OBJ = "+cplex.getObjValue());
				System.out.println("printing bindings ...");
				
				for(int r=0; r<R; r++) {
					for(int j=0; j < A ; j++) {
						int binding = (int)cplex.getValue(alpha[r][j]);
						if (binding == 1)
							System.out.println("Actor "+actors.get(j).getName()+" is bound to resource "+r+" starts: "+startTime.get(j)+" ends: "+endTime.get(j));
					}
				}
			}
			else {
				System.out.println("Model not solved");
			}
			
			cplex.end();
        } catch (Exception exc) {
            exc.printStackTrace();
        }*/
		
		
	}
	
	
	
	
}
