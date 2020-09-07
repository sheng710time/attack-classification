package com.ditecting.attackclassification.anomalyclassification.showdata;

import java.util.*;
import java.util.Map.Entry;

public class ShowDataHandler {
	

	/**
	 * 将聚类结果数据转换成JfreeChart能够显示的格式
	 * 
	 * @param sortedGammaList
	 * 					聚类结果数据
	 * @return
	 * 			JfreeChart显示散点图所需要的数据
	 */
	public static Map dpTo2DScatterPlot(List<Entry<Integer, Double>> sortedGammaList) {
		//添加类别和所含数据条数
		ArrayList<Set<Integer>> all = new ArrayList<Set<Integer>>();
		//添加所有待展示数据,所有数据点都放在一个list中，由all指定数据点所属的类别
		LinkedList<HashMap> ac_cnlList = new LinkedList<HashMap>();
		
		int elementNum = 0;//成员序号
		Set<Integer> dotSet = new HashSet<Integer>();
		for(Entry<Integer, Double> gammaEntry : sortedGammaList){
			HashMap<String, Double> ac_cnl = new HashMap<String, Double>();
			ac_cnl.put("AC", (double)elementNum+1);
			ac_cnl.put("CNL", gammaEntry.getValue());
			ac_cnlList.add(ac_cnl);
			dotSet.add(elementNum);
			elementNum++;
		}
		all.add(dotSet);

		Map<String, Collection> returnMap = new HashMap<String, Collection>();
		returnMap.put("ac_cnlList", ac_cnlList);
		returnMap.put("all", all);
		
		return returnMap;
	}
}
