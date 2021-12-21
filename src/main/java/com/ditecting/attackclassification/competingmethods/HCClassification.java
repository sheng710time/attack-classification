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
        System.out.println("HCClassification");
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePath = desktopPath + "\\experiment5\\exp5\\HC\\0 org\\all_data_encode_14-6-label.csv";
        int classIndex = 7;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "1-6", "-N", "7"};
        Instances instancesTrain = FileLoader.loadInstancesFromCSV(trainFilePath,classIndex, includeHeader, options);

        int clusterNum = 47;
        HierarchicalClusterer HC = new HierarchicalClusterer();
        HC.setNumClusters(clusterNum);
        HC.buildClusterer(instancesTrain);

        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(HC);
        eval.evaluateClusterer(instancesTrain);
        double[] cnum = eval.getClusterAssignments();

        String labelFilePath = desktopPath + "\\experiment5\\exp5\\HC\\0 org\\all_data_encode_14-6-label.csv";
        Instances instancesLabel = FileLoader.loadInstancesFromCSV(labelFilePath,7, includeHeader, options);
        List<String[]> output = new ArrayList<String[]>();
        output.add(new String[]{"flowNo", "data_class", "predicted_class"});
        for(int a=0; a<cnum.length; a++){
            output.add(new String[]{a+"", instancesLabel.get(a).classValue()+"", cnum[a]+""});
        }
        String outputPath = desktopPath + "\\experiment5\\exp5\\HC\\0 org\\all_data_encode_14-6_result_HC_clusterNum-"+ clusterNum +".csv";
        CSVUtil.write(outputPath, output);

        System.out.println("");
    }
}