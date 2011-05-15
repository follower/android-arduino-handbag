#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define  LED1_RED       8

#define  RELAY1         A0

#define  LIGHT_SENSOR   A2

#define  BUTTON1        A6

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
}


void init_leds()
{
	digitalWrite(LED1_RED, 1);

	pinMode(LED1_RED, OUTPUT);
}

byte b1, c;
void setup()
{
	Serial.begin(115200);
	Serial.print("\r\nStart");

	init_leds();
	init_relays();
	init_buttons();


	b1 = digitalRead(BUTTON1);
	c = 0;

	acc.powerOn();
}

void loop()
{
	byte err;
	byte idle;
	static byte count = 0;
	byte msg[3];
	long touchcount;

	if (acc.isConnected()) {
		int len = acc.read(msg, sizeof(msg), 1);
		int i;
		byte b;
		uint16_t val;
		int x, y;
		char c0;

		if (len > 0) {
			// assumes only one command per packet
			if (msg[0] == 0x2) {
				if (msg[1] == 0x0)
					analogWrite(LED1_RED, 255 - msg[2]);
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
		analogWrite(LED1_RED, 255);
		digitalWrite(RELAY1, LOW);
	}

	delay(10);
}

