package multitile.tests;

import multitile.Transfer;
import multitile.application.Actor;
import multitile.application.Fifo;
import multitile.architecture.Memory;
import multitile.architecture.Crossbar;

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

public class testCrossbar {
    public static void main(String[] args) throws IOException {
      System.out.println("Testing crossbar!");

      Memory memoryTest1 = new Memory("Memory_1");
      memoryTest1.setType(Memory.MEMORY_TYPE.TILE_LOCAL_MEM);

      Actor actor1 = new Actor("actor1");
      actor1.setId(1);
      Fifo fifo1 = new Fifo("fifo1",0,2,1000000,memoryTest1,1,1,actor1,actor1);
      Transfer  t1 = new Transfer(actor1,fifo1);
      t1.setStart_time(100);
      t1.setType(Transfer.TRANSFER_TYPE.WRITE);

      Actor actor2 = new Actor("actor2");
      actor2.setId(2);
      Fifo fifo2 = new Fifo("fifo2",0,2,1000000,memoryTest1,1,1,actor1,actor1);
      Transfer  t2 = new Transfer(actor2,fifo2);
      t2.setStart_time(100);
      t2.setType(Transfer.TRANSFER_TYPE.WRITE);

      Actor actor3 = new Actor("actor3");
      actor3.setId(3);
      Fifo fifo3 = new Fifo("fifo3",0,2,1000000,memoryTest1,1,1,actor1,actor1);
      Transfer  t3 = new Transfer(actor3,fifo3);
      t3.setStart_time(100);
      t3.setType(Transfer.TRANSFER_TYPE.WRITE);

      Actor actor4 = new Actor("actor4");
      actor4.setId(4);
      Fifo fifo4 = new Fifo("fifo4",0,2,1000000,memoryTest1,1,1,actor1,actor1);
      Transfer  t4 = new Transfer(actor4,fifo4);
      t4.setStart_time(100);
      t4.setType(Transfer.TRANSFER_TYPE.READ);

      Actor actor5 = new Actor("actor5");
      actor5.setId(5);
      Actor actor6 = new Actor("actor6");
      actor6.setId(6);
      Actor actor7 = new Actor("actor5");
      actor7.setId(7);
      Actor actor8 = new Actor("actor6");
      actor8.setId(8);


      Crossbar crossbar1 = new Crossbar("Crossbar",1,4);
      crossbar1.insertTransfer(t1);
      crossbar1.insertTransfer(t2);
      crossbar1.insertTransfer(t3);
      crossbar1.insertTransfer(t4);
      crossbar1.insertTransfer(t4);
      crossbar1.insertTransfer(t4);
      crossbar1.insertTransfer(t4);

      Transfer  t5 = new Transfer(actor1,fifo1);
      t5.setStart_time(100);
      t5.setType(Transfer.TRANSFER_TYPE.WRITE);
      crossbar1.insertTransfer(t5);
      t4.setStart_time(4000);
      crossbar1.insertTransfer(t4);
      t4.setStart_time(5000); 
      crossbar1.insertTransfer(t4);
      crossbar1.insertTransfer(t4);

      List<Transfer> transfers = Arrays.asList(t4,t4,t4);
      crossbar1.insertTransfers(transfers);

      crossbar1.commitTransfersinQueue();
      crossbar1.saveCrossbarUtilizationStats(".");

      System.out.println("Finishing testing crossbar!");
    }
}

