package com.ditecting.attackclassification.anomalyclassification;

import com.ditecting.attackclassification.dataprocess.CSVUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataReader {
	private ArrayList<Sample> samples;

    public ArrayList<Sample> getSamples() {
        return samples;
    }

	/**
	 * 读数据
	 * @param filename 文件名
	 * @param labelIndex 标签所在位子，-1表示无标签, from 0
	 */
	public void readData(String filename, int labelIndex) throws IOException {
        List<String[]> strsList = CSVUtil.readMulti(filename, true);
        int numAttributes = -1;
        if(strsList != null && strsList.size()>0){
            numAttributes = strsList.get(0).length;
            if(labelIndex != -1){
                numAttributes--;
            }
        }else{
            throw new IOException("No record in " + filename);
        }

        samples = new ArrayList<Sample>();
        for(String[] strs : strsList){
            String label = "-1";
            double[] atts = new double[numAttributes];
            double[] attsStd = new double[numAttributes];
            int index = 0;
            for(int a=0; a<strs.length; a++){
                if(labelIndex == a){
                    label = strs[a];
                }else {
                    atts[index] = Double.parseDouble(strs[a]);
                    attsStd[index] = atts[index];
                    index++;
                }
            }
            samples.add(new Sample(atts, label));
        }
	}
}
