package multitile.scheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import multitile.application.Actor;
import multitile.application.CommunicationTask;
import multitile.application.MyEntry;
import multitile.architecture.Architecture;
import multitile.architecture.Crossbar;
import multitile.architecture.NoC;
import multitile.architecture.Processor;

public class UtilizationTable {
	
	public static class TimeSlot{
		private int actorId;
		private int startTime = 0;
		private int endTime;
		private int length;
		private boolean split = false;
		private int iteration;
		private int resourceId;
		
		public TimeSlot(int actorId, int startTime, int endTime) {
			assert endTime >= startTime : "This should not happen";
			this.actorId = actorId;
			this.endTime = endTime;
			this.startTime = startTime;
			this.length = endTime - startTime;
		}
		
		public TimeSlot(TimeSlot other) {
			this.actorId = other.getActorId();
			this.startTime = other.getStartTime();
			this.endTime	= other.endTime;
			this.length = other.getLength();
			this.split = other.isSplit();
			this.iteration = other.getIteration();
			this.resourceId = other.getResourceId();
		}
		
		@Override
		public String toString() {
			return "actorId="+actorId+",("+startTime+","+endTime+")";
		}
		@Override
		public boolean equals(Object obj) {
			if(obj == null)
				return false;
		
			if(obj.getClass() != this.getClass())
				return false;
			
			final TimeSlot other = (TimeSlot)obj;
			
			if (this.actorId != other.getActorId())
				return false;
			
			if (this.startTime != other.getStartTime())
				return false;
			
			if(this.endTime != other.getEndTime())
				return false;
			
			if(this.length != other.getLength())
				return false;
			
			return true;
		}

		public int getResourceId() {
			return this.resourceId;
		}
		
		public void setResourceId(int resourceId) {
			this.resourceId = resourceId;
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
		
		public void setIteration(int iteration) {
			this.iteration = iteration;
		}
		
		public int getIteration() {
			return this.iteration;
		}
		
	};
	// key: is the core type
	// value:
	// 		key: is the core enumeration
	// 		value: is the occupation list of time slots
	private Map<Integer,Map<Integer,LinkedList<TimeSlot>>> utilizationTab;
	
	// key: is the core type
	// value:
	// 		key: is the core enumeration
	// 		value: is the occupation time 
	private Map<Integer,Map<Integer,Integer>> resourceOccupation;
	
	private HashMap<Integer,Integer> countCoresPerType;
	private int P;
	
	
	public UtilizationTable(HashMap<Integer,Integer> countCoresPerType, int P) {
		//System.out.println("countCoresPerType "+countCsoresPerType );
		
		this.countCoresPerType = new HashMap<>(countCoresPerType);
		utilizationTab = new HashMap<>();
		resourceOccupation = new HashMap<>();
		// key is type and value is the count
		for(Map.Entry<Integer, Integer> e : countCoresPerType.entrySet())  {
			HashMap<Integer,LinkedList<TimeSlot>> coresUtil = new HashMap<>();
			HashMap<Integer,Integer> count = new HashMap<>();
			for(int i=0; i < e.getValue(); i++) {
				LinkedList<TimeSlot> entries =new LinkedList<TimeSlot>();
				coresUtil.put(i, entries);
				count.put(i, 0);
			}
			utilizationTab.put(e.getKey(), coresUtil);
			resourceOccupation.put(e.getKey(), count);
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

	public void printUtilizationTable(Map<Integer, Actor> actors,Map<Integer, CommunicationTask> setCommTasks, ArrayList<String>  coreTypes ) {
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>>  e : utilizationTab.entrySet()) {
			System.out.println("Core Type "+e.getKey());
			Map<Integer,LinkedList<TimeSlot>> util = e.getValue();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> u : util.entrySet()) {
				LinkedList<TimeSlot> slots = u.getValue();
				System.out.println("\tCore # "+u.getKey()+" : ");
				for(TimeSlot ts : slots) {
					if (actors.containsKey(ts.getActorId()))
						System.out.println("\t\tActor "+actors.get(ts.getActorId()).getName()+" starts "+ts.getStartTime()+" ends "+ts.getEndTime()+" lenght "+ts.getLength());
					if (setCommTasks.containsKey(ts.getActorId()))
						System.out.println("\t\tActor "+setCommTasks.get(ts.getActorId()).getName()+" starts "+ts.getStartTime()+" ends "+ts.getEndTime()+" lenght "+ts.getLength());
				}
			}
		}
	}
	
