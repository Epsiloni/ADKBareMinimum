package com.ADKBareMinimumAndroid;

public class Data {
	private char flag;
	private int reading;

	public Data(char flag, int reading) {
		this.flag = flag;
		this.reading = reading;
	}

	public int getReading() {
		return reading;
	}
	
	public char getFlag() {
		return flag;
	}
}