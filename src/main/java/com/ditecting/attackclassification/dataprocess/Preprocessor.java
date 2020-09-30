package com.ditecting.attackclassification.dataprocess;

import com.ditecting.honeyeye.cachepool.PluginCachePool;
import com.ditecting.honeyeye.inputer.loader.LoadHolder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private LoadHolder loadHolder;

    @Autowired
    private PluginCachePool pluginCachePool;

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

    public void extract(String inPath, String outPath, String outPathNo, int data_class) throws InterruptedException {
        //input data from PCAP file
        loadHolder.load(inPath);
        List<String> stringFlowList = pluginCachePool.getAllString();

        //extract
        List<List<Integer>> numbersList = new ArrayList<>();
        List<ADData> adDataList = new ArrayList<>();
        for(String stringFlow : stringFlowList){
            if(filter(stringFlow)){//filter condition: 1) Json format, 2) non-udp
                List<Integer> numbers = extractNumbers(stringFlow);
                numbersList.add(extractNumbers(stringFlow));
                adDataList.add(extractADData(stringFlow, data_class));
            }
        }

        //output data to csv file
        List<String[]> strNumbersList = new ArrayList<>();
        strNumbersList.add(new String[]{"flowNo", "packetNo"});
        for( int a=0; a<numbersList.size(); a++){
            for(int number : numbersList.get(a)){
                strNumbersList.add(new String[]{a+"", number+""});
            }
        }
        CSVUtil.write(outPathNo, strNumbersList);

        //output data to csv file
        List<String[]> strDataList = new ArrayList<>();
        strDataList.add(ADData.getHeader());
        for(ADData adData : adDataList){
            strDataList.add(adData.toStrings());
        }

        CSVUtil.write(outPath, strDataList);
    }

    public void extract(String inPath, String inPathLabel, String outPath, String outPathNo) throws InterruptedException {
        //input data from PCAP file
        loadHolder.load(inPath);
        List<String> stringFlowList = pluginCachePool.getAllString();
        Map<Integer, Integer> labels = extractLabels(inPathLabel);

        //extract
        List<List<Integer>> numbersList = new ArrayList<>();
        List<ADData> adDataList = new ArrayList<>();
        for(String stringFlow : stringFlowList){
            if(filter(stringFlow)){//filter condition: 1) Json format, 2) non-udp
                List<Integer> numbers = extractNumbers(stringFlow);
                numbersList.add(extractNumbers(stringFlow));
                int data_class = extractDataClass(numbers, labels);
                adDataList.add(extractADData(stringFlow, data_class));
            }
        }

        //output data to csv file
        List<String[]> strNumbersList = new ArrayList<>();
        strNumbersList.add(new String[]{"flowNo", "packetNo"});
        for( int a=0; a<numbersList.size(); a++){
            for(int number : numbersList.get(a)){
                strNumbersList.add(new String[]{a+"", number+""});
            }
        }
        CSVUtil.write(outPathNo, strNumbersList);

        //output data to csv file
        List<String[]> strDataList = new ArrayList<>();
        strDataList.add(ADData.getHeader());
        for(ADData adData : adDataList){
            strDataList.add(adData.toStrings());
        }

        CSVUtil.write(outPath, strDataList);
    }

    /**
     * extract data_class of flow
     * @param numbers
     * @param labels
     * @return
     */
    public int extractDataClass (List<Integer> numbers, Map<Integer, Integer> labels) {
        Map<Integer, Integer> votes = new HashMap<>();
        for(int number : numbers){
            if(votes.containsKey(labels.get(number))){
                votes.put(labels.get(number), votes.get(labels.get(number))+1);
            }else{
                votes.put(labels.get(number), 1);
            }
        }
        int max = -1;
        int data_class = -1;

        for(Map.Entry<Integer, Integer> vote : votes.entrySet()){
            if(vote.getValue() > max){
                max = vote.getValue();
                data_class = vote.getKey();
            }
        }

        return data_class;
    }

    /**
     * extract labels from label file
     * @param inPathLabel
     * @return
     */
    public Map<Integer, Integer> extractLabels (String inPathLabel) {
        List<String> strList = CSVUtil.read(inPathLabel, false);
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

    /**
     * extract ADData from flow
     * @param stringFlow
     * @param data_class
     * @return
     */
    private ADData extractADData(String stringFlow, int data_class) {
        JsonObject jsonObject = JsonParser.parseString(stringFlow).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("segments");
        int srcPort = jsonObject.get("srcPort").getAsInt();
        int dstPort = jsonObject.get("dstPort").getAsInt();
        List<JsonObject> srcList = new ArrayList<>();
        List<JsonObject> dstList = new ArrayList<>();

        for(int i=0; i<jsonArray.size(); i++){
            JsonObject segment = jsonArray.get(i).getAsJsonObject();
            if(getSrcPort(segment) == srcPort){
                srcList.add(segment);
            }else {
                dstList.add(segment);
            }
        }

        /*Packets*/
        int src_num_packet = srcList.size();
        int dst_num_packet = dstList.size();

        /*Bytes & Packet Sizes*/
        /*src*/
        int src_num_byte;
        int src_size_max_packet;
        int src_size_min_packet;
        double src_size_mean_packet;
        double src_size_stddev_packet;
        if(src_num_packet == 0){
            src_num_byte = 0;
            src_size_max_packet = 0;
            src_size_min_packet = 0;
            src_size_mean_packet = 0;
            src_size_stddev_packet = 0;
        }else{
            int[] srcSizes = new int[src_num_packet];
            src_num_byte = 0;
            src_size_max_packet = Integer.MIN_VALUE;
            src_size_min_packet = Integer.MAX_VALUE;
            for(int a=0; a<src_num_packet; a++){
                int size = srcList.get(a).get("size").getAsInt();
                srcSizes[a] = size;
                src_num_byte += size;
                if(size > src_size_max_packet){
                    src_size_max_packet = size;
                }
                if(size < src_size_min_packet){
                    src_size_min_packet = size;
                }
            }
            src_size_mean_packet = ((double)src_num_byte)/src_num_packet;
            double srcSizeDiff = 0;
            for(int a=0; a<src_num_packet; a++) {
                srcSizeDiff+=(srcSizes[a]-src_size_mean_packet)*(srcSizes[a]-src_size_mean_packet);
            }
            src_size_stddev_packet = Math.sqrt(srcSizeDiff/src_num_packet);
        }
        /*dst*/
        int dst_num_byte;
        int dst_size_max_packet;
        int dst_size_min_packet;
        double dst_size_mean_packet;
        double dst_size_stddev_packet;
        if(dst_num_packet == 0){
            dst_num_byte = 0;
            dst_size_max_packet = 0;
            dst_size_min_packet = 0;
            dst_size_mean_packet = 0;
            dst_size_stddev_packet = 0;
        }else{
            int[] dstSizes = new int[dst_num_packet];
            dst_num_byte = 0;
            dst_size_max_packet = Integer.MIN_VALUE;
            dst_size_min_packet = Integer.MAX_VALUE;
            for(int b=0; b<dst_num_packet; b++){
                int size = dstList.get(b).get("size").getAsInt();
                dstSizes[b] = size;
                dst_num_byte += size;
                if(size > dst_size_max_packet){
                    dst_size_max_packet = size;
                }
                if(size < dst_size_min_packet){
                    dst_size_min_packet = size;
                }
            }
            dst_size_mean_packet = ((double)dst_num_byte) / dst_num_packet;
            double dstSizeDiff = 0;
            for(int a=0; a<dst_num_packet; a++) {
                dstSizeDiff+=(dstSizes[a]-dst_size_mean_packet)*(dstSizes[a]-dst_size_mean_packet);
            }
            dst_size_stddev_packet = Math.sqrt(dstSizeDiff/dst_num_packet);
        }

        /*Inter-Packet Time*/
        /*src*/
        double src_time_min_packet;
        double src_time_max_packet;
        double src_time_mean_packet;
        double src_time_stddev_packet;
        if(src_num_packet == 0 || src_num_packet == 1){
            src_time_min_packet = 0;
            src_time_max_packet = 0;
            src_time_mean_packet = 0;
            src_time_stddev_packet = 0;
        }else{
            double[] srcTimes = new double[src_num_packet-1];
            src_time_min_packet = Double.MAX_VALUE;
            src_time_max_packet = Double.MIN_VALUE;
            double src_time_packet = 0;
            for(int c=0; c<src_num_packet-1; c++) {
                double firstTime = srcList.get(c).get("time").getAsDouble();
                double secondTime = srcList.get(c+1).get("time").getAsDouble();
                double time = secondTime - firstTime;
                srcTimes[c] = time;
                src_time_packet += time;
                if(time < src_time_min_packet){
                    src_time_min_packet = time;
                }
                if(time > src_time_max_packet){
                    src_time_max_packet = time;
                }
            }
            src_time_mean_packet = src_time_packet/srcTimes.length;
            double srcTimeDiff = 0;
            for(int c=0; c<src_num_packet-1; c++) {
                srcTimeDiff+=(srcTimes[c]-src_time_mean_packet)*(srcTimes[c]-src_time_mean_packet);
            }
            src_time_stddev_packet = Math.sqrt(srcTimeDiff/(src_num_packet-1));
        }
        /*dst*/
        double dst_time_min_packet;
        double dst_time_max_packet;
        double dst_time_mean_packet;
        double dst_time_stddev_packet;
        if(dst_num_packet == 0 || dst_num_packet == 1){
            dst_time_min_packet = 0;
            dst_time_max_packet = 0;
            dst_time_mean_packet = 0;
            dst_time_stddev_packet = 0;
        }else{
            double[] dstTimes = new double[dst_num_packet-1];
            dst_time_min_packet = Double.MAX_VALUE;
            dst_time_max_packet = Double.MIN_VALUE;
            double dst_time_packet = 0;
            for(int d=0; d<dst_num_packet-1; d++) {
                double firstTime = dstList.get(d).get("time").getAsDouble();
                double secondTime = dstList.get(d+1).get("time").getAsDouble();
                double time = secondTime - firstTime;
                dstTimes[d] = time;
                dst_time_packet += time;
                if(time < dst_time_min_packet){
                    dst_time_min_packet = time;
                }
                if(time > dst_time_max_packet){
                    dst_time_max_packet = time;
                }
            }
            dst_time_mean_packet = dst_time_packet/dstTimes.length;
            double dstTimeDiff = 0;
            for(int d=0; d<dst_num_packet-1; d++) {
                dstTimeDiff+=(dstTimes[d]-dst_time_mean_packet)*(dstTimes[d]-dst_time_mean_packet);
            }
            dst_time_stddev_packet = Math.sqrt(dstTimeDiff/(dst_num_packet-1));
        }

        ADData adData = ADData.builder()
                .src_num_packet(src_num_packet)
                .dst_num_packet(dst_num_packet)
                .src_num_byte(src_num_byte)
                .dst_num_byte(dst_num_byte)
                .src_size_min_packet(src_size_min_packet)
                .src_size_max_packet(src_size_max_packet)
                .src_size_mean_packet(src_size_mean_packet)
                .src_size_stddev_packet(src_size_stddev_packet)
                .dst_size_min_packet(dst_size_min_packet)
                .dst_size_max_packet(dst_size_max_packet)
                .dst_size_mean_packet(dst_size_mean_packet)
                .dst_size_stddev_packet(dst_size_stddev_packet)
                .src_time_min_packet(src_time_min_packet)
                .src_time_max_packet(src_time_max_packet)
                .src_time_mean_packet(src_time_mean_packet)
                .src_time_stddev_packet(src_time_stddev_packet)
                .dst_time_min_packet(dst_time_min_packet)
                .dst_time_max_packet(dst_time_max_packet)
                .dst_time_mean_packet(dst_time_mean_packet)
                .dst_time_stddev_packet(dst_time_stddev_packet)
                .data_class(data_class)
                .build();
        return adData;
    }

    /**
     * extract numbers from flow
     * @param stringFlow
     * @return
     */
    private List<Integer> extractNumbers (String stringFlow){
        JsonObject jsonObject = JsonParser.parseString(stringFlow).getAsJsonObject();
        JsonArray segments = jsonObject.getAsJsonArray("segments");

        List<Integer> numbers = new ArrayList<>();
        for(int i=0; i<segments.size(); i++){
            JsonObject segment = segments.get(i).getAsJsonObject();
            numbers.add(segment.get("number").getAsInt());
        }

        return numbers;
    }

    /**
     * find the srcPort in the segment
     *
     * @param segment
     *
     * @return
     */
    private int getSrcPort(JsonObject segment){//TODO test
        if(segment.has("tcpHeader")){
            return segment.get("tcpHeader").getAsJsonObject().get("srcPort").getAsInt();
        }
        if(segment.has("udpHeader")){
            return segment.get("udpHeader").getAsJsonObject().get("srcPort").getAsInt();
        }

        return -1;
    }

    /**
     * filter condition: 1) Json format, 2) non-udp
     *
     * @param stringFlow
     *
     * @return
     */
    private boolean filter(String stringFlow){
        JsonElement jsonElement =  JsonParser.parseString(stringFlow);

        /* Json format */
        if(!jsonElement.isJsonObject()){
            return false;
        }

        /* non-prot */
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if(!jsonObject.has("prot")){
            return false;
        }

        return true;
    }
}