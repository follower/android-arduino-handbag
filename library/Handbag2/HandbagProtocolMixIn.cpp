#include <string.h>

#include <Stream.h>

#include "HandbagProtocolMixIn.h"

void HandbagProtocolMixIn::storeWidgetInfo(unsigned int widgetId, BASIC_CALLBACK(callback)) {

  unsigned int offset = getEmptyWidgetSlotOffset(widgetId);

  if (!widgets[offset].isTerminalMarker()) {
    widgets[offset].setCallback(callback);
  }
}


// TODO: Refactor more to remove duplication with above?
void HandbagProtocolMixIn::storeWidgetInfo(unsigned int widgetId, TEXT_CALLBACK(callback)) {

  unsigned int offset = getEmptyWidgetSlotOffset(widgetId);

  if (!widgets[offset].isTerminalMarker()) {
    widgets[offset].setCallback(callback);
  }
}


unsigned int HandbagProtocolMixIn::getEmptyWidgetSlotOffset(unsigned int widgetId) {

  // TODO: Just store this value instead?
  // TODO: Change default to be TERMINAL_WIDGET_ID so the definition doesn't need to be public?
  unsigned int offset = findOffsetOfWidget(TERMINAL_WIDGET_ID);

  if ((offset + 1) < MAX_INTERACTIVE_WIDGETS) {
    widgets[offset + 1].reset();

    widgets[offset].id = widgetId; // Used to indicate a slot was found.
  }

  return offset;
}


unsigned int HandbagProtocolMixIn::findOffsetOfWidget(unsigned int widgetId) {
  unsigned int offset = 0;

  while ((offset < MAX_INTERACTIVE_WIDGETS) // Note: if this is ever untrue it's a bailout error.
	 && !widgets[offset].isTerminalMarker()
	 && (widgets[offset].id != widgetId)) {
    offset++;
  }

  // TODO: Handle "not found" better (and in callers)?

  return offset;
}

InteractiveWidget HandbagProtocolMixIn::getWidgetInfo(unsigned int widgetId) {
  /*
   */

  return widgets[findOffsetOfWidget(widgetId)];
}


int HandbagProtocolMixIn::readChar() {
  /*

    Provides a blocking read.

    Returns: >= 0 - character
    < 0 - an error
  */

  // TODO: Include time-out?

  int theChar = -1;

  unsigned int timeoutAt = millis() + 1000;

  while ((theChar = strm->read()) == -1) {
    if (millis() > timeoutAt) {
      Serial.println("*readChar timeout*");
      break;
    }
    delay(10);
  }

  return theChar;
}


boolean HandbagProtocolMixIn::resetBuffer() {
  // TODO: Handle this differently (for other buffers especially)?
  bufferOffset = 0;
  scratchBuffer[bufferOffset] = 0;

  return true;
}


boolean HandbagProtocolMixIn::storeCharInBuffer(const char theChar) {

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


void HandbagProtocolMixIn::reset() {
  lastWidgetId = 0;

  widgets[0].reset(); // Mark as "last" widget

  // TODO: Add other items?
}


void HandbagProtocolMixIn::sendSeparator(boolean isFinalField) {
  /*

    Sends field separator or packet terminator if final field.

  */

  // TODO: Handle this better?
  if (isFinalField) {
    strm->write("\n");
  } else {
    strm->write(";");
  }

}


void HandbagProtocolMixIn::sendField(const char *fieldData, boolean isFinalField) {
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


void HandbagProtocolMixIn::sendField(const int data, boolean isFinalField) {
  strm->print(data);

  sendSeparator(isFinalField);
}


void HandbagProtocolMixIn::sendField(const unsigned int data, boolean isFinalField) {
  strm->print(data);

  sendSeparator(isFinalField);
}


void HandbagProtocolMixIn::sendField(const byte data, boolean isFinalField) {
  strm->print(data);

  sendSeparator(isFinalField);
}


int HandbagProtocolMixIn::getFieldContent() {

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


void HandbagProtocolMixIn::processPacket() {

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


void HandbagProtocolMixIn::setText(unsigned int widgetId, const char *labelText, byte fontSize, byte alignment) {

  sendField("widget");

  sendField("label");

  sendField(widgetId);

  sendField(fontSize);

  sendField(alignment);

  sendField(labelText, true);
}


unsigned int HandbagProtocolMixIn::addButton(const char *labelText, BASIC_CALLBACK(callback)) {

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


unsigned int HandbagProtocolMixIn::addLabel(const char *labelText, byte fontSize, byte alignment) {

  unsigned int widgetId = ++lastWidgetId;

  // Note: Takes advantage that widgets are auto-created if the Id is new.
  setText(widgetId, labelText, fontSize, alignment);

  return widgetId;
}


unsigned int HandbagProtocolMixIn::addTextInput(TEXT_CALLBACK(callback)) {
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


void HandbagProtocolMixIn::showDialog(const char *messageText) {

  sendField("widget");

  sendField("dialog");

  sendField(messageText, true);
}


void HandbagProtocolMixIn::setProgressBar(unsigned int widgetId, int value) {
  sendField("widget");

  sendField("progress");

  sendField(widgetId);

  sendField(value, true);
}


unsigned int HandbagProtocolMixIn::addProgressBar(int initialValue) {

  unsigned int widgetId = ++lastWidgetId;

  // Note: Takes advantage that widgets are auto-created if the Id is new.
  setProgressBar(widgetId, initialValue);

  return widgetId;
}


void HandbagProtocolMixIn::speakText(const char *textToSay /* TODO: Add pitch/rate support? */) {
  sendField("feature");
  sendField("speech");
  sendField("speak");
  sendField(textToSay);
  sendField("1.0");
  sendField("1.0", true);
}


void HandbagProtocolMixIn::sendSms(const char *recipient, const char *messageText) {
  /*

    Note: `recipient` can be either a phone number or a contact name.

  */

  sendField("feature");
  sendField("sms");
  sendField("send");
  sendField(recipient);
  sendField(messageText, true);
}
