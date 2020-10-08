package com.ditecting.attackclassification.dataprocess;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

import java.io.File;
import java.util.*;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/5 14:28
 */
@Component
@Slf4j
public class Preprocessor {

    /**
     * combine some csv files
     * @param inPathList
     * @param includeHeader
     * @param outputPath
     */
    public void combineCSVFiles (List<String> inPathList, boolean includeHeader, String outputPath) {
        List<String[]> strsListAll = new ArrayList<>();

        for(int a=0; a<inPathList.size(); a++){
            List<String[]> strsList = CSVUtil.readMulti(inPathList.get(a), includeHeader);
            if(strsListAll.size() < 1 && includeHeader && strsList.size()>0){
                String[] header = new String[strsList.get(0).length];
                for(int b=0; b<header.length; b++){
                    header[b] = "attr" + (b+1);
                }
                strsListAll.add(header);
            }
            strsListAll.addAll(strsList);
        }

        CSVUtil.write(outputPath, strsListAll);
    }

    /**
     * Create a set containing size random numbers [0, capability)
     * @param capability
     * @param size
     * @param seed
     * @return
     */
    public Integer[] createSampleIndicesWithReplacement (int capability, int size, int seed){
        Integer[] indices = new Integer[size];
        Random rand = new Random(seed);
        for(int a=0; a<size; a++){
            indices[a] = (int)(rand.nextDouble() * capability);
        }
        return indices;
    }

    /**
     * Create a set containing size distinct random numbers [0, capability)
     * @param capability
     * @param size
     * @param seed
     * @return
     */
    public Integer[] createSampleIndicesWithoutReplacement (int capability, int size, int seed){
        if(capability < size){
            throw new IllegalArgumentException("capability is smaller than size.");
        }
        Random rand = new Random(seed);
        Set<Integer> indexSet = new HashSet<>();
        while(indexSet.size() < size){
            indexSet.add((int)(rand.nextDouble() * capability));
        }

        Integer[] indices = new Integer[size];
        int count = 0;
        for(int index : indexSet){
            indices[count++] =  index;
        }

        return indices;
    }

    /**
     * Create sample indices list
     * @param capabilities
     * @param size
     * @param seed
     * @param replacement
     * @return
     */
    public List<Integer[]> createSampleIndicesList (int[] capabilities, int size, int seed, boolean replacement){
        List<Integer[]> indicesList = new ArrayList<>();
        for(int a=0; a<capabilities.length; a++){
            Integer[] indices;
            if(replacement){
                indices = createSampleIndicesWithReplacement(capabilities[a], size, seed);
            }else {
                indices = createSampleIndicesWithoutReplacement(capabilities[a], size, seed);
            }
            indicesList.add(indices);
        }
        return indicesList;
    }

    /**
     * Sample part of the data
     * @param inPath
     * @param classIndex
     * @param includeHeader
     * @param options
     * @throws Exception
     */
    public void sampleNSLKDD (String inPath, int size, boolean replacement, int seed, int classIndex, boolean includeHeader, String[] options) throws Exception {
        Instances instAll = FileLoader.loadInstancesFromCSV(inPath+".csv", classIndex, includeHeader, options);

        Map<Integer, List<Instance>> class_instancesMap = new HashMap<>();
        for(Instance inst : instAll){
            int inst_class = (int) inst.classValue();
            if(class_instancesMap.containsKey(inst_class)){
                class_instancesMap.get(inst_class).add(inst);
            }else {
                List<Instance> instances = new ArrayList<>();
                instances.add(inst);
                class_instancesMap.put(inst_class, instances);
            }
        }

        int[] capabilities = new int[class_instancesMap.size()];
        int count = 0;
        for(Map.Entry<Integer, List<Instance>> class_instances : class_instancesMap.entrySet()){
            capabilities[count++] = class_instances.getValue().size();
        }

        List<Integer[]> indicesList = createSampleIndicesList(capabilities, size, seed, replacement);

        List<Instance> samples = new ArrayList<>();
        int countB = 0;
        for(Map.Entry<Integer, List<Instance>> class_instances : class_instancesMap.entrySet()){
            for(int a=0; a<size; a++){
                samples.add(class_instances.getValue().get(indicesList.get(countB)[a]));
            }
            countB++;
        }

        /* Output data to csv file */
        int numAttributes = samples.get(0).numAttributes();
        String[] header = new String[numAttributes];
        for(int a=0; a<samples.get(0).numAttributes(); a++){
            header[a] = "attr" + (a+1);
        }
        int index = 0;
        List<String[]> strDataList = new ArrayList<>();
        strDataList.add(header);
        for(int b=0; b<samples.size(); b++) {
            Instance instance = samples.get(index++);
            String[] data = new String[numAttributes];
            for (int d = 0; d < numAttributes; d++) {
                data[d] = instance.toDoubleArray()[d] + "";
            }
            strDataList.add(data);
        }
        String suffix = "_sample_seed"+ seed +".csv";
        CSVUtil.write(inPath+suffix, strDataList);
    }

