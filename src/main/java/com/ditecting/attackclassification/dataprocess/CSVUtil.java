package com.ditecting.attackclassification.dataprocess;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/7 8:52
 */
public class CSVUtil {

    /**
     * read CSV file
     * @param filePath
     * @return List<String>
     */
    public static List<String[]> read(String filePath, char delimiter, boolean includeHeader){

        List<String[]> strList = new ArrayList<String[]>();
        try {
            CsvReader csvReader = new CsvReader(filePath, delimiter, Charset.forName("UTF-8"));
            // read header
            if(includeHeader){
                csvReader.readHeaders();
            }

            while (csvReader.readRecord()){
                strList.add(csvReader.getValues());
            }

            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            return strList;
        }
    }

    /**
     * read csv file
     * @param filePath
     * @param includeHeader
     * @return
     */
    public static List<String> read(String filePath, boolean includeHeader){
        List<String> strList = new ArrayList<String>();
        try {
            CsvReader csvReader = new CsvReader(filePath,',', Charset.forName("UTF-8"));
            // read header
            if(includeHeader){
                csvReader.readHeaders();
            }

            while (csvReader.readRecord()){
                // read line
                String str = csvReader.getRawRecord();
                strList.add(str);
//                System.out.println(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            return strList;
        }
    }

    /**
     * read csv file
     * @param filePath
     * @param includeHeader
     * @return
     */
    public static List<String[]> readMulti(String filePath, boolean includeHeader){
        List<String[]> strsList = new ArrayList<String[]>();
        try {
            CsvReader csvReader = new CsvReader(filePath,',', Charset.forName("UTF-8"));
            // read header
            if(includeHeader){
                csvReader.readHeaders();
            }

            while (csvReader.readRecord()){
                // read line
                String[] strs = csvReader.getValues();
                strsList.add(strs);
//                System.out.println(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            return strsList;
        }
    }

    /**
     * write csv file
     * @param filePath
     */
    public static void write(String filePath, String[] str){
        try {
            CsvWriter csvWriter = new CsvWriter(filePath,',', Charset.forName("UTF-8"));

            // write a line
            csvWriter.writeRecord(str);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write csv file
     * @param filePath
     */
    public static void write(String filePath, List<String[]> strList){
        try {
            CsvWriter csvWriter = new CsvWriter(filePath,',', Charset.forName("UTF-8"));

            for(String[] str : strList){
                csvWriter.writeRecord(str);
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}