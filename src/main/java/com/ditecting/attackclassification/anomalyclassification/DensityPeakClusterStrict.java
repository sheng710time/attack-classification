package com.ditecting.attackclassification.anomalyclassification;

import com.ditecting.attackclassification.dataprocess.CSVUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/29 10:54
 */
@Slf4j
@Getter
@Setter
public class DensityPeakClusterStrict implements Serializable{
    private static final long serialVersionUID = 326446623915254422L;
    private double dc;
    private List<Sample> samples; // training samples
    private transient List<Sample> testingSamples; // testing samples
    private transient Map<Integer, Double> densityCountMap;//局部密度Map ：<index,densitycount>
    private transient List<Pair<Integer, Double>> sortedDensityList;//由大到小排序的Density list
    private transient Map<Integer, Double> deltaMap;//deltaMap:<index, delta>
    private transient List<Pair<Integer, Double>> sortedDeltaList;//由大到小排序的Density list
    private transient Map<Integer, Double> gammaMap;//gammaMap:<index, gamma>
    private transient List<Pair<Integer, Double>> sortedGammaList;//由大到小排序的Gamma list
    private transient Map<Integer, Integer> nearestNeighborMap;//每个样本的最近邻：<sampleIndex, nearestNeighborIndex>
    private transient Map<String, Double> pairDistanceMap;//样本对距离：<"index1 index2", distance>
    private transient double maxDelta;//最大Delta
    private transient double minDelta;//最小Delta
    private transient double maxDensity;//最大Density
    private transient double minDensity;//最小Density
    private transient double maxDistance;//最大样本距离
    private transient double minDistance;//最小样本距离
    private transient double neighborPercentage;//dc选取比例
    private transient Map<Integer, Integer> sampleToClusterMap;//划分的聚类结果<sampleIndex, clusterIndex>
    private Map<Integer, List<Integer>> clusterToSampleMap;//划分的聚类结果<clusterIndex, List(sampleIndex)>
    private Map<Integer, Integer> clustersLabels;// labels of clusters <clusterIndex, class>
    private Map<Integer, Sample> clusterCenterMap; // centers of clusters <clusterIndex, center-sample>

    public void init (List<Sample> samples) {
        log.info("Start to calculate dc.");
        this.samples = samples;
        this.calPairDistance();
        dc = this.findDC();
        log.info("Get dc:" + dc);
    }

    public void train (){
        log.info("Start to train.");

        this.calRhoCK(dc);//截断距离
//		cluster.calRhoGK(dc);//高斯距离
        this.calDelta();
        this.calGamma();

        /* Cluster */
        this.clusterByHeuristics(dc);
        log.info("End to train.");
    }

    public void train (List<Sample> samples){
        log.info("Start to train.");
        this.samples = samples;

        /* Calculate statistical properties of data */
        this.calPairDistance();
//        dc = 0.10007584708487159/20;
        log.info("Start to calculate dc.");
        dc = this.findDC();
        log.info("Get dc:" + dc);
        this.calRhoCK(dc);//截断距离
//		cluster.calRhoGK(dc);//高斯距离
        this.calDelta();
        this.calGamma();

        /* Cluster */
        this.clusterByHeuristics(dc);
        log.info("End to train.");
    }

    public void train (String trainFilePathLabel, String trainFilePath, int trainLabelIndex, int trainIndex) throws IOException {
        log.info("Start to train.");
        /* Load labeled and unlabeled training data */
        samples = new ArrayList<>();
        if(trainFilePathLabel != null){
            DataReader trainingReaderLabel = new DataReader();
            trainingReaderLabel.readData(trainFilePathLabel,trainLabelIndex);
            samples.addAll(trainingReaderLabel.getSamples());
        }
        if(trainFilePath != null){
            DataReader trainingReader = new DataReader();
            trainingReader.readData(trainFilePath,trainIndex);
            samples.addAll(trainingReader.getSamples());
        }

        /* Calculate statistical properties of data */
        this.calPairDistance();
//        dc = 0.10007584708487159/20;
        log.info("Start to calculate dc.");
        dc = this.findDC();
        log.info("Get dc:" + dc);
        this.calRhoCK(dc);//截断距离
//		cluster.calRhoGK(dc);//高斯距离
        this.calDelta();
        this.calGamma();

        /* Cluster */
        this.clusterByHeuristics(dc);
        log.info("End to train.");
    }

