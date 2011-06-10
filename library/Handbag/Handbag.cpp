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
int HandbagApp::addWidget(WIDGET_TYPE widgetType, CALLBACK(callback), byte fontSize, byte widgetAlignment, const char *labelText) {
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
  
  accessory.powerOn();
  
  while (!accessory.isConnected()) {
    // TODO: Don't do this here?
    // Wait for connection
    // TODO: Do time out?
  }

  // TODO: Do protocol version handshake.
  // TODO: Return result status.

  // This ensures the UI etc is setup once connected because we have
  // no control over when it's first called otherwise.
  refresh();
}


void HandbagApp::refresh() {
  /*
   */
  byte msg[3];

  // TODO: Ensure we're not called too often? (Original had 'delay(10)'.)

  if (accessory.isConnected()) {
    if (!uiIsSetup) {
      setupUI();
    }
    
    // TODO: Change this to all be stream based.
    int len = accessory.read(msg, sizeof(msg), 1);
    
    if (len > 0) {
      // Requires only one command per "packet".
      // TODO: Check actual length received?
      // Currently bytes are: (command_type, arg1, arg2)
      // For command type event occured: arg 1 = event type, arg2 = widget id 
      switch (msg[0]) {
        case COMMAND_GOT_EVENT:
          switch (msg[1]) {
            case EVENT_BUTTON_CLICK:
              triggerButtonCallback(msg[2]);
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

