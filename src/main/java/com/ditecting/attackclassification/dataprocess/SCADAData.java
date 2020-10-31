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
public class SCADAData {

    @Autowired
    private LoadHolder loadHolder;

    @Autowired
    private PluginCachePool pluginCachePool;

    public List<String> convertData (String inPath) throws InterruptedException {//TODO test
        Map<String, String> ipMap = new HashMap<>();
//        ipMap.put("192.168.43.148", "192.168.1.100");
        ipMap.put("192.168.43.149", "192.168.1.101");

        loadHolder.load(inPath);
        List<String> stringFlowList = pluginCachePool.getAllString();
        List<String> modificationList = new ArrayList<>();
        for(String stringFlow : stringFlowList){
            JsonObject jsonObject = JsonParser.parseString(stringFlow).getAsJsonObject();
//            String srcIp = jsonObject.get("srcIp").getAsString();
            String dstIp = jsonObject.get("dstIp").getAsString();
            if(dstIp.equals("192.168.43.149")){//ignore unrelated data
                String modification1 = stringFlow.replaceAll(dstIp, ipMap.get(dstIp));
                modificationList.add(modification1);
            }
        }
        return modificationList;
    }
}