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
  @date   20 February 2023
  @version 1.1
  @ brief
     This class describes a Network-on-Chip interconnect (NoC).
        - NoC: NoC that communicates the tiles
        - Parameters:
	- BW: the NoC bandwidth
	- Channels: the number of parallel transfers that might occur in the
		    NoC
--------------------------------------------------------------------------
*/
package multitile.architecture;

import multitile.Transfer;
import multitile.MapManagement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.Queue;


public class NoC{
  private int id;
  private String name;
  private List<LinkedList<Transfer>> channels;
  private List<Transfer> queueTransfers;
  private List<Double> timeEachChannel;

  private int numberofParallelChannels=4;
  private double bandwidth;  
  private double bandwidthPerChannel;
  
  public NoC(){
    this.id = ArchitectureManagement.getNoCId();
    this.name = "NoC";
    this.queueTransfers = new ArrayList<>();
    this.numberofParallelChannels = 4; // 4 as default
    this.setBandwidth(4,10);

    this.timeEachChannel  = new ArrayList<>();
    this.channels = new ArrayList<>();
 
    for(int i=0;i<numberofParallelChannels;i++){
      LinkedList<Transfer> schedActions =  new LinkedList<Transfer>();
      this.channels.add(schedActions);
      this.timeEachChannel.add(0.0);
    }
  }

  public NoC(NoC other){
    this.id = other.getId();
    this.setName(other.getName()); 
    this.channels = new ArrayList<>(other.getScheduledTransfersChannels()); 
    this.queueTransfers = new ArrayList<>(other.getQueueTransfers()); 
    this.numberofParallelChannels = other.getNumberOfParallelChannels();
    this.setBandwidth(other.getNumberOfParallelChannels(),other.getBandwidth());
    this.timeEachChannel = new ArrayList<Double>(other.getTimeEachChannel());
  }

  public void restartNoC(){
    this.channels.clear();
    this.queueTransfers.clear();
    this.timeEachChannel.clear(); 
    for(int i = 0; i<numberofParallelChannels;i++){
      LinkedList<Transfer> schedActions =  new LinkedList<Transfer>();
      this.channels.add(schedActions);
      this.timeEachChannel.add(0.0);
    }
  }

  public double calculateNoCOverallUtilization(double endTime){
    ArrayList<Double> utilization = new ArrayList<>();
    for(int i=0; i<this.numberofParallelChannels;i++){
      utilization.add(0.0);
    }
    // now proceed to count the utilization of each channel
    for(int i=0; i<this.numberofParallelChannels;i++){
      for(Transfer t : channels.get(i)){
        double preVal = utilization.get(i);
        utilization.set(i, preVal + (t.getDue_time() - t.getStart_time()));
      }
    }
    // now proceed to calculate the utilization crossbar
    double fractionPerChannel = (double)1/(double)numberofParallelChannels;
    double utilizationNoC = 0.0;
    for(int i=0; i<this.numberofParallelChannels;i++){
      utilizationNoC += (utilization.get(i)/endTime)*fractionPerChannel;
    }
    return utilizationNoC;
  }

  public String getName(){
    return this.name;
  }

  public void setName(String name){
    this.name = name;
  }

  public void setId(int id){
    this.id = id;
  }

  public int getId(){
    return this.id;
  }

  public List<Double> getTimeEachChannel(){
    return this.timeEachChannel;  
  }

  public List<LinkedList<Transfer>> getScheduledTransfersChannels(){
    return this.channels;
  }

  public void setScheduledTransfersChannels(List<LinkedList<Transfer>> channels){
    this.channels = channels;
  }

  public List<Transfer> getQueueTransfers(){
    return this.queueTransfers;
  }

  public void setQueueTransfers(List<Transfer> queueTransfers ){
    this.queueTransfers = queueTransfers;
  }

  public double getBandwidth(){
    return this.bandwidth;
  }

  public int getNumberOfParallelChannels(){
    return this.numberofParallelChannels;
  }

  public double getBandwithPerChannel(){
    return this.bandwidthPerChannel;
  }

  public void setBandwidth(int numberofParallelChannels,double BW){
    this.bandwidth = BW;
    this.numberofParallelChannels = numberofParallelChannels;
    this.bandwidthPerChannel = BW/(double)numberofParallelChannels;
  } 

  // methods for managing the NoC
  public void insertTransfer(Transfer transfer) {
    queueTransfers.add(new Transfer(transfer));
  }

  public void insertTransfers(List<Transfer> transfers) {
    for(Transfer  transfer : transfers)
      queueTransfers.add(new Transfer(transfer));
  }

  // DUMPING the NoC utilzation
  public void saveNoCUtilizationStats(String path) throws IOException{
    try{
        File memUtilStatics = new File(path+"/NoC-utilization-"+this.getName()+".csv");
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

    FileWriter myWriter = new FileWriter(path+"/NoC-utilization-"+this.getName()+".csv"); 
    myWriter.write("Job\tStart\tFinish\tResource\n");
    saveNoCUtilizationStats(myWriter);

    myWriter.close();
  }

  public void saveNoCUtilizationStats(FileWriter myWriter) throws IOException{
    for(int i=0;i<channels.size();i++){
      for(Transfer transfer : channels.get(i)){
        String operation = "reading_NoC";
        if (transfer.getType() == Transfer.TRANSFER_TYPE.WRITE) 
          operation = "writing_NoC";
        myWriter.write(operation+"\t"+ transfer.getStart_time()+"\t"+transfer.getDue_time()+"\t"+this.getName()+"_"+i+"\n");
      }
    }
  }

 // functions to manage the crossbar
  public Transfer putTransferInNoC(Transfer t){
    Transfer commitTransfer = new Transfer(t);
    int availChannelIndex = getAvailableChannel();  
    double timeLastAction = this.timeEachChannel.get(availChannelIndex);
    double transferTime = this.calculateTransferTime(commitTransfer);
    double startTime = (commitTransfer.getStart_time() > timeLastAction) ? commitTransfer.getStart_time() : timeLastAction;
    double endTime  = startTime + transferTime;
    commitTransfer.setStart_time(startTime);
    commitTransfer.setDue_time(endTime);
    // update the channel time
    this.timeEachChannel.set(availChannelIndex,endTime);
    // commit transfer in NoC
    channels.get(availChannelIndex).addLast(commitTransfer);
    return commitTransfer; 
   }
  
  public int getAvailableChannel(){
    int availChannelIndex = 0;
    int numberScheduledActions = Integer.MAX_VALUE;
    for (int i=0; i<this.numberofParallelChannels;i++){
      if (channels.get(i).size() < numberScheduledActions){
        availChannelIndex = i;
        numberScheduledActions  = channels.get(i).size(); 
      }
    }
    return availChannelIndex;
  }
  
  public double calculateTransferTime(Transfer transfer){
    int numberofBytes = transfer.getBytes();
    double processingTime = ((( BytesToGigabytes(numberofBytes) / this.bandwidthPerChannel))*1000000); // 8 bits in a byte, 100 000 to convert from secs to microseconds
    return processingTime;
  }

  double BytesToGigabytes(int bytes) {
    double ToKylo = bytes/1024;
    double ToMega = ToKylo/1024;
    double ToGiga = ToMega/1024;
    return ToGiga;
  }

}
