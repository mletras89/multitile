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
  @date   02 November 2022
  @version 1.1
  @ brief
     -This class describes a crossbar that connects processors in a tile to 
      a tile local memory. 
     -numberofParallelChannels define the number of parallel transfers that
      can be scheduled
     -scheduledActions scheduled transfers in the crossbar
--------------------------------------------------------------------------
*/
package multitile.architecture;

import multitile.Transfer;
import multitile.application.Actor;
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

public class Crossbar{
  private int id;
  private String name;
  private List<Transfer> queueTransfers;
  private List<LinkedList<Transfer>> scheduledActions;
  private Map<Actor,List<Transfer>> scheduledReadTransfers;
  private Map<Actor,List<Transfer>> scheduledWriteTransfers;
  private List<Double> timeEachChannel;
  private int numberofParallelChannels;
  private double bandwidth;  // each crossbar has a bandwidht in Gbps
  private double bandwidthPerChannel;


  // initializing empty crossbar
  public Crossbar() {
    this.id = ArchitectureManagement.getCrossbarId();
    this.name = "bus";
    this.queueTransfers = new ArrayList<>();
    this.numberofParallelChannels = 1; // as a regular bus
    this.scheduledActions = new ArrayList<>();
    this.timeEachChannel  = new ArrayList<>();
    LinkedList<Transfer> schedActions =  new LinkedList<Transfer>();
    this.scheduledReadTransfers = new HashMap<>();
    this.scheduledWriteTransfers = new HashMap<>();
    this.scheduledActions.add(schedActions);
    this.timeEachChannel.add(0.0);
    this.setBandwidth(1,16);
  }
   // cloning crossbar
  public Crossbar(Crossbar other) {
    this.name = other.getName();
    this.id   = other.getId();
    this.queueTransfers = new ArrayList<>(other.getQueueTransfers());
    this.scheduledActions = new ArrayList<>(other.getScheduledActions());
    this.setBandwidth(other.getNumberofParallelChannels(),other.getBandwidth());
    this.scheduledReadTransfers = new HashMap<>();
    this.scheduledWriteTransfers = new HashMap<>();
    //this.ownerTile = other.getOwnerTile();
  }
  // creating crossbar from given parameters
  public Crossbar(String name, double bandwidth, int numberofParallelChannels){
    this.name = name;
    this.id   = ArchitectureManagement.getCrossbarId();
    this.numberofParallelChannels = numberofParallelChannels;
    this.queueTransfers = new ArrayList<>();
    this.scheduledActions = new ArrayList<>();
    this.timeEachChannel  = new ArrayList<>();
    for(int i = 0; i<numberofParallelChannels;i++){
      LinkedList<Transfer> schedActions =  new LinkedList<Transfer>();
      this.scheduledActions.add(schedActions);
      this.timeEachChannel.add(0.0);
    }
    this.setBandwidth(numberofParallelChannels,bandwidth);
    this.scheduledReadTransfers = new HashMap<>();
    this.scheduledWriteTransfers = new HashMap<>();
    //System.out.println("CROSSBAR BW="+this.bandwidth+" BW PER CHANNEL="+this.bandwidthPerChannel);
  }

  public double calculateCrossbarOverallUtilization(double endTime){
    ArrayList<Double> utilization = new ArrayList<>();
    for(int i=0; i<this.numberofParallelChannels;i++){
      utilization.add(0.0);
    }
    // now proceed to count the utilization of each channel
    for(int i=0; i<this.numberofParallelChannels;i++){
      for(Transfer t : scheduledActions.get(i)){
        double preVal = utilization.get(i);
        utilization.set(i, preVal + (t.getDue_time() - t.getStart_time()));
      }
    }
    // now proceed to calculate the utilization crossbar
    double fractionPerChannel = (double)1/(double)numberofParallelChannels;
    double utilizationCrossbar = 0.0;
    for(int i=0; i<this.numberofParallelChannels;i++){
      utilizationCrossbar += (utilization.get(i)/endTime)*fractionPerChannel;
    }
    return utilizationCrossbar;
  }

  public double getBandwithPerChannel(){
    return this.bandwidthPerChannel;
  }

