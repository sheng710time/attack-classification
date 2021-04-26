package com.ditecting.attackclassification.competingmethods;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

import javax.swing.filechooser.FileSystemView;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/11/10 20:40
 */
public class EMClassification {
    public static void main(String[] args) throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePath = desktopPath + "\\experiment5\\exp4\\EM\\all_data_encode_14-6-label.csv";
        int classIndex = -1;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        Instances instancesTrain = FileLoader.loadInstancesFromCSV(trainFilePath,classIndex, includeHeader, options);

        int centerNum = 123;
        EM em = new EM();
        em.setMaxIterations(100);
        em.setNumClusters(centerNum);
        em.buildClusterer(instancesTrain);

        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(em);
        eval.evaluateClusterer(instancesTrain);
        double[] cnum = eval.getClusterAssignments();

        String labelFilePath = desktopPath + "\\experiment5\\exp4\\EM\\all_data_encode_14-6.csv";
        Instances instancesLabel = FileLoader.loadInstancesFromCSV(labelFilePath,0, includeHeader, options);
        List<String[]> output = new ArrayList<String[]>();
        output.add(new String[]{"flowNo", "data_class", "predicted_class"});
        for(int a=0; a<cnum.length; a++){
            output.add(new String[]{a+"", instancesLabel.get(a).classValue()+"", cnum[a]+""});
        }
        String outputPath = desktopPath + "\\experiment5\\exp4\\EM\\all_data_encode_14-6_result_EM_centerNum-"+ centerNum +".csv";
        CSVUtil.write(outputPath, output);

//        System.out.println("");
    }
}