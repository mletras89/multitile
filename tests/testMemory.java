package multitile.tests;

import multitile.architecture.Memory;

import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class testMemory {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing Memory class!");
      Memory memoryTest = new Memory("Memory_1",1000);
      Memory memoryTest2 = new Memory("Memory_2",2000);
      Memory memoryTest3 = new Memory("Memory_3",Double.POSITIVE_INFINITY);
     
      int load1 = 100;
      int load2 = 200;
      int load3 = 20;
      
      for(int i=0; i<20; i++){
        if (memoryTest.canPutDataInMemory(load1))
          memoryTest.writeDataInMemory(load1,i*10);
        else
          // put the data in the unbounded memory
          memoryTest3.writeDataInMemory(load1,i*10);

        if (memoryTest2.canPutDataInMemory(load2))
          memoryTest2.writeDataInMemory(load2,i*10);
        else
          // put the data in the undounded memory
          memoryTest3.writeDataInMemory(load2,i*10);

        if (memoryTest3.canPutDataInMemory(load2))
          memoryTest3.writeDataInMemory(load3*10,i*10);
      }

      memoryTest.readDataInMemory(memoryTest.getCurrentAmountofBytes(),200);
      memoryTest2.readDataInMemory(memoryTest2.getCurrentAmountofBytes(),200);
      memoryTest3.readDataInMemory(memoryTest3.getCurrentAmountofBytes(),200);

      System.out.println("The memory 1 utilization over the execution was: "+memoryTest.getUtilization(250));
      System.out.println("The memory 2 utilization over the execution was: "+memoryTest2.getUtilization(250));
      System.out.println("The memory 3 utilization over the execution was: "+memoryTest3.getUtilization(250));

      try{
          File memUtilStatics = new File("testMemory.csv");
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

      FileWriter myWriter = new FileWriter("testMemory.csv"); 
      myWriter.write("Memory\tWhen\tCapacity\n");

      memoryTest.saveMemoryUtilizationStats(myWriter);
      memoryTest2.saveMemoryUtilizationStats(myWriter);
      memoryTest3.saveMemoryUtilizationStats(myWriter);

      myWriter.close();

      System.out.println("End Testing Memory class!");
    }
}

