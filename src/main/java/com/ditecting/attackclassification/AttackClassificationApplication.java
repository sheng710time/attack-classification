package com.ditecting.attackclassification;

import com.ditecting.attackclassification.anomalyclassification.DensityPeakClusterStrictDistributed;
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

import java.io.IOException;

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
        callDPC();

        System.out.println("");
    }

    public void callDPC () throws IOException, InterruptedException {
        String trainFilePathLabel = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_part.csv";
        int trainIndex = -1;
        String trainFilePath = null;
        int labelIndex = 122;
        int batchSize = 2600;
        DensityPeakClusterStrictDistributed dhcsd = new DensityPeakClusterStrictDistributed();
        dhcsd.init(trainFilePathLabel, trainFilePath, labelIndex, trainIndex, batchSize);
        dhcsd.train();

//        DHCSD.train(trainFilePathLabel, trainFilePath, labelIndex, trainIndex, batchSize);

        /* DPCS
        DensityPeakClusterStrict DPCS = new DensityPeakClusterStrict();
        DPCS.train(trainFilePathLabel, trainFilePath, labelIndex, trainIndex);
        String modelFilePath = "C:\\Users\\18809\\Desktop\\test5\\DPCS.model";
        ModelIO.outputModel(modelFilePath, DPCS);
//        DensityPeakClusterStrict DPCS = (DensityPeakClusterStrict) ModelIO.inputModel(modelFilePath);

        int KNC = 50;
        String testsFilePath = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_part.csv";
        DPCS.test(testsFilePath, labelIndex, KNC);
        DPCS.evaluate();
        String outPathResult = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_part_result_dpc.csv";
        DPCS.output(outPathResult);*/
    }

    public void callSAE_AD () throws Exception {
        int first = 122;
        int second = 64;
        int third = 10;
        SAE_AD saeAD = new SAE_AD(first, second, third, 0);
        String trainFilePath = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm.csv";
        String encodeFilePath = "C:\\Users\\18809\\Desktop\\test5\\KDDTest+_edited_ef_ed_oh_norm.csv";
        String outPathEncode = "C:\\Users\\18809\\Desktop\\test5\\KDDTest+_edited_ef_ed_oh_norm"+ "_encode_" + third +".csv";
        int labelIndex = 122;
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
        /* Build LOF model */
        LOF_AD lofAD = new LOF_AD(0);
        String trainFilePath = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_normal_part.csv";
        int KNN = 40;
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        lofAD.train(trainFilePath, KNN, KNN, classIndex, includeHeader, options);

        /* Evaluate training data */
        double cutOffValue = 1.1;
//        lofAD.evaluateTrainingData(cutOffValue, KNN, true);
//        String outPathOutliers = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_normal_part_outliers_KNN30.csv";
//        lofAD.outputOutliers(outPathOutliers);

        /* Save LOF model */
//        String modelPath = "C:\\Users\\18809\\Desktop\\test2\\LOF.model";
//        lofAD.saveLOF(modelPath);
        /* Read LOF model */
//        LOF lof = LOF_AD.readLOF(modelPath);

        /* Test testing data*/
        String testFilePath = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_attack_sample.csv";
        lofAD.test(testFilePath, classIndex, includeHeader, options);
        lofAD.evaluate(cutOffValue);
        String outPathResult = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh_norm_attack_sample_result_lof_KNN40.csv";
        lofAD.output(outPathResult, cutOffValue);

//        Instances predictedData = LOF_AD.test(lof, testFilePath, classIndex, includeHeader, options);
//        LOF_AD.evaluate(predictedData, testFilePathNo, testFilePathLabel, cutOffValue);
    }

    public void callPreprocessor () throws Exception {
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
        String inPath1 = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_ef_ed_oh";
        String inPath2 = "C:\\Users\\18809\\Desktop\\test5\\KDDTest+_edited_ef_ed_oh";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        int classIndex = 0;
        boolean includeHeader = true;
        String[] options = new String[]{"-R", "first-last"};
        preprocessor.normalize(inPathList, false, true, classIndex, includeHeader, options);*/

        /* Transform NSLKDD
        List<String> inPathList = new ArrayList<>();
        String inPath1 = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited";
        String inPath2 = "C:\\Users\\18809\\Desktop\\test5\\KDDTest+_edited";
//        String inPath3 = "C:\\Users\\18809\\Desktop\\test5\\KDDTrain+_edited_test_002";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
//        inPathList.add(inPath3);
        int classIndex = 0;
        boolean includeHeader = false;
        preprocessor.transformNSLKDD(inPathList, classIndex, includeHeader);*/

        /* Sample NSLKDD*/
        String inPath = "C:\\Users\\18809\\Desktop\\test6\\KDDTrain+_edited_ef_ed_oh_norm_normal";
        int classIndex = 0;
        boolean includeHeader = true;
//        String[] optionsKDD = new String[]{"-N", "first-last"};
        String[] optionsKDD = null;
//        int seed = 0;
        for(int a=0; a<10; a++){
            preprocessor.sampleNSLKDD(inPath, 5000, false, a, classIndex, includeHeader, optionsKDD);
        }

        /* combine some csv files
        List<String> inPathList = new ArrayList<>();
        String inPath1 = "C:\\Users\\18809\\Desktop\\test4_SAE\\KDDTrain+_edited_one-hot_discretize_only_normal_part_norm_encode_13_outliers.csv";
        String inPath2 = "C:\\Users\\18809\\Desktop\\test4_SAE\\KDDTrain+_edited_one-hot_discretize_sample_no_normal_norm_encode_13.csv";
        inPathList.add(inPath1);
        inPathList.add(inPath2);
        String outputPath = "C:\\Users\\18809\\Desktop\\test4_SAE\\KDDTrain+_edited_one-hot_discretize_norm_encode_13_combined.csv";
        preprocessor.combineCSVFiles(inPathList, true, outputPath);*/
    }
}
