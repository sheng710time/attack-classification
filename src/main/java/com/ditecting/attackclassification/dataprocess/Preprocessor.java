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
     * @param allPath
     * @param allName
     * @param includeHeader
     * @param outputPath
     */
    public void combineCSVFiles (String allPath, String allName, boolean includeHeader, String outputPath) {
        List<String[]> allList = CSVUtil.readMulti(allPath + allName+".csv", includeHeader);

        List<String[]> strsListAll = new ArrayList<>();
        for(int a=0; a<allList.size(); a++){
            List<String[]> strsList = CSVUtil.readMulti(allPath+allList.get(a)[0]+".csv", includeHeader);
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
     * Get all involved IPs
     * @param innerPath
     * @param includeHeader
     * @return
     */
    public Set<String> getIpSet (String innerPath, boolean includeHeader){
        Set<String> innerIpSet = new HashSet<>();
        List<String[]> strsList = CSVUtil.readMulti(innerPath, includeHeader);
        for(int b=0; b<strsList.size(); b++){
            innerIpSet.add(strsList.get(b)[0]);//src_addr
            innerIpSet.add(strsList.get(b)[1]);//dst_addr
        }

        return innerIpSet;
    }

    /**
     *
     * @param allPath
     * @param allName
     * @param includeHeader
     * @throws Exception
     */
    public void transformSCADADataInICSSession (Set<String> innerIpSet, String allPath, String allName, int classIndex, boolean includeHeader) throws Exception {
        List<String[]> allList = CSVUtil.readMulti(allPath + allName, includeHeader);

        /* Create a temp CSV file to store all CSV files*/
        List<String[]> strsListAll = new ArrayList<>();
        int [] sizes = new int[allList.size()];
        for(int a=0; a<allList.size(); a++){
            List<String[]> strsList = CSVUtil.readMulti(allPath+allList.get(a)[0]+".csv", includeHeader);
            sizes[a] = strsList.size();
            if(strsListAll.size() < 1 && includeHeader && strsList.size()>0){
                String[] header = new String[strsList.get(0).length];
                for(int b=0; b<header.length; b++){
                    header[b] = "attr" + (b+1);
                }
                strsListAll.add(header);
            }
            for(int c=0; c<strsList.size(); c++){
                if(!innerIpSet.contains(strsList.get(c)[0])){// manipulate outer src_ip
                    strsList.get(c)[0] = "0.0.0.0";
                }
                if(!innerIpSet.contains(strsList.get(c)[1])){// manipulate outer dst_ip
                    strsList.get(c)[1] = "0.0.0.1";
                }
            }

            strsListAll.addAll(strsList);
        }
        String tempFileName = allPath+"_temp_"+System.currentTimeMillis()+".csv";
        CSVUtil.write(tempFileName, strsListAll);

        /* Load all instances from the temp CSV file */
        String[] optionsNominal = new String[]{"-N", "1-2", "-R", "3-last"};
        Instances instAll = FileLoader.loadInstancesFromCSV(tempFileName, classIndex, includeHeader, optionsNominal);

        /* Discretize continuous data (eq freq) : numeric attr->nominal attr including data types and values */
        Discretize discretizeEF = new Discretize();
        discretizeEF.setOptions(new String[]{"-B", "100", "-R", "3-30", "-F"});//"-F" equal frequency method for discretization
        discretizeEF.setInputFormat(instAll);
        Instances instAll_EF = Filter.useFilter(instAll, discretizeEF);



        /* One-hot encode unordered nominal data*/
        NominalToBinary nominalToBinary = new NominalToBinary();
        nominalToBinary.setOptions(new String[]{"-R", "1-2"});//counting begins at 1
        nominalToBinary.setInputFormat(instAll_EF);
        Instances instAll_EF_OH = Filter.useFilter(instAll_EF, nominalToBinary);

        /* Normalize all data */
        String[] tempHeader = new String[instAll_EF_OH.get(0).numAttributes()];
        for(int a=0; a<tempHeader.length; a++){
            tempHeader[a] = "attr" + (a+1);
        }
        List<String[]> strsListAll_EF_OH = new ArrayList<>();
        strsListAll_EF_OH.add(tempHeader);
        for(int b=0; b<instAll_EF_OH.size(); b++){
            Instance instance = instAll_EF_OH.get(b);
            String[] data = new String[instAll_EF_OH.numAttributes()];
            for (int d = 0; d < data.length; d++) {
                data[d] = instance.toDoubleArray()[d] + "";
            }
            strsListAll_EF_OH.add(data);
        }
        String tempFileName2 = allPath+"_temp2_"+System.currentTimeMillis()+".csv";
        CSVUtil.write(tempFileName2, strsListAll_EF_OH);
        /* Load all instances from the temp CSV file */
        String[] optionsNominal2 = new String[]{"-R", "first-last"};
        Instances instAll_EF_OH_Num = FileLoader.loadInstancesFromCSV(tempFileName2, classIndex, includeHeader, optionsNominal2);
        /* Normalize data */
        Normalize normalize = new Normalize();
        normalize.setInputFormat(instAll_EF_OH_Num);
        Instances instAll_EF_OH_NORM = Filter.useFilter(instAll_EF_OH_Num, normalize);

        /* Output data to csv file */
        String[] header = new String[instAll_EF_OH_NORM.numAttributes()];
        for(int a=0; a<header.length; a++){
            header[a] = "attr" + (a+1);
        }
        int index = 0;
        for(int b=0; b<allList.size(); b++) {
            List<String[]> strDataList = new ArrayList<>();
            strDataList.add(header);
            for (int c = 0; c < sizes[b]; c++) {
                Instance instance = instAll_EF_OH_NORM.get(index++);
                String[] data = new String[header.length];
                for (int d = 0; d < header.length; d++) {
                    data[d] = instance.toDoubleArray()[d] + "";
                }
                strDataList.add(data);
            }

            String suffix = "_ef_oh_norm.csv";
            CSVUtil.write(allPath+"\\dealed\\"+allList.get(b)[0]+suffix, strDataList);
        }

        /* Delete the temp file*/
        System.gc();
        File file1 = new File(tempFileName);
        File file2 = new File(tempFileName2);
        boolean flag = false;
        while(!flag){
            flag = file1.delete() & file2.delete();
        }
    }

    /**
     * Discretize SCADA data
     * @param allPath
     * @param allName
     * @param includeHeader
     * @throws Exception
     */
    public void transformSCADADataInICS (Set<String> innerIpSet, String allPath, String allName, int classIndex, boolean includeHeader) throws Exception {
        List<String[]> allList = CSVUtil.readMulti(allPath + allName, includeHeader);

        /* Create a temp CSV file to store all CSV files*/
        List<String[]> strsListAll = new ArrayList<>();
        int [] sizes = new int[allList.size()];
        for(int a=0; a<allList.size(); a++){
            List<String[]> strsList = CSVUtil.readMulti(allPath+allList.get(a)[0]+".csv", includeHeader);
            sizes[a] = strsList.size();
            if(strsListAll.size() < 1 && includeHeader && strsList.size()>0){
                String[] header = new String[strsList.get(0).length];
                for(int b=0; b<header.length; b++){
                    header[b] = "attr" + (b+1);
                }
                strsListAll.add(header);
            }
            for(int c=0; c<strsList.size(); c++){
                if(!innerIpSet.contains(strsList.get(c)[0])){// manipulate outer src_ip
                    strsList.get(c)[0] = "0.0.0.0";
                }
                if(!innerIpSet.contains(strsList.get(c)[1])){// manipulate outer dst_ip
                    strsList.get(c)[1] = "0.0.0.1";
                }
            }

            strsListAll.addAll(strsList);
        }
        String tempFileName = allPath+"_temp_"+System.currentTimeMillis()+".csv";
        CSVUtil.write(tempFileName, strsListAll);

        /* Load all instances from the temp CSV file */
        String[] optionsNominal = new String[]{"-N", "1-4", "-R", "5-last"};
        Instances instAll = FileLoader.loadInstancesFromCSV(tempFileName, classIndex, includeHeader, optionsNominal);

        /* Discretize continuous data (eq freq) : numeric attr->nominal attr including data types and values */
        Discretize discretizeEF = new Discretize();
        discretizeEF.setOptions(new String[]{"-B", "100", "-R", "5-last", "-F"});//"-F" equal frequency method for discretization
        discretizeEF.setInputFormat(instAll);
        Instances instAll_EF = Filter.useFilter(instAll, discretizeEF);

        /* One-hot encode unordered nominal data*/
        NominalToBinary nominalToBinary = new NominalToBinary();
        nominalToBinary.setOptions(new String[]{"-R", "1-4"});//counting begins at 1
        nominalToBinary.setInputFormat(instAll_EF);
        Instances instAll_EF_OH = Filter.useFilter(instAll_EF, nominalToBinary);

        /* Normalize all data */
        String[] tempHeader = new String[instAll_EF_OH.get(0).numAttributes()];
        for(int a=0; a<tempHeader.length; a++){
            tempHeader[a] = "attr" + (a+1);
        }
        List<String[]> strsListAll_EF_OH = new ArrayList<>();
        strsListAll_EF_OH.add(tempHeader);
        for(int b=0; b<instAll_EF_OH.size(); b++){
            Instance instance = instAll_EF_OH.get(b);
            String[] data = new String[instAll_EF_OH.numAttributes()];
            for (int d = 0; d < data.length; d++) {
                data[d] = instance.toDoubleArray()[d] + "";
            }
            strsListAll_EF_OH.add(data);
        }
        String tempFileName2 = allPath+"_temp2_"+System.currentTimeMillis()+".csv";
        CSVUtil.write(tempFileName2, strsListAll_EF_OH);
        /* Load all instances from the temp CSV file */
        String[] optionsNominal2 = new String[]{"-R", "first-last"};
        Instances instAll_EF_OH_Num = FileLoader.loadInstancesFromCSV(tempFileName2, classIndex, includeHeader, optionsNominal2);
        /* Normalize data */
        Normalize normalize = new Normalize();
        normalize.setInputFormat(instAll_EF_OH_Num);
        Instances instAll_EF_OH_NORM = Filter.useFilter(instAll_EF_OH_Num, normalize);

        /* Output data to csv file */
        String[] header = new String[instAll_EF_OH_NORM.numAttributes()];
        for(int a=0; a<header.length; a++){
            header[a] = "attr" + (a+1);
        }
        int index = 0;
        for(int b=0; b<allList.size(); b++) {
            List<String[]> strDataList = new ArrayList<>();
            strDataList.add(header);
            for (int c = 0; c < sizes[b]; c++) {
                Instance instance = instAll_EF_OH_NORM.get(index++);
                String[] data = new String[header.length];
                for (int d = 0; d < header.length; d++) {
                    data[d] = instance.toDoubleArray()[d] + "";
                }
                strDataList.add(data);
            }

            String suffix = "_ef_oh_norm.csv";
            CSVUtil.write(allPath+"\\dealed\\"+allList.get(b)[0]+suffix, strDataList);
        }

        /* Delete the temp file*/
        System.gc();
        File file1 = new File(tempFileName);
        File file2 = new File(tempFileName2);
        boolean flag = false;
        while(!flag){
            flag = file1.delete() & file2.delete();
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