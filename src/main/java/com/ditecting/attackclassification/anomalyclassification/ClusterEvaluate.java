package com.ditecting.attackclassification.anomalyclassification;

import java.util.List;

public class ClusterEvaluate {
	private List<List<double[]>> result;
	private int dimension;
	
	public ClusterEvaluate(List<List<double[]>> result, int dimension){
		this.result = result;
		this.dimension = dimension;
	}
	
	public double evaluateClusterDBI(){
    	double  dbi = 0;
    	double avgs[] = avgCluster();//求簇内样本间的平均距离，返回数组
    	double cens[][] = cenCluster();//求两个簇中心点间的距离，返回矩阵
    	double maxSec[] = new double[result.size()];
    	for(int i=0; i<result.size(); i++){
    		maxSec[i]=0;
    		for(int j=0; j<result.size(); j++){
    			if(j!=i){
    				double sec = (avgs[i]+avgs[j])/cens[i][j];
    				if(maxSec[i]<sec){
    					maxSec[i] = sec;
    				}
    			}
    		}
    	}
    	
    	double dbiT = 0;
    	for(int k=0; k<result.size(); k++){
    		dbiT += maxSec[k];
    	}
    	dbi = dbiT/result.size();
    	return dbi;
    }

	/**
	 * 计算两个向量的欧氏距离
	 * @param dp1
	 * @param dp2
	 * @return
	 */
	public double distance(double[] dp1, double[] dp2)
	{
		double distance=0.0;
		if(dp1.length==dp2.length){
			for(int i=0;i<dp1.length;i++){
				double temp=Math.pow((dp1[i]-dp2[i]), 2);
				distance=distance+temp;
			}
			distance=Math.pow(distance, 0.5);
			return distance;
		}
		return distance;
	}
    
    /**
     * 计算簇内样本间的平均距离
     * @return
     * 			各簇内样本间的平均距离
     */
    public double[] avgCluster(){
    	double avgs[] = new double[result.size()];
    
    	for(int i=0; i<result.size(); i++){
    		List<double[]> cluster = result.get(i);
    		double avg = 0;
    		if(cluster.size() < 2){
    			avgs[i] = 0;
    		}else{
    			for(int j=0; j<cluster.size(); j++){
        			for(int k=0; k<cluster.size(); k++){
        				if(k>j){
        					avg += distance(cluster.get(j), cluster.get(k));
        				}
        			}
        		}
        		avgs[i] = 2*avg / (cluster.size()*(cluster.size()-1));
    		}
    	}
    	
    	return avgs;
    }
    
    /**
     * 计算各个聚类的中心
     * @return
     */
    public double[][] centroidGen(){
    	double[][] centroids = new double[result.size()][dimension];
    	double[][] evalues = new double[result.size()][dimension];
    	for(int i=0; i<result.size(); i++){
    		List<double[]> cluster = result.get(i);
    		evalues[i] = new double[dimension];
    		for(int j=0; j<cluster.size(); j++){
    			for(int k=0; k<dimension; k++){
    				evalues[i][k] = (evalues[i][k]*j+cluster.get(j)[k])/(j+1);
    			}
    		}
    	}
		for (int m = 0; m < result.size(); m++){
			centroids[m] = evalues[m];
		}
    	return centroids;
    }
    
    /**
     * 计算各聚类中心的距离
     * @return
     */
    public double[][] cenCluster(){
    	double[][] centroidObs = centroidGen();
    	double cens[][] = new double[result.size()][result.size()];
    	for(int i=0; i<result.size(); i++){
    		for(int j=0; j<result.size(); j++){
    			cens[i][j] = distance(centroidObs[i], centroidObs[j]);
    		}
    	}
    	return cens;
    }
    
	/**
	 * 将聚类结果存入文件
	 * 
	 * @param result
	 * 				聚类结果
	public void saveResult(List<List<double[]>> result){
        try {
            File file = new File("F:\\Users\\chuan\\Desktop\\clusters2.txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            for (List<double[]> ps : result) {
                for (double[] p : ps) {
                    String w = p[0] + "\t" + p[1] + "\t" + p[2];
                    bw.write(w);
                    bw.write("\n");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	} */
	
}
