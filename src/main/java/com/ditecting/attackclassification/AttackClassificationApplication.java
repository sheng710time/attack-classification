package com.ditecting.attackclassification;

import com.ditecting.attackclassification.anomalyclassification.DensityPeakClusterStrict;
import com.ditecting.attackclassification.anomalyclassification.DensityPeakClusterStrictDistributed;
import com.ditecting.attackclassification.anomalyclassification.ModelIO;
import com.ditecting.attackclassification.anomalyclassification.Sample;
import com.ditecting.attackclassification.anomalydetection.LOF_AD;
import com.ditecting.attackclassification.anomalydetection.SAE_AD;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import com.ditecting.attackclassification.dataprocess.Preprocessor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(scanBasePackages = {"com.ditecting.*"})
@MapperScan("com.ditecting.honeyeye.dao")
public class AttackClassificationApplication  implements CommandLineRunner {

    @Autowired
    private Preprocessor preprocessor;

    @Autowired
    private FileLoader loader;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AttackClassificationApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("AttackClassificationApplication !!!");

        /* Preprocessor */
//        callPreprocessor();

        /* SAE */
//        callSAE_AD();

        /* LOF */
//        callLOF_AD ();

        /* DPC */
//        callDPCS();

        System.out.println("");
    }

    public void callDPCS () throws IOException{
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePathLabel = desktopPath + "\\test5\\KDDTrain+_edited_ef_ed_oh_norm_part.csv";
        int trainIndex = -1;
        String trainFilePath = null;
        int labelIndex = 122;
        int KNC = 50;
        double percentage = 0.015;
        DensityPeakClusterStrict dpcs = new DensityPeakClusterStrict();
        dpcs.train(trainFilePathLabel, trainFilePath, labelIndex, trainIndex, percentage);
//        String modelFilePath = "C:\\Users\\18809\\Desktop\\test5\\DPCS.model";
//        ModelIO.outputModel(modelFilePath, DPCS);
//        DensityPeakClusterStrict DPCS = (DensityPeakClusterStrict) ModelIO.inputModel(modelFilePath);
        String testsFilePath = desktopPath + "\\test5\\KDDTrain+_edited_ef_ed_oh_norm_part.csv";
        dpcs.test(testsFilePath, labelIndex, KNC);
        dpcs.evaluate();
//        String outPathResult = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_part_result_dpc.csv";
//        dpcs.output(outPathResult);

        System.out.println("KNC: " + KNC);
        System.out.println("percentage: " + percentage);
    }

    public void callDPCSD () throws IOException, InterruptedException {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        String trainFilePathLabel = desktopPath + "\\test5\\KDDTrain+_edited_ef_ed_oh_norm.csv";
        int trainIndex = -1;
        String trainFilePath = null;
        int labelIndex = 122;
        int KNC = 50;
        double percentage = 0.009;
        int batchSize = 1000;
        DensityPeakClusterStrictDistributed dpcsd = new DensityPeakClusterStrictDistributed();
        dpcsd.init(trainFilePathLabel, trainFilePath, labelIndex, trainIndex, batchSize, percentage);
        dpcsd.train();
        String modelFilePath = desktopPath + "\\test5\\dhcsd_"+dpcsd.getInputSamples().size()+"_"+batchSize+"_"+dpcsd.getDc()+".model";
        ModelIO.outputModel(modelFilePath, dpcsd);
//        DensityPeakClusterStrictDistributed dhcsd = (DensityPeakClusterStrictDistributed) ModelIO.inputModel(modelFilePath);
        String testsFilePath = desktopPath + "\\test5\\KDDTest+_edited_ef_ed_oh_norm.csv";
        List<Sample> testingSamples = dpcsd.test(testsFilePath, labelIndex, KNC);
        dpcsd.evaluate(testingSamples);
        String outPathResult = desktopPath + "\\test5\\KDDTest+_edited_ef_ed_oh_norm_result_dhcsd_3000.csv";
        dpcsd.output(testingSamples, outPathResult);

        System.out.println("KNC: " + KNC);
        System.out.println("percentage: " + percentage);
    }

    public void callSAE_AD () throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        int first = 20;
        int second = 8;
        int third = 2;
        SAE_AD saeAD = new SAE_AD(first, second, third, 0);
        String trainFilePath = desktopPath + "\\test3\\dealed\\run1_6rtu(1)_ef_norm.csv";
        String encodeFilePath = desktopPath + "\\test3\\dealed\\attacks.csv";
        String outPathEncode = desktopPath + "\\test3\\dealed\\attacks"+ "_encode_" + second + "-" +third +".csv";
        int labelIndex = 20;
        int numClasses = 1;
        int batchSizeTraining = 100;
        int batchSizeTesting = 100;
        saeAD.train(trainFilePath, labelIndex, numClasses, batchSizeTraining);
        saeAD.encode(encodeFilePath,  labelIndex, numClasses, batchSizeTesting, outPathEncode);

