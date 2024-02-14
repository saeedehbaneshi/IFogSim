package org.fog.application.selectivity;

import java.util.Random;
import java.util.Scanner;
/**
 * Generates an output tuple for an incoming input tuple with a fixed probability
 * @author Harshit Gupta
 *
 */
public class FractionalSelectivity implements SelectivityModel{

	/**
	 * The fixed probability of output tuple creation per incoming input tuple
	 */
	double selectivity;
	
	Random generator = new Random(0);
	int counter= 0;
	
	public FractionalSelectivity(double selectivity){
		System.out.println("reversed_selectivity: "+1/selectivity);
		setSelectivity(selectivity);
	}
	public double getSelectivity() {
		return selectivity;
	}
	public void setSelectivity(double selectivity) {
		this.selectivity = selectivity;
	}
	
	
	
	/*
	 * This is their function for deciding about input of user interface module I (Saeedeh changed it to randomGenerator(0) function to remove the randomness)
	 * @Override
	public boolean canSelect() {
		if(Math.random() < getSelectivity()) // if the probability condition is satisfied
			return true;
		return false;
	}*/
	
	
	
	/*@Override
	public boolean canSelect() {
		if(randomGenerator() < getSelectivity()) // if the probability condition is satisfied
			return true;
		return false;
	}*/
	
	@Override
	public boolean canSelect() {
		/*if(selectivity==0.05) {
			System.out.println("reversed_selectivity: "+1/selectivity);
		}*/
		
		int reversed_selectivity=(int) (1/selectivity);
		if (reversed_selectivity==20) {
			reversed_selectivity=20;
		}
		counter++;
		if (counter%reversed_selectivity ==0 ) {	
			//if(selectivity<1) {
			//	System.out.println("reversed_selectivity: "+reversed_selectivity);
			//	System.out.println("counter: "+counter);
			//	Scanner in = new Scanner(System.in);
			//	String s=in.nextLine();
			//}
			if(reversed_selectivity==20) {
				counter=0;
			}
			counter=0;
			return true;
		}
		else
			return false;	
	}

	
	
	
	@Override
	public double getMeanRate() {
		return getSelectivity(); // the average rate of tuple generation is the fixed probability value
	}
	
	@Override
	public double getMaxRate() {
		return getSelectivity(); // the maximum rate of tuple generation is the fixed probability value
	}
	
	/*public double randomGenerator() {
	    //Random generator = new Random(seed);
	    double num = generator.nextDouble() * (0.5);
	    return num;
	}*/
	
	
}
