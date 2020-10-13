package com.ditecting.attackclassification.anomalyclassification;


import java.util.Collections;
import java.util.List;

public class MathMethod {
	
	//极差R(X)=[max-min]
	public static double Range(double[] x){
		int m=x.length;
		
		if(m == 0){//没有元素返回0
			return 0;
		}
		
		if(m == 1){//只有一个元素，则设极差为0
			return 0;
		}
		
        double max = -1;  
        double min = -1;
        for(int i=0;i<m;i++){//求和  
            if(x[i]>max || max == -1){
            	max = x[i];
            }
            if(x[i]<min || min == -1){
            	min = x[i];
            }
        }  
        return max-min;
	}
	
	//期望E(X)=[x1+...+xm]/m
		public static double Average(double[] x){
			int m=x.length;
			if(m == 0){//没有元素返回0
				return 0;
			}
	        double sum=0;  
	        for(int i=0;i<m;i++){//求和  
	            sum+=x[i];  
	        }  
	        return sum/m;
		}
	
	 //方差D(X)=[(x1-x)^2 +...(xm-x)^2]/m  
    public static double Variance(double[] x) {   
        int m=x.length;  
        if(m == 0){//没有元素返回0
			return 0;
		}
        double sum=0;  
        for(int i=0;i<m;i++){//求和  
            sum+=x[i];  
        }  
        double dAve=sum/m;//求平均值  
        double dVar=0;  
        for(int i=0;i<m;i++){//求方差  
            dVar+=(x[i]-dAve)*(x[i]-dAve);  
        }  
        return dVar/m;  
    }  
      
    //标准差σ(X)=sqrt(D(X))  
    public static double StandardDeviation(double[] x) {   
        int m=x.length;  
        if(m == 0){//没有元素返回0
			return 0;
		}
        double sum=0;  
        for(int i=0;i<m;i++){//求和  
            sum+=x[i];  
        }  
        double dAve=sum/m;//求平均值  
        double dVar=0;  
        for(int i=0;i<m;i++){//求方差  
            dVar+=(x[i]-dAve)*(x[i]-dAve);  
        }  
        return Math.sqrt(dVar/m);     
    }

    /**
     * 求使距离平方和最小的点
     * @param pointList
     * @return
     */
    public static Double minDisSquare(List<Double> pointList, int step){
	    Collections.sort(pointList);
        double min = pointList.get(0);
        double max = pointList.get(pointList.size()-1);
        double stepLength = (max-min)/step;
        double minDis = Double.MAX_VALUE;
        double minPoint = min;
        for (double a=min; a<max; a+=stepLength){
            double curDis = calcuDistance(a, pointList);
            if( minDis > curDis) {
                minDis = curDis;
                minPoint = a;
            }
        }
        return minPoint;
    }

    /**
     * 求与x点的距离平方和
     * @param x
     * @param pointList
     * @return
     */
    public static Double calcuDistance(Double x, List<Double> pointList){
        Double distance = 0d;
        for(Double point: pointList){
            distance += Math.pow(point - x, 2);
        }
//        System.out.println(distance);
        return distance;
    }
}
