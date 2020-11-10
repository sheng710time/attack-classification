package com.ditecting.attackclassification.competingmethods;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.DBSCAN;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

import javax.swing.filechooser.FileSystemView;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/11/5 10:56
 */
public class DBSCANClassification {
    public static void main(String[] args) throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePath = desktopPath + "\\experiment5\\exp4\\DBSCAN\\all_data-label.csv";
        int classIndex = -1;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        Instances instancesTrain = FileLoader.loadInstancesFromCSV(trainFilePath,classIndex, includeHeader, options);

        double eps = 0.2;
        int minps = 2;
        DBSCAN dbscan = new DBSCAN();
        dbscan.setEpsilon(eps);
        dbscan.setMinPoints(minps);
        dbscan.buildClusterer(instancesTrain);

        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(dbscan);
        eval.evaluateClusterer(instancesTrain);
        double[] cnum = eval.getClusterAssignments();

        String labelFilePath = desktopPath + "\\experiment5\\exp4\\DBSCAN\\all_data.csv";
        Instances instancesLabel = FileLoader.loadInstancesFromCSV(labelFilePath,0, includeHeader, options);
        List<String[]> output = new ArrayList<String[]>();
        output.add(new String[]{"flowNo", "data_class", "predicted_class"});
        for(int a=0; a<cnum.length; a++){
            output.add(new String[]{a+"", instancesLabel.get(a).classValue()+"", cnum[a]+""});
        }
        String outputPath = desktopPath + "\\experiment5\\exp4\\DBSCAN\\all_data_result_DBSCAN_eps-"+ eps +"_minps-"+ minps +".csv";
        CSVUtil.write(outputPath, output);

        System.out.println("");
    }
}