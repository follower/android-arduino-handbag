/*

   TextInputHandbag -- A simple demo for the Handbag App for Android.
   
   This sketch demonstrates a simple Arduino-based accessory for Android.
   
   See the 'SimpleHandbag' demo for general requirements.
   
   This demonstrates entering text on Android device and sending it to the
   Arduino.
      
 */

#include <AndroidAccessory.h>

#include "Handbag.h"


AndroidAccessory acc("rancidbacon.com",
		     "Handbag",
		     "Handbag (Arduino Board)",
		     "0.1",
		     "http://HandbagDevices.com/#i");

HandbagApp Handbag(acc);


void buttonCallback() {
  Handbag.showDialog("Button pressed!");
}

int widgetId;

void textInputCallback(char *theString) {
  Handbag.setText(widgetId, theString);
}


void setupUI() {
  /*
   */
  Handbag.addLabel("", 8);
  Handbag.addLabel("TextInput Handbag example", 20, 0x01);
  Handbag.addLabel("", 20);
  widgetId = Handbag.addButton("Press me", buttonCallback);
  Handbag.addLabel("", 20);
  Handbag.addLabel("Change button label (press enter):", 12);  
  Handbag.addLabel("", 6);
  Handbag.addTextInput(textInputCallback);
}


void setup() {
  Serial.begin(9600);
  
  Handbag.begin(setupUI);
}


void loop() {
  Handbag.refresh();  
}


