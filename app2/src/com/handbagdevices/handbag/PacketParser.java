package com.handbagdevices.handbag;

import java.io.*;
import java.util.*;

class PacketParser {

    private Reader input;

    private Scanner scanner;


    private StringBuilder currentFieldContent = new StringBuilder();

    private List<String> fieldsInPacket = new ArrayList<String>(); 


    PacketParser(Reader theInput) {
	input = theInput;
	scanner = new Scanner(input);
    }

    public String[] getNextPacket() {
	boolean packetComplete = false;
	String token;

	currentFieldContent.setLength(0);
	fieldsInPacket.clear();

	while (true) {

	    // TODO: Fix this so we don't end up getting a character
	    //       at a time... (I think from the last '?' character in the regex?)
	    scanner.useDelimiter("((?=\\[|;|\n))?");

	    if ((!scanner.hasNext()) || (packetComplete)) {
	    	break;
	    }

	    // TODO: Handle blocking (here & elsewhere)?
	    token = scanner.next();

	    switch (token.charAt(0)) {

	        case '\n': // Fall through
		    packetComplete = true;
		case ';':
		    fieldsInPacket.add(currentFieldContent.toString());
		    currentFieldContent.setLength(0);
		    break;

		case '[':
		    scanner.useDelimiter("]");
		    int stringLength = scanner.nextInt();
		    scanner.useDelimiter("");
		    scanner.next(); // Skip the trailing "]". TODO: Avoid this?
		    for (int i = 0; i < stringLength; i++) {
			currentFieldContent.append(scanner.next());
		    }
		    break;

		default:
		    currentFieldContent.append(token);
		    break;
	    }

	}

	// TODO: Handle incomplete packets.

	return fieldsInPacket.toArray(new String[] {});
    }
}