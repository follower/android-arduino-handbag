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

#endif