    /**
     * cluster flows according to centerNum
     * @param centerNum
     */
    public void clusterByCenterNum(int centerNum, int dimension) {
        //Generate clustering centers according to centerNum
        List<Integer> centerList = new ArrayList<Integer>();
        sampleToClusterMap = new HashMap<Integer, Integer>();
        for(int a=0; a<centerNum; a++){
            centerList.add(sortedGammaList.get(a).getKey());
            sampleToClusterMap.put(sortedGammaList.get(a).getKey(), sortedGammaList.get(a).getKey());
        }

        /*根据聚类中心进行聚类，注意：一定要按照密度由大到小逐个划分簇（从高局部密度到低局部密度）*/
        for(Pair<Integer, Double> candidate : sortedDensityList) {
            if(!centerList.contains(candidate.getKey())) {//处理非聚类中心
                //将最近邻居的类别索引作为该样本的类别索引
                if(sampleToClusterMap.containsKey(nearestNeighborMap.get(candidate.getKey()))) {
                    sampleToClusterMap.put(candidate.getKey(), sampleToClusterMap.get(nearestNeighborMap.get(candidate.getKey())));
                } else {
                    sampleToClusterMap.put(candidate.getKey(), -1);
                }
            }
        }
    }

    /**
     * cluster flows according to a heuristic algorithm with dc
     * @param dc
     */
    public void clusterByHeuristics (double dc) {
        if(sortedGammaList.size() < 1){
            throw new IllegalStateException("The size of sortedGammaList is smaller than 1.");
        }
        sampleToClusterMap = new HashMap<>();

        //Select the first point as the only initial cluster center
        ArrayList<Integer> beginnerList = new ArrayList<>();
        beginnerList.add(sortedGammaList.get(0).getKey());
        sampleToClusterMap.put(sortedGammaList.get(0).getKey(), sortedGammaList.get(0).getKey());

        /*根据聚类中心进行聚类，注意：一定要按照密度由大到小逐个划分簇（从高局部密度到低局部密度）*/
        for(Pair<Integer, Double> candidate : sortedDensityList) {
            if(!beginnerList.contains(candidate.getKey())) {//处理非聚类中心
                //将最近邻居的类别索引作为该样本的类别索引
                if(deltaMap.get(candidate.getKey()) <= dc){
                    sampleToClusterMap.put(candidate.getKey(), sampleToClusterMap.get(nearestNeighborMap.get(candidate.getKey())));
                }else{
                    beginnerList.add(candidate.getKey());
                    sampleToClusterMap.put(candidate.getKey(), candidate.getKey());
                }
            }
        }

        mapClustersToClasses();
        createClusterCenters();
    }

