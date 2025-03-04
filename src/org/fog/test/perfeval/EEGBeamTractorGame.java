package org.fog.test.perfeval;

	import java.util.ArrayList;
	import java.util.Calendar;
	import java.util.LinkedList;
	import java.util.List;

	import org.cloudbus.cloudsim.Host;
	import org.cloudbus.cloudsim.Log;
	import org.cloudbus.cloudsim.Pe;
	import org.cloudbus.cloudsim.Storage;
	import org.cloudbus.cloudsim.core.CloudSim;
	import org.cloudbus.cloudsim.power.PowerHost;
	import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
	import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
	import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
	import org.fog.application.AppEdge;
	import org.fog.application.AppLoop;
	import org.fog.application.Application;
	import org.fog.application.selectivity.FractionalSelectivity;
	import org.fog.entities.Actuator;
	import org.fog.entities.FogBroker;
	import org.fog.entities.FogDevice;
	import org.fog.entities.FogDeviceCharacteristics;
	import org.fog.entities.Sensor;
	import org.fog.entities.Tuple;
	import org.fog.placement.Controller;
	import org.fog.placement.ModuleMapping;
	import org.fog.placement.ModulePlacementEdgewards;
	import org.fog.placement.ModulePlacementMapping;
	import org.fog.policy.AppModuleAllocationPolicy;
	import org.fog.scheduler.StreamOperatorScheduler;
	import org.fog.utils.FogLinearPowerModel;
	import org.fog.utils.FogUtils;
	import org.fog.utils.TimeKeeper;
	import org.fog.utils.distribution.DeterministicDistribution;

	/**
	 * Simulation setup for EEG Beam Tractor Game
	 *
	 */

public class EEGBeamTractorGame {
	
	
		static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
		static List<Sensor> sensors = new ArrayList<Sensor>();
		static List<Actuator> actuators = new ArrayList<Actuator>();
		
		static boolean USER_DEFINED = false;
		static boolean CLOUD = false;
		static int numOfDepts = 2;
		static int numOfMobilesPerDept = 4;
		static double EEG_TRANSMISSION_TIME = 10;
		
