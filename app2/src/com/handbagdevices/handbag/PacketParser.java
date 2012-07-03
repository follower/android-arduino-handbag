package com.handbagdevices.handbag;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import android.util.Log;

class PacketParser extends Thread {

    private InputStreamReader input;

    private Scanner scanner;

    private BlockingQueue<String[]> packetsReceivedQueue;


    private StringBuilder currentFieldContent = new StringBuilder();

    private List<String> fieldsInPacket = new ArrayList<String>();


    PacketParser(BlockingQueue<String[]> packetsReceivedQueue, InputStream theInput) {
        input = new InputStreamReader(theInput);
        scanner = new Scanner(input);

        this.packetsReceivedQueue = packetsReceivedQueue;
    }


    @Override
    public synchronized void run() {

        Log.d(this.getClass().getSimpleName(), "Parser started.");

        while (true) {
            try {
                packetsReceivedQueue.put(getNextPacket());
            } catch (InterruptedException e) {
                Log.d(this.getClass().getSimpleName(), "InterruptedException while getting/putting next packet.");
                break;
            } catch (NoSuchElementException e) {
                Log.d(this.getClass().getSimpleName(),
                        "NoSuchElementException while getting/putting next packet (probably due to disconnect).");
                break;
            }
        }
    }


    private String[] getNextPacket() {
        boolean packetComplete = false;
        String token;

        currentFieldContent.setLength(0);
        fieldsInPacket.clear();

        while (true) {

            // TODO: Fix this so we don't end up getting a character
            // at a time... (I think from the last '?' character in the regex?)
            scanner.useDelimiter("((?=\\[|;|\n))?");

            if (packetComplete) {
                break;
            }

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

        Log.d(this.getClass().getSimpleName(), "Complete packet received.");

        return fieldsInPacket.toArray(new String[] {});
    }
}