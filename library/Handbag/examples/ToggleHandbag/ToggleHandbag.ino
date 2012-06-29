/*

   ToggleHandbag -- A simple demo for the Handbag App for Android.
   
   This sketch demonstrates a simple Arduino-based accessory for Android.
   
   See the 'SimpleHandbag' demo for general requirements.
   
   Additional requirements:
   
     * LEDs connected to digital pin 4 and analog pin 5.

   This example matches the original demo released for Handbag and toggles
   the lighting of two LEDs connected to the board.
      
 */

#include <AndroidAccessory.h>

#include "Handbag.h"


AndroidAccessory acc("rancidbacon.com",
		     "Handbag",
		     "Handbag (Arduino Board)",
		     "0.1",
		     "http://HandbagDevices.com/#i");

HandbagApp Handbag(acc);


const int firstLedPin = 4;
const int secondLedPin = A5;

void toggleFirstLed() {  
  digitalWrite(firstLedPin, !digitalRead(firstLedPin));
}


void bothLedsOff() {
  digitalWrite(firstLedPin, LOW);
  digitalWrite(secondLedPin, LOW);  
}


void toggleSecondLed() {
  digitalWrite(secondLedPin, !digitalRead(secondLedPin));  
}


void bothLedsOn() {
  digitalWrite(firstLedPin, HIGH);
  digitalWrite(secondLedPin, HIGH);  
}

void bothLedsToggle() {
  toggleFirstLed();
  toggleSecondLed();
}


void setupUI() {
  /*
   */

    Handbag.addLabel("");
    Handbag.addLabel("Example Handbag Android Accessory", 16, 0x01);
    Handbag.addLabel("");        
    Handbag.addButton("Toggle Digital Pin 4", toggleFirstLed);
    Handbag.addButton("Turn D4 and A5 off", bothLedsOff);
    Handbag.addButton("Toggle Analog Pin 5", toggleSecondLed);
    Handbag.addButton("Turn D4 and A5 on", bothLedsOn);        
    Handbag.addButton("Toggle D4 and A5", bothLedsToggle);                
    Handbag.addLabel("");
    Handbag.addLabel("rancidbacon.com", 32, 0x01);

}


void setup() {
  Serial.begin(9600);
  
  pinMode(firstLedPin, OUTPUT);
  pinMode(secondLedPin, OUTPUT);  
  
  Handbag.begin(setupUI);
}


void loop() {
  Handbag.refresh();  
}


