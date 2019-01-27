package com.azyd.face.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatUtils {

    public static String StringToDate(String time,String sourseFormat,String destFormat) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat(sourseFormat);
        Date date;
        date = format.parse(time);
        SimpleDateFormat format1 = new SimpleDateFormat(destFormat);
        String s = format1.format(date);
        return s;

    }
    public static Date StringToDate(String time,String sourseFormat) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat(sourseFormat);
        Date date;
        date = format.parse(time);
        return date;

    }

}
