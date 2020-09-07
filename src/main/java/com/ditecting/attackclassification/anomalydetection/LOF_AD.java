package com.ditecting.attackclassification.anomalydetection;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import lombok.extern.slf4j.Slf4j;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.neighboursearch.LinearNNSearch;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.LOF;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/13 21:49
 */
@Slf4j
public class LOF_AD {
    private Normalize normalize;
    private Standardize standardize;
    private LOF lof;
    private Instances trainingData;
    private Instances testingData;
    private Instances predictedData;
    private boolean isTrained;
    private boolean isTested;
    private int mode; // 0: none, 1: normalize, 2: standardize
    private List<Instance> innerTrainingData;
    private List<Instance> outlierTrainingData;

    public LOF_AD() {
        this(2);
    }

    public LOF_AD(int mode) {
        normalize = new Normalize();
        standardize = new Standardize();
        lof = new LOF();
        isTrained = false;
        isTested = false;
        this.mode = mode;
    }

    public void train (String trainFilePath, int min, int max, int classIndex, boolean includeHeader, String[] options) throws Exception {
        /* normalize training data*/
        Instances instancesTrain = FileLoader.loadInstancesFromCSV(trainFilePath,classIndex, includeHeader, options);
        switch (mode){
            case 0:
                trainingData = instancesTrain;break;
            case 1:
                normalize.setInputFormat(instancesTrain);
                trainingData = Filter.useFilter(instancesTrain, normalize);break;
            case 2:
                standardize.setInputFormat(instancesTrain);
                trainingData = Filter.useFilter(instancesTrain, standardize);break;
        }

        EuclideanDistanceOverZero edoz = new EuclideanDistanceOverZero();
        LinearNNSearch linearNNSearch = new LinearNNSearch();
        linearNNSearch.setDistanceFunction(edoz);
        lof.setInputFormat(trainingData);
        lof.setOptions(new String[]{"-min", min+"", "-max", max+""});
        lof.setNNSearch(linearNNSearch);
        for(Instance inst : trainingData){
            lof.input(inst);
        }
        lof.batchFinished();

        isTrained = true;
    }

    public double evaluateTrainingData (double cutOffValue, int KNN) throws Exception {
        if(!isTrained){
            throw new IllegalStateException("Lof has not been trained.");
        }
        predictedData = Filter.useFilter(trainingData, lof);

        /* Find normal instances */
        innerTrainingData = new ArrayList<>();
        outlierTrainingData = new ArrayList<>();
        for(Instance inst : predictedData){
            int preLabel = inst.value(inst.numAttributes()-1) < cutOffValue ? 0:1;
            if(preLabel == 0){
                innerTrainingData.add(inst);
            }else {
                outlierTrainingData.add(inst);
            }
        }

        /* Calculate KNN distances */
        predictedData.deleteAttributeAt(predictedData.numAttributes()-1);
        double[] knnDistances = new double[innerTrainingData.size()];

        EuclideanDistanceOverZero edoz = new EuclideanDistanceOverZero();
        LinearNNSearch lnn = new LinearNNSearch();
        lnn.setDistanceFunction(edoz);
        lnn.setInstances(trainingData);
        int nnFactor = 2;
        double totalKnnDistance = 0;
        for(int a=0; a<innerTrainingData.size(); a++){
            Instances instances = lnn.kNearestNeighbours(innerTrainingData.get(a), (KNN+1) * nnFactor);
            double[] distances = lnn.getDistances();
            int indexOfKDistanceForK;
            for(indexOfKDistanceForK = KNN - 1; indexOfKDistanceForK < distances.length - 1 && distances[indexOfKDistanceForK] == distances[indexOfKDistanceForK + 1]; ++indexOfKDistanceForK) {
                ;
            }
            knnDistances[a] = distances[indexOfKDistanceForK];
            totalKnnDistance += distances[indexOfKDistanceForK];
        }

        double avgKnnDistance = totalKnnDistance / innerTrainingData.size();
        return avgKnnDistance;
    }

    public void test (String testFilePath, int classIndex, boolean includeHeader, String[] options) throws Exception {
        if(!isTrained){
            throw new IllegalStateException("Lof has not been trained.");
        }

        Instances instancesTest = FileLoader.loadInstancesFromCSV(testFilePath, classIndex, includeHeader, options);
        switch (mode){
            case 0:
                testingData = instancesTest;break;
            case 1:
                testingData = Filter.useFilter(instancesTest, normalize);break;
            case 2:
                testingData = Filter.useFilter(instancesTest, standardize);break;
        }

        predictedData = Filter.useFilter(testingData, lof);

        isTested = true;
    }

