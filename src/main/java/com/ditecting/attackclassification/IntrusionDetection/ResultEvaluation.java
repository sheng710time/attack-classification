package com.ditecting.attackclassification.IntrusionDetection;

import com.ditecting.attackclassification.dataprocess.CSVUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultEvaluation {

    public static void main(String[] args) {
        /* mapSessionToPacket
        String filename = "send_a_fake_command_modbus_6RTU_with_operate";
        String resultSessionFilePath = "E:\\Desktop\\experiment5\\exp4-1\\old\\" + filename + "_IDS.csv";
        String sessionNoFile = "E:\\Desktop\\experiment5\\exp4-1\\old\\" + filename + "_no.csv";
        String outputPath = "E:\\Desktop\\experiment5\\exp4-1\\old\\" + filename + "_result.csv";
        mapSessionToPacket(resultSessionFilePath, sessionNoFile, outputPath);*/

        /* evaluate*/
        String filename = "send_a_fake_command_modbus_6RTU_with_operate";
        String resultPacketPath = "E:\\Desktop\\experiment5\\exp4-1\\old\\" + filename + "_result.csv";
        String labelFile = "E:\\Desktop\\experiment5\\exp4-1\\old\\" + filename + "_labeled.csv";
        evaluate(resultPacketPath, labelFile);
    }
    /**
     * Map session results to packet results
     * @param resultSessionFilePath
     * @param sessionNoFile
     * @param outputPath
     */
    public static void mapSessionToPacket (String resultSessionFilePath, String sessionNoFile, String outputPath){
        List<String[]> resultsSession = CSVUtil.readMulti(resultSessionFilePath, true);
        List<String[]> sessionNoList = CSVUtil.readMulti(sessionNoFile, true);
        List<String[]> sessionNoListWithResult = new ArrayList<>();

        Map<String, String> resultMap = new HashMap<>();
        for(String[] session: resultsSession){
            resultMap.put(session[0], session[1]);
        }
        for(String[] sessionNo : sessionNoList){
            String[] sessionWithResult = new String[]{sessionNo[0], sessionNo[1], resultMap.get(sessionNo[0])};
            sessionNoListWithResult.add(sessionWithResult);
        }
        CSVUtil.write(outputPath, sessionNoListWithResult);
    }

    public static void evaluate (String resultPacketPath, String labelFile){//TODO test
        List<String[]> resultsPacket = CSVUtil.readMulti(resultPacketPath, false);
        List<String[]> labels = CSVUtil.read(labelFile, ';',false);

        Map<String, String> resultMap = new HashMap<>();
        for(String[] result: resultsPacket){
            resultMap.put(result[1], result[2]);
        }

        double TN = 0, TP = 0, FN = 0, FP = 0;
        for(String[] label : labels){
            if(label[1].equals("0")){
                if(!resultMap.containsKey(label[0])){
                    TN += 1;
                }else{
                    if(resultMap.get(label[0]).equals("0")){
                        TN += 1;
                    }else {
                        FP += 1;
                    }
                }

            }else {
                if(!resultMap.containsKey(label[0])){
                    FN += 1;
                }else {
                    if (!resultMap.get(label[0]).equals("0")) {
                        TP += 1;
                    } else {
                        FN += 1;
                    }
                }
            }
        }

        double detection_rate = TP / (TP+FN);
        double accuracy = (TP+TN) / (TP+TN+FP+FN);
        double f1 = 2*TP / (2*TP + FN + FP);

        System.out.println("TP: " + TP);
        System.out.println("TN: " + TN);
        System.out.println("FP: " + FP);
        System.out.println("FN: " + FN);
        System.out.println("detection_rate: " + detection_rate);
        System.out.println("accuracy: " + accuracy);
        System.out.println("f1: " + f1);
    }
}
