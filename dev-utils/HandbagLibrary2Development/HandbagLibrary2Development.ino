#include <string.h>
#include <Stream.h>

// Basic callback is a no-argument, no return value callback.
// TODO: Rename?
#define BASIC_CALLBACK(varname) void (*varname)()

#define SCRATCH_BUFFER_SIZE 32 // TODO: Change size?

// TODO: Increase this?
// NOTE: This includes 1 location for a terminal marker (with Id 'TERMINAL_WIDGET_ID' i.e. 0).
#define MAX_INTERACTIVE_WIDGETS 11 // "Interactive" means associated with a callback

#define TERMINAL_WIDGET_ID 0

class InteractiveWidget {
  // TODO: Store callback/widget type also?
  unsigned int widgetId; // TODO: Change name to just 'id'?
  union {
    BASIC_CALLBACK(basic_callback);
  };

  friend class HandbagProtocolMixIn;
};

class HandbagProtocolMixIn {

private:
  unsigned int lastWidgetId;

  InteractiveWidget widgets[MAX_INTERACTIVE_WIDGETS]; // TODO: Change name?

  // TODO: Support other call back types.
  void storeWidgetInfo(unsigned int widgetId, BASIC_CALLBACK(basic_callback)) {
    unsigned int offset = 0;

    // TODO: Handle all this better?
    // TODO: Just store this value instead?
    while (widgets[offset].widgetId != TERMINAL_WIDGET_ID) {
      offset++;
    }

    if ((offset + 1) < MAX_INTERACTIVE_WIDGETS) {
      widgets[offset + 1].widgetId = TERMINAL_WIDGET_ID;

      widgets[offset].widgetId = widgetId;
      widgets[offset].basic_callback = basic_callback;
    }
  }


  InteractiveWidget getWidgetInfo(unsigned int widgetId) {
    /*
     */
    unsigned int offset = 0;

    while ((widgets[offset].widgetId != TERMINAL_WIDGET_ID)
           && (widgets[offset].widgetId != widgetId)) {
      offset++;
    }

    // TODO: Handle "not found" better?

    return widgets[offset];
  }


protected:
  Stream *strm; // TODO: Make a reference to avoid needing "->" use? // TODO: Ensure strm isn't NULL.

  BASIC_CALLBACK(setupUICallback);


  void reset() { // TODO: Rename?
    lastWidgetId = 0;

    widgets[0].widgetId = TERMINAL_WIDGET_ID; // Mark as "last" widget

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


  // TODO: Indicate when truncated. Indicate packet complete.
  boolean packetComplete; // TODO: Combine this plus overflow etc in a field structure?

  // TODO: Provide way to supply an larger, separate buffer.

  // TODO: Handle length-prefixed fields
  int getFieldContent() {

    int bufferOffset = 0;

    packetComplete = false; // TODO: Check previous "complete" was handled?

    scratchBuffer[bufferOffset] = 0;

    // TODO: Include time-out?

    // TODO: Handle overflow by truncation or byte access or multiple calls?

    while (true) {
      int newChar = strm->read();

      if (newChar == -1) {
        // TODO: Handle differently?
        delay(10);
        continue;
      }

      if ((newChar == ';') || (newChar == '\n')) { // end of field and/or packet

        // TODO: Put NUL terminator here?

        if (newChar == '\n') {
          packetComplete = true;
        }
        break;
      }

      if ((bufferOffset + 2) <= SCRATCH_BUFFER_SIZE) { // TODO: Verify this.
        scratchBuffer[bufferOffset++] = (char) newChar;
        scratchBuffer[bufferOffset] = 0;
      } else {
        // We drop the characters because we're now overflowing the buffer.
        // TODO: Indicate overflow?
      }

    }

    return bufferOffset; // TODO: Ensure correct.
  }


  void processPacket() {

    if (strm->available() > 0) {

      packetComplete = false; // TODO: Initialise this elsewhere?

      while (!packetComplete) {
        getFieldContent();
        // TODO: Handle strings with embedded nulls?
        Serial.println(scratchBuffer);
      }

    }

  }

public:
  char scratchBuffer[SCRATCH_BUFFER_SIZE];

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
        processPacket();

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
