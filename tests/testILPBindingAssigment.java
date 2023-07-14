package multitile.tests;

import java.io.IOException;
import java.util.ArrayList;

import multitile.scheduler.UtilizationTable.TimeSlot;
import ilog.concert.*;
import ilog.cplex.*;


public class testILPBindingAssigment {
	public static void main(String[] args) throws IOException {
		System.out.println("Testing here!!");
		
		// List of sorted actions
		ArrayList<TimeSlot> actions = new ArrayList<TimeSlot>(); 
		TimeSlot a1 = new TimeSlot("a1",0,3);
		actions.add(a1);
		TimeSlot a2 = new TimeSlot("a2",3,4);
		actions.add(a2);
		TimeSlot a3 = new TimeSlot("a3",4,7);
		actions.add(a3);
		TimeSlot a4 = new TimeSlot("a4",7,9);
		actions.add(a4);
		TimeSlot a5 = new TimeSlot("a5",9,12);
		actions.add(a5);
		TimeSlot a6 = new TimeSlot("a6",9,11);
		actions.add(a6);
		TimeSlot a7 = new TimeSlot("a7",12,15);
		actions.add(a7);
		TimeSlot a8 = new TimeSlot("a8",15,18);
		actions.add(a8);
		
		// now we made the start and end times to fit between 0 and period P
		// in the beginning P is the same as the MII
		
		int P = 7;
		
		for(TimeSlot t : actions) {
			//System.out.println("The actor "+t.getName()+" starts at "+t.getStartTime()+" ends at "+t.getEndTime());
			t.setStartTime( t.getStartTime() % P );
			t.setEndTime( t.getEndTime() % P );
			System.out.println("The actor "+t.getName()+" starts at "+t.getStartTime()+" ends at "+t.getEndTime());
		}
		
		
		
	}
	
	
	
	
}
