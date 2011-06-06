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

class Widget {
  private:
    CALLBACK(callback);
    unsigned int id;
    unsigned int type;
    
    friend class HandbagApp;
};

#define MAX_WIDGETS 20

class HandbagApp {

private:
  AndroidAccessory& accessory;
  
  // TODO: Dynamically allocate this?
  Widget widgets[MAX_WIDGETS];
  
  unsigned int widgetCount;
  
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

  int add(CALLBACK(callback)) {
    /*
     */
    if (widgetCount == MAX_WIDGETS) {
      return -1;
    }
    widgets[widgetCount].callback = callback; 
    return widgetCount++;
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
  Serial.println(Handbag.add(callMe));
  
  Serial.print("Widget id: ");
  Serial.println(Handbag.add(callMe));

  Handbag.doIt();
  
  Serial.println("Finished.");  
}

void loop() {
  // put your main code here, to run repeatedly: 
  
}


