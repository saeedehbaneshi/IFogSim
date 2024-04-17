package org.fog.placement;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.test.perfeval.DCNSFog;

public class Controller extends SimEntity{
	
	public static boolean ONLY_CLOUD = false;
		
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	
	private Map<String, Application> applications;
	private Map<String, Integer> appLaunchDelays;

	private Map<String, ModulePlacement> appModulePlacementPolicy;
	
	public Controller(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
		super(name);
		this.applications = new HashMap<String, Application>();
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
		for(FogDevice fogDevice : fogDevices){
			fogDevice.setControllerId(getId());
		}
		setFogDevices(fogDevices);
		setActuators(actuators);
		setSensors(sensors);
		connectWithLatencies();
	}

	private FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices()){
			if(id==fogDevice.getId())
				return fogDevice;
		}
		return null;
	}
	
	private void connectWithLatencies(){
		for(FogDevice fogDevice : getFogDevices()){
			FogDevice parent = getFogDeviceById(fogDevice.getParentId());
			if(parent == null)
				continue;
			double latency = fogDevice.getUplinkLatency();
			parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
			parent.getChildrenIds().add(fogDevice.getId());
		}
	}
	
	@Override
	public void startEntity() {
		for(String appId : applications.keySet()){
			if(getAppLaunchDelays().get(appId)==0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		
		//Saeedeh added this for last update of NIC:
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.LAST_UPDATE);
		//
		
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		
		for(FogDevice dev : getFogDevices())
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);
			break;
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();
			break;
			//Saeedeh added for last update of NIC state and tracking the time:
		case FogEvents.LAST_UPDATE:
			for (FogDevice fogDevice : getFogDevices()) {
				fogDevice.lastUpdate();
			}
			break;
			//
		case FogEvents.STOP_SIMULATION:
			CloudSim.stopSimulation();
			printTimeDetails();
			printPowerDetails();
			printCostDetails();
			printNetworkUsageDetails();
			//Saeedeh added following links energy detail functions:
			//printLinksEnergyDetailsCombinationWifiWired();
			//printLinksEnergyDetailsSpecEcofenBased();
			//printLinksEnergyDetailsMeasuredEcofenBased();
			//printFlowBasedNetworkingEnergyDetails();
			printEnergyTimeMapsDetailsForFlowBaseModel();
			System.exit(0);
			break;
			
		}
	}
	
	private void printNetworkUsageDetails() {
		System.out.println("Total network usage = "+NetworkUsageMonitor.getNetworkUsage()/Config.MAX_SIMULATION_TIME);
		//Saeedeh added energy model base on network usage
		
		System.out.println("Total network Energy = "+NetworkUsageMonitor.getNetworkTotalEnergy());
		
		System.out.println("Total networking Time = "+NetworkUsageMonitor.getTotalNetworkingTime());
		
		/*System.out.println("Network usage-based energy = "+NetworkUsageMonitor.getNetworkUsage()*Config.LINK_POWER);
		//
		System.out.println("links usage = "+NetworkUsageMonitor.getLinksUsage());
		
		for(String deviceId : NetworkUsageMonitor.getLinksUsageMap().keySet()) {
			System.out.println("link usage of device Id : "+ deviceId +" --->= "+NetworkUsageMonitor.getLinksUsageMap().get(deviceId));
		}
		
		for(String deviceId : NetworkUsageMonitor.getUpLinksEnergyMap().keySet()) {
			System.out.println("Uplink energy of device Id : "+ deviceId +" --->= "+NetworkUsageMonitor.getUpLinksEnergyMap().get(deviceId));
		}
		
		for(String deviceId : NetworkUsageMonitor.getDownLinksEnergyMap().keySet()) {
			System.out.println("Downlink energy of device Id : "+ deviceId +" --->= "+NetworkUsageMonitor.getDownLinksEnergyMap().get(deviceId));
		}
		
		for(String deviceId : NetworkUsageMonitor.getDownLinksEnergyMap().keySet()) {
			System.out.println("Total link energy of device Id : "+ deviceId +" --->= "+NetworkUsageMonitor.getTotalLinksEnergyMap().get(deviceId));
		}*/
	}


	private FogDevice getCloud(){
		for(FogDevice dev : getFogDevices())
			if(dev.getName().equals("cloud"))
				return dev;
		return null;
	}
	
	private void printCostDetails(){
		System.out.println("Cost of execution in cloud = "+getCloud().getTotalCost());
	}
	
	private void printPowerDetails() {
		for(FogDevice fogDevice : getFogDevices()){
			System.out.println(fogDevice.getName() + " : Energy Consumed = "+fogDevice.getEnergyConsumption());
			//Saeedeh added this code for reporting application Energy
			System.out.println(fogDevice.getName() + " : Application Energy Consumed = "+fogDevice.getApplicationEnergyConsumption());
			System.out.println(fogDevice.getName()+ " : Vms Energy Map of device : "+fogDevice.getName()+" "+fogDevice.getVmsEnergyMap());
			
			for (String applicationId : fogDevice.getVmsEnergyMap().keySet()) {
				double totalVmsEnergy=0.0;
					Map<String ,Double > innerMap= fogDevice.getVmsEnergyMap().get(applicationId);
					for(String vmName : innerMap.keySet()) {
						double VmsEnergy= innerMap.get(vmName);
						totalVmsEnergy+=VmsEnergy;
					}
				System.out.println(fogDevice.getName()+" : Application " +applicationId+ " total Energy = "+ totalVmsEnergy);
				}
			if (fogDevice.getVmsEnergyMap().isEmpty()) {
				double totalVmsEnergy=0.0;
				System.out.println(fogDevice.getName()+" : Application total Energy = "+ totalVmsEnergy);


			}
			System.out.println(fogDevice.getName() + " : North Link Busy Time = "+fogDevice.getNorthLinkBusyTime());
			System.out.println(fogDevice.getName() + " : North Link Idle Time = "+fogDevice.getNortLinkIdleTime());
			System.out.println(fogDevice.getName() + " : South Link Busy Time = "+fogDevice.getSouthLinkBusyTime());
			System.out.println(fogDevice.getName() + " : South Link Idle Time = "+fogDevice.getSouthLinkIdleTime());
		}
	}
	
	
	//Saeedeh added
	
	private void printLinksEnergyDetailsCombinationWifiWired() {
		for(FogDevice fogDevice : getFogDevices()){
			if(fogDevice.getName().startsWith("m")) {
				double staticEnergy=Config.MAX_SIMULATION_TIME*Config.WiFi_Idle_Power;
				double dynamicEnergy=fogDevice.getNorthLinkBusyTime()*(Config.Wifi_Transmitter_Power+(DCNSFog.numOfCamerasPerArea*Config.Wifi_Reciever_Power));
				double activeEnergy=fogDevice.getNorthLinkBusyTime()*(Config.WiFi_Idle_Power+Config.Wifi_Transmitter_Power+(DCNSFog.numOfCamerasPerArea*Config.Wifi_Reciever_Power));
				//System.out.println(fogDevice.getName() + " : North Link Static Energy = "+staticEnergy);
				//System.out.println(fogDevice.getName() + " : North Link Dynamic Energy = "+dynamicEnergy);
				System.out.println(fogDevice.getName() + " : North Link Total Energy = "+(dynamicEnergy+staticEnergy));
				System.out.println(fogDevice.getName() + " : Active Energy of North Link = "+activeEnergy);
				//System.out.println(fogDevice.getName() + " : South Link Static Energy = "+staticEnergy);
			}else if(fogDevice.getName().startsWith("d")) {
				
				double staticNorthLinkEnergy= Config.MAX_SIMULATION_TIME*Config.Wired_Min_Power;
				double dynamicNorthLinkEnergy= fogDevice.getNorthLinkBusyTime()*(Config.Wired_Max_Power-Config.Wired_Min_Power);
				double activeNorthEnergy=fogDevice.getNorthLinkBusyTime()*Config.Wired_Max_Power;
				//System.out.println(fogDevice.getName() + " : North Link Static Energy = "+staticNorthLinkEnergy);
				//System.out.println(fogDevice.getName() + " : North Link Dynamic Energy = "+dynamicNorthLinkEnergy);
				System.out.println(fogDevice.getName() + " : North Link Total Energy = "+(dynamicNorthLinkEnergy+staticNorthLinkEnergy));
				System.out.println(fogDevice.getName() + " : Active nergy of North Link = "+activeNorthEnergy);
				
				double staticSouthLinkEnergy= Config.MAX_SIMULATION_TIME*Config.WiFi_Idle_Power;
				double dynamicSouthLinkEnergy= fogDevice.getSouthLinkBusyTime()*(Config.Wifi_Transmitter_Power+(DCNSFog.numOfCamerasPerArea*Config.Wifi_Reciever_Power));
				double activeSouthEnergy=fogDevice.getSouthLinkBusyTime()*(Config.WiFi_Idle_Power+Config.Wifi_Transmitter_Power+(DCNSFog.numOfCamerasPerArea*Config.Wifi_Reciever_Power));
				//System.out.println(fogDevice.getName() + " : South Link Static Energy = "+staticSouthLinkEnergy);
				//System.out.println(fogDevice.getName() + " : South Link Static Energy = "+dynamicSouthLinkEnergy);
				System.out.println(fogDevice.getName() + " : South Link Total Energy = "+(dynamicSouthLinkEnergy+staticSouthLinkEnergy));
				System.out.println(fogDevice.getName() + " : Active Energy of South Link = "+activeSouthEnergy);
				}else {
					
					double northLinkStaticEnergy= Config.MAX_SIMULATION_TIME*Config.Wired_Min_Power;
					double northLinkDynamicEnergy= fogDevice.getNorthLinkBusyTime()*(Config.Wired_Max_Power-Config.Wired_Min_Power);
					double northActiveEnergy= fogDevice.getNorthLinkBusyTime()*Config.Wired_Max_Power;
					//System.out.println(fogDevice.getName() + " : North Link Static Energy = "+northLinkStaticEnergy);
					//System.out.println(fogDevice.getName() + " : North Link Dynamic Energy = "+northLinkDynamicEnergy);
					System.out.println(fogDevice.getName() + " : North Link Total Energy = "+(northLinkDynamicEnergy+northLinkStaticEnergy));
					System.out.println(fogDevice.getName() + " : Active Energy of North Link = "+northActiveEnergy);
					
					double southLinkStaticEnergy= Config.MAX_SIMULATION_TIME*Config.Wired_Min_Power;
					double southLinkDynamicEnergy= fogDevice.getSouthLinkBusyTime()*(Config.Wired_Max_Power-Config.Wired_Min_Power);
					double southActiveEnergy= fogDevice.getSouthLinkBusyTime()*Config.Wired_Max_Power;
					//System.out.println(fogDevice.getName() + " : South Link Static Energy = "+southLinkStaticEnergy);
					//System.out.println(fogDevice.getName() + " : South Link Dynamic Energy = "+southLinkDynamicEnergy);
					System.out.println(fogDevice.getName() + " : South Link Total Energy = "+(southLinkDynamicEnergy+southLinkStaticEnergy));
					System.out.println(fogDevice.getName() + " : Active Energy of South Link = "+southActiveEnergy);
				}
			}
		}
	
	
	private void printLinksEnergyDetailsSpecEcofenBased() {
		
		System.out.println(" **** This is NIC time tracking results for ECOFEN based model with specification(Rated MAX power) power parameters **** ");
		
		
		//System.out.println(" **** This is the energy results of simple ECOFEN based model **** ");
		
		for(FogDevice fogDevice : getFogDevices()){
			System.out.println("Fog Device ID: " + fogDevice.getId());
			
			//System.out.println(" NIC Total Idle Time = "+fogDevice.getTotalNetworkInterfaceCardIdleTime());
			//System.out.println(" NIC Total Busy Time = "+fogDevice.getTotalNetworkInterfaceCardBusyTime());
			
			//System.out.println("Uplink Map: " + NetworkUsageMonitor.totalTransmissionDelayUplinkMap);
			//System.out.println("Downlink Map: " + NetworkUsageMonitor.totalTransmissionDelayDownlinkMap);
			
			if(fogDevice.getName().startsWith("m")) {
				double NIC_Device_Total_Energy=fogDevice.getTotalNetworkInterfaceCardIdleTime()*Config.LowEnd_Hub_A_Measured_Idle_Power+fogDevice.getTotalNetworkInterfaceCardBusyTime()*(Config.LowEnd_Hub_A_Rated_Max_Power-Config.LowEnd_Hub_A_Measured_Idle_Power);
				double NIC_App_ActiveEnergy=fogDevice.getTotalNetworkInterfaceCardBusyTime()*Config.LowEnd_Hub_A_Rated_Max_Power;
				
				System.out.println(fogDevice.getName() + " : NIC Device Total Energy = " + NIC_Device_Total_Energy);
				System.out.println(fogDevice.getName() + " : NIC App Active Energy = "+ NIC_App_ActiveEnergy);
					
			}else if(fogDevice.getName().startsWith("c")) {

					double NIC_Device_Total_Energy=fogDevice.getTotalNetworkInterfaceCardIdleTime()*Config.Core_Switch_E_Measured_Idle_Power+fogDevice.getTotalNetworkInterfaceCardBusyTime()*((Config.Core_Switch_E_Rated_Max_Power-Config.Core_Switch_E_Measured_Idle_Power));
					double NIC_App_ActiveEnergy=fogDevice.getTotalNetworkInterfaceCardBusyTime()*Config.Core_Switch_E_Rated_Max_Power;
					
					System.out.println(fogDevice.getName() + " : NIC Device Total Energy = " + NIC_Device_Total_Energy);
					System.out.println(fogDevice.getName() + " : NIC APP Active Energy = "+ NIC_App_ActiveEnergy);

				}else {
						double NIC_Device_Total_Energy=fogDevice.getTotalNetworkInterfaceCardIdleTime()*Config.Edge_Lan_Switch_D_Measured_Idle_Power+fogDevice.getTotalNetworkInterfaceCardBusyTime()*(Config.Edge_Lan_Switch_D_Rated_Max_Power-Config.Edge_Lan_Switch_D_Measured_Idle_Power);
						double NIC_App_ActiveEnergy=fogDevice.getTotalNetworkInterfaceCardBusyTime()*Config.Edge_Lan_Switch_D_Rated_Max_Power;
						
						System.out.println(fogDevice.getName() + " : NIC Device Total Energy = " + NIC_Device_Total_Energy);
						System.out.println(fogDevice.getName() + " : NIC App Active Energy = "+NIC_App_ActiveEnergy);
				}
			}
		}

	
	private void printLinksEnergyDetailsMeasuredEcofenBased() {
		
		System.out.println(" **** This is NIC time tracking results for ECOFEN based model with Measured MAX power parameters **** ");

		for(FogDevice fogDevice : getFogDevices()){
			System.out.println("Fog Device ID: " + fogDevice.getId());
			
			//System.out.println(" NIC Total Idle Time = "+fogDevice.getTotalNetworkInterfaceCardIdleTime());
			//System.out.println(" NIC Total Busy Time = "+fogDevice.getTotalNetworkInterfaceCardBusyTime());
			
			//System.out.println("Uplink Map: " + NetworkUsageMonitor.totalTransmissionDelayUplinkMap);
			//System.out.println("Downlink Map: " + NetworkUsageMonitor.totalTransmissionDelayDownlinkMap);
			
			if(fogDevice.getName().startsWith("m")) {
				double NIC_Device_Total_Energy=fogDevice.getTotalNetworkInterfaceCardIdleTime()*Config.LowEnd_Hub_A_Measured_Idle_Power+fogDevice.getTotalNetworkInterfaceCardBusyTime()*(Config.LowEnd_Hub_A_Measured_Max_Power-Config.LowEnd_Hub_A_Measured_Idle_Power);
				double NIC_App_ActiveEnergy=fogDevice.getTotalNetworkInterfaceCardBusyTime()*Config.LowEnd_Hub_A_Measured_Max_Power;
				
				System.out.println(fogDevice.getName() + " : measured_NIC_Device_Total_Energy = " + NIC_Device_Total_Energy);
				System.out.println(fogDevice.getName() + " : measured_NIC_App_Active_Energy = "+ NIC_App_ActiveEnergy);
					
			}else if(fogDevice.getName().startsWith("c")) {

					double NIC_Device_Total_Energy=fogDevice.getTotalNetworkInterfaceCardIdleTime()*Config.Core_Switch_E_Measured_Idle_Power+fogDevice.getTotalNetworkInterfaceCardBusyTime()*((Config.Core_Switch_E_Measured_Max_Power-Config.Core_Switch_E_Measured_Idle_Power));
					double NIC_App_ActiveEnergy=fogDevice.getTotalNetworkInterfaceCardBusyTime()*Config.Core_Switch_E_Measured_Max_Power;
					
					System.out.println(fogDevice.getName() + " : measured_NIC_Device_Total_Energy = " + NIC_Device_Total_Energy);
					System.out.println(fogDevice.getName() + " : measured_NIC_App_Active_Energy = "+ NIC_App_ActiveEnergy);

				}else {
						double NIC_Device_Total_Energy=fogDevice.getTotalNetworkInterfaceCardIdleTime()*Config.Edge_Lan_Switch_D_Measured_Idle_Power+fogDevice.getTotalNetworkInterfaceCardBusyTime()*(Config.Edge_Lan_Switch_D_Measured_Max_Power-Config.Edge_Lan_Switch_D_Measured_Idle_Power);
						double NIC_App_ActiveEnergy=fogDevice.getTotalNetworkInterfaceCardBusyTime()*Config.Edge_Lan_Switch_D_Measured_Max_Power;
						
						System.out.println(fogDevice.getName() + " : measured_NIC_Device_Total_Energy = " + NIC_Device_Total_Energy);
						System.out.println(fogDevice.getName() + " : measured_NIC_App_Active_Energy = "+NIC_App_ActiveEnergy);
					
					
				}
			}
		}
	
	private void printFlowBasedNetworkingEnergyDetails() {
		System.out.println("\n=========================================");
		System.out.println("============== SINGLE APPLICATION TIME & ENERGY MAP RESULTS ==================");
		for(FogDevice fogDevice: getFogDevices()){
			System.out.println(fogDevice.getName()+" Networking Time Map : "+ fogDevice.getNetworkingTimeMap());
			System.out.println(fogDevice.getName()+" Networking Energy Map : "+ fogDevice.getNetworkingEnergyConsumptionMap());
		}
		System.out.println("=========================================\n");
	}
	
	private void printEnergyTimeMapsDetailsForFlowBaseModel() {

		System.out.println("\n=========================================");
		System.out.println("============== TIME & ENERGY MAP RESULTS ==================");
		for (FogDevice fogDevice: fogDevices) {
			//System.out.println(fogDevice.getId()+" "+fogDevice.getName() + " Networking Tuples Time Map " + fogDevice.getNetworkingTuplesTimeMap());
			System.out.println(fogDevice.getId()+" "+fogDevice.getName() + " Networking Tuples energy Map " + fogDevice.getNetworkingTuplesEnergyMap());

		}
	}
	
	private String getStringForLoopId(int loopId){
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return appId + " " + loop.getModules().toString();
			}
		}
		return null;
	}
	private void printTimeDetails() {
		System.out.println("=========================================");
		System.out.println("============== RESULTS ==================");
		System.out.println("=========================================");
		System.out.println("EXECUTION TIME = "+ (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
		System.out.println("=========================================");
		System.out.println("APPLICATION LOOP DELAYS");
		System.out.println("=========================================");
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){
			/*double average = 0, count = 0;
			for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
				Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
				Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
				if(startTime == null || endTime == null)
					break;
				average += endTime-startTime;
				count += 1;
				System.err.println(average);
			}
			System.out.println(getStringForLoopId(loopId) + " --->= "+(average/count));*/
			
			System.out.println(getStringForLoopId(loopId) + " --->= "+TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
		}
		System.out.println("=========================================");
		System.out.println("TUPLE CPU EXECUTION DELAY");
		System.out.println("=========================================");
		
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()){
			System.out.println(tupleType + " --->= "+TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
		}
		
		System.out.println("=========================================");
		
		
		
		/*Saeedeh All Stack Traces Method:
		Map<Thread,StackTraceElement[]> stackTrace = Thread.getAllStackTraces();
        System.err.println("Displaying All Stack traces using StackTraceElement in Java By Saeedeh ");
        for (Thread t : stackTrace.keySet()) {
        	System.out.println("this is the name of thread : "+ t.getName());
        	for(StackTraceElement st : stackTrace.get(t)){
        		// print the stack trace   
        		System.out.println(st);  
        	}
        }*/
		
		/*//Saeedeh Current Thread Stack Trace Method
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();  
        System.err.println("Displaying Stack trace using StackTraceElement in Java");  
        for(StackTraceElement st : stackTrace){
        	// print the stack trace   
            System.out.println(st);  
        }*/
	}

	protected void manageResources(){
		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
	}
	
	private void processTupleFinished(SimEvent ev) {
	}
	
	@Override
	public void shutdownEntity() {	
	}
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		application.setAppSubmissionDelay(delay);
		
		for(Sensor sensor : sensors){
			sensor.setApp(getApplications().get(sensor.getAppId()));
		}
		for(Actuator ac : actuators){
			ac.setApp(getApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(Actuator actuator : getActuators()){
					if(actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application){
		System.out.println(CloudSim.clock()+" Submitted application "+ application.getAppId());
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		for(FogDevice fogDevice : fogDevices){
			sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
		}
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();

		for(Integer deviceId : deviceToModuleMap.keySet()){
			System.out.println("^^^^^^^^^^^^^^^DeviceId : "+deviceId);
			System.out.println("^^^^^^^^^^^^^^^DeviceName : "+getFogDeviceById(deviceId).getName());

			for(AppModule module : deviceToModuleMap.get(deviceId)){
				System.out.println("^^^^^^^^^^^^^^^ module : "+module.getName());

				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
			}
		}
	}

	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		for(Sensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}
}