//        saeAD.evaluate(testFilePathNo, testFilePathLabel, cutOffValue);
//        saeAD.test(testFilePath, 20, 2, 1000);
//        saeAD.output(testFilePathNo, testFilePathLabel, outPathResult, cutOffValue);
    }

    public void callLOF_AD () throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        /* Build LOF model */
        LOF_AD lofAD = new LOF_AD(0);
        String trainFilePath = desktopPath + "\\test3\\dealed\\run1_6rtu(1)_ef_norm.csv";
        int KNN = 10;
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        lofAD.train(trainFilePath, KNN, KNN, classIndex, includeHeader, options);

        /* Evaluate training data */
        double cutOffValue = 1.1;
        lofAD.evaluateTrainingData(cutOffValue, KNN, true);
//        String outPathOutliers = desktopPath + "\\test3\\dealed\\run1_6rtu(1)_ef_norm_outliers_KNN-"+ KNN +"_CV-"+ cutOffValue +".csv";
//        lofAD.outputOutliers(outPathOutliers);

        /* Save LOF model */
//        String modelPath = desktopPath + "\\test2\\LOF.model";
//        lofAD.saveLOF(modelPath);
        /* Read LOF model */
//        LOF lof = LOF_AD.readLOF(modelPath);

        /* Test testing data
        String testFilePath = desktopPath + "\\test3\\dealed\\attacks.csv";
        lofAD.test(testFilePath, classIndex, includeHeader, options);
        lofAD.evaluate(cutOffValue);
        String outPathResult = desktopPath + "\\test3\\dealed\\attacks_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +".csv";
        lofAD.output(outPathResult, cutOffValue);*/