  public void restartCrossbar(){
    this.queueTransfers.clear();
    this.scheduledActions.clear();
    this.timeEachChannel.clear();
    for(int i = 0; i<numberofParallelChannels;i++){
      LinkedList<Transfer> schedActions =  new LinkedList<Transfer>();
      this.scheduledActions.add(schedActions);
      this.timeEachChannel.add(0.0);
    }
    this.scheduledReadTransfers.clear();
    this.scheduledWriteTransfers.clear();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Transfer> getQueueTransfers(){
    return this.queueTransfers;
  }

  public void setQueueTransfers(List<Transfer> queueTransfers ){
    this.queueTransfers = queueTransfers;
  }
  
  public void cleanQueueTransfers(){
    this.queueTransfers.clear();
    this.scheduledReadTransfers.clear();
    this.scheduledWriteTransfers.clear();
  }

  public List<LinkedList<Transfer>> getScheduledActions(){
    return this.scheduledActions;
  }

  public void setScheduledActions(List<LinkedList<Transfer>> scheduledAction){
    this.scheduledActions = scheduledActions;
  }

  public int getNumberofParallelChannels(){
    return numberofParallelChannels;
  }

  public void setNumberofParallelChannels(int numberofParallelChannels){
    this.numberofParallelChannels = numberofParallelChannels;
  }

  public double getBandwidth(){
    return bandwidth;
  }

  public void setBandwidth(int numberOfParallelChannels,double bandwidth){
    this.bandwidth = bandwidth;
    this.numberofParallelChannels = numberOfParallelChannels;
    this.bandwidthPerChannel = bandwidth/(double)numberOfParallelChannels;
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

  public Map<Actor,List<Transfer>> getScheduledReadTransfers(){
    return scheduledReadTransfers;
  }
  
  public Map<Actor,List<Transfer>> getScheduledReadTransfers(Processor processor){
    Map<Actor,List<Transfer>> processorTransfers = new HashMap<>();
    for(Map.Entry<Actor,List<Transfer>> entry : this.scheduledReadTransfers.entrySet()){
      if(entry.getKey().getMapping().equals(processor)){
	processorTransfers.put(entry.getKey(),entry.getValue());
      }
    }
    return processorTransfers;
  }

  public Map<Actor,List<Transfer>> getScheduledWriteTransfers(){
    return this.scheduledWriteTransfers;
  }

  public Map<Actor,List<Transfer>> getScheduledWriteTransfers(Processor processor){
    Map<Actor,List<Transfer>> processorTransfers = new HashMap<>();
    for(Map.Entry<Actor,List<Transfer>> entry : this.scheduledWriteTransfers.entrySet()){
      if(entry.getKey().getMapping().equals(processor)){
	processorTransfers.put(entry.getKey(),entry.getValue());
      }
    }
    return processorTransfers;
  }

// methods for managing the crossbar, the insertion in each channel

  public void insertTransfer(Transfer transfer) {
    queueTransfers.add(new Transfer(transfer));
  }

  public void insertTransfers(List<Transfer> transfers) {
    for(Transfer  transfer : transfers)
      queueTransfers.add(new Transfer(transfer));
  }

  public void addScheduledTransfer(Transfer transfer){
    if(transfer.getType()==Transfer.TRANSFER_TYPE.READ){
      List<Transfer> transfers;
      if(MapManagement.isActorIdinMap(scheduledReadTransfers.keySet(),transfer.getActor().getId())){
        transfers = scheduledReadTransfers.get(transfer.getActor());
      }else{
        transfers = new ArrayList<>();
      } 
      transfers.add(transfer);
      scheduledReadTransfers.put(transfer.getActor(),transfers);
    }else{
      List<Transfer> transfers;
      if(MapManagement.isActorIdinMap(scheduledWriteTransfers.keySet(),transfer.getActor().getId())){
        transfers = scheduledWriteTransfers.get(transfer.getActor());
      }else{
        transfers = new ArrayList<>();
      } 
      transfers.add(transfer);
      scheduledWriteTransfers.put(transfer.getActor(),transfers);
    }
  }
   
  public Transfer putTransferInCrossbar(Transfer t){
    Transfer commitTransfer   = new Transfer(t);
    int availChannelIndex     = getAvailableChannel();
    double timeLastAction     = this.timeEachChannel.get(availChannelIndex);
    double transferTime       = this.calculateTransferTime(commitTransfer);
    double startTime          = (commitTransfer.getStart_time() > timeLastAction) ? commitTransfer.getStart_time() : timeLastAction;
    double endTime            = startTime + transferTime;

    // update now the commit transfer
    commitTransfer.setStart_time(startTime);
    commitTransfer.setDue_time(endTime);
    // update the channel time 
    this.timeEachChannel.set(availChannelIndex,endTime);
    // commit transfer
    scheduledActions.get(availChannelIndex).addLast(commitTransfer);
    return commitTransfer;
  }

  public void commitTransfersinQueue(){
    // then commit all the transfers in the Queue
    int elementsinQueue = queueTransfers.size();
    
    for(int i=0;i<elementsinQueue;i++){
      Transfer commitTransfer = queueTransfers.remove(0);
      int availChannelIndex = getAvailableChannel();
      //System.out.println("avail index "+availChannelIndex);
      double timeLastAction = this.timeEachChannel.get(availChannelIndex);
      double transferTime = this.calculateTransferTime(commitTransfer);
      double startTime = (commitTransfer.getStart_time() > timeLastAction) ? commitTransfer.getStart_time() : timeLastAction;
      double endTime  = startTime + transferTime;

      if(commitTransfer.getFifo().getMapping().getType() == Memory.MEMORY_TYPE.TILE_LOCAL_MEM ||
        (commitTransfer.getFifo().getMapping().getType() == Memory.MEMORY_TYPE.LOCAL_MEM &&
        !commitTransfer.getFifo().getMapping().getEmbeddedToProcessor().equals(commitTransfer.getActor().getMapping()))){
        endTime = startTime + transferTime;  
      }
      else{
        endTime = startTime;
      }

      // update now the commit transfer
      commitTransfer.setStart_time(startTime);
      commitTransfer.setDue_time(endTime);
      // update the channel time 
      this.timeEachChannel.set(availChannelIndex,endTime);
      commitTransfer.setEndOverall(commitTransfer.getDue_time()); 
      // commit transfer
      scheduledActions.get(availChannelIndex).addLast(commitTransfer);
      // then add the scheduled transfers accordingly, with the scheduled due time
      this.addScheduledTransfer(commitTransfer);
    }
  }
  
  public int getAvailableChannel(){
    int availChannelIndex = 0;
    int numberScheduledActions = Integer.MAX_VALUE;
    for (int i=0; i<this.numberofParallelChannels;i++){
      //System.out.println("SIZE CHANNEL:"+scheduledActions.get(i).size()+ " CROSSBAR "+this.getName());
      if (scheduledActions.get(i).size() < numberScheduledActions){
        availChannelIndex = i;
        numberScheduledActions  = scheduledActions.get(i).size(); 
      }
    }
    return availChannelIndex;
  }

  // DUMPING the crossbar utilzation locally
  public void saveCrossbarUtilizationStats(String path) throws IOException{
    try{
        File memUtilStatics = new File(path+"/crossbar-utilization-"+this.getName()+".csv");
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

    FileWriter myWriter = new FileWriter(path+"/crossbar-utilization-"+this.getName()+".csv"); 
    myWriter.write("Job\tStart\tFinish\tResource\n");
    saveCrossbarUtilizationStats(myWriter);

    myWriter.close();
  }

  public void saveCrossbarUtilizationStats(FileWriter myWriter) throws IOException{
    for(int i=0;i<scheduledActions.size();i++){
      for(Transfer transfer : scheduledActions.get(i)){
        String operation = "reading_crossbar";
        if (transfer.getType() == Transfer.TRANSFER_TYPE.WRITE) 
          operation = "writing_crossbar";
        myWriter.write(operation+"\t"+ transfer.getStart_time()+"\t"+transfer.getDue_time()+"\t"+this.getName()+"_"+i+"\n");
      }
    }
  }

}
