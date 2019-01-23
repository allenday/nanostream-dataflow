package com.theappsolutions.nanostream.util;

public class StringUtils {

    public static String removeWhiteSpaces(String src){
        return src.replaceAll("\\s+","");
    }
}
