package com.ditecting.attackclassification.anomalyclassification;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/9/16 15:56
 */
@Slf4j
@Getter
public class DensityPeakClusterStrictDistributed implements Serializable{
    private static final long serialVersionUID = 8905565197052736960L;
    private transient int batchSize;
    private double dc;
    private List<Sample> inputSamples;
    private int inputSampleSize;
    private transient List<List<Sample>> samplesList;
    private transient List<DensityPeakClusterStrict> dpcsList;
    private transient Map<Integer, Integer> sampleToClusterMap;// <sampleIndex, clusterIndex>
    private Map<Integer, List<Integer>> clusterToSampleMap;// <clusterIndex, List(sampleIndex)>
    private Map<Integer, Integer> clustersLabels;// labels of clusters <clusterIndex, class>
    private Map<Integer, Sample> clusterCenterMap; // centers of clusters <clusterIndex, center-sample>


    public void init (String trainFilePath, int labelIndex, int batchSize, double percentage, double myDc) throws IOException, InterruptedException {
        log.info("Start to init.");
        this.batchSize = batchSize;
        /* Load labeled and unlabeled training data */
        DataReader trainingReaderLabel = new DataReader();
        trainingReaderLabel.readData(trainFilePath,labelIndex);
        inputSamples = trainingReaderLabel.getSamples();
        inputSampleSize = inputSamples.size();
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
            BatchDcFinder bdf = new BatchDcFinder(dpcs, samples, percentage, countDownLatch);
            Thread thread = new Thread(bdf, "BatchDcFinder_" + taskIndex++);
            thread.start();
        }
        countDownLatch.await();
        Thread.sleep(100);// wait for all sub-threads

        double dcTotal = 0;
        for(DensityPeakClusterStrict dpcs : dpcsList){
            dcTotal += dpcs.getDc();
        }
//        this.dc = dcTotal/dpcsList.size();
        this.dc = myDc;
        log.info("Get dc:" + dc);
        for(DensityPeakClusterStrict dpcs : dpcsList){
            dpcs.setDc(dc);
        }

