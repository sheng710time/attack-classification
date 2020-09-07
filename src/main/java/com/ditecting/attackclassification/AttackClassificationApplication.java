package com.ditecting.attackclassification;

import com.ditecting.attackclassification.anomalyclassification.DensityPeakClusterStrict;
import com.ditecting.attackclassification.anomalydetection.LOF_AD;
import com.ditecting.attackclassification.anomalydetection.SAE_AD;
import com.ditecting.attackclassification.anomalydetection.SDAE_AD;
import com.ditecting.attackclassification.dataprocess.FileLoader;
import com.ditecting.attackclassification.dataprocess.Preprocessor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.LOF;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        /* deal with raw data*/
        /* has label file
        String inPath = "C:\\Users\\18809\\Desktop\\test\\send_a_fake_command_modbus_6RTU_with_operate.pcap";
        String inPathLabel = "C:\\Users\\18809\\Desktop\\test\\send_a_fake_command_modbus_6RTU_with_operate_labeled.csv";
        String outPath = "C:\\Users\\18809\\Desktop\\test\\send_a_fake_command_modbus_6RTU_with_operate.csv";
        String outPathNo = "C:\\Users\\18809\\Desktop\\test\\send_a_fake_command_modbus_6RTU_with_operate_no.csv";
        preprocessor.extract(inPath, inPathLabel, outPath, outPathNo);*/
        /* has no label file
        String inPath = "C:\\Users\\18809\\Desktop\\test\\run1_6rtu(1).pcap";
        String outPath = "C:\\Users\\18809\\Desktop\\test\\run1_6rtu(1).csv";
        String outPathNo = "C:\\Users\\18809\\Desktop\\test\\run1_6rtu(1)_no.csv";
        int data_class = 0;
        preprocessor.extract(inPath, outPath, outPathNo, data_class);*/

        /* Generate label file
        String inPathLabel = "C:\\Users\\18809\\Desktop\\test2\\run1_6rtu(1)_labeled.csv";
        preprocessor.generateLabelFile(inPathLabel, 134690, 0);*/


        /* Normalize input data
        List<String> inPathList = new ArrayList<>();
        String inPath1 = "C:\\Users\\18809\\Desktop\\test2\\run1_6rtu(1)";
        String inPath2 = "C:\\Users\\18809\\Desktop\\test2\\CnC_uploading_exe_modbus_6RTU_with_operate";
        String inPath3 = "C:\\Users\\18809\\Desktop\\test2\\exploit_ms08_netapi_modbus_6RTU_with_operate";
        String inPath4 = "C:\\Users\\18809\\Desktop\\test2\\send_a_fake_command_modbus_6RTU_with_operate";
        String inPath5 = "C:\\Users\\18809\\Desktop\\test2\\Modbus_polling_only_6RTU";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        inPathList.add(inPath3);
        inPathList.add(inPath4);
        inPathList.add(inPath5);
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "1-20", "-N", "last"};
        preprocessor.normalize(inPathList, false, true, classIndex, includeHeader, options);*/

        /* Transform NSLKDD
        List<String> inPathList = new ArrayList<>();
        String inPath1 = "C:\\Users\\18809\\Desktop\\test3\\KDDTrain+_one-hot";
        String inPath2 = "C:\\Users\\18809\\Desktop\\test3\\KDDTest+_one-hot";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        int classIndex = 0;
        boolean includeHeader = false;
        String[] optionsKDD = new String[]{"-N", "7,12,14,15,21,22"};
        preprocessor.transformNSLKDD(inPathList, classIndex, includeHeader, optionsKDD);*/

        /* Sample NSLKDD
        String inPath = "C:\\Users\\18809\\Desktop\\test3\\KDDTrain+_edited_one-hot_discretize";
        int classIndex = 0;
        boolean includeHeader = true;
        String[] optionsKDD = new String[]{"-N", "first-last"};
        preprocessor.sampleNSLKDD(inPath, 50, false, 12345, classIndex, includeHeader, optionsKDD)*/;

        /* LOF */
        double cutOffValue = 1.1;
        int KNN = 20;
        LOF_AD lofAD = new LOF_AD(0);
        String trainFilePath = "C:\\Users\\18809\\Desktop\\test2\\run1_6rtu(1)_norm.csv";
        String testFileName = "C:\\Users\\18809\\Desktop\\test2\\send_a_fake_command_modbus_6RTU_with_operate";
        String testFilePath = testFileName + "_norm.csv";
        String testFilePathNo = testFileName + "_no.csv";
        String testFilePathLabel = testFileName + "_labeled.csv";
        String outPathResult = testFileName + "_result_lof.csv";
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        lofAD.train(trainFilePath, KNN, KNN, classIndex, includeHeader, options);
//        String modelPath = "C:\\Users\\18809\\Desktop\\test2\\LOF.model";
//        lofAD.saveLOF(modelPath);
//        LOF lof = LOF_AD.readLOF(modelPath);

        double dc = lofAD.evaluateTrainingData(cutOffValue, KNN);
