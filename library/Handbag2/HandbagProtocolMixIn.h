#ifndef __HANDBAG_PROTOCOL_MIX_IN_H__
#define __HANDBAG_PROTOCOL_MIX_IN_H__

#include <Arduino.h>

#define SCRATCH_BUFFER_SIZE 32 // TODO: Change size?

// TODO: Increase this?
// NOTE: This includes 1 location for a terminal marker (with Id 'TERMINAL_WIDGET_ID' i.e. 0).
#define MAX_INTERACTIVE_WIDGETS 11 // "Interactive" means associated with a callback

#include "InteractiveWidget.h"

class HandbagProtocolMixIn {

private:
  unsigned int lastWidgetId;

  InteractiveWidget widgets[MAX_INTERACTIVE_WIDGETS]; // TODO: Change name?

  void storeWidgetInfo(unsigned int widgetId, BASIC_CALLBACK(callback));
  void storeWidgetInfo(unsigned int widgetId, TEXT_CALLBACK(callback));

  unsigned int getEmptyWidgetSlotOffset(unsigned int widgetId);
  unsigned int findOffsetOfWidget(unsigned int widgetId);

  InteractiveWidget getWidgetInfo(unsigned int widgetId);

  int readChar();

  int bufferOffset;

  boolean resetBuffer();
  boolean storeCharInBuffer(const char theChar);


protected:
  Stream *strm; // TODO: Make a reference to avoid needing "->" use? // TODO: Ensure strm isn't NULL.

  BASIC_CALLBACK(setupUICallback);

  void reset(); // TODO: Rename?

  void sendSeparator(boolean isFinalField = false);

  // TODO: Make these methods private?
  // TODO: Subclass Print somehow to avoid the repetition?
  void sendField(const char *fieldData, boolean isFinalField = false);
  void sendField(const int data, boolean isFinalField = false);
  void sendField(const unsigned int data, boolean isFinalField = false);
  void sendField(const byte data, boolean isFinalField = false);

  // TODO: Indicate when truncated. Indicate packet complete.
  boolean packetComplete; // TODO: Combine this plus overflow etc in a field structure?

  int getFieldContent(); // TODO: Provide way to supply an larger, separate buffer.
  void processPacket();

public:
  char scratchBuffer[SCRATCH_BUFFER_SIZE];

  void setText(unsigned int widgetId, const char *labelText, byte fontSize = 0, byte alignment = 0);

  unsigned int addButton(const char *labelText, BASIC_CALLBACK(callback));
  unsigned int addLabel(const char *labelText, byte fontSize = 0, byte alignment = 0);
  unsigned int addTextInput(TEXT_CALLBACK(callback)); //TODO: Allow a different/larger buffer to be supplied.

  void showDialog(const char *messageText);

  void setProgressBar(unsigned int widgetId, int value);
  unsigned int addProgressBar(int initialValue = 0);

  void speakText(const char *textToSay /* TODO: Add pitch/rate support? */);

  void sendSms(const char *recipient, const char *messageText);

};



#endif
