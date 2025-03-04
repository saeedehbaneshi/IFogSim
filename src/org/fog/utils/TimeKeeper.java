package org.fog.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Tuple;

public class TimeKeeper {

	private static TimeKeeper instance;
	
	private long simulationStartTime;
	private int count; 
	private Map<Integer, Double> emitTimes;
	private Map<Integer, Double> endTimes;
	private Map<Integer, List<Integer>> loopIdToTupleIds;
	private Map<Integer, Double> tupleIdToCpuStartTime;
	private Map<String, Double> tupleTypeToAverageCpuTime;
	private Map<String, Integer> tupleTypeToExecutedTupleCount;
	
	private Map<Integer, Double> loopIdToCurrentAverage;
	private Map<Integer, Integer> loopIdToCurrentNum;

	private Map<Integer, Integer> loopIdToLatencyQoSSuccessCount = new HashMap<>();

	// loopID -> < Microservice -> < deviceID, <requestCount,totalExecutionTime > >
	private Map<Integer, Map<String, Map<Integer, Pair<Integer, Double>>>> costCalcData = new HashMap<>();
	// last execution time
	private Map<Integer, Double> tupleIdToExecutionTime = new HashMap<>();
	
	public static TimeKeeper getInstance(){
		if(instance == null)
			instance = new TimeKeeper();
		return instance;
	}
	
	public int getUniqueId(){
		return count++;
	}
	
	public void tupleStartedExecution(Tuple tuple){
		//saedeh//System.out.println("tuple started execution");
		tupleIdToCpuStartTime.put(tuple.getCloudletId(), CloudSim.clock());	
		//saeedeh//System.out.println("tupleIdToCpuStartTime : "+tupleIdToCpuStartTime);
	}
	
