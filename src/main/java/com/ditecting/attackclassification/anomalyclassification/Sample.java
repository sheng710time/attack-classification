package com.ditecting.attackclassification.anomalyclassification;

import java.io.Serializable;

public class Sample implements Serializable{
    private static final long serialVersionUID = -4313720880601207893L;
    private double[] attributes;
	private String label;
	private String predictLabel;
	public Sample(double[] attributes, String label) {
		this.attributes = attributes;
		this.label = label;
	}
	public double[] getAttributes() {
		return attributes;
	}
	public String getLabel() {
		return label;
	}
	public String getPredictLabel() {
		return predictLabel;
	}
	public void setPredictLabel(String predictLabel) {
		this.predictLabel = predictLabel;
	}
	
}
