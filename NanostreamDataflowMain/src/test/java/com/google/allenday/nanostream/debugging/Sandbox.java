package com.google.allenday.nanostream.debugging;

import org.apache.beam.sdk.values.KV;

public class Sandbox {
    public static void main(String[] args) {
        String name = "1.txt";
        String folder = "/";
        int index = name.lastIndexOf("/");
        if (index >= 0){
            folder += name.substring(0, index+1);
        }

        System.out.println(folder);
    }
}
