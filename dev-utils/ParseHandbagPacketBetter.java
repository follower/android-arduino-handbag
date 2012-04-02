// Compile & run with: javac ParseHandbagPacketBetter.java && java ParseHandbagPacketBetter

import java.io.*;
import java.util.*;

class ParseHandbagPacketBetter {

    public static void main(String [ ] args) {

	String data = "abc;[12]abcdefghijkl;123;[5]a;b\nc\n";

	StringReader input = new StringReader(data);

	StringBuilder field = new StringBuilder();

	List<String> all_fields = new ArrayList<String>(); 


	Scanner sc = new Scanner(input);

	while (true) {

	    // TODO: Fix this so we don't end up getting a character
	    //       at a time... (I think from the last '?' character in the regex?)
	    sc.useDelimiter("(?=\\[|;|\n)?");

	    if (!sc.hasNext()) {
		break;
	    }

	    String token = sc.next();

	    //System.out.println(">> " + token);

	    switch (token.charAt(0)) {

	        case '\n': // Fall through
		case ';':
		    all_fields.add(field.toString());
		    field.setLength(0);
		    break;

		case '[':
		    sc.useDelimiter("]");
		    int chars_to_read = sc.nextInt();
		    //System.out.println(chars_to_read);
		    sc.useDelimiter("");
		    sc.next(); // Skip the trailing "]". TODO: Avoid this?
		    field.setLength(0);
		    for (int i = 0; i < chars_to_read; i++) {
			field.append(sc.next());
		    }
		    break;

		default:
		    field.append(token);
		    break;
	    }

	    // System.out.println("{" + theField + "}");
	}
	System.out.println(all_fields.toString());
    }
}