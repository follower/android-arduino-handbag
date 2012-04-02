// Compile & run with: javac ParseHandbagPacket.java && java ParseHandbagPacket

import java.io.*;
import java.util.*;

class ParseHandbagPacket {

    final static int PARSE_STATE_FIRST_CHAR = 0;
    final static int PARSE_STATE_NORMAL = 1;

    // TODO: Fix up static related stuff...
    static StringReader input;

    private static int saferRead() {
	int c = -1;

	try {
	    c = input.read();
	} catch (IOException e) {
	    // Treat IO Exceptions as end of stream?
	}
	
	if (c==-1) {
	    // TODO: How handle partially read packet?
	    // TODO: What here? Big barf exception?
	    throw new RuntimeException();
	}

	return c;
    }

    public static void main(String [ ] args) {

	String data = "abc;[12]abcdefghijkl;123;[5]a;b\nc\n";

	input = new StringReader(data);

	int c;

	int state = PARSE_STATE_FIRST_CHAR;

	StringBuilder field = new StringBuilder();

	List<String> all_fields = new ArrayList<String>(); 

	while (true) {

	    try {
		c = saferRead();
	    } catch (RuntimeException e) {
		break;
	    }

	    //System.out.print((char) c);

	    switch (state) {
	        case PARSE_STATE_FIRST_CHAR:
		    if (c == '[') {
			// Read length & string here
			int chars_to_read = 0;
			
			while (true) {
			    c = saferRead();

			    switch(c) {
			        case ']':
				    //System.out.println("length: " + chars_to_read);
				    
				    for (int i = 0; i < chars_to_read; i++) {
					c = saferRead();
					field.append((char) c);
				    }
				    break;

			        default:
				    // TODO: Bail if not digits?
				    chars_to_read = (10 * chars_to_read) +
					Character.getNumericValue(c);
				    continue;
			    }
			    break;
			}
			state = PARSE_STATE_NORMAL; // Force next read
			continue;
		    }

		    state = PARSE_STATE_NORMAL;
		    // Drop through to "normal"

	        case PARSE_STATE_NORMAL:
		    if ((c == '\n') || (c == ';')) {
			all_fields.add(field.toString());
			field.setLength(0);
			state = PARSE_STATE_FIRST_CHAR;
			
			if (c != '\n') {
			    continue;
			}
		    } else {
			field.append((char) c);
			continue;
		    }
		    break;

	        default:
		    break; // TODO: Log unknown.
	    }
	};
	System.out.println(all_fields.toString());
    }
}