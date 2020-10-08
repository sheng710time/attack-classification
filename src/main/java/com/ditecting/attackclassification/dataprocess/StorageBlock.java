package com.ditecting.attackclassification.dataprocess;

import lombok.Getter;

import java.io.Serializable;

/**
 * @author CSheng
 * @version 1.0
 * @date 2020/4/28 18:56
 */
@Getter
public class StorageBlock implements Serializable, Comparable<StorageBlock> {
    private static final long serialVersionUID = 6256860939579013212L;
    private int type; // type of storage block, Modbus: 1: Discrete Input, 2: Coil, 3: Input Register, 4: Holding Register, 5:Others
    private int startPosition;
    private int length;

    public StorageBlock(int type, int startPosition, int length) {
        this.type = type;
        this.startPosition = startPosition;
        this.length = length;
    }

    public int getBitLength () {
        switch (type) {
            case 1: return length;
            case 2: return length;
            case 3: return length*16;
            case 4: return length*16;
            default: return 0;
        }
    }

    public int overlapped (StorageBlock sb){
        if(type != sb.type){
            return 0;
        }

        if(startPosition <= sb.startPosition && sb.startPosition <= startPosition+length){
            if(sb.startPosition+sb.length <= startPosition+length){
                return sb.length;
            }else{
                switch (type) {
                    case 1: return startPosition+length-sb.startPosition;
                    case 2: return startPosition+length-sb.startPosition;
                    case 3: return (startPosition+length-sb.startPosition)*16;
                    case 4: return (startPosition+length-sb.startPosition)*16;
                    default: return 0;
                }
            }
        }
        if(startPosition <= sb.startPosition+sb.length && sb.startPosition+sb.length <= startPosition+length){
            switch (type) {
                case 1: return sb.startPosition+sb.length-startPosition;
                case 2: return sb.startPosition+sb.length-startPosition;
                case 3: return (sb.startPosition+sb.length-startPosition)*16;
                case 4: return (sb.startPosition+sb.length-startPosition)*16;
                default: return 0;
            }
        }
        return 0;
    }

    @Override
    public int hashCode (){
        int hashCode = 1;
        hashCode = 31*hashCode + type;
        hashCode = 31*hashCode + startPosition;
        hashCode = 31*hashCode + length;
        return hashCode;
    }

    @Override
    public boolean equals (Object obj) {
        if(this == obj){
            return true;
        }

        if(obj == null){
            return false;
        }

        if(obj instanceof StorageBlock) {
            StorageBlock sb = (StorageBlock) obj;
            if(type != sb.type){
                return false;
            }
            if(startPosition != sb.startPosition){
                return false;
            }
            if (length != sb.length){
                return false;
            }
            return true;
        }

        return false;
    }

    @Override
    public int compareTo(StorageBlock o) {
        if(type - o.type != 0){
            return type - o.type;
        }
        if(startPosition - o.startPosition != 0){
            return startPosition - o.startPosition;
        }
        if(length - o.length != 0){
            return length - o.length;
        }

        return 0;
    }
}