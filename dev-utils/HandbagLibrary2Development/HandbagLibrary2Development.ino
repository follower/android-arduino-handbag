#include <string.h>
#include <Stream.h>

// Basic callback is a no-argument, no return value callback.
// TODO: Rename?
#define BASIC_CALLBACK(varname) void (*varname)()

// Text callback is a single character string argument, no return value callback.
#define TEXT_CALLBACK(varname) void (*varname)(const char *)


#define SCRATCH_BUFFER_SIZE 32 // TODO: Change size?

// TODO: Increase this?
// NOTE: This includes 1 location for a terminal marker (with Id 'TERMINAL_WIDGET_ID' i.e. 0).
#define MAX_INTERACTIVE_WIDGETS 11 // "Interactive" means associated with a callback

// TODO: Call this sentinel instead?
#define TERMINAL_WIDGET_ID 0

class InteractiveWidget {
  /*

     Note: The intention is that it should be safe for you call
           a `callback()` method even when there is no callback
           set--it should just silently do nothing. This is so
           you don't need to worry if you've encountered the
           "terminal" marker or not.

   */

private:
  enum CallbackType {
    NONE,
    BASIC,
    TEXT
  };

  // TODO: Store widget type also?

  CallbackType callbackType;

  union {
    BASIC_CALLBACK(basic_callback);
    TEXT_CALLBACK(text_callback);
  };


public:

  unsigned int id;

  InteractiveWidget() {
    reset();
  }

  void reset() {
    /*

       Make this widget a "terminal" marker. (Also inactive/inert).

     */
    id = TERMINAL_WIDGET_ID;
    callbackType = NONE;

    // Note: We don't bother clearing the callback pointer.
  }

  boolean isTerminalMarker() {
    return id == TERMINAL_WIDGET_ID;
  }

  void callback() {
    if (callbackType == BASIC) {
      basic_callback();
    }
  }

  void callback(const char *text) {
    if (callbackType == TEXT) {
      text_callback(text);
    }
  }

  void setCallback(BASIC_CALLBACK(callback)) {
    basic_callback = callback;
    callbackType = BASIC;
  }

  void setCallback(TEXT_CALLBACK(callback)) {
    text_callback = callback;
    callbackType = TEXT;
  }

};


class HandbagProtocolMixIn {

private:
  unsigned int lastWidgetId;

  InteractiveWidget widgets[MAX_INTERACTIVE_WIDGETS]; // TODO: Change name?

  // TODO: Support other call back types.
  void storeWidgetInfo(unsigned int widgetId, BASIC_CALLBACK(callback)) {

    unsigned int offset = getEmptyWidgetSlotOffset(widgetId);

    if (!widgets[offset].isTerminalMarker()) {
      widgets[offset].setCallback(callback);
    }
  }

  // TODO: Refactor to remove duplication with above.
  void storeWidgetInfo(unsigned int widgetId, TEXT_CALLBACK(callback)) {

    unsigned int offset = getEmptyWidgetSlotOffset(widgetId);

    if (!widgets[offset].isTerminalMarker()) {
      widgets[offset].setCallback(callback);
    }
  }

  unsigned int getEmptyWidgetSlotOffset(unsigned int widgetId) {

    // TODO: Just store this value instead?
    unsigned int offset = findOffsetOfWidget(TERMINAL_WIDGET_ID);

    if ((offset + 1) < MAX_INTERACTIVE_WIDGETS) {
      widgets[offset + 1].reset();

      widgets[offset].id = widgetId; // Used to indicate a slot was found.
    }

    return offset;
  }

  unsigned int findOffsetOfWidget(unsigned int widgetId) {
    unsigned int offset = 0;

    while ((offset < MAX_INTERACTIVE_WIDGETS) // Note: if this is ever untrue it's a bailout error.
           && !widgets[offset].isTerminalMarker()
           && (widgets[offset].id != widgetId)) {
      offset++;
    }

    // TODO: Handle "not found" better (and in callers)?

    return offset;
  }

  InteractiveWidget getWidgetInfo(unsigned int widgetId) {
    /*
     */

    return widgets[findOffsetOfWidget(widgetId)];
  }


  int readChar() {
    /*

        Provides a blocking read.

        Returns: >= 0 - character
                 < 0 - an error
     */

    // TODO: Include time-out?

    int theChar = -1;

    while ((theChar = strm->read()) == -1) {
        delay(10);
     }

    return theChar;
  }


  int bufferOffset;

  boolean resetBuffer() {
    // TODO: Handle this differently (for other buffers especially)?
    bufferOffset = 0;
    scratchBuffer[bufferOffset] = 0;

    return true;
  }