    /**
     * Mapping clusters to classes according to clustered results and labeled data
     *
     */
    public void mapClustersToClasses() {
        /* generate sampleToClusterMap */
        clusterToSampleMap = new HashMap<>();
        for(Map.Entry<Integer, Integer> element : sampleToClusterMap.entrySet()){
            if(clusterToSampleMap.containsKey(element.getValue())){
                clusterToSampleMap.get(element.getValue()).add(element.getKey());
            }else {
                List<Integer> elementList = new ArrayList<>();
                elementList.add(element.getKey());
                clusterToSampleMap.put(element.getValue(), elementList);
            }
        }

        /* generate clustersLabels */
        clustersLabels = new HashMap<>();
        for(Map.Entry<Integer, List<Integer>> elementList : clusterToSampleMap.entrySet()){
            Map<Integer, Integer> votes = new HashMap<>();
            for(int element : elementList.getValue()){
                int vote = (int) Double.parseDouble(samples.get(element).getLabel());
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
     * create centers of clusters
     *
     */
    public void createClusterCenters () {
        if(clusterToSampleMap == null || clustersLabels == null){
            throw new NullPointerException("clusterToSampleMap or clustersLabels is null.");
        }

        clusterCenterMap = new HashMap<>();
        int numColumns = samples.get(0).getAttributes().length;
        for(Map.Entry<Integer, List<Integer>> cluster : clusterToSampleMap.entrySet()){
            double[] totalValues = new double[numColumns];
            for(int a=0; a<cluster.getValue().size(); a++){
                for(int b=0; b<numColumns; b++){
                    totalValues[b] += samples.get(cluster.getValue().get(a)).getAttributes()[b];
                }
            }

            double[] attrs = new double[numColumns];
            for(int c=0; c<numColumns; c++){
                attrs[c] = totalValues[c]/cluster.getValue().size();
            }
            clusterCenterMap.put(cluster.getKey(), new Sample(attrs, clustersLabels.get(cluster.getKey())+""));
        }
    }

    public void test (List<Sample> testingSamples, int KNC) {
        log.info("Start to test.");
        this.testingSamples = testingSamples;
        for(int a=0; a<testingSamples.size(); a++){
            Sample sample = testingSamples.get(a);
            int centerId = findNearestCenter(sample, dc, KNC);
            if(centerId != -1){
                sample.setPredictLabel(clustersLabels.get(centerId)+"");
            } else {// The sample doesn't belong to any existing classes, and create a new cluster for it
                sample.setPredictLabel("-1");
            }
        }
        log.info("End to test.");
    }

    public void test (String testFilePath, int testLabelIndex, int KNC) throws IOException {
        log.info("Start to test.");
        DataReader testingReader = new DataReader();
        testingReader.readData(testFilePath,testLabelIndex);
        testingSamples = testingReader.getSamples();
        for(int a=0; a<testingSamples.size(); a++){
            Sample sample = testingSamples.get(a);
            int centerId = findNearestCenter(sample, dc, KNC);
            if(centerId != -1){
                sample.setPredictLabel(clustersLabels.get(centerId)+"");
            } else {// The sample doesn't belong to any existing classes, and create a new cluster for it
                sample.setPredictLabel("-1");
            }
        }
        log.info("End to test.");
    }

    /**
     * Find the nearest cluster center to the sample within cutOffValue
     * @param sample
     * @param cutOffValue
     * @return
     */
    public int findNearestCenter (Sample sample, double cutOffValue, int KNC) {
        List<Pair<Integer, Double>> distanceList = findKNearestCenters(sample, KNC);

        double currentMinDistance = Double.MAX_VALUE;
        int centerId = -1;
        for(int a=0; a<KNC; a++){
            double distance = findNearestDistanceToCenter(sample, distanceList.get(a).getLeft());
            if(distance<= cutOffValue && distance < currentMinDistance) {
                currentMinDistance = distance;
                centerId = distanceList.get(a).getLeft();
            }
        }

        return centerId;
    }

    /**
     * Find the nearest cluster center to the sample within cutOffValue
     * @param sample
     * @param KNC
     * @return
     */
    public List<Pair<Integer, Double>> findKNearestCenters (Sample sample, int KNC) {
        List<Pair<Integer, Double>> distanceList = new ArrayList();

        for(Map.Entry<Integer, Sample> center : clusterCenterMap.entrySet()){
            double distance = twoSampleDistance(sample, center.getValue());
            distanceList.add(new ImmutablePair<>(center.getKey(), distance));
        }

        Collections.sort(distanceList, new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                return o1.getRight().compareTo(o2.getRight());
            }
        });

        return distanceList.subList(0,KNC);
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
            double distance = twoSampleDistance(sample, samples.get(idList.get(a)));
            if(distance < minDistance){
                minDistance = distance;
            }
        }
        return minDistance;
    }

    public double evaluate () {
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
        System.out.println("total: " + total);
        System.out.println("TP: " + TP);
        System.out.println("TN: " + TN);
        System.out.println("FP: " + FP);
        System.out.println("FN: " + FN);
        System.out.println("error: " + error);
        System.out.println("accuracy: " + accuracy);
        System.out.println("detection_rate: " + detection_rate);
        return accuracy;
    }

    public void output (String outPathResult) {
        List<String[]> resultsList = new ArrayList<>();
        resultsList.add(new String[]{"flowNo", "data_class", "predicted_class"});
        for( int a=0; a<testingSamples.size(); a++){
            resultsList.add(new String[]{a + "", testingSamples.get(a).getLabel(), testingSamples.get(a).getPredictLabel()});
        }

        CSVUtil.write(outPathResult, resultsList);
    }

