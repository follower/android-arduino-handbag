#include "Handbag.h"

UsbHandbag::UsbHandbag(AndroidAccessory& accessory) : accessory(accessory) {
  /*
   */

  uiIsSetup = false;
}

int UsbHandbag::begin(BASIC_CALLBACK(setupUICallback)) {

  // TODO: Find a more friendly way to supply UI configuration/callback?

  this->setupUICallback = setupUICallback;

  accessory.begin();

  return 1;
}


void UsbHandbag::refresh() {
  /*
   */

  // TODO: Ensure we're not called too often? (Original had 'delay(10)'.)

  accessory.refresh();

  if (accessory.isConnected()) {
    if (!uiIsSetup) {
      // TODO: Do protocol version handshake.
      // *****

      // TODO: Put this common functionality in MixIn?

      // TODO: Initialise here

      // TODO: Reset widget ids etc.
      reset();

      strm = &accessory;

      if (setupUICallback != NULL) {
	// NOTE: This *must* only be called after 'strm' is valid.
	setupUICallback();
      }

      uiIsSetup = true;
    }

    processPacket();

  } else {
    // TODO: Handle disconnection.
    // TODO: Move widget configuration to happen when connected? (Or also via a callback?)
    uiIsSetup = false;
  }

}


boolean UsbHandbag::isConnected() {
  /*
   */
  // Note: This kind of makes refresh() redundant and I'm
  //       not convinced this is the best approach.
  // TODO: Make better?
  // TODO: Make sure all communication routines check for connection first?
  refresh();
  return uiIsSetup;
}
