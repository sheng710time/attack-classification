package com.ditecting.attackclassification.anomalydetection;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/24 11:20
 */
public class SAE_AD {
    private DataNormalization normalization;// 0: none, 1: normalize, 2: standardize
    private MultiLayerNetwork net;
    private MultiLayerNetwork encoder;
    private MultiLayerNetwork decoder;
    private DataSetIterator iterTraining;
    private DataSetIterator iterTesting;
    private List<Triple<Integer, Double,INDArray>> predictedData;
    private boolean isTrained;
    private boolean isTested;
    private int mode; // 0: none, 1: normalize, 2: standardize

    public SAE_AD(){
        this(20, 16, 8, 2);
    }

    public SAE_AD (int first, int second, int third, int mode){
        this.mode = mode;

        switch (mode){
            case 0:
                normalization = null;break;
            case 1:
                normalization = new NormalizerMinMaxScaler();break;
            case 2:
                normalization = new NormalizerStandardize();break;
        }

        /* Set up SAE (20 -> 16 -> 8 -> 16 -> 20)*/
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new AdaGrad(0.05))
                .activation(Activation.RELU)
                .l2(0.0001)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(first).nOut(second)
                        .build())
                .layer(1, new DenseLayer.Builder().nIn(second).nOut(third)
                        .build())
                .layer(2, new DenseLayer.Builder().nIn(third).nOut(second)
                        .build())
                .layer(3, new OutputLayer.Builder().nIn(second).nOut(first).activation(Activation.RELU)
                        .lossFunction(LossFunctions.LossFunction.MSE)
                        .build())
                .build();
        net = new MultiLayerNetwork(conf);
        net.setListeners(Collections.singletonList(new ScoreIterationListener(1)));

        /* Set up encoder */
        MultiLayerConfiguration conf2 = new NeuralNetConfiguration.Builder()
                .list()
                .layer(0, new DenseLayer.Builder().nIn(first).nOut(second)
                        .build())
                .layer(1, new OutputLayer.Builder().nIn(second).nOut(third)
                        .lossFunction(LossFunctions.LossFunction.MSE)
                        .build())
                .build();
        encoder = new MultiLayerNetwork(conf2);

        /* Set up decoder */
        MultiLayerConfiguration conf3 = new NeuralNetConfiguration.Builder()
                .list()
                .layer(0, new DenseLayer.Builder().nIn(third).nOut(second)
                        .build())
                .layer(1, new OutputLayer.Builder().nIn(second).nOut(first)
                        .lossFunction(LossFunctions.LossFunction.MSE)
                        .build())
                .build();
        decoder = new MultiLayerNetwork(conf3);

        isTrained = false;
        isTested = false;
    }


    public void train (String trainFilePath, int labelIndex, int numClasses, int batchSizeTraining) throws Exception {
        /* Load training data */
        iterTraining = readCSVDataset(trainFilePath, batchSizeTraining, labelIndex, numClasses);
        List<INDArray> featuresTrain = new ArrayList<>();
        while(iterTraining.hasNext()){
            featuresTrain.add(iterTraining.next().getFeatures());
        }

        if(normalization != null){
            /* Normalization fit */
            normalization.fit(iterTraining);
            /* Normalize training data */
            for(INDArray trainElement : featuresTrain) {
                normalization.transform(trainElement);//Normalize data
            }
        }

        /* Train the model */
        double totalScore = 0;
        int count = 0;
        int nEpochs = 30;
        for( int epoch=0; epoch<nEpochs; epoch++ ){
            totalScore = 0;
            count = 0;
            for(INDArray trainElement : featuresTrain){
                net.fit(trainElement,trainElement);
                totalScore += net.score();
                count ++;
            }
            System.out.println("Epoch " + epoch + " complete with avg score (" + totalScore/count +")");
        }

        /* Set encoder and decoder */
        encoder.setLayers(new Layer[]{net.getLayer(0), net.getLayer(1)});
        decoder.setLayers(new Layer[]{net.getLayer(2), net.getLayer(3)});

        this.isTrained = true;
    }

    public void test (String testFilePath, int labelIndex, int numClasses, int batchSizeTesting) throws IOException, InterruptedException {
        if(!isTrained){
            throw new IllegalStateException("SAE has not been trained.");
        }

        /* Load testing data */
        iterTesting = readCSVDataset(testFilePath, batchSizeTesting, labelIndex, numClasses);
        List<INDArray> featuresTest = new ArrayList<>();
        List<INDArray> labelsTest = new ArrayList<>();
        while(iterTesting.hasNext()){
            DataSet ds = iterTesting.next();
            featuresTest.add(ds.getFeatures());
            INDArray indexes = Nd4j.argMax(ds.getLabels(),1); //Convert from one-hot representation -> index
            labelsTest.add(indexes);
        }

        /* Predict testing data */
        predictedData = new ArrayList<>();
        int count = 0;
        for(INDArray testElement : featuresTest){
            if(normalization != null){
                normalization.transform(testElement);//Normalize data
            }
            int nRows = testElement.rows();
            for( int j=0; j<nRows; j++){
                INDArray example = testElement.getRow(j, true);
                double score = net.score(new DataSet(example,example));
                List<INDArray> ss = net.feedForward(example);
                predictedData.add(new ImmutableTriple<>(count++, score, example));
            }
        }

        this.isTested = true;
    }

    public void encode (String testFilePath, int labelIndex, int numClasses, int batchSizeTesting, String outPathEncode) throws IOException, InterruptedException {
        if(!isTrained){
            throw new IllegalStateException("SAE has not been trained.");
        }

        /* Load testing data */
        iterTesting = readCSVDataset(testFilePath, batchSizeTesting, labelIndex, numClasses);
        List<INDArray> featuresTest = new ArrayList<>();
        List<INDArray> labelsTest = new ArrayList<>();
        while(iterTesting.hasNext()){
            DataSet ds = iterTesting.next();
            featuresTest.add(ds.getFeatures());
            INDArray indexes = Nd4j.argMax(ds.getLabels(),1); //Convert from one-hot representation -> index
            labelsTest.add(indexes);
        }

        /* encode testing data */
        List<String[]> resultsList = new ArrayList<>();
        List<Triple<Integer, INDArray, INDArray>> encodedDataList = new ArrayList<>();
        int encodeSize = encoder.layerSize(encoder.getnLayers()-1);
        String[] header = new String[encodeSize];
        for(int a=0; a<encodeSize; a++){
            header[a] = "attr" + (a+1);
        }
        resultsList.add(header);

        int count = 0;
        for(INDArray testElement : featuresTest){
            if(normalization != null){
                normalization.transform(testElement);//Normalize data
            }
            int nRows = testElement.rows();
            for( int j=0; j<nRows; j++){
                INDArray example = testElement.getRow(j, true);
                List<INDArray> ss = net.feedForward(example);
                double score = net.score(new DataSet(example, example));
                INDArray encodedData = encoder.output(example);
                encodedDataList.add(new ImmutableTriple<>(count++, encodedData, example));
                String[] code = new String[encodeSize];
                for(int a=0; a<encodeSize; a++){
                    code[a] = encodedData.getRow(0).getColumn(a).getDouble()+"";
                }
                resultsList.add(code);
            }
        }

        CSVUtil.write(outPathEncode, resultsList);
    }

    public INDArray decode (INDArray encodedData)  {
        if(!isTrained){
            throw new IllegalStateException("SAE has not been trained.");
        }

        /* decode data */
        return decoder.output(encodedData);
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
                int preLabel = predictedData.get(a).getMiddle() < cutOffValue ? 0:1;

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
        resultsList.add(new String[]{"flowNo", "packetNo", "data_class", "predicted_class", "sae_score"});
        for( int a=0; a<predictedData.size(); a++){
            List<Integer> numbers = numbersMap.get(a);
            for(int number : numbers){
                int preLabel = predictedData.get(a).getMiddle() < cutOffValue ? 0:1;
                String[] result = new String[]{a+"", number+"", labels.get(number)+"", preLabel+"", predictedData.get(a).getMiddle()+""};
                resultsList.add(result);
            }
        }
        CSVUtil.write(outPathResult, resultsList);
    }

    /**
     * used for testing and training
     *
     * @param csvFileClasspath
     * @param batchSize
     * @param labelIndex
     * @param numClasses
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private static DataSetIterator readCSVDataset(String csvFileClasspath, int batchSize, int labelIndex, int numClasses) throws IOException, InterruptedException{
        int skipNumLines = 1;
        RecordReader rr = new CSVRecordReader(skipNumLines); //Skip first n lines (the first line -- header)

        rr.initialize(new FileSplit(new File(csvFileClasspath)));
        DataSetIterator iterator = new RecordReaderDataSetIterator(rr,batchSize,labelIndex,numClasses);
        return iterator;
    }

}