#include "HandbagProtocolMixIn.h"

#include <SPI.h>
#include <Ethernet.h>


class NetworkHandbag : public HandbagProtocolMixIn {

private:
  EthernetServer& server;

  EthernetClient client;

  boolean uiIsSetup;

public:
  NetworkHandbag(EthernetServer& server) : server(server) {
    /*
     */
    // Ideally we'd use 'Server' instead but it doesn't have a 'available()' method
    // we can use.
    // Maybe we need to accept a 'Client' at a different stage...
    // TODO: Handle all this better.

    uiIsSetup = false;
    client = EthernetClient();
  }

  int begin(BASIC_CALLBACK(setupUICallback)) {
    this->setupUICallback = setupUICallback;

    server.begin();

    return 1;
  }


  void refresh() {
    /*
     */

    if (!uiIsSetup) { // This implies we're not already connected to someone,
                      // whereas checking `client` just says if there's data
                      // available--it can be connected but still false.
      client = server.available();
    }

    if (client) {

      if (!uiIsSetup) {
        Serial.println("setup ui");
        // TODO: Initialise here

        // TODO: Reset widget ids etc.
        reset();

        strm = &client;

        if (setupUICallback != NULL) {
          // NOTE: This *must* only be called after 'strm' is valid.
          setupUICallback();
        }

        uiIsSetup = true;
      }

      // TODO: Simplify/improve the handling of disconnected/available issues.
      if (client.connected()) {

        processPacket();

      } else {

	Serial.println("client is being stopped. ");

        client.stop(); // TODO: Unnecessary and/or enough?
        uiIsSetup = false;
      }
    }
  }


  boolean isConnected() {
    /*
     */
    // Note: This kind of makes refresh() redundant and I'm
    //       not convinced this is the best approach.
    // TODO: Make better?
    // TODO: Make sure all communication routines check for connection first?
    refresh();
    return uiIsSetup;
  }

};

#define HandbagApp NetworkHandbag
