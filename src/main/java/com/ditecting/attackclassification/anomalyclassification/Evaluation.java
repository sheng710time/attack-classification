package com.ditecting.attackclassification.anomalyclassification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluation {
	private ArrayList<Sample> samples;
	
//	public Evaluation(ArrayList<Sample> samples){
//		this.samples = samples;
//	}
	
	public void precision(){
		int count = 0;
		for(Sample sample : samples) {
			if(sample.getPredictLabel() != null && 
					sample.getLabel().equals(sample.getPredictLabel())) {
				count ++;
			}
		}
		System.out.println("precison is "+count * 1.0 / samples.size());
	}

	public void evaluate(Map<Integer, Map<Integer, List<Object>>> clusterMap){
		int true_num = 0;
		int false_num = 0;
		double accuracy = 0;
		double TP = 0;//真正例
		double FN = 0;//假反例
		double FP = 0;//假正例
		double TN = 0;//真反例
		double TPFP = 0;//总正例
		double TNFN = 0;//总反例
		double precision = 0;//查准率
		double recall = 0;//查准率
		double F1 = 0;//F1
		int size = 0;
		int remainedSize = 0;

		int[] clustersSize = new int[clusterMap.size()];
		Map<Integer, Integer> labelSize  = new HashMap<>();

		int num = 0;
		 for(Map.Entry<Integer, Map<Integer, List<Object>>> cluster : clusterMap.entrySet()){
		 	for(Map.Entry<Integer, List<Object>> pointList : cluster.getValue().entrySet()){
				size= pointList.getValue().size();
				clustersSize[num] += size;
				if(labelSize.containsKey(pointList.getKey())){//包含label
					labelSize.put(pointList.getKey(), labelSize.get(pointList.getKey())+pointList.getValue().size());
				}else{
					labelSize.put(pointList.getKey(), pointList.getValue().size());
				}
				if(size>1){
					TP += size*(size-1)/2;
				}
			}
			 num++;
		 }
		 for(int a=0; a<clustersSize.length; a++){
			 TPFP += clustersSize[a]*(clustersSize[a]-1)/2;
			 for(int b=a+1; b<clustersSize.length; b++){
			 	TNFN += clustersSize[a]*clustersSize[b];
			 }
		 }
		 FP = TPFP - TP;

		for(Map.Entry<Integer, Map<Integer, List<Object>>> cluster : clusterMap.entrySet()){
			for(Map.Entry<Integer, List<Object>> pointList : cluster.getValue().entrySet()){
				size= pointList.getValue().size();
				remainedSize = labelSize.get(pointList.getKey()) - size;
				FN += size*remainedSize;
				labelSize.put(pointList.getKey(), remainedSize);
			}
		}
		TN = TNFN - FN;

		//计算precision
		if(TP + FP == 0){
			precision = 0;
		}else{
			precision = TP/(TP + FP);
		}
		//计算recall
		if(TP + FN == 0){
			recall = 0;
		}else{
			recall = TP/(TP + FN);
		}
		//计算F1
		if(precision + recall == 0){
			F1 = 0;
		}else{
			F1 = 2*precision*recall/(precision + recall);
		}

		System.out.println("precision : " + precision + " !");
		System.out.println("recall : " + recall + " !");
		System.out.println("F1 : " + F1 + " !");
	}
}
