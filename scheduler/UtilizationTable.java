package multitile.scheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import multitile.application.Actor;

public class UtilizationTable {
	
	public static class TimeSlot{
		private int actorId;
		private int startTime = 0;
		private int endTime;
		private int length;
		private boolean split = false;
		
		
		public TimeSlot(int actorId, int startTime, int endTime) {
			assert endTime > startTime : "This should not happen";
			this.actorId = actorId;
			this.endTime = endTime;
			this.startTime = startTime;
			this.length = endTime - startTime;
		}

		public int getStartTime() {
			return startTime;
		}

		public int getEndTime() {
			return endTime;
		}

		public int getLength() {
			return length;
		}
		
		public int intersection(TimeSlot other ) {
			
			int X1 = this.startTime;
			int Y1 = this.endTime;
			int X2 = other.getStartTime();
			int Y2 = other.getEndTime();
			
			if (X1 <= X2 && Y2 <= Y1)
				return other.getLength();
			if (X2 <= X1 && Y1 <= Y2)
				return this.getLength();
			
			// two cases
			if (X1 <= X2 && Y1 <= Y2) {
				if (X1 <= Y2 && Y1 >= X2) 
					return Math.abs( Y1 - X2);
			}
			if (X2 <= X1 && Y2 <= Y1) {
				if(X2 <= Y1 && Y2 >= X1) 
					return Math.abs(Y2 - X1);
			}
			
			return 0;
		}

		public int getActorId() {
			return actorId;
		}
		