		public static void main(String[] args) {

			Log.printLine("Starting EEG Beam Tractor Game...");

			try {
				Log.disable();
				int num_user = 1; // number of cloud users
				Calendar calendar = Calendar.getInstance();
				boolean trace_flag = false; // mean trace events

				CloudSim.init(num_user, calendar, trace_flag);

				String appId = "vr_game"; // identifier of the application
				
				FogBroker broker = new FogBroker("broker");
				
				Application application = createApplication(appId, broker.getId());
				application.setUserId(broker.getId());
				
				createFogDevices(broker.getId(), appId);
				
				ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
				
				//// You can define your placement here ////
				if(USER_DEFINED) {
					moduleMapping.addModuleToDevice("coordinator", "cloud");
					moduleMapping.addModuleToDevice("concentration_calculator", "proxy-server");
					moduleMapping.addModuleToDevice("brain_state_analyzer", "proxy-server");

					for(FogDevice device : fogDevices){
						if(device.getName().startsWith("m")){
							moduleMapping.addModuleToDevice("client", device.getName());  // fixing all instances of the Client module to the Smartphones
							moduleMapping.addModuleToDevice("signal_analyzer", device.getName()); // fixing all instances of the Signal Analyzer module to the Smartphones
						}
					}		
				}
				
				if(CLOUD){
					// if the mode of deployment is cloud-based
					moduleMapping.addModuleToDevice("coordinator", "cloud"); // fixing all instances of the Connector module to the Cloud
					moduleMapping.addModuleToDevice("concentration_calculator", "cloud"); // fixing all instances of the Concentration Calculator module to the Cloud
					moduleMapping.addModuleToDevice("brain_state_analyzer", "cloud");
					for(FogDevice device : fogDevices){
						if(device.getName().startsWith("m")){
							moduleMapping.addModuleToDevice("client", device.getName());  // fixing all instances of the Client module to the Smartphones
							moduleMapping.addModuleToDevice("signal_analyzer", device.getName());					
						}
					}
				}else{
					// if the mode of deployment is Edge-ward
					moduleMapping.addModuleToDevice("coordinator", "cloud"); // fixing all instances of the Connector module to the Cloud
					for(FogDevice device : fogDevices){
						if(device.getName().startsWith("m")){
							moduleMapping.addModuleToDevice("client", device.getName());  // fixing all instances of the Client module to the Smartphones
							moduleMapping.addModuleToDevice("signal_analyzer", device.getName());					
						}
					}
					// rest of the modules will be placed by the Edge-ward placement policy
					
				}
				
				
				Controller controller = new Controller("master-controller", fogDevices, sensors, 
						actuators);
				
				controller.submitApplication(application, 0, 
						(CLOUD || USER_DEFINED)?(new ModulePlacementMapping(fogDevices, application, moduleMapping))
								:(new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

				TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

				CloudSim.startSimulation();

				CloudSim.stopSimulation();

				Log.printLine("VRGame finished!");
			} catch (Exception e) {
				e.printStackTrace();
				Log.printLine("Unwanted errors happen");
			}
		}

		/**
		 * Creates the fog devices in the physical topology of the simulation.
		 * @param userId
		 * @param appId
		 */
		private static void createFogDevices(int userId, String appId) {
			FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 100000, 0, 0.01, 16*103, 16*83.25, "Shared", 12300, 11070); // creates the fog device Cloud at the apex of the hierarchy with level=0
			cloud.setParentId(-1);
			cloud.setDeviceType("Shared");
			FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 100000, 100000, 1, 0.0, 107.339, 83.4333, "Shared", 4550, 4095); // creates the fog device Proxy Server (level=1)
			proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
			proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms
			proxy.setDeviceType("Shared");

			
			fogDevices.add(cloud);
			fogDevices.add(proxy);
			
			for(int i=0;i<numOfDepts;i++){
				addGw(i+"", userId, appId, proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
			}
			
		}

		private static FogDevice addGw(String id, int userId, String appId, int parentId){
			FogDevice router = createFogDevice("router-"+id, 2800, 4000, 100000, 100000, 1, 0.0, 107.339, 83.4333, "Shared", 4550, 4095);
			fogDevices.add(router);
			router.setParentId(parentId);
			router.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms
			router.setDeviceType("Shared");
			for(int i=0;i<numOfMobilesPerDept;i++){
				String mobileId = id+"-"+i;
				FogDevice mobile = addMobile(mobileId, userId, appId, router.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
				mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 2 ms
				fogDevices.add(mobile);
			}
			return router;
		}
		
		private static FogDevice addMobile(String id, int userId, String appId, int parentId){
			FogDevice mobile = createFogDevice("m-"+id, 500, 1000, 10000, 270, 3, 0, 87.53, 82.44, "CPE", 4.6, 2.8);
			mobile.setParentId(parentId);
			mobile.setDeviceType("CPE");
			Sensor eegSensor = new Sensor("s-"+id, "EEG", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
			sensors.add(eegSensor);
			Actuator display = new Actuator("a-"+id, userId, appId, "DISPLAY");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
			return mobile;
		}
		
		/**
		 * Creates a vanilla fog device
		 * @param nodeName name of the device to be used in simulation
		 * @param mips MIPS
		 * @param ram RAM
		 * @param upBw uplink bandwidth
		 * @param downBw downlink bandwidth
		 * @param level hierarchy level of the device
		 * @param ratePerMips cost rate per MIPS used
		 * @param busyPower
		 * @param idlePower
		 * @return
		 */
		private static FogDevice createFogDevice(String nodeName, long mips,
				int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower, String deviceType, double networkingMaxPower, double networkingIdlePower) {
			
			List<Pe> peList = new ArrayList<Pe>();

			// 3. Create PEs and add these into a list.
			peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

			int hostId = FogUtils.generateEntityId();
			long storage = 1000000; // host storage
			int bw = 10000;

			PowerHost host = new PowerHost(
					hostId,
					new RamProvisionerSimple(ram),
					new BwProvisionerOverbooking(bw),
					storage,
					peList,
					new StreamOperatorScheduler(peList),
					new FogLinearPowerModel(busyPower, idlePower)
				);

			List<Host> hostList = new ArrayList<Host>();
			hostList.add(host);

			String arch = "x86"; // system architecture
			String os = "Linux"; // operating system
			String vmm = "Xen";
			double time_zone = 10.0; // time zone this resource located
			double cost = 3.0; // the cost of using processing in this resource
			double costPerMem = 0.05; // the cost of using memory in this resource
			double costPerStorage = 0.001; // the cost of using storage in this
											// resource
			double costPerBw = 0.0; // the cost of using bw in this resource
			LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
														// devices by now

			FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
					arch, os, vmm, host, time_zone, cost, costPerMem,
					costPerStorage, costPerBw);

			FogDevice fogdevice = null;
			try {
				fogdevice = new FogDevice(nodeName, characteristics, 
						new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips, deviceType, networkingMaxPower, networkingIdlePower);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			fogdevice.setLevel(level);
			return fogdevice;
		}

		/**
		 * Function to create the EEG Tractor Beam game application in the DDF model. 
		 * @param appId unique identifier of the application
		 * @param userId identifier of the user of the application
		 * @return
		 */
		@SuppressWarnings({"serial" })
		private static Application createApplication(String appId, int userId){
			
			Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
			
			/*
			 * Adding modules (vertices) to the application model (directed graph)
			 */
			application.addAppModule("signal_analyzer", 10); // adding module Signal analyzer to the application model
			application.addAppModule("client", 10); // adding module Client to the application model
			application.addAppModule("brain_state_analyzer", 10); // adding module Brain state analyzer to the application model
			application.addAppModule("concentration_calculator", 10); // adding module Concentration Calculator to the application model
			application.addAppModule("coordinator", 10); // adding module Coordinator to the application model
			
			/*
			 * Connecting the application modules (vertices) in the application model (directed graph) with edges
			 */

			application.addAppEdge("EEG", "signal_analyzer", 1000, 1000, "EEG", Tuple.UP, AppEdge.SENSOR);
			application.addAppEdge("signal_analyzer", "client", 3000, 500, "FILTERED_SIGNAL", Tuple.UP, AppEdge.MODULE); // adding edge from Signal Analyzer to Client module carrying tuples of type _SENSOR
			application.addAppEdge("client", "brain_state_analyzer", 1000, 500, "PRE_PROCCESED_SIGNAL", Tuple.UP, AppEdge.MODULE); // adding edge from Client to Brain State Analyzer module carrying tuples of type _BRAIN_STATE
			application.addAppEdge("brain_state_analyzer", "concentration_calculator", 2000, 500, "BRAIN_STATE", Tuple.UP, AppEdge.MODULE); // adding edge from Brain State Analyzer to Concentration Calculator module carrying tuples of type BRAIN_STATE
			application.addAppEdge("concentration_calculator", "coordinator", 100, 1000, 1000, "PLAYER_GAME_STATE", Tuple.UP, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to Coordinator module carrying tuples of type PLAYER_GAME_STATE
			application.addAppEdge("concentration_calculator", "client", 20, 500, "CONCENTRATION_LEVEL", Tuple.DOWN, AppEdge.MODULE);  // adding edge from Concentration Calculator to Client module carrying tuples of type CONCENTRATION
			application.addAppEdge("coordinator", "client", 100, 100, 1000, "GLOBAL_GAME_STATE", Tuple.DOWN, AppEdge.MODULE); // adding periodic edge (period=100ms) from Coordinator to Client module carrying tuples of type GLOBAL_GAME_STATE
			application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type SELF_STATE_UPDATE
			application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type GLOBAL_STATE_UPDATE

			
			/*
			 * Defining the input-output relationships (represented by selectivity) of the application modules. 
			 */
			application.addTupleMapping("signal_analyzer", "EEG", "FILTERED_SIGNAL", new FractionalSelectivity(0.9)); // 0.9 tuples of type _SENSOR are emitted by Signal Analyzer module per incoming tuple of type EEG 
			application.addTupleMapping("client", "FILTERED_SIGNAL", "PRE_PROCCESED_SIGNAL", new FractionalSelectivity(1.0)); // 1.0 tuples of type _BRAIN_STATE are emitted by Client module per incoming tuple of type _SENSOR 
			application.addTupleMapping("brain_state_analyzer", "PRE_PROCCESED_SIGNAL", "BRAIN_STATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type BRAIN_STATE are emitted by Brain State Analyzer module per incoming tuple of type _BRAIN_STATE 
			application.addTupleMapping("client", "CONCENTRATION_LEVEL", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type SELF_STATE_UPDATE are emitted by Client module per incoming tuple of type CONCENTRATION 
			application.addTupleMapping("concentration_calculator", "BRAIN_STATE", "CONCENTRATION_LEVEL", new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration Calculator module per incoming tuple of type _SENSOR 
			application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module per incoming tuple of type GLOBAL_GAME_STATE
		
			/*
			 * Defining application loops to monitor the latency of. 
			 * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
			 */
			final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG");add("signal_analyzer");add("client");add("brain_state_analyzer");add("concentration_calculator");add("client");}});
			final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("client");add("DISPLAY");}});
			
			List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
			application.setLoops(loops);
			
			return application;
		}

}
