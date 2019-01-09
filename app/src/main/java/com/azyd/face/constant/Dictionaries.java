package com.azyd.face.constant;

import java.util.HashMap;

/**
 * @author suntao
 * @creat-time 2019/1/9 on 10:53
 * $describe$
 */
public class Dictionaries {
    private static HashMap<String,Integer> peoples=new HashMap<>();
    private static HashMap<String,Integer> sexs=new HashMap<>();

    static {
        peoples.put("汉",1);
        peoples.put("蒙古",2);
        peoples.put("回",3);
        peoples.put("藏",4);
        peoples.put("维吾尔",5);
        peoples.put("苗",6);
        peoples.put("彝",7);
        peoples.put("壮",8);
        peoples.put("布依",9);
        peoples.put("朝鲜",10);
        peoples.put("满",11);
        peoples.put("侗",12);
        peoples.put("瑶",13);
        peoples.put("白",14);
        peoples.put("土家",15);
        peoples.put("哈尼",16);
        peoples.put("哈萨克",17);
        peoples.put("傣",18);
        peoples.put("黎",19);
        peoples.put("傈僳",20);
        peoples.put("佤",21);
        peoples.put("畲",22);
        peoples.put("高山",23);
        peoples.put("拉祜",24);
        peoples.put("水",25);
        peoples.put("东乡",26);
        peoples.put("纳西",27);
        peoples.put("景颇",28);
        peoples.put("柯尔克孜",29);
        peoples.put("土",30);
        peoples.put("达斡尔",31);
        peoples.put("仫佬",32);
        peoples.put("羌",33);
        peoples.put("布朗",34);
        peoples.put("撒拉",35);
        peoples.put("毛南",36);
        peoples.put("仡佬",37);
        peoples.put("锡伯",38);
        peoples.put("阿昌",39);
        peoples.put("普米",40);
        peoples.put("塔吉克",41);
        peoples.put("怒",42);
        peoples.put("乌孜别克",43);
        peoples.put("俄罗斯",44);
        peoples.put("鄂温克",45);
        peoples.put("德昂",46);
        peoples.put("保安",47);
        peoples.put("裕固",48);
        peoples.put("京",49);
        peoples.put("塔塔尔",50);
        peoples.put("独龙",51);
        peoples.put("鄂伦春",52);
        peoples.put("赫哲",53);
        peoples.put("门巴",54);
        peoples.put("珞巴",55);
        peoples.put("基诺",56);

        sexs.put("未知",0);
        sexs.put("男",1);
        sexs.put("女",2);
        sexs.put("其他",9);
    }

    public static Integer getPeopleKey(String value){
        return peoples.get(value);
    }
    public static Integer getSexKey(String value){
        return sexs.get(value);
    }
}