    /**
     * 计算gamma
     */
    public void calGamma(){
        HashMap<Integer, Double> deltaMapStd = new HashMap<Integer, Double>();
        HashMap<Integer, Double> densityCountMapStd = new HashMap<Integer, Double>();
		/*对delta和density进行标准化处理
		System.out.print("******************deltaStd and densityStd***************************");*/
        for(Map.Entry<Integer, Double> deltaEntry : deltaMap.entrySet()){
            double deltaStd = (deltaEntry.getValue()-minDelta)/(maxDelta - minDelta);
            double densityStd = (densityCountMap.get(deltaEntry.getKey()) - minDensity) / (maxDensity - minDensity);
//			System.out.print(deltaEntry.getValue() +"	"+densityCountMap.get(deltaEntry.getKey()));
            deltaMapStd.put(deltaEntry.getKey(), deltaStd);
            densityCountMapStd.put(deltaEntry.getKey(), densityStd);
        }

        //计算gamma
        gammaMap = new HashMap<Integer, Double>(samples.size());
        for(Map.Entry<Integer, Double> deltaStdEntry : deltaMapStd.entrySet()){
//			double gamma = Math.log(deltaStdEntry.getValue()+1)*Math.log(densityCountMapStd.get(deltaStdEntry.getKey())+1);//对delta和density做对数变换
            double gamma = deltaStdEntry.getValue()*densityCountMapStd.get(deltaStdEntry.getKey());
//			double gamma = deltaEntry.getValue()*densityCountMapStd.get(deltaEntry.getKey());
//			double gamma = deltaEntry.getValue()*densityCountMap.get(deltaEntry.getKey())
            gammaMap.put(deltaStdEntry.getKey(), gamma);
//			gammaMap.put(deltaStdEntry.getKey(), Math.log(gamma+1));//log变换
//			gammaMap.put(deltaStdEntry.getKey(), Math.pow(Math.E, gamma));//e变换
        }

        sortedGammaList = new ArrayList<Pair<Integer, Double>>();
        for(Map.Entry<Integer, Double> entry : gammaMap.entrySet()){
            sortedGammaList.add(new ImmutablePair<>(entry.getKey(), entry.getValue()));
        }

        Collections.sort(sortedGammaList, new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                Double temp1 = o1.getValue();
                Double temp2 = o2.getValue();
                return -temp1.compareTo(temp2);//倒序排列
            }
        });
    }

    /**
     * 计算Delta
     */
    public void calDelta() {
        //局部密度由大到小排序
        sortedDensityList = new ArrayList<Pair<Integer, Double>>();
        for(Map.Entry<Integer, Double> entry : densityCountMap.entrySet()){
            sortedDensityList.add(new ImmutablePair<>(entry.getKey(), entry.getValue()));
        }

        Collections.sort(sortedDensityList, new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                Double temp1 = o1.getValue();
                Double temp2 = o2.getValue();
                return -temp1.compareTo(temp2);//倒序排列
            }
        });
        nearestNeighborMap = new HashMap<Integer, Integer>(samples.size());
        deltaMap = new HashMap<Integer, Double>(samples.size());
        maxDelta = Double.MIN_VALUE;
        minDelta = Double.MAX_VALUE;

        for(int i = 0; i < sortedDensityList.size(); i++) {
            if(i == 0) {
                double maxDensityDistance = Double.MIN_VALUE;
                int farestNeighbor = -1;
                for(int k = 1; k < sortedDensityList.size(); k++){
                    double tempDist = getDistanceFromIndex(sortedDensityList.get(i).getKey(), sortedDensityList.get(k).getKey());
                    if(tempDist > maxDensityDistance){
                        maxDensityDistance = tempDist;
                        farestNeighbor = sortedDensityList.get(k).getKey();
                    }
                }
                nearestNeighborMap.put(sortedDensityList.get(i).getKey(), farestNeighbor);
                deltaMap.put(sortedDensityList.get(i).getKey(), maxDensityDistance);
            } else {
                double minDij = Double.MAX_VALUE;
                int index = 0;
                for(int j = 0; j < i; j++) {
                    double dis = getDistanceFromIndex(sortedDensityList.get(i).getKey(), sortedDensityList.get(j).getKey());
                    if(dis < minDij)  {
                        index = j;
                        minDij = dis;
                    }
                }
                nearestNeighborMap.put(sortedDensityList.get(i).getKey(), sortedDensityList.get(index).getKey());
                deltaMap.put(sortedDensityList.get(i).getKey(), minDij);
            }
        }
        //计算delta的极值
        for(Map.Entry<Integer, Double> deltaEntry : deltaMap.entrySet()){
            if(deltaEntry.getValue() > maxDelta){
                maxDelta = deltaEntry.getValue();
            }
            if(deltaEntry.getValue() < minDelta){
//				System.out.println("deltaEntry.getValue()  : "+deltaEntry.getValue());
                minDelta = deltaEntry.getValue();
            }
        }

        sortedDeltaList = new ArrayList<Pair<Integer, Double>>();
        for(Map.Entry<Integer, Double> entry : deltaMap.entrySet()){
            sortedDeltaList.add(new ImmutablePair<>(entry.getKey(), entry.getValue()));
        }

        Collections.sort(sortedDeltaList, new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                Double temp1 = o1.getValue();
                Double temp2 = o2.getValue();
                return -temp1.compareTo(temp2);//倒序排列
            }
        });

        //输出样本索引+样本局部密度+最近邻索引+delta值
