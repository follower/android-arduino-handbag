#include <string.h>
#include <Print.h>

void sendField(Print& strm, const char *fieldData, boolean isFinalField = false) {
  if ((fieldData == NULL)) {
    return;
  }

  if ((fieldData[0] == '[') || (strpbrk(fieldData, ";\n") != NULL)) {
    strm.write("[");
    strm.print(strlen(fieldData));
    strm.write("]");
  }

  strm.write(fieldData);

  // TODO: Handle this better?
  if (isFinalField) {
    strm.write("\n");
    delay(100);
  } else {
    strm.write(";");
  }

}

unsigned int lastWidgetId = 0;

unsigned int addLabel(Print& strm, const char *labelText, byte fontSize = 0, byte alignment = 0) {

  unsigned int widgetId = ++lastWidgetId;

  sendField(strm, "widget");

  sendField(strm, "label");

  // TODO: Handle other types in sendField?
  strm.print(widgetId);
  strm.write(";");

  strm.print(fontSize);
  strm.write(";");

  strm.print(alignment);
  strm.write(";");

  sendField(strm, labelText, true);

  return widgetId;

}


#include <SPI.h>
#include <Ethernet.h>

byte mac[] = {0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
IPAddress ip(169, 254, 254, 169);

EthernetServer server(0xba9);

void setup() {

  Serial.begin(9600);

  Ethernet.begin(mac, ip);

  server.begin();

  Serial.println("start");
}

void loop() {
  EthernetClient client = server.available();

  if (client) {

    Serial.println("connected.");

    addLabel(client, "A Label!", 35, 1);

    addLabel(client, "Further words", 35, 1);

    client.write("widget;dialog;Hello!\n");
    delay(100);

    addLabel(client, "[\nhello there;\n]", 35, 1);

    addLabel(client, "MORE", 100, 1);

    Serial.println("sent");

    while (client.connected()) {

      client.write("widget;label;10;35;1;");
      client.print(analogRead(A0));
      client.write("\n");
      delay(100);

      delay(1000);

      if (client.available() > 0) {
        client.read();
      }
      delay(10);
    }

    Serial.println("not connected");

    client.stop();

    Serial.println("stop.");

  }

}
