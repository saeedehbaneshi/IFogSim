/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 * 
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;

/**
 * VmSchedulerTimeShared is a VMM allocation policy that allocates one or more Pe to a VM, and
 * allows sharing of PEs by multiple VMs. This class also implements 10% performance degration due
 * to VM migration. This scheduler does not support over-subscription.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class VmSchedulerTimeShared extends VmScheduler {

	/** The mips map requested. */
	private Map<String, List<Double>> mipsMapRequested;

	/** The pes in use. */
	private int pesInUse;

	/**
	 * Instantiates a new vm scheduler time shared.
	 * 
	 * @param pelist the pelist
	 */
	public VmSchedulerTimeShared(List<? extends Pe> pelist) {
		super(pelist);
		setMipsMapRequested(new HashMap<String, List<Double>>());
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.VmScheduler#allocatePesForVm(cloudsim.Vm, java.util.List)
	 */
	@Override
	public boolean allocatePesForVm(Vm vm, List<Double> mipsShareRequested) {
		//saeedeh//System.out.println("##########################$$$$$$$$$$$$$$$$$$$$$$$ Vm Scheduler TIME share (allocate Pe for Vm function)$$$$$$$$$$$$$$$$$$$$#################");

		/**
		 * TODO: add the same to RAM and BW provisioners
		 */
		if (vm.isInMigration()) {
			if (!getVmsMigratingIn().contains(vm.getUid()) && !getVmsMigratingOut().contains(vm.getUid())) {
				getVmsMigratingOut().add(vm.getUid());
			}
		} else {
			if (getVmsMigratingOut().contains(vm.getUid())) {
				getVmsMigratingOut().remove(vm.getUid());
			}
		}
		//sshh//System.out.println(" vmUidd = "+vm.getUid());
		//saeedeh//System.out.println(" mipsShareRequested(list) = "+mipsShareRequested);
		/*Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        System.out.println("next entity...");
        String userName = myObj.nextLine();*/

		boolean result = allocatePesForVm(vm.getUid(), mipsShareRequested);
		//saeedeh//System.out.println("this is boolean result: "+result);
		updatePeProvisioning();
		//saeedeh//System.out.println("##########################$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$#################");
		return result;
	}

	/**
	 * Allocate pes for vm.
	 * 
	 * @param vmUid the vm uid
	 * @param mipsShareRequested the mips share requested
	 * @return true, if successful
	 */
	protected boolean allocatePesForVm(String vmUid, List<Double> mipsShareRequested) {
		/*System.out.println(" vvmUid = "+vmUid);
		System.out.println(" mipsShareRequested(list) = "+mipsShareRequested);
		Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        System.out.println("next entity...");
        String userName = myObj.nextLine();*/

		double totalRequestedMips = 0;
		double peMips = getPeCapacity();
		
		//saeedeh//System.out.println(" peMips = "+peMips);

		for (Double mips : mipsShareRequested) {
			// each virtual PE of a VM must require not more than the capacity of a physical PE
			if (mips > peMips) {
				return false;
			}
			totalRequestedMips += mips;
		}
		//saeedeh//System.out.println(" totalRequestedMips(sumation of all mips numbers in mipsShareRequested list) = "+totalRequestedMips);

		// This scheduler does not allow over-subscription
		if (getAvailableMips() < totalRequestedMips) {
			return false;
		}

		getMipsMapRequested().put(vmUid, mipsShareRequested);
		setPesInUse(getPesInUse() + mipsShareRequested.size());

		if (getVmsMigratingIn().contains(vmUid)) {
			// the destination host only experience 10% of the migrating VM's MIPS
			totalRequestedMips *= 0.1;
		}

		List<Double> mipsShareAllocated = new ArrayList<Double>();
		for (Double mipsRequested : mipsShareRequested) {
			if (getVmsMigratingOut().contains(vmUid)) {
				// performance degradation due to migration = 10% MIPS
				mipsRequested *= 0.9;
			} else if (getVmsMigratingIn().contains(vmUid)) {
				// the destination host only experience 10% of the migrating VM's MIPS
				mipsRequested *= 0.1;
			}
			mipsShareAllocated.add(mipsRequested);
			//saeedeh//System.out.println(" mipsShareAllocated = "+mipsShareAllocated);

		}

		getMipsMap().put(vmUid, mipsShareAllocated);
		//sshh//System.out.println(" MipsMap = "+getMipsMap());

		setAvailableMips(getAvailableMips() - totalRequestedMips);
		//sshh//System.out.println(" AvailableMips = "+getAvailableMips());

		return true;
	}

	/**
	 * Update allocation of VMs on PEs.
	 */
	protected void updatePeProvisioning() {
		//saeedeh//System.out.println("++++++++++++++++++++++++++++++++++ Update pe provisioner function in vm scheduler time share file s++++++++++++++++++++++++++++++++++" );
		getPeMap().clear();
		//saeedeh//System.out.println(" Pe list : "+getPeList());
		for (Pe pe : getPeList()) {
			pe.getPeProvisioner().deallocateMipsForAllVms();
		}

		Iterator<Pe> peIterator = getPeList().iterator();
		Pe pe = peIterator.next();
		PeProvisioner peProvisioner = pe.getPeProvisioner();
		double availableMips = peProvisioner.getAvailableMips();
		//saeedeh//System.out.println("(From updatePeProvisioning function) availableMips1 (update pe provisioner)= "+availableMips);

		for (Map.Entry<String, List<Double>> entry : getMipsMap().entrySet()) {
			String vmUid = entry.getKey();
			getPeMap().put(vmUid, new LinkedList<Pe>());

			for (double mips : entry.getValue()) {
			//saeedeh//System.out.println(" mips: "+mips);
				while (mips >= 0.1) {
					if (availableMips >= mips) {
						peProvisioner.allocateMipsForVm(vmUid, mips);
						getPeMap().get(vmUid).add(pe);
						availableMips -= mips;
						//saeedeh//System.out.println("available mips in if condition(available mips>= mips)"+availableMips);
						break;
					} else {
						peProvisioner.allocateMipsForVm(vmUid, availableMips);
						getPeMap().get(vmUid).add(pe);
						mips -= availableMips;
						if (mips <= 0.1) {
							break;
						}
						if (!peIterator.hasNext()) {
							Log.printLine("There is no enough MIPS (" + mips + ") to accommodate VM " + vmUid);
							System.err.println("There is no enough MIPS (" + mips + ") to accommodate VM " + vmUid);
						}
						pe = peIterator.next();
						peProvisioner = pe.getPeProvisioner();
						availableMips = peProvisioner.getAvailableMips();
						//saeedeh//System.out.println(" availableMips in else condition = "+availableMips);

					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.VmScheduler#deallocatePesForVm(cloudsim.Vm)
	 */
	@Override
	public void deallocatePesForVm(Vm vm) {
		getMipsMapRequested().remove(vm.getUid());
		setPesInUse(0);
		getMipsMap().clear();
		setAvailableMips(PeList.getTotalMips(getPeList()));

		for (Pe pe : getPeList()) {
			pe.getPeProvisioner().deallocateMipsForVm(vm);
		}
		//saeedeh//System.out.println(" Entry set of mips map requested: "+getMipsMapRequested().entrySet());
		for (Map.Entry<String, List<Double>> entry : getMipsMapRequested().entrySet()) {
			allocatePesForVm(entry.getKey(), entry.getValue());
		}

		updatePeProvisioning();
	}

	/**
	 * Releases PEs allocated to all the VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	@Override
	public void deallocatePesForAllVms() {
		super.deallocatePesForAllVms();
		getMipsMapRequested().clear();
		setPesInUse(0);
	}

	/**
	 * Returns maximum available MIPS among all the PEs. For the time shared policy it is just all
	 * the avaiable MIPS.
	 * 
	 * @return max mips
	 */
	@Override
	public double getMaxAvailableMips() {
		return getAvailableMips();
	}

	/**
	 * Sets the pes in use.
	 * 
	 * @param pesInUse the new pes in use
	 */
	protected void setPesInUse(int pesInUse) {
		this.pesInUse = pesInUse;
	}

	/**
	 * Gets the pes in use.
	 * 
	 * @return the pes in use
	 */
	protected int getPesInUse() {
		return pesInUse;
	}

	/**
	 * Gets the mips map requested.
	 * 
	 * @return the mips map requested
	 */
	protected Map<String, List<Double>> getMipsMapRequested() {
		return mipsMapRequested;
	}

	/**
	 * Sets the mips map requested.
	 * 
	 * @param mipsMapRequested the mips map requested
	 */
	protected void setMipsMapRequested(Map<String, List<Double>> mipsMapRequested) {
		this.mipsMapRequested = mipsMapRequested;
	}

}
