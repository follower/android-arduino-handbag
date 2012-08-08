#ifndef __HANDBAG_H__
#define __HANDBAG_H__

#include <AndroidAccessory.h>

#include "HandbagProtocolMixIn.h"


class UsbHandbag : public HandbagProtocolMixIn {

private:
  AndroidAccessory& accessory;

  boolean uiIsSetup; // TODO: Make part of MixIn?

public:
  UsbHandbag(AndroidAccessory& accessory);

  // TODO: Make these part of MixIn?
  int begin(BASIC_CALLBACK(setupUICallback));

  void refresh();

  boolean isConnected();

};

// This enables us to have a single class name used publically
#define HandbagApp UsbHandbag

#endif
