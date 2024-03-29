package com.ditecting.attackclassification;

import com.ditecting.attackclassification.anomalyclassification.*;
import com.ditecting.attackclassification.anomalydetection.LOF_AD;
import com.ditecting.attackclassification.anomalydetection.SAE_AD;
import com.ditecting.attackclassification.dataprocess.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.filechooser.FileSystemView;
import java.io.IOException;
import java.util.*;

@SpringBootApplication(scanBasePackages = {"com.ditecting.*"})
public class AttackClassificationApplication  implements CommandLineRunner {

    @Autowired
    private Preprocessor preprocessor;

    @Autowired
    private ExtractorADData extractorADData;

    @Autowired
    private ExtractorICSADData extractorICSADData;

    @Autowired
    private ExtractorICSSessionADData extractorICSSessionADData;

    @Autowired
    private FileLoader loader;

    @Autowired
    private SCADAData scadaData;

    @Autowired
    private ScanningToolData scanningToolData;

    @Autowired
    private HoneypotData honeypotData;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AttackClassificationApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("AttackClassificationApplication !!!");

        /* Extractor */
        callExtractor();

        /* Preprocessor */
//        callPreprocessor();

        /* SAE */
//        callSAE_AD();

        /* DPCSD */
//        callDPCSD();

        /* Evaluator*/
//        callEvaluator();

        System.out.println("");
    }

    public void callEvaluator () throws IOException {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        /* Evaluate our method
        String resultsPath = desktopPath + "\\experiment5\\exp5\\1 org\\all_attacks_encode_14-6_result_dpcsd_myDc-0.03_Max-100.csv";
        Evaluator.evaluate(resultsPath);*/

        /* Evaluate other methods*/
        int trainingIndex = 4680;
        String outlierResultsPath = desktopPath + "\\experiment5\\exp5\\DBSCAN\\5 org\\all_data_encode_14-6_result_DBSCAN_eps-0.03_minps-2.csv";
        Evaluator.evaluate(trainingIndex, outlierResultsPath);

    }

    public void callDPCSD () throws IOException, InterruptedException {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePath = desktopPath + "\\experiment5\\exp5\\5 org\\run1_6rtu(1)_ef_oh_norm_encode_14-6.csv";
        int labelIndex = 6;// from 0
        int KNC = 100000;
        double percentage = 1;
        int batchSize = 1000;
        double myDc = 0.03;
        DensityPeakClusterStrictDistributed dpcsd = new DensityPeakClusterStrictDistributed();
        dpcsd.init(trainFilePath, labelIndex, batchSize, percentage, myDc);
        dpcsd.train();
//        String trainingResultPath = desktopPath + "\\experiment3\\exp3\\allNormal-allTesting\\all_data_result_dpcsd_myDc-"+myDc+ ".csv";
//        dpcsd.output(dpcsd.getInputSamples(), trainingResultPath);

        String testFileName = "all_attacks_encode_14-6";
        String testFilePath = desktopPath + "\\experiment5\\exp5\\5 org\\"+ testFileName +".csv";
        int Maximum = 100;
        List<Sample> testingSamples = dpcsd.predict(testFilePath, labelIndex, KNC, Maximum);
//        List<Sample> testingSamples = dpcsd.test(testFilePath, labelIndex, KNC);

        dpcsd.evaluate(testingSamples);
        String outPathResult = desktopPath + "\\experiment5\\exp5\\5 org\\"+ testFileName +"_result_dpcsd_myDc-"+myDc+"_Max-" + Maximum + ".csv";
        dpcsd.output(testingSamples, outPathResult);
    }

    public void callSAE_AD () throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        int first = 28;
        int second = 14;
        int third = 6;
        SAE_AD saeAD = new SAE_AD(first, second, third, 0);
        String trainFilePath = desktopPath + "\\experiment5\\exp4-1\\DAE\\run1_6rtu(1)_ef_oh_norm.csv";
        String encodeFileName = "send_a_fake_command_modbus_6RTU_with_operate_ef_oh_norm";
        String encodeFilePath = desktopPath + "\\experiment5\\exp4-1\\DAE\\"+ encodeFileName +".csv";
        String outPathEncode = desktopPath + "\\experiment5\\exp4-1\\DAE\\"+ encodeFileName + "_encode_" + second + "-" +third +".csv";
        int labelIndex = 28;
        int numClasses = 1;
        int batchSizeTraining = 100;
        int batchSizeTesting = 100;
        saeAD.train(trainFilePath, labelIndex, numClasses, batchSizeTraining);
        saeAD.encode(encodeFilePath,  labelIndex, numClasses, batchSizeTesting, outPathEncode);

