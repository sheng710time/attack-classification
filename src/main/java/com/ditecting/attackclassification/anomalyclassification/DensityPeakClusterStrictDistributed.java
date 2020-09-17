package com.ditecting.attackclassification.anomalyclassification;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/9/16 15:56
 */
@Slf4j
public class DensityPeakClusterStrictDistributed implements Serializable{
    private static final long serialVersionUID = 8905565197052736960L;
    private int batchSize;
    private List<List<Sample>> samplesList;
    private List<DensityPeakClusterStrict> dpcsList;
    private double dc;
    private transient Map<Integer, Integer> sampleToClusterMap;// <sampleIndex, clusterIndex>
    private Map<Integer, List<Integer>> clusterToSampleMap;// <clusterIndex, List(sampleIndex)>
    private Map<Integer, Integer> clustersLabels;// labels of clusters <clusterIndex, class>
    private Map<Integer, Sample> clusterCenterMap; // centers of clusters <clusterIndex, center-sample>


    public void init (String trainFilePathLabel, String trainFilePath, int trainLabelIndex, int trainIndex,int batchSize) throws IOException, InterruptedException {
        log.info("Start to train.");
        this.batchSize = batchSize;
        /* Load labeled and unlabeled training data */
        List<Sample> inputSamples = new ArrayList<>();
        if(trainFilePathLabel != null){
            DataReader trainingReaderLabel = new DataReader();
            trainingReaderLabel.readData(trainFilePathLabel,trainLabelIndex);
            inputSamples.addAll(trainingReaderLabel.getSamples());
        }
        if(trainFilePath != null){
            DataReader trainingReader = new DataReader();
            trainingReader.readData(trainFilePath,trainIndex);
            inputSamples.addAll(trainingReader.getSamples());
        }
        /* Split data into batches */
        samplesList = new ArrayList<>();
        int batchIndex = 0;
        while(inputSamples.size()-batchIndex*batchSize > batchSize){
            samplesList.add(inputSamples.subList((batchIndex++)*batchSize, batchIndex*batchSize));
        }
        if(inputSamples.size()-batchIndex*batchSize > 0){
            samplesList.add(inputSamples.subList(batchIndex*batchSize, inputSamples.size()));
        }

        /* Calculate dc */
        CountDownLatch countDownLatch = new CountDownLatch(samplesList.size());
        int taskIndex = 0;
        dpcsList = new ArrayList<>();
        for(List<Sample> samples : samplesList){
            DensityPeakClusterStrict dpcs = new DensityPeakClusterStrict();
            dpcsList.add(dpcs);
            BatchDcFinder bdf = new BatchDcFinder(dpcs, samples, countDownLatch);
            Thread thread = new Thread(bdf, "BatchDcFinder_" + taskIndex++);
            thread.start();
        }
        countDownLatch.await();

        double dcTotal = 0;
        for(DensityPeakClusterStrict dpcs : dpcsList){
            dcTotal += dpcs.getDc();
        }
        this.dc = dcTotal/dpcsList.size();
        for(DensityPeakClusterStrict dpcs : dpcsList){
            dpcs.setDc(dc);
        }
    }

    public void train () throws InterruptedException {//TODO incomplete
        int taskIndex = 0;
        CountDownLatch countDownLatch = new CountDownLatch(samplesList.size());
        int batchIndex = 0;
        for(DensityPeakClusterStrict dpcs : dpcsList){
            BatchTrainor bt = new BatchTrainor(batchIndex, dpcs, countDownLatch);
            Thread thread = new Thread(bt, "BatchTrainor_" + taskIndex++);
            thread.start();
            batchIndex += batchSize;
        }
        countDownLatch.await();

        /* Combine batches */
        Map<Integer, Integer> combinedSampleToClusterMap = new HashMap<>();
        Map<Integer, List<Integer>> combinedClusterToSampleMap = new HashMap<>();
        Map<Integer, Integer> combinedClustersLabels = new HashMap<>();
        Map<Integer, Sample> combinedClusterCenterMap = new HashMap<>();
        for(DensityPeakClusterStrict dpcs : dpcsList){//TODO test
            combinedSampleToClusterMap.putAll(dpcs.getSampleToClusterMap());
            combinedClusterToSampleMap.putAll(dpcs.getClusterToSampleMap());
            combinedClustersLabels.putAll(dpcs.getClustersLabels());
            combinedClusterCenterMap.putAll(dpcs.getClusterCenterMap());
        }

        sampleToClusterMap = new HashMap<>();
        clusterToSampleMap = new HashMap<>();
        clustersLabels = new HashMap<>();
        clusterCenterMap = new HashMap<>();


    }

    class BatchDcFinder implements Runnable {
        private DensityPeakClusterStrict dpcs;
        private List<Sample> samples;
        private CountDownLatch countDownLatch;

        public BatchDcFinder (DensityPeakClusterStrict dpcs, List<Sample> samples, CountDownLatch countDownLatch) {
            this.dpcs = dpcs;
            this.samples = samples;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            log.info("Thread [" + Thread.currentThread().getName() + "] starts to perform.");
            dpcs.init(samples);
            countDownLatch.countDown();
            log.info("Thread [" + Thread.currentThread().getName() + "] ends to perform.");
        }
    }

    class BatchTrainor implements Runnable {
        private int batchIndex;
        private DensityPeakClusterStrict dpcs;
        private CountDownLatch countDownLatch;

        public BatchTrainor(int batchIndex, DensityPeakClusterStrict dpcs, CountDownLatch countDownLatch) {
            this.batchIndex = batchIndex;
            this.dpcs = dpcs;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            log.info("Thread [" + Thread.currentThread().getName() + "] starts to perform.");
            dpcs.train();

            /* update index with batchIndex */
            if(batchIndex != 0){
                Map<Integer, Integer> newSampleToClusterMap = new HashMap<>();
                for(Map.Entry<Integer, Integer> entry : dpcs.getSampleToClusterMap().entrySet()){
                    newSampleToClusterMap.put(entry.getKey()+batchIndex, entry.getValue()+batchIndex);
                }
                dpcs.setSampleToClusterMap(newSampleToClusterMap);

                Map<Integer, List<Integer>> newClusterToSampleMap = new HashMap<>();
                for(Map.Entry<Integer, List<Integer>> entry : dpcs.getClusterToSampleMap().entrySet()){
                    List list = new ArrayList();
                    for(int a=0; a<entry.getValue().size(); a++){
                        list.add(entry.getValue().get(a) + batchIndex);
                    }
                    newClusterToSampleMap.put(entry.getKey()+batchIndex, list);
                }
                dpcs.setClusterToSampleMap(newClusterToSampleMap);

                Map<Integer, Integer> newClustersLabels = new HashMap<>();
                for(Map.Entry<Integer, Integer> entry : dpcs.getClustersLabels().entrySet()){
                    newClustersLabels.put(entry.getKey()+batchIndex, entry.getValue());// The values of labels do not need to be altered.
                }
                dpcs.setClustersLabels(newClustersLabels);

                Map<Integer, Sample> newClusterCenterMap = new HashMap<>();
                for(Map.Entry<Integer, Sample> entry : dpcs.getClusterCenterMap().entrySet()){
                    newClusterCenterMap.put(entry.getKey()+batchIndex, entry.getValue());
                }
                dpcs.setClusterCenterMap(newClusterCenterMap);
            }

            countDownLatch.countDown();
            log.info("Thread [" + Thread.currentThread().getName() + "] ends to perform.");
        }
    }

}

