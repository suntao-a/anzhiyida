package com.azyd.face.util.permission;


import java.util.HashMap;
import java.util.Map;

/**
 * @author suntao
 * @creat-time 2018/9/29 on 16:02
 * $describe$
 */
public class PermissionTable {
    static Map<String,String> map = new HashMap<>();

    static  {
        map.put(Permissions.CAMERA[0],"相机");
        map.put(Permissions.STORAGE[1],"存储");

    }
    public static String get(String key){
        return map.get(key);
    }

}