//        saeAD.evaluate(testFilePathNo, testFilePathLabel, cutOffValue);
//        saeAD.test(testFilePath, 20, 2, 1000);
//        saeAD.output(testFilePathNo, testFilePathLabel, outPathResult, cutOffValue);
    }

    public void callPreprocessor () throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();

        /* Combine csv files
        String allPath = desktopPath + "\\experiment5\\exp1\\flow-ICS\\all data\\dealed\\";
        String allName = "all_attack_file";
        boolean includeHeader = true;
        String outputPath = desktopPath + "\\experiment5\\exp1\\flow-ICS\\all data\\dealed\\all_attacks.csv";
        preprocessor.combineCSVFiles (allPath, allName, includeHeader, outputPath);*/

        /* Transform Data By ICSSession*/
        String innerPath = desktopPath + "\\experiment5\\exp4-1\\all data\\run1_6rtu(1).csv";
        Set<String> innerIpSet = preprocessor.getIpSet(innerPath, true);
        String allPath = desktopPath + "\\experiment5\\exp4-1\\all data\\";
        String allName = "all files.csv";
        int classIndex = 0;
        boolean includeHeader = true;
        preprocessor.transformSCADADataInICSSession(innerIpSet, allPath, allName, classIndex, includeHeader);

        /* Transform Data By ICS
        String innerPath = desktopPath + "\\experiment5\\exp1\\flow-ICS\\all data\\run1_6rtu(1).csv";
        Set<String> innerIpSet = preprocessor.getIpSet(innerPath, true);
        String allPath = desktopPath + "\\experiment5\\exp1\\flow-ICS\\all data\\";
        String allName = "all files.csv";
        int classIndex = 0;
        boolean includeHeader = true;
        preprocessor.transformSCADADataInICS(innerIpSet, allPath, allName, classIndex, includeHeader);*/

        /* Transform Data By IT
        List<String> inPathList = new ArrayList<>();
        String inPath1 = desktopPath + "\\experiment\\exp1\\IT\\channel_4d_12s(2)";
        String inPath2 = desktopPath + "\\experiment\\exp1\\IT\\characterization_modbus_6RTU_with_operate";
        String inPath3 = desktopPath + "\\experiment\\exp1\\IT\\CnC_uploading_exe_modbus_6RTU_with_operate";
        String inPath4 = desktopPath + "\\experiment\\exp1\\IT\\exploit_ms08_netapi_modbus_6RTU_with_operate";
        String inPath5 = desktopPath + "\\experiment\\exp1\\IT\\moving_two_files_modbus_6RTU";
        String inPath6 = desktopPath + "\\experiment\\exp1\\IT\\run1_6rtu(1)";
        String inPath7 = desktopPath + "\\experiment\\exp1\\IT\\send_a_fake_command_modbus_6RTU_with_operate";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        inPathList.add(inPath3);
        inPathList.add(inPath4);
        inPathList.add(inPath5);
        inPathList.add(inPath6);
        inPathList.add(inPath7);
        int classIndex = 0;
        boolean includeHeader = true;
        preprocessor.transformSCADAData(inPathList, classIndex, includeHeader);*/

        /* Normalize data
        List<String> inPathList = new ArrayList<>();
        String inPath1 = desktopPath + "\\experiment\\exp1\\IT\\channel_4d_12s(2)_ef";
        String inPath2 = desktopPath + "\\experiment\\exp1\\IT\\characterization_modbus_6RTU_with_operate_ef";
        String inPath3 = desktopPath + "\\experiment\\exp1\\IT\\CnC_uploading_exe_modbus_6RTU_with_operate_ef";
        String inPath4 = desktopPath + "\\experiment\\exp1\\IT\\exploit_ms08_netapi_modbus_6RTU_with_operate_ef";
        String inPath5 = desktopPath + "\\experiment\\exp1\\IT\\moving_two_files_modbus_6RTU_ef";
        String inPath6 = desktopPath + "\\experiment\\exp1\\IT\\run1_6rtu(1)_ef";
        String inPath7 = desktopPath + "\\experiment\\exp1\\IT\\send_a_fake_command_modbus_6RTU_with_operate_ef";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        inPathList.add(inPath3);
        inPathList.add(inPath4);
        inPathList.add(inPath5);
        inPathList.add(inPath6);
        inPathList.add(inPath7);
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        preprocessor.normalize(inPathList, false, true, classIndex, includeHeader, options);*/
    }

    public void callExtractor () throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();

        /* scanning tools*/
        String filename = "scadascan-master_4";
        String inPath = desktopPath + "\\exp data\\attack tool\\"+ filename +".pcap";
        String outPath = desktopPath + "\\exp data\\attack tool\\"+ filename +".csv";
        String outPathNo = desktopPath + "\\exp data\\attack tool\\"+ filename +"_no.csv";
        int data_class = 5;
        List<String> stringList = scanningToolData.convertData(inPath);
