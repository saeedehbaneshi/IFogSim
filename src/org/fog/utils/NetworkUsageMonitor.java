package org.fog.utils;

import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Tuple;

import java.util.HashMap;

public class NetworkUsageMonitor {

	private static double networkUsage = 0.0;
	private static double networkTotalEnergy = 0.0;
	private static double totalNetworkingTime = 0.0;
	public static Map<Integer, Double>  totalTransmissionDelayUplinkMap = new HashMap<Integer, Double>();
	public static Map<Integer, Double>  totalTransmissionDelayDownlinkMap = new HashMap<Integer, Double>();
	//Saeedeh// private static double lastTime = 0.0;
	protected static double wiredLinkEnergy;
	
	private static double totalPower= 3.72;
	static Map<String, Double> upLinksEnergyMap = new HashMap<String, Double>();
	static Map<String, Double> downLinksEnergyMap = new HashMap<String, Double>();
	static Map<String, Double> totalLinksEnergyMap = new HashMap<String, Double>();
	private static double linksUsage = 0;
	static Map<String, Double> linksUsageMap = new HashMap<String, Double>();



	static Map<Integer, Pair<Double, Double>> tupleWaitingTimeMap = new HashMap<Integer, Pair<Double, Double>>();
	
	public static void sendingTuple(double latency, double tupleNwSize){
		networkUsage += latency*tupleNwSize;
	}
	//Saeedeh added for links energy
	/*public static void busyLinkEnergy(int id, double latency, double delay){
		double linkEnergy=0;
		linkEnergy= latency* powerPerPaket;
		String newId = String.valueOf(id);
		if(linksEnergyMap.containsKey(newId)) {
			
			linksEnergyMap.put(String.valueOf(newId), linksEnergyMap.get(newId)+linkEnergy);
			
		}
		else {
			linksEnergyMap.put(newId, linkEnergy);	
			}
	}*/
	
	
	//Saeedeh added this function to track total transmission delay of fog devices for Ecofen based models
	public static void updateTotalTransmissionDelayUplinkMap(int id, double networkDelay){
		if (totalTransmissionDelayUplinkMap.containsKey(id)) {
            double currentDelay = totalTransmissionDelayUplinkMap.get(id);
            totalTransmissionDelayUplinkMap.put(id, currentDelay + networkDelay);
        } else {
        	totalTransmissionDelayUplinkMap.put(id, networkDelay);
        }
	}

	public static void updateTotalTransmissionDelayDownlinkMap(int id, double networkDelay){
		if (totalTransmissionDelayDownlinkMap.containsKey(id)) {
            double currentDelay = totalTransmissionDelayDownlinkMap.get(id);
            totalTransmissionDelayDownlinkMap.put(id, currentDelay + networkDelay);
        } else {
        	totalTransmissionDelayDownlinkMap.put(id, networkDelay);
        }
	}
	
	
	public static void busyUpLinkEnergy(int id, double delay, double latency, long dataSize){
		double linkEnergy=0;
		linkEnergy= (latency+delay)* totalPower;
		//linkEnergy= delay* totalPower;
		totalNetworkingTime +=delay;
		networkTotalEnergy+= linkEnergy;
		String newId = String.valueOf(id);
		//String newChildId = String.valueOf(childId);
		linksUsage+= dataSize*latency;
		if(upLinksEnergyMap.containsKey(newId)) {
			
			upLinksEnergyMap.put(String.valueOf(newId), upLinksEnergyMap.get(newId)+linkEnergy);
			totalLinksEnergyMap.put(String.valueOf(newId), totalLinksEnergyMap.get(newId)+linkEnergy);
			
		}
		else {
			upLinksEnergyMap.put(newId, linkEnergy);
			if(totalLinksEnergyMap.containsKey(newId)){
				totalLinksEnergyMap.put(String.valueOf(newId), totalLinksEnergyMap.get(newId)+linkEnergy);
				}
			else {
				totalLinksEnergyMap.put(newId, linkEnergy);	
			}
			totalLinksEnergyMap.put(newId, linkEnergy);	

			}
		
	}
	