//        System.out.println("dc: " + dc);
//        Instances predictedData = lofAD.test(lof, testFilePath, classIndex, includeHeader, options);
//        LOF_AD.evaluate(predictedData, testFilePathNo, testFilePathLabel, cutOffValue);
//        lofAD.test(testFilePath, classIndex, includeHeader, options);
//        lofAD.evaluate(testFilePathNo, testFilePathLabel, cutOffValue);
//        lofAD.output(testFilePathNo, testFilePathLabel, outPathResult, cutOffValue);

        /* SAE
        double cutOffValue = 0.00625;
        SAE_AD saeAD = new SAE_AD(20, 14, 8, 0);
        String trainFilePath = "C:\\Users\\18809\\Desktop\\test2\\run1_6rtu(1)_norm.csv";
        String testFileName = "C:\\Users\\18809\\Desktop\\test2\\send_a_fake_command_modbus_6RTU_with_operate";
        String testFilePath = testFileName + "_norm.csv";
        String testFilePathNo = testFileName + "_no.csv";
        String testFilePathLabel = testFileName + "_labeled.csv";
        String outPathResult = testFileName + "_result_sae.csv";
        String outPathEncode = testFileName + "_code_8.csv";
        saeAD.train(trainFilePath, 20, 2, 100);
//        saeAD.test(testFilePath, 20, 2, 1000);
        saeAD.encode(testFilePath, 20, 2, 1000, outPathEncode);
//        saeAD.evaluate(testFilePathNo, testFilePathLabel, cutOffValue);
//        saeAD.output(testFilePathNo, testFilePathLabel, outPathResult, cutOffValue);*/

        /* DPC
        String trainFilePathLabel = "C:\\Users\\18809\\Desktop\\test2\\Modbus_polling_only_6RTU_norm_test_label.csv";
        String trainFilePath = "C:\\Users\\18809\\Desktop\\test2\\Modbus_polling_only_6RTU_norm_test.csv";
        String testFileName = "C:\\Users\\18809\\Desktop\\test2\\send_a_fake_command_modbus_6RTU_with_operate";
        String testsFilePath = testFileName + "_norm_test.csv";
        String testFilePathNo = testFileName + "_no.csv";
        String testFilePathLabel = testFileName + "_labeled.csv";
        String outPathResult = testFileName + "_result_dpc.csv";
        double dc = 0.01029;
        int trainLabelIndex = 20;
        int trainIndex = -1;
        DensityPeakClusterStrict cluster = new DensityPeakClusterStrict();
        cluster.train(trainFilePathLabel, trainFilePath, trainLabelIndex, trainIndex, dc);
        cluster.test(testsFilePath, dc);
//		cluster.evaluate(testFilePathLabel, testFilePathNo);
//        cluster.output(testFilePathLabel, testFilePathNo, outPathResult);*/

        System.out.println("");
    }

}
