package com.handbagdevices.handbag;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import android.util.Log;

class PacketParser extends Thread {

    private InputStreamReader input;

    private BlockingQueue<String[]> packetsReceivedQueue;


    private StringBuilder currentFieldContent = new StringBuilder();

    private List<String> fieldsInPacket = new ArrayList<String>();


    PacketParser(BlockingQueue<String[]> packetsReceivedQueue, InputStream theInput) {
        input = new InputStreamReader(theInput);

        this.packetsReceivedQueue = packetsReceivedQueue;
    }


    @Override
    public synchronized void run() {

        Log.d(this.getClass().getSimpleName(), "Parser started.");

        while (true) {
            try {
                packetsReceivedQueue.put(getNextPacket());
            } catch (IOException e) {
                Log.d(this.getClass().getSimpleName(), "IOException while getting/putting next packet.");
                break;
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


    private String[] getNextPacket() throws IOException {
        boolean packetComplete = false;
        String token;

        currentFieldContent.setLength(0);
        fieldsInPacket.clear();

        while (true) {

            if (packetComplete) {
                break;
            }

            token = Character.toString((char) input.read());

            // Log.d(this.getClass().getSimpleName(), "Got token: " + token);

            switch (token.charAt(0)) { // TODO: Do properly.

                case '\n': // Fall through
                    packetComplete = true;
                case ';':
                    fieldsInPacket.add(currentFieldContent.toString());
                    currentFieldContent.setLength(0);
                    break;

                case '[':
                    // TODO: Do this properly:
                    int stringLength = 0;
                    while (true) {
                        token = Character.toString((char) input.read());
                        if (token.equals("]")) {
                            break;
                        }
                        stringLength = (stringLength * 10) + Integer.valueOf(token);
                    }

                    for (int i = 0; i < stringLength; i++) {
                        currentFieldContent.append(Character.toString((char) input.read()));
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