  boolean storeCharInBuffer(const char theChar) {

    boolean wasStoredOk = false;

    if ((bufferOffset + 2) <= SCRATCH_BUFFER_SIZE) { // TODO: Verify this.
      scratchBuffer[bufferOffset++] = theChar;
      scratchBuffer[bufferOffset] = 0;

      wasStoredOk = true;
    } else {
      // We drop the characters to avoid overflowing the buffer.
    }

    return wasStoredOk;
  }


protected:
  Stream *strm; // TODO: Make a reference to avoid needing "->" use? // TODO: Ensure strm isn't NULL.

  BASIC_CALLBACK(setupUICallback);


  void reset() { // TODO: Rename?
    lastWidgetId = 0;

    widgets[0].reset(); // Mark as "last" widget

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

  int getFieldContent() {

    resetBuffer();

    boolean isFirstChar = true;

    packetComplete = false; // TODO: Check previous "complete" was handled?


    // TODO: Include time-out?

    // TODO: Handle overflow by truncation or byte access or multiple calls?

    while (true) {
      int newChar = readChar();

      if (newChar < 0) {
        // TODO: Handle errors better.
        break;
      }

      if (isFirstChar && (newChar == '[')) {
        // Handle length-prefixed field
        unsigned int numCharsToRead = 0;

        int theChar;

        // TODO: Handle mal-formed, too large & other failure states better?
        // Get the length value
        while (true) {

          theChar = readChar();

          if (theChar < 0) {
            // TODO: Handle errors better.
            break;
          }

          if (theChar == ']') {
            break;
          }

          // TODO: Bail if not digits?
          numCharsToRead = (10 * numCharsToRead) + (theChar - '0');
        }

        for (unsigned int count = 0; count < numCharsToRead; count++) {
          theChar = readChar();

          if (theChar < 0) {
            // TODO: Handle errors better.
            break;
          }

          storeCharInBuffer((char) theChar); // TODO: Flag overflow?
        }

        // TODO: Bail if not end of field or end of packet?
        newChar = readChar();

        if (newChar < 0) {
          // TODO: Handle errors better.
          break;
        }

        // Drop through to normal handling

      }

      if ((newChar == ';') || (newChar == '\n')) { // end of field and/or packet

        // TODO: Put NUL terminator here?

        if (newChar == '\n') {
          packetComplete = true;
        }
        break;
      }

      storeCharInBuffer((char) newChar); // TODO: Flag overlow?

      isFirstChar = false; // TODO: Put this somewhere else?
    }

    return bufferOffset; // TODO: Ensure correct.
  }


  void processPacket() {

    if (strm->available() > 0) {

      packetComplete = false; // TODO: Initialise this elsewhere?

      getFieldContent();

      // TODO: Handle all this properly...
      if (strcmp(scratchBuffer, "widget") == 0) {
        // TODO: Handle packetComplete
        getFieldContent();
        if (strcmp(scratchBuffer, "event") == 0) {
          getFieldContent();
          unsigned int widgetId = atoi(scratchBuffer); // TODO: Check if this works with unsigned ok.

          getFieldContent();

          // TODO: Refactor all this...
          if (strcmp(scratchBuffer, "click") == 0) {

            Serial.print("Click on: ");
            Serial.println(widgetId);

            getWidgetInfo(widgetId).callback();

          } else if (strcmp(scratchBuffer, "input") == 0) {

            Serial.print("Input from: ");
            Serial.println(widgetId);

            getFieldContent();

            getWidgetInfo(widgetId).callback(scratchBuffer);
          }

        }
      }

      // Skip remainder of packet...
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


  unsigned int addButton(const char *labelText, BASIC_CALLBACK(callback)) {

    // TODO: Refactor all this...

    unsigned int widgetId = ++lastWidgetId;

    storeWidgetInfo(widgetId, callback);

    sendField("widget");

    sendField("button");

    sendField(widgetId);

    sendField(0);

    sendField(0);

    sendField(labelText, true);

    return widgetId;
  }


  unsigned int addLabel(const char *labelText, byte fontSize = 0, byte alignment = 0) {

    unsigned int widgetId = ++lastWidgetId;

    // Note: Takes advantage that widgets are auto-created if the Id is new.
    setText(widgetId, labelText, fontSize, alignment);

    return widgetId;
  }


  // TODO: Allow a different/larger buffer to be supplied.
  unsigned int addTextInput(TEXT_CALLBACK(callback)) {
    /*
       Note: The string supplied to the callback must be copied if it
             is used after the callback exits.
     */

    unsigned int widgetId = ++lastWidgetId;

    storeWidgetInfo(widgetId, callback);

    sendField("widget");

    sendField("textinput");

    sendField(widgetId, true);

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

  void sendSms(const char *recipient, const char *messageText) {
    /*

      Note: `recipient` can be either a phone number or a contact name.

     */

    sendField("feature");
    sendField("sms");
    sendField("send");
    sendField(recipient);
    sendField(messageText, true);
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
