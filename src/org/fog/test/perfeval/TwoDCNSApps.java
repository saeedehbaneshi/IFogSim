package org.fog.test.perfeval;

import org.fog.utils.TimeKeeper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.random.RandomGenerator;
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
 * Simulation setup for case study 2 - Intelligent Surveillance
 * @author Saeedeh 
 *
 */
public class TwoDCNSApps {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<FogDevice> mobiles = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();
    static int numOfAreas = 1;
    public static int numOfCamerasPerArea = 4;

    private static boolean CLOUD = false;
    private static boolean USER_BASED = true;

    public static void main(String[] args) {
        Log.printLine("Starting TwoDCNSApps...");

        try {
            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId0 = "dcns_0"; // identifier of the application
            String appId1 = "dcns_1";
            
            FogBroker broker0 = new FogBroker("broker_0");
			FogBroker broker1 = new FogBroker("broker_1");

			Application application0 = createApplication0(appId0, broker0.getId());
			Application application1 = createApplication1(appId1, broker1.getId());
			application0.setUserId(broker0.getId());
			application1.setUserId(broker1.getId());
			

			createFogDevices();
			
			createEdgeDevices0(broker0.getId(), appId0);
			createEdgeDevices1(broker1.getId(), appId1);
			
			ModuleMapping moduleMapping_0 = ModuleMapping.createModuleMapping(); // initializing a module mapping
			ModuleMapping moduleMapping_1 = ModuleMapping.createModuleMapping(); // initializing a module mapping
			
            Controller controller = null;

            for (FogDevice device : fogDevices) {
                if (device.getName().startsWith("m")) {
                	moduleMapping_0.addModuleToDevice("motion_detector", device.getName());
                	moduleMapping_1.addModuleToDevice("motion_detector_1", device.getName());
                }
            }

            moduleMapping_0.addModuleToDevice("user_interface", "cloud");
            moduleMapping_1.addModuleToDevice("user_interface_1", "cloud");


            if (CLOUD) {
                moduleMapping_0.addModuleToDevice("object_detector", "cloud");
                moduleMapping_0.addModuleToDevice("object_tracker", "cloud");
                
                moduleMapping_1.addModuleToDevice("object_detector_1", "cloud");
                moduleMapping_1.addModuleToDevice("object_tracker_1", "cloud");
            }

            if (USER_BASED) {
            	moduleMapping_0.addModuleToDevice("object_detector", "d-0");
                moduleMapping_0.addModuleToDevice("object_tracker", "proxy-server");
                
                moduleMapping_1.addModuleToDevice("object_detector_1", "d-0");
                moduleMapping_1.addModuleToDevice("object_tracker_1", "proxy-server");
            }
            

            controller = new Controller("master-controller", fogDevices, sensors, actuators);
            //controller.submitApplication(application0, new ModulePlacementMapping(fogDevices, application0, moduleMapping_0));
			//controller.submitApplication(application1, 1000, new ModulePlacementMapping(fogDevices, application1, moduleMapping_1));
			
            List<String> n = application1.getModuleNames();
            for (String name : n) {
                System.out.println(name);
            }
            System.out.println("\n\n\n\n*****************************\n\n\n");
            
           /* controller.submitApplication(application0,
                    (CLOUD || USER_BASED) ? (new ModulePlacementMapping(fogDevices, application0, moduleMapping_0))
                            : (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application0, moduleMapping_0)));*/
			controller.submitApplication(application0, new ModulePlacementMapping(fogDevices, application0, moduleMapping_0));

            controller.submitApplication(application1, 80, new ModulePlacementMapping(fogDevices, application1, moduleMapping_1));
            /*controller.submitApplication(application1, 1000,
                    (CLOUD || USER_BASED) ? (new ModulePlacementMapping(fogDevices, application1, moduleMapping_1))
                            : (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application1, moduleMapping_1)));*/

            
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            Log.printLine("DCNS Two Apps finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    
    private static void createEdgeDevices0(int userId, String appId) {
		for(FogDevice camera : mobiles){
			String id = camera.getName();
			Sensor sensor = new Sensor("s-"+id, "CAMERA", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
			sensors.add(sensor);
			Actuator ptz = new Actuator("ptz-"+id, userId, appId, "PTZ_CONTROL");
			actuators.add(ptz);
			sensor.setGatewayDeviceId(camera.getId());
			sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
			ptz.setGatewayDeviceId(camera.getId());
			ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms		
		}
	}
	
	private static void createEdgeDevices1(int userId, String appId) {
		for(FogDevice camera : mobiles){
			String id = camera.getName();
			Sensor sensor = new Sensor("s-"+id, "CAMERA_1", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
			sensors.add(sensor);
			Actuator ptz = new Actuator("ptz-"+id, userId, appId, "PTZ_CONTROL_1");
			actuators.add(ptz);
			sensor.setGatewayDeviceId(camera.getId());
			sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
			ptz.setGatewayDeviceId(camera.getId());
			ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms		
		}
	}
    
    
    static boolean modified_bw=true;
	static int cloud_up_bw=100;
	static int cloud_down_bw=100000;
	static int proxy_up_bw=100000;
	static int proxy_down_bw=100000;
	//Saeedeh changed this from 10000 to 100000
	static int router_up_bw=100000;//To prevent overflow (10000 will make this link botleneck when number of cameras increases)
	static int router_down_bw=10000;
	
	private static void createFogDevices() {
		
		//*********************Default(Original of iFogSim) for bandwidths****/
		//********************Please do not touch these values****************/
		int c_band_up=100;
		int c_band_down=10000;
		int p_band_up=10000;
		int p_band_down=10000;
		//*********************************************************************/
		
		if(modified_bw==true){
			c_band_up=cloud_up_bw;
			c_band_down=cloud_down_bw;			
			p_band_up=proxy_up_bw;
			p_band_down=proxy_down_bw;
		}
		
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, c_band_up, c_band_down, 0, 0.01, 16*103, 16*83.25, "Shared",12300,11070);
		cloud.setParentId(-1);
		cloud.setDeviceType("Shared");// Saeedeh added
		
		fogDevices.add(cloud);
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, p_band_up, p_band_down, 1, 0.0, 107.339, 83.4333, "Shared", 4550, 4095);
		proxy.setParentId(cloud.getId());
		proxy.setUplinkLatency(100); // latency of connection between proxy server and cloud is 100 ms
		proxy.setDeviceType("Shared");// Saeedeh added
		fogDevices.add(proxy);
		for(int i=0;i<numOfAreas;i++){
			addArea(i+"", proxy.getId());
		}
	}

	private static FogDevice addArea(String id, int parentId){
		int r_band_up=10000;
		int r_band_down=10000;
		if(modified_bw==true){
			r_band_up=router_up_bw;
			r_band_down=router_down_bw;
		}
		
		FogDevice router = createFogDevice("d-"+id, 2800, 4000, r_band_up, r_band_down, 2, 0.0, 107.339, 83.4333, "Shared", 4550, 4095);
		fogDevices.add(router);
		router.setUplinkLatency(2); // latency of connection between router and proxy server is 2 ms
		router.setDeviceType("Shared");// Saeedeh added
		for(int i=0;i<numOfCamerasPerArea;i++){
			String mobileId = id+"-"+i;
			FogDevice camera = addCamera(mobileId, router.getId()); // adding a smart camera to the physical topology. Smart cameras have been modeled as fog devices as well.
			camera.setUplinkLatency(2); // latency of connection between camera and router is 2 ms
			fogDevices.add(camera);

		}
		router.setParentId(parentId);
		return router;
	}
	
	private static FogDevice addCamera(String id, int parentId){
		FogDevice camera = createFogDevice("m-"+id, 500, 100000, 10000, 100, 3, 0, 87.53, 82.44, "CPE", 4.6, 2.8);
		camera.setParentId(parentId);
		camera.setDeviceType("CPE");// Saeedeh added
		mobiles.add(camera);
		return camera;
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
		//Saeedeh: You can change scheduling interval here: 5 argument of FogDevice = 10
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips, deviceType, networkingMaxPower, networkingIdlePower);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}
    
    
    


	@SuppressWarnings({"serial" })
    private static Application createApplication0(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("object_detector", 10);
		application.addAppModule("motion_detector", 10);
		application.addAppModule("object_tracker", 10);
		application.addAppModule("user_interface", 10);
		
		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "CAMERA", Tuple.UP, AppEdge.SENSOR); // adding edge from CAMERA (sensor) to Motion Detector module carrying tuples of type CAMERA
		application.addAppEdge("motion_detector", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", Tuple.UP, AppEdge.MODULE); // adding edge from Motion Detector to Object Detector module carrying tuples of type MOTION_VIDEO_STREAM
		application.addAppEdge("object_detector", "user_interface", 500, 2000, "DETECTED_OBJECT", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to User Interface module carrying tuples of type DETECTED_OBJECT
		application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to Object Tracker module carrying tuples of type OBJECT_LOCATION
		//*Saeedeh*//this edge from obj tracker to ptz control is periodic and third argument is the periodicity= 100 (it means every 10msec)
		application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", Tuple.DOWN, AppEdge.ACTUATOR); // adding edge from Object Tracker to PTZ CONTROL (actuator) carrying tuples of type PTZ_PARAMS
		
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		double UI_Frac=0.05;
		//UI_Frac=0.005;
		application.addTupleMapping("motion_detector", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0)); // 1.0 tuples of type MOTION_VIDEO_STREAM are emitted by Motion Detector module per incoming tuple of type CAMERA
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0)); // 1.0 tuples of type OBJECT_LOCATION are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(UI_Frac)); // 0.05 tuples of type MOTION_VIDEO_STREAM are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM
	
		/*
		 * Defining application loops (maybe incomplete loops) to monitor the latency of. 
		 * Here, we add two loops for monitoring : Motion Detector -> Object Detector -> Object Tracker and Object Tracker -> PTZ Control
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("object_detector");add("object_tracker");}});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}});

		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
		
		application.setLoops(loops);
		return application;
	}

	@SuppressWarnings({"serial" })
    private static Application createApplication1(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("object_detector_1", 10);
		application.addAppModule("motion_detector_1", 10);
		application.addAppModule("object_tracker_1", 10);
		application.addAppModule("user_interface_1", 10);
		
		/*
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("CAMERA_1", "motion_detector_1", 1000, 20000, "CAMERA_1", Tuple.UP, AppEdge.SENSOR); // adding edge from CAMERA (sensor) to Motion Detector module carrying tuples of type CAMERA
		application.addAppEdge("motion_detector_1", "object_detector_1", 2000, 2000, "MOTION_VIDEO_STREAM_1", Tuple.UP, AppEdge.MODULE); // adding edge from Motion Detector to Object Detector module carrying tuples of type MOTION_VIDEO_STREAM
		application.addAppEdge("object_detector_1", "user_interface_1", 500, 2000, "DETECTED_OBJECT_1", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to User Interface module carrying tuples of type DETECTED_OBJECT
		application.addAppEdge("object_detector_1", "object_tracker_1", 1000, 100, "OBJECT_LOCATION_1", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to Object Tracker module carrying tuples of type OBJECT_LOCATION
		//*Saeedeh*//this edge from obj tracker to ptz control is periodic and third argument is the periodicity= 100 (it means every 10msec)
		application.addAppEdge("object_tracker_1", "PTZ_CONTROL_1", 100, 28, 100, "PTZ_PARAMS_1", Tuple.DOWN, AppEdge.ACTUATOR); // adding edge from Object Tracker to PTZ CONTROL (actuator) carrying tuples of type PTZ_PARAMS
		
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		double UI_Frac=0.05;
		//UI_Frac=0.005;
		application.addTupleMapping("motion_detector_1", "CAMERA_1", "MOTION_VIDEO_STREAM_1", new FractionalSelectivity(1.0)); // 1.0 tuples of type MOTION_VIDEO_STREAM are emitted by Motion Detector module per incoming tuple of type CAMERA
		application.addTupleMapping("object_detector_1", "MOTION_VIDEO_STREAM_1", "OBJECT_LOCATION_1", new FractionalSelectivity(1.0)); // 1.0 tuples of type OBJECT_LOCATION are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM
		application.addTupleMapping("object_detector_1", "MOTION_VIDEO_STREAM_1", "DETECTED_OBJECT_1", new FractionalSelectivity(UI_Frac)); // 0.05 tuples of type MOTION_VIDEO_STREAM are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM
	
		/*
		 * Defining application loops (maybe incomplete loops) to monitor the latency of. 
		 * Here, we add two loops for monitoring : Motion Detector -> Object Detector -> Object Tracker and Object Tracker -> PTZ Control
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("motion_detector_1");add("object_detector_1");add("object_tracker_1");}});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("object_tracker_1");add("PTZ_CONTROL_1");}});

		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
		
		application.setLoops(loops);
		return application;
	}
}
