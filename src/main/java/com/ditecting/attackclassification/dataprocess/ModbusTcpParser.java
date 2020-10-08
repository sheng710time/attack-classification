package com.ditecting.attackclassification.dataprocess;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/4/28 16:43
 */
public class ModbusTcpParser {

    /**
     *
     * @param jsonObject segment in jsonObject
     * @return
     */
    public static List<Transition> segmentToTransitions (JsonObject jsonObject){
        if( !jsonObject.has("tcpHeader") ||!jsonObject.has("modbus")){
            return null;
        }

        JsonObject jsonModbus = jsonObject.getAsJsonObject("modbus");
        int functionCode = jsonModbus.get("modbus.func_code").getAsInt();
        int[] operation = null;
        int[] storageType = null;

        switch (functionCode){
            case 2:
                operation = new int[]{1}; storageType = new int[]{1}; break;
            case 1:;
                operation = new int[]{1}; storageType = new int[]{2}; break;
            case 5:
                operation = new int[]{2}; storageType = new int[]{2}; break;
            case 15:
                operation = new int[]{2}; storageType = new int[]{2}; break;
            case 4:
                operation = new int[]{1}; storageType = new int[]{3}; break;
            case 3:
                operation = new int[]{1}; storageType = new int[]{4}; break;
            case 6:
                operation = new int[]{2}; storageType = new int[]{4}; break;
            case 16:
                operation = new int[]{2}; storageType = new int[]{4}; break;
            case 23:
                operation = new int[]{2,1}; storageType = new int[]{4,4}; break;
            case 22:
                operation = new int[]{2}; storageType = new int[]{4}; break;
            default:
                operation = new int[]{1}; storageType = new int[]{5};
        }

        List<Transition> transitions = new ArrayList<>();
        for(int i=0; i<operation.length; i++){
            int type;
            int startPosition;
            int length;
            StorageBlock sb = null;
            if(operation[i] == 1){//read
                type = storageType[i];
                if(type == 5){
                    sb = new StorageBlock(type,0, 0);
                }else {
                    if(jsonModbus.has("modbus.reference_num")){
                        startPosition = jsonModbus.get("modbus.reference_num").getAsInt();
                    }else if(jsonModbus.has("modbus.read_reference_num")){
                        startPosition = jsonModbus.get("modbus.read_reference_num").getAsInt();
                    }else {
                        startPosition = -1;
                    }

                    if(jsonModbus.has("modbus.bit_cnt")){
                        length = jsonModbus.get("modbus.bit_cnt").getAsInt();
                    } else if(jsonModbus.has("modbus.word_cnt")){
                        length = jsonModbus.get("modbus.word_cnt").getAsInt();
                    } else if(jsonModbus.has("modbus.read_word_cnt")){
                        length = jsonModbus.get("modbus.read_word_cnt").getAsInt();
                    }else {
                        if(startPosition == -1){
                            length = -1;
                        }else {
                            length = 1;
                        }
                    }

                    sb = new StorageBlock(type,startPosition, length);
                }
            }else if(operation[i] == 2){//write
                type = storageType[i];
                if(jsonModbus.has("modbus.reference_num")){
                    startPosition = jsonModbus.get("modbus.reference_num").getAsInt();
                }else if(jsonModbus.has("modbus.write_reference_num")){
                    startPosition = jsonModbus.get("modbus.write_reference_num").getAsInt();
                }else {
                    startPosition = -1;
                }

                if(jsonModbus.has("modbus.bit_cnt")){
                    length = jsonModbus.get("modbus.bit_cnt").getAsInt();
                } else if(jsonModbus.has("modbus.word_cnt")){
                    length = jsonModbus.get("modbus.word_cnt").getAsInt();
                } else if(jsonModbus.has("modbus.write_word_cnt")){
                    length = jsonModbus.get("modbus.write_word_cnt").getAsInt();
                }else {
                    if(startPosition == -1){
                        length = -1;
                    }else {
                        length = 1;
                    }
                }
                sb = new StorageBlock(type,startPosition, length);
            }

            if(sb != null){
                Transition transition = new Transition();
                transition.setDeviceIp(jsonObject.get("dstIp").getAsString());
                transition.setOperation(operation[i]);
                transition.setStorageBlock(sb);
                transitions.add(transition);
            }
        }

        if(transitions.size() == 0){
            return null;
        }else {
            return transitions;
        }
    }
}