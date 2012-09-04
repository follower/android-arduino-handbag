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

// The following is a hacky work around to enable us to have both
// the Network and USB implementation in one library folder.
// The Arduino IDE tries to compile all *.cpp files in the library
// folder which will fail when the sketch is Network only (thus
// doesn't include the USB headers) or USB only (thus doesn't
// include the Network headers). If libraries could include
// other libraries then we could probably work around this another
// way but we can't, so we're stuck with this approach.
#include "Handbag.cpp_"

#endif
