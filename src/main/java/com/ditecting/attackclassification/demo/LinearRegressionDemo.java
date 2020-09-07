package com.ditecting.attackclassification.demo;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/14 16:23
 */
public class LinearRegressionDemo {

    public static void main (String[] args) throws IOException {
        int numInput = 1;
        int numOutput = 1;
        long seed = 0;
        double learningRate = 0.01;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.XAVIER)
                .updater(new Sgd(learningRate))
                .list()
                .layer(0, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(numInput).nOut(numOutput).build())
                .backpropType(BackpropType.Standard).build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();


        /* visualization */
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);

        /* save the model */
        String filePath = "C:\\Users\\18809\\Desktop\\model.zip";;
        File locationToSave = new File(filePath);
        boolean saveUpdater = true;
        ModelSerializer.writeModel(net, locationToSave, saveUpdater);

        /* load the model */
        MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

        int listenerFrequency = 1;
        net.setListeners(new ScoreIterationListener(1));
//        net.setListeners(new ScoreIterationListener(1), new StatsListener(statsStorage, listenerFrequency));

        int batchSize = 1;
        DataSetIterator iterator = getTrainingData(batchSize, new Random(seed));

        int nEpochs = 20;
        for(int i=0; i<nEpochs; i++){
            iterator.reset();
            net.fit(iterator);
        }

        final INDArray testNDArray = Nd4j.create(new double[]{10, 100}, new int[]{2, 1});
        INDArray result = net.output(testNDArray, false);
        System.out.println(result);

    }

    private static DataSetIterator getTrainingData (int batchSize, Random rand) {
        int nSamples = 1000;
        int MIN_RANGE = 0;
        int MAX_RANGE = 3;

        double[] output = new double[nSamples];
        double[] input = new double[nSamples];
        for(int i=0; i<nSamples; i++){
            input[i] = MIN_RANGE + (MAX_RANGE - MIN_RANGE)*rand.nextDouble();
            output[i] = 0.5 * input[i] + 0.1;
        }
        INDArray inputNDArray = Nd4j.create(input, new int[]{nSamples, 1});
        INDArray outputNDArray = Nd4j.create(output, new int[]{nSamples, 1});
        DataSet dataset = new DataSet(inputNDArray, outputNDArray);
        List<DataSet> listDs = dataset.asList();

        return new ListDataSetIterator(listDs, batchSize);
    }

}