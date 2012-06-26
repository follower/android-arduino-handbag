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

    server.write("widget;label;1;35;1;A Label!\n");
    delay(100);    
    server.write("widget;label;2;35;1;Further words\n");
    delay(100);    
    server.write("widget;dialog;Hello!\n");
    delay(100);

    Serial.println("sent");

    while (client.connected()) {

      server.write("widget;label;3;35;1;");
      server.print(analogRead(A0));
      server.write("\n");
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
