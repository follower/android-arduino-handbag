package com.handbagdevices.handbag;

import java.io.*;
import java.util.*;

import android.util.Log;

class PacketParser {

    private InputStream input;

    private Scanner scanner;


    private StringBuilder currentFieldContent = new StringBuilder();

    private List<String> fieldsInPacket = new ArrayList<String>();


    PacketParser(InputStream theInput) {
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
            // at a time... (I think from the last '?' character in the regex?)
            scanner.useDelimiter("((?=\\[|;|\n))?");

            if ((packetComplete) || (!scanner.hasNext())) {
                break;
            }

            // TODO: Handle blocking (here & elsewhere)?
            token = scanner.next();

            Log.d(this.getClass().getSimpleName(), "Got token: " + token);

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

        if (scanner.ioException() != null) {
            if (scanner.ioException().getClass().equals(java.net.SocketTimeoutException.class)) {
                Log.d(this.getClass().getSimpleName(), "Last exception: " + scanner.ioException());
                Log.d(this.getClass().getSimpleName(), "  currentFieldContent length: " + currentFieldContent.length());
                Log.d(this.getClass().getSimpleName(), "  fieldsInPacket size: " + fieldsInPacket.size());
                if ((currentFieldContent.length() == 0) && (fieldsInPacket.size() == 0)) {
                    // Note: This is required because the docs for Scanner say ~"If the underlying read() method
                    //       throws an IOException then the scanner assumes that the end of the input has been reached."
                    scanner = new Scanner(input);
                }
                // TODO: Throw an error on a mid-packet timeout...
            }
        }

        // TODO: Handle incomplete packets.

        return fieldsInPacket.toArray(new String[] {});
    }
}