    /**
     * Discretize and one-hot encode data
     * @param inPathList
     * @param includeHeader
     * @throws Exception
     */
    public void transformNSLKDD (List<String> inPathList, int classIndex, boolean includeHeader) throws Exception {
        /* Create a temp CSV file to store all CSV files*/
        List<String[]> strsListAll = new ArrayList<>();
        int [] sizes = new int[inPathList.size()];
        for(int a=0; a<inPathList.size(); a++){
            List<String[]> strsList = CSVUtil.readMulti(inPathList.get(a)+".csv", includeHeader);
            sizes[a] = strsList.size();
            if(strsListAll.size() < 1 && includeHeader && strsList.size()>0){
                String[] header = new String[strsList.get(0).length];
                for(int b=0; b<header.length; b++){
                    header[b] = "attr" + (b+1);
                }
                strsListAll.add(header);
            }
            strsListAll.addAll(strsList);
        }
        String tempFileName = inPathList.get(0)+"_temp_"+System.currentTimeMillis()+".csv";
        CSVUtil.write(tempFileName, strsListAll);

        /* Load all instances from the temp CSV file */
        String[] optionsNominal = new String[]{"-N", "7,12,14,15,21,22"};
        Instances instAll = FileLoader.loadInstancesFromCSV(tempFileName, classIndex, includeHeader, optionsNominal);

        /* Discretize continuous data (eq freq) : numeric attr->nominal attr including data types and values */
        Discretize discretizeEF = new Discretize();
        discretizeEF.setOptions(new String[]{"-B", "100", "-R", "1,5,6,13,16", "-F"});//"-F" equal frequency method for discretization
        discretizeEF.setInputFormat(instAll);
        Instances instAll_EF = Filter.useFilter(instAll, discretizeEF);

        /* Discretize continuous data (eq width) : numeric attr->nominal attr including data types and values */
        Discretize discretizeED = new Discretize();
        discretizeED.setOptions(new String[]{"-B", "100", "-R", "10,17,23-41"});
        discretizeED.setInputFormat(instAll_EF);
        Instances instAll_EF_ED = Filter.useFilter(instAll_EF, discretizeED);

        /* One-hot encode unordered nominal data*/
        NominalToBinary nominalToBinary = new NominalToBinary();
        nominalToBinary.setOptions(new String[]{"-R", "2-4"});//counting begins at 1
        nominalToBinary.setInputFormat(instAll_EF_ED);
        Instances instAll_EF_ED_OH = Filter.useFilter(instAll_EF_ED, nominalToBinary);

        /* Output data to csv file */
        String[] header = new String[instAll_EF_ED_OH.get(0).numAttributes()];
        for(int a=0; a<instAll_EF_ED_OH.get(0).numAttributes(); a++){
            header[a] = "attr" + (a+1);
        }
        int index = 0;
        for(int b=0; b<inPathList.size(); b++) {
            List<String[]> strDataList = new ArrayList<>();
            strDataList.add(header);
            for (int c = 0; c < sizes[b]; c++) {
                Instance instance = instAll_EF_ED_OH.get(index++);
                String[] data = new String[instAll_EF_ED_OH.numAttributes()];
                for (int d = 0; d < instance.numAttributes(); d++) {
                    data[d] = instance.toDoubleArray()[d] + "";
                }
                strDataList.add(data);
            }

            String suffix = "_ef_ed_oh.csv";
            CSVUtil.write(inPathList.get(b)+suffix, strDataList);
        }

        /* Delete the temp file*/
        System.gc();
        File file = new File(tempFileName);
        boolean flag = false;
        while(!flag){
            flag = file.delete();
        }
    }

