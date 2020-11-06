package com.ditecting.attackclassification.anomalyclassification;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

import javax.swing.filechooser.FileSystemView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/11/5 8:28
 */
public class KmeansClassification {
    public static void main(String[] args) throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePath = desktopPath + "\\experiment3\\exp4\\Kmeans\\all_data-label.csv";
        int classIndex = -1;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        Instances instancesTrain = FileLoader.loadInstancesFromCSV(trainFilePath,classIndex, includeHeader, options);

        int centerNum = 232;
        SimpleKMeans KM = new SimpleKMeans();
        KM.setNumClusters(centerNum);
        KM.buildClusterer(instancesTrain);

        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(KM);
        eval.evaluateClusterer(instancesTrain);
        double[] cnum = eval.getClusterAssignments();

        String labelFilePath = desktopPath + "\\experiment3\\exp4\\Kmeans\\all_data.csv";
        Instances instancesLabel = FileLoader.loadInstancesFromCSV(labelFilePath,0, includeHeader, options);
        List<String[]> output = new ArrayList<String[]>();
        output.add(new String[]{"flowNo", "data_class", "predicted_class"});
        for(int a=0; a<cnum.length; a++){
            output.add(new String[]{a+"", instancesLabel.get(a).classValue()+"", cnum[a]+""});
        }
        String outputPath = desktopPath + "\\experiment3\\exp4\\Kmeans\\all_data_result_Kmeans_centerNum-"+ centerNum +".csv";
        CSVUtil.write(outputPath, output);

        System.out.println("");
    }

}