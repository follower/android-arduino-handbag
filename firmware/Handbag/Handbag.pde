// Extracted from 'demokit' example from 'ADK_release_0512.zip'. 

#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define  PWM_OUT           3

#define  DIGITAL_OUT       4

#define  ANALOG_IN_SENSOR  A0

#define  DIGITAL_IN        5

AndroidAccessory acc("rancidbacon.com",
		     "Handbag",
		     "Handbag (Arduino Board)",
		     "0.1",
		     "http://rancidbacon.com",
		     "0000000000000001");


#define MESSAGE_CONFIGURE 0x10

#define UI_WIDGET_BUTTON 0x00
#define UI_WIDGET_LABEL 0x01

#define COMMAND_GOT_EVENT 0x80

#define EVENT_BUTTON_CLICK 0x01

void setup();
void loop();

void init_buttons() {
	pinMode(DIGITAL_IN, INPUT);

	// enable the internal pullups
	digitalWrite(DIGITAL_IN, HIGH);
}


void init_relays() {
	pinMode(DIGITAL_OUT, OUTPUT);
	digitalWrite(DIGITAL_OUT, LOW);
}


void init_leds() {
	digitalWrite(PWM_OUT, 1);

	pinMode(PWM_OUT, OUTPUT);
}

void doAction1() {
  digitalWrite(DIGITAL_OUT, !digitalRead(DIGITAL_OUT));
}

void doAction2() {
  digitalWrite(DIGITAL_OUT, LOW);
  digitalWrite(A5, LOW);  
}

void doAction3() {
  pinMode(A5, OUTPUT);
  digitalWrite(A5, !digitalRead(A5));  
}

void doAction4() {
  digitalWrite(DIGITAL_OUT, HIGH);
  digitalWrite(A5, HIGH);  
}

void doAction5() {
  pinMode(A5, OUTPUT);
  digitalWrite(A5, !digitalRead(A5));  
  digitalWrite(DIGITAL_OUT, !digitalRead(DIGITAL_OUT));
}

#define MSG_BUFFER_SIZE 50
void configureWidget(byte widgetType, byte widgetId, char *labelText) {
  /*
   */
  byte msg[MSG_BUFFER_SIZE];
  byte offset = 0;
  
  byte lengthToCopy = 0;
  
  msg[offset++] = MESSAGE_CONFIGURE;
  msg[offset++] = widgetType;
  
  msg[offset++] = widgetId;
  
  lengthToCopy = MSG_BUFFER_SIZE - (offset + 1);
  
  if (strlen(labelText) < lengthToCopy) {
    lengthToCopy = strlen(labelText);
  }
  
  msg[offset++] = lengthToCopy;
  
  memcpy(msg+offset, labelText, lengthToCopy);
  
  offset += lengthToCopy;
  
  acc.write(msg, offset);
}

#define ID_NONE 0
#define ID_B1 1
#define ID_B2 2
#define ID_B3 3
#define ID_B4 4
#define ID_B5 5

byte b1;
void setup() {
	Serial.begin(115200);
	Serial.print("\r\nStart");

	init_leds();
	init_relays();
	init_buttons();


	b1 = 0;

	acc.powerOn();

        while (!acc.isConnected()) {
          // Wait for connection
        }
        
        // Do UI configuration
	byte msg[4];
        configureWidget(UI_WIDGET_LABEL, ID_NONE, "");
        configureWidget(UI_WIDGET_LABEL, ID_NONE, "Example Handbag Android Accessory");
        configureWidget(UI_WIDGET_LABEL, ID_NONE, "");        
        configureWidget(UI_WIDGET_BUTTON, ID_B1, "Toggle Digital Pin 4");
        configureWidget(UI_WIDGET_BUTTON, ID_B2, "Turn D4 and A5 off");
        configureWidget(UI_WIDGET_BUTTON, ID_B3, "Toggle Analog Pin 5");
        configureWidget(UI_WIDGET_BUTTON, ID_B4, "Turn D4 and A5 on");        
        configureWidget(UI_WIDGET_BUTTON, ID_B5, "Toggle D4 and A5");                
        configureWidget(UI_WIDGET_LABEL, ID_NONE, "");
        configureWidget(UI_WIDGET_LABEL, ID_NONE, "rancidbacon.com");        
}

void loop() {
	static byte count = 0;
	byte msg[3];

	if (acc.isConnected()) {
		int len = acc.read(msg, sizeof(msg), 1);
		byte b;
		uint16_t val;

		if (len > 0) {
			// assumes only one command per packet
			if (msg[0] == 0x2) {
				if (msg[1] == 0x0)
					analogWrite(PWM_OUT, 255 - msg[2]);
			} else if (msg[0] == 0x3) {
				if (msg[1] == 0x0)
					digitalWrite(DIGITAL_OUT, msg[2] ? HIGH : LOW);
			} else if (msg[0] == COMMAND_GOT_EVENT) {
                          if (msg[1] == EVENT_BUTTON_CLICK) {
                            switch (msg[2]) {
                              case ID_B1:
                                doAction1();
                                break;
                              
                              case ID_B2:
                                doAction2();
                                break;
                                
                               case ID_B3:
                                 doAction3();
                                 break;
                                
                               case ID_B4:
                                 doAction4();
                                 break;
                                
                               case ID_B5:
                                 doAction5();
                                 break;
                            }
                          }
                        }
		}

		msg[0] = 0x1;

		b = digitalRead(DIGITAL_IN);
		if (b != b1) {
			msg[1] = 0;
			msg[2] = b ? 0 : 1;
			acc.write(msg, 3);
			b1 = b;
		}

		switch (count++ % 0x10) {
		case 0x4:
			val = analogRead(ANALOG_IN_SENSOR);
			msg[0] = 0x5;
			msg[1] = val >> 8;
			msg[2] = val & 0xff;
			acc.write(msg, 3);
			break;

		}
	} else {
		// reset outputs to default values on disconnect
		analogWrite(PWM_OUT, 255);
		digitalWrite(DIGITAL_OUT, LOW);
	}

	delay(10);
}