    /**
     * Discretize SCADA data
     * @param inPathList
     * @param includeHeader
     * @throws Exception
     */
    public void transformSCADADataInICS (List<String> inPathList, int classIndex, boolean includeHeader) throws Exception {
        /* Create a temp CSV file to store all CSV files*/
        List<String[]> strsListAll = new ArrayList<>();
        int [] sizes = new int[inPathList.size()];
        for(int a=0; a<inPathList.size(); a++){
            List<String[]> strsList = CSVUtil.readMulti(inPathList.get(a)+".csv", includeHeader);
            sizes[a] = strsList.size();
            if(strsListAll.size() < 1 && includeHeader && strsList.size()>0){
                String[] header = new String[strsList.get(0).length];
                for(int b=0; b<header.length; b++){
                    header[b] = "attr" + (b+1);
                }
                strsListAll.add(header);
            }
            strsListAll.addAll(strsList);
        }
        String tempFileName = inPathList.get(0)+"_temp_"+System.currentTimeMillis()+".csv";
        CSVUtil.write(tempFileName, strsListAll);

        /* Load all instances from the temp CSV file */
        String[] optionsNominal = new String[]{"-N", "1-4", "-R", "5-last"};
        Instances instAll = FileLoader.loadInstancesFromCSV(tempFileName, classIndex, includeHeader, optionsNominal);

        /* Discretize continuous data (eq freq) : numeric attr->nominal attr including data types and values */
        Discretize discretizeEF = new Discretize();
        discretizeEF.setOptions(new String[]{"-B", "100", "-R", "first-last", "-F"});//"-F" equal frequency method for discretization
        discretizeEF.setInputFormat(instAll);
        Instances instAll_EF = Filter.useFilter(instAll, discretizeEF);

        /* One-hot encode unordered nominal data*/
        NominalToBinary nominalToBinary = new NominalToBinary();
        nominalToBinary.setOptions(new String[]{"-R", "1-4"});//counting begins at 1
        nominalToBinary.setInputFormat(instAll_EF);
        Instances instAll_EF_OH = Filter.useFilter(instAll_EF, nominalToBinary);

        /* Output data to csv file */
        String[] header = new String[instAll_EF_OH.get(0).numAttributes()];
        for(int a=0; a<instAll_EF_OH.get(0).numAttributes(); a++){
            header[a] = "attr" + (a+1);
        }
        int index = 0;
        for(int b=0; b<inPathList.size(); b++) {
            List<String[]> strDataList = new ArrayList<>();
            strDataList.add(header);
            for (int c = 0; c < sizes[b]; c++) {
                Instance instance = instAll_EF_OH.get(index++);
                String[] data = new String[instAll_EF_OH.numAttributes()];
                for (int d = 0; d < instance.numAttributes(); d++) {
                    data[d] = instance.toDoubleArray()[d] + "";
                }
                strDataList.add(data);
            }

            String suffix = "_ef_oh.csv";
            CSVUtil.write(inPathList.get(b)+suffix, strDataList);
        }

        /* Delete the temp file*/
        System.gc();
        File file = new File(tempFileName);
        boolean flag = false;
        while(!flag){
            flag = file.delete();
        }
    }

