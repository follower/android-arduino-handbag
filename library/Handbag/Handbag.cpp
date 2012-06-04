/*

  Handbag library -- create Android accessories without Android code

  (c) 2011-2012 follower@rancidbacon.com

  Licensed under the LGPL 2.0.

 */

#include "Handbag.h"

// Requires Serial to have been initialised.
#define LOG_DEBUG(x) Serial.println(x)

void HandbagApp::sendWidgetConfiguration(byte widgetType, byte widgetId, byte fontSize, byte widgetAlignment, const char *labelText) {
  /*
   */
  // TODO: Do something stream based instead.
  byte msg[MSG_BUFFER_SIZE];
  byte offset = 0;
  
  byte lengthToCopy = 0;
  
  msg[offset++] = MESSAGE_CONFIGURE;
  msg[offset++] = widgetType;
  
  msg[offset++] = widgetId;

  msg[offset++] = fontSize;
  msg[offset++] = widgetAlignment;
  
  lengthToCopy = MSG_BUFFER_SIZE - (offset + 1);
  
  if (strlen(labelText) < lengthToCopy) {
    lengthToCopy = strlen(labelText);
  }
  
  msg[offset++] = lengthToCopy;
  
  memcpy(msg+offset, labelText, lengthToCopy);
  
  offset += lengthToCopy;
  
  accessory.write(msg, offset);
}  


void HandbagApp::setupUI() {
  /*
   */
  widgetCount = 0; // TODO: Reset more than just this?
  
  if (setupUICallback != NULL) {
    setupUICallback();
  }

  uiIsSetup = true; 
}


// TODO: Set type-specific things like "label text" differently?
int HandbagApp::addWidget(WIDGET_TYPE widgetType, CALLBACK(callback), byte fontSize, byte widgetAlignment, const char *labelText, CALLBACK2(callback2)) {
  /*
   */

  if (widgetCount == MAX_WIDGETS) {
    return -1;
  }

  Widget& theWidget = widgets[widgetCount];

  theWidget.id = widgetCount;
  widgetCount++;
  
  theWidget.type = widgetType;
  theWidget.callback = callback;
  theWidget.callback2 = callback2;
  
  // TODO: Wait for confirmation?
  sendWidgetConfiguration(theWidget.type, theWidget.id, fontSize, widgetAlignment, labelText);
  
  return theWidget.id;
}  

void HandbagApp::setText(int widgetId, const char *labelText, byte fontSize, byte widgetAlignment) {
  /*
   */
  if (widgetId < widgetCount) {
    // TODO: Actually search to match the ID?
    Widget& theWidget = widgets[widgetId];
    
    // Check this is valid for this widget
    if ((theWidget.type == UI_WIDGET_TYPE_LABEL) ||
	(theWidget.type == UI_WIDGET_TYPE_BUTTON)) { 

      sendWidgetConfiguration(UI_WIDGET_TYPE_LABEL, theWidget.id,
			      fontSize, widgetAlignment, labelText);
    }
  }
}


void HandbagApp::triggerButtonCallback(int widgetId) {
  /*
   */
  if (widgetId < widgetCount) {
    // TODO: Actually search to match the ID?
    Widget& theWidget = widgets[widgetId];
    
    if ((theWidget.type == UI_WIDGET_TYPE_BUTTON) && (theWidget.callback != NULL)) {
      theWidget.callback();
    }
  }
}


void HandbagApp::triggerTextInputCallback(int widgetId, char *theString) {
  /*
   */
  if (widgetId < widgetCount) {
    // TODO: Actually search to match the ID?
    Widget& theWidget = widgets[widgetId];
    
    // TODO: Fix up this callback stuff.
    if ((theWidget.type == UI_WIDGET_TYPE_TEXT_INPUT) &&
	(theWidget.callback2 != NULL)) {
      theWidget.callback2(theString);
    }
  }
}


HandbagApp::HandbagApp(AndroidAccessory& accessory) : accessory(accessory) {
  /*
   */
   uiIsSetup = false;
}


int HandbagApp::begin(CALLBACK(theSetupUICallback)) {
  /*
   */
  // TODO: Find a more friendly way to supply UI configuration/callback?

  setupUICallback = theSetupUICallback;
  
  accessory.begin();
}


