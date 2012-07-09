package com.handbagdevices.handbag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class HandbagWiFiCommsService extends Service {

    // Messages from activities connecting to us


	static final int MSG_PARSE_SERVICE_REGISTERED = 502;

    static final int MSG_UI_CONNECT_TO_TARGET = 503;
    static final int MSG_UI_DISCONNECT_FROM_TARGET = 504;

    static final int MSG_COMMS_PACKET_RECEIVED = 505;
    static final int MSG_COMMS_SEND_PACKET = 506;


    static final int MSG_SETUP_ACTIVITY_REGISTER = 600;


	// Flag that should be checked
	private boolean shutdownRequested = false;

	// Used to communicate with the UI Activity & Communication Service
	Messenger uiActivity = null; // Orders us around
	Messenger parseService = null; // Receives our data, sends us its data.

    NetworkConnection targetNetworkConnection;


/*	private class TestSocketTask extends AsyncTask<Void, Void, String[]> {

		@Override
		protected String[] doInBackground(Void... params) {
			return doSocketTest();
		}

		@Override
		protected void onPostExecute(String[] result) {
			super.onPostExecute(result);

			if ((!shutdownRequested) && (result != null)) {
				// Only continue if we haven't been told to shutdown
				// and request was successful.

				// TODO: Notify caller of (reason for) unsuccessful request
				//       and/or leave message handler to check for null?

				// TODO: Do this properly send to parse service...
				try {
					Message msg = Message.obtain(null, HandbagUI.MSG_UI_TEST_ARRAY_MESSAGE);
					Bundle bundle = new Bundle();
					bundle.putStringArray(null, result);
					msg.setData(bundle);

					uiActivity.send(msg);

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}

					new TestSocketTask().execute();

				} catch (RemoteException e) {
					// UI Activity client is dead so no longer try to access it.
					uiActivity = null;
				}
			}

		}

	};
*/
/*	String[] doSocketTest() {
		Socket socket = null;

		DataOutputStream dataOutStream = null;
		DataInputStream dataInStream = null;

		String[] newPacket = null;

		// ---------
		// TODO: Should really handle this differently--like when first registered?
		//       (Also, these won't have been updated until the app paused...)
		SharedPreferences appPrefs = getSharedPreferences("handbag", MODE_PRIVATE); // Add MODE_MULTI_PROCESS ?

		String hostName = appPrefs.getString("network_host_name", "");

		if (hostName.isEmpty()) {
			Log.e(this.getClass().getSimpleName(), "No hostname provided.");
			return null; // TODO: Do something else
		}

		String hostPortAsString = appPrefs.getString("network_host_port", "");

		Log.d(this.getClass().getSimpleName(), "Port as string: " + hostPortAsString);

		if (hostPortAsString.isEmpty()) {
			hostPortAsString = "0";
		}

		Integer hostPort = Integer.valueOf(hostPortAsString);

		if (hostPort == 0) {
			hostPort = 0xba9;
		}

		Log.d(this.getClass().getSimpleName(), "Host: " + hostName +" Port: " + hostPort);

		// ---------

		// TODO: Check network available

		try {
			//socket = new Socket("www.google.com", 80);
			//socket = new Socket("74.125.237.100", 80);
			//socket = new Socket("10.1.1.3", 0xBA9);
			socket = new Socket(hostName, hostPort);
		} catch (UnknownHostException e) {
			Log.d(this.getClass().getSimpleName(), "Unknown Host."); // Note: Requires internet permission ***
		} catch (IOException e) {
			Log.d(this.getClass().getSimpleName(), "IOException when creating Socket."); // Note: Requires internet permission ***
		}

		Log.d(this.getClass().getSimpleName(), "socket: " + socket);

		if (socket != null) {
			try {
				dataOutStream = new DataOutputStream(socket.getOutputStream());
				dataInStream = new DataInputStream(socket.getInputStream());
			} catch (IOException e) {
				Log.d(this.getClass().getSimpleName(), "IOException when creating data streams.");
			}

			Log.d(this.getClass().getSimpleName(), "dataOutStream: " + dataOutStream);
			Log.d(this.getClass().getSimpleName(), "dataInStream: " + dataInStream);


			// TODO: Redo all this with one catch for IOException & use finally to close socket?
			if ((dataOutStream != null) && (dataInStream != null)) {
				try {
					dataOutStream.writeBytes("HELLO SERVER. I CAN HAZ PAGE?\n\n");

					Log.d("Got", "available: " + dataInStream.available());

                    PacketParser parser = new PacketParser(dataInStream);

					newPacket = parser.getNextPacket();

					Log.d(this.getClass().getSimpleName(), "Got result: " + Arrays.toString(newPacket));

				} catch (IOException e) {
					Log.d(this.getClass().getSimpleName(), "IOException when sending/reading data.");
					e.printStackTrace();
				}
			}

			if (socket!= null) { // TODO: Can this happen here?
				try {
					socket.close();
				} catch (IOException e) {
					Log.d(this.getClass().getSimpleName(), "IOException when closing Socket.");
				}
			}

			try {
				dataOutStream.close();
				dataInStream.close();
			} catch (NullPointerException ex1) {
				Log.d(this.getClass().getSimpleName(), "NullPointerException when closing data streams.");
			} catch (IOException e) {
				Log.d(this.getClass().getSimpleName(), "IOException when closing data streams.");
			}
		}

		return newPacket;

	}
*/




	// Used to receive messages from client(s)
	public class IncomingWiFiCommsServiceHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Log.d(this.getClass().getSimpleName(), "Tid (wifi):" + android.os.Process.myTid());

			Log.d(this.getClass().getSimpleName(), "WiFi Comms Service received message:");

			switch (msg.what) {

				case HandbagParseService.MSG_UI_ACTIVITY_REGISTER: // TODO: Move this constant into UI class?
					// TODO: Handle receiving this more than once?
					Log.d(this.getClass().getSimpleName(), "    MSG_UI_ACTIVITY_REGISTER");
					uiActivity = msg.replyTo;

					shutdownRequested = false;

					try {
						uiActivity.send(Message.obtain(null, HandbagUI.MSG_UI_ACTIVITY_REGISTERED)); // TODO: Change to specify who was registered with. ***
					} catch (RemoteException e) {
						// UI Activity client is dead so no longer try to access it.
						uiActivity = null;
					}

					if (uiActivity != null) {
						//startTestMessages();

						// Can't do this here because a callback counts as a main thread?
						//doSocketTest();

					}

					break;


				case HandbagParseService.MSG_PARSE_SERVICE_REGISTER: // TODO: Move this constant into UI class?
					// TODO: Handle receiving this more than once?
					Log.d(this.getClass().getSimpleName(), "    MSG_PARSE_SERVICE_REGISTER");
					parseService = msg.replyTo;

					try {
						parseService.send(Message.obtain(null, MSG_PARSE_SERVICE_REGISTERED));
					} catch (RemoteException e) {
						// UI Activity client is dead so no longer try to access it.
						parseService = null;
					}

					break;


                case MSG_UI_CONNECT_TO_TARGET:
                    Log.d(this.getClass().getSimpleName(), "    MSG_UI_CONNECT_TO_TARGET");
                    connectToTarget(msg.getData());
                    // TODO: Send some sort of response?
                    break;


                case MSG_COMMS_SEND_PACKET:
                    Log.d(this.getClass().getSimpleName(), "    MSG_COMMS_SEND_PACKET");
                    sendPacketToTarget(msg.getData().getStringArray(null));
                    // TODO: Return some sort of response?
                    break;


                case MSG_UI_DISCONNECT_FROM_TARGET:
                    Log.d(this.getClass().getSimpleName(), "    MSG_UI_DISCONNECT_FROM_TARGET");
                    if (targetNetworkConnection != null) {
                        Log.d(this.getClass().getSimpleName(), "    Cancelling target connection.");
                        targetNetworkConnection.cancel(true);
                        try {
                            targetNetworkConnection.socket.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            // TODO: Handle this better?
                            Log.d(this.getClass().getSimpleName(), "    Ignoring NullPointerException (probably due to failure to connect).");
                        }
                    }
                    break;


				case HandbagParseService.MSG_UI_SHUTDOWN_REQUEST: // TODO: Move this constant into UI class?
					Log.d(this.getClass().getSimpleName(), "    MSG_UI_SHUTDOWN_REQUEST");

					// Set a flag so repeating sub-tasks will stop themselves.
					shutdownRequested  = true;
					// TODO: Be more proactive and stop them here instead?

                    if (targetNetworkConnection != null) {
                        Log.d(this.getClass().getSimpleName(), "    Cancelling target connection.");
                        targetNetworkConnection.cancel(true); // TODO: Handle like MSG_UI_DISCONNECT_FROM_TARGET above.
                    }
					break;

				default:
                    Log.d(this.getClass().getSimpleName(), "    (unknown): " + msg.what);
					super.handleMessage(msg);
			}
		}

        private void connectToTarget(Bundle data) {
            String hostName = data.getString("hostName");
            Integer hostPort = data.getInt("hostPort");

            Log.d(this.getClass().getSimpleName(), "Host: " + hostName + " Port: " + hostPort);

            targetNetworkConnection = new NetworkConnection(hostName, hostPort);

            targetNetworkConnection.connect();

        }

	}

	// TODO: Pull this class into separate file?
    public class NetworkConnection extends AsyncTask<Void, String[], Integer> {

        private String hostName;
        private int hostPort;

        private Socket socket = null;

        private DataOutputStream dataOutStream;
        private DataInputStream dataInStream;

        private PacketParser parser;

        public BlockingQueue<String[]> packetsToSendQueue = new LinkedBlockingQueue<String[]>();

        public BlockingQueue<String[]> packetsReceivedQueue = new LinkedBlockingQueue<String[]>();

        public NetworkConnection(String hostName, int hostPort) {
            super();
            this.hostName = hostName;
            this.hostPort = hostPort;
        }


        private void connect() {
            this.execute();
        }


        private boolean setUp() {

            // TODO: Check network available

            try {
                // Note: Requires internet permission ***
                socket = new Socket(hostName, hostPort);
            } catch (UnknownHostException e) {
                Log.d(this.getClass().getSimpleName(), "Unknown Host.");
            } catch (IOException e) {
                Log.d(this.getClass().getSimpleName(), "IOException when creating socket.");
            }

            Log.d(this.getClass().getSimpleName(), "Socket: " + socket);

            // TODO: Handle differently if socket open fails?
            if (socket != null) {
                try {
                    dataInStream = new DataInputStream(socket.getInputStream());
                    dataOutStream = new DataOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    Log.d(this.getClass().getSimpleName(), "IOException when creating data streams.");
                }

                Log.d(this.getClass().getSimpleName(), "dataInStream: " + dataInStream);
                Log.d(this.getClass().getSimpleName(), "dataOutStream: " + dataOutStream);

                if ((dataInStream != null) && (dataOutStream != null)) {
                    parser = new PacketParser(packetsReceivedQueue, dataInStream);
                }
            }

            // TODO: Do handshake/version check when first connected?

            return ((socket != null) && (dataInStream != null) && (dataOutStream != null));
        }


        @Override
        protected Integer doInBackground(Void... params) {
            String[] newPacket;

            int result = -1;

            if (setUp()) {
                result = 0;

                // TODO: Do proper handshake.
                // Note: We have to send bytes first for an Arduino-based network device to detect us.
                try {
                    dataOutStream.writeBytes(PacketGenerator.fromArray(new String[] { "HB2" }));
                    dataOutStream.flush();
                } catch (IOException e1) {
                    Log.d(this.getClass().getSimpleName(), "IOException sending initial handshake packet.");
                    e1.printStackTrace();
                    // TODO: Bail properly here.
                    return -1;
                }

                parser.start();

                Log.d(this.getClass().getSimpleName(), "Entering main data transfer handling loop.");

                while (true) {
                    if (this.isCancelled()) {
                        parser.interrupt();
                        Log.d(this.getClass().getSimpleName(), "Connection canceled.");
                        break;
                    }

                    try {
                        newPacket = this.packetsReceivedQueue.poll(1, TimeUnit.MILLISECONDS);

                        if (newPacket != null) {
                            Log.d(this.getClass().getSimpleName(), "Got result: " + Arrays.toString(newPacket));

                            publishProgress(newPacket);
                        }
                    } catch (InterruptedException e) {
                        // TODO: Handle properly?
                        Log.d(this.getClass().getSimpleName(), "InterruptedException handling received packet.");
                    }


                    try {
                        // TODO: Figure out why neither of these return true with the Python test server.
                        if (!socket.isConnected()) {
                            Log.d(this.getClass().getSimpleName(), "Socket is no longer connected.");
                            break;
                        }
                        if (socket.isClosed()) {
                            Log.d(this.getClass().getSimpleName(), "Socket is closed.");
                            break;
                        }

                        String[] packetToSend = this.packetsToSendQueue.poll(1, TimeUnit.MILLISECONDS);

                        if (packetToSend != null) {
                            Log.d(this.getClass().getSimpleName(), "Sending packet. ");
                            dataOutStream.writeBytes(PacketGenerator.fromArray(packetToSend));
                            dataOutStream.flush();
                        }

                    } catch (IOException e) {
                        // TODO: Handle properly?
                        Log.d(this.getClass().getSimpleName(), "IOException sending packet.");
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO: Handle properly?
                        Log.d(this.getClass().getSimpleName(), "InterruptedException sending packet.");
                    }

                }
            }

            cleanUp();

            return result; // TODO: Return something more useful?
        }


        private void deliverPacket(String[] packet) {

            Log.d(this.getClass().getSimpleName(), "Enter 'deliverPacket'.");

            Log.d(this.getClass().getSimpleName(), "    'shutdownRequested`:" + shutdownRequested);

            // TODO: Do this properly...
            if (!shutdownRequested) {
                // Only continue if we haven't been told to shutdown

                try {
                    Message msg = Message.obtain(null, MSG_COMMS_PACKET_RECEIVED);
                    Bundle bundle = new Bundle();
                    bundle.putStringArray(null, packet);
                    msg.setData(bundle);

                    parseService.send(msg);
                } catch (RemoteException e) {
                    // Parse service client is dead so no longer try to access it.
                    parseService = null;
                }
            }


            Log.d(this.getClass().getSimpleName(), "Exit 'deliverPacket'.");

        }


        @Override
        protected void onProgressUpdate(String[]... values) {
            deliverPacket(values[0]);
        }


        private void loggedClose(Socket theSocket, String socketName) { // TODO: Determine name automatically?
            /*
               To avoid the following boilerplate:
             */

            if (theSocket != null) {
                try {
                    // Note: Don't need to close associated streams as docs say:
                    //       "Closing this socket will also close the socket's InputStream and OutputStream."
                    //       <http://docs.oracle.com/javase/6/docs/api/java/net/Socket.html#close()>
                    theSocket.close();
                } catch (IOException e) {
                    Log.d(this.getClass().getSimpleName(), "IOException when closing: " + socketName);
                }
            }
        }


        private void cleanUp() {
            loggedClose(socket, "socket");
        }


        @Override
        protected void onPostExecute(Integer result) {
            // TODO: Do something useful? e.g. send message to UI/data this etc?
        }
    }

    // Clients will use this to communicate with us.
    final Messenger ourMessenger = new Messenger(new IncomingWiFiCommsServiceHandler());

	@Override
	public IBinder onBind(Intent intent) {
		// Return our `messenger` instance to enable clients to send us messages.
		Log.d(this.getClass().getSimpleName(), "onBind entered");
		return ourMessenger.getBinder();
	}


    public void sendPacketToTarget(String[] packet) {
        targetNetworkConnection.packetsToSendQueue.add(packet);
    }

}