//        Instances predictedData = LOF_AD.test(lof, testFilePath, classIndex, includeHeader, options);
//        LOF_AD.evaluate(predictedData, testFilePathNo, testFilePathLabel, cutOffValue);
    }

    public void callPreprocessor () throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        /* deal with raw data*/
        /* has label file
        String filename = "send_a_fake_command_modbus_6RTU_with_operate";
        String inPath = desktopPath + "\\test3\\"+ filename +".pcap";
        String inPathLabel = desktopPath + "\\test3\\"+ filename +"_labeled.csv";
        String outPath = desktopPath + "\\test3\\"+ filename +".csv";
        String outPathNo = desktopPath + "\\test3\\"+ filename +"_no.csv";
        preprocessor.extract(inPath, inPathLabel, outPath, outPathNo);*/
        /* has no label file
        String inPath = desktopPath + "\\test3\\testqq3.pcap";
        String outPath = desktopPath + "\\test3\\testqq3.csv";
        String outPathNo = desktopPath + "\\test3\\testqq3_no.csv";
        int data_class = 0;
        preprocessor.extract(inPath, outPath, outPathNo, data_class);*/

        /* Generate label file
        String inPathLabel = desktopPath + "\\test2\\run1_6rtu(1)_labeled.csv";
        preprocessor.generateLabelFile(inPathLabel, 134690, 0);*/

        /* Transform NSLKDD
        List<String> inPathList = new ArrayList<>();
        String inPath1 = desktopPath + "\\test5\\KDDTrain+_edited";
        String inPath2 = desktopPath + "\\test5\\KDDTest+_edited";
//        String inPath3 = desktopPath + "\\test5\\KDDTrain+_edited_test_002";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
//        inPathList.add(inPath3);
        int classIndex = 0;
        boolean includeHeader = false;
        preprocessor.transformNSLKDD(inPathList, classIndex, includeHeader);*/

        /* Transform SCADA Data
        List<String> inPathList = new ArrayList<>();
        String inPath1 = desktopPath + "\\test3\\CnC_uploading_exe_modbus_6RTU_with_operate";
        String inPath2 = desktopPath + "\\test3\\exploit_ms08_netapi_modbus_6RTU_with_operate";
        String inPath3 = desktopPath + "\\test3\\Modbus_polling_only_6RTU";
        String inPath4 = desktopPath + "\\test3\\run1_6rtu(1)";
        String inPath5 = desktopPath + "\\test3\\send_a_fake_command_modbus_6RTU_with_operate";
        String inPath6 = desktopPath + "\\test3\\channel_4d_12s(2)";
        String inPath7 = desktopPath + "\\test3\\characterization_modbus_6RTU_with_operate";
        String inPath8 = desktopPath + "\\test3\\moving_two_files_modbus_6RTU";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        inPathList.add(inPath3);
        inPathList.add(inPath4);
        inPathList.add(inPath5);
        inPathList.add(inPath6);
        inPathList.add(inPath7);
        inPathList.add(inPath8);
        int classIndex = 0;
        boolean includeHeader = true;
        preprocessor.transformSCADAData(inPathList, classIndex, includeHeader);*/

        /* Normalize input data
        List<String> inPathList = new ArrayList<>();
        String inPath1 = desktopPath + "\\test3\\CnC_uploading_exe_modbus_6RTU_with_operate_ef";
        String inPath2 = desktopPath + "\\test3\\exploit_ms08_netapi_modbus_6RTU_with_operate_ef";
        String inPath3 = desktopPath + "\\test3\\Modbus_polling_only_6RTU_ef";
        String inPath4 = desktopPath + "\\test3\\run1_6rtu(1)_ef";
        String inPath5 = desktopPath + "\\test3\\send_a_fake_command_modbus_6RTU_with_operate_ef";
        String inPath6 = desktopPath + "\\test3\\channel_4d_12s(2)_ef";
        String inPath7 = desktopPath + "\\test3\\characterization_modbus_6RTU_with_operate_ef";
        String inPath8 = desktopPath + "\\test3\\moving_two_files_modbus_6RTU_ef";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        inPathList.add(inPath3);
        inPathList.add(inPath4);
        inPathList.add(inPath5);
        inPathList.add(inPath6);
        inPathList.add(inPath7);
        inPathList.add(inPath8);
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        preprocessor.normalize(inPathList, false, true, classIndex, includeHeader, options);*/

        /* Sample NSLKDD
        String inPath = desktopPath + "\\test6\\KDDTrain+_edited_ef_ed_oh_norm_normal";
        int classIndex = 0;
        boolean includeHeader = true;
//        String[] optionsKDD = new String[]{"-N", "first-last"};
        String[] optionsKDD = null;
//        int seed = 0;
        for(int a=0; a<10; a++){
            preprocessor.sampleNSLKDD(inPath, 5000, false, a, classIndex, includeHeader, optionsKDD);
        }*/

        /* combine some csv files
        List<String> inPathList = new ArrayList<>();
        String inPath1 = desktopPath + "\\test4_SAE\\KDDTrain+_edited_one-hot_discretize_only_normal_part_norm_encode_13_outliers.csv";
        String inPath2 = desktopPath + "\\test4_SAE\\KDDTrain+_edited_one-hot_discretize_sample_no_normal_norm_encode_13.csv";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        String outputPath = desktopPath + "\\test4_SAE\\KDDTrain+_edited_one-hot_discretize_norm_encode_13_combined.csv";
        preprocessor.combineCSVFiles(inPathList, true, outputPath);*/
    }
}
