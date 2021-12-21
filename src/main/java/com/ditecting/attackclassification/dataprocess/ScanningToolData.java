package com.ditecting.attackclassification.dataprocess;

import com.ditecting.honeyeye.cachepool.PluginCachePool;
import com.ditecting.honeyeye.inputer.loader.LoadHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/10/22 20:42
 */
@Component
public class ScanningToolData {

    @Autowired
    private LoadHolder loadHolder;

    @Autowired
    private PluginCachePool pluginCachePool;

    public List<String> convertData (String inPath) throws InterruptedException {
        Map<String, String> ipMap = new HashMap<>();
//        ipMap.put("202.199.13.224", "192.168.1.100");
//        ipMap.put("202.199.13.129", "192.168.1.100");
        ipMap.put("1.224.102.6", "192.168.1.101");
        ipMap.put("1.225.67.4", "192.168.1.102");
        ipMap.put("1.230.226.50", "192.168.1.103");
        ipMap.put("1.231.115.71", "192.168.1.104");
        ipMap.put("1.252.63.42", "192.168.1.105");

        loadHolder.load(inPath);
        List<String> stringList = pluginCachePool.getAllString();
        return stringList;

//        List<String> modificationList = new ArrayList<>();
//        for(String str : stringList){
//            JsonObject jsonObject = JsonParser.parseString(str).getAsJsonObject();
//            String srcIp = jsonObject.get("srcIp").getAsString();
//            String dstIp = jsonObject.get("dstIp").getAsString();
//            if(srcIp.equals("202.199.13.224") || srcIp.equals("202.199.13.129")){//ignore unrelated data
//                String modification1 = str.replaceAll(dstIp, ipMap.get(dstIp));
//                modificationList.add(modification1);
//            }
//        }
//        return modificationList;
    }
}