        log.info("End to init.");
    }

    /**
     * Create clustersLabels
     *
     */
    public void mapClustersToClasses() {
        /* generate clustersLabels */
        clustersLabels = new HashMap<>();
        for(Map.Entry<Integer, List<Integer>> elementList : clusterToSampleMap.entrySet()){
            Map<Integer, Integer> votes = new HashMap<>();
            for(int element : elementList.getValue()){
                int vote = (int) Double.parseDouble(inputSamples.get(element).getLabel());
                if(votes.containsKey(vote)){
                    votes.put(vote, votes.get(vote)+1);
                }else {
                    votes.put(vote, 1);
                }
            }
            int data_class = -1;
            int maxCount = Integer.MIN_VALUE;
            for(Map.Entry<Integer, Integer> vote : votes.entrySet()){
                if(vote.getKey() != -1){
                    if(vote.getValue() > maxCount){
                        maxCount = vote.getValue();
                        data_class = vote.getKey();
                    }
                }
            }

            clustersLabels.put(elementList.getKey(), data_class);
        }
    }

    /**
     * Create clusterCenterMap
     *
     */
    public void createClusterCenters () {
        if(clusterToSampleMap == null || clustersLabels == null){
            throw new NullPointerException("clusterToSampleMap or clustersLabels is null.");
        }

        clusterCenterMap = new HashMap<>();
        int numColumns = inputSamples.get(0).getAttributes().length;
        for(Map.Entry<Integer, List<Integer>> cluster : clusterToSampleMap.entrySet()){
            double[] totalValues = new double[numColumns];
            for(int a=0; a<cluster.getValue().size(); a++){
                for(int b=0; b<numColumns; b++){
                    totalValues[b] += inputSamples.get(cluster.getValue().get(a)).getAttributes()[b];
                }
            }

            double[] attrs = new double[numColumns];
            for(int c=0; c<numColumns; c++){
                attrs[c] = totalValues[c]/cluster.getValue().size();
            }
            clusterCenterMap.put(cluster.getKey(), new Sample(attrs, clustersLabels.get(cluster.getKey())+""));
        }
    }

    public void train () throws InterruptedException {
        log.info("Start to train.");
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
        Thread.sleep(100);// wait for all sub-threads

        log.info("Start to combine batches.");
        /* Combine batches */
        Map<Integer, Integer> combinedSampleToClusterMap = new HashMap<>();
        Map<Integer, List<Integer>> combinedClusterToSampleMap = new HashMap<>();
        Map<Integer, Integer> combinedClustersLabels = new HashMap<>();
        Map<Integer, Sample> combinedClusterCenterMap = new HashMap<>();
        for(DensityPeakClusterStrict dpcs : dpcsList){
            combinedSampleToClusterMap.putAll(dpcs.getSampleToClusterMap());
            combinedClusterToSampleMap.putAll(dpcs.getClusterToSampleMap());
            combinedClustersLabels.putAll(dpcs.getClustersLabels());
            combinedClusterCenterMap.putAll(dpcs.getClusterCenterMap());
        }

        List<Integer> centerList = new ArrayList<>();
        for(Map.Entry<Integer, Integer> entry : combinedClustersLabels.entrySet()){
            centerList.add(entry.getKey());
        }
        log.info("Find " + centerList.size() +" batch centers.");
        Map<Integer, Integer> centerToMasterMap = new HashMap<>();// <centerId, masterId>
        Map<Integer, List<Integer>> masterToCentersMap = new HashMap<>();// <masterId, centerId list>
        for(int a=0; a<centerList.size(); a++){
            Sample ca = combinedClusterCenterMap.get(centerList.get(a));
            int numCa = combinedClusterToSampleMap.get(centerList.get(a)).size();
            if(!centerToMasterMap.containsKey(centerList.get(a))){
                centerToMasterMap.put(centerList.get(a), centerList.get(a));
                List master = new ArrayList();
                master.add(centerList.get(a));
                masterToCentersMap.put(centerList.get(a), master);
            }

            for(int b=a+1; b<centerList.size(); b++){
                Sample cb = combinedClusterCenterMap.get(centerList.get(b));
                int numCb = combinedClusterToSampleMap.get(centerList.get(b)).size();
                double cutoffValue = (double)(numCa+numCb)/2*dc;
                if(twoSampleDistance(ca,cb) <= cutoffValue && nearCenters(centerList.get(a), centerList.get(b), combinedClusterToSampleMap, dc)){// distance of centers <= cutoff && nearest distance between samples in centers <= dc
                    if(!centerToMasterMap.containsKey(centerList.get(b))) {
                        centerToMasterMap.put(centerList.get(b), centerToMasterMap.get(centerList.get(a)));
                        masterToCentersMap.get(centerToMasterMap.get(centerList.get(a))).add(centerList.get(b));
                    }else {
                        int caMasterId = centerToMasterMap.get(centerList.get(a));
                        int cbMasterId = centerToMasterMap.get(centerList.get(b));
                        List<Integer> caList = masterToCentersMap.get(caMasterId);
                        List<Integer> cbList = masterToCentersMap.get(cbMasterId);

                        if(caMasterId != cbMasterId){
                            if(cbList.size() > caList.size()){// caList -> cbList(bigger)
                                for(int i=0; i<caList.size(); i++){
                                    centerToMasterMap.put(caList.get(i), cbMasterId);
                                    masterToCentersMap.get(cbMasterId).add(caList.get(i));
                                }
                                masterToCentersMap.remove(caMasterId);
                            }else {// cbList -> caList(bigger)
                                for(int i=0; i<cbList.size(); i++){
                                    centerToMasterMap.put(cbList.get(i), caMasterId);
                                    masterToCentersMap.get(caMasterId).add(cbList.get(i));
                                }
                                masterToCentersMap.remove(cbMasterId);
                            }
                        }
                    }
                }
            }
        }
        log.info("End to combine batches.");

        sampleToClusterMap = new HashMap<>();
        clusterToSampleMap = new HashMap<>();
        for(Map.Entry<Integer, Integer> entry : centerToMasterMap.entrySet()){
            int masterId = entry.getValue();
            int centerId = entry.getKey();
            List<Integer> sampleIdList = combinedClusterToSampleMap.get(centerId);
            for(int a=0; a<sampleIdList.size(); a++){
                sampleToClusterMap.put(sampleIdList.get(a), masterId);
                inputSamples.get(sampleIdList.get(a)).setPredictLabel(masterId+"");
            }
            if(clusterToSampleMap.containsKey(masterId)){
                clusterToSampleMap.get(masterId).addAll(sampleIdList);
            }else {
                List<Integer> list = new ArrayList<>();
                list.addAll(sampleIdList);
                clusterToSampleMap.put(masterId, sampleIdList);
            }
        }

        mapClustersToClasses();
        createClusterCenters();
        System.out.println("The number of training data: " + inputSamples.size());
        System.out.println("The number of clusters: " + clustersLabels.size());
        System.out.println("dc: " + dc);
        log.info("End to train.");
    }

    /**
     * Whether some samples exist in the two centers that distances between them are less than cutoffValue
     *
     * @param caId
     * @param cbId
     * @param combinedClusterToSampleMap
     * @param cutoffValue
     * @return
     */
    private boolean nearCenters (int caId, int cbId, Map<Integer, List<Integer>> combinedClusterToSampleMap, double cutoffValue) {
        List<Integer> caList = combinedClusterToSampleMap.get(caId);
        List<Integer> cbList = combinedClusterToSampleMap.get(cbId);
        for(int a=0; a<caList.size(); a++){
            for(int b=0; b<cbList.size(); b++){
                if(twoSampleDistance(inputSamples.get(caList.get(a)), inputSamples.get(cbList.get(b))) <= cutoffValue){
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find the nearest cluster center to the sample within cutOffValue
     * @param sample
     * @param cutOffValue
     * @return
     */
    public Pair<Integer, Double> findNearestCenter (Sample sample, double cutOffValue, int KNC) {
        List<Pair<Integer, Double>> distanceList = findPossibleNearestCenters(sample, cutOffValue, KNC);

        double currentMinDistance = Double.MAX_VALUE;
        int centerId = -1;
        for(int a=0; a<distanceList.size(); a++){
            double distance = findNearestDistanceToCenter(sample, distanceList.get(a).getLeft());
            if(distance<= cutOffValue && distance < currentMinDistance) {
                currentMinDistance = distance;
                centerId = distanceList.get(a).getLeft();
            }
        }
        return new ImmutablePair<>(centerId, currentMinDistance);
    }

    /**
     * Find the nearest cluster center to the sample within cutOffValue with the maximum number of KNC
     * @param sample
     * @param cutOffValue
     * @param KNC
     * @return
     */
    public List<Pair<Integer, Double>> findPossibleNearestCenters (Sample sample, double cutOffValue, int KNC) {
        List<Pair<Integer, Double>> distanceList = new ArrayList();

        for(Map.Entry<Integer, Sample> center : clusterCenterMap.entrySet()){
            double possibleCutOffValue = (double)(clusterToSampleMap.get(center.getKey()).size()+1)/2*cutOffValue;
            double distance = twoSampleDistance(sample, center.getValue());
            if(distance <= possibleCutOffValue){
                distanceList.add(new ImmutablePair<>(center.getKey(), distance));
            }
        }

        if(distanceList.size() <= KNC){
            return distanceList;
        }else{
            Collections.sort(distanceList, new Comparator<Pair<Integer, Double>>() {
                @Override
                public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                    return o1.getRight().compareTo(o2.getRight());
                }
            });

            return distanceList.subList(0,KNC);
        }
    }

    /**
     * find the nearest distance from the targeted sample within the cluster with centerId
     * @param sample
     * @param centerId
     * @return
     */
    public double findNearestDistanceToCenter (Sample sample, int centerId) {
        double minDistance = Double.MAX_VALUE;
        List<Integer> idList = clusterToSampleMap.get(centerId);
        for(int a=0; a<idList.size(); a++){
            double distance = twoSampleDistance(sample, inputSamples.get(idList.get(a)));
            if(distance < minDistance){
                minDistance = distance;
            }
        }
        return minDistance;
    }

    public List<Sample> test (String testFilePath, int testLabelIndex, int KNC) throws IOException {
        log.info("Start to test.");
        DataReader testingReader = new DataReader();
        testingReader.readData(testFilePath,testLabelIndex);
        List<Sample> testingSamples = testingReader.getSamples();
        for(int a=0; a<testingSamples.size(); a++){
            Sample sample = testingSamples.get(a);
            Pair<Integer, Double> nearestCenter = findNearestCenter(sample, dc, KNC);
            System.out.println(nearestCenter.getRight());
            int centerId = nearestCenter.getKey();
            if(centerId != -1){
                sample.setPredictLabel(clustersLabels.get(centerId)+"");
            } else {// The sample doesn't belong to any existing classes, and create a new cluster for it
                sample.setPredictLabel("-1");
            }
        }
        log.info("End to test.");
        return testingSamples;
    }

    /**
     *
     * @param testFilePath
     * @param testLabelIndex
     * @param KNC
     * @param Maximum >=2
     * @return
     * @throws IOException
     */
    public List<Sample> predict (String testFilePath, int testLabelIndex, int KNC, int Maximum) throws IOException {
        log.info("Start to predict.");
        int currentLabel = 1;
        DataReader testingReader = new DataReader();
        testingReader.readData(testFilePath,testLabelIndex);
        List<Sample> testingSamples = testingReader.getSamples();
        int times_update =0;
        int times_non_update =0;
        int times_create =0;
        for(int a=0; a<testingSamples.size(); a++){
            Sample sample = testingSamples.get(a);
            Pair<Integer, Double> nearestCenter = findNearestCenter(sample, dc, KNC);
            int centerId = nearestCenter.getKey();
            if(centerId != -1){
                sample.setPredictLabel(clustersLabels.get(centerId)+"");
                double wc = clusterToSampleMap.get(centerId).size()<Maximum ? clusterToSampleMap.get(centerId).size() : Maximum-1;
                double du = dc / Maximum * wc;
//                double du = 0;
                double centerDistance = twoSampleDistance(sample, clusterCenterMap.get(centerId));
                if(centerDistance > du){// update dpcsd with a new sample
                    times_update++;
                    update(sample, centerId);
                }else{
                    times_non_update++;
                }
            } else {// The sample doesn't belong to any existing classes, and then create a new cluster for it
//                System.out.println("create: " + (++times_create));
                sample.setPredictLabel(currentLabel+"");
                createNewCluster(sample, currentLabel++);
            }
        }
        System.out.println("update: " + times_update);
        System.out.println("non_update: " + times_non_update);
        log.info("End to predict.");
        return testingSamples;
    }

    /**
     * create and add a new cluster to dpcsd
     * @param sample
     * @param label
     */
    private void createNewCluster (Sample sample, int label){
//        log.info("Create a new cluster.");
        inputSamples.add(sample);
        List<Integer> elementList = new ArrayList<>();
        elementList.add(inputSamples.size()-1);
        clusterToSampleMap.put(elementList.get(0), elementList);
        clustersLabels.put(elementList.get(0), label);
        clusterCenterMap.put(elementList.get(0), sample);
    }

    /**
     * update the cluster with centerId and the whole model with the sample
     * @param sample
     * @param centerId
     */
    private void update(Sample sample, int centerId) {
//        log.info("Update the model.");
        inputSamples.add(sample);
        /*update the cluster with centerId*/
        List<Integer> cluster = clusterToSampleMap.get(centerId);
        cluster.add(inputSamples.size()-1);
        int numColumns = inputSamples.get(0).getAttributes().length;
        double[] totalValues = new double[numColumns];
        for(int a=0; a<cluster.size(); a++){
            for(int b=0; b<numColumns; b++){
                totalValues[b] += inputSamples.get(cluster.get(a)).getAttributes()[b];
            }
        }
        double[] attrs = new double[numColumns];
        for(int c=0; c<numColumns; c++){
            attrs[c] = totalValues[c]/cluster.size();
        }
        Sample newCenter = new Sample(attrs, clustersLabels.get(centerId)+"");
        clusterCenterMap.put(centerId, newCenter);

        /*update the whole model*/
        int numCa = clusterToSampleMap.get(centerId).size();
        List<Integer> nearCenterIds = new ArrayList<>();
        /*find near clusters*/
        for(Map.Entry<Integer, Sample> entry : clusterCenterMap.entrySet()){
            if(entry.getKey() == centerId){
                continue;
            }
            Sample cb = entry.getValue();
            int numCb = clusterToSampleMap.get(entry.getKey()).size();
            double cutoffValue = (double)(numCa+numCb)/2*dc;
            if(twoSampleDistance(newCenter,cb) <= cutoffValue && nearCenters(centerId, entry.getKey(), clusterToSampleMap, dc)){
                nearCenterIds.add(entry.getKey());
            }
        }
        if(nearCenterIds.size() == 0){
            return;
        }
        /*update clusterToSampleMap*/
        for(Integer id : nearCenterIds){
            clusterToSampleMap.get(centerId).addAll(clusterToSampleMap.get(id));
            clusterToSampleMap.remove(id);
            clustersLabels.remove(id);
            clusterCenterMap.remove(id);
        }
        /*update clustersLabels*/
        Map<Integer, Integer> votes = new HashMap<>();
        for(int element : clusterToSampleMap.get(centerId)){
            int vote;
            if(element < inputSampleSize){
                vote = (int) Double.parseDouble(inputSamples.get(element).getLabel());
            }else{
                vote = (int) Double.parseDouble(inputSamples.get(element).getPredictLabel());
            }
            if(votes.containsKey(vote)){
                votes.put(vote, votes.get(vote)+1);
            }else {
                votes.put(vote, 1);
            }
        }
        int data_class = -1;
        int maxCount = Integer.MIN_VALUE;
        for(Map.Entry<Integer, Integer> vote : votes.entrySet()){
            if(vote.getKey() != -1){
                if(vote.getValue() > maxCount){
                    maxCount = vote.getValue();
                    data_class = vote.getKey();
                }
            }
        }
        clustersLabels.put(centerId, data_class);
        /*update clusterCenterMap and sample labels*/
        List<Integer> clusterEx = clusterToSampleMap.get(centerId);
        int numColumnsEx = inputSamples.get(0).getAttributes().length;
        double[] totalValuesEx = new double[numColumnsEx];
        for(int a=0; a<clusterEx.size(); a++){
            inputSamples.get(clusterEx.get(a)).setPredictLabel(data_class+"");
            for(int b=0; b<numColumnsEx; b++){
                totalValuesEx[b] += inputSamples.get(clusterEx.get(a)).getAttributes()[b];
            }
        }
        double[] attrsEx = new double[numColumnsEx];
        for(int c=0; c<numColumnsEx; c++){
            attrsEx[c] = totalValuesEx[c]/clusterEx.size();
        }
        Sample newCenterEx = new Sample(attrsEx, clustersLabels.get(centerId)+"");
        clusterCenterMap.put(centerId, newCenterEx);
    }

    public void evaluate (List<Sample> testingSamples) {
        log.info("Start to evaluate.");
        int total = testingSamples.size();
        int error = 0;
        int TP = 0;
        int FP = 0;
        int TN = 0;
        int FN = 0;

        for( Sample sample : testingSamples){
            int realLabel = (int) Double.parseDouble(sample.getLabel());
            int preLabel = (int) Double.parseDouble(sample.getPredictLabel());
            if(realLabel != preLabel){
                error++;
            }
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

        double accuracy = 1 - ((double)error)/total;
        double detection_rate = -1;
        if(TP+FN > 0){
            detection_rate = ((double)TP)/(TP+FN);
        }
        System.out.println("The number of training data: " + inputSamples.size());
        System.out.println("The number of clusters: " + clustersLabels.size());
        System.out.println("dc: " + dc);
        System.out.println("total: " + total);
        System.out.println("TP: " + TP);
        System.out.println("TN: " + TN);
        System.out.println("FP: " + FP);
        System.out.println("FN: " + FN);
        System.out.println("error: " + error);
        System.out.println("accuracy: " + accuracy);
        System.out.println("detection_rate: " + detection_rate);
        log.info("End to evaluate.");
    }

    public void output (List<Sample> testingSamples, String outPathResult) {
        log.info("Start to output.");
        List<String[]> resultsList = new ArrayList<>();
        resultsList.add(new String[]{"flowNo", "data_class", "predicted_class"});
        for( int a=0; a<testingSamples.size(); a++){
            resultsList.add(new String[]{a + "", testingSamples.get(a).getLabel(), testingSamples.get(a).getPredictLabel()});
        }

        CSVUtil.write(outPathResult, resultsList);
        log.info("End to output.");
    }

    /**
     * 计算两个样本的距离
     * @param a
     * @param b
     * @return
     */
    private double twoSampleDistance(Sample a, Sample b){
        double[] aData = a.getAttributes();
        double[] bData = b.getAttributes();
        double distance = 0.0;
        for(int i = 0; i < aData.length; i++) {
            distance += Math.pow(aData[i] - bData[i], 2);
        }
//		return 1 - Math.exp(distance * (-0.5));//高斯距离没查到，换成欧式距离
        distance=Math.pow(distance, 0.5);//欧式距离
        return distance;
    }

    class BatchDcFinder implements Runnable {
        private DensityPeakClusterStrict dpcs;
        private List<Sample> samples;
        private double percentage;
        private CountDownLatch countDownLatch;

        public BatchDcFinder (DensityPeakClusterStrict dpcs, List<Sample> samples, double percentage, CountDownLatch countDownLatch) {
            this.dpcs = dpcs;
            this.samples = samples;
            this.percentage = percentage;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
//            log.info("Thread [" + Thread.currentThread().getName() + "] starts to run.");
            dpcs.init(samples, percentage);
            countDownLatch.countDown();
//            log.info("Thread [" + Thread.currentThread().getName() + "] ends to run.");
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
//            log.info("Thread [" + Thread.currentThread().getName() + "] starts to run.");
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
//            log.info("Thread [" + Thread.currentThread().getName() + "] ends to run.");
        }
    }


}
