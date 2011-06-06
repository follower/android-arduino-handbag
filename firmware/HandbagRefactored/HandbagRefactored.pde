#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

// Requires Serial to have been initialised.
#define LOG_DEBUG(x) Serial.println(x)

AndroidAccessory acc("rancidbacon.com",
		     "Handbag",
		     "Handbag (Arduino Board)",
		     "0.1",
		     "http://rancidbacon.com",
		     "0000000000000001");

#define CALLBACK(varname) void (*varname)()


#define UI_WIDGET_TYPE_BUTTON 0x00
#define UI_WIDGET_TYPE_LABEL 0x01


#define WIDGET_TYPE unsigned int

class Widget {
  private:
    CALLBACK(callback);
    unsigned int id;
    unsigned int type;
    
    friend class HandbagApp;
};

#define MAX_WIDGETS 20

#define MESSAGE_CONFIGURE 0x10

class HandbagApp {

private:
  AndroidAccessory& accessory;
  
  // TODO: Dynamically allocate this?
  Widget widgets[MAX_WIDGETS];
  
  unsigned int widgetCount;

// TODO: Dynamically allocate this?  
#define MSG_BUFFER_SIZE 50

  void sendWidgetConfiguration(byte widgetType, byte widgetId, const char *labelText) {
    /*
     */
    // TODO: Do something stream based instead.
    byte msg[MSG_BUFFER_SIZE];
    byte offset = 0;
    
    byte lengthToCopy = 0;
    
    msg[offset++] = MESSAGE_CONFIGURE;
    msg[offset++] = widgetType;
    
    msg[offset++] = widgetId;
    
    lengthToCopy = MSG_BUFFER_SIZE - (offset + 1);
    
    if (strlen(labelText) < lengthToCopy) {
      lengthToCopy = strlen(labelText);
    }
    
    msg[offset++] = lengthToCopy;
    
    memcpy(msg+offset, labelText, lengthToCopy);
    
    offset += lengthToCopy;
    
    accessory.write(msg, offset);
  }  
  
  
public:
  HandbagApp(AndroidAccessory& accessory) : accessory(accessory) {
    /*
     */
     widgetCount = 0;
  }

  int begin() {
    /*
     */
    accessory.powerOn();
    
    while (!accessory.isConnected()) {
      // Wait for connection
      // TODO: Do time out?
    }

    // TODO: Do protocol version handshake.
    // TODO: Return result status.
        
  }

  // TODO: Make private
  // TODO: Set type-specific things like "label text" differently?
  int addWidget(WIDGET_TYPE widgetType, CALLBACK(callback), const char *labelText) {
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
    sendWidgetConfiguration(theWidget.type, theWidget.id, labelText);
    
    return theWidget.id;
  }
  
  
  void doIt() {
    for (int i = 0; i < widgetCount; i++) {
      widgets[i].callback();
    }
  }
};


HandbagApp Handbag(acc);


void callMe() {
  /*
   */
  Serial.println("Callback called."); 
}

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  
  Serial.println("Starting...");

  Handbag.begin();

  Serial.println("Started.");
  
  Serial.print("Widget id: ");
  Serial.println(Handbag.addWidget(UI_WIDGET_TYPE_LABEL, callMe, "Hello"));
  
  Serial.print("Widget id: ");
  Serial.println(Handbag.addWidget(UI_WIDGET_TYPE_LABEL, callMe, "There"));

  Handbag.doIt();
  
  Serial.println("Finished.");  
}

void loop() {
  // put your main code here, to run repeatedly: 
  
}


