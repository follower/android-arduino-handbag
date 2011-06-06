#ifndef __HANDBAG_H__
#define __HANDBAG_H__

#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define CALLBACK(varname) void (*varname)()


#define UI_WIDGET_TYPE_BUTTON 0x00
#define UI_WIDGET_TYPE_LABEL 0x01

#define COMMAND_GOT_EVENT 0x80

#define EVENT_BUTTON_CLICK 0x01

#define WIDGET_TYPE unsigned int

class Widget {
  private:
    CALLBACK(callback);
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

  void sendWidgetConfiguration(byte widgetType, byte widgetId, const char *labelText);
  
  void setupUI();

  int addWidget(WIDGET_TYPE widgetType, CALLBACK(callback), const char *labelText);

  void triggerButtonCallback(int widgetId);

  
public:
  HandbagApp(AndroidAccessory& accessory);

  int begin(CALLBACK(theSetupUICallback));
  
  int addLabel(const char *labelText);
  
  int addButton(const char *labelText, CALLBACK(callback));

  void refresh();

};

#endif

