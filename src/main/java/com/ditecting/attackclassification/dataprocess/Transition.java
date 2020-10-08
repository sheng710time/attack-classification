package com.ditecting.attackclassification.dataprocess;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/4/23 10:24
 */
@Setter
@Getter
public class Transition implements Serializable, Comparable<Transition> {
    private static final long serialVersionUID = -2462356455091987079L;
    private String deviceIp;
    private int operation; // 1:read, 2:write
    private StorageBlock storageBlock;

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = hashCode*31 + deviceIp.hashCode();
        hashCode = hashCode*31 + operation;
        if(storageBlock == null){
            return  hashCode;
        }
        hashCode = hashCode*31 + storageBlock.hashCode();

        return  hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }

        if(obj == null){
            return false;
        }

        if(obj instanceof Transition) {
            Transition other = (Transition) obj;
            if(!deviceIp.equals(other.deviceIp)){
                return false;
            }
            if(operation != other.operation){
                return false;
            }
            if(storageBlock == other.storageBlock){
                return true;
            }
            if(storageBlock==null || other.storageBlock==null){
                return false;
            }
            if(!storageBlock.equals(other.storageBlock)){
                return false;
            }
            return true;
        }

        return false;
    }

    @Override
    public int compareTo(Transition o) {
        if(deviceIp.compareTo(o.deviceIp) != 0){
            return deviceIp.compareTo(o.deviceIp);
        }
        if(operation - o.operation != 0){
            return operation - o.operation;
        }
        int diff = storageBlock.compareTo(o.storageBlock);
        if(diff != 0){
            return diff;
        }

        return 0;
    }
}