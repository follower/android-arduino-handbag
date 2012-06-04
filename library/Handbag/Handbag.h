#ifndef __HANDBAG_H__
#define __HANDBAG_H__

#include <AndroidAccessory.h>

#define CALLBACK(varname) void (*varname)()

#define CALLBACK2(varname) void (*varname)(char *)


#define UI_WIDGET_TYPE_BUTTON 0x00
#define UI_WIDGET_TYPE_LABEL 0x01
#define UI_WIDGET_TYPE_TEXT_INPUT 0x02
#define UI_WIDGET_TYPE_DIALOG 0x03

#define COMMAND_GOT_EVENT 0x80

#define EVENT_BUTTON_CLICK 0x01
#define EVENT_TEXT_INPUT 0x02

#define WIDGET_TYPE unsigned int

class Widget {
  private:
    CALLBACK(callback);
    CALLBACK2(callback2);
    unsigned int id;
    unsigned int type;
    
    friend class HandbagApp;
};

#define MAX_WIDGETS 20

#define MESSAGE_CONFIGURE 0x10

class HandbagApp {

private:
  AndroidAccessory& accessory;
  
  // TODO: Dynamically allocate this?
  Widget widgets[MAX_WIDGETS];
  
  unsigned int widgetCount;
  
  CALLBACK(setupUICallback);

  boolean uiIsSetup;  

// TODO: Dynamically allocate this?  
#define MSG_BUFFER_SIZE 50

  void sendWidgetConfiguration(byte widgetType, byte widgetId, byte fontSize, byte widgetAlignment, const char *labelText);
  
  void setupUI();

  int addWidget(WIDGET_TYPE widgetType, CALLBACK(callback), byte widgetId, byte fontSize, const char *labelText, CALLBACK2(callback2) = NULL);

  void triggerButtonCallback(int widgetId);

  void triggerTextInputCallback(int widgetId, char *theString);

  
public:
  HandbagApp(AndroidAccessory& accessory);

  int begin(CALLBACK(theSetupUICallback));
  
  int addLabel(const char *labelText, byte fontSize = 0, byte alignment = 0);
  
  int addButton(const char *labelText, CALLBACK(callback));

  int addTextInput(CALLBACK2(callback2));

  void setText(int widgetId, const char *labelText, byte fontSize = 0, byte alignment = 0);

  void showDialog(const char *messageText);

  void refresh();

  boolean isConnected();
};

#endif

