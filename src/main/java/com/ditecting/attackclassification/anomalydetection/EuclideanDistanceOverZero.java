package com.ditecting.attackclassification.anomalydetection;

import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.neighboursearch.PerformanceStats;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/13 14:49
 */
public class EuclideanDistanceOverZero extends EuclideanDistance {

    private double delta = 1e-16;

    public void setDelta(double delta) {
        this.delta = delta;
    }

    public double distance(Instance first, Instance second, double cutOffValue, PerformanceStats stats) {
        return super.distance(first, second, cutOffValue, stats) + delta;
    }

}