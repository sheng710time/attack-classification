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
public class HoneypotData {
    @Autowired
    private LoadHolder loadHolder;

    @Autowired
    private PluginCachePool pluginCachePool;

    public Map<String, List<String>> convertData (String inPath, String orgPath) throws InterruptedException {
        Map<String, String> orgs = extractOrgs(orgPath);
        Map<String, String> pots = new HashMap<>();
        pots.put("192.168.0.11", "192.168.1.106");
        pots.put("192.168.0.7", "192.168.1.106");

        loadHolder.load(inPath);
        List<String> stringFlowList = pluginCachePool.getAllString();
        Map<String, List<String>> orgModificationsMap = new HashMap<>();
        for(String stringFlow : stringFlowList){
            JsonObject jsonObject = JsonParser.parseString(stringFlow).getAsJsonObject();
            String srcIp = jsonObject.get("srcIp").getAsString();
            String dstIp = jsonObject.get("dstIp").getAsString();
            if(pots.containsKey(dstIp) && orgs.containsKey(srcIp)) {//ignore unrelated data
                String modification = stringFlow.replaceAll(dstIp, pots.get(dstIp));
                if(orgModificationsMap.containsKey(orgs.get(srcIp))){
                    orgModificationsMap.get(orgs.get(srcIp)).add(modification);
                }else{
                    List<String> modifications = new ArrayList<>();
                    modifications.add(modification);
                    orgModificationsMap.put(orgs.get(srcIp), modifications);
                }
            }
        }

        return orgModificationsMap;
    }

    /**
     * extract organizations from org file
     * @param orgPath
     * @return
     */
    private Map<String, String> extractOrgs (String orgPath) {
        List<String> strList = CSVUtil.read(orgPath, false);
        Map<String, String> orgs = new HashMap<>();
        strList.forEach((str)->{
            int index = str.indexOf(',');
            if(index != -1){
                String ip = str.substring(0,index);
                String org = str.substring(index+1,str.length());
                orgs.put(ip, org);
            }
        });

        return orgs;
    }
}