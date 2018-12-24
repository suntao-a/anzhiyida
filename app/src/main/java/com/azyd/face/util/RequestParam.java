package com.azyd.face.util;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:46
 * $describe$
 */
public class RequestParam {
    public static Build build(){
        return new Build();
    }
    public static Build build(int size){
        return new Build(size);
    }
    public static class Build{
        Map<String,Object> paramMap;
        public Build(){
            paramMap = new HashMap<>();
        }
        public Build(int count){
            paramMap = new HashMap<>(count);
        }

        /**
         *
         * @param key
         * @param value
         * @return
         * replace {@link ParamHelp#with} method.
         */
        @Deprecated
        public Build put(String key,Object value){
            paramMap.put(key,value);
            return  this;
        }

        /**
         *
         * @param key
         * @param value
         * @return
         */
        public Build with(String key,Object value){
            if(value!=null){
                paramMap.put(key,value);
            }
            return  this;
        }
        public Build withPageNo(int no){
            paramMap.put("pageNo", no);
            return  this;
        }
        public Build withPageSize(int size){
            paramMap.put("pageSize", size);
            return  this;
        }
        public Map<String,Object> create(){
            return paramMap;
        }
    }
}
