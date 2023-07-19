package multitile.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class UtilizationTable {
	
	public static class TimeSlot {
		
		private int startTime = 0;
		private int endTime;
		private String name;
		
		
		public TimeSlot(int startTime, int endTime) {
			assert endTime > startTime : "This should not happen";
			this.setEndTime(endTime);
			this.setStartTime(startTime);
			
		}

		public TimeSlot(String name, int startTime, int endTime) {
			assert endTime > startTime : "This should not happen";
			this.setName(name);
			this.setEndTime(endTime);
			this.setStartTime(startTime);
			
		}
		
		public int getStartTime() {
			return startTime;
		}

		public void setStartTime(int start) {
			//assert endTime > start : "This should not happen start";
			
			this.startTime = start;
		}

		public int getEndTime() {
			return endTime;
		}

		public void setEndTime(int end) {
			//assert end > this.startTime : "This should not happen end="+end+" start = "+this.startTime;
			
			this.endTime = end;
		}
		
		public int getLength() {
			return endTime - startTime;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
	}
	// key is the core type
	
	// inner map
	// key is the core ennumeration
	// valuie is the occupation list of timeslots
	private Map<Integer,Map<Integer,LinkedList<TimeSlot>>> utilizationTab;
	
	public UtilizationTable(ArrayList<Integer> resourceTypes, ArrayList<Integer> countResources) {
		assert resourceTypes.size() == countResources.size() : "These two must always have the same size...";
		utilizationTab = new HashMap<>();
		
		for(int c = 0; c < resourceTypes.size(); c++) {
			int type = resourceTypes.get(c);
			int countRes = countResources.get(c);
			
			Map<Integer,LinkedList<TimeSlot>> entries =new HashMap<Integer,LinkedList<TimeSlot>>();
			for(int i = 0 ; i < countRes; i++) {
				entries.put(i, new LinkedList<TimeSlot>());
			}
			utilizationTab.put(type, entries);
		}
	}

	public int getMaxOccupationResourceType(int length, int resourceType) {
		Map<Integer,LinkedList<TimeSlot>> selectedResourceOcc = utilizationTab.get(resourceType);
		int nResourcesType = selectedResourceOcc.size();
		return nResourcesType * length;
	}
	
	public int intersectionOverResourceType(int timeStart, int length , int resourceType) {
		Map<Integer,LinkedList<TimeSlot>> selectedResourceOcc = utilizationTab.get(resourceType);
		
		int accOccupation = 0;
		TimeSlot timeSlot = new TimeSlot(timeStart,timeStart+length);
		
		for(Map.Entry<Integer,LinkedList<TimeSlot>> e : selectedResourceOcc.entrySet()) {
			LinkedList<TimeSlot> singleResourceUtil = e.getValue();
			accOccupation += intersectionSingleResource(singleResourceUtil,timeSlot);
		}
		return accOccupation;
	}

	public boolean isGreatestTimeSlot(ArrayList<TimeSlot> occupation, TimeSlot timeSlot) {
		int size = occupation.size();
		if (size == 0)
			return true;
		
		if(occupation.get(size-1).getEndTime() <= timeSlot.getStartTime())
			return true;
		
		return false;
	}
	
	public int intersectionSingleResource(LinkedList<TimeSlot> occupation, TimeSlot timeSlot) {
		int count =0;
		// adding up all in between
		for(TimeSlot t : occupation) {
			if( timeSlot.getStartTime() <=  t.getStartTime() && t.getEndTime() <= timeSlot.getEndTime()) {
				count += t.getLength();
			}
			// check the borders
			if(t.getStartTime() < timeSlot.startTime && timeSlot.startTime < t.getEndTime()) {
				count += t.getEndTime() - timeSlot.startTime;
			}
			if(t.getStartTime() < timeSlot.getEndTime() && timeSlot.endTime < t.getEndTime() ) {
				count += timeSlot.endTime - t.getStartTime();
			}
			
		}
		return count;
	}
	
	// given a resource type, try to schedule an action
	public boolean occupyTimeSlotInResourceType(int startTime, int length,int resourceType) {
		Map<Integer,LinkedList<TimeSlot>> selectedResourceOcc = utilizationTab.get(resourceType);
		TimeSlot timeSlot = new TimeSlot(startTime,startTime+length);
		for(int i = 0 ;  i < selectedResourceOcc.size(); i++) {
			LinkedList<TimeSlot> singleResource = selectedResourceOcc.get(i);
			if (insertActionInResource(timeSlot,singleResource))
				return true;
		}
		return false;
	}
	
	public boolean insertActionInResource(TimeSlot timeSlot, LinkedList<TimeSlot> occupation) {
		int currentElements = occupation.size(); 
		if (currentElements == 0) {
			occupation.add(timeSlot);
			return true;
		}
		if(occupation.get(currentElements - 1).endTime <= timeSlot.getStartTime()) { 
			occupation.add(timeSlot);
			return true;
		}
		// the new action start in the middle of something already scheduled
		for(int i=0 ; i < currentElements;i++) {
			if(occupation.get(i).getStartTime() < timeSlot.startTime && timeSlot.startTime < occupation.get(i).getEndTime()) {
				return false;
			}
		}
		// check if there is enough space between already scheduled actions 
		for(int i=0 ; i < currentElements-1;i++) {
			if (occupation.get(i).getEndTime() <= timeSlot.startTime && timeSlot.endTime <= occupation.get(i+1).getStartTime()) {
				occupation.add(i+1, timeSlot);
				return true;
			}  
		}
		return false;
	}
	
}
