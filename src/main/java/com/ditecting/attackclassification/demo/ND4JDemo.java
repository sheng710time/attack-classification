package com.ditecting.attackclassification.demo;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/14 14:19
 */
public class ND4JDemo {

    public static void main(String[] args){
        INDArray nd = Nd4j.create(new float[]{1,2}, new int[]{1,2});

        INDArray nd2 = Nd4j.rand(new int[]{2,3});

        System.out.println(nd.mmul(nd2));
    }




}