	public void tupleEndedExecution(Tuple tuple){
		
		/*saeedeh
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();  
        System.out.println("///////////////////////////Displaying Stack trace using StackTraceElement in Java//////////////////////////////");  
        for(StackTraceElement st : stackTrace){
        	// print the stack trace   
            System.out.println(st);  
        }saeedeh*/
		
		
		if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
			return;
		double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
		//saeedeh added this to consider adding 0.1 for early finished events://
		/*if (executionTime<0.1)
			executionTime+=0.1;*/
		////////////////////////////finish/////////////////////////////////////////
		
		if(!tupleTypeToAverageCpuTime.containsKey(tuple.getTupleType())){
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), executionTime);
			tupleTypeToExecutedTupleCount.put(tuple.getTupleType(), 1);
			//saeedeh//System.out.println("Tuple name in if : "+tuple.getTupleType());
			//saeedeh//System.out.println("execution time in if "+executionTime);

		} else{

			double currentAverage = tupleTypeToAverageCpuTime.get(tuple.getTupleType());
			
			int currentCount = tupleTypeToExecutedTupleCount.get(tuple.getTupleType());
			
			//saeedeh//System.out.println("Tuple name in else : "+tuple.getTupleType());
			//saeedeh//System.out.println("Current average :" +currentAverage);
			//saeedeh//System.out.println("Current count :" +currentCount);
			//saeedeh//System.out.println("avg cpu time :" +(currentAverage*currentCount+executionTime)/(currentCount+1));

			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), (currentAverage*currentCount+executionTime)/(currentCount+1));
			//saeedeh//System.out.println("tuple type to avg cpu time :" +tupleTypeToAverageCpuTime.get(tuple.getTupleType()));

		}
        /* Saeedeh
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();  
        System.err.println("Displaying Stack trace using StackTraceElement in Java");  
        for(StackTraceElement st : stackTrace){
        	// print the stack trace   
            System.out.println(st);  
        }*/
		//saeedeh//System.out.println("tuple type :" +tuple.getTupleType());
		//saeedeh//System.out.println("tuple exec time :" +executionTime);
		//saeedeh//System.out.println("tuple type to avg cpu time :" +tupleTypeToAverageCpuTime.get(tuple.getTupleType()));
		//saeedeh//System.out.println("tuple type to avg cpu time :" +tupleTypeToAverageCpuTime);
		//saeedeh//System.out.println("tuple type to executed tuple count :" +tupleTypeToExecutedTupleCount);
		//saeedeh//System.out.println("tuple Id to cpu start time :" +tupleIdToCpuStartTime.get(tuple.getCloudletId()));

	}
	
	public Map<Integer, List<Integer>> loopIdToTupleIds(){
		return getInstance().getLoopIdToTupleIds();
	}
	
	private TimeKeeper(){
		count = 1;
		setEmitTimes(new HashMap<Integer, Double>());
		setEndTimes(new HashMap<Integer, Double>());
		setLoopIdToTupleIds(new HashMap<Integer, List<Integer>>());
		setTupleTypeToAverageCpuTime(new HashMap<String, Double>());
		setTupleTypeToExecutedTupleCount(new HashMap<String, Integer>());
		setTupleIdToCpuStartTime(new HashMap<Integer, Double>());
		setLoopIdToCurrentAverage(new HashMap<Integer, Double>());
		setLoopIdToCurrentNum(new HashMap<Integer, Integer>());
	}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Map<Integer, Double> getEmitTimes() {
		return emitTimes;
	}

	public void setEmitTimes(Map<Integer, Double> emitTimes) {
		this.emitTimes = emitTimes;
	}

	public Map<Integer, Double> getEndTimes() {
		return endTimes;
	}

	public void setEndTimes(Map<Integer, Double> endTimes) {
		this.endTimes = endTimes;
	}

	public Map<Integer, List<Integer>> getLoopIdToTupleIds() {
		return loopIdToTupleIds;
	}

	public void setLoopIdToTupleIds(Map<Integer, List<Integer>> loopIdToTupleIds) {
		this.loopIdToTupleIds = loopIdToTupleIds;
	}

	public Map<String, Double> getTupleTypeToAverageCpuTime() {
		return tupleTypeToAverageCpuTime;
	}

	public void setTupleTypeToAverageCpuTime(
			Map<String, Double> tupleTypeToAverageCpuTime) {
		this.tupleTypeToAverageCpuTime = tupleTypeToAverageCpuTime;
	}

	public Map<String, Integer> getTupleTypeToExecutedTupleCount() {
		return tupleTypeToExecutedTupleCount;
	}

	public void setTupleTypeToExecutedTupleCount(
			Map<String, Integer> tupleTypeToExecutedTupleCount) {
		this.tupleTypeToExecutedTupleCount = tupleTypeToExecutedTupleCount;
	}

	public Map<Integer, Double> getTupleIdToCpuStartTime() {
		return tupleIdToCpuStartTime;
	}

	public void setTupleIdToCpuStartTime(Map<Integer, Double> tupleIdToCpuStartTime) {
		this.tupleIdToCpuStartTime = tupleIdToCpuStartTime;
	}

	public long getSimulationStartTime() {
		return simulationStartTime;
	}

	public void setSimulationStartTime(long simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
	}

	public Map<Integer, Double> getLoopIdToCurrentAverage() {
		return loopIdToCurrentAverage;
	}

	public void setLoopIdToCurrentAverage(Map<Integer, Double> loopIdToCurrentAverage) {
		this.loopIdToCurrentAverage = loopIdToCurrentAverage;
	}

	public Map<Integer, Integer> getLoopIdToCurrentNum() {
		return loopIdToCurrentNum;
	}

	public void setLoopIdToCurrentNum(Map<Integer, Integer> loopIdToCurrentNum) {
		this.loopIdToCurrentNum = loopIdToCurrentNum;
	}

	public Map<Integer, Integer> getLoopIdToLatencyQoSSuccessCount() {
		return loopIdToLatencyQoSSuccessCount;
	}

	public void addCostCalcData(List<Integer> loopIds, String microserviceName, int deviceId, int tupleId) {
//		for (Integer loopid : loopIds) {
//			if (costCalcData.containsKey(loopid)) {
//				if (costCalcData.get(loopid).containsKey(microserviceName)) {
//					if (costCalcData.get(loopid).get(microserviceName).containsKey(deviceId)) {
//						double totalExecutionTime = tupleIdToExecutionTime.get(tupleId) + costCalcData.get(loopid).get(microserviceName).get(deviceId).getSecond();
//						int totalRequestCount = costCalcData.get(loopid).get(microserviceName).get(deviceId).getFirst() + 1;
//						costCalcData.get(loopid).get(microserviceName).put(deviceId, new Pair<>(totalRequestCount, totalExecutionTime));
//					} else {
//						costCalcData.get(loopid).get(microserviceName).put(deviceId, new Pair<>(1, tupleIdToExecutionTime.get(tupleId)));
//					}
//				} else {
//					Map<Integer, Pair<Integer, Double>> m1 = new HashMap<>();
//					m1.put(deviceId, new Pair<>(1, tupleIdToExecutionTime.get(tupleId)));
//
//					costCalcData.get(loopid).put(microserviceName, m1);
//				}
//			} else {
//				Map<Integer, Pair<Integer, Double>> m1 = new HashMap<>();
//				m1.put(deviceId, new Pair<>(1, tupleIdToExecutionTime.get(tupleId)));
//
//				Map<String, Map<Integer, Pair<Integer, Double>>> m2 = new HashMap<>();
//				m2.put(microserviceName, m1);
//
//				costCalcData.put(loopid, m2);
//			}
//		}
	}
	
	
}
