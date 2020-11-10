package com.ditecting.attackclassification.competingmethods;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Instances;

import javax.swing.filechooser.FileSystemView;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/11/10 16:30
 */
public class HCClassification {

    public static void  main(String[] args) throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePath = desktopPath + "\\experiment5\\exp4\\HC\\all_data-label.csv";
        int classIndex = -1;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        Instances instancesTrain = FileLoader.loadInstancesFromCSV(trainFilePath,classIndex, includeHeader, options);

        int clusterNum = 42;
        HierarchicalClusterer HC = new HierarchicalClusterer();
        HC.setNumClusters(clusterNum);
        HC.buildClusterer(instancesTrain);

        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(HC);
        eval.evaluateClusterer(instancesTrain);
        double[] cnum = eval.getClusterAssignments();

        String labelFilePath = desktopPath + "\\experiment5\\exp4\\HC\\all_data.csv";
        Instances instancesLabel = FileLoader.loadInstancesFromCSV(labelFilePath,0, includeHeader, options);
        List<String[]> output = new ArrayList<String[]>();
        output.add(new String[]{"flowNo", "data_class", "predicted_class"});
        for(int a=0; a<cnum.length; a++){
            output.add(new String[]{a+"", instancesLabel.get(a).classValue()+"", cnum[a]+""});
        }
        String outputPath = desktopPath + "\\experiment5\\exp4\\HC\\all_data_result_HC_clusterNum-"+ clusterNum +".csv";
        CSVUtil.write(outputPath, output);

//        System.out.println("");
    }
}