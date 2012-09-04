#ifndef __NETWORK_HANDBAG_H__
#define __NETWORK_HANDBAG_H__

#include <SPI.h>
#include <Ethernet.h>

#include "HandbagProtocolMixIn.h"


class NetworkHandbag : public HandbagProtocolMixIn {

private:
  EthernetServer& server;

  EthernetClient client;

  boolean uiIsSetup;

public:
  NetworkHandbag(EthernetServer& server);

  int begin(BASIC_CALLBACK(setupUICallback));

  void refresh();

  boolean isConnected();

};

// This enables us to have a single class name used publically
#define HandbagApp NetworkHandbag

// The following is a hacky work around to enable us to have both
// the Network and USB implementation in one library folder.
// The Arduino IDE tries to compile all *.cpp files in the library
// folder which will fail when the sketch is Network only (thus
// doesn't include the USB headers) or USB only (thus doesn't
// include the Network headers). If libraries could include
// other libraries then we could probably work around this another
// way but we can't, so we're stuck with this approach.
#include "NetworkHandbag.cpp_"

#endif
