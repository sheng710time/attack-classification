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
	private ArrayList<Sample> samplesStd;


    public ArrayList<Sample> getSamples() {
        return samples;
    }

    public ArrayList<Sample> getSamplesStd() {
        return samplesStd;
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
        samplesStd = new ArrayList<Sample>();
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
            samplesStd.add(new Sample(attsStd, label));
        }

        trainDataStandardize(numAttributes);
	}
	
	/**
	 * min-max标准化，新数据=（原数据-极小值）/（极大值-极小值）
	 * 
	 */
	public void trainDataStandardize(int numAttributes){
		double minimums[] = new double[numAttributes];
		double maximums[] = new double[numAttributes];
		for(int i=0 ; i<minimums.length ; i++){
			minimums[i] = Double.MAX_VALUE;
			maximums[i] = Double.MIN_VALUE;
		}
		
		for(Sample sample : samplesStd){
			for(int j=0 ; j<numAttributes; j++){
				if(minimums[j] > sample.getAttributes()[j]){//更新最大值
					minimums[j] = sample.getAttributes()[j];
				}
				if(maximums[j] < sample.getAttributes()[j]){//更新最小值
					maximums[j] = sample.getAttributes()[j];
				}
			}
		}
		
		//标准化数据
		for(Sample sample : samplesStd){
			for(int j=0 ; j<numAttributes; j++){
				if(maximums[j] - minimums[j] > 0){
					sample.getAttributes()[j] = (sample.getAttributes()[j] - minimums[j]) / (maximums[j] - minimums[j]);
				}else{//若最大值等于最小值，即只有一个值存在时，值置0
					sample.getAttributes()[j] = 0;
				}
			}
		}
	}

	public static void main(String[] args) {
        String filename = "C:\\Users\\18809\\Desktop\\test2\\Modbus_polling_only_6RTU.csv";
		DataReader reader = new DataReader();
        try {
            reader.readData(filename, 20);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(reader.getSamples());
	}
}
