/*

   MultimeterHandbag -- A simple demo for the Handbag App for Android.
   
   This sketch demonstrates a simple Arduino-based accessory for Android.
   
   See the 'SimpleHandbag' demo for general requirements.
   
   Additional requirements:
   
     * (optional) LEDs connected to digital pin 4 & analog pin 5.

   This example demonstrates the use of the `setText()` method to update
   the value of a label. 
   
   It is a "multimeter" that only measures voltages from 0 to 5.0V DC.
   
   It also has an incrementing counter.
      
 */

#include <AndroidAccessory.h>

#include "Handbag.h"


AndroidAccessory acc("rancidbacon.com",
		     "Handbag",
		     "Handbag (Arduino Board)",
		     "0.1",
		     "http://HandbagDevices.com/#i");

HandbagApp Handbag(acc);

boolean displayCounter = false;
boolean runCounter = true;

unsigned int counter = 0;

unsigned int displayWidgetId;
unsigned int toggleWidgetId; 

void toggleCounterAdcDisplay() {
  /*
   */
  displayCounter = !displayCounter;
  
  // Change the label on the toggle button
  if (displayCounter) {
    Handbag.setText(toggleWidgetId, "Display multimeter");
  } else {
    Handbag.setText(toggleWidgetId, "Display counter");
  }
}

void toggleCounterStartOrStop() {
  /*
   */
   runCounter = !runCounter;
}

void resetCounter() {
  /*
   */
   counter=0;
}

const int firstLedPin = 4;
const int secondLedPin = A5;

void toggleFirstLed() {  
  digitalWrite(firstLedPin, !digitalRead(firstLedPin));
}

void toggleSecondLed() {
  digitalWrite(secondLedPin, !digitalRead(secondLedPin));  
}

void bothLedsToggle() {
  toggleFirstLed();
  toggleSecondLed();
}


void setupUI() {
  /*
   */

    Handbag.addLabel("", 8);
    Handbag.addLabel("Handbag \"Multimeter\"", 24, 0x01);
    displayWidgetId = Handbag.addLabel("0.00", 128, 0x01);
    toggleWidgetId = Handbag.addButton("-", toggleCounterAdcDisplay);
    Handbag.addButton("Toggle counter start/stop", toggleCounterStartOrStop);
    Handbag.addButton("Reset counter", resetCounter);    
    Handbag.addButton("Toggle D4 and A5", bothLedsToggle);                
    Handbag.addLabel("", 8);
    Handbag.addLabel("rancidbacon.com", 32, 0x01);

    // To get the label displayed
    // TODO: Preserve the previous state?
    displayCounter = false;
    toggleCounterAdcDisplay();
}


void setup() {
  Serial.begin(9600);
  
  pinMode(firstLedPin, OUTPUT);
  pinMode(secondLedPin, OUTPUT);  
  
  pinMode(A0, INPUT);
  
  Handbag.begin(setupUI);

}


void loop() {
  Handbag.refresh();

  if (runCounter) {
    if (counter == 999) {
      counter = 0;
    } else {
      counter++;
    }
  }
  
  unsigned long value;
  
  if (displayCounter) {
    value = counter;
  } else {
    value = ((analogRead(A0) * 500UL)/1023);    
  }
  
  char result[5];
  
  result[0] = (value / 100) + '0';
  result[1] = '.';
  result[2] = ((value / 10) % 10) + '0';
  result[3] = (value % 10) + '0';
  result[4] = '\0';

  if (Handbag.isConnected()) {
    Handbag.setText(displayWidgetId, result);
  }
  
  delay(100);
}