    /**
     * Discretize SCADA data
     * @param inPathList
     * @param includeHeader
     * @throws Exception
     */
    public void transformSCADAData (List<String> inPathList, int classIndex, boolean includeHeader) throws Exception {
        /* Create a temp CSV file to store all CSV files*/
        List<String[]> strsListAll = new ArrayList<>();
        int [] sizes = new int[inPathList.size()];
        for(int a=0; a<inPathList.size(); a++){
            List<String[]> strsList = CSVUtil.readMulti(inPathList.get(a)+".csv", includeHeader);
            sizes[a] = strsList.size();
            if(strsListAll.size() < 1 && includeHeader && strsList.size()>0){
                String[] header = new String[strsList.get(0).length];
                for(int b=0; b<header.length; b++){
                    header[b] = "attr" + (b+1);
                }
                strsListAll.add(header);
            }
            strsListAll.addAll(strsList);
        }
        String tempFileName = inPathList.get(0)+"_temp_"+System.currentTimeMillis()+".csv";
        CSVUtil.write(tempFileName, strsListAll);

        /* Load all instances from the temp CSV file */
        String[] optionsNominal = new String[]{"-R", "first-last"};
        Instances instAll = FileLoader.loadInstancesFromCSV(tempFileName, classIndex, includeHeader, optionsNominal);

        /* Discretize continuous data (eq freq) : numeric attr->nominal attr including data types and values */
        Discretize discretizeEF = new Discretize();
        discretizeEF.setOptions(new String[]{"-B", "100", "-R", "first-last", "-F"});//"-F" equal frequency method for discretization
        discretizeEF.setInputFormat(instAll);
        Instances instAll_EF = Filter.useFilter(instAll, discretizeEF);

        /* Output data to csv file */
        String[] header = new String[instAll_EF.get(0).numAttributes()];
        for(int a=0; a<instAll_EF.get(0).numAttributes(); a++){
            header[a] = "attr" + (a+1);
        }
        int index = 0;
        for(int b=0; b<inPathList.size(); b++) {
            List<String[]> strDataList = new ArrayList<>();
            strDataList.add(header);
            for (int c = 0; c < sizes[b]; c++) {
                Instance instance = instAll_EF.get(index++);
                String[] data = new String[instAll_EF.numAttributes()];
                for (int d = 0; d < instance.numAttributes(); d++) {
                    data[d] = instance.toDoubleArray()[d] + "";
                }
                strDataList.add(data);
            }

            String suffix = "_ef.csv";
            CSVUtil.write(inPathList.get(b)+suffix, strDataList);
        }

        /* Delete the temp file*/
        System.gc();
        File file = new File(tempFileName);
        boolean flag = false;
        while(!flag){
            flag = file.delete();
        }
    }

    /**
     * generate label file
     *
     * @param outPath
     * @param length
     * @param data_class
     */
    public void generateLabelFile (String outPath, int length, int data_class) {
        List<String[]> strDataList = new ArrayList<>();
        for(int a=1; a<=length; a++){
            strDataList.add(new String[]{a+";"+data_class});
        }
        CSVUtil.write(outPath, strDataList);
    }

    /**
     * stand before norm
     * @param inPathList
     * @param norm
     * @param stand
     * @throws Exception
     */
    public void normalize (List<String> inPathList, boolean stand, boolean norm, int classIndex, boolean includeHeader, String[] options) throws Exception {
        int [] sizes = new int[inPathList.size()];

        /* Load and merge all instances */
        Instances instAll = null;
        for(int a=0; a<inPathList.size(); a++){
            Instances inst = FileLoader.loadInstancesFromCSV(inPathList.get(a)+".csv", classIndex, includeHeader, options);
            sizes[a] = inst.size();
            if(instAll == null){
                instAll = inst;
            } else {
                for(int i = 0; i < inst.size(); ++i) {
                    instAll.add(inst.get(i));
                }
            }
        }

        String suffix = "";
        if(stand){
            suffix = suffix + "_stand";
            Standardize standardize = new Standardize();
            standardize.setInputFormat(instAll);
            instAll = Filter.useFilter(instAll,standardize);
        }

        if(norm){
            suffix = suffix + "_norm";
            Normalize normalize = new Normalize();
            normalize.setInputFormat(instAll);
            instAll = Filter.useFilter(instAll, normalize);
        }
        suffix = suffix + ".csv";

        /* Output data to csv file */
        String[] header = new String[instAll.get(0).numAttributes()];
        for(int a=0; a<instAll.get(0).numAttributes(); a++){
            header[a] = "attr" + (a+1);
        }
        int index = 0;
        for(int b=0; b<inPathList.size(); b++) {
            List<String[]> strDataList = new ArrayList<>();
            strDataList.add(header);
            for (int c = 0; c < sizes[b]; c++) {
                Instance instance = instAll.get(index++);
                String[] data = new String[instance.numAttributes()];
                for (int d = 0; d < instance.numAttributes(); d++) {
                    data[d] = instance.toDoubleArray()[d] + "";
                }
                strDataList.add(data);
            }
            CSVUtil.write(inPathList.get(b)+suffix, strDataList);
        }
    }
}