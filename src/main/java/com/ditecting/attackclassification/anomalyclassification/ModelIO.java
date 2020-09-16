package com.ditecting.attackclassification.anomalyclassification;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/5/1 11:48
 */
@Slf4j
public class ModelIO {

    public static boolean outputModel (String filePath, Object object){
        log.info("Start to output model.");
        boolean flag = true;

        try {
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(object);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        log.info("Finish outputting model.");
        return flag;
    }

    public static Object inputModel (String filePath){
        log.info("Start to input model.");
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("The file [" + filePath +"] does not exist.");
            }

            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            Object object = ois.readObject();
            ois.close();

            log.info("Finish inputting model.");
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}