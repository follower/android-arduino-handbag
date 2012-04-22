// Compile & run with: javac GenerateHandbagPacket.java && java GenerateHandbagPacket

import java.util.*;

class GenerateHandbagPacket {

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

	// TODO: Use android text util approach
	String result = "";

	for (String field : encodedFields) {
	    result += field + ";";
	}

	// TODO: NOTE: This isn't correct because the last field shouldn't end with ";".

	result += "\n";

	return result;
    }

    public static void main(String [ ] args) {

	// TODO: Handle collections/non-string array entries automatically?
	String[] fields = new String[] {"abcdef", "4567", "My goodness;\nI spot a newline!", "1", "[bad"};

	System.out.println(fromArray(fields));

    }
}