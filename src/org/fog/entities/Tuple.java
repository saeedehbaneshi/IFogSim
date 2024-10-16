package org.fog.entities;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class Tuple extends Cloudlet{

	public static final int UP = 1;
	public static final int DOWN = 2;
	public static final int ACTUATOR = 3;
	
	private String appId;
	
	private String tupleType;
	private String destModuleName;
	private String srcModuleName;
	private int actualTupleId;
	private int direction;
	private int actuatorId;
	private int sourceDeviceId;
	private int sourceModuleId;
	
	/// Saeedeh added these two variable to track start and finish time of sending tuples for tracking networking energy consumption:
	private double startSendingTupleTime;
	private double estimatedFinishSendingTupleTime;
	
	/// Saeedeh added this to track estimated finish time and compare with actual finish time to track and remove outdated VM DC Events:
    private double estimatedFinishTime;

	
	
	/**
	 * Map to keep track of which module instances has a tuple traversed.
	 * 
	 * Map from moduleName to vmId of a module instance
	 */
	private Map<String, Integer> moduleCopyMap;

	/**
	 * For device id based routing used
	 */
	protected int destinationDeviceId;
	/* keep track of traversed microservices by tuples of type UP in microservices architecture UP -> tuple travelling towards service
	 DOWN -> tuple travelling from service to client microservice.*/
	protected Map<String, Integer> traversedMicroservices = new HashMap<>();
	
	public Tuple(String appId, int cloudletId, int direction, long cloudletLength, int pesNumber,
			long cloudletFileSize, long cloudletOutputSize,
			UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize,
				cloudletOutputSize, utilizationModelCpu, utilizationModelRam,
				utilizationModelBw);
		setAppId(appId);
		setDirection(direction);
		setSourceDeviceId(-1);
		setModuleCopyMap(new HashMap<String, Integer>());
		setDestinationDeviceId(-1);
	}

	public int getActualTupleId() {
		return actualTupleId;
	}

	public void setActualTupleId(int actualTupleId) {
		this.actualTupleId = actualTupleId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getTupleType() {
		return tupleType;
	}

	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}

	public String getDestModuleName() {
		return destModuleName;
	}

	public void setDestModuleName(String destModuleName) {
		this.destModuleName = destModuleName;
	}

	public String getSrcModuleName() {
		return srcModuleName;
	}

	public void setSrcModuleName(String srcModuleName) {
		this.srcModuleName = srcModuleName;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getActuatorId() {
		return actuatorId;
	}

	public void setActuatorId(int actuatorId) {
		this.actuatorId = actuatorId;
	}

	public int getSourceDeviceId() {
		return sourceDeviceId;
	}

	public void setSourceDeviceId(int sourceDeviceId) {
		this.sourceDeviceId = sourceDeviceId;
	}

	public Map<String, Integer> getModuleCopyMap() {
		return moduleCopyMap;
	}

	public void setModuleCopyMap(Map<String, Integer> moduleCopyMap) {
		this.moduleCopyMap = moduleCopyMap;
	}

	public int getSourceModuleId() {
		return sourceModuleId;
	}

	public void setSourceModuleId(int sourceModuleId) {
		this.sourceModuleId = sourceModuleId;
	}

	public void setDestinationDeviceId(int deviceId) {
		destinationDeviceId = deviceId;
	}

	public int getDestinationDeviceId() {
		return destinationDeviceId;
	}

	public void addToTraversedMicroservices(Integer deviceID, String microserviceName) {
		traversedMicroservices.put(microserviceName, deviceID);
	}

	public int getDeviceForMicroservice(String microserviceName) {
		if (!traversedMicroservices.containsKey(microserviceName))
			return -1;
		else {
			return traversedMicroservices.get(microserviceName);
		}
	}

	public Map<String, Integer> getTraversed() {
		return traversedMicroservices;
	}

	public void setTraversedMicroservices(Map<String, Integer> traversed) {
		traversedMicroservices = traversed;
	}
	
	
/// Saeedeh added these functions to have access to start and finish sending time of tuple in fog device class:
	
	public void setStartSendingTupleTime(double time) {
		startSendingTupleTime = time;
	}

	public double getStartSendingTupleTime() {
		return startSendingTupleTime;
	}
	
	public void setEstimatedFinishSendingTupleTime(double time) {
		estimatedFinishSendingTupleTime = time;
	}

	public double getEstimatedFinishSendingTupleTime() {
		return estimatedFinishSendingTupleTime;
	}
/// Saeedeh added mentioned functions to this class(4 last functions)
	
	/// Saeedeh added this to track estimated finish time and compare with actual finish time to track and remove outdated VM DC Events:
	public void setEstimatedFinishTime(double time) {
        this.estimatedFinishTime = time;
    }

    public double getEstimatedFinishTime() {
        return this.estimatedFinishTime;
    }
}