	public static void busyDownLinkEnergy(int childId, double delay, double latency, long dataSize){
		double linkEnergy=0;
		linkEnergy= (latency+delay)* totalPower;
		//linkEnergy= delay* totalPower;
		totalNetworkingTime +=delay;
		networkTotalEnergy+= linkEnergy;
		String newId = String.valueOf(childId);
		linksUsage+= dataSize*latency;
		if(downLinksEnergyMap.containsKey(newId)) {
			
			downLinksEnergyMap.put(String.valueOf(newId), downLinksEnergyMap.get(newId)+linkEnergy);
			totalLinksEnergyMap.put(String.valueOf(newId), totalLinksEnergyMap.get(newId)+linkEnergy);
			
		}
		else {
			downLinksEnergyMap.put(newId, linkEnergy);
			if(totalLinksEnergyMap.containsKey(newId)){
				totalLinksEnergyMap.put(String.valueOf(newId), totalLinksEnergyMap.get(newId)+linkEnergy);
				}
			else {
				totalLinksEnergyMap.put(newId, linkEnergy);	
			}

		}
		
	}
	
	
	// Saeeddeh 
	
	/* public static void updateWiredLinkEnergy(double idlePower, double busyPower, double tupleNwSize, double bw) {
		double timeNow= CloudSim.clock();
		double powerSlope= busyPower -idlePower;
		double linkUsage= tupleNwSize/bw;
		double linkDynamicPower= powerSlope*linkUsage;
		
		double LinkTotalPower=idlePower+linkDynamicPower;
		double currentWiredLinkEnergy= getWiredLinkEnergy();
		double newWiredLinkEnergy = currentWiredLinkEnergy + (timeNow-lastTime)*LinkTotalPower;
		setWiredLinkEnergy(newWiredLinkEnergy);
		
		lastTime= timeNow;
		
	}*/
	
	
	public static void updateLinksUsageMap(int id, double delay, double latency, long dataSize){
	
		if(linksUsageMap.containsKey(String.valueOf(id))) {
			linksUsageMap.put(String.valueOf(id), linksUsageMap.get(String.valueOf(id))+(latency*dataSize));
		}
		else {
			linksUsageMap.put(String.valueOf(id), latency*dataSize);
		}
		
	}
	
	
	
	
	public static void idleLinkEnergy(Integer tupleId, double startTime, double endTime){

		
	}
	
	public static void sendingModule(double latency, long moduleSize){
		networkUsage += latency*moduleSize;
	}
	
	public static double getNetworkUsage(){
		return networkUsage;
	}
	
	public static double getNetworkTotalEnergy(){
		return networkTotalEnergy;
	}
	
	public static double getWiredLinkEnergy(){
		return wiredLinkEnergy;
	}
	
	/* Saeedeh public static void setWiredLinkEnergy(double wiredLinkEnergy){
		NetworkUsageMonitor.wiredLinkEnergy= wiredLinkEnergy;
	}*/
	
	public static double getTotalNetworkingTime(){
		return totalNetworkingTime;
	}
	
	/*public static double getTotalTransmissionDelay(){
		return totalTransmissionDelay;
	}*/
	
	public static double getLinksUsage(){
		return linksUsage;
	}
	
	/*public static double getTotalNetworkEnergy(){
		return totalNetworkEnergy;
	}*/
	//Saeedeh
	public static Map<String, Double> getUpLinksEnergyMap(){
		return upLinksEnergyMap;
	}
	
	
	public static Map<String, Double> getLinksUsageMap(){
		return linksUsageMap;
	}
	
	
	public static Map<String, Double> getDownLinksEnergyMap(){
		return downLinksEnergyMap;
	}
	
	public static Map<String, Double> getTotalLinksEnergyMap(){
		return totalLinksEnergyMap;
	}
	
	public static Map<Integer, Pair<Double, Double>> getTupleWaitingTimeMap(){
		return tupleWaitingTimeMap;
	}
}
