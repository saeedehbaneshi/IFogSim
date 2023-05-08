package org.fog.entities;

import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

public class FogBroker extends PowerDatacenterBroker{

	public FogBroker(String name) throws Exception {
		super(name);
		//System.out.println("FogBroker constructor called");
		
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startEntity() {
		System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHEEEEEEEEEEEEEEEEEEEEEEEEEEEELLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLOOOOOOOOOOOOOOOOOOO");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processEvent(SimEvent ev) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdownEntity() {
		// TODO Auto-generated method stub
		
	}
	
	

}
