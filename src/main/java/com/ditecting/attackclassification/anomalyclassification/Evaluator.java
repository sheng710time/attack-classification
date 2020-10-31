package com.ditecting.attackclassification.anomalyclassification;

import com.ditecting.attackclassification.dataprocess.CSVUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluator {

    /**
     *
     * @param innerPath
     * @param outlierPath
     * @param testLabelIndex
     * @param outlierResultsPath
     */
	public static void evaluate(String innerPath, String outlierPath, int testLabelIndex, String outlierResultsPath) throws IOException {
        DataReader innerReader = new DataReader();
        innerReader.readData(innerPath, testLabelIndex);
        List<Sample> innerSamples = innerReader.getSamples();
        DataReader outlierReader = new DataReader();
        outlierReader.readData(outlierPath, testLabelIndex);
        List<Sample> outlierSamples = outlierReader.getSamples();
        List<String[]> outlierResults = CSVUtil.readMulti(outlierResultsPath, true);

        double num_total = innerSamples.size() + outlierSamples.size();// total number of all testing data
        double num_total_P = 0;// total number of all testing attack data
        double num_TP = 0;// number of detected testing attack data
        double num_TN = 0;// number of detected testing attack data
        double num_total_T = 0;// total number of correctly classified testing data
        double num_new_cluster = 0;// number of newly created clusters
        /* calculate num_total_P and num_TP*/
        for (int a = 0; a < innerSamples.size(); a++) {
            if (Double.parseDouble(innerSamples.get(a).getLabel()) > 0) {
                num_total_P++;
            }else {
                num_TN++;
            }
        }
        for (int b = 0; b < outlierSamples.size(); b++) {
            if (Double.parseDouble(outlierSamples.get(b).getLabel()) > 0) {
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
        Map<String, List<Sample>> clusters = new HashMap<>();
        for (int c = 0; c < outlierResults.size(); c++) {
            String predicted_label = outlierResults.get(c)[2];
            if (clusters.containsKey(predicted_label)) {
                clusters.get(predicted_label).add(outlierSamples.get(c));
            } else {
                List<Sample> newCluster = new ArrayList<>();
                newCluster.add(outlierSamples.get(c));
                clusters.put(predicted_label, newCluster);
                if (!predicted_label.equals(0 + "")) {
                    num_new_cluster++;
                }
            }
        }
        /* map clusters to the real label of testing data*/
        Map<String, String> clusterLabels = new HashMap<>();
        for (Map.Entry<String, List<Sample>> cluster : clusters.entrySet()) {
            Map<String, Integer> labels = new HashMap<>();
            List<Sample> samples = cluster.getValue();
            for (int d = 0; d < samples.size(); d++) {
                String label = samples.get(d).getLabel();
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
        for (Map.Entry<String, List<Sample>> cluster : clusters.entrySet()) {
            String label = clusterLabels.get(cluster.getKey());
            List<Sample> samples = cluster.getValue();
            for (Sample sample : samples) {
                if (sample.getLabel().equals(label)) {
                    num_total_T++;
                }
            }
        }

        double detection_rate = num_TP / num_total_P;
        double accuracy = (num_TP+num_TN) / num_total;
        double classification_accuracy = num_total_T / num_total;
        double extension_rate = num_new_cluster / num_total;
        System.out.println("detection_rate: " + detection_rate);
        System.out.println("accuracy: " + accuracy);
        System.out.println("classification_accuracy: " + classification_accuracy);
        System.out.println("extension_rate: " + extension_rate);
    }
}
