package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.utils.Logger;

public class ModulePlacementEdgewardsOrginal extends ModulePlacement{
	
	protected ModuleMapping moduleMapping;
	protected List<Sensor> sensors;
	protected List<Actuator> actuators;
	protected Map<Integer, Double> currentCpuLoad;
	
	/**
	 * Stores the current mapping of application modules to fog devices 
	 */
	protected Map<Integer, List<String>> currentModuleMap;
	protected Map<Integer, Map<String, Double>> currentModuleLoadMap;
	protected Map<Integer, Map<String, Integer>> currentModuleInstanceNum;
	
	public ModulePlacementEdgewardsOrginal(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, 
			Application application, ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		setSensors(sensors);
		setActuators(actuators);
		setCurrentCpuLoad(new HashMap<Integer, Double>());
		setCurrentModuleMap(new HashMap<Integer, List<String>>());
		setCurrentModuleLoadMap(new HashMap<Integer, Map<String, Double>>());
		setCurrentModuleInstanceNum(new HashMap<Integer, Map<String, Integer>>());
		for(FogDevice dev : getFogDevices()){
			getCurrentCpuLoad().put(dev.getId(), 0.0);
			getCurrentModuleLoadMap().put(dev.getId(), new HashMap<String, Double>());
			getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
			getCurrentModuleInstanceNum().put(dev.getId(), new HashMap<String, Integer>());
		}
		
		mapModules();
		setModuleInstanceCountMap(getCurrentModuleInstanceNum());
	}
	
