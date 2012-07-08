#include <SPI.h>
#include <Ethernet.h>

#include "NetworkHandbag.h"



byte mac[] = {0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
IPAddress ip(169, 254, 254, 169);

EthernetServer server(0xba9);

HandbagApp Handbag(server);


unsigned int analogWidgetId;

unsigned int progressWidgetId;

unsigned int changeLabelWidgetId;


boolean saidSomething = false;

const int ledPin = A5;


void callbackDialog() {
  /*
   */
  Handbag.showDialog("You pushed it!");
}


void callbackToggle() {
  /*
   */
  digitalWrite(ledPin, !digitalRead(ledPin));
}


void callbackText(const char *text) {
  Handbag.setText(changeLabelWidgetId, text);
}

#define SMS_RECIPIENT_BUFFER_SIZE 16
#define SMS_MSG_BUFFER_SIZE 32

char smsRecipient[SMS_RECIPIENT_BUFFER_SIZE];

char smsMessage[SMS_MSG_BUFFER_SIZE];


void callbackCopyRecipientText(const char *text) {
  strncpy(smsRecipient, text, SMS_RECIPIENT_BUFFER_SIZE); // TODO: Should really be smallest of the two.
  smsRecipient[SMS_RECIPIENT_BUFFER_SIZE-1] = 0;
}


void callbackSmsMessageText(const char *text) {
  strncpy(smsMessage, text, SMS_MSG_BUFFER_SIZE); // TODO: Should really be smallest of the two.
  smsMessage[SMS_MSG_BUFFER_SIZE-1] = 0;
}


void callbackSMS() {
  /*
   */
  // TODO: Should really check values are vaguely sane somewhere.
  Handbag.sendSms(smsRecipient, smsMessage);
  Handbag.showDialog("Message sent!");
}


void setupUI() {
  /*
   */
  Handbag.addLabel("Hello, again!");

  analogWidgetId = Handbag.addLabel("0", 50, 1);

  progressWidgetId = Handbag.addProgressBar();

  Handbag.addButton("Push Me", callbackDialog);

  Handbag.addButton("Toggle LED", callbackToggle);

  changeLabelWidgetId = Handbag.addLabel("Change Me", 0, 1);

  Handbag.addTextInput(callbackText);

  Handbag.addLabel("", 25);


  Handbag.addLabel("SMS number/name");

  Handbag.addTextInput(callbackCopyRecipientText);

  Handbag.addLabel("Message text");

  Handbag.addTextInput(callbackSmsMessageText);


  Handbag.addButton("Send SMS", callbackSMS);
}


void setup() {

  Serial.begin(9600);

  Ethernet.begin(mac, ip);

  Handbag.begin(setupUI);

  Serial.println("start");

  pinMode(ledPin, OUTPUT);
}


unsigned long nextUpdateDue = 0;


void loop() {

  Handbag.refresh();

  if (Handbag.isConnected()) {

    if (millis() > nextUpdateDue) {

      unsigned int value = analogRead(A0);

      // Hacky itoa for analog range:
      char result[5];
      byte offset = 0;
      if (value > 1000) {
        result[offset++] = '1';
      }
      if (value > 100) {
        result[offset++] = ((value / 100) % 10) + '0';
      }
      if (value > 10) {
        result[offset++] = ((value / 10) % 10) + '0';
      }
      result[offset++] = (value % 10) + '0';
      result[offset++] = '\0';

      Handbag.setText(analogWidgetId, result);
      Handbag.setProgressBar(progressWidgetId, ((value*100UL)/1023));

      nextUpdateDue = millis() + 100;
    }

    if (!saidSomething) {
      Handbag.speakText("Hello from Ardweeno!");
      saidSomething = true;
    }

  } else {
    saidSomething = false;
    nextUpdateDue = 0;
  }

}