//        extractorICSADData.extract(stringList, outPath, outPathNo, data_class);
        extractorICSSessionADData.extract(stringList, outPath, outPathNo, data_class);

        /* channel_4d_12s
        String filename = "channel_4d_12s(2)";
        String inPath = desktopPath + "\\experiment5\\exp1\\flow-ICS\\SCADA\\"+ filename +".pcap";
        String outPath = desktopPath + "\\experiment5\\exp1\\flow-ICS\\SCADA\\"+ filename +".csv";
        String outPathNo = desktopPath + "\\experiment5\\exp1\\flow-ICS\\SCADA\\"+ filename +"_no.csv";
        int data_class = 6;
        List<String> stringList = scadaData.convertData(inPath);
        extractorICSADData.extract(stringList, outPath, outPathNo, data_class);
//        extractorICSSessionADData.extract(stringList, outPath, outPathNo, data_class);*/

        /* has label file
        String filename = "run1_6rtu(1)";
        String inPath = desktopPath + "\\experiment5\\exp4-1\\"+ filename +".pcap";
        String inPathLabel = desktopPath + "\\experiment5\\exp4-1\\"+ filename +"_labeled.csv";
        String outPath = desktopPath + "\\experiment5\\exp4-1\\"+ filename +".csv";
        String outPathNo = desktopPath + "\\experiment5\\exp4-1\\"+ filename +"_no.csv";
//        extractorADData.extract(inPath, inPathLabel, outPath, outPathNo);
//        extractorICSADData.extract(inPath, inPathLabel, outPath, outPathNo);
        extractorICSSessionADData.extract(inPath, inPathLabel, outPath, outPathNo);*/

        /*honeypot
        String filename = "modbus_2017-2018";
        String inPath = desktopPath + "\\experiment5\\honeypot\\"+ filename +".pcap";
        String orgPath = desktopPath + "\\experiment5\\honeypot\\honeypot-info.csv";
        int data_class = 14;
        Map<String, List<String>> orgModificationsMap = honeypotData.convertData(inPath, orgPath);
        for(Map.Entry<String, List<String>> entry : orgModificationsMap.entrySet()){
            String outPath = desktopPath + "\\experiment5\\honeypot\\"+ entry.getKey() +".csv";
            String outPathNo = desktopPath + "\\experiment5\\honeypot\\"+ entry.getKey() +"_no.csv";
//            extractorICSADData.extract(entry.getValue(), outPath, outPathNo, data_class++);
            extractorICSSessionADData.extract(entry.getValue(), outPath, outPathNo, data_class++);
        }*/
    }

    /* Deprecated */
