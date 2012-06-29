/*

   LcdHandbag -- A simple demo for the Handbag App for Android.
   
   This sketch demonstrates a simple Arduino-based accessory for Android.
   
   See the 'SimpleHandbag' demo for general requirements.
   
   Requires:
   
     * LCD 16 x 2 display connected with pins A0 through A5.
   
   This demonstrates entering text on an Android device and sending
   it to the Arduino, then storing it and displaying it on an LCD.
   
   This approach shows how the Android can be used to supply 
   configuration information to a sketch.
   
   Also demonstrated is performing a time-based task on the
   Arduino whether or not an Android device is connected.
      
 */

#include <AndroidAccessory.h>

#define USE_SHIFT_LCD 0

#if USE_SHIFT_LCD
#include <ShiftLCD.h>
ShiftLCD lcd(6, A2, A3);
#else
#include <LiquidCrystal.h>

// initialize the library with the numbers of the interface pins
LiquidCrystal lcd(A0, A1, A2, A3, A4, A5);
#endif

#include "Handbag.h"


AndroidAccessory acc("rancidbacon.com",
		     "Handbag",
		     "Handbag (Arduino Board)",
		     "0.1",
		     "http://HandbagDevices.com/#i");

HandbagApp Handbag(acc);


#include <EEPROM.h>

char text[30];

// TODO: Tidy all this.
void fillFromStorage(char *theText, byte bufferSize) {
  if ((EEPROM.read(0) == 'H') && (EEPROM.read(1) == 'B')) {
    int i = 2; // Skip the first two bytes with the "magic number" signature.
    char c;
    while ((((c = EEPROM.read(i)) != '\0') && ((i-3) < bufferSize))) { // TODO: Check this.
      theText[i-2] = c;
      i++;
    }
    theText[i-2] = '\0';
  } else {
    text[0] = '\0';    
  }
}

// TODO: Tidy all this.
void putInStorage(char *theText) {
#define MAX_CHARS 20
  int i;
  for (i = 0; (i < strlen(theText)) && (i < MAX_CHARS); i++) {
    EEPROM.write(i+2, (byte) theText[i]);
  }
  
  EEPROM.write(i+2, '\0');

  // Store our "magic number" signature.
  EEPROM.write(0, 'H');
  EEPROM.write(1, 'B');  
}

int widgetId;

unsigned long nextScroll = 0;
int scrollCount = 0;
boolean scrollLeft = false;

boolean doScroll = true;


void buttonCallback() {
  doScroll = !doScroll;
}


void displayText(char *theString) {
  /*
   */   
  lcd.clear();
  lcd.print(theString);
  
  // TODO: Make this match so it's seamless?
  scrollCount = 0;
  scrollLeft = false;
}

void textInputCallback(char *theString) {
  
  putInStorage(theString);
  
  displayText(theString);
}


void setupUI() {
  /*
   */
  Handbag.addLabel("", 8);
  Handbag.addLabel("LCD Handbag example", 20, 0x01);
  Handbag.addLabel("", 20);
  widgetId = Handbag.addButton("Scrolling on/off", buttonCallback);
  Handbag.addLabel("", 20);
  Handbag.addLabel("Change LCD display text (press enter):", 12);  
  Handbag.addLabel("", 6);
  Handbag.addTextInput(textInputCallback);
}


void setup() {
  Serial.begin(9600);
  
  // set up the LCD's number of columns and rows: 
  lcd.begin(16, 2);  
  
  fillFromStorage(text, sizeof(text));
  
  if (strlen(text) == 0) {
    displayText("Please change me!!");
  } else {
    displayText(text);    
  }
  
  Handbag.begin(setupUI);
}


void loop() {
  Handbag.refresh();

  if (doScroll && (millis() > nextScroll)) {
    if (scrollLeft) {
      lcd.scrollDisplayLeft();
      scrollCount--;
    } else {
      lcd.scrollDisplayRight();
      scrollCount++;
    }
    
    if (scrollCount <= -5) {
      scrollCount = -5;
      scrollLeft = false;
    } else if (scrollCount >= 5) {
      scrollCount = 5;
      scrollLeft = true;
    }
    
    nextScroll = millis() + 400;
  }  
}