	public Map<Integer,Map<Integer,LinkedList<TimeSlot>>> getUtilizationTable(){
		return this.utilizationTab;
	}

	public void saveKernel(String path, Map<Integer, Actor> actors, ArrayList<String> coreTypes, double scaleFactor) throws IOException{
		try{
			File memUtilStatics = new File(path+"/heuristicSchedule-RepetitionKernel.csv");
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
	
		FileWriter myWriter = new FileWriter(path+"/heuristicSchedule-RepetitionKernel.csv"); 
		myWriter.write("Job\tStart\tFinish\tResource\n");
		saveScheduleKernelStats(myWriter, actors, coreTypes,scaleFactor);
	
	    myWriter.close();
	}
	
	public void saveKernelWithCommunications(String path, Map<Integer, Actor> actors, Map<Integer, CommunicationTask> setCommunicationTasks, Architecture architecture, double scaleFactor) throws IOException{
		try{
			File memUtilStatics = new File(path+"/heuristicSchedule-RepetitionKernel-with-Communications.csv");
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
	
		FileWriter myWriter = new FileWriter(path+"/heuristicSchedule-RepetitionKernel-with-Communications.csv"); 
		myWriter.write("Job\tStart\tFinish\tResource\n");
		saveScheduleKernelWithCommunicationsStats(myWriter, actors, setCommunicationTasks, architecture, scaleFactor);
	
	    myWriter.close();
	}
	
	
	public void saveScheduleKernelStats(FileWriter myWriter, Map<Integer, Actor> actors, ArrayList<String> coreTypes, double scaleFactor) throws IOException{
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>>  e : utilizationTab.entrySet()) {
			//System.out.println("Core Type "+e.getKey());
			Map<Integer,LinkedList<TimeSlot>> util = e.getValue();
			for(Map.Entry<Integer,LinkedList<TimeSlot>> u : util.entrySet()) {
				LinkedList<TimeSlot> slots = u.getValue();
				//System.out.println("\tCore # "+u.getKey()+" : ");
				for(TimeSlot ts : slots) {
					if (ts.getStartTime() == ts.getEndTime()) {
						myWriter.write(actors.get(ts.getActorId()).getName()+"\t"+0+"\t"+ts.getEndTime()*scaleFactor+"\t"+coreTypes.get(e.getKey())+","+u.getKey()+"\n");
						myWriter.write(actors.get(ts.getActorId()).getName()+"\t"+ts.getStartTime()*scaleFactor+"\t"+this.P*scaleFactor+"\t"+coreTypes.get(e.getKey())+","+u.getKey()+"\n");
					}else
						myWriter.write(actors.get(ts.getActorId()).getName()+"\t"+ts.getStartTime()*scaleFactor+"\t"+ts.getEndTime()*scaleFactor+"\t"+coreTypes.get(e.getKey())+","+u.getKey()+"\n");
				}
			}
		}
	}
	
	public void saveScheduleKernelWithCommunicationsStats(FileWriter myWriter, Map<Integer, Actor> actors, Map<Integer, CommunicationTask> setCommunicationTasks, Architecture architecture, double scaleFactor) throws IOException{
		for(Map.Entry<Integer,Map<Integer,LinkedList<TimeSlot>>>  e : utilizationTab.entrySet()) {
			//System.out.println("Core Type "+e.getKey());
			// e.getKey() resource id
			
			Map<Integer,LinkedList<TimeSlot>> util = e.getValue();
			
			
			for(Map.Entry<Integer,LinkedList<TimeSlot>> u : util.entrySet()) {
				LinkedList<TimeSlot> slots = u.getValue();
				for(TimeSlot ts : slots) {
					String resourceName = "SP";		
					Processor p = architecture.isProcessor(ts.getResourceId());
					Crossbar  c = architecture.isCrossbar(ts.getResourceId());
					NoC		noc = architecture.isNoC(ts.getResourceId()); 
					if (p!=null) {
						resourceName = p.getName();
					}
					if(c!=null) {
						resourceName = c.getName();
					}
					if(noc != null) {
						resourceName = noc.getName();
					}		
					Actor selectActor = null;
					if(actors.containsKey(ts.getActorId()))
						selectActor = actors.get(ts.getActorId());
					if(setCommunicationTasks.containsKey(ts.getActorId()))
						selectActor = setCommunicationTasks.get(ts.actorId);
					
					if ((resourceName.compareTo("SP") != 0) )
						if (ts.getStartTime() == ts.getEndTime() && ts.getLength() != 0 ) {
							myWriter.write(selectActor.getName()+"\t"+0+"\t"+ts.getEndTime()*scaleFactor+"\t"+resourceName+"\n");
							
							myWriter.write(selectActor.getName()+"\t"+ts.getStartTime()*scaleFactor+"\t"+this.P*scaleFactor+"\t"+resourceName+"\n");
						}else
							myWriter.write(selectActor.getName()+"\t"+ts.getStartTime()*scaleFactor+"\t"+ts.getEndTime()*scaleFactor+"\t"+resourceName+"\n");
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
		
		if(endTime > startTime || length==0) {
			// try to insert a single interval
			TimeSlot ts = new TimeSlot(actorId,startTime,endTime);
			if (canInsertInCoreType(coreType,ts)) {
				boolean status = insertInCoreType(coreType,ts);
				assert status : "this must not happen";
				return true;
			}
		}else {
			// create two intervals and try to insert them
			TimeSlot ts1 = new TimeSlot(actorId,startTime,P);
			TimeSlot ts2 = new TimeSlot(actorId,0, endTime);
			ts1.split = true;
			ts2.split = true;
			if (canInsertInCoreType(coreType,ts1,ts2)) {
				boolean status = insertInCoreType(coreType, ts1, ts2);
				assert status : "this must not happen";
				return true;
			}
		}
		return false;
	}

	
	public boolean canInsertIntervalUtilizationTable(int actorId, ArrayList<Integer> boundResources, int startTime, int endTime, int length) {
		if (length>P)
			return false;
		
		// normalize startTime and endTIme
		startTime = startTime % P;
		endTime = endTime % P;
		
		if (endTime == 0)
			endTime = P;
		//System.out.println("TRYING AT "+startTime+" to "+endTime);
		
		if(endTime > startTime || length==0) {
			// try to insert a single interval
			TimeSlot ts = new TimeSlot(actorId,startTime,endTime);
			if (canInsertInBoundResources(boundResources,ts)) {
				return true;
			}
		}else {
			// create two intervals and try to insert them
			TimeSlot ts1 = new TimeSlot(actorId,startTime,P);
			TimeSlot ts2 = new TimeSlot(actorId,0, endTime);
			ts1.split = true;
			ts2.split = true;
			if (canInsertInBoundResources(boundResources,ts1) && canInsertInBoundResources(boundResources,ts2)) {
				return true;
			}
		}
		return false;
	}
	
	
	public boolean insertIntervalUtilizationTable(int actorId, ArrayList<Integer> boundResources, int startTime, int endTime, int length) {
		if (length>P)
			return false;
		
		// normalize startTime and endTIme
		startTime = startTime % P;
		endTime = endTime % P;
		
		if (endTime == 0 && length!=0)
			endTime = P;
		//System.out.println("TRYING AT "+startTime+" to "+endTime);
		
		if(endTime > startTime || length==0) {
			// try to insert a single interval
			TimeSlot ts = new TimeSlot(actorId,startTime,endTime);
			if (canInsertInBoundResources(boundResources,ts)) {
				boolean state = insertInBoundResources(boundResources,ts);
				assert state : "this must not happen";
				return true;
			}
		}else {
			// create two intervals and try to insert them
			TimeSlot ts1 = new TimeSlot(actorId,startTime,P);
			TimeSlot ts2 = new TimeSlot(actorId,0, endTime);
			ts1.split = true;
			ts2.split = true;
			if (canInsertInBoundResources(boundResources,ts1) && canInsertInBoundResources(boundResources,ts2)) {
				boolean state = insertInBoundResources(boundResources, ts1, ts2);
				assert state : "this must not happen";
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
	
	
	boolean canInsertInBoundResources(ArrayList<Integer> boundResources, TimeSlot t) {
		// this only works if all the resources in the architecture are treated as unique
		ArrayList<Boolean> status = new ArrayList<>();
		for(int resourceType : boundResources) {
			int nResources = countCoresPerType.get(resourceType);
			for(int i=0; i< nResources; i++) {
				if(canInsertInCore(resourceType,i,t))
					status.add(true);
				else
					status.add(false);
			}
		}
		for(Boolean s: status)
			if (!s) return false;
		return  true;
	}
	
	public boolean insertInBoundResources(ArrayList<Integer> boundResources, TimeSlot t) {
		// this only works if all the resources in the architecture are treated as unique
		ArrayList<Boolean> status = new ArrayList<Boolean>();
		for(int resourceType : boundResources) {
			int nResources = countCoresPerType.get(resourceType);
			for(int i=0; i< nResources; i++) {
				if(canInsertInCore(resourceType,i,t)) {
					LinkedList<TimeSlot> timeSlots = utilizationTab.get(resourceType).get(i);
					TimeSlot clone = new TimeSlot(t);
					clone.setResourceId(resourceType);
					timeSlots.add(clone);
					sortIntervals(timeSlots);
					utilizationTab.get(resourceType).put(i, timeSlots);
					resourceOccupation.get(resourceType).put(i, resourceOccupation.get(resourceType).get(i)+t.getLength());
					status.add(true);
					break;
				}
			}
		}
		//System.out.println("Bound resources "+boundResources);
		//System.out.println("status "+status);
		if (boundResources.size() == status.size())
			return true;
		return false;
	}
	
	public boolean insertInBoundResources(ArrayList<Integer> boundResources, TimeSlot t1, TimeSlot t2) {
		// this only works if all the resources in the architecture are treated as unique
		ArrayList<Boolean> status = new ArrayList<Boolean>();
		for(int resourceType : boundResources) {
			int nResources = countCoresPerType.get(resourceType);
			for(int i=0; i< nResources; i++) {
				if(canInsertInCore(resourceType,i,t1) && canInsertInCore(resourceType,i,t2)) {
					LinkedList<TimeSlot> timeSlots = utilizationTab.get(resourceType).get(i);
					TimeSlot clone1 = new TimeSlot(t1);
					TimeSlot clone2 = new TimeSlot(t2);
					clone1.setResourceId(resourceType);
					clone2.setResourceId(resourceType);
					timeSlots.add(clone1);
					timeSlots.add(clone2);
					sortIntervals(timeSlots);
					utilizationTab.get(resourceType).put(i, timeSlots);
					resourceOccupation.get(resourceType).put(i, resourceOccupation.get(resourceType).get(i)+t1.getLength());
					resourceOccupation.get(resourceType).put(i, resourceOccupation.get(resourceType).get(i)+t2.getLength());
					status.add(true);
					break;
				}
			}	
		}
		if (boundResources.size() == status.size())
			return true;
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
	
	//MyEntry<Integer,Integer>
	// key :start
	// value : end
	public Queue<MyEntry<Integer,Integer>> getCandidateStartsInBoundResources(ArrayList<Integer> boundResources, int start, int length) {
		Queue<MyEntry<Integer,Integer>> candidateStartsInBoundResources = new LinkedList<>();
		
		ArrayList<MyEntry<Integer,Integer>> overAllCandidateStartTimes = new ArrayList<>();
		
		// this only works if all the resources in the architecture are treated as unique, as the case of the heuristic with communications
		for(int resourceType : boundResources) {
			int nResources = countCoresPerType.get(resourceType);
			for(int i=0; i< nResources; i++) 
				overAllCandidateStartTimes.addAll( getCandidateStartsInCore(resourceType, i , start, length));
		}
		// filtering repeated
		overAllCandidateStartTimes = new ArrayList<MyEntry<Integer,Integer>>(new HashSet<MyEntry<Integer,Integer>>(overAllCandidateStartTimes));
		// sorting 
		overAllCandidateStartTimes.sort((o1,o2) 
				-> o1.getKey() - o2.getKey());
		// sort with respect of startTime
		for(MyEntry<Integer,Integer> k : overAllCandidateStartTimes) {
			if (k.getKey() >= start)
				candidateStartsInBoundResources.add(k);
		}
		for(MyEntry<Integer,Integer> k : overAllCandidateStartTimes) {
			if (k.getKey() < start)
				candidateStartsInBoundResources.add(k);
		}
		return candidateStartsInBoundResources;
	}
	
	ArrayList<MyEntry<Integer,Integer>> getCandidateStartsInCore(int  coreType, int core, int start, int length){
		ArrayList<MyEntry<Integer,Integer>> candidateStarts = new ArrayList<>();
		LinkedList<TimeSlot> timeSlots = utilizationTab.get(coreType).get(core);
		sortIntervals(timeSlots);
		
		int nTimeSlots = timeSlots.size();
		
		if (nTimeSlots == 0) {  // the thing is empty, starting from start and ending at start
			candidateStarts.add(new MyEntry<Integer,Integer>(0,this.P));
			return candidateStarts;
		}
		
		for(int i = 0; i<nTimeSlots-1;i++) {
			if( timeSlots.get(i+1).getStartTime() - timeSlots.get(i).getEndTime() >= length  ) {
				candidateStarts.add(new MyEntry<Integer,Integer>(timeSlots.get(i).getEndTime(), timeSlots.get(i+1).getStartTime() ));
			}
		}
		// case when the borders are connected
		if(timeSlots.get(nTimeSlots-1).getEndTime() < this.P  && timeSlots.get(0).getStartTime()>0 ) {
			int sizeBorderSpace = (P - timeSlots.get(nTimeSlots-1).getEndTime()) + timeSlots.get(0).getStartTime();
			if(sizeBorderSpace >= length) 
				candidateStarts.add(new MyEntry<Integer,Integer>(timeSlots.get(nTimeSlots-1).getEndTime(), timeSlots.get(0).getStartTime() ));
			return candidateStarts;
		}
		// check individual borders
		if(timeSlots.get(nTimeSlots-1).getEndTime() < this.P) {
			if((P - timeSlots.get(nTimeSlots-1).getEndTime() ) >= length) 
				candidateStarts.add(new MyEntry<Integer,Integer>(timeSlots.get(nTimeSlots-1).getEndTime(),  this.P));
		}
		if(timeSlots.get(0).getStartTime()>0 ) {
			if(timeSlots.get(0).getStartTime() >= length)
				candidateStarts.add(new MyEntry<Integer,Integer>(0,timeSlots.get(0).getStartTime()));
		}
			
		return candidateStarts;
		
	}
	
	boolean canInsertInCore(int coreType, int core,TimeSlot t) {
		LinkedList<TimeSlot> timeSlots = utilizationTab.get(coreType).get(core);
		sortIntervals(timeSlots);
		
		int nIntervals = timeSlots.size();
		if (nIntervals == 0 && t.getLength() <= this.P)
			return true;
		
		if(!(t.startTime>=0 && t.endTime <= this.P))
			return false;
		
		for(TimeSlot ts : timeSlots) {
			//System.out.println("intersection "+ts.intersection(t)+" core type "+coreType+" core "+core+" timeslot "+ts);
			if (ts.intersection(t) != 0)
				return false;
			
			if (t.getLength() == 0 && t.getStartTime()> ts.getStartTime() && t.getEndTime() < ts.getEndTime())
				return false;
			
			if (ts.getLength() == 0 && ts.getStartTime()> t.getStartTime() && ts.getEndTime() < t.getEndTime())
				return false;
			
		}
		
		return true;
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
	
	public void sortIntervals(LinkedList<TimeSlot> timeSlots) {
		timeSlots.sort((o1,o2) -> {
			int result = o1.getStartTime() - o2.getStartTime();
			if (result == 0) result = o1.getLength() - o2.getLength();
					return result;
			});
	}
	
	
}