//    public void callDPCS () throws IOException{
//        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
//        String trainFilePathLabel = desktopPath + "\\test2\\dealed\\run1_6rtu(1)_ef_oh_norm.csv";
//        int trainIndex = -1;
//        String trainFilePath = null;
//        int labelIndex = 90;
//        int KNC = 10;
//        double percentage = 0.018;
//        DensityPeakClusterStrict dpcs = new DensityPeakClusterStrict();
//        dpcs.train(trainFilePathLabel, trainFilePath, labelIndex, trainIndex, percentage);
////        String modelFilePath = "C:\\Users\\18809\\Desktop\\test5\\DPCS.model";
////        ModelIO.outputModel(modelFilePath, DPCS);
////        DensityPeakClusterStrict DPCS = (DensityPeakClusterStrict) ModelIO.inputModel(modelFilePath);
//        String testsFilePath = desktopPath + "\\test2\\dealed\\moving_two_files_modbus_6RTU_ef_oh_norm_result_lof_KNN-20_CV-3.0_outlier.csv";
//        dpcs.test(testsFilePath, labelIndex, KNC);
//        dpcs.evaluate();
////        String outPathResult = desktopPath + "\\test2\\dealed\\moving_two_files_modbus_6RTU_ef_oh_norm_result_lof_KNN-20_CV-3.0_outlier_result_dpcs.csv";
////        dpcs.output(outPathResult);
//
//        System.out.println("KNC: " + KNC);
//        System.out.println("percentage: " + percentage);
//    }
    /* Deprecated */
//    public void callLOF_AD () throws Exception {
//        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
//
//        int KNN = 15;
//        int classIndex = 0;
//        boolean includeHeader = true;
//        String[] options = new String[]{"-R", "first-last"};
//        double cutOffValue = 2;
//        /* Build LOF model*/
//        LOF_AD lofAD = new LOF_AD(0);
//        String trainFilePath = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\run1_6rtu(1)_ef_oh_norm.csv";
//        lofAD.train(trainFilePath, KNN, KNN, classIndex, includeHeader, options);
//
//        /* Evaluate training data*/
//        lofAD.evaluateTrainingData(cutOffValue, KNN, true);
//        String outPathOutliers = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\run1_6rtu(1)_ef_oh_norm_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +"_outlier.csv";
//        String outPathOutliersNo = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\run1_6rtu(1)_ef_oh_norm_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +"_outlier_no.csv";
//        lofAD.outputOutliers(outPathOutliers, outPathOutliersNo);
//
//        /* Test testing data*/
//        String testFileName = "all_attacks";
//        String testFilePath = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\" + testFileName + ".csv";
//        lofAD.test(testFilePath, classIndex, includeHeader, options);
//        lofAD.evaluate(cutOffValue);
//        String outPathResult = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\" + testFileName + "_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +".csv";
//        String outPathInner = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\" + testFileName + "_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +"_inner.csv";
//        String outPathInnerNo = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\" + testFileName + "_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +"_inner_no.csv";
//        String outPathOutlier = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\" + testFileName + "_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +"_outlier.csv";
//        String outPathOutlierNo = desktopPath + "\\experiment4\\exp2\\full\\KNN-15_CV-2.0\\" + testFileName + "_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +"_outlier_no.csv";
////        String outPathInner = null;
////        String outPathInnerNo = null;
////        String outPathOutlier = null;
////        String outPathOutlierNo = null;
//        lofAD.output(outPathResult, outPathInner, outPathInnerNo, outPathOutlier, outPathOutlierNo, cutOffValue);
//    }
}
