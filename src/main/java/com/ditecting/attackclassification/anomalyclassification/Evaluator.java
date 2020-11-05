package com.ditecting.attackclassification.anomalyclassification;

import com.ditecting.attackclassification.dataprocess.CSVUtil;

import java.io.IOException;
import java.util.*;

public class Evaluator {

    /**
     *
     * @param innerPath
     * @param testLabelIndex
     * @param outlierResultsPath
     */
	public static void evaluate(String innerPath, int testLabelIndex, String outlierResultsPath) throws IOException {
        List<Sample> innerSamples = new ArrayList<>();
	    if(innerPath != null){
            DataReader innerReader = new DataReader();
            innerReader.readData(innerPath, testLabelIndex);
            innerSamples.addAll(innerReader.getSamples());
        }
        List<String[]> outlierResults = CSVUtil.readMulti(outlierResultsPath, true);

        double num_total = innerSamples.size() + outlierResults.size();// total number of all testing data
        double num_total_P = 0;// total number of all testing attack data
        double num_TP = 0;// number of detected testing attack data
        double num_TN = 0;// number of detected testing attack data
        double num_total_T = 0;// total number of correctly classified testing data
        double num_new_cluster = 0;// number of newly created clusters
        /* calculate num_total_P, num_TP and num_total_T*/
        for (int a = 0; a < innerSamples.size(); a++) {
            if (Double.parseDouble(innerSamples.get(a).getLabel()) > 0) {
                num_total_P++;
            }else {
                num_TN++;
                num_total_T++;
            }
        }
        for (int b = 0; b < outlierResults.size(); b++) {
            if (Double.parseDouble(outlierResults.get(b)[1]) > 0) {
                num_total_P++;
                if (Double.parseDouble(outlierResults.get(b)[2]) > 0) {
                    num_TP++;
                }
            }else {
                if (Double.parseDouble(outlierResults.get(b)[2]) == 0) {
                    num_TN++;
                }
            }
        }
        /* create clusters according to new labels, and calculate num_new_cluster*/
        Map<String, List<String[]>> clusters = new HashMap<>();
        for (int c = 0; c < outlierResults.size(); c++) {
            String predicted_label = outlierResults.get(c)[2];
            if (clusters.containsKey(predicted_label)) {
                clusters.get(predicted_label).add(outlierResults.get(c));
            } else {
                List<String[]> newCluster = new ArrayList<>();
                newCluster.add(outlierResults.get(c));
                clusters.put(predicted_label, newCluster);
                if (!predicted_label.equals(0 + "")) {
                    num_new_cluster++;
                }
            }
        }
        /* map clusters to the real label of testing data*/
        Map<String, String> clusterLabels = new HashMap<>();
        for (Map.Entry<String, List<String[]>> cluster : clusters.entrySet()) {
            Map<String, Integer> labels = new HashMap<>();
            List<String[]> samples = cluster.getValue();
            for (int d = 0; d < samples.size(); d++) {
                String label = samples.get(d)[1];
                if (labels.containsKey(label)) {
                    labels.put(label, labels.get(label) + 1);
                } else {
                    labels.put(label, 1);
                }
            }
            String finalLabel = null;
            int maxVote = Integer.MIN_VALUE;
            for (Map.Entry<String, Integer> vote : labels.entrySet()) {
                if (vote.getValue() > maxVote) {
                    maxVote = vote.getValue();
                    finalLabel = vote.getKey();
                }
            }
            clusterLabels.put(cluster.getKey(), finalLabel);
        }
        /* calculate num_total_T according to labeled clusters*/
        for (Map.Entry<String, List<String[]>> cluster : clusters.entrySet()) {
            String label = clusterLabels.get(cluster.getKey());
            List<String[]> samples = cluster.getValue();
            for (String[] sample : samples) {
                if (sample[1].equals(label)) {
                    num_total_T++;
                }else {
//                    System.out.println("");
                }
            }
        }

        double detection_rate = num_TP / num_total_P;
        double accuracy = (num_TP+num_TN) / num_total;
        double classification_accuracy = num_total_T / num_total;
        double extension_rate = num_new_cluster / num_total_P;
        System.out.println("detection_rate: " + detection_rate);
        System.out.println("accuracy: " + accuracy);
        System.out.println("classification_accuracy: " + classification_accuracy);
        System.out.println("extension_rate: " + extension_rate);
    }

    /**
     *
     * @param trainingIndex [0, trainingIndex]
     * @param outlierResultsPath
     */
    public static void evaluate(int trainingIndex, String outlierResultsPath) throws IOException {
        List<String[]> outlierResults = CSVUtil.readMulti(outlierResultsPath, true);
        double num_total = outlierResults.size() - trainingIndex -1;// total number of all testing data
        double num_total_P = 0;// total number of all testing attack data
        double num_TP = 0;// number of detected testing attack data
        double num_TN = 0;// number of detected testing attack data
        double num_total_T = 0;// total number of correctly classified testing data
        /* create clusters according to new labels*/
        Map<String, List<String[]>> clusters = new HashMap<>();
        for (int c = 0; c < outlierResults.size(); c++) {
            String predicted_label = outlierResults.get(c)[2];
            if (clusters.containsKey(predicted_label)) {
                clusters.get(predicted_label).add(outlierResults.get(c));
            } else {
                List<String[]> newCluster = new ArrayList<>();
                newCluster.add(outlierResults.get(c));
                clusters.put(predicted_label, newCluster);
            }
        }
        /* map clusters to the real label of testing data*/
        Set<String> normalSet = new HashSet<>();
        Map<String, String> clusterLabels = new HashMap<>();
        for (Map.Entry<String, List<String[]>> cluster : clusters.entrySet()) {
            String finalLabel = null;
            List<String[]> samples = cluster.getValue();
            for(int e=0; e<samples.size(); e++){
                if(Double.parseDouble(samples.get(e)[0]) <= trainingIndex){
                    normalSet.add(cluster.getKey());
                    break;
                }
            }
            Map<String, Integer> labels = new HashMap<>();
            for (int d = 0; d < samples.size(); d++) {
                String label = samples.get(d)[1];
                if (labels.containsKey(label)) {
                    labels.put(label, labels.get(label) + 1);
                } else {
                    labels.put(label, 1);
                }
            }
            int maxVote = Integer.MIN_VALUE;
            for (Map.Entry<String, Integer> vote : labels.entrySet()) {
                if (vote.getValue() > maxVote) {
                    maxVote = vote.getValue();
                    finalLabel = vote.getKey();
                }
            }
            clusterLabels.put(cluster.getKey(), finalLabel);
        }

        /* calculate num_total_P, num_TP,num_TN */
        for(int f=trainingIndex+1; f<outlierResults.size(); f++){
            String clusterKey = outlierResults.get(f)[2];
            String clusterLabel = clusterLabels.get(clusterKey);
            if(Double.parseDouble(outlierResults.get(f)[1]) != 0){
                num_total_P++;
                if(!normalSet.contains(clusterKey)){
                    num_TP++;
                }
            }else if(normalSet.contains(clusterKey)){
                num_TN++;
            }
            if(Double.parseDouble(outlierResults.get(f)[1]) == Double.parseDouble(clusterLabel)){
                num_total_T++;
            }
        }

        double detection_rate = num_TP / num_total_P;
        double accuracy = (num_TP+num_TN) / num_total;
        double classification_accuracy = num_total_T / num_total;
        System.out.println("detection_rate: " + detection_rate);
        System.out.println("accuracy: " + accuracy);
        System.out.println("classification_accuracy: " + classification_accuracy);
    }
}
