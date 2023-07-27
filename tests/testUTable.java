package multitile.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import multitile.application.Actor;
import multitile.application.ActorManagement;
import multitile.scheduler.BindingAssignment;
import multitile.scheduler.UtilizationTable;

public class testUTable {
	public static void main(String[] args) throws IOException {
		System.out.println("Testing here!!");
		HashMap<Integer,Actor> actors = new HashMap<>();
		
		ActorManagement.resetCounters();
		
		Actor a1 = new Actor("source");
		Actor a2 = new Actor("grayscale");
		Actor a3 = new Actor("convert");
		Actor a4 = new Actor("multicast");
		Actor a5 = new Actor("vertical");
		Actor a6 = new Actor("horizontal");
		Actor a7 = new Actor("magnitude");
		Actor a8 = new Actor("sink");
		
		actors.put(a1.getId(),a1);
		actors.put(a2.getId(),a2);
		actors.put(a3.getId(),a3);
		actors.put(a4.getId(),a4);
		actors.put(a5.getId(),a5);
		actors.put(a6.getId(),a6);
		actors.put(a7.getId(),a7);
		actors.put(a8.getId(),a8);
		
		HashMap<Integer,Integer> countCoresPerType = new HashMap<>();
		countCoresPerType.put(0, 3);
		ArrayList<String> coreTypes = new ArrayList<>();
		coreTypes.add("C1");
		
		
		//tab.printUtilizationTable(actors, coreTypes, P);
		
		HashMap<Integer,Integer> start = new HashMap<>();
		start.put(a1.getId(), 0);
		start.put(a2.getId(), 3);
		start.put(a3.getId(), 17);
		start.put(a4.getId(), 24);
		start.put(a5.getId(), 25);
		start.put(a6.getId(), 25);
		start.put(a7.getId(), 24);
		start.put(a8.getId(), 16);
		
		HashMap<Integer,Integer> length = new HashMap<>();
		length.put(a1.getId(), 3);
		length.put(a2.getId(), 14);
		length.put(a3.getId(), 7);
		length.put(a4.getId(), 1);
		length.put(a5.getId(), 8);
		length.put(a6.getId(), 10);
		length.put(a7.getId(), 18);
		length.put(a8.getId(), 8);
		
		int P = 0 ;
		
		for(Map.Entry<Integer, Integer> e : length.entrySet()) {
			P += e.getValue();
		}
		
		P = P / countCoresPerType.get(0);
		System.out.println("Period P = " + P);
		
		
		HashMap<Integer,Integer> end = new HashMap<>();
		for(Map.Entry<Integer, Integer> e : start.entrySet()) {
			//start.put(e.getKey(), e.getValue() % P);
			end.put(e.getKey(), (e.getValue()  +  length.get(e.getKey())) );
		}
		
		
		for(Map.Entry<Integer, Integer> e : start.entrySet()) {
			System.out.println("Actor "+actors.get(e.getKey()).getName()+" should start at "+start.get(e.getKey())+" ends "+end.get(e.getKey())+" with lenght "+length.get(e.getKey()));
		}
		
		UtilizationTable tab = new UtilizationTable(countCoresPerType,P); 
		int countActions = start.size();
		ArrayList<Integer> keys = new ArrayList<>(start.keySet());
		int counter = 0;
		
		
		while(counter < countActions) {
			int currentKey = keys.get(counter);
			//System.out.println("inserting actor "+actors.get(currentKey).getName());
			boolean state = tab.insertIntervalUtilizationTable(currentKey, 0, start.get(currentKey), end.get(currentKey), length.get(currentKey));
			if (!state) {
				System.out.println("\tERROR: last "+actors.get(currentKey).getName()+" I have to recalculate\n");
				tab.printUtilizationTable(actors, coreTypes);
				//assert false;
				
				// recalculate
				
				int newStartTime 	= (start.get(currentKey) + 1);
				int newEndTime 		= (newStartTime + length.get(currentKey));
				start.put( currentKey ,  newStartTime );
				end.put(currentKey,  newEndTime   );
			}
			else {
				//tab.printUtilizationTable(actors, coreTypes, P);
				counter++;
			}
		}
		
		
		
		System.out.println("Corrected start time and end times");
		tab.printUtilizationTable(actors, coreTypes);
		
		for(Map.Entry<Integer, Integer> e : start.entrySet()) {
			System.out.println("Actor "+actors.get(e.getKey()).getName()+" should start at "+start.get(e.getKey())+" ends "+end.get(e.getKey())+" with lenght "+length.get(e.getKey()));
		}
		
		
		BindingAssignment bd = new BindingAssignment(actors, start, end,length, 3, P, 1);
		bd.solveBinding();
		
		
		
	}
}
