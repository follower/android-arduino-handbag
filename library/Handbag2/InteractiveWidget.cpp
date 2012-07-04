#include "InteractiveWidget.h"

InteractiveWidget::InteractiveWidget() {
  reset();
}

void InteractiveWidget::reset() {
  /*

    Make this widget a "terminal" marker. (Also inactive/inert).

  */
  id = TERMINAL_WIDGET_ID;
  callbackType = NONE;

  // Note: We don't bother clearing the callback pointer.
}

bool InteractiveWidget::isTerminalMarker() {
  return id == TERMINAL_WIDGET_ID;
}

void InteractiveWidget::callback() {
  if (callbackType == BASIC) {
    basic_callback();
  }
}

void InteractiveWidget::callback(const char *text) {
  if (callbackType == TEXT) {
    text_callback(text);
  }
}

void InteractiveWidget::setCallback(BASIC_CALLBACK(callback)) {
  basic_callback = callback;
  callbackType = BASIC;
}

void InteractiveWidget::setCallback(TEXT_CALLBACK(callback)) {
  text_callback = callback;
  callbackType = TEXT;
}
