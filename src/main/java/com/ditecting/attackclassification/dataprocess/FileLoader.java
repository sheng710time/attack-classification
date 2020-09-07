package com.ditecting.attackclassification.dataprocess;

import org.springframework.stereotype.Component;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/7 11:41
 */
@Component
public class FileLoader {

    /**
     * load instances from csv file, includeHeader == true
     * @param path file path
     * @param classIndex the index of class attribute
     * @return
     * @throws Exception
     */
    public static Instances loadInstancesFromCSV(String path, int classIndex) throws Exception {
        return loadInstancesFromCSV(path, classIndex, true, null);
    }

    /**
     * load instances from csv file, , includeHeader = true, options = new String[]{"-R", "1-20", "-N", "last"}
     * @param path file path
     * @param classIndex the index of class attribute
     * @param  includeHeader true: include header, false: exclude header
     * @return
     * @throws Exception
     */
    public static Instances loadInstancesFromCSV(String path, int classIndex, boolean includeHeader) throws Exception {
        return loadInstancesFromCSV(path, classIndex, includeHeader, null);
    }

    /**
     * load instances from csv file
     * @param path file path
     * @param classIndex the index of class attribute
     * @param includeHeader true: include header, false: exclude header
     * @param options options
     * @return
     * @throws Exception
     */
    public static Instances loadInstancesFromCSV(String path, int classIndex, boolean includeHeader, String[] options) throws Exception {
         CSVLoader csv = new CSVLoader();
        csv.setSource(new File(path));
        if(options != null){
            csv.setOptions(options);
        }
        csv.setNoHeaderRowPresent(!includeHeader);
        Instances data = csv.getDataSet();
        if(classIndex == -1){
            data.setClassIndex(-1);
        }else{
            data.setClassIndex(data.numAttributes() - 1);
        }

        return data;
    }

    /**
     * load numbers from csv file
     * @param path
     * @return
     * @throws Exception
     */
    public static Map<Integer, List<Integer>> loadNumbersFromCSV(String path) throws Exception {
        List<String[]> strsList = CSVUtil.readMulti(path, true);
        Map<Integer, List<Integer>> numbersMap = new HashMap<>();
        strsList.forEach((strs)->{
            if(numbersMap.containsKey(Integer.parseInt(strs[0]))){
                numbersMap.get(Integer.parseInt(strs[0])).add(Integer.parseInt(strs[1]));
            }else{
                List<Integer> numbers = new ArrayList<>();
                numbers.add(Integer.parseInt(strs[1]));
                numbersMap.put(Integer.parseInt(strs[0]), numbers);
            }
        });

        return numbersMap;
    }

    /**
     * load labels from csv file
     * @param path
     * @return
     */
    public static Map<Integer, Integer> loadLabelsFromCSV (String path) {
        List<String> strList = CSVUtil.read(path, false);
        Map<Integer, Integer> labels = new HashMap<>();
        strList.forEach((str)->{
            int index = str.indexOf(';');
            if(index != -1){
                int number = Integer.parseInt(str.substring(0,index));
                int label = Integer.parseInt(str.substring(index+1,str.length()));
                labels.put(number, label);
            }
        });

        return labels;
    }
}