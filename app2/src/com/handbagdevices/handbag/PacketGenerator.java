package com.handbagdevices.handbag;

import java.util.*;

import android.text.TextUtils;

class PacketGenerator {

    private static String encodeField(String content) {

    	if (content.startsWith("[")
    	    || content.contains(";")
    	    || content.contains("\n")) {
    	    // TODO: Explictly create formatter instance?
    	    //       (Due to comment in String.format docs:
    	    //       "...somewhat costly in terms of memory and
    	    //       time...if you rely on it for formatting a large
    	    //       number of strings, consider creating and reusing
    	    //       your own Formatter instance instead.")
    	    content = String.format("[%d]%s", content.length(), content);
    	}

    	return content;
    }

    public static String fromArray(String[] fields) {

        List<String> encodedFields = new ArrayList<String>();

        for (String field : fields) {
            encodedFields.add(encodeField(field));
        }

        // TODO: Replace all use of "[", ";" & "\n" in code with constants.

        return TextUtils.join(";", encodedFields) + "\n";
    }
}