//		System.out.println("输出样本索引  样本局部密度  最近邻索引  delta值");
//		for(Map.Entry<Integer, Integer> entry : sortedDensityList) {
//			System.out.println(entry.getKey()+" "+entry.getValue()+" "+nearestNeighborMap.get(entry.getKey())+" "+deltaMap.get(entry.getKey()));
//		}
    }

    /**
     * 根据索引获得两个样本间距离
     * @param index1
     * @param index2
     * @return
     */
    private double getDistanceFromIndex(int index1, int index2) {
        if(pairDistanceMap.containsKey(index1+" "+index2)) {
            return pairDistanceMap.get(index1+" "+index2);
        } else {
            return pairDistanceMap.get(index2+" "+index1);
        }
    }

    /**
     * 计算局部密度，Cut-off kernel
     */
    public void calRhoCK(double dcThreshold) {
        if(samples == null){
            throw new NullPointerException("samples is null.");
        }
        densityCountMap = new HashMap<Integer, Double>(samples.size());
        maxDensity = Double.MIN_VALUE;
        minDensity = Double.MAX_VALUE;
        //初始化为0
        for(int i= 0; i < samples.size(); i++) {
            densityCountMap.put(i, 0d);
        }
        for(Map.Entry<String, Double> diss : pairDistanceMap.entrySet()) {
            if(diss.getValue() <= dcThreshold) {
                String[] segs = diss.getKey().split(" ");
                int[] indexs = new int[2];
                indexs[0] = Integer.parseInt(segs[0]);
                indexs[1] = Integer.parseInt(segs[1]);
                for(int i = 0; i < indexs.length; i++) {
                    densityCountMap.put(indexs[i], densityCountMap.get(indexs[i]) + 1);
                }
            }
        }
        for(int i= 0; i < samples.size(); i++) {
            Double density = densityCountMap.get(i);
            if(density >maxDensity){
                maxDensity = density;
            }
            if(density < minDensity){
                minDensity = density;
            }
        }
    }

    /**
     * 计算局部密度，Gaussian kernel
     */
    public void calRhoGK(double dcThreshold) {
        if(samples == null){
            throw new NullPointerException("samples is null.");
        }
        densityCountMap = new HashMap<Integer, Double>(samples.size());
        maxDensity = Double.MIN_VALUE;
        minDensity = Double.MAX_VALUE;
        //初始化为0
        for(int i= 0; i < samples.size(); i++) {
            densityCountMap.put(i, 0d);
        }
        for(int j= 0; j < samples.size(); j++){
            for(int k= 0; k < samples.size(); k++){
                if(k != j) {
                    double diss = getDistanceFromIndex(j, k);
                    densityCountMap.put(j, densityCountMap.get(j)+Math.pow(Math.E, -Math.pow(diss/dcThreshold, 2)));
                }
            }
        }

        for(int i= 0; i < samples.size(); i++) {
            Double density = densityCountMap.get(i);
            if(density >maxDensity){
                maxDensity = density;
            }
            if(density < minDensity){
                minDensity = density;
            }
        }
    }

    /**
     * 计算所有样本每两个样本点的距离
     */
    public void calPairDistance() {
        pairDistanceMap = new HashMap<String, Double>();
        maxDistance = Double.MIN_VALUE;
        minDistance = Double.MAX_VALUE;
        for(int i = 0; i < samples.size() - 1; i++) {
            for(int j = i+1; j < samples.size(); j++) {
                double dis = twoSampleDistance(samples.get(i), samples.get(j));
                pairDistanceMap.put(i+" "+j, dis);
                if(dis > maxDistance) {
                    maxDistance = dis;
                }
                if(dis < minDistance){
                    minDistance = dis;
//					System.out.println("minDistanc： i："+i+" j："+j);
                }
            }
        }
    }

    /**
     * 计算截断距离
     * @return
     */
    public double findDC(){
        double tmpMax = maxDistance;
        double tmpMin = minDistance;
        double dc = 0.5 * (tmpMax + tmpMin);
        int dd=0;
        for(int iteration = 0; iteration < 10; iteration ++) {
            int neighbourNum = 0;
            for(Map.Entry<String, Double> dis : pairDistanceMap.entrySet()) {
                if(dis.getValue() < dc) neighbourNum ++;
            }
            neighborPercentage = (double)neighbourNum / pairDistanceMap.size();
            if(neighborPercentage >= 0.01 && neighborPercentage <= 0.01) break;
            if(neighborPercentage > 0.02) {
                tmpMax = dc;
                dc = 0.5 * (tmpMax + tmpMin);
            }
            if(neighborPercentage < 0.01) {
                tmpMin = dc;
                dc = 0.5 * (tmpMax + tmpMin);
            }
            dd++;
        }
        return dc;
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
}