	@Override
	protected void mapModules() {
		//
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
			//System.err.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ device name is : "+ deviceName);
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){
				int deviceId = CloudSim.getEntityId(deviceName);
				getCurrentModuleMap().get(deviceId).add(moduleName);
				getCurrentModuleLoadMap().get(deviceId).put(moduleName, 0.0);
				getCurrentModuleInstanceNum().get(deviceId).put(moduleName, 0);
				//System.err.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ module name is : "+moduleName);
			}
		}
		
		List<List<Integer>> leafToRootPaths = getLeafToRootPaths();
		//*Saeedeh*//for each path of devices from leaf to cloud place modules on devices of path
		for(List<Integer> path : leafToRootPaths){
			//System.err.println("this is path = "+ path);
			placeModulesInPath(path);
			System.out.format("\n\nCurrentModuleLoadMap: %s\nCurrentInstances: %s\nCurrentCPULOAD: %s\\n\\n",getCurrentModuleLoadMap(),getCurrentModuleInstanceNum(),getCurrentCpuLoad());
		}
		System.out.format("Instances:%s\n",getCurrentModuleInstanceNum());
		
		for(int deviceId : getCurrentModuleMap().keySet()){
			System.out.println("'''deviceId'''"+deviceId);
			for(String module : getCurrentModuleMap().get(deviceId)){
				System.out.println("'''module'''"+module);
				createModuleInstanceOnDevice(getApplication().getModuleByName(module), getFogDeviceById(deviceId));
			}
		}
	}
	
	/**
	 * Get the list of modules that are ready to be placed 
	 * @param placedModules Modules that have already been placed in current path
	 * @return list of modules ready to be placed
	 */
	private List<String> getModulesToPlace(List<String> placedModules){
		Application app = getApplication();
		List<String> modulesToPlace_1 = new ArrayList<String>();
		List<String> modulesToPlace = new ArrayList<String>();
		for(AppModule module : app.getModules()){
			if(!placedModules.contains(module.getName()))
				modulesToPlace_1.add(module.getName());
		}
		/*
		 * Filtering based on whether modules (to be placed) lower in physical topology are already placed
		 */
		for(String moduleName : modulesToPlace_1){
			boolean toBePlaced = true;
			
			for(AppEdge edge : app.getEdges()){
				//CHECK IF OUTGOING DOWN EDGES ARE PLACED
				if(edge.getSource().equals(moduleName) && edge.getDirection()==Tuple.DOWN && !placedModules.contains(edge.getDestination()))
					toBePlaced = false;
				//CHECK IF INCOMING UP EDGES ARE PLACED
				if(edge.getDestination().equals(moduleName) && edge.getDirection()==Tuple.UP && !placedModules.contains(edge.getSource()))
					toBePlaced = false;
			}
			if(toBePlaced)
				modulesToPlace.add(moduleName);
		}

		return modulesToPlace;
	}
	
	protected double getRateOfSensor(String sensorType){
		for(Sensor sensor : getSensors()){
			if(sensor.getTupleType().equals(sensorType))
				return 1/sensor.getTransmitDistribution().getMeanInterTransmitTime();
		}
		return 0;
	}
	
	private void placeModulesInPath(List<Integer> path) {
		System.out.println(currentModuleMap);
		System.out.println(path);
		if(path.size()==0)return;
		List<String> placedModules = new ArrayList<String>();
		Map<AppEdge, Double> appEdgeToRate = new HashMap<AppEdge, Double>();
		
		/**
		 * Periodic edges have a fixed periodicity of tuples, so setting the tuple rate beforehand
		 */
		for(AppEdge edge : getApplication().getEdges()){
			if(edge.isPeriodic()){
				//saeedeh//System.err.println(String.format("periodec edge: %s", edge.toString()));
				//saeedeh//System.err.println(String.format("periodecicity of edge: %s", edge.getPeriodicity()));
				appEdgeToRate.put(edge, 1/edge.getPeriodicity());
			}
		}
		
		for(Integer deviceId : path){		
			FogDevice device = getFogDeviceById(deviceId);
			//saeedeh//System.out.format("'''Exploring Device: '''%d (%s)\n",deviceId,device.getName());
			Map<String, Integer> sensorsAssociated = getAssociatedSensors(device);
			Map<String, Integer> actuatorsAssociated = getAssociatedActuators(device);
			
			//saeedeh//System.out.format("Associated Sensors: %s\n",sensorsAssociated);
			placedModules.addAll(sensorsAssociated.keySet()); // ADDING ALL SENSORS TO PLACED LIST
			placedModules.addAll(actuatorsAssociated.keySet()); // ADDING ALL ACTUATORS TO PLACED LIST
			/*
			 * Setting the rates of application edges emanating from sensors
			 */
			for(String sensor : sensorsAssociated.keySet()){
				for(AppEdge edge : getApplication().getEdges()){
					if(edge.getSource().equals(sensor)){
						//System.err.println(String.format("Sendor:%s edge: %s",sensor, edge.toString()));
						appEdgeToRate.put(edge, sensorsAssociated.get(sensor)*getRateOfSensor(sensor));
					}
				}
			}
						
			/*
			 * Updating the AppEdge rates for the entire application based on knowledge so far
			 */
			boolean changed = true;
			while(changed){		//Loop runs as long as some new information is added
				changed=false;	
				Map<AppEdge, Double> rateMap = new HashMap<AppEdge, Double>(appEdgeToRate);
				//System.err.println("******************\n start \n ****************************");
				//System.err.println(rateMap);
				//System.err.println(rateMap.keySet());
				//System.err.println("******************\n end \n ******************************");
				//System.err.println("\nNext Loop\n");
				for(AppEdge edge : rateMap.keySet()){
					//System.err.println(rateMap.gsudo nano /usr/share/X11/xorg.conf.d/99-disabletouch.confet(edge));
					//System.err.println(String.format("Dest is:%s",edge.getDestination()));
					//System.err.println(edge.getTupleType());
					AppModule destModule = getApplication().getModuleByName(edge.getDestination());
					if(destModule == null)continue;
					Map<Pair<String, String>, SelectivityModel> map = destModule.getSelectivityMap();
					for(Pair<String, String> pair : map.keySet()){
						//System.err.println(String.format("KeySet pair is: %s",pair));
						//System.err.println("******************\n\n\n***************");
						if(pair.getFirst().equals(edge.getTupleType())){
							double outputRate = appEdgeToRate.get(edge)*map.get(pair).getMeanRate(); // getting mean rate from SelectivityModel
							//saeedeh//System.err.println("pair is :=" +pair);
							//saeedeh//System.err.println("mean rate selectivity is :=" +map.get(pair).getMeanRate());
							AppEdge outputEdge = getApplication().getEdgeMap().get(pair.getSecond());
							if(!appEdgeToRate.containsKey(outputEdge) || appEdgeToRate.get(outputEdge)!=outputRate){
								// if some new information is available
								changed = true;
							}
							appEdgeToRate.put(outputEdge, outputRate);
						}
					}
				}
			}
			
			/*
			 * Getting the list of modules ready to be placed on current device on path
			 */
			//Saeedeh: Be carefull, that this is based on placedModules, which is initialized as an empty lists at
			//start of this function (just sensors and actuators are added to it at start of this function)
			List<String> modulesToPlace = getModulesToPlace(placedModules);
			//saeedeh//System.err.println("****************************************************************************\n start \n****************************************************************************");
			//saeedeh//System.err.println(String.format("Device name is : %s   AND   device id is : %d",device.getName(), device.getId()));
			//saeedeh//System.err.println(String.format("This is Placed modules last element: %s   And   placedmodule list size= %d",placedModules.get(placedModules.size()-1), placedModules.size()));
			//saeedeh//System.err.println(String.format("This is modules to place first element : %s",modulesToPlace.get(0)));
			while(modulesToPlace.size() > 0){ // Loop runs until all modules in modulesToPlace are deployed in the path
				String moduleName = modulesToPlace.get(0);
				//saeedeh//System.out.println(String.format("'''Module to place name is''' %s ", moduleName));

				double totalCpuLoad = 0;
				
				//IF MODULE IS ALREADY PLACED UPSTREAM, THEN UPDATE THE EXISTING MODULE
				//Saeedeh: Be careful, that this is determined based on currentModuleMap attribute of this class
				//And it is filled based on mapping that user defined earlier
				int upsteamDeviceId = isPlacedUpstream(moduleName, path);
				//saeedeh//System.out.println("Device ="+device.getName());
				//saeedeh//System.out.println(String.format("Module to place name is %s ", moduleName));
				//saeedeh//System.err.println(String.format("This is upsteamDeviceId : %d", upsteamDeviceId));
				if(upsteamDeviceId > 0){
					//saeedeh//System.err.println("//////////////////////// Module is mapped in this path /////////////////////////");
					if(upsteamDeviceId==deviceId){
						//*saeedeh*//System.out.println("module should place in current device");
						placedModules.add(moduleName);
						modulesToPlace = getModulesToPlace(placedModules);
						//saeedeh/System.err.println(String.format("this module deployed : %s", moduleName));
						
						// NOW THE MODULE TO PLACE IS IN THE CURRENT DEVICE. CHECK IF THE NODE CAN SUSTAIN THE MODULE
						for(AppEdge edge : getApplication().getEdges()){		// take all incoming edges to the current module and find the rate of them
							if(edge.getDestination().equals(moduleName)){
								double rate = appEdgeToRate.get(edge);
								totalCpuLoad += rate*edge.getTupleCpuLength();  //*Saeedeh// calculating total cpu load of task(tuple)
								//saeedeh//System.out.println("'''rate = '''" +rate + "");
								//saeedeh//System.out.println("'''edge.getTupleCpuLength() = '''" +edge.getTupleCpuLength() + "");
								//saeedeh//System.out.println("'''totalCpuLoad''' = rate* tupleCpuLength = " +totalCpuLoad + "");
							}
						}
						//saeedeh//System.out.println("totalCpuLoad + getCurrentCpuLoad(device Id)= "+ (totalCpuLoad + getCurrentCpuLoad().get(deviceId)));
						//saeedeh//System.out.println("'''In module placement'''device Total Mips= "+device.getHost().getTotalMips()+ "");
						if(totalCpuLoad + getCurrentCpuLoad().get(deviceId) > device.getHost().getTotalMips()){
							Logger.debug("ModulePlacementEdgeward", "Need to shift module "+moduleName+" upstream from device " + device.getName());
							System.err.println(String.format("This module   %s    Need to shift up from this device   %s", moduleName, device.getName()));
							/*Saeedeh
							System.out.println(moduleName);
							System.out.println(totalCpuLoad);
							System.out.println(deviceId);
							System.out.println(modulesToPlace);*/
							List<String> _placedOperators = shiftModuleNorth(moduleName, totalCpuLoad, deviceId, modulesToPlace);
							
							for(String placedOperator : _placedOperators){
								if(!placedModules.contains(placedOperator))
									placedModules.add(placedOperator);
							}
						} else{
							placedModules.add(moduleName);
							getCurrentCpuLoad().put(deviceId, getCurrentCpuLoad().get(deviceId)+totalCpuLoad);
							getCurrentModuleInstanceNum().get(deviceId).put(moduleName, getCurrentModuleInstanceNum().get(deviceId).get(moduleName)+1);
							Logger.debug("ModulePlacementEdgeward", "AppModule "+moduleName+" can be created on device "+device.getName());
							System.out.println(String.format("ModulePlacementEdgeward   %s    Can be created on device   %s", moduleName, device.getName()));
						}
					}
					else {
						//saeedeh//System.out.println("This Module should map on another device in this path ");
					}
					//saeedeh//System.out.println("Device "+device.getName()+ " CurrentCpuLoad is = " +getCurrentCpuLoad().get(deviceId) + "");

				}
				
				
				
				
				
				
				
				
				
				else{
					// FINDING OUT WHETHER PLACEMENT OF OPERATOR ON DEVICE IS POSSIBLE
					//saeedeh//System.err.println("//////////////////////// module is not mapped in this path /////////////////////////");
					for(AppEdge edge : getApplication().getEdges()){		// take all incoming edges
						if(edge.getDestination().equals(moduleName)){
							double rate = appEdgeToRate.get(edge);
							totalCpuLoad += rate*edge.getTupleCpuLength();
							//saeedeh//System.out.println("rate is = " +rate + "");
							//saeedeh//System.out.println("edge.getTupleCpuLength is = " +edge.getTupleCpuLength() + "");
							//saeedeh//System.out.println("totalCpuLoad is = " +totalCpuLoad + "");

						}
					}
					//saeedeh//System.out.println("totalCpuLoad + getCurrentCpuLoad(device Id)= "+ (totalCpuLoad+getCurrentCpuLoad().get(deviceId)));
					//saeedehSystem.out.println("device Total Mips()="+device.getHost().getTotalMips()+ "");
					if(totalCpuLoad + getCurrentCpuLoad().get(deviceId) > device.getHost().getTotalMips()){
						Logger.debug("ModulePlacementEdgeward", "Placement of operator "+moduleName+ "NOT POSSIBLE on device "+device.getName());
						System.err.println("ModulePlacementEdgeward of operator "+moduleName+ " on device "+device.getName() + " is NOT POSSIBLE.");

					}
					else{
						Logger.debug("ModulePlacementEdgeward", "Placement of operator "+moduleName+ " on device "+device.getName() + " successful.");
						getCurrentCpuLoad().put(deviceId, totalCpuLoad + getCurrentCpuLoad().get(deviceId));
						System.out.println("ModulePlacementEdgeward of operator "+moduleName+ " on device "+device.getName() + " successful.");
						System.out.println("Device "+device.getName()+ "CurrentCpuLoad is = " +getCurrentCpuLoad().get(deviceId));

						if(!currentModuleMap.containsKey(deviceId))
							currentModuleMap.put(deviceId, new ArrayList<String>());
						currentModuleMap.get(deviceId).add(moduleName);
						placedModules.add(moduleName);
						modulesToPlace = getModulesToPlace(placedModules);
						getCurrentModuleLoadMap().get(device.getId()).put(moduleName, totalCpuLoad);
						
						int max = 1;
						for(AppEdge edge : getApplication().getEdges()){
							if(edge.getSource().equals(moduleName) && actuatorsAssociated.containsKey(edge.getDestination()))
								max = Math.max(actuatorsAssociated.get(edge.getDestination()), max);
							if(edge.getDestination().equals(moduleName) && sensorsAssociated.containsKey(edge.getSource()))
								max = Math.max(sensorsAssociated.get(edge.getSource()), max);
						}
						getCurrentModuleInstanceNum().get(deviceId).put(moduleName, max);
					}
				}
			
				modulesToPlace.remove(moduleName);
			}
			
		}
		
	}

	/**
	 * Shifts a module moduleName from device deviceId northwards. This involves other modules that depend on it to be shifted north as well.
	 * @param moduleName
	 * @param cpuLoad cpuLoad of the module
	 * @param deviceId
	 */
	private List<String> shiftModuleNorth(String moduleName, double cpuLoad, Integer deviceId, List<String> operatorsToPlace) {
		System.out.println(CloudSim.getEntityName(deviceId)+" is shifting "+moduleName+" north.");
		List<String> modulesToShift = findModulesToShift(moduleName, deviceId);
		
		Map<String, Integer> moduleToNumInstances = new HashMap<String, Integer>(); // Map of number of instances of modules that need to be shifted
		double totalCpuLoad = 0;
		Map<String, Double> loadMap = new HashMap<String, Double>();
		for(String module : modulesToShift){
			loadMap.put(module, getCurrentModuleLoadMap().get(deviceId).get(module));
			//saeedeh//System.out.println("8888888888888888888888 number instance" +getCurrentModuleInstanceNum().get(deviceId).get(module));
			moduleToNumInstances.put(module, getCurrentModuleInstanceNum().get(deviceId).get(module)+1);
			totalCpuLoad += getCurrentModuleLoadMap().get(deviceId).get(module);
			//saeedeh//System.out.println("00000000 totalCpuLoad = "+ totalCpuLoad);
			//saeedeh//System.out.println("00000000 getCurrentCpuLoad = "+ getCurrentCpuLoad().get(deviceId));

			getCurrentModuleLoadMap().get(deviceId).remove(module);
			getCurrentModuleMap().get(deviceId).remove(module);
			getCurrentModuleInstanceNum().get(deviceId).remove(module);
			System.out.println(String.format("Module to shift is %s ", module));

		}

		getCurrentCpuLoad().put(deviceId, getCurrentCpuLoad().get(deviceId)-totalCpuLoad); // change info of current CPU load on device
		//saeedeh//System.out.println("11111111 moduleName = " +moduleName);


		loadMap.put(moduleName, loadMap.get(moduleName)+cpuLoad);
		
		//saeedeh//System.out.println("22222222 cpuLoad = " +cpuLoad);
		//saeedeh//System.out.println("22222222 loadMap = " +loadMap);
		//saeedeh//System.out.println("22222222 moduleName = " +moduleName);
		//saeedeh//System.out.println(" totalCpuLoad = "+ totalCpuLoad);


		totalCpuLoad += cpuLoad;
		
		//saeedeh//System.out.println(" getCurrentCpuLoad = "+ getCurrentCpuLoad().get(deviceId));
		//saeedeh//System.out.println(" totalCpuLoad = "+ totalCpuLoad);

		
		int id = getParentDevice(deviceId);
		while(true){ // Loop iterates over all devices in path upstream from current device. Tries to place modules (to be shifted northwards) on each of them.
			if(id==-1){
				// Loop has reached the apex fog device in hierarchy, and still could not place modules. 
				Logger.debug("ModulePlacementEdgeward", "Could not place modules "+modulesToShift+" northwards.");
				break;
			}
			FogDevice fogDevice = getFogDeviceById(id);
			System.out.println(String.format("Device = %s ",fogDevice.getName()));
			//saeedeh//System.out.println("totalCpuLoad + getCurrentCpuLoad = "+ (totalCpuLoad + getCurrentCpuLoad().get(id)));

			//saeedeh//System.out.println("device Total Mips= "+fogDevice.getHost().getTotalMips()+ "");

			
			if(getCurrentCpuLoad().get(id) + totalCpuLoad > fogDevice.getHost().getTotalMips()){
				// Device cannot take up CPU load of incoming modules. Keep searching for device further north.
				List<String> _modulesToShift = findModulesToShift(modulesToShift, id);	// All modules in _modulesToShift are currently placed on device id
				double cpuLoadShifted = 0;		// the total CPU load shifted from device id to its parent
				for(String module : _modulesToShift){
					if(!modulesToShift.contains(module)){
						// Add information of all newly added modules (to be shifted) 
						moduleToNumInstances.put(module, getCurrentModuleInstanceNum().get(id).get(module)+moduleToNumInstances.get(module));
						loadMap.put(module, getCurrentModuleLoadMap().get(id).get(module));
						cpuLoadShifted += getCurrentModuleLoadMap().get(id).get(module);
						totalCpuLoad += getCurrentModuleLoadMap().get(id).get(module);
						// Removing information of all modules (to be shifted north) in device with ID id 
						getCurrentModuleLoadMap().get(id).remove(module);
						getCurrentModuleMap().get(id).remove(module);
						getCurrentModuleInstanceNum().get(id).remove(module);
					}					
				}
				getCurrentCpuLoad().put(id, getCurrentCpuLoad().get(id)-cpuLoadShifted); // CPU load on device id gets reduced due to modules shifting northwards
				//saeedehSystem.out.println(" getCurrentCpuLoad = "+ getCurrentCpuLoad().get(id));

				modulesToShift = _modulesToShift;
				id = getParentDevice(id); // iterating to parent device
			} else{
				// Device (@ id) can accommodate modules. Placing them here.
				double totalLoad = 0;
				for(String module : loadMap.keySet()){
					totalLoad += loadMap.get(module);
					System.out.println("ModulePlacement shifting of operator "+module+ " to device "+fogDevice.getName() + " successful.");
					getCurrentModuleLoadMap().get(id).put(module, loadMap.get(module));
					getCurrentModuleMap().get(id).add(module);
					String module_ = module;
					int initialNumInstances = 0;
					if(getCurrentModuleInstanceNum().get(id).containsKey(module_))
						initialNumInstances = getCurrentModuleInstanceNum().get(id).get(module_);
					int finalNumInstances = initialNumInstances + moduleToNumInstances.get(module_);
					getCurrentModuleInstanceNum().get(id).put(module_, finalNumInstances);
				}
				getCurrentCpuLoad().put(id, getCurrentCpuLoad().get(id)+totalLoad);
				//saeedeh//System.out.println("Device " +fogDevice.getName()+ " CURRENT CPU LOAD = " +getCurrentCpuLoad().get(id));
				operatorsToPlace.removeAll(loadMap.keySet());
				List<String> placedOperators = new ArrayList<String>();
				for(String op : loadMap.keySet())placedOperators.add(op);
				return placedOperators;
			}
		}
		return new ArrayList<String>();
	}

	/**
	 * Get all modules that need to be shifted northwards along with <b>module</b>.  
	 * Typically, these other modules are those that are hosted on device with ID <b>deviceId</b> and lie upstream of <b>module</b> in application model. 
	 * @param module the module that needs to be shifted northwards
	 * @param deviceId the fog device ID that it is currently on
	 * @return list of all modules that need to be shifted north along with <b>module</b>
	 */
	private List<String> findModulesToShift(String module, Integer deviceId) {
		List<String> modules = new ArrayList<String>();
		modules.add(module);
		return findModulesToShift(modules, deviceId);
		/*List<String> upstreamModules = new ArrayList<String>();
		upstreamModules.add(module);
		boolean changed = true;
		while(changed){ // Keep loop running as long as new information is added.
			changed = false;
			for(AppEdge edge : getApplication().getEdges()){
				
				 * If there is an application edge UP from the module to be shifted to another module in the same device
				 
				if(upstreamModules.contains(edge.getSource()) && edge.getDirection()==Tuple.UP && 
						getCurrentModuleMap().get(deviceId).contains(edge.getDestination()) 
						&& !upstreamModules.contains(edge.getDestination())){
					upstreamModules.add(edge.getDestination());
					changed = true;
				}
			}
		}
		return upstreamModules;	*/
	}
	/**
	 * Get all modules that need to be shifted northwards along with <b>modules</b>.  
	 * Typically, these other modules are those that are hosted on device with ID <b>deviceId</b> and lie upstream of modules in <b>modules</b> in application model. 
	 * @param module the module that needs to be shifted northwards
	 * @param deviceId the fog device ID that it is currently on
	 * @return list of all modules that need to be shifted north along with <b>modules</b>
	 */
	private List<String> findModulesToShift(List<String> modules, Integer deviceId) {
		List<String> upstreamModules = new ArrayList<String>();
		upstreamModules.addAll(modules);
		boolean changed = true;
		while(changed){ // Keep loop running as long as new information is added.
			changed = false;
			/*
			 * If there is an application edge UP from the module to be shifted to another module in the same device
			 */
			for(AppEdge edge : getApplication().getEdges()){
				if(upstreamModules.contains(edge.getSource()) && edge.getDirection()==Tuple.UP && 
						getCurrentModuleMap().get(deviceId).contains(edge.getDestination()) 
						&& !upstreamModules.contains(edge.getDestination())){
					upstreamModules.add(edge.getDestination());
					changed = true;
				}
			}
		}
		return upstreamModules;	
	}
	
	private int isPlacedUpstream(String operatorName, List<Integer> path) {
		for(int deviceId : path){
			if(currentModuleMap.containsKey(deviceId) && currentModuleMap.get(deviceId).contains(operatorName))
				return deviceId;
		}
		return -1;
	}

	/**
	 * Gets all sensors associated with fog-device <b>device</b>
	 * @param device
	 * @return map from sensor type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedSensors(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for(Sensor sensor : getSensors()){
			if(sensor.getGatewayDeviceId()==device.getId()){
				if(!endpoints.containsKey(sensor.getTupleType()))
					endpoints.put(sensor.getTupleType(), 0);
				endpoints.put(sensor.getTupleType(), endpoints.get(sensor.getTupleType())+1);
			}
		}
		return endpoints;
	}
	
	/**
	 * Gets all actuators associated with fog-device <b>device</b>
	 * @param device
	 * @return map from actuator type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedActuators(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for(Actuator actuator : getActuators()){
			if(actuator.getGatewayDeviceId()==device.getId()){
				if(!endpoints.containsKey(actuator.getActuatorType()))
					endpoints.put(actuator.getActuatorType(), 0);
				endpoints.put(actuator.getActuatorType(), endpoints.get(actuator.getActuatorType())+1);
			}
		}
		return endpoints;
	}
	
	@SuppressWarnings("serial")
	protected List<List<Integer>> getPaths(final int fogDeviceId){
		FogDevice device = (FogDevice)CloudSim.getEntity(fogDeviceId); 
		if(device.getChildrenIds().size() == 0){		
			final List<Integer> path =  (new ArrayList<Integer>(){{add(fogDeviceId);}});
			List<List<Integer>> paths = (new ArrayList<List<Integer>>(){{add(path);}});
			return paths;
		}
		List<List<Integer>> paths = new ArrayList<List<Integer>>();
		for(int childId : device.getChildrenIds()){
			List<List<Integer>> childPaths = getPaths(childId);
			for(List<Integer> childPath : childPaths)
				childPath.add(fogDeviceId);
			paths.addAll(childPaths);
		}
		return paths;
	}
	
	protected List<List<Integer>> getLeafToRootPaths(){
		FogDevice cloud=null;
		for(FogDevice device : getFogDevices()){
			if(device.getName().equals("cloud"))
				cloud = device;
		}
		return getPaths(cloud.getId());
	}
	
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}

	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}

	public Map<Integer, List<String>> getCurrentModuleMap() {
		return currentModuleMap;
	}

	public void setCurrentModuleMap(Map<Integer, List<String>> currentModuleMap) {
		this.currentModuleMap = currentModuleMap;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<Integer, Double> getCurrentCpuLoad() {
		return currentCpuLoad;
	}

	public void setCurrentCpuLoad(Map<Integer, Double> currentCpuLoad) {
		this.currentCpuLoad= currentCpuLoad;
	}

	public Map<Integer, Map<String, Double>> getCurrentModuleLoadMap() {
		return currentModuleLoadMap;
	}

	public void setCurrentModuleLoadMap(
			Map<Integer, Map<String, Double>> currentModuleLoadMap) {
		this.currentModuleLoadMap = currentModuleLoadMap;
	}

	public Map<Integer, Map<String, Integer>> getCurrentModuleInstanceNum() {
		return currentModuleInstanceNum;
	}

	public void setCurrentModuleInstanceNum(
			Map<Integer, Map<String, Integer>> currentModuleInstanceNum) {
		this.currentModuleInstanceNum = currentModuleInstanceNum;
	}
}
