package com.handbagdevices.handbag;

import java.io.*;
import java.util.*;

class PacketParser {

    private InputStreamReader input;

    private Scanner scanner;


    private StringBuilder currentFieldContent = new StringBuilder();

    private List<String> fieldsInPacket = new ArrayList<String>();


    PacketParser(InputStream theInput) {
        input = new InputStreamReader(theInput);
        scanner = new Scanner(input);
    }

    public String[] getNextPacket() {
        boolean packetComplete = false;
        String token;

        currentFieldContent.setLength(0);
        fieldsInPacket.clear();


        // TODO/NOTE: This doesn't avoid blocking mid-packet, only at the beginning.
        //            We assume that if there is any data available then a complete
        //            packet will be available in a "reasonable time frame".

        boolean inputReady = false;

        try {
            inputReady = input.ready();
        } catch (IOException e) {
            e.printStackTrace();
            // Leaves `inputReady` as previous value. i.e. false
        }

        if (!inputReady) {
            return new String[] {};
        }

        while (true) {

            // TODO: Fix this so we don't end up getting a character
            // at a time... (I think from the last '?' character in the regex?)
            scanner.useDelimiter("((?=\\[|;|\n))?");

            if (packetComplete) {
                break;
            }

            // TODO: Handle blocking (here & elsewhere)?
            token = scanner.next();

            // Log.d(this.getClass().getSimpleName(), "Got token: " + token);

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