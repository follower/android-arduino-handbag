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

class HandbagApp {

private:
  AndroidAccessory& accessory;
  
  CALLBACK(callbacks[10]);
  
public:
  HandbagApp(AndroidAccessory& accessory) : accessory(accessory) {
  
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

  void add(CALLBACK(callback)) {
    /*
     */
    callbacks[0] = callback; 
  }
  
  
  void doIt() {
    callbacks[0]();
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
  
  Handbag.add(callMe);
  
  Handbag.doIt();
  
  Serial.println("Finished.");  
}

void loop() {
  // put your main code here, to run repeatedly: 
  
}


