package com.ditecting.attackclassification;

import com.ditecting.attackclassification.anomalyclassification.DensityPeakClusterStrict;
import com.ditecting.attackclassification.anomalyclassification.DensityPeakClusterStrictDistributed;
import com.ditecting.attackclassification.anomalyclassification.ModelIO;
import com.ditecting.attackclassification.anomalyclassification.Sample;
import com.ditecting.attackclassification.anomalydetection.LOF_AD;
import com.ditecting.attackclassification.anomalydetection.SAE_AD;
import com.ditecting.attackclassification.dataprocess.ExtractorADData;
import com.ditecting.attackclassification.dataprocess.ExtractorICSADData;
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
    private ExtractorADData extractorADData;

    @Autowired
    private ExtractorICSADData extractorICSADData;

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
        callLOF_AD ();

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
        int first = 90;
        int second = 40;
        int third = 2;
        SAE_AD saeAD = new SAE_AD(first, second, third, 0);
        String trainFilePath = desktopPath + "\\test2\\dealed\\run1_6rtu(1)_ef_oh_norm.csv";
        String encodeFilePath = desktopPath + "\\test2\\dealed\\run1_6rtu(1) && attacks.csv";
        String outPathEncode = desktopPath + "\\test2\\dealed\\run1_6rtu(1) && attacks"+ "_encode_" + second + "-" +third +".csv";
        int labelIndex = 90;
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
        String trainFilePath = desktopPath + "\\test2\\dealed\\run1_6rtu(1)_ef_oh_norm.csv";
        int KNN = 20;
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        lofAD.train(trainFilePath, KNN, KNN, classIndex, includeHeader, options);

        /* Evaluate training data */
        double cutOffValue = 3;
        lofAD.evaluateTrainingData(cutOffValue, KNN, true);
        String outPathOutliers = desktopPath + "\\test2\\dealed\\run1_6rtu(1)_ef_oh_norm_outliers_KNN-"+ KNN +"_CV-"+ cutOffValue +".csv";
        lofAD.outputOutliers(outPathOutliers);

        /* Save LOF model */
//        String modelPath = desktopPath + "\\test2\\LOF.model";
//        lofAD.saveLOF(modelPath);
        /* Read LOF model */
//        LOF lof = LOF_AD.readLOF(modelPath);

        /* Test testing data*/
        String testFilePath = desktopPath + "\\test2\\dealed\\attacks_ef_oh_norm.csv";
        lofAD.test(testFilePath, classIndex, includeHeader, options);
        lofAD.evaluate(cutOffValue);
        String outPathResult = desktopPath + "\\test2\\dealed\\attacks_ef_oh_norm_result_lof_KNN-"+ KNN +"_CV-"+ cutOffValue +".csv";
        lofAD.output(outPathResult, cutOffValue);

//        Instances predictedData = LOF_AD.test(lof, testFilePath, classIndex, includeHeader, options);
//        LOF_AD.evaluate(predictedData, testFilePathNo, testFilePathLabel, cutOffValue);
    }

    public void callPreprocessor () throws Exception {
        String desktopPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        /* deal with raw data*/
        /* has label file
        String filename = "send_a_fake_command_modbus_6RTU_with_operate";
        String inPath = desktopPath + "\\test1_new\\"+ filename +".pcap";
        String inPathLabel = desktopPath + "\\test1_new\\"+ filename +"_labeled.csv";
        String outPath = desktopPath + "\\test1_new\\"+ filename +".csv";
        String outPathNo = desktopPath + "\\test1_new\\"+ filename +"_no.csv";
        extractorADData.extract(inPath, inPathLabel, outPath, outPathNo);*/
        /* has no label file
        String inPath = desktopPath + "\\test1\\exploit_ms08_netapi_modbus_6RTU_with_operate.pcap";
        String outPath = desktopPath + "\\test1\\exploit_ms08_netapi_modbus_6RTU_with_operateqqq.csv";
        String outPathNo = desktopPath + "\\test1\\exploit_ms08_netapi_modbus_6RTU_with_operate_noqqq.csv";
        int data_class = 0;
//        extractorADData.extract(inPath, outPath, outPathNo, data_class);
        extractorICSADData.extract(inPath, outPath, outPathNo, data_class);*/

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
        String inPath1 = desktopPath + "\\test1_new\\CnC_uploading_exe_modbus_6RTU_with_operate";
        String inPath2 = desktopPath + "\\test1_new\\exploit_ms08_netapi_modbus_6RTU_with_operate";
        String inPath3 = desktopPath + "\\test1_new\\Modbus_polling_only_6RTU";
        String inPath4 = desktopPath + "\\test1_new\\run1_6rtu(1)";
        String inPath5 = desktopPath + "\\test1_new\\send_a_fake_command_modbus_6RTU_with_operate";
        String inPath6 = desktopPath + "\\test1_new\\channel_4d_12s(2)";
        String inPath7 = desktopPath + "\\test1_new\\characterization_modbus_6RTU_with_operate";
        String inPath8 = desktopPath + "\\test1_new\\moving_two_files_modbus_6RTU";
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

        /* Normalize input data*/
        List<String> inPathList = new ArrayList<>();
        String inPath1 = desktopPath + "\\test2\\CnC_uploading_exe_modbus_6RTU_with_operate_ef_oh";
        String inPath2 = desktopPath + "\\test2\\exploit_ms08_netapi_modbus_6RTU_with_operate_ef_oh";
        String inPath3 = desktopPath + "\\test2\\Modbus_polling_only_6RTU_ef_oh";
        String inPath4 = desktopPath + "\\test2\\run1_6rtu(1)_ef_oh";
        String inPath5 = desktopPath + "\\test2\\send_a_fake_command_modbus_6RTU_with_operate_ef_oh";
        String inPath6 = desktopPath + "\\test2\\channel_4d_12s(2)_ef_oh";
        String inPath7 = desktopPath + "\\test2\\characterization_modbus_6RTU_with_operate_ef_oh";
        String inPath8 = desktopPath + "\\test2\\moving_two_files_modbus_6RTU_ef_oh";
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
        preprocessor.normalize(inPathList, false, true, classIndex, includeHeader, options);

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
