package org.fog.utils;

import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Tuple;

import java.util.HashMap;

public class NetworkUsageMonitor {

	private static double networkUsage = 0.0;
	private static double totalNetworkEnergy = 0.0;
	private static double busyPower= 100;
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
	
	
	
	public static void busyUpLinkEnergy(int id, double delay, double latency, long dataSize){
		double linkEnergy=0;
		linkEnergy= latency* busyPower;
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
		linkEnergy= latency* busyPower;
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
	
	
	/* Saeeddeh public static void updateLinkEnergy() {
		double timeNow= CloudSim.clock();
		
		
		double Lasttime= CloudSim.clock();
		
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
	
	public static double getLinksUsage(){
		return linksUsage;
	}
	
	public static double getTotalNetworkEnergy(){
		return totalNetworkEnergy;
	}
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
