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
	byte msg[3];
	msg[0] = MESSAGE_CONFIGURE;
	msg[1] = UI_WIDGET_LABEL;
	msg[2] = 0;
	acc.write(msg, 3);

	msg[1] = UI_WIDGET_BUTTON;
	acc.write(msg, 3);

	acc.write(msg, 3);
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

