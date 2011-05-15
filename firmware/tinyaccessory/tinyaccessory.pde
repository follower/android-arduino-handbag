#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define  LED1           3

#define  RELAY1         4

#define  LIGHT_SENSOR   A0

#define  BUTTON1        5

AndroidAccessory acc("example.com",
		     "TinyAccessory",
		     "TinyAccessory (Arduino Board)",
		     "1.0",
		     "http://www.example.com",
		     "0000000012345678");
void setup();
void loop();

void init_buttons()
{
	pinMode(BUTTON1, INPUT);

	// enable the internal pullups
	digitalWrite(BUTTON1, HIGH);
}


void init_relays()
{
	pinMode(RELAY1, OUTPUT);
	digitalWrite(RELAY1, LOW);
}


void init_leds()
{
	digitalWrite(LED1, 1);

	pinMode(LED1, OUTPUT);
}

byte b1;
void setup()
{
	Serial.begin(115200);
	Serial.print("\r\nStart");

	init_leds();
	init_relays();
	init_buttons();


	b1 = 0;

	acc.powerOn();
}

void loop()
{
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
					analogWrite(LED1, 255 - msg[2]);
			} else if (msg[0] == 0x3) {
				if (msg[1] == 0x0)
					digitalWrite(RELAY1, msg[2] ? HIGH : LOW);
			}
		}

		msg[0] = 0x1;

		b = digitalRead(BUTTON1);
		if (b != b1) {
			msg[1] = 0;
			msg[2] = b ? 0 : 1;
			acc.write(msg, 3);
			b1 = b;
		}

		switch (count++ % 0x10) {
		case 0x4:
			val = analogRead(LIGHT_SENSOR);
			msg[0] = 0x5;
			msg[1] = val >> 8;
			msg[2] = val & 0xff;
			acc.write(msg, 3);
			break;

		}
	} else {
		// reset outputs to default values on disconnect
		analogWrite(LED1, 255);
		digitalWrite(RELAY1, LOW);
	}

	delay(10);
}

