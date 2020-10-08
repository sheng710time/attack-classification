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
import java.math.BigDecimal;
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
        edoz.setDontNormalize(true);
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

    /**
     * compare value with cutOffValue, if smaller return true, else return false
     * @param including whether including "equal to" cutOffValue
     * @param value
     * @param cutOffValue
     * @return
     */
    public boolean compareCutOffValue (boolean including, double value, double cutOffValue) {
        if(including){
            return value <= cutOffValue;
        }else {
            return value < cutOffValue;
        }
    }

    public double evaluateTrainingData (double cutOffValue, int KNN, boolean including) throws Exception {
        if(!isTrained){
            throw new IllegalStateException("Lof has not been trained.");
        }
        predictedData = Filter.useFilter(trainingData, lof);

        /* Find normal instances */
        innerTrainingData = new ArrayList<>();
        outlierTrainingData = new ArrayList<>();
        for(Instance inst : predictedData){
            double lof = inst.value(inst.numAttributes()-1);
            double lofFormatted = new BigDecimal(lof).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            int preLabel = compareCutOffValue(including, lofFormatted, cutOffValue) ? 0:1;
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
        edoz.setDontNormalize(true);
        LinearNNSearch lnn = new LinearNNSearch();
        lnn.setDistanceFunction(edoz);
        lnn.setInstances(trainingData);
        int nnFactor = 2;
        double totalKnnDistance = 0;
        double minKnnDistance = Double.MAX_VALUE;
        double maxKnnDistance = Double.MIN_VALUE;
        for(int a=0; a<innerTrainingData.size(); a++){
            Instances instances = lnn.kNearestNeighbours(innerTrainingData.get(a), (KNN+1) * nnFactor);
            double[] distances = lnn.getDistances();
            int indexOfKDistanceForK;
            for(indexOfKDistanceForK = KNN - 1; indexOfKDistanceForK < distances.length - 1 && distances[indexOfKDistanceForK] == distances[indexOfKDistanceForK + 1]; ++indexOfKDistanceForK) {
                ;
            }
            knnDistances[a] = distances[indexOfKDistanceForK];
            totalKnnDistance += distances[indexOfKDistanceForK];
            System.out.println(distances[indexOfKDistanceForK]);
            if(minKnnDistance > distances[indexOfKDistanceForK]){
                minKnnDistance = distances[indexOfKDistanceForK];
            }
            if(maxKnnDistance < distances[indexOfKDistanceForK]){
                maxKnnDistance = distances[indexOfKDistanceForK];
            }
        }

        double avgKnnDistance = totalKnnDistance / innerTrainingData.size();
        System.out.println("cutOffValue: " + cutOffValue);
        System.out.println("innerNum: " + innerTrainingData.size());
        System.out.println("outerNum: " + outlierTrainingData.size());
        System.out.println("avgKnnDistance: " + avgKnnDistance);
        System.out.println("minKnnDistance: " + minKnnDistance);
        System.out.println("maxKnnDistance: " + maxKnnDistance);
        return avgKnnDistance;
    }

    public void outputOutliers (String outPathOutliers) {
        if(outlierTrainingData==null || outlierTrainingData.size()<1){
            return;
        }
        /* Output data to csv file */
        int numAttributes = outlierTrainingData.get(0).numAttributes();
        String[] header = new String[numAttributes];
        for(int a=0; a<numAttributes; a++){
            header[a] = "attr" + (a+1);
        }

        List<String[]> strDataList = new ArrayList<>();
        strDataList.add(header);
        for(int b=0; b<outlierTrainingData.size(); b++) {
            Instance instance = outlierTrainingData.get(b);
            String[] data = new String[numAttributes];
            for (int d = 0; d < numAttributes; d++) {
                data[d] = instance.toDoubleArray()[d] + "";
            }
            strDataList.add(data);
        }
        CSVUtil.write(outPathOutliers, strDataList);
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

    public double evaluate (double cutOffValue) {
        if(!isTested){
            throw new IllegalStateException("No testing data has been predicted.");
        }

        int total = predictedData.size();
        int TP = 0;
        int FP = 0;
        int TN = 0;
        int FN = 0;

        for( int a=0; a<predictedData.size(); a++){
            int realLabel = (int) predictedData.get(a).value(predictedData.classIndex());
            double lof = predictedData.get(a).value(predictedData.numAttributes()-1);
            double lofFormatted = new BigDecimal(lof).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            int preLabel = compareCutOffValue(true, lofFormatted, cutOffValue)? 0:1;

            if(realLabel == 0){
                if(preLabel == 0){
                    TN++;
                }else {
                    FP++;
                }
            }else {
                if(preLabel == 0){
                    FN++;
                }else {
                    TP++;
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

    public void output (String outPathResult, double cutOffValue){
        if(!isTested){
            throw new IllegalStateException("No testing data has been predicted.");
        }

        List<String[]> resultsList = new ArrayList<>();
        resultsList.add(new String[]{"flowNo", "data_class", "predicted_class", "lof_score"});
        for( int a=0; a<predictedData.size(); a++){
            int realLabel = (int) predictedData.get(a).value(predictedData.classIndex());
            double lof = predictedData.get(a).value(predictedData.numAttributes()-1);
            double lofFormatted = new BigDecimal(lof).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            int preLabel = compareCutOffValue(true, lofFormatted, cutOffValue)? 0:1;
            String[] result = new String[]{a+"", realLabel+"", preLabel+"", predictedData.get(a).value(predictedData.numAttributes()-1)+""};
            resultsList.add(result);
        }
        CSVUtil.write(outPathResult, resultsList);
    }

    /**
     * Save the trained LOF model
     * @param filePath
     * @throws Exception
     */
    public void saveLOF (String filePath) throws Exception {
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
    public static LOF readLOF (String filePath) throws Exception {
        File file = new File(filePath);
        if(!file.exists()){
            throw new NullPointerException(filePath + " does not exist.");
        }

        LOF lof = (LOF) SerializationHelper.read(filePath);
        return lof;
    }

    public static Instances test (LOF lof, String testFilePath, int classIndex, boolean includeHeader, String[] options) throws Exception {
        if(lof == null){
            throw new NullPointerException("lof is null.");
        }

        //Load data
        Instances testingData = FileLoader.loadInstancesFromCSV(testFilePath, classIndex, includeHeader, options);
        //Predict data
        Instances predictedData = Filter.useFilter(testingData, lof);

        return predictedData;
    }

    public static double evaluate (Instances predictedData, String testFilePathNo, String testFilePathLabel, double cutOffValue) throws Exception {
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
                int preLabel = predictedData.get(a).value(predictedData.get(a).numAttributes()-1) <= cutOffValue ? 0:1;

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