#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#include "Handbag.h"


AndroidAccessory acc("rancidbacon.com",
		     "Handbag",
		     "Handbag (Arduino Board)",
		     "0.1",
		     "http://rancidbacon.com",
		     "0000000000000001");

  

HandbagApp Handbag(acc);


void callMe() {
  /*
   */
  Serial.println("Callback called."); 
}

void setupUI() {
  /*
   */
  Handbag.addLabel("Hello");
  Handbag.addButton("There", callMe);
}

void setup() {
  Serial.begin(9600);
  
  Handbag.begin(setupUI);

}

void loop() {
  Handbag.refresh();  
}


