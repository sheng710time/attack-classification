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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/10/6 9:04
 */
@Component
@Slf4j
public class ExtractorICSADData {
    @Autowired
    private LoadHolder loadHolder;

    @Autowired
    private PluginCachePool pluginCachePool;

    public void extract(List<String> stringFlowList, String outPath, String outPathNo, int data_class) throws InterruptedException {
        //extract
        List<List<Integer>> numbersList = new ArrayList<>();
        List<ICSADData> icsadDataList = new ArrayList<>();
        for(String stringFlow : stringFlowList){
            if(filter(stringFlow)){//filter condition: Json format
                numbersList.add(extractNumbers(stringFlow));
                icsadDataList.add(extractICSADData(stringFlow, data_class));
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
        strDataList.add(ICSADData.getHeader());
        for(ICSADData icsadData : icsadDataList){
            strDataList.add(icsadData.toStrings());
        }

        CSVUtil.write(outPath, strDataList);
    }

    public void extract(String inPath, String outPath, String outPathNo, int data_class) throws InterruptedException {
        //input data from PCAP file
        loadHolder.load(inPath);
        List<String> stringFlowList = pluginCachePool.getAllString();

        //extract
        List<List<Integer>> numbersList = new ArrayList<>();
        List<ICSADData> icsadDataList = new ArrayList<>();
        for(String stringFlow : stringFlowList){
            if(filter(stringFlow)){//filter condition: Json format
                numbersList.add(extractNumbers(stringFlow));
                icsadDataList.add(extractICSADData(stringFlow, data_class));
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
        strDataList.add(ICSADData.getHeader());
        for(ICSADData icsadData : icsadDataList){
            strDataList.add(icsadData.toStrings());
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
        List<ICSADData> icsadDataList = new ArrayList<>();
        for(String stringFlow : stringFlowList){
            if(filter(stringFlow)){//filter condition: 1) Json format, 2) non-udp
                List<Integer> numbers = extractNumbers(stringFlow);
                numbersList.add(numbers);
                int data_class = extractDataClass(numbers, labels);
                icsadDataList.add(extractICSADData(stringFlow, data_class));
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
        strDataList.add(ICSADData.getHeader());
        for(ICSADData icsadData : icsadDataList){
            strDataList.add(icsadData.toStrings());
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
    private ICSADData extractICSADData(String stringFlow, int data_class) {
        JsonObject jsonObject = JsonParser.parseString(stringFlow).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("segments");
        int prot = jsonObject.get("prot").getAsInt();
        int dstPort;
        String src_addr, dst_addr;
        if(prot == 1){// tcp
            JsonObject jsonFirst = jsonArray.get(0).getAsJsonObject();
            JsonObject header = jsonFirst.get("tcpHeader").getAsJsonObject();
            String SYN = header.get("SYN").getAsString();
            String ACK = header.get("ACK").getAsString();
            if(SYN.equals("true") && ACK.equals("true")){ // second handshake: SYN && ACK
                dstPort = jsonObject.get("srcPort").getAsInt();
                dst_addr = jsonObject.get("srcIp").getAsString();
                src_addr = jsonObject.get("dstIp").getAsString();
            }else {
                dstPort =  jsonObject.get("dstPort").getAsInt();
                src_addr = jsonObject.get("srcIp").getAsString();
                dst_addr = jsonObject.get("dstIp").getAsString();
            }
        }else{// udp
            dstPort =  jsonObject.get("dstPort").getAsInt();
            src_addr = jsonObject.get("srcIp").getAsString();
            dst_addr = jsonObject.get("dstIp").getAsString();
        }

        List<JsonObject> srcList = new ArrayList<>();
        List<JsonObject> dstList = new ArrayList<>();

        List<Transition> transitions = new ArrayList<>();
        for(int i=0; i<jsonArray.size(); i++){
            JsonObject segment = jsonArray.get(i).getAsJsonObject();
            if(segment.get("srcIp").getAsString().equals(src_addr)){
                srcList.add(segment);
                List<Transition> myTransitions = ModbusTcpParser.segmentToTransitions(segment);
                if(myTransitions != null){
                    transitions.addAll(myTransitions);
                }
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

        /*ICS features*/
        int read_times = 0;
        int read_num_location = 0;
        int read_length_location = 0;
        int write_times = 0;
        int write_num_location = 0;
        int write_length_location = 0;
        List<StorageBlock> readSBList = new ArrayList<>();
        List<StorageBlock> writeSBList = new ArrayList<>();
        for(Transition transition : transitions){
            if(transition.getOperation() == 1){// read
                read_times++;
                int overlap = overlapSBInList(readSBList, transition.getStorageBlock());
                if(overlap < transition.getStorageBlock().getBitLength()) {
                    read_num_location++;
                    read_length_location += transition.getStorageBlock().getBitLength() - overlap;
                    readSBList.add(transition.getStorageBlock());
                }
            }else {// write
                write_times++;
                int overlap = overlapSBInList(writeSBList, transition.getStorageBlock());
                if(overlap < transition.getStorageBlock().getBitLength()){
                    write_num_location++;
                    write_length_location += transition.getStorageBlock().getBitLength() - overlap;
                    writeSBList.add(transition.getStorageBlock());
                }
            }
        }

        ICSADData icsadData = ICSADData.builder()
                .src_addr(src_addr)
                .dst_addr(dst_addr)
                .prot_type(prot)
                .dst_port(dstPort)
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
                .read_times(read_times)
                .read_num_location(read_num_location)
                .read_length_location(read_length_location)
                .write_times(write_times)
                .write_num_location(write_num_location)
                .write_length_location(write_length_location)
                .data_class(data_class)
                .build();

        return icsadData;
    }

    private int overlapSBInList(List<StorageBlock> writeSBList, StorageBlock mySB) {
        int overlap = 0;
        for(StorageBlock sb : writeSBList){
            if(sb.overlapped(mySB) > overlap){
                overlap = sb.overlapped(mySB);
            }
        }
        return overlap;
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
    private int getSrcPort(JsonObject segment){
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