#include <string.h>
#include <Print.h>

#define BASIC_CALLBACK(varname) void (*varname)()

class HandbagProtocolMixIn {

private:
  unsigned int lastWidgetId;

protected:
  Print *strm; // TODO: Make a reference to avoid needing "->" use? // TODO: Ensure strm isn't NULL.

  BASIC_CALLBACK(setupUICallback);


  void reset() { // TODO: Rename?
    lastWidgetId = 0;

    // TODO: Add other items?
  }


  void sendSeparator(boolean isFinalField = false) {
    /*

       Sends field separator or packet terminator if final field.

     */

    // TODO: Handle this better?
    if (isFinalField) {
      strm->write("\n");
      delay(100); // TODO: Move elsewhere?
    } else {
      strm->write(";");
    }

  }


  // TODO: Make these methods private?
  // TODO: Subclass Print somehow to avoid the repetition?
  void sendField(const char *fieldData, boolean isFinalField = false) {
    if ((fieldData == NULL)) {
      return;
    }

    if ((fieldData[0] == '[') || (strpbrk(fieldData, ";\n") != NULL)) {
      strm->write("[");
      strm->print(strlen(fieldData));
      strm->write("]");
    }

    strm->write(fieldData);

    sendSeparator(isFinalField);
  }


  void sendField(const int data, boolean isFinalField = false) {
    strm->print(data);

    sendSeparator(isFinalField);
  }


  void sendField(const unsigned int data, boolean isFinalField = false) {
    strm->print(data);

    sendSeparator(isFinalField);
  }


  void sendField(const byte data, boolean isFinalField = false) {
    strm->print(data);

    sendSeparator(isFinalField);
  }


public:
  void setText(unsigned int widgetId, const char *labelText, byte fontSize = 0, byte alignment = 0) {

    sendField("widget");

    sendField("label");

    sendField(widgetId);

    sendField(fontSize);

    sendField(alignment);

    sendField(labelText, true);
  }


  unsigned int addLabel(const char *labelText, byte fontSize = 0, byte alignment = 0) {

    unsigned int widgetId = ++lastWidgetId;

    // Note: Takes advantage that widgets are auto-created if the Id is new.
    setText(widgetId, labelText, fontSize, alignment);

    return widgetId;
  }


  void showDialog(const char *messageText) {

    sendField("widget");

    sendField("dialog");

    sendField(messageText, true);
  }


  void setProgressBar(unsigned int widgetId, int value) {
    sendField("widget");

    sendField("progress");

    sendField(widgetId);

    sendField(value, true);
  }


  unsigned int addProgressBar(int initialValue = 0) {

    unsigned int widgetId = ++lastWidgetId;

    // Note: Takes advantage that widgets are auto-created if the Id is new.
    setProgressBar(widgetId, initialValue);

    return widgetId;
  }


  void speakText(const char *textToSay /* TODO: Add pitch/rate support? */) {
    sendField("feature");
    sendField("speech");
    sendField("speak");
    sendField(textToSay);
    sendField("1.0");
    sendField("1.0", true);
  }

};


#include <SPI.h>
#include <Ethernet.h>


class Handbag2 : public HandbagProtocolMixIn {

private:
  EthernetServer& server;

  EthernetClient client;

  boolean uiIsSetup;

public:
  Handbag2(EthernetServer& server) : server(server) {
    /*
     */
    // Ideally we'd use 'Server' instead but it doesn't have a 'available()' method
    // we can use.
    // Maybe we need to accept a 'Client' at a different stage...
    // TODO: Handle all this better.

    uiIsSetup = false;
    client = EthernetClient();
  }

  int begin(BASIC_CALLBACK(setupUICallback)) {
    this->setupUICallback = setupUICallback;

    server.begin();

    return 1;
  }


  void refresh() {
    /*
     */

    if (!client) {
      client = server.available();
    }

    if (client) {
      if (!uiIsSetup) {
        // TODO: Initialise here

        // TODO: Reset widget ids etc.
        reset();

        strm = &client;

        if (setupUICallback != NULL) {
          // NOTE: This *must* only be called after 'strm' is valid.
          setupUICallback();
        }

        uiIsSetup = true;
      }

      if (client.connected()) {

        // TODO: Process unread bytes
        // TODO: Do this properly
        while (client.available() > 0) {
          client.read();
        }

      } else {
        client.stop(); // TODO: Unnecessary and/or enough?
        uiIsSetup = false;
      }
    }
  }


  boolean isConnected() {
    /*
     */
    // Note: This kind of makes refresh() redundant and I'm
    //       not convinced this is the best approach.
    // TODO: Make better?
    // TODO: Make sure all communication routines check for connection first?
    refresh();
    return uiIsSetup;
  }

};


byte mac[] = {0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
IPAddress ip(169, 254, 254, 169);

EthernetServer server(0xba9);

Handbag2 Handbag(server);


unsigned int analogWidgetId;

unsigned int progressWidgetId;

boolean saidSomething = false;

void setupUI() {
  /*
   */
  Handbag.addLabel("Hello, again!");

  analogWidgetId = Handbag.addLabel("0", 50, 1);

  progressWidgetId = Handbag.addProgressBar();
}


void setup() {

  Serial.begin(9600);

  Ethernet.begin(mac, ip);

  Handbag.begin(setupUI);

  Serial.println("start");
}


void loop() {

#if 1
  Handbag.refresh();

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

  if (Handbag.isConnected()) {
    Handbag.setText(analogWidgetId, result);
    Handbag.setProgressBar(progressWidgetId, ((value*100UL)/1023));

    if (!saidSomething) {
      Handbag.speakText("Hello from Ardweeno!");
      saidSomething = true;
    }

  } else {
    saidSomething = false;
  }

  delay(100);

#else
  EthernetClient client = server.available();

  if (client) {

    Serial.println("connected.");

    addLabel(client, "A Label!", 35, 1);

    addLabel(client, "Further words", 35, 1);

    showDialog(client, "Hello Dialog!");

    addLabel(client, "[\nhello there;\n]", 35, 1);

    addLabel(client, "MORE", 100, 1);

    unsigned int analogWidgetId = addLabel(client, "0", 35, 1);

    Serial.println("sent");

    while (client.connected()) {

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

      setText(client, analogWidgetId, result);


      delay(1000);

      if (client.available() > 0) {
        client.read();
      }
      delay(10);
    }

    Serial.println("not connected");

    client.stop();

    Serial.println("stop.");

  }
#endif
}
