package org.fog.utils.distribution;

public class DeterministicDistribution extends Distribution{

	private double value;

	public DeterministicDistribution(double value) {
		super();
		setValue(value);
	}
	
	@Override
	public double getNextValue() {
		//System.out.println("@@ From deterministic distribution it is next value = "+value);
		return value;
	}
	
	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public int getDistributionType() {
		return Distribution.DETERMINISTIC;
	}

	@Override
	public double getMeanInterTransmitTime() {
		return value;
	}
	
}
