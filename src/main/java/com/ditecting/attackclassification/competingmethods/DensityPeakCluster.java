package com.ditecting.attackclassification.competingmethods;

import com.ditecting.attackclassification.anomalyclassification.DataReader;
import com.ditecting.attackclassification.anomalyclassification.Sample;
import com.ditecting.attackclassification.dataprocess.CSVUtil;

import javax.swing.filechooser.FileSystemView;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class DensityPeakCluster {
	private ArrayList<Sample> samples;
	/**局部密度Map ：<index,densitycount>*/
	private HashMap<Integer, Double> densityCountMap;
	/**由大到小排序的Density list*/
	private ArrayList<Entry<Integer, Double>> sortedDensityList;
	/**deltaMap:<index, delta>*/
	private HashMap<Integer, Double> deltaMap;
	/**由大到小排序的Density list*/
	private ArrayList<Entry<Integer, Double>> sortedDeltaList;
	/**gammaMap:<index, gamma>*/
	private HashMap<Integer, Double> gammaMap;
	/**由大到小排序的Gamma list*/
	private ArrayList<Entry<Integer, Double>> sortedGammaList;
	/**每个样本的最近邻：<sampleIndex, nearestNeighborIndex>*/
	private HashMap<Integer, Integer> nearestNeighborMap;
	/**样本对距离：<"index1 index2", distance>*/
	private HashMap<String, Double> pairDistanceMap;
	/**最大Delta*/
	private double maxDelta;
	/**最小Delta*/
	private double minDelta;
	/**最大Density*/
	private double maxDensity;
	/**最小Density*/
	private double minDensity;
	/**最大样本距离*/
	private double maxDistance;
	/**最小样本距离*/
	private double minDistance;
	/**dc选取比例*/
	private double neighborPercentage;
	/**选取的簇中心*/
	private ArrayList<Integer> centerList;
	/**划分的聚类结果<sampleIndex, clusterIndex>*/
	private HashMap<Integer, Integer> clusterMap;
	//通过alpha确定的聚类中心
	private List<Integer> centerListAlpha;
	private Map<Integer, Integer> clusterMapAlpha;
	//通过beta确定的聚类中心
	private List<Integer> centerListBeta;
	private Map<Integer, Integer> clusterMapBeta;
	//最大平均局部密度
	double maxClusterRho = 0;

	public DensityPeakCluster(ArrayList<Sample> samples) {
		this.samples = samples;
	}

	public ArrayList<Entry<Integer, Double>> getSortedGammaList(){
		return this.sortedGammaList;
	}
	
	public static void main(String[] args) throws IOException {
		//读取文件数据
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String filename = desktopPath + "\\experiment5\\exp4\\FSFDP\\all_data_encode_14-6.csv";
		DataReader reader = new DataReader();
        reader.readData(filename,6);
		DensityPeakCluster cluster = new DensityPeakCluster(reader.getSamples());
		cluster.calPairDistance();
//		double dc = cluster.findDC();//dc的确定采用了类似二分查找的方法，具有一定的随机性
        double dc = 0.001;
		cluster.calRhoCK(dc);//截断距离
		cluster.calDelta();
		cluster.calGamma();

		/*聚类并输出结果*/
		int centerNum = 47;
		cluster.clustering(centerNum);
		String outputPath = desktopPath + "\\experiment5\\exp4\\FSFDP\\all_data_encode_14-6_result_dpc_dc-"+ dc+"_centerNum-"+ centerNum +".csv";
		cluster.outputCluster(outputPath, cluster.clusterMap );
	}

	/**
	 * 将聚类结果转换成可评估格式
	 * @param clusterMap
	 * @param samples
	 * @return
	 */
	public Map<Integer, Map<Integer, List<Object>>> resultToEvaluation(Map<Integer, Integer> clusterMap, ArrayList<Sample> samples){//测试
		Map<Integer, Map<Integer, List<Object>>>  resultMap = new HashMap<Integer, Map<Integer, List<Object>>>();
		for(Entry<Integer, Integer> point:clusterMap.entrySet()){
			int pointNum = point.getKey();//数据点的编号
			int pointLabel = Integer.parseInt(samples.get(pointNum).getLabel());//数据点的标签
			int clusterLabel = point.getValue();//数据点的聚类标签

			if(resultMap.containsKey(clusterLabel)){//包含该类
				if(resultMap.get(clusterLabel).containsKey(pointLabel)){//当前cluster是否包含该数据对应的label
					resultMap.get(clusterLabel).get(pointLabel).add(samples.get(pointNum));
				}else{
					List<Object> sampleList = new ArrayList<>();
					sampleList.add(samples.get(pointNum));
					resultMap.get(clusterLabel).put(pointLabel, sampleList);
				}
			}else{
				Map<Integer, List<Object>> cluster = new HashMap<Integer, List<Object>>();
				List<Object> sampleList = new ArrayList<>();
				sampleList.add(samples.get(pointNum));
				cluster.put(pointLabel, sampleList);
				resultMap.put(clusterLabel, cluster);
			}
		}
		return resultMap;
	}

	/**
	 * 输出聚类结果
	 * @param clusterMap
	 */
	public void outputCluster(String filePath, Map<Integer, Integer> clusterMap){
		List<String[]> output = new ArrayList<String[]>();
		output.add(new String[]{"flowNo", "data_class", "predicted_class"});
		for(int a=0; a<samples.size(); a++){
            output.add(new String[]{a+"", samples.get(a).getLabel(), clusterMap.get(a)+""});
        }

		CSVUtil.write(filePath, output);
	}
	
	/**
	 * 通过函数斜率查找聚类中心
	 * @param centerNum 聚类中心数量
	 */
	public void clustering(int centerNum) {
		//根据centerNum生成聚类中心
		centerList = new ArrayList<Integer>();
		clusterMap = new HashMap<Integer, Integer>();
		for(int a=0; a<centerNum; a++){
			centerList.add(sortedGammaList.get(a).getKey());
			clusterMap.put(sortedGammaList.get(a).getKey(), sortedGammaList.get(a).getKey());
		}
		System.out.println(centerNum +" cluster center index list is "+centerList);
		
		/*根据聚类中心进行聚类，注意：一定要按照密度由大到小逐个划分簇（从高局部密度到低局部密度）*/
		for(Entry<Integer, Double> candidate : sortedDensityList) {
			if(!centerList.contains(candidate.getKey())) {//处理非聚类中心
				//将最近邻居的类别索引作为该样本的类别索引
				if(clusterMap.containsKey(nearestNeighborMap.get(candidate.getKey()))) {
					clusterMap.put(candidate.getKey(), clusterMap.get(nearestNeighborMap.get(candidate.getKey())));
				} else {
					clusterMap.put(candidate.getKey(), -1);
				}
			}
		}
	}

	/**
	 * 根据聚类中心划分clusterCore和clusterHalo
	 * @param dc
	 */
	public HashMap<Integer, Integer> clusterSeparate(ArrayList<Integer> centerList, HashMap<Integer, Integer> clusterMap, double dc){
		HashMap<Integer, Integer> clusterSeparateMap = new HashMap<Integer, Integer>();
		/**基于平均局部密度*/
		//根据centerList进行初次聚类
		for(Entry<Integer, Double> candidate : sortedDensityList) {
			if(!centerList.contains(candidate.getKey())) {//处理非聚类中心
				//将最近邻居的类别索引作为该样本的类别索引
				if(clusterMap.containsKey(nearestNeighborMap.get(candidate.getKey()))) {
					clusterMap.put(candidate.getKey(), clusterMap.get(nearestNeighborMap.get(candidate.getKey())));
				} else {
					clusterMap.put(candidate.getKey(), -1);
				}
			}
		}
		//初始化各个簇的平均局部密度
		HashMap<Integer, Double> clusterRhoMap = new HashMap<Integer, Double>();
		for(int center : centerList){
			clusterRhoMap.put(center, 0d);
		}
		
		//计算各个簇的平均局部密度
		for(int i=0; i<sortedDensityList.size(); i++){
			Entry<Integer, Double> pointI = sortedDensityList.get(i);
			for(int j=i+1;  j<sortedDensityList.size(); j++){
				Entry<Integer, Double> pointJ = sortedDensityList.get(j);
				if(clusterMap.get(pointI.getKey()) != clusterMap.get(pointJ.getKey()) && getDistanceFromIndex(pointI.getKey(), pointJ.getKey()) <dc){
//					System.out.println("pointI："+pointI.getKey()+"  ||  pointJ："+pointJ.getKey());
//					System.out.println("clusterNum: "+clusterMap.get(pointI.getKey())+"  ||  "+clusterMap.get(pointJ.getKey()));
//					System.out.println("dc："+dc+"  distance："+getDistanceFromIndex(pointI.getKey(), pointJ.getKey()));
					
					double meanRho = ((double)densityCountMap.get(pointI.getKey())+(double)densityCountMap.get(pointJ.getKey()))/2;
					if(meanRho > clusterRhoMap.get(clusterMap.get(pointI.getKey())))  clusterRhoMap.put(clusterMap.get(pointI.getKey()), meanRho);
					if(meanRho > clusterRhoMap.get(clusterMap.get(pointJ.getKey())))  clusterRhoMap.put(clusterMap.get(pointJ.getKey()), meanRho);
					if(meanRho > maxClusterRho){
						maxClusterRho = meanRho;
					}
				}
			}
		}
		//根据平均局部密度划分clusterCore和clusterHalo
		for(int k=0; k<sortedDensityList.size(); k++){
			Entry<Integer, Double> point = sortedDensityList.get(k);
			if(densityCountMap.get(point.getKey()) < clusterRhoMap.get(clusterMap.get(point.getKey()))){
				clusterSeparateMap.put(point.getKey(), 1);//clusterHalo
			}else{
				clusterSeparateMap.put(point.getKey(), 0);//clusterCore
			}
		}
		return clusterSeparateMap;
	}
	
	/**
	 * 计算gamma
	 */
	public void calGamma(){
		HashMap<Integer, Double> deltaMapStd = new HashMap<Integer, Double>();
		HashMap<Integer, Double> densityCountMapStd = new HashMap<Integer, Double>();
		/*对delta和density进行标准化处理
		System.out.print("******************deltaStd and densityStd***************************");*/
		for(Entry<Integer, Double> deltaEntry : deltaMap.entrySet()){
			double deltaStd = (deltaEntry.getValue()-minDelta)/(maxDelta - minDelta);
			double densityStd = (densityCountMap.get(deltaEntry.getKey()) - minDensity) / (maxDensity - minDensity);
//			System.out.print(deltaEntry.getValue() +"	"+densityCountMap.get(deltaEntry.getKey()));
			deltaMapStd.put(deltaEntry.getKey(), deltaStd);
			densityCountMapStd.put(deltaEntry.getKey(), densityStd);
		}
		
		//计算gamma
		gammaMap = new HashMap<Integer, Double>(samples.size());
		for(Entry<Integer, Double> deltaStdEntry : deltaMapStd.entrySet()){
//			double gamma = Math.log(deltaStdEntry.getValue()+1)*Math.log(densityCountMapStd.get(deltaStdEntry.getKey())+1);//对delta和density做对数变换
			double gamma = deltaStdEntry.getValue()*densityCountMapStd.get(deltaStdEntry.getKey());
//			double gamma = deltaEntry.getValue()*densityCountMapStd.get(deltaEntry.getKey());
//			double gamma = deltaEntry.getValue()*densityCountMap.get(deltaEntry.getKey())
			gammaMap.put(deltaStdEntry.getKey(), gamma);
//			gammaMap.put(deltaStdEntry.getKey(), Math.log(gamma+1));//log变换
//			gammaMap.put(deltaStdEntry.getKey(), Math.pow(Math.E, gamma));//e变换
		}
		sortedGammaList = new ArrayList<Entry<Integer,Double>>(gammaMap.entrySet());
		Collections.sort(sortedGammaList, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> o1,Entry<Integer, Double> o2) {
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
		sortedDensityList = new ArrayList<Entry<Integer,Double>>(densityCountMap.entrySet());
		Collections.sort(sortedDensityList, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> o1,Entry<Integer, Double> o2) {
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
		for(Entry<Integer, Double> deltaEntry : deltaMap.entrySet()){
			if(deltaEntry.getValue() > maxDelta){
				maxDelta = deltaEntry.getValue();
			}
			if(deltaEntry.getValue() < minDelta){
//				System.out.println("deltaEntry.getValue()  : "+deltaEntry.getValue());
				minDelta = deltaEntry.getValue();
			}
		}
		sortedDeltaList = new ArrayList<Entry<Integer,Double>>(deltaMap.entrySet());
		Collections.sort(sortedDeltaList, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> o1,Entry<Integer, Double> o2) {
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
		densityCountMap = new HashMap<Integer, Double>(samples.size());
		maxDensity = Double.MIN_VALUE;
		minDensity = Double.MAX_VALUE;
		//初始化为0
		for(int i= 0; i < samples.size(); i++) {
			densityCountMap.put(i, 0d);
		}
		for(Entry<String, Double> diss : pairDistanceMap.entrySet()) {
			if(diss.getValue() < dcThreshold) {
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
		for(int iteration = 0; iteration < 100; iteration ++) {
			int neighbourNum = 0;
			for(Entry<String, Double> dis : pairDistanceMap.entrySet()) {
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
	public ArrayList<Integer> getCenterList() {
		return centerList;
	}
	
	public void predictLabel() {
		for(int i = 0; i < samples.size(); i++) {
			//System.out.println(clusterMap.get(i));
			if(clusterMap.get(i) != -1)
				samples.get(i).setPredictLabel(samples.get(clusterMap.get(i)).getLabel());//认为聚簇中心的标签就是簇成员的标签
		}
	}
}