		public boolean isSplit() {
			return split;
		}
		
	};
	// key: is the core type
	// value:
	// 		key: is the core enumeration
	// 		value: is the occupation list of time slots
	private Map<Integer,Map<Integer,LinkedList<TimeSlot>>> utilizationTab;
	private HashMap<Integer,Integer> countCoresPerType;
	private int P;
	
	
	public UtilizationTable(HashMap<Integer,Integer> countCoresPerType, int P) {
		//System.out.println("countCoresPerType "+countCoresPerType );
		
		this.countCoresPerType = new HashMap<>(countCoresPerType);
		utilizationTab = new HashMap<>();
		// key is type and value is the count
		for(Map.Entry<Integer, Integer> e : countCoresPerType.entrySet())  {
			HashMap<Integer,LinkedList<TimeSlot>> coresUtil = new HashMap<>();
			for(int i=0; i < e.getValue(); i++) {
				LinkedList<TimeSlot> entries =new LinkedList<TimeSlot>();
				coresUtil.put(i, entries);	
			}
			utilizationTab.put(e.getKey(), coresUtil);
		}
		this.P = P;
	}
	
	
	public void printUtilizationTable(Map<Integer, Actor> actors, ArrayList<String>  coreTypes ) {
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>>  e : utilizationTab.entrySet()) {
			System.out.println("Core Type "+e.getKey());
			Map<Integer,LinkedList<TimeSlot>> util = e.getValue();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> u : util.entrySet()) {
				LinkedList<TimeSlot> slots = u.getValue();
				System.out.println("\tCore # "+u.getKey()+" : ");
				for(TimeSlot ts : slots) {
					System.out.println("\t\tActor "+actors.get(ts.getActorId()).getName()+" starts "+ts.getStartTime()+" ends "+ts.getEndTime()+" lenght "+ts.getLength());
				}
				
			}
			
		}
	}
	
	
	public Map<Integer,Map<Integer,LinkedList<TimeSlot>>> getUtilizationTable(){
		return this.utilizationTab;
	}

	public void saveStats(String path, Map<Integer, Actor> actors, ArrayList<String> coreTypes) throws IOException{
		try{
			File memUtilStatics = new File(path+"/heuristicSchedule.csv");
			if (memUtilStatics.createNewFile()) {
				System.out.println("File created: " + memUtilStatics.getName());
			} else {
				System.out.println("File already exists.");
			}
		}
		catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	
		FileWriter myWriter = new FileWriter(path+"/heuristicSchedule.csv"); 
		myWriter.write("Job\tStart\tFinish\tResource\n");
		saveScheduleStats(myWriter, actors, coreTypes);
	
	    myWriter.close();
	}
	
	public void saveScheduleStats(FileWriter myWriter, Map<Integer, Actor> actors, ArrayList<String> coreTypes) throws IOException{
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>>  e : utilizationTab.entrySet()) {
			//System.out.println("Core Type "+e.getKey());
			Map<Integer,LinkedList<TimeSlot>> util = e.getValue();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> u : util.entrySet()) {
				LinkedList<TimeSlot> slots = u.getValue();
				//System.out.println("\tCore # "+u.getKey()+" : ");
				for(TimeSlot ts : slots) {
					myWriter.write(actors.get(ts.getActorId()).getName()+"\t"+ts.getStartTime()+"\t"+ts.getEndTime()+"\t"+coreTypes.get(e.getKey())+","+u.getKey()+"\n");
				}
			}
		}
	}
	
	public boolean insertIntervalUtilizationTable(int actorId, int coreType, int startTime, int endTime, int length) {
		
		if (length>P)
			return false;
		
		// normalize startTime and endTIme
		startTime = startTime % P;
		endTime = endTime % P;
		
		if (endTime == 0)
			endTime = P;
		
		//System.out.println("TRYING AT "+startTime+" to "+endTime);
		
		if(endTime > startTime) {
			// try to insert a single interval
			TimeSlot ts = new TimeSlot(actorId,startTime,endTime);
			if (canInsertInCoreType(coreType,ts)) {
				insertInCoreType(coreType,ts);
				return true;
			}
				
			
		}else {
			// create two intervals and try to insert them
			TimeSlot ts1 = new TimeSlot(actorId,startTime,P);
			TimeSlot ts2 = new TimeSlot(actorId,0, endTime);
			ts1.split = true;
			ts2.split = true;
			if (canInsertInCoreType(coreType,ts1) && canInsertInCoreType(coreType,ts2)) {
				insertInCoreType(coreType, ts1, ts2);
				//insertInCoreType(coreType,ts1);
				//insertInCoreType(coreType,ts2);
				return true;
			}
		}
		return false;
	}
	


	boolean canInsertInCoreType(int coreType, TimeSlot t) {
		int nCores = countCoresPerType.get(coreType);
		
		for(int i=0; i< nCores; i++) {
			if(canInsertInCore(coreType,i,t))
				return true;
		}
		
		return false;
	}
	
	boolean canInsertInCoreType(int coreType, TimeSlot t1, TimeSlot t2) {
		int nCores = countCoresPerType.get(coreType);
		for(int i=0; i< nCores; i++) {
			if(canInsertInCore(coreType,i,t1) && canInsertInCore(coreType,i,t2))
				return true;
		}
		return false;
	}
	
	public boolean insertInCoreType(int coreType, TimeSlot t1, TimeSlot t2) {
		int nCores = countCoresPerType.get(coreType);
		
		for(int i=0; i< nCores; i++) {
			if(canInsertInCore(coreType,i,t1) && canInsertInCore(coreType,i,t2)) {
				LinkedList<TimeSlot> timeSlots = utilizationTab.get(coreType).get(i);
				timeSlots.add(t1);
				timeSlots.add(t2);
				sortIntervals(timeSlots);
				utilizationTab.get(coreType).put(i, timeSlots);
				return true;
			}
		}
		return false;
	}
	
	
	boolean canInsertInCore(int coreType, int core,TimeSlot t) {
		LinkedList<TimeSlot> timeSlots = utilizationTab.get(coreType).get(core);
		sortIntervals(timeSlots);
		
		int nIntervals = timeSlots.size();
		
		if (nIntervals == 0 && t.getLength() <= this.P)
			return true;
		// check if it is at the beginning
		if (timeSlots.get(0).getStartTime() >= t.getEndTime() && timeSlots.get(0).getStartTime() > 0 )
			return true;
		
		// check if it is in between
		for(int i=0; i < nIntervals - 1 ; i ++) {
			if (timeSlots.get(i).getEndTime() <= t.getStartTime() && t.getEndTime() <= timeSlots.get(i+1).getStartTime())
				return true;
		}
		
		// check last interval
		if (timeSlots.get(nIntervals-1).getEndTime() <= t.getStartTime() && t.getEndTime() <= P)
			return true;
		
		return false;
	}
	
	public boolean insertInCoreType(int coreType, TimeSlot t) {
		
		int nCores = countCoresPerType.get(coreType);
		
		for(int i=0; i< nCores; i++) {
			if(canInsertInCore(coreType,i,t)) {
				LinkedList<TimeSlot> timeSlots = utilizationTab.get(coreType).get(i);
				timeSlots.add(t);
				sortIntervals(timeSlots);
				utilizationTab.get(coreType).put(i, timeSlots);
				return true;
			}
		}
		return false;
	}
	
	public static void sortIntervals(LinkedList<TimeSlot> timeSlots) {
		timeSlots.sort((o1,o2) 
				-> o1.getStartTime() - o2.getStartTime());
	}
	
	
}
