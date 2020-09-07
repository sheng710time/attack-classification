package com.ditecting.attackclassification.dataprocess;

import lombok.Builder;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/8/5 11:58
 */
@Builder
public class ADData {
    /*Packets*/
    private int src_num_packet;
    private int dst_num_packet;
    /*Bytes*/
    private int src_num_byte;
    private int dst_num_byte;
    /*Packet Size*/
    private int src_size_min_packet;
    private int src_size_max_packet;
    private double src_size_mean_packet;
    private double src_size_stddev_packet;
    private int dst_size_min_packet;
    private int dst_size_max_packet;
    private double dst_size_mean_packet;
    private double dst_size_stddev_packet;
    /*Inter-Packet Time*/
    private double src_time_min_packet;
    private double src_time_max_packet;
    private double src_time_mean_packet;
    private double src_time_stddev_packet;
    private double dst_time_min_packet;
    private double dst_time_max_packet;
    private double dst_time_mean_packet;
    private double dst_time_stddev_packet;
    private int data_class;

    public String[] toStrings (){
        String[] features = new String[21];
        features[0] = src_num_packet + "";
        features[1] = dst_num_packet + "";
        features[2] = src_num_byte + "";
        features[3] = dst_num_byte + "";
        features[4] = src_size_min_packet + "";
        features[5] = src_size_max_packet + "";
        features[6] = DataUtil.formatDouble(src_size_mean_packet, 2);
        features[7] = DataUtil.formatDouble(src_size_stddev_packet, 2);
        features[8] = dst_size_min_packet + "";
        features[9] = dst_size_max_packet + "";
        features[10] = DataUtil.formatDouble(dst_size_mean_packet, 2);
        features[11] = DataUtil.formatDouble(dst_size_stddev_packet, 2);
        features[12] = DataUtil.formatDouble(src_time_min_packet, 3);
        features[13] = DataUtil.formatDouble(src_time_max_packet, 3);
        features[14] = DataUtil.formatDouble(src_time_mean_packet, 3);
        features[15] = DataUtil.formatDouble(src_time_stddev_packet, 3);
        features[16] = DataUtil.formatDouble(dst_time_min_packet, 3);
        features[17] = DataUtil.formatDouble(dst_time_max_packet, 3);
        features[18] = DataUtil.formatDouble(dst_time_mean_packet, 3);
        features[19] = DataUtil.formatDouble(dst_time_stddev_packet, 3);
        features[20] = data_class + "";
        return features;
    }

    public static String[] getHeader () {
        String[] header = new String[21];
        header[0] = "src_num_packet";
        header[1] = "dst_num_packet";
        header[2] = "src_num_byte";
        header[3] = "dst_num_byte";
        header[4] = "src_size_min_packet";
        header[5] = "src_size_max_packet";
        header[6] = "src_size_mean_packet";
        header[7] = "src_size_stddev_packet";
        header[8] = "dst_size_min_packet";
        header[9] = "dst_size_max_packet";
        header[10] = "dst_size_mean_packet";
        header[11] = "dst_size_stddev_packet";
        header[12] ="src_time_min_packet";
        header[13] = "src_time_max_packet";
        header[14] = "src_time_mean_packet";
        header[15] = "src_time_stddev_packet";
        header[16] = "dst_time_min_packet";
        header[17] = "dst_time_max_packet";
        header[18] = "dst_time_mean_packet";
        header[19] = "dst_time_stddev_packet";
        header[20] = "data_class";
        return header;
    }
}