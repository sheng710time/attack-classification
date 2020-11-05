package com.ditecting.attackclassification.dataprocess;

import lombok.Builder;

@Builder
public class ICSSessionADData {
    /* TCP/IP*/
    private String src_addr;
    private String dst_addr;
    private int src_num_port; // the number of source ports
    private int dst_num_port; // the number of destination ports
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
    /* ICS */
    private int read_times; // the times of reading operation, default value: 0
    private int read_num_location; // the number of read locations, default value: 0
    private int read_length_location; // the total length of read locations, default value: 0
    private int write_times; // the times of writing operation, default value: 0
    private int write_num_location; // the number of written locations, default value: 0
    private int write_length_location; // the total length of written locations, default value: 0
    private int data_class;

    public String[] toStrings (){
        String[] features = new String[31];
        features[0] = src_addr + "";
        features[1] = dst_addr + "";
        features[2] = src_num_port + "";
        features[3] = dst_num_port + "";
        features[4] = src_num_packet + "";
        features[5] = dst_num_packet + "";
        features[6] = src_num_byte + "";
        features[7] = dst_num_byte + "";
        features[8] = src_size_min_packet + "";
        features[9] = src_size_max_packet + "";
        features[10] = DataUtil.formatDouble(src_size_mean_packet, 2);
        features[11] = DataUtil.formatDouble(src_size_stddev_packet, 2);
        features[12] = dst_size_min_packet + "";
        features[13] = dst_size_max_packet + "";
        features[14] = DataUtil.formatDouble(dst_size_mean_packet, 2);
        features[15] = DataUtil.formatDouble(dst_size_stddev_packet, 2);
        features[16] = DataUtil.formatDouble(src_time_min_packet, 3);
        features[17] = DataUtil.formatDouble(src_time_max_packet, 3);
        features[18] = DataUtil.formatDouble(src_time_mean_packet, 3);
        features[19] = DataUtil.formatDouble(src_time_stddev_packet, 3);
        features[20] = DataUtil.formatDouble(dst_time_min_packet, 3);
        features[21] = DataUtil.formatDouble(dst_time_max_packet, 3);
        features[22] = DataUtil.formatDouble(dst_time_mean_packet, 3);
        features[23] = DataUtil.formatDouble(dst_time_stddev_packet, 3);
        features[24] = read_times + "";
        features[25] = read_num_location + "";
        features[26] = read_length_location + "";
        features[27] = write_times + "";
        features[28] = write_num_location + "";
        features[29] = write_length_location + "";
        features[30] = data_class + "";
        return features;
    }

    public static String[] getHeader () {
        String[] header = new String[31];
        header[0] = "src_addr";
        header[1] = "dst_addr";
        header[2] = "src_num_port";
        header[3] = "dst_num_port";
        header[4] = "src_num_packet";
        header[5] = "dst_num_packet";
        header[6] = "src_num_byte";
        header[7] = "dst_num_byte";
        header[8] = "src_size_min_packet";
        header[9] = "src_size_max_packet";
        header[10] = "src_size_mean_packet";
        header[11] = "src_size_stddev_packet";
        header[12] = "dst_size_min_packet";
        header[13] = "dst_size_max_packet";
        header[14] = "dst_size_mean_packet";
        header[15] = "dst_size_stddev_packet";
        header[16] ="src_time_min_packet";
        header[17] = "src_time_max_packet";
        header[18] = "src_time_mean_packet";
        header[19] = "src_time_stddev_packet";
        header[20] = "dst_time_min_packet";
        header[21] = "dst_time_max_packet";
        header[22] = "dst_time_mean_packet";
        header[23] = "dst_time_stddev_packet";
        header[24] = "read_times";
        header[25] = "read_num_location";
        header[26] = "read_length_location";
        header[27] = "write_times";
        header[28] = "write_num_location";
        header[29] = "write_length_location";
        header[30] = "data_class";
        return header;
    }
}
