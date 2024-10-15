package org.fog.entities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.mobilitydata.Clustering;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.*;
import org.json.simple.JSONObject;

import org.cloudbus.cloudsim.core.predicates.PredicateBetweenTimes;

import java.util.*;

public class FogDevice extends PowerDatacenter {
    protected Queue<Tuple> northTupleQueue;
    protected Queue<Pair<Tuple, Integer>> southTupleQueue;

    protected List<String> activeApplications;

    protected Map<String, Application> applicationMap;
    protected Map<String, List<String>> appToModulesMap;
    protected Map<Integer, Double> childToLatencyMap;

    
    //Saeedeh added this for Flow-based energy model for Networking:
    protected Map<String, Map<String, Double>> networkingEnergyConsumptionMap = new HashMap<String, Map<String, Double>>();
    protected Map<String, Map<String, Double>> networkingTimeMap = new HashMap<String, Map<String, Double>>();
    protected double previous_update_networking_time_CPE;
    protected List<Tuple> activeTuplesList= new ArrayList<Tuple>();
    protected double previous_networking_update_state_time=0.0;
    protected String deviceType;
    protected double uplinkTotalActiveEnergy=0.0;
    protected double downlinkTotalActiveEnergy=0.0;
    protected double uplinkTotalIdleEnergy=0.0;
    protected double downlinkTotalIdleEnergy=0.0;
    protected double networkingMaxPower;
    protected double networkingIdlePower;
    protected Map<String, Map<String, Double>> networkingTuplesEnergyMap = new HashMap<String, Map<String, Double>>();
    protected Map<String, Map<String, Double>> networkingTuplesTimeMap = new HashMap<String, Map<String, Double>>();
	Map<String, Map<String, Double>> networkingTuplesIdleEnergyMap = new HashMap<String, Map<String, Double>>();//This only used by CPE devices
	
	Map <String, Map<String, Double>> VmsEnergyMap= new HashMap<String, Map<String, Double>>();
	Map <String, Map<String, Double>> VmsTimeMap= new HashMap<String, Map<String, Double>>();
	
	// Saeedeh added these parameters to calculate CO2 emission of VMs:
	private static final double GLOBAL_CARBON_INTENSITY = 0.475; // kgCO2 per kWh
    private static final double MILLIJOULES_TO_KWH = 3.6e9;
	Map <String, Map<String, Double>> VmsCO2Map= new HashMap<String, Map<String, Double>>();
	// Map to store CO2 emissions for each tuple within each application
	private Map<String, Map<String, Double>> networkingTuplesCO2Map = new HashMap<>();

    
    protected Map<Integer, Integer> cloudTrafficMap;

    protected double lockTime;

    /**
     * ID of the parent Fog Device
     */
    protected int parentId;

    /**
     * ID of the Controller
     */
    protected int controllerId;
    /**
     * IDs of the children Fog devices
     */
    protected List<Integer> childrenIds;

    protected Map<Integer, List<String>> childToOperatorsMap;

    /**
     * Flag denoting whether the link southwards from this FogDevice is busy
     */
    protected boolean isSouthLinkBusy;

    /**
     * Flag denoting whether the link northwards from this FogDevice is busy
     */
    
    //Saeedeh added for communication energy estimation //////
    protected double northLinkIdleTime;
    protected double northLinkBusyTime;
    
    protected double southLinkIdleTime;
    protected double southLinkBusyTime;
    protected double communicationEnergyConsumption;
    protected double appCommunicationEnergyConsumption;
    protected double lastNorthwardCommunicationUpdateTime;
    protected double lastSouthwardCommunicationUpdateTime;

    protected boolean previousSouthLinkState;
    protected boolean previousNorthLinkState;
    
    
    protected boolean isNorthLinkBusy;
    
    //Saeedeh these added for tracking busy time of NIC of each device for ECOFEN based model
    
    protected boolean isNetworkInterfaceCardBusy;
    protected boolean previousNetworkCardState;
    protected double lastNetworkInterfaceCardUpdateTime;
    protected double totalNetworkInterfaceCardIdleTime;
    protected double totalNetworkInterfaceCardBusyTime;

    //Saeedeh added this:
    protected double aggregateBandwidth;


    protected double uplinkBandwidth;
    protected double downlinkBandwidth;
    protected double uplinkLatency;
    protected List<Pair<Integer, Double>> associatedActuatorIds;

    protected double energyConsumption;
    protected double applicationEnergyConsumption;

    protected double lastUtilizationUpdateTime;
    protected double lastUtilization;
    //Saeedeh added:
    //protected double lastVmMipsRatio;
    protected Map<Vm, Double> lastVmsMipsMap= new HashMap<Vm, Double>();
    //
    protected boolean ComputationEnergyEstimationTracking= false;
    
    private int level;

    protected double ratePerMips;

    protected double totalCost;

    protected Map<String, Map<String, Integer>> moduleInstanceCount;

    protected List<Integer> clusterMembers = new ArrayList<Integer>();
    protected boolean isInCluster = false;
    protected boolean selfCluster = false; // IF there is only one fog device in one cluster without any sibling
    protected Map<Integer, Double> clusterMembersToLatencyMap; // latency to other cluster members

    protected Queue<Pair<Tuple, Integer>> clusterTupleQueue;// tuple and destination cluster device ID
    protected boolean isClusterLinkBusy; //Flag denoting whether the link connecting to cluster from this FogDevice is busy
    protected double clusterLinkBandwidth;


    public FogDevice(
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips, String deviceType, double networkingMaxPower, double networkingIdlePower) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        
        //Saeedeh added this:
        setAggregateBandwidth(uplinkBandwidth+downlinkBandwidth);
        setDeviceType(deviceType);
        setNetworkingIdlePower(networkingIdlePower);
        setNetworkingMaxPower(networkingMaxPower);
        //
        
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);


        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.applicationEnergyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());

        clusterTupleQueue = new LinkedList<>();
        setClusterLinkBusy(false);
        
        previous_update_networking_time_CPE=0.0;

    }

    public FogDevice(
            String name, long mips, int ram,
            double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
        super(name, null, null, new LinkedList<Storage>(), 0);

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
                powerModel
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));

        String arch = Config.FOG_DEVICE_ARCH;
        String os = Config.FOG_DEVICE_OS;
        String vmm = Config.FOG_DEVICE_VMM;
        double time_zone = Config.FOG_DEVICE_TIMEZONE;
        double cost = Config.FOG_DEVICE_COST;
        double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
        double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
        double costPerBw = Config.FOG_DEVICE_COST_PER_BW;

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        setCharacteristics(characteristics);

        setLastProcessTime(0.0);
        setVmList(new ArrayList<Vm>());
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        
        //Saeedeh added next line:
        setDeviceType(deviceType);
        setNetworkingIdlePower(networkingIdlePower);
        setNetworkingMaxPower(networkingMaxPower);
        //
        
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host1 : getCharacteristics().getHostList()) {
            host1.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }


        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);


        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.applicationEnergyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setChildToLatencyMap(new HashMap<Integer, Double>());
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());

        clusterTupleQueue = new LinkedList<>();
        setClusterLinkBusy(false);
    }

    /**
     * Overrides this method when making a new and different type of resource. <br>
     * <b>NOTE:</b> You do not need to override {@link #body()} method, if you use this method.
     *
     * @pre $none
     * @post $none
     */
    protected void registerOtherEntity() {

    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
    	/*StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();  
        System.out.println("///////////////////////////Displaying Stack trace using StackTraceElement in Java//////////////////////////////");  
        for(StackTraceElement st : stackTrace){
        	// print the stack trace   
            System.out.println(st);  
        }*/
    	boolean debug=false;
        if(debug) {
	        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
	        System.out.println("Enter username");
	        String userName = myObj.nextLine();  // Read user in1put
	    }
        if(ev.getDestination()==5) {
        	//sshh//  System.out.println(ev.getTag());
        	//sshh//  System.out.println(CloudSim.clock());
        	//debug=true;
        	if(debug) {
    	        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
    	        System.out.println("event for d0");
    	        String userName = myObj.nextLine();  // Read user input
    	    }
        	
        }
        
        
        switch (ev.getTag()) {
            case FogEvents.TUPLE_ARRIVAL:
            	/*if (getName().equals("d-0")) {
                 	 System.out.println(" Current time : "+CloudSim.clock());
                     processTupleArrival(ev);
                     break;

                 }else {*/
                    processTupleArrival(ev);
                	break;
 
            case FogEvents.LAUNCH_MODULE:
                processModuleArrival(ev);
                break;
            case FogEvents.RELEASE_OPERATOR:
                processOperatorRelease(ev);
                break;
            case FogEvents.SENSOR_JOINED:
                processSensorJoining(ev);
                break;
            case FogEvents.SEND_PERIODIC_TUPLE:
                sendPeriodicTuple(ev);
                break;
            case FogEvents.APP_SUBMIT:
                processAppSubmit(ev);
                break;
            case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
                updateNorthTupleQueue();
                break;
            case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
                updateSouthTupleQueue();
                break;
            case FogEvents.ACTIVE_APP_UPDATE:
                updateActiveApplications(ev);
                break;
            case FogEvents.ACTUATOR_JOINED:
                processActuatorJoined(ev);
                break;
            case FogEvents.LAUNCH_MODULE_INSTANCE:
                updateModuleInstanceCount(ev);
                break;
            case FogEvents.MODULE_SEND:
                moduleSend(ev);
                break;
            case FogEvents.MODULE_RECEIVE:
                moduleReceive(ev);
                break;
            case FogEvents.RELEASE_MODULE:
                processModuleTermination(ev);
                break;
            case FogEvents.RESOURCE_MGMT:
            	//updateCloudetProcessingWithoutSchedulingFutureEventsForce();
                manageResources(ev);
                
                break;
            case FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE:
                updateClusterTupleQueue();
                break;
            case FogEvents.START_DYNAMIC_CLUSTERING:
                //This message is received by the devices to start their clustering
                processClustering(this.getParentId(), this.getId(), ev);
                break;
/////////////////////////////// Saeedeh added this to resolve infinite problem in estimated finish time but it didnt work properly so call updateCloudletProcessing() func in datacenter with ev as arg
            /*case FogEvents.INFINITY_RESOURCE_MGMT:
            	double mintime=updateCloudetProcessingWithoutSchedulingFutureEventsForce();
                infinityManageResources(ev);
                //updateCloudetProcessingWithoutSchedulingFutureEventsForce();
                break;*/
            default:
                break;
        }
    }

    protected void moduleSend(SimEvent ev) {
        // TODO Auto-generated method stub
        JSONObject object = (JSONObject) ev.getData();
        AppModule appModule = (AppModule) object.get("module");
        System.out.println(getName() + " is sending " + appModule.getName());
        NetworkUsageMonitor.sendingModule((double) object.get("delay"), appModule.getSize());
        MigrationDelayMonitor.setMigrationDelay((double) object.get("delay"));


        sendNow(getId(), FogEvents.RELEASE_MODULE, appModule);


    }

    protected void moduleReceive(SimEvent ev) {
        // TODO Auto-generated method stub
        JSONObject object = (JSONObject) ev.getData();
        AppModule appModule = (AppModule) object.get("module");
        Application app = (Application) object.get("application");
        System.out.println(getName() + " is receiving " + appModule.getName());
        NetworkUsageMonitor.sendingModule((double) object.get("delay"), appModule.getSize());
        MigrationDelayMonitor.setMigrationDelay((double) object.get("delay"));

        sendNow(getId(), FogEvents.APP_SUBMIT, app);
        sendNow(getId(), FogEvents.LAUNCH_MODULE, appModule);
    }

    /**
     * Perform miscellaneous resource management tasks
     *
     * @param ev
     */
    protected void manageResources(SimEvent ev) {
        updateEnergyConsumption();
        send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
    }
    