void HandbagApp::refresh() {
  /*
   */
  byte msg[3];

  // TODO: Ensure we're not called too often? (Original had 'delay(10)'.)

  accessory.refresh();

  if (accessory.isConnected()) {
    if (!uiIsSetup) {
      // Do protocol version handshake.
      msg[0] = 'H';
      msg[1] = 'B';
      msg[2] = 0x01;
      accessory.write(msg, sizeof(msg));

      // Wait for response.
      unsigned long timeout = millis() + 2000;

      boolean timedOut = true;

      int len;

      while (millis() < timeout) {
	len = accessory.readBytes((char *) msg, sizeof(msg));

	if (len > 0) {
	  timedOut = false;
	  break;
	}
      }

      if (timedOut 
	  || (len < 3) 
	  || !((msg[0] == 'H') && (msg[1] == 'B') && (msg[2] == 0x01))) {
	// No response, short response or bad response received so we
	// assume it's an "old" version.
	// TODO: Print error message to Serial for debugging?
	// Serial.println("Version mismatch.");
	return;
      }

      setupUI();
    }
    
    // TODO: Change this to all be stream based.
    int len = accessory.readBytes((char *) msg, sizeof(msg));
    
    if (len > 0) {
      // Requires only one command per "packet".
      // TODO: Check actual length received?
      // Currently bytes are: (command_type, arg1, arg2)
      // For command type event occured: arg 1 = event type, arg2 = widget id 
      byte widgetId;

      switch (msg[0]) {
        case COMMAND_GOT_EVENT:
          switch (msg[1]) {
            case EVENT_BUTTON_CLICK:
              triggerButtonCallback(msg[2]);
              break;

            case EVENT_TEXT_INPUT:
	      len = 0;
	      widgetId = msg[2];
	      // TODO: Provide timeout on -1's/0
	      while (len <= 0) {
		len = accessory.readBytes((char *) msg, sizeof(msg));
	      }
	      if (len > 0) {
		byte lengthToRequest = msg[2];
		// TODO: Fix all this
#define MAX_SIZE 30
		char theString[MAX_SIZE];
		len = 0;
		// TODO: Provide timeout on -1's
		while (len <= 0) {
		  len = accessory.readBytes(theString, MAX_SIZE-1);
		}

		if (len > 0) {
		  theString[len] = '\0';
		  triggerTextInputCallback(widgetId, theString);
		}
	      }
	      break;
              
            default:
              break;
          }
          break;
        
        default:
          break;
      }
    }
  } else {
    // TODO: Handle disconnection.
    // TODO: Move widget configuration to happen when connected? (Or also via a callback?)
    uiIsSetup = false;
  }
  
}

boolean HandbagApp::isConnected() {
  /*
   */
  // Note: This kind of makes refresh() redundant and I'm
  //       not convinced this is the best approach.
  // TODO: Make better?
  // TODO: Make sure all communication routines check for connection first?
  refresh();
  return uiIsSetup;
}


int HandbagApp::addLabel(const char *labelText, byte fontSize, byte alignment) {
  /*


    alignment - See the valid "gravity" values from here:

      * <http://developer.android.com/reference/android/widget/TextView.html#attr_android:gravity>

   */
  return addWidget(UI_WIDGET_TYPE_LABEL, NULL, fontSize, alignment, labelText);
}


int HandbagApp::addButton(const char *labelText, CALLBACK(callback)) {
  /*
   */
  return addWidget(UI_WIDGET_TYPE_BUTTON, callback, 0, 0, labelText);
}


int HandbagApp::addTextInput(CALLBACK2(callback2)) {
  /*

     Note: The string supplied to the callback must be copied if it
           is used after the callback exits.

   */
  return addWidget(UI_WIDGET_TYPE_TEXT_INPUT, NULL, 0, 0, NULL, callback2);
}

void HandbagApp::showDialog(const char *messageText) {
  /*
   */
  
  // TODO: Wait for confirmation?
  // TODO: Do this properly
  sendWidgetConfiguration(UI_WIDGET_TYPE_DIALOG, 0, 0, 0, messageText);
}
