package org.fog.utils;

public class Config {

	public static final double RESOURCE_MGMT_INTERVAL = 150;
	public static int MAX_SIMULATION_TIME = 4000;
	public static int RESOURCE_MANAGE_INTERVAL = 100;
	public static String FOG_DEVICE_ARCH = "x86";
	public static String FOG_DEVICE_OS = "Linux";
	public static String FOG_DEVICE_VMM = "Xen";
	public static double FOG_DEVICE_TIMEZONE = 10.0;
	public static double FOG_DEVICE_COST = 3.0;
	public static double FOG_DEVICE_COST_PER_MEMORY = 0.05;
	public static double FOG_DEVICE_COST_PER_STORAGE = 0.001;
	public static double FOG_DEVICE_COST_PER_BW = 0.0;
	public static double MAX_VALUE = 1000000.0;
	//Saeedeh added this coefficient for links power
	public static int LINK_POWER = 100;

	// Create cluster among devices of same level with common parent irrespective of location. Only one of the two clustering modes should be used for clustering
	public static boolean ENABLE_STATIC_CLUSTERING = false;
	//Dynamic Clustering
	public static boolean ENABLE_DYNAMIC_CLUSTERING = true;
	public static double Node_Communication_RANGE = 300.0; // In terms of meter
	public static double clusteringLatency = 2.0; //milisecond

	public static final int TRANSMISSION_START_DELAY = 50;
	
	//Saeedeh considered these for printLinksEnergyDetailsCombinationWifiWired() function based on ns3:
	public static double WiFi_Idle_Power = 0.82;
	public static double Wifi_Transmitter_Power = 1.14;
	public static double Wifi_Reciever_Power = 0.94;
	
	public static double Wired_Min_Power = 100;
	public static double Wired_Max_Power = 200;
	
	//Saeedeh considered these for printLinksEnergyDetailsSimpleEcofenBased() function based on Measured Idle and max power of network devices in P.Mahadevan et al paper
	public static double LowEnd_Hub_A_Measured_Idle_Power = 11.7;
	public static double LowEnd_Hub_A_Measured_Max_Power = 12.8;
	public static double LowEnd_Hub_A_Rated_Max_Power = 35;
	
	public static double Edge_Lan_Switch_B_Measured_Idle_Power = 150;
	public static double Edge_Lan_Switch_B_Measured_Max_Power = 198;
	public static double Edge_Lan_Switch_B_Rated_Max_Power = 759;
	
	public static double Edge_Lan_Switch_C_Measured_Idle_Power = 133.5;
	public static double Edge_Lan_Switch_C_Measured_Max_Power = 175;
	public static double Edge_Lan_Switch_C_Rated_Max_Power = 875;
	
	public static double Edge_Lan_Switch_D_Measured_Idle_Power = 76.4;
	public static double Edge_Lan_Switch_D_Measured_Max_Power = 102;
	public static double Edge_Lan_Switch_D_Rated_Max_Power = 300;
	
	public static double Core_Switch_E_Measured_Idle_Power = 555;
	public static double Core_Switch_E_Measured_Max_Power = 656;
	public static double Core_Switch_E_Rated_Max_Power = 3000;
	
	//Saeedeh added for flow-based networking model:
	public static double CPE_Max_Power_Fast_Ethernet_Gateway = 4.6;
	public static double CPE_Idle_Power_Fast_Ethernet_Gateway = 2.8;
	
	public static double Shared_Access_Point_EthernetSwitch_Max_Power = 1766;
	public static double Shared_Access_Point_EthernetSwitch_Idle_Power = 1589;
	
	public static double Shared_Edge_Equipment_Max_Power = 4550;
	public static double Shared_Edge_Equipment_Idle_Power = 4095;
	
	public static double Shared_Core_Equipment_Max_Power = 12300;
	public static double Shared_Core_Equipment_Idle_Power = 11070;
	
	//Saeedeh added for flow-based networking model:
	/*public static double CPE_Max_Power_Fast_Ethernet_Gateway = 10;
	public static double CPE_Idle_Power_Fast_Ethernet_Gateway = 9;
	
	public static double Shared_Access_Point_EthernetSwitch_Max_Power = 10;
	public static double Shared_Access_Point_EthernetSwitch_Idle_Power = 9;
	
	public static double Shared_Edge_Equipment_Max_Power = 10;
	public static double Shared_Edge_Equipment_Idle_Power = 9;
	
	public static double Shared_Core_Equipment_Max_Power = 10;
	public static double Shared_Core_Equipment_Idle_Power = 9;*/



	

	
	
	
}