    public double evaluate (String testFilePathNo, String testFilePathLabel, double cutOffValue) throws Exception {
        if(!isTested){
            throw new IllegalStateException("No testing data has been predicted.");
        }

        Map<Integer, List<Integer>> numbersMap = FileLoader.loadNumbersFromCSV(testFilePathNo);
        Map<Integer, Integer> labels = FileLoader.loadLabelsFromCSV(testFilePathLabel);

        int total = 0;
        int TP = 0;
        int FP = 0;
        int TN = 0;
        int FN = 0;

        for( int a=0; a<predictedData.size(); a++){
            List<Integer> numbers = numbersMap.get(a);
            for(int number : numbers){
                total++;
                int realLabel = labels.get(number);
                int preLabel = predictedData.get(a).value(predictedData.get(a).numAttributes()-1) < cutOffValue ? 0:1;

                if(realLabel == 1){
                    if(preLabel == 1){
                        TP++;
                    }else {
                        FN++;
                    }
                }else {
                    if(preLabel == 1){
                        FP++;
                    }else {
                        TN++;
                    }
                }
            }
        }

        double accuracy = ((double)TP+TN)/total;
        double detection_rate = -1;
        if(TP+FN > 0){
            detection_rate = ((double)TP)/(TP+FN);
        }
        System.out.println("total: " + total);
        System.out.println("TP: " + TP);
        System.out.println("TN: " + TN);
        System.out.println("FP: " + FP);
        System.out.println("FN: " + FN);
        System.out.println("accuracy: " + accuracy);
        System.out.println("detection_rate: " + detection_rate);
        return accuracy;
    }

    public void output (String testFilePathNo, String testFilePathLabel, String outPathResult, double cutOffValue) throws Exception {
        if(!isTested){
            throw new IllegalStateException("No testing data has been predicted.");
        }

        Map<Integer, List<Integer>> numbersMap = FileLoader.loadNumbersFromCSV(testFilePathNo);
        Map<Integer, Integer> labels = FileLoader.loadLabelsFromCSV(testFilePathLabel);

        List<String[]> resultsList = new ArrayList<>();
        resultsList.add(new String[]{"flowNo", "packetNo", "data_class", "predicted_class", "lof_score"});
        for( int a=0; a<predictedData.size(); a++){
            List<Integer> numbers = numbersMap.get(a);
            for(int number : numbers){
                int preLabel = predictedData.get(a).value(predictedData.get(a).numAttributes()-1) < cutOffValue ? 0:1;
                String[] result = new String[]{a+"", number+"", labels.get(number)+"", preLabel+"", predictedData.get(a).value(predictedData.get(a).numAttributes()-1)+""};
                resultsList.add(result);
            }
        }
        CSVUtil.write(outPathResult, resultsList);
    }

    /**
     * Save the trained LOF model
     * @param filePath
     * @throws Exception
     */
    public void saveLOF (String filePath) throws Exception {//TODO test
        if(!isTrained){
            throw new IllegalStateException("Lof has not been trained.");
        }

        File file = new File(filePath);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        if(!file.exists()){
            file.createNewFile();
        }

//        SerializationHelper.write(filePath, lof);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(lof);
        oos.flush();
        oos.close();

        log.info("LOF model has been saved.");
    }

    /**
     * Reads the existing LOF model
     * @param filePath
     * @return
     * @throws Exception
     */
    public static LOF readLOF (String filePath) throws Exception {//TODO test
        File file = new File(filePath);
        if(!file.exists()){
            throw new NullPointerException(filePath + " does not exist.");
        }

        LOF lof = (LOF) SerializationHelper.read(filePath);
        return lof;
    }

    public static Instances test (LOF lof, String testFilePath, int classIndex, boolean includeHeader, String[] options) throws Exception {//TODO test
        if(lof == null){
            throw new NullPointerException("lof is null.");
        }

        //Load data
        Instances testingData = FileLoader.loadInstancesFromCSV(testFilePath, classIndex, includeHeader, options);
        //Predict data
        Instances predictedData = Filter.useFilter(testingData, lof);

        return predictedData;
    }

    public static double evaluate (Instances predictedData, String testFilePathNo, String testFilePathLabel, double cutOffValue) throws Exception {//TODO test
        Map<Integer, List<Integer>> numbersMap = FileLoader.loadNumbersFromCSV(testFilePathNo);
        Map<Integer, Integer> labels = FileLoader.loadLabelsFromCSV(testFilePathLabel);

        int total = 0;
        int TP = 0;
        int FP = 0;
        int TN = 0;
        int FN = 0;

        for( int a=0; a<predictedData.size(); a++){
            List<Integer> numbers = numbersMap.get(a);
            for(int number : numbers){
                total++;
                int realLabel = labels.get(number);
                int preLabel = predictedData.get(a).value(predictedData.get(a).numAttributes()-1) < cutOffValue ? 0:1;

                if(realLabel == 1){
                    if(preLabel == 1){
                        TP++;
                    }else {
                        FN++;
                    }
                }else {
                    if(preLabel == 1){
                        FP++;
                    }else {
                        TN++;
                    }
                }
            }
        }

        double accuracy = ((double)TP+TN)/total;
        double detection_rate = -1;
        if(TP+FN > 0){
            detection_rate = ((double)TP)/(TP+FN);
        }
        System.out.println("total: " + total);
        System.out.println("TP: " + TP);
        System.out.println("TN: " + TN);
        System.out.println("FP: " + FP);
        System.out.println("FN: " + FN);
        System.out.println("accuracy: " + accuracy);
        System.out.println("detection_rate: " + detection_rate);
        return accuracy;
    }

}