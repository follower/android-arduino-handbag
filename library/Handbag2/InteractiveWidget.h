#ifndef _INTERACTIVE_WIDGET_H_
#define _INTERACTIVE_WIDGET_H_

#include "CallbackTypes.h"

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
  // TODO: Store widget type also?

  CallbackType callbackType;

  union {
    BASIC_CALLBACK(basic_callback);
    TEXT_CALLBACK(text_callback);
  };


public:

  unsigned int id;

  InteractiveWidget();

  void reset();

  bool isTerminalMarker();

  void callback();

  void callback(const char *text);

  void setCallback(BASIC_CALLBACK(callback));

  void setCallback(TEXT_CALLBACK(callback));

};

#endif
