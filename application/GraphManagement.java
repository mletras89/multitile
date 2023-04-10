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
  @date 09 of April 2023
  @version 1.1
  @ brief
        This class implements auxiliary functions for graph, given an application
	looks for cycles and calculate the shortest distance
 
--------------------------------------------------------------------------
*/
package multitile.application;

import java.util.*;

import multitile.application.Actor;
import multitile.application.Application;
import multitile.application.Fifo;
import multitile.application.Fifo.FIFO_MAPPING_TYPE;
import multitile.application.CompositeFifo;



public class GraphManagement{

	// The main function that finds shortest distances from
	// src to all other vertices using Bellman-Ford
	// algorithm. The function also detects negative weight
	// cycle
	// return: the smallest cycle from src -> src
	// if there is no cycle returns 0
	public static HashMap<Integer,Integer> BellmanFordCycleDistance(Application application, Actor src)
	{
		int V = application.getActors().size();
		int E = application.getFifos().size();
		// key -> actor id
		// val -> distance
		HashMap<Integer,Integer> dist = new HashMap<>();

	    	// Step 1: Initialize distances from src to all
	    	// other vertices as INFINITE
	    	for (Map.Entry<Integer,Actor> a : application.getActors().entrySet())
	        	dist.put(a.getKey(), Integer.MAX_VALUE);
	    	dist.put(src.getId(), 0);
	
	    	// Step 2: Relax all edges |V| - 1 times. A simple
	    	// shortest path from src to any other vertex can
	    	// have at-most |V| - 1 edges
	    	//for (int i = 1; i < V; ++i) {
		for(Map.Entry<Integer,Actor> a : application.getActors().entrySet()){
			for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
	        	//for (int j = 0; j < E; ++j) {
	            		int u = f.getValue().getSource().getId();
	            		int v = f.getValue().getDestination().getId();
				// here, I always assume 1 as weight
	            		int weight = 1;
	            		if (dist.get(u) != Integer.MAX_VALUE
	                	    && dist.get(u) + weight < dist.get(v))
	               			dist.put(v, dist.get(u) + weight);
	        	}
	    	}
	
	    	// Step 3: check for negative-weight cycles. The
	    	// above step guarantees shortest distances if graph
	    	// doesn't contain negative weight cycle. If we get
	    	// a shorter path, then there is a cycle.
	    	//for (int j = 0; j < E; ++j) {
		for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
	    	    	int u = f.getValue().getSource().getId(); 
	    	    	int v = f.getValue().getDestination().getId(); 
			// here, we assume 1 as weight
	    	    	int weight = 1;
	    	    	if (dist.get(u) != Integer.MAX_VALUE
	    	            && dist.get(u) + weight < dist.get(v)) {
	    	        	System.out.println(
	    	            	"Graph contains negative weight cycle");
	    	        	return null;
	    	    	}
	    	}
		return dist;
	}

	// The main function that finds shortest distances from
	// src to all other vertices using Bellman-Ford
	// algorithm. The function also detects negative weight
	// cycle
	public static void BellmanFord(Application application, Actor src)
	{
		int V = application.getActors().size();
		int E = application.getFifos().size();
		// key -> actor id
		// val -> distance
		HashMap<Integer,Integer> dist = new HashMap<>();

	    	// Step 1: Initialize distances from src to all
	    	// other vertices as INFINITE
	    	for (Map.Entry<Integer,Actor> a : application.getActors().entrySet())
	        	dist.put(a.getKey(), Integer.MAX_VALUE);
	    	dist.put(src.getId(), 0);
	
	    	// Step 2: Relax all edges |V| - 1 times. A simple
	    	// shortest path from src to any other vertex can
	    	// have at-most |V| - 1 edges
	    	//for (int i = 1; i < V; ++i) {
		for(Map.Entry<Integer,Actor> a : application.getActors().entrySet()){
			for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
	        	//for (int j = 0; j < E; ++j) {
	            		int u = f.getValue().getSource().getId();
	            		int v = f.getValue().getDestination().getId();
				// here, I always assume 1 as weight
	            		int weight = 1;
	            		if (dist.get(u) != Integer.MAX_VALUE
	                	    && dist.get(u) + weight < dist.get(v))
	               			dist.put(v, dist.get(u) + weight);
	        	}
	    	}
	
	    	// Step 3: check for negative-weight cycles. The
	    	// above step guarantees shortest distances if graph
	    	// doesn't contain negative weight cycle. If we get
	    	// a shorter path, then there is a cycle.
	    	//for (int j = 0; j < E; ++j) {
		for(Map.Entry<Integer,Fifo> f : application.getFifos().entrySet()){
	    	    	int u = f.getValue().getSource().getId(); 
	    	    	int v = f.getValue().getDestination().getId(); 
			// here, we assume 1 as weight
	    	    	int weight = 1;
	    	    	if (dist.get(u) != Integer.MAX_VALUE
	    	            && dist.get(u) + weight < dist.get(v)) {
	    	        	System.out.println(
	    	            	"Graph contains negative weight cycle");
	    	        	return;
	    	    	}
	    	}
	    	printArr(dist, application,src);
	}
	
	// A utility function used to print the solution
	public static void printArr(HashMap<Integer,Integer> dist, Application application, Actor src)
	{
	    	System.out.println("Vertex Distance from Source: "+src.getName());
	    	//for (int i = 0; i < V; ++i)
		for(Map.Entry<Integer,Actor> a : application.getActors().entrySet())
			System.out.println(a.getValue().getName() + "\t\t" + dist.get(a.getKey()));
	}





}

