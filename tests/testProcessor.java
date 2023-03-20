package multitile.tests;

import java.io.*;

import multitile.application.Actor;
import multitile.architecture.Processor;
import multitile.mapping.Binding;
import multitile.mapping.Bindings;
import multitile.Action;

public class testProcessor {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing processor!");

      Bindings bindings = new Bindings();
      
      Actor actor1 = new Actor("actor1");      
      Actor actor2 = new Actor("actor2");     
      Actor actor3 = new Actor("actor3");   
      Actor actor4 = new Actor("actor4");
      
      
      Processor cpu1 = new Processor("cpu1");
      // biding cpus
      bindings.getActorProcessorBindings().put(actor1.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(actor2.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(actor3.getId(), new Binding<Processor>(cpu1));
      bindings.getActorProcessorBindings().put(actor4.getId(), new Binding<Processor>(cpu1));
      // setting execution time to binding
      bindings.getActorProcessorBindings().get(actor1.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(actor2.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(actor3.getId()).getProperties().put("runtime", 10000.0);
      bindings.getActorProcessorBindings().get(actor4.getId()).getProperties().put("runtime", 10000.0);
      Action a1 = new Action(actor1,10000.0);
      Action a2 = new Action(actor2,10000.0);
      Action a3 = new Action(actor3,10000.0);
      Action a4 = new Action(actor4,10000.0);
      
      cpu1.getScheduler().insertAction(a1);
      cpu1.getScheduler().insertAction(a2);
      cpu1.getScheduler().insertAction(a3);
      cpu1.getScheduler().insertAction(a4);

      a1.setStart_time(70000);
      cpu1.getScheduler().insertAction(a1);
      cpu1.getScheduler().insertAction(a2);
      cpu1.getScheduler().insertAction(a3);
      cpu1.getScheduler().insertAction(a4);

      cpu1.getScheduler().commitActionsinQueue();
      cpu1.getScheduler().saveScheduleStats(".");

      System.out.println("Finishing testing crossbar!");
    }
}