///////////////////Saeedeh added for case infinity estimated finish time
    protected void infinityManageResources(SimEvent ev) {
        updateEnergyConsumption();
        //send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
    }
///////////////////Saeedeh added for case infinity estimated finish time
    
    
    
    /**
     * Updating the number of modules of an application module on this device
     *
     * @param ev instance of SimEvent containing the module and no of instances
     */
    protected void updateModuleInstanceCount(SimEvent ev) {
        ModuleLaunchConfig config = (ModuleLaunchConfig) ev.getData();
        String appId = config.getModule().getAppId();
        if (!moduleInstanceCount.containsKey(appId))
            moduleInstanceCount.put(appId, new HashMap<String, Integer>());
        moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
        System.out.println(getName() + " Creating " + config.getInstanceCount() + " instances of module " + config.getModule().getName());
    }

    private AppModule getModuleByName(String moduleName) {
        AppModule module = null;
        for (Vm vm : getHost().getVmList()) {
            if (((AppModule) vm).getName().equals(moduleName)) {
                module = (AppModule) vm;
                break;
            }
        }
        return module;
    }

    /**
     * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, only one tuple is sent DOWN while instanceCount number of tuples are sent UP.
     *
     * @param ev SimEvent instance containing the edge to send tuple on
     */
    protected void sendPeriodicTuple(SimEvent ev) {
        AppEdge edge = (AppEdge) ev.getData();
        String srcModule = edge.getSource();
        AppModule module = getModuleByName(srcModule);

        if (module == null)
            return;

        int instanceCount = module.getNumInstances();
        /*
         * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
         */
        for (int i = 0; i < ((edge.getDirection() == Tuple.UP) ? instanceCount : 1); i++) {
            //System.out.println(CloudSim.clock()+" : Sending periodic tuple "+edge.getTupleType());
            Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId(), module.getId());
            updateTimingsOnSending(tuple);
            sendToSelf(tuple);
        }
        send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
    }

    protected void processActuatorJoined(SimEvent ev) {
        int actuatorId = ev.getSource();
        double delay = (double) ev.getData();
        getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
    }


    protected void updateActiveApplications(SimEvent ev) {
        Application app = (Application) ev.getData();
       //saeedeh// System.out.println(" This active app, app(data) :  "+app.getModuleNames()+" UPDATED");
        getActiveApplications().add(app.getAppId());
    }


    public String getOperatorName(int vmId) {setDeviceType(deviceType);
        for (Vm vm : this.getHost().getVmList()) {
            if (vm.getId() == vmId)
                return ((AppModule) vm).getName();
        }
        return null;
    }

    /**
     * Update cloudet processing without scheduling future events.
     *
     * @return the double
     */
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        for (PowerHost host : this.<PowerHost>getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }

        if (timeDiff > 0) {
            Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);

            for (PowerHost host : this.<PowerHost>getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                //saeedeh//System.out.println("SAEEDEH Time frame host energy = "+timeFrameHostEnergy);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;
               //saeedeh// System.out.println("SAEEDEH Time frame datacenter energy = "+timeFrameDatacenterEnergy);

                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }

            Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
        }

        setPower(getPower() + timeFrameDatacenterEnergy);
        //saeedehSystem.out.println("GET POWER = "+getPower());
        checkCloudletCompletion();

        /** Remove completed VMs **/
        /**
         * Change made by HARSHIT GUPTA
         */
		/*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/

        Log.printLine();

        setLastProcessTime(currentTime);
        //saeedeh//System.out.println("\nLast process time is(output of updateCloudetProcessingWithoutSchedulingFutureEventsForce function) : "+getLastProcessTime()+"\n");
        //saeedeh//System.out.println("\nmin time is(output of updateCloudetProcessingWithoutSchedulingFutureEventsForce function): "+minTime+"\n");

        return minTime;
    }
    
    
    
    ////// Saeedeh added this func with arg
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce(SimEvent ev) {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        for (PowerHost host : this.<PowerHost>getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }

        if (timeDiff > 0) {
            Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);

            for (PowerHost host : this.<PowerHost>getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                //saeedeh//System.out.println("SAEEDEH Time frame host energy = "+timeFrameHostEnergy);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;
               //saeedeh// System.out.println("SAEEDEH Time frame datacenter energy = "+timeFrameDatacenterEnergy);

                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }

            Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
        }

        setPower(getPower() + timeFrameDatacenterEnergy);
        //saeedehSystem.out.println("GET POWER = "+getPower());
        checkCloudletCompletion(ev);

        /** Remove completed VMs **/
        /**
         * Change made by HARSHIT GUPTA
         */
		/*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/

        Log.printLine();

        setLastProcessTime(currentTime);
        
        return minTime;
    }
    
    protected void checkCloudletCompletion() {
        boolean cloudletCompleted = false;
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            //saeedeh//System.out.println("HOST ID : "+host.getId());
            for (Vm vm : host.getVmList()) {
                //saeedeh//System.out.println("VM ID : "+vm.getId());
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {

                        cloudletCompleted = true;
                        Tuple tuple = (Tuple) cl;
                        
                     // Saeedeh added these lines to handle early finished cloudlets and their outdated finish event
                        double currentTime = CloudSim.clock();
                        double estimatedFinishTime = tuple.getEstimatedFinishTime();
                        // Compare actual and estimated finish times
                        if (currentTime < estimatedFinishTime) {
                            // Cancel outdated events
                            cancelOutdatedEvents(currentTime, estimatedFinishTime);
                        }
                                                
                       //saeedeh// System.out.println("TUPLE TYPE : "+tuple.getTupleType());
                        TimeKeeper.getInstance().tupleEndedExecution(tuple);
                        Application application = getApplicationMap().get(tuple.getAppId());
                        Logger.error(getName(), "Completed execution of tuple " + tuple.getCloudletId() + "on " + tuple.getDestModuleName());
                       //saeede// System.out.println(getName()+ "Completed execution of tuple " + tuple.getTupleType() + "on " + tuple.getDestModuleName());
                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
                        for (Tuple resTuple : resultantTuples) {
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule) vm).getName(), vm.getId());
                           //saeedeh// System.out.println("Get module copy map of tuple : "+resTuple.getModuleCopyMap());
                            updateTimingsOnSending(resTuple);
                            sendToSelf(resTuple);
                        }
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
        }
        if (cloudletCompleted)
            updateAllocatedMips(null);
    }

///// Saeedeh added this function with input argument to solve the infinity estimated finish time problem:
    protected void checkCloudletCompletion(SimEvent ev) {
        boolean cloudletCompleted = false;
        ////// Saeedeh added ev as input arg and cast it to cl to call updateAllocatedMips with destination of this event
        Tuple arrivingTuple = (Tuple) ev.getData();
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            //saeedeh//System.out.println("HOST ID : "+host.getId());
            for (Vm vm : host.getVmList()) {
                //saeedeh//System.out.println("VM ID : "+vm.getId());
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {

                        cloudletCompleted = true;
                        Tuple tuple = (Tuple) cl;
                        
                        // aeedeh added these lines to handle early finished cloudlets and their outdated finish event
                        double currentTime = CloudSim.clock();
                        double estimatedFinishTime = tuple.getEstimatedFinishTime();
                        // Compare actual and estimated finish times
                        if (currentTime < estimatedFinishTime) {
                            // Cancel outdated events
                            cancelOutdatedEvents(currentTime, estimatedFinishTime);
                        }
                        
                       //saeedeh// System.out.println("TUPLE TYPE : "+tuple.getTupleType());
                        TimeKeeper.getInstance().tupleEndedExecution(tuple);
                        Application application = getApplicationMap().get(tuple.getAppId());
                        Logger.error(getName(), "Completed execution of tuple " + tuple.getCloudletId() + "on " + tuple.getDestModuleName());
                       //saeede// System.out.println(getName()+ "Completed execution of tuple " + tuple.getTupleType() + "on " + tuple.getDestModuleName());
                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
                        for (Tuple resTuple : resultantTuples) {
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule) vm).getName(), vm.getId());
                           //saeedeh// System.out.println("Get module copy map of tuple : "+resTuple.getModuleCopyMap());
                            updateTimingsOnSending(resTuple);
                            sendToSelf(resTuple);
                        }
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
        }
        if (cloudletCompleted)
            //updateAllocatedMips(null);
        	updateAllocatedMips(arrivingTuple.getDestModuleName());
    }
    

    

    protected void updateTimingsOnSending(Tuple resTuple) {
        // TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE.
        // WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
        String srcModule = resTuple.getSrcModuleName();
        String destModule = resTuple.getDestModuleName();
        for (AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()) {
            if (loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)) {
                int tupleId = TimeKeeper.getInstance().getUniqueId();
                resTuple.setActualTupleId(tupleId);
                if (!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
                    TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
                TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
                TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());

                //Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);

            }
        }
    }

    protected int getChildIdWithRouteTo(int targetDeviceId) {
        for (Integer childId : getChildrenIds()) {
            if (targetDeviceId == childId)
                return childId;
            if (((FogDevice) CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
                return childId;
        }
        return -1;
    }

    protected int getChildIdForTuple(Tuple tuple) {
        if (tuple.getDirection() == Tuple.ACTUATOR) {
            int gatewayId = ((Actuator) CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
            return getChildIdWithRouteTo(gatewayId);
        }
        return -1;
    }
//////Saeedeh made the type of updateAllocatedMips function public instead of protected to use in cloudletschedulertimesharedclass
    protected void updateAllocatedMips(String incomingOperator) {
    	//saeedeh//System.out.println(" look at here Saeedeh! Updating alocated Mips");
    	//saeedeh//System.out.println(" look at here Saeedeh! total mips :"+getHost().getTotalMips());
        getHost().getVmScheduler().deallocatePesForAllVms();
        for (final Vm vm : getHost().getVmList()) {
            if (vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule) vm).getName().equals(incomingOperator)) {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add((double) getHost().getTotalMips());
                    }
                });
            } else {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add(0.0);
                    }
                });
            }
        }
        
    	//saeedeh//System.out.println(" look at here Saeedeh! New total mips :"+getHost().getTotalMips());

        updateEnergyConsumption();

    }

    private void updateEnergyConsumption() {
        double totalMipsAllocated = 0;
        for (final Vm vm : getHost().getVmList()) {
            AppModule operator = (AppModule) vm;
            operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
                    .getAllocatedMipsForVm(operator));
            totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
        }

        double timeNow = CloudSim.clock();
        
        double currentEnergyConsumption = getEnergyConsumption();
        double nn=getHost().getPowerModel().getPower(lastUtilization);
        double tspan=(timeNow - lastUtilizationUpdateTime);
        double newEnergyPart=0.0;
        double newEnergyConsumption =0.0;
        
        //if(lastUtilizationUpdateTime!=0 || totalMipsAllocated%1000!=0 && lastUtilizationUpdateTime%100!=0) {
        	newEnergyPart=(timeNow - lastUtilizationUpdateTime) * nn;
        	newEnergyConsumption = currentEnergyConsumption + (timeNow - lastUtilizationUpdateTime) * nn;
        //}
        if(ComputationEnergyEstimationTracking)	
        	setEnergyConsumption(newEnergyConsumption);
        
      //Saeedeh added this code to model the single application energy consumption*****************
        
        double currentAppEnergyConsumption = getApplicationEnergyConsumption();
        double newAppEnergyConsumption;
        double intervalAppEnergyConsumption=0.0;
        
        //if(lastUtilization!=0 && lastUtilizationUpdateTime!=0 | totalMipsAllocated%1000!=0 && lastUtilizationUpdateTime%100!=0) {
        if(lastUtilization!=0) { 
        	intervalAppEnergyConsumption=(timeNow - lastUtilizationUpdateTime) * getHost().getPowerModel().getPower(lastUtilization);
        	 newAppEnergyConsumption = currentAppEnergyConsumption + intervalAppEnergyConsumption;
        }
        else {
        	 newAppEnergyConsumption = currentAppEnergyConsumption;
        }

        if(ComputationEnergyEstimationTracking)	
        	setApplicationEnergyConsumption(newAppEnergyConsumption);
        else
        	intervalAppEnergyConsumption=0;
        
       //Saeedeh added last lines for modeling application energy****************************
        
        
        
        //^^^^^^^^ Saeedeh added this part to model multiple application and have energy consumption per VM
        double intervalVms=0; 
	        for (final Vm vm : getVmList()) {
	        	if (!getLastVmsMipsMap().containsKey(vm))
	            	getLastVmsMipsMap().put(vm, 0.0);
	
	        	AppModule operator = (AppModule) vm;
	        	if (!getVmsEnergyMap().containsKey(operator.getAppId())) {
	        		getVmsEnergyMap().put(operator.getAppId(), new HashMap<>());
	            	Map<String, Double> innerMap = new HashMap<>();
	        		innerMap.put(operator.getName(), 0.0);
	        		}
	        	
	        	Map<String, Double> innerMap = getVmsEnergyMap().get(operator.getAppId());
	        	
	        	if (!innerMap.containsKey(operator.getName()))
	        		innerMap.put(operator.getName(), 0.0);
	        		
	        		
	        	double currentVmEnergy = innerMap.get(operator.getName());
	        	
	           
	        	
	        	double VM_share=getLastVmsMipsMap().get(vm)/getHost().getTotalMips();
	        	
	        	 boolean checkEnergy=false;
		            if (operator.getName()=="concentration_calculator") {
		            	checkEnergy=true;
		            }
	        	
	        	//if(lastUtilizationUpdateTime!=0 | totalMipsAllocated%1000!=0 && lastUtilizationUpdateTime%100!=0) {
	        	double newVmEnergy=0;
        		if(ComputationEnergyEstimationTracking)
        			newVmEnergy = (timeNow - lastUtilizationUpdateTime)*getHost().getPowerModel().getPower(lastUtilization)*VM_share;
        		intervalVms+=newVmEnergy;
                innerMap.put(operator.getName(), currentVmEnergy+newVmEnergy);
                getVmsEnergyMap().put(operator.getAppId(), innerMap);
	        	//}
                
                
               
                
             // Saeedeh add these lines for CO2 emissions calculation and update the CO2 map
                if (!getVmsCO2Map().containsKey(operator.getAppId())) {
                    getVmsCO2Map().put(operator.getAppId(), new HashMap<>());
                }
                
                Map<String, Double> co2InnerMap = getVmsCO2Map().get(operator.getAppId());
                if (!co2InnerMap.containsKey(operator.getName())) {
                    co2InnerMap.put(operator.getName(), 0.0);
                }
                
                double currentVmCO2 = co2InnerMap.get(operator.getName());
                double newVmCO2 = calculateCO2Emissions(newVmEnergy);
                co2InnerMap.put(operator.getName(), currentVmCO2 + newVmCO2);
                getVmsCO2Map().put(operator.getAppId(), co2InnerMap);
                
                
                
	        	getLastVmsMipsMap().put(vm, getHost().getTotalAllocatedMipsForVm(vm));
	        }
	        
	       // if(intervalVms!= intervalAppEnergyConsumption)
	        	//System.out.println("App interval energy is not equal to Vms interval energy");
        
        //^^^^^^^^ Saeedeh added last lines to model multiple application and have energy consumption per VM
        
        
        
        double currentCost = getTotalCost();

        double newcost = currentCost + (timeNow - lastUtilizationUpdateTime) * getRatePerMips() * lastUtilization * getHost().getTotalMips();

        
        setTotalCost(newcost);

        lastUtilization = Math.min(1, totalMipsAllocated / getHost().getTotalMips());

        lastUtilizationUpdateTime = timeNow;
        
        //lastMipsRatio=;
        
    }
    
    
    // Saeedeh added this method to calculate CO2 emissions from energy in millijoules
    public double calculateCO2Emissions(double energyInMilliJoules) {
        // Convert millijoules to kilowatt-hours
        double energyInKWh = energyInMilliJoules / MILLIJOULES_TO_KWH;
        // Calculate CO2 emissions
        return GLOBAL_CARBON_INTENSITY * energyInKWh;
    }
    
    

    protected void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        applicationMap.put(app.getAppId(), app);
    }

    public void addChild(int childId) {
        if (CloudSim.getEntityName(childId).toLowerCase().contains("sensor"))
            return;
        if (!getChildrenIds().contains(childId) && childId != getId())
            getChildrenIds().add(childId);
        if (!getChildToOperatorsMap().containsKey(childId))
            getChildToOperatorsMap().put(childId, new ArrayList<String>());
    }


    protected void updateCloudTraffic() {
        int time = (int) CloudSim.clock() / 1000;
        if (!cloudTrafficMap.containsKey(time))
            cloudTrafficMap.put(time, 0);
        cloudTrafficMap.put(time, cloudTrafficMap.get(time) + 1);
    }

    protected void sendTupleToActuator(Tuple tuple) {
		/*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
        for (Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()) {
            int actuatorId = actuatorAssociation.getFirst();
            double delay = actuatorAssociation.getSecond();
            String actuatorType = ((Actuator) CloudSim.getEntity(actuatorId)).getActuatorType();
            if (tuple.getDestModuleName().equals(actuatorType)) {
                send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
                return;
            }
        }
        for (int childId : getChildrenIds()) {
            sendDown(tuple, childId);
        }
    }

    int numClients = 0;

    protected void processTupleArrival(SimEvent ev) {
    		
        Tuple tuple = (Tuple) ev.getData();
      //saeedeh//  System.out.println("//////////////////////Process tuple Arrival function/////////////////////////////");
        if (getName().equals("cloud")) {
            updateCloudTraffic();
        }
        
		
        Logger.debug(getName(), "Received tuple " + tuple.getCloudletId() + "with tupleType : " + tuple.getTupleType() + "\t| Source : " +
                CloudSim.getEntityName(ev.getSource()) + "|Dest : " + CloudSim.getEntityName(ev.getDestination()));
		
       
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);

        if (FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())) {
        }

        if (tuple.getDirection() == Tuple.ACTUATOR) {
            sendTupleToActuator(tuple);
            return;
        }
        
        
        
        
        boolean checkArrival=false;
        if (tuple.getDestModuleName()=="user_interface") {
        	checkArrival=true;
        }
        
        int ss=getHost().getVmList().size();
        
        /*if (getHost().getVmList().size() > 0) {
            final AppModule operator = (AppModule) getHost().getVmList().get(0);            

            if (CloudSim.clock() > 0) {
                getHost().getVmScheduler().deallocatePesForVm(operator);
                getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add((double) getHost().getTotalMips());
                    }
                });
            }
        }*/
        


        if (getName().equals("cloud") && tuple.getDestModuleName() == null) {
            sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
        }

        if (appToModulesMap.containsKey(tuple.getAppId())) {
        	
        	
            if (appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())) {
                int vmId = -1;
                for (Vm vm : getHost().getVmList()) {
                    if (((AppModule) vm).getName().equals(tuple.getDestModuleName()))
                        vmId = vm.getId();
                }
                if (vmId < 0
                        || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) &&
                        tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId)) {
                    return;
                }
                tuple.setVmId(vmId);
                //Logger.error(getName(), "Executing tuple for operator " + moduleName);

                updateTimingsOnReceipt(tuple);

                executeTuple(ev, tuple.getDestModuleName());
            // Saeedeh added this to not consider the energy consumption of warm up interval of devices
                if(ComputationEnergyEstimationTracking==false)
            		ComputationEnergyEstimationTracking=true;
            
            
            } else if (tuple.getDestModuleName() != null) {
                if (tuple.getDirection() == Tuple.UP)
                    sendUp(tuple);
                else if (tuple.getDirection() == Tuple.DOWN) {
                    for (int childId : getChildrenIds())
                        sendDown(tuple, childId);
                }
            } else {
                sendUp(tuple);
            }
       
        
        
        } else {
            if (tuple.getDirection() == Tuple.UP)
                sendUp(tuple);
            else if (tuple.getDirection() == Tuple.DOWN) {
                for (int childId : getChildrenIds())
                    sendDown(tuple, childId);
            }
        }
    }

    protected void updateTimingsOnReceipt(Tuple tuple) {
    	//saeedeh//System.out.println("'''' Update timing on receipt tuple function ''''");
        Application app = getApplicationMap().get(tuple.getAppId());
        String srcModule = tuple.getSrcModuleName();
       //saeedeh// System.out.println(" tuple SrcModuleName() : "+tuple.getSrcModuleName());
        String destModule = tuple.getDestModuleName();
        //saeedeh//System.out.println(" tuple DestModuleName :"+tuple.getDestModuleName());
        List<AppLoop> loops = app.getLoops();
        for (AppLoop loop : loops) {
            if (loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)) {
            	//saeedeh//System.out.println(" loop Id: "+loop.getLoopId()+" loop modules :"+loop.getModules());
                Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                //saeedeh//System.out.println(" start time ="+startTime);
                if (startTime == null)
                    break;
                if (!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())) {
                    TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
                    TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
                }
                double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
                int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
                double delay = CloudSim.clock() - TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                
                
                //saeedeh//System.out.println(" currentAverage : "+currentAverage);
               //saeedeh// System.out.println(" currentCount : "+currentCount);
               //saeedeh// System.out.println(" delay : "+delay);
                
                //////Saeedeh commented this, If you want to use python script to track emission delay you need to uncomment these lines
                //1// System.out.println(app.getAppId());
                //2// System.out.println(" Tuple actual Id "+tuple.getActualTupleId());
                //3// System.out.println(" Loop count is "+currentCount+" and delay is "+delay+" and emit time is "+TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId())+" and completion time is "+CloudSim.clock());
                
                TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
                
               
                
                double newAverage = (currentAverage * currentCount + delay) / (currentCount + 1);
              //saeedeh//  System.out.println(" newAverage : "+newAverage);

                /*
                System.out.println(tuple.getDestModuleName());
                Scanner myObj = new Scanner(System.in);  // Create a Scanner object
    	        System.out.println("event for d0");
    	        String userName = myObj.nextLine();  // Read user input
                if(tuple.getDestinationDeviceId()==5) {
                	System.out.println("AVG(C): "+currentAverage+
                						" C(C): "+currentCount+
                						" delay: "+delay+
                						" AVG(n): "+newAverage);
                	boolean debug=true;
                	if(debug) {
            	        //Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            	        System.out.println("event for d0");
            	        userName = myObj.nextLine();  // Read user input
            	    }
                }*/
                
                TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
                TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount + 1);
                break;
            }
        }
    }

    protected void processSensorJoining(SimEvent ev) {
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
    }

    protected void executeTuple(SimEvent ev, String moduleName) {
        Logger.debug(getName(), "Executing tuple on module " + moduleName);
       //sshh// System.out.println("\n\n"+getName()+ " Executing tuple on module " + moduleName);

        Tuple tuple = (Tuple) ev.getData();

        AppModule module = getModuleByName(moduleName);
        //saeedeh//System.out.println(" Module down instance Id map :"+module.getDownInstanceIdsMaps());
        if (tuple.getDirection() == Tuple.UP) {
            String srcModule = tuple.getSrcModuleName();
            if (!module.getDownInstanceIdsMaps().containsKey(srcModule))
                module.getDownInstanceIdsMaps().put(srcModule, new ArrayList<Integer>());
            if (!module.getDownInstanceIdsMaps().get(srcModule).contains(tuple.getSourceModuleId()))
                module.getDownInstanceIdsMaps().get(srcModule).add(tuple.getSourceModuleId());

           //saeedeh// System.out.println(" Module down instance Id map2 :"+module.getDownInstanceIdsMaps());

            
            int instances = -1;
            for (String _moduleName : module.getDownInstanceIdsMaps().keySet()) {
                instances = Math.max(module.getDownInstanceIdsMaps().get(_moduleName).size(), instances);
            }
            module.setNumInstances(instances);
           //saeedeh// System.out.println("module number instance :"+module.getNumInstances());
        }
        String temp=tuple.getDestModuleName();
        boolean check=false;
        if (temp=="user_interface" | temp=="user_interface_1") {
        	check=true;
        }
        TimeKeeper.getInstance().tupleStartedExecution(tuple);
        //Saeedeh
        for (Vm vm : getVmList()) {
			vm.updateVmProcessing(CloudSim.clock(),getHost().getVmScheduler().getAllocatedMipsForVm(vm));
		}
        updateAllocatedMips(moduleName);
        processCloudletSubmit(ev, false);
        updateAllocatedMips(moduleName);
		for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
			//sshh//System.out.println(getName()+ " MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}
    }

    protected void processModuleArrival(SimEvent ev) {
        AppModule module = (AppModule) ev.getData();
       //saeedeh// System.out.println("WE ARE IN PROCESMODULEARRIVAL FUNCTION AND Data Module name is: "+module.getName());
        String appId = module.getAppId();
        if (!appToModulesMap.containsKey(appId)) {
            appToModulesMap.put(appId, new ArrayList<String>());
        }
        //saeedeh//System.out.println("mmm: "+appToModulesMap.get(appId));
        appToModulesMap.get(appId).add(module.getName());
        processVmCreate(ev, false);
        if (module.isBeingInstantiated()) {
            module.setBeingInstantiated(false);
        }

        initializePeriodicTuples(module);

        module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
                .getAllocatedMipsForVm(module));
        
        //saeedeh//System.out.println("_____________________________ It is the END of PROCESMODULEARRIVAL FUNCTION _________________________________________________________________\n");
    }

    protected void processModuleTermination(SimEvent ev) {
        processVmDestroy(ev, false);
    }

    protected void initializePeriodicTuples(AppModule module) {
        String appId = module.getAppId();
        Application app = getApplicationMap().get(appId);
        List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
        for (AppEdge edge : periodicEdges) {
        	System.out.println("periodic edge source (from initialize periodic tuple): "+edge.getSource());
            send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
        }
    }

    protected void processOperatorRelease(SimEvent ev) {
        this.processVmMigrate(ev, false);
    }


    protected void updateNorthTupleQueue() {
    	
    	updateNetworkingEnergy(false);
    	
    	
    	 ///Saeedeh added this for removing tuples from active tuple list:
    	Iterator<Tuple> iterator = activeTuplesList.iterator();
        while (iterator.hasNext()) {
            Tuple tuple = iterator.next();
            if (CloudSim.clock() >= tuple.getEstimatedFinishSendingTupleTime()) {
                iterator.remove();
            }
        }
    	///Saeedeh added for removing tuples from active tuple list
    	
        if (!getNorthTupleQueue().isEmpty()) {
            Tuple tuple = getNorthTupleQueue().poll();
            sendUpFreeLink(tuple);
        } else {
            setNorthLinkBusy(false);
            if(previousNorthLinkState!= isNorthLinkBusy) {
            double currentTime=CloudSim.clock();
            northLinkBusyTime += currentTime-lastNorthwardCommunicationUpdateTime;
            lastNorthwardCommunicationUpdateTime= currentTime;
            previousNorthLinkState= isNorthLinkBusy;
            }
            
            if(isSouthLinkBusy==false & isNorthLinkBusy==false) {
            	if(isNetworkInterfaceCardBusy==true) {
            		isNetworkInterfaceCardBusy=false;
            		double currentTime= CloudSim.clock();
            		totalNetworkInterfaceCardBusyTime += currentTime - lastNetworkInterfaceCardUpdateTime;
            		lastNetworkInterfaceCardUpdateTime= currentTime;
                	}
            }
        }
        
    }

    protected void sendUpFreeLink(Tuple tuple) {	
        double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
        String appId= tuple.getAppId();
        String tupleType=tuple.getTupleType();
        int deviceId= getId();
        String deviceName= getName();
        double aggBW= getAggregateBandwidth();
        
        updateNetworkingEnergy(false);
        
        setNorthLinkBusy(true);
        send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
        send(parentId, networkDelay + getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
        
        ////* Saeedeh added:
        tuple.setStartSendingTupleTime(CloudSim.clock());
        tuple.setEstimatedFinishSendingTupleTime(CloudSim.clock()+networkDelay);
        activeTuplesList.add(tuple);
        ////*
        
       
        updateNetworkingEnergyConsumptionMap( appId, tuple, networkDelay, deviceId, deviceName);
        
        NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
        // Saeedeh added//
        NetworkUsageMonitor.busyUpLinkEnergy(getId(), networkDelay, getUplinkLatency(), tuple.getCloudletFileSize());
        NetworkUsageMonitor.updateLinksUsageMap(getId(), networkDelay, getUplinkLatency(), tuple.getCloudletFileSize());
        NetworkUsageMonitor.updateTotalTransmissionDelayUplinkMap(getId(), networkDelay);
        //NetworkUsageMonitor.updateLinkEnergy();  
        if(previousNorthLinkState!= isNorthLinkBusy) {
        	double currentTime= CloudSim.clock();
        	northLinkIdleTime += currentTime - lastNorthwardCommunicationUpdateTime;
        	lastNorthwardCommunicationUpdateTime= currentTime;
        	previousNorthLinkState= isNorthLinkBusy;
        	}
        if(isNetworkInterfaceCardBusy== false) {
        	isNetworkInterfaceCardBusy= true;
            double currentTime= CloudSim.clock();
            totalNetworkInterfaceCardIdleTime += currentTime - lastNetworkInterfaceCardUpdateTime;
            lastNetworkInterfaceCardUpdateTime= currentTime;
            	}
    }

    protected void sendUp(Tuple tuple) {
        if (parentId > 0) {
            if (!isNorthLinkBusy()) {
                sendUpFreeLink(tuple);
            } else {
                northTupleQueue.add(tuple);
            }
        }
    }


    protected void updateSouthTupleQueue() {
    	
    	updateNetworkingEnergy(false);
    	
    	///Saeedeh added for removing tuples from active tuple list:
    	Iterator<Tuple> iterator = activeTuplesList.iterator();
        while (iterator.hasNext()) {
            Tuple tuple = iterator.next();
            if (CloudSim.clock() >= tuple.getEstimatedFinishSendingTupleTime()) {
                iterator.remove();
            }
        }
    	///Saeedeh added for removing tuples from active tuple list
    	
    	
    	
        if (!getSouthTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getSouthTupleQueue().poll();
            sendDownFreeLink(pair.getFirst(), pair.getSecond());
        } else {
            setSouthLinkBusy(false);
            if(previousSouthLinkState!= isSouthLinkBusy) {
                double currentTime= CloudSim.clock();
                southLinkBusyTime += currentTime - lastSouthwardCommunicationUpdateTime; //+ getChildToLatencyMap().get(getChildrenIds().get(0));
                lastSouthwardCommunicationUpdateTime= currentTime; //+ getChildToLatencyMap().get(getChildrenIds().get(0));
                previousSouthLinkState= isSouthLinkBusy;
                	}
            
            if(isSouthLinkBusy==false & isNorthLinkBusy==false) {
            	if(isNetworkInterfaceCardBusy==true) {
            		isNetworkInterfaceCardBusy=false;
            		double currentTime= CloudSim.clock();
            		totalNetworkInterfaceCardBusyTime += currentTime - lastNetworkInterfaceCardUpdateTime;
            		lastNetworkInterfaceCardUpdateTime= currentTime;
                	}
            }
        }
        
        
    }

    protected void sendDownFreeLink(Tuple tuple, int childId) {
        double networkDelay = tuple.getCloudletFileSize() / getDownlinkBandwidth();
        //int temp=tuple.getDirection();
        //Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
        
        updateNetworkingEnergy(false);
        
        setSouthLinkBusy(true);
        //System.out.println(getName()+" Sending tuple with tupleType = "+tuple.getTupleType()+" to "+childId);
        double latency = getChildToLatencyMap().get(childId);
        send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
        send(childId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
        updateNetworkingEnergyConsumptionMap( tuple.getAppId(), tuple, networkDelay, getId(), getName());

        
    ////* Saeedeh added:
        tuple.setStartSendingTupleTime(CloudSim.clock());
        tuple.setEstimatedFinishSendingTupleTime(CloudSim.clock()+networkDelay);
        activeTuplesList.add(tuple);
        ////*
        
        
       //Saeedeh added these lines to add the links energy consumption//
        //NetworkUsageMonitor.busyDownLinkEnergy(childId, networkDelay, latency, tuple.getCloudletFileSize());
        //NetworkUsageMonitor.updateLinksUsageMap(childId, networkDelay, getUplinkLatency(), tuple.getCloudletFileSize());
        NetworkUsageMonitor.updateTotalTransmissionDelayDownlinkMap(getId(), networkDelay);
        if(previousSouthLinkState!= isSouthLinkBusy) {
            double currentTime= CloudSim.clock();
            southLinkIdleTime += currentTime - lastSouthwardCommunicationUpdateTime;
            lastSouthwardCommunicationUpdateTime= currentTime;
            previousSouthLinkState= isSouthLinkBusy;
            	}
        if(isNetworkInterfaceCardBusy== false) {
        	isNetworkInterfaceCardBusy = true;
            double currentTime= CloudSim.clock();
            totalNetworkInterfaceCardIdleTime += currentTime - lastNetworkInterfaceCardUpdateTime;
            lastNetworkInterfaceCardUpdateTime= currentTime;
            	}
    }

    protected void sendDown(Tuple tuple, int childId) {
        if (getChildrenIds().contains(childId)) {
            if (!isSouthLinkBusy()) {
                sendDownFreeLink(tuple, childId);
            } else {
                southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
            }
        }
    }
    
    
    
    //saeedeh added for last update of NIC and track the time:
    public void lastUpdate() {
    	
    	updateNetworkingEnergy(true);
    	
    	if(isNetworkInterfaceCardBusy== false) {
    		double currentTime= CloudSim.clock();
            totalNetworkInterfaceCardIdleTime += currentTime - lastNetworkInterfaceCardUpdateTime;
            lastNetworkInterfaceCardUpdateTime= currentTime;
    	}
    	if (isNetworkInterfaceCardBusy== true){
    		double currentTime= CloudSim.clock();
            totalNetworkInterfaceCardBusyTime += currentTime - lastNetworkInterfaceCardUpdateTime;
            lastNetworkInterfaceCardUpdateTime= currentTime;
    	}
    	
    	
    	calculateFlowBasedModelNetworkingEnergy();
    	
    }
    
    public void updateNetworkingEnergy(boolean last) {
    	double intervalTime = CloudSim.clock()-previous_networking_update_state_time;
    	int southLinkBusy = isSouthLinkBusy ? 1 : 0;
		int northLinkBusy = isNorthLinkBusy ? 1 : 0;
		double active_BW = (northLinkBusy*uplinkBandwidth) + (southLinkBusy*downlinkBandwidth);
		double time = CloudSim.clock();
		
		
    	if (getDeviceType()=="CPE") {
    		  		
    		for (Tuple tuple : activeTuplesList) {
    			    			
    			double intervalActiveEnergy =0.0;
    			double intervalPartiallyIdleEnergy = 0.0;
    					
    			if(tuple.getDirection()==1) {
		    		
		    		intervalActiveEnergy = intervalTime*(networkingMaxPower-networkingIdlePower)*(uplinkBandwidth/aggregateBandwidth);
		    		intervalPartiallyIdleEnergy = intervalTime * (northLinkBusy*uplinkBandwidth)/active_BW;
    			} else {
    				intervalActiveEnergy = intervalTime*(networkingMaxPower-networkingIdlePower)*(downlinkBandwidth/aggregateBandwidth);
    				intervalPartiallyIdleEnergy = intervalTime * (southLinkBusy*downlinkBandwidth)/active_BW;
    			}

    				
    				// Update timing map
					updateNetworkingTuplesTimeMap(tuple, intervalTime);
		    		
		    		 /// update Energy Map
		             updateTuplesEnergyMap(tuple, intervalActiveEnergy);
		    		 
		    		 ///Update Idle energy Map
		    		 if (!networkingTuplesIdleEnergyMap.containsKey(tuple.getAppId())) {
		    			 networkingTuplesIdleEnergyMap.put(tuple.getAppId(), new HashMap<>());
		    		 }

		    		 Map<String, Double> idleEnergyInnerMap = networkingTuplesIdleEnergyMap.get(tuple.getAppId());

		    		 if (!idleEnergyInnerMap.containsKey(tuple.getTupleType())) {
		    			 idleEnergyInnerMap.put(tuple.getTupleType(), 0.0);
		    		 }
		    		 double currentIdleEnergy = idleEnergyInnerMap.get(tuple.getTupleType());
		    		 idleEnergyInnerMap.put(tuple.getTupleType(), currentIdleEnergy + intervalPartiallyIdleEnergy);
		    		 
    		}
    		
    		if (last==true) {
//    			double uplinkFinalTotalIdleEnergy = networkingIdlePower*(Config.MAX_SIMULATION_TIME/totalNetworkInterfaceCardBusyTime)*uplinkTotalIdleEnergy ;
//    			double downlinkFinalTotalIdleEnergy = networkingIdlePower*(Config.MAX_SIMULATION_TIME/totalNetworkInterfaceCardBusyTime)*downlinkTotalIdleEnergy ;
//	    		System.out.println(" uplinkFinalTotalIdleEnergy "+uplinkFinalTotalIdleEnergy);
//	    		System.out.println(" downlinkFinalTotalIdleEnergy "+downlinkFinalTotalIdleEnergy);

    			for(String appId: networkingTuplesIdleEnergyMap.keySet()) {
    				Map<String, Double> innerMap = networkingTuplesIdleEnergyMap.get(appId);
    				for (String tupleType: innerMap.keySet()) {
    					double currentIdleEnergy = innerMap.get(tupleType);
    			        double newIdleEnergy = currentIdleEnergy * (networkingIdlePower * Config.MAX_SIMULATION_TIME / totalNetworkInterfaceCardBusyTime);
    			        innerMap.put(tupleType, newIdleEnergy);
			    		 
	    				}
    				}
    			
    			// Assuming networkingTuplesEnergyMap and networkingTuplesIdleEnergyMap are defined as Map<String, Map<String, Double>>

    			for (String appId : networkingTuplesEnergyMap.keySet()) {
    			    Map<String, Double> energyMap = networkingTuplesEnergyMap.get(appId);
    			    Map<String, Double> idleEnergyMap = networkingTuplesIdleEnergyMap.get(appId);

    			    for (String tupleType : energyMap.keySet()) {
    			        double energyValue = energyMap.get(tupleType);
    			        
    			        // Check if the idleEnergyMap contains the same tupleType
    			        if (idleEnergyMap.containsKey(tupleType)) {
    			            double idleEnergyValue = idleEnergyMap.get(tupleType);

    			            // Add the energy values
    			            double totalEnergyValue = energyValue + idleEnergyValue;

    			            // Update the value in the energy map
    			            energyMap.put(tupleType, totalEnergyValue);
    			            
    			         // Calculate CO2 emissions based on the final total energy
    	                    double co2Emissions = calculateCO2Emissions(totalEnergyValue);
    	                    if (!networkingTuplesCO2Map.containsKey(appId)) {
    	                        networkingTuplesCO2Map.put(appId, new HashMap<>());
    	                    }

    	                    Map<String, Double> co2InnerMap = networkingTuplesCO2Map.get(appId);

    	                    if (!co2InnerMap.containsKey(tupleType)) {
    	                        co2InnerMap.put(tupleType, 0.0);
    	                    }

    	                    double currentCO2 = co2InnerMap.get(tupleType);
    	                    co2InnerMap.put(tupleType, currentCO2 + co2Emissions);
    	                    
    			        } else {
    			            // Handle the case where tupleType is not present in idleEnergyMap
    			            // You might want to log an error or handle it according to your requirements
    			            System.err.println("TupleType " + tupleType + " not found in idleEnergyMap for appId " + appId);
    			        }
    			    }
    			}
    			}
    	}
    		
    	
    	if(getDeviceType()== "Shared") {
    		
    		for (Tuple tuple : activeTuplesList) {
    			
    			double intervalActiveEnergy =0.0;
    			double intervalIdleEnergy = 0.0;
    			
    			
		    	//if(isNorthLinkBusy && isSouthLinkBusy) {
		    		//double active_BW = (northLinkBusy*uplinkBandwidth) + (southLinkBusy*downlinkBandwidth);
    			if(tuple.getDirection()==1) {
		    		
		    		intervalActiveEnergy = ((uplinkBandwidth*intervalTime)*(networkingMaxPower-networkingIdlePower)/aggregateBandwidth);
		    		intervalIdleEnergy = intervalTime * (northLinkBusy*uplinkBandwidth)*networkingIdlePower/active_BW;
    			} else {
		    		intervalActiveEnergy = ((downlinkBandwidth*intervalTime)*(networkingMaxPower-networkingIdlePower)/aggregateBandwidth);
    				intervalIdleEnergy = intervalTime * (southLinkBusy*downlinkBandwidth)*networkingIdlePower/active_BW;
    			}
		    		
		    		
		    		// Update timing map
    				updateNetworkingTuplesTimeMap(tuple, intervalTime);
		    		 
		    		 /// update Tuples Energy Map
		             updateTuplesEnergyMap(tuple, intervalActiveEnergy + intervalIdleEnergy);
		             
		             // Update CO2 Emission Map
		             updateTuplesCO2Map(tuple, intervalActiveEnergy + intervalIdleEnergy);
    		}
    	}
    	
    	previous_networking_update_state_time = CloudSim.clock();
    }
    
    ///// Saeedeh override this function to track and removing outdated VM DC events without changing the core
    @Override
	protected void processCloudletSubmit(SimEvent ev, boolean ack) {
		//updateCloudletProcessing(ev);
		//updateCloudletProcessing();
		try {
			// gets the Cloudlet object
			Cloudlet cl = (Cloudlet) ev.getData();
			// checks whether this Cloudlet has finished or not
			if (cl.isFinished()) {
				String name = CloudSim.getEntityName(cl.getUserId());
				Log.printLine(getName() + ": Warning - Cloudlet #" + cl.getCloudletId() + " owned by " + name
						+ " is already completed/finished.");
				Log.printLine("Therefore, it is not being executed again");
				Log.printLine();

				
				
				System.out.println(getName() + ": Warning - Cloudlet #" + cl.getCloudletId() + " owned by " + name
						+ " is already completed/finished.");
				System.out.println("Therefore, it is not being executed again");
				
				
				
				// NOTE: If a Cloudlet has finished, then it won't be processed.
				// So, if ack is required, this method sends back a result.
				// If ack is not required, this method don't send back a result.
				// Hence, this might cause CloudSim to be hanged since waiting
				// for this Cloudlet back.
				if (ack) {
					int[] data = new int[3];
					data[0] = getId();
					data[1] = cl.getCloudletId();
					data[2] = CloudSimTags.FALSE;

					// unique tag = operation tag
					int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
					sendNow(cl.getUserId(), tag, data);
				}

				sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

				return;
			}

			// process this Cloudlet to this CloudResource
			cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics()
					.getCostPerBw());

			int userId = cl.getUserId();
			int vmId = cl.getVmId();
						// time to transfer the files
			double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
			Host host = getVmAllocationPolicy().getHost(vmId, userId);
			Vm vm = host.getVm(vmId, userId);
			CloudletScheduler scheduler = vm.getCloudletScheduler();
			double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);
			
			// if this cloudlet is in the exec queue
			if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
				estimatedFinishTime += fileTransferTime;
				/*	edited by HARSHIT	*/
				//if(getName().equals("gateway-3"))
				//System.out.println(getName()+" : ESTIMATED FINISH TIME ON "+((StreamOperator)vm).getName()+": "+estimatedFinishTime);
				
				
				///Saeedeh commented this line to all events created for a time more than min time (To unified the logic of simulator with what is happening in cloudletscheduler time share when calculate estimated finish time)
				 //This line was the original adding event event for estimated finish time of Cloudlet
				//send(getId(), CloudSim.getMinTimeBetweenEvents()
						//+estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);

				
				// Saeedeh added this round up short estimated finish timees to the min time between events based on simulators logic
                if (estimatedFinishTime<CloudSim.getMinTimeBetweenEvents()) {
                	send(getId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_DATACENTER_EVENT);
                }else {
                	send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
                }
				
			}

			////************************************************************************************
			/////Saeedeh [Bug] added this because in mintime calculation this new arriving cloudlet (that is submitted above with scheduler.cloudletSubmit(cl, fileTransferTime);) is not considered
			///// Now after submitting new cl to exec list we again call updateCloudletProcessing(ev);
			updateCloudletProcessing(ev);
			
			

			
			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = cl.getCloudletId();
				data[2] = CloudSimTags.TRUE;

				// unique tag = operation tag
				int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
				sendNow(cl.getUserId(), tag, data);
			}
		} catch (ClassCastException c) {
			Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
			c.printStackTrace();
		} catch (Exception e) {
			Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
			e.printStackTrace();
		}

		checkCloudletCompletion();
		setCloudletSubmitted(CloudSim.clock());
	}
    
    

    
    @Override
    protected void updateCloudletProcessing() {

        double currentTime = CloudSim.clock();

        if (currentTime > getLastProcessTime()) {
            double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();

            // Handle early completed cloudlets
            /*for (Host host : getVmAllocationPolicy().getHostList()) {
                for (Vm vm : host.getVmList()) {
                    CloudletScheduler scheduler = vm.getCloudletScheduler();

                    // Get the list of finished cloudlets
                    List<Cloudlet> finishedCloudlets = new ArrayList<>();
                    Cloudlet cloudlet;
                    while ((cloudlet = scheduler.getNextFinishedCloudlet()) != null) {
                        finishedCloudlets.add(cloudlet);
                    }

                    // Process each finished cloudlet
                    for (Cloudlet cl : finishedCloudlets) {
                        double estimatedFinishTime = Double.MAX_VALUE;
                        if (cl instanceof Tuple) {
                            estimatedFinishTime = ((Tuple) cl).getEstimatedFinishTime();
                        }

                        // Compare actual and estimated finish times
                        if (currentTime < estimatedFinishTime) {
                            // Cancel outdated events
                            cancelOutdatedEvents(currentTime, estimatedFinishTime);
                        }

                        // Send the finished cloudlet back to the user
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }*/
            
            //Saeedeh: minTime should be calculated based on future capacities(previous minTime was based on previous capacity data)
            //(in checkcloudletcompletion the capacities are updated but the mintime is based on outdated capacities; so we repeat mintime calculation now:)
            
            
            
            minTime = Double.MAX_VALUE;
            for (final Vm vm : getHost().getVmList()) {
                AppModule operator = (AppModule) vm;
                double nextEventTime=operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
                        .getAllocatedMipsForVm(operator));
                if(nextEventTime > 0.0 && nextEventTime<minTime) {
                	minTime=nextEventTime;
                }
            }

            // Reschedule the next event based on minTime
            if (minTime != Double.MAX_VALUE) {
            	int removedEvents = 0;
                double delay = minTime - currentTime;
                if (delay < CloudSim.getMinTimeBetweenEvents()) {
                    delay = CloudSim.getMinTimeBetweenEvents();
                }
            	removedEvents=CloudSim.cancelAllCount(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                send(getId(), delay, CloudSimTags.VM_DATACENTER_EVENT);
            }

            setLastProcessTime(currentTime);
        }
    }

    ///Saeedeh added this function to resolve the infinity estimated finish time problem. This problem happens when arriving a tuple cause finish of other tuples and checkCloudLetCompletion function should call with (ev)
    /// To consider mips allocation for arriving tuple 
    @Override
    protected void updateCloudletProcessing(SimEvent ev) {
    	int removedEvents = 0;
		if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
		//if (getCloudletSubmitted() == -1) {
			removedEvents=CloudSim.cancelAllCount(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
			///////////////// Saeedeh added this
			//if (removedEvents>0)
				//schedule(getId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_DATACENTER_EVENT);
			///////////////// Saeedeh added
			double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce(ev);
			if (minTime != Double.MAX_VALUE) {
                double delay = minTime - CloudSim.clock();
                if (delay < CloudSim.getMinTimeBetweenEvents()) {
                    delay = CloudSim.getMinTimeBetweenEvents();
                }
                send(getId(), delay, CloudSimTags.VM_DATACENTER_EVENT);
            }else
            	schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			return;
		}
        double currentTime = CloudSim.clock();

        if (currentTime > getLastProcessTime()) {
            double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce(ev);
            
            if (!isDisableMigrations()) {
				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());

				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						Vm vm = (Vm) migrate.get("vm");
						PowerHost targetHost = (PowerHost) migrate.get("host");
						PowerHost oldHost = (PowerHost) vm.getHost();

						if (oldHost == null) {
							Log.formatLine(
									"%.2f: Migration of VM #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									targetHost.getId());
						} else {
							Log.formatLine(
									"%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									oldHost.getId(),
									targetHost.getId());
						}

						targetHost.addMigratingInVm(vm);
						incrementMigrationCount();

						/** VM migration delay = RAM / bandwidth **/
						// we use BW / 2 to model BW available for migration purposes, the other
						// half of BW is for VM communication
						// around 16 seconds for 1024 MB using 1 Gbit/s network
						send(
								getId(),
								vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
								CloudSimTags.VM_MIGRATE,
								migrate);
					}
				}
			}

            // Reschedule the next event based on minTime
            if (minTime != Double.MAX_VALUE) {
                double delay = minTime - currentTime;
                if (delay < CloudSim.getMinTimeBetweenEvents()) {
                    delay = CloudSim.getMinTimeBetweenEvents();
                }
                CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                send(getId(), delay, CloudSimTags.VM_DATACENTER_EVENT);
            }

            setLastProcessTime(currentTime);
        }
    }
    
    
    
    
    
    protected void updateCloudletProcessing(SimEvent ev, double arrivingEstimatedFinishTime) {
    	int removedEvents = 0;
		if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
		//if (getCloudletSubmitted() == -1) {
			removedEvents=CloudSim.cancelAllCount(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
			///////////////// Saeedeh added this
			//if (removedEvents>0)
				//schedule(getId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_DATACENTER_EVENT);
			///////////////// Saeedeh added
			double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce(ev);
			if (minTime != Double.MAX_VALUE) {
                double delay = minTime - CloudSim.clock();
                if (delay < CloudSim.getMinTimeBetweenEvents()) {
                    delay = CloudSim.getMinTimeBetweenEvents();
                }
                send(getId(), delay, CloudSimTags.VM_DATACENTER_EVENT);
            }else
            	schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			return;
		}
        double currentTime = CloudSim.clock();

        if (currentTime > getLastProcessTime()) {
            double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce(ev);
            
            if (!isDisableMigrations()) {
				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());

				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						Vm vm = (Vm) migrate.get("vm");
						PowerHost targetHost = (PowerHost) migrate.get("host");
						PowerHost oldHost = (PowerHost) vm.getHost();

						if (oldHost == null) {
							Log.formatLine(
									"%.2f: Migration of VM #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									targetHost.getId());
						} else {
							Log.formatLine(
									"%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									oldHost.getId(),
									targetHost.getId());
						}

						targetHost.addMigratingInVm(vm);
						incrementMigrationCount();

						/** VM migration delay = RAM / bandwidth **/
						// we use BW / 2 to model BW available for migration purposes, the other
						// half of BW is for VM communication
						// around 16 seconds for 1024 MB using 1 Gbit/s network
						send(
								getId(),
								vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
								CloudSimTags.VM_MIGRATE,
								migrate);
					}
				}
			}

            // Reschedule the next event based on minTime
            if (minTime != Double.MAX_VALUE && (minTime - currentTime) <= arrivingEstimatedFinishTime) {
                double delay = minTime - currentTime;
                if (delay < CloudSim.getMinTimeBetweenEvents()) {
                    delay = CloudSim.getMinTimeBetweenEvents();
                }
                CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                send(getId(), delay, CloudSimTags.VM_DATACENTER_EVENT);
            }
            else {
            	double ttt=0;
            }

            setLastProcessTime(currentTime);
        }
    }
    
    
    
    private void cancelOutdatedEvents(double actualFinishTime, double estimatedFinishTime) {
        // Cancel VM_DATACENTER_EVENTs scheduled between actualFinishTime and estimatedFinishTime
        CloudSim.cancelAll(getId(), new PredicateBetweenTimes(CloudSimTags.VM_DATACENTER_EVENT, actualFinishTime, estimatedFinishTime));
    }
    
    
    
    
    private void updateTuplesEnergyMap(Tuple tuple, double energy) {
        if (!networkingTuplesEnergyMap.containsKey(tuple.getAppId())) {
            networkingTuplesEnergyMap.put(tuple.getAppId(), new HashMap<>());
        }

        Map<String, Double> energyInnerMap = networkingTuplesEnergyMap.get(tuple.getAppId());

        if (!energyInnerMap.containsKey(tuple.getTupleType())) {
            energyInnerMap.put(tuple.getTupleType(), 0.0);
        }

        double currentEnergy = energyInnerMap.get(tuple.getTupleType());
        energyInnerMap.put(tuple.getTupleType(), currentEnergy + energy);
    }
    
    
    
    private void updateTuplesCO2Map(Tuple tuple, double energy) {
        if (!networkingTuplesCO2Map.containsKey(tuple.getAppId())) {
            networkingTuplesCO2Map.put(tuple.getAppId(), new HashMap<>());
        }

        Map<String, Double> co2InnerMap = networkingTuplesCO2Map.get(tuple.getAppId());

        if (!co2InnerMap.containsKey(tuple.getTupleType())) {
            co2InnerMap.put(tuple.getTupleType(), 0.0);
        }

        double currentCO2 = co2InnerMap.get(tuple.getTupleType());
        double co2Emissions = calculateCO2Emissions(energy);
        co2InnerMap.put(tuple.getTupleType(), currentCO2 + co2Emissions);
    }
    
 //  Method to update the networking tuples time map
    private void updateNetworkingTuplesTimeMap(Tuple tuple, double intervalTime) {
        if (!networkingTuplesTimeMap.containsKey(tuple.getAppId())) {
            networkingTuplesTimeMap.put(tuple.getAppId(), new HashMap<>());
        }

        Map<String, Double> timeInnerMap = networkingTuplesTimeMap.get(tuple.getAppId());

        if (!timeInnerMap.containsKey(tuple.getTupleType())) {
            timeInnerMap.put(tuple.getTupleType(), 0.0);
        }

        double currentActiveTime = timeInnerMap.get(tuple.getTupleType());
        timeInnerMap.put(tuple.getTupleType(), currentActiveTime + intervalTime);
    }
    
    
    // Saeedeh added this for network energy consumption of flow based model
    public double calculateNetworkingEnergyConsumptionForCPE(double transmissionTime) {
    	//	calculatedNetworkingEnergy=(Config.CPE_Idle_Power_Fast_Ethernet_Gateway*Config.MAX_SIMULATION_TIME);
    	double partialTotalTime = CloudSim.clock() - previous_update_networking_time_CPE;
    	previous_update_networking_time_CPE = CloudSim.clock();
    	double calculatedNetworkingEnergy = partialTotalTime * Config.CPE_Idle_Power_Fast_Ethernet_Gateway;
    	calculatedNetworkingEnergy += (Config.CPE_Max_Power_Fast_Ethernet_Gateway-Config.CPE_Idle_Power_Fast_Ethernet_Gateway)*transmissionTime;
    	return calculatedNetworkingEnergy;
    }
    
    public static double calculateNetworkingEnergyConsumptionForSharedEdgeEquipment(double transmissionTime) {
    	double calculatedNetworkingEnergy = Config.Shared_Edge_Equipment_Idle_Power*transmissionTime+(Config.Shared_Edge_Equipment_Max_Power-Config.Shared_Edge_Equipment_Idle_Power)*transmissionTime;
    	return calculatedNetworkingEnergy;
    }
    
    public void updateNetworkingEnergyConsumptionMap(String appId, Tuple tuple, double transmissionTime, double deviceId, String deviceName) {
    	String tupleType=tuple.getTupleType();
    	double calculatedNetworkingEnergy=0.0;
    	if (deviceName.startsWith("m")) {
    		 calculatedNetworkingEnergy=calculateNetworkingEnergyConsumptionForCPE (transmissionTime);
    		
    	}else if(deviceName.startsWith("c")){
    		 calculatedNetworkingEnergy = Config.Shared_Core_Equipment_Idle_Power*transmissionTime+(Config.Shared_Core_Equipment_Max_Power-Config.Shared_Core_Equipment_Idle_Power)*transmissionTime;
    	}else {
    		calculatedNetworkingEnergy= calculateNetworkingEnergyConsumptionForSharedEdgeEquipment(transmissionTime);
		}
    	
    	
    	if(tuple.getDirection()==1) {
    		calculatedNetworkingEnergy = calculatedNetworkingEnergy*(uplinkBandwidth/aggregateBandwidth);
		} else {
			calculatedNetworkingEnergy = calculatedNetworkingEnergy*(downlinkBandwidth/aggregateBandwidth);
		}
    
    	
    	
    	
    	
	    if (!networkingEnergyConsumptionMap.containsKey(appId))
	    	networkingEnergyConsumptionMap.put(appId, new HashMap<String, Double>());
	    Map<String, Double> innerMap = networkingEnergyConsumptionMap.get(appId);

	    if(!networkingEnergyConsumptionMap.get(appId).containsKey(tupleType))
		    networkingEnergyConsumptionMap.get(appId).put(tupleType, 0.0);

	    double currentNetworkingEnergy = networkingEnergyConsumptionMap.get(appId).get(tupleType);
	    innerMap.put(tupleType, currentNetworkingEnergy + calculatedNetworkingEnergy);

	    networkingEnergyConsumptionMap.get(appId).put(tupleType, innerMap.get(tupleType));
	    
	    
	   

	 // Check if the outer map contains the key
	 if (!networkingTimeMap.containsKey(appId)) {
	     networkingTimeMap.put(appId, new HashMap<>());
	 }

	 // Get the inner map
	 Map<String, Double> timeInnerMap = networkingTimeMap.get(appId);

	 // Check if the inner map contains the key
	 if (!timeInnerMap.containsKey(tupleType)) {
	     timeInnerMap.put(tupleType, 0.0);
	 }

	 // Update the value in the inner map
	 double currentActiveTime = timeInnerMap.get(tupleType);
	 timeInnerMap.put(tupleType, currentActiveTime + transmissionTime);

	 // Optional: You can remove the last line since the value is already updated in the inner map
	 // networkingTimeMap.get(appId).put(tupleType, timeInnerMap.get(tupleType));
    
	    
    }

    
    public double calculateFlowBasedModelNetworkingEnergy() {
    	String deviceType= "shared";
    	double powerIdle=0.0;
    	double powerActive=0.0;
    	String deviceName=getName();
    	if (deviceName.startsWith("m")) {
    		powerIdle=Config.CPE_Idle_Power_Fast_Ethernet_Gateway;
    		powerActive= Config.CPE_Max_Power_Fast_Ethernet_Gateway-Config.CPE_Idle_Power_Fast_Ethernet_Gateway;
    		deviceType="CPE";
    	}else if(deviceName.startsWith("c")){
    		powerIdle = Config.Shared_Core_Equipment_Idle_Power;
    		powerActive = Config.Shared_Core_Equipment_Max_Power-Config.Shared_Core_Equipment_Idle_Power;
    	}else {
    		powerIdle = Config.Shared_Edge_Equipment_Idle_Power;
    		powerActive = Config.Shared_Edge_Equipment_Max_Power-Config.Shared_Edge_Equipment_Idle_Power;
   		}
   
    	double totalActiveEnergy=0.0;
    	double totalIdleEnergy=0.0;
    	double totalDeviceNetworkingEnergy=0.0;
    	
    	if(deviceType == "shared") {
	    	double sharedBusyTime = northLinkBusyTime+southLinkBusyTime-totalNetworkInterfaceCardBusyTime;
	    	double northLinkIdleEnergy=(northLinkBusyTime-sharedBusyTime)*powerIdle+
	    			sharedBusyTime*powerIdle*(uplinkBandwidth/(uplinkBandwidth+downlinkBandwidth));
	    	
	    	double southLinkIdleEnergy=(southLinkBusyTime-sharedBusyTime)*powerIdle+
	    			sharedBusyTime*powerIdle*(downlinkBandwidth/(uplinkBandwidth+downlinkBandwidth));
	    	
	    	double northLinkActiveEnergy = northLinkBusyTime*powerActive*(uplinkBandwidth/(uplinkBandwidth+downlinkBandwidth));
	    	
	    	double southLinkActiveEnergy = southLinkBusyTime*powerActive*(downlinkBandwidth/(uplinkBandwidth+downlinkBandwidth));
	    	
	    	totalActiveEnergy= northLinkActiveEnergy+southLinkActiveEnergy;
	    			
	    	totalIdleEnergy= northLinkIdleEnergy+southLinkIdleEnergy;
	    	
	    	totalDeviceNetworkingEnergy= totalActiveEnergy+totalIdleEnergy;
    	}
    	
    	if(deviceType == "CPE") {
    		totalActiveEnergy = powerActive * (totalNetworkInterfaceCardBusyTime);
    		totalIdleEnergy = powerIdle * Config.MAX_SIMULATION_TIME;
    		totalDeviceNetworkingEnergy= totalActiveEnergy+totalIdleEnergy;
    	}
    	//* Saeedeh commented this 
    	//System.out.println("\n\n***************************************************\n");
    	//System.out.println( getName()+ " Total Idle Networking Energy with Flow-base model = "+ totalIdleEnergy);
    	
    	//System.out.println(getName()+ " Total Active Networking Energy with Flow-base model = "+ totalActiveEnergy);

    	//System.out.println(getName()+ " Total Device Networking Energy with Flow-base model = "+ totalDeviceNetworkingEnergy);
    	// Saeedeh commented this 

    	return totalDeviceNetworkingEnergy;
    }
    
    protected void sendToSelf(Tuple tuple) {
        send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
    }

    public PowerHost getHost() {
        return (PowerHost) getHostList().get(0);
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public List<Integer> getChildrenIds() {
        return childrenIds;
    }

    public void setChildrenIds(List<Integer> childrenIds) {
        this.childrenIds = childrenIds;
    }

    public double getUplinkBandwidth() {
        return uplinkBandwidth;
    }

    public void setUplinkBandwidth(double uplinkBandwidth) {
        this.uplinkBandwidth = uplinkBandwidth;
    }

    // Saeedeh added:
    public void setAggregateBandwidth(double aggregateBandwidth) {
        this.aggregateBandwidth = aggregateBandwidth;
    }
    
    public double getAggregateBandwidth() {
        return aggregateBandwidth;
    }
    ////
    
    
    public double getUplinkLatency() {
        return uplinkLatency;
    }

    public void setUplinkLatency(double uplinkLatency) {
        this.uplinkLatency = uplinkLatency;
    }

    public boolean isSouthLinkBusy() {
        return isSouthLinkBusy;
    }

    public boolean isNorthLinkBusy() {
        return isNorthLinkBusy;
    }

    public void setSouthLinkBusy(boolean isSouthLinkBusy) {
        this.isSouthLinkBusy = isSouthLinkBusy;
    }

    public void setNorthLinkBusy(boolean isNorthLinkBusy) {
        this.isNorthLinkBusy = isNorthLinkBusy;
    }

    public int getControllerId() {
        return controllerId;
    }

    public void setControllerId(int controllerId) {
        this.controllerId = controllerId;
    }

    public List<String> getActiveApplications() {
        return activeApplications;
    }

    public void setActiveApplications(List<String> activeApplications) {
        this.activeApplications = activeApplications;
    }

    public Map<Integer, List<String>> getChildToOperatorsMap() {
        return childToOperatorsMap;
    }

    public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
        this.childToOperatorsMap = childToOperatorsMap;
    }

    public Map<String, Application> getApplicationMap() {
        return applicationMap;
    }

    public void setApplicationMap(Map<String, Application> applicationMap) {
        this.applicationMap = applicationMap;
    }

    public Queue<Tuple> getNorthTupleQueue() {
        return northTupleQueue;
    }

    public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
        this.northTupleQueue = northTupleQueue;
    }

    public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
        return southTupleQueue;
    }

    public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
        this.southTupleQueue = southTupleQueue;
    }

    public double getDownlinkBandwidth() {
        return downlinkBandwidth;
    }

    public void setDownlinkBandwidth(double downlinkBandwidth) {
        this.downlinkBandwidth = downlinkBandwidth;
    }

    public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
        return associatedActuatorIds;
    }

    public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
        this.associatedActuatorIds = associatedActuatorIds;
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }
    
    
    //Saeedeh added /////////////////////
    public double getApplicationEnergyConsumption() {
        return applicationEnergyConsumption;
    }
    
    
    
    public double getTotalNetworkInterfaceCardIdleTime() {
        return totalNetworkInterfaceCardIdleTime;
    }
    
    public double getTotalNetworkInterfaceCardBusyTime() {
        return totalNetworkInterfaceCardBusyTime;
    }

    
    
    public double getNortLinkIdleTime() {
        return northLinkIdleTime;
    }
    
    
    
    public double getNorthLinkBusyTime() {
        return northLinkBusyTime;
    }
    
    public double getSouthLinkIdleTime() {
        return southLinkIdleTime;
    }
    
    
    
    public double getSouthLinkBusyTime() {
        return southLinkBusyTime;
    }
    
    /////////////////////////////

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }
    
    public void setApplicationEnergyConsumption(double appEnergyConsumption) {
        this.applicationEnergyConsumption = appEnergyConsumption;
    }

    public Map<Integer, Double> getChildToLatencyMap() {
        return childToLatencyMap;
    }

    public void setChildToLatencyMap(Map<Integer, Double> childToLatencyMap) {
        this.childToLatencyMap = childToLatencyMap;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getRatePerMips() {
        return ratePerMips;
    }

    public void setRatePerMips(double ratePerMips) {
        this.ratePerMips = ratePerMips;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public Map<String, Map<String, Integer>> getModuleInstanceCount() {
        return moduleInstanceCount;
    }

    public void setModuleInstanceCount(
            Map<String, Map<String, Integer>> moduleInstanceCount) {
        this.moduleInstanceCount = moduleInstanceCount;
    }

    public List<String> getPlacedAppModulesPerApplication(String appId) {
        return appToModulesMap.get(appId);
    }

    public void removeChild(int childId) {
        // TODO Auto-generated method stub
        @SuppressWarnings("deprecation")
        Integer childIDobject = new Integer(childId);
        if (getChildrenIds().contains(childId) && childId != getId())
            getChildrenIds().remove(childIDobject);
        if (getChildToOperatorsMap().containsKey(childId)) {
            List<String> operatorName = getChildToOperatorsMap().get(childId);
            getChildToOperatorsMap().remove(childId, operatorName);
        }
    }

    public void setClusterMembers(List clusterList) {
        this.clusterMembers = clusterList;
    }

    public void addClusterMember(int clusterMemberId) {
        this.clusterMembers.add(clusterMemberId);
    }

    public List<Integer> getClusterMembers() {
        return this.clusterMembers;
    }

    public void setIsInCluster(Boolean bool) {
        this.isInCluster = bool;
    }

    public void setSelfCluster(Boolean bool) {
        this.selfCluster = bool;
    }

    public Boolean getIsInCluster() {
        return this.isInCluster;
    }

    public Boolean getSelfCluster() {
        return this.selfCluster;
    }

    public void setClusterMembersToLatencyMap(Map<Integer, Double> clusterMembersToLatencyMap) {
        this.clusterMembersToLatencyMap = clusterMembersToLatencyMap;
    }

    public Map<Integer, Double> getClusterMembersToLatencyMap() {
        return this.clusterMembersToLatencyMap;
    }

    protected void processClustering(int parentId, int nodeId, SimEvent ev) {
        JSONObject objectLocator = (JSONObject) ev.getData();
        Clustering cms = new Clustering();
        cms.createClusterMembers(this.getParentId(), this.getId(), objectLocator);
    }

    public double getClusterLinkBandwidth() {
        return clusterLinkBandwidth;
    }

    protected void setClusterLinkBandwidth(double clusterLinkBandwidth) {
        this.clusterLinkBandwidth = clusterLinkBandwidth;
    }

    protected void sendToCluster(Tuple tuple, int clusterNodeID) {
        if (getClusterMembers().contains(clusterNodeID)) {
            if (!isClusterLinkBusy) {
                sendThroughFreeClusterLink(tuple, clusterNodeID);
            } else {
                clusterTupleQueue.add(new Pair<Tuple, Integer>(tuple, clusterNodeID));
            }
        }
    }

    private void updateClusterTupleQueue() {
        if (!getClusterTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getClusterTupleQueue().poll();
            sendThroughFreeClusterLink(pair.getFirst(), pair.getSecond());
        } else {
            setClusterLinkBusy(false);
        }
    }

    private void sendThroughFreeClusterLink(Tuple tuple, Integer clusterNodeID) {
        double networkDelay = tuple.getCloudletFileSize() / getClusterLinkBandwidth();
        setClusterLinkBusy(true);
        double latency = (getClusterMembersToLatencyMap()).get(clusterNodeID);
        send(getId(), networkDelay, FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE);
        send(clusterNodeID, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
    }

    protected void setClusterLinkBusy(boolean busy) {
        this.isClusterLinkBusy = busy;
    }

    public Queue<Pair<Tuple, Integer>> getClusterTupleQueue() {
        return clusterTupleQueue;
    }
    
    
    //Saeedeh added:
    public Map<String, Map<String, Double>> getNetworkingEnergyConsumptionMap() {
        return networkingEnergyConsumptionMap;
    }
    
    public Map<String, Map<String, Double>> getNetworkingTuplesEnergyMap() {
        return networkingTuplesEnergyMap;
    }
    
  //Saeedeh added:
    public Map<String, Map<String, Double>> getNetworkingTimeMap() {
        return networkingTimeMap;
    }
    
    public Map<String, Map<String, Double>> getNetworkingTuplesTimeMap() {
        return networkingTuplesTimeMap;
    }
    // Saeedeh added for multiple apps support
    
    public Map<String, Map<String, Double>> getVmsTimeMap() {
        return VmsTimeMap;
    }
    
    public Map<String, Map<String, Double>> getVmsEnergyMap() {
        return VmsEnergyMap;
    }
    
    public Map<String, Map<String, Double>> getVmsCO2Map() {
        return VmsCO2Map;
    }
    
    public Map<String, Map<String, Double>> getNetworkingTuplesCO2Map() {
        return networkingTuplesCO2Map;
    }
    
    public Map<Vm, Double> getLastVmsMipsMap() {
        return lastVmsMipsMap;
    }
    //Saeedeh added:
    public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}
	
	public void setNetworkingMaxPower(double networkingMaxPower) {
		this.networkingMaxPower= networkingMaxPower;
	}

	public void setNetworkingIdlePower(double networkingIdlePower) {
		this.networkingIdlePower= networkingIdlePower;
	}

}