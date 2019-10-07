package com.google.allenday.nanostream.util;

public class StringUtils {

    public static String removeWhiteSpaces(String src){
        return src.replaceAll("\\s+","");
    }
}
