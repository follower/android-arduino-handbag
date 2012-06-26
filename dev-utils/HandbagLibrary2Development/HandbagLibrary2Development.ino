#include <SPI.h>
#include <Ethernet.h>

byte mac[] = {0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
IPAddress ip(169, 254, 254, 169);

EthernetServer server(0xba9);

void setup() {
  Ethernet.begin(mac, ip);

  server.begin();

}

void loop() {
  EthernetClient client = server.available();

  if (client) {

    //server.write("widget;label;1;35;1;A Label!\nwidget;dialog;Hello!\n");
    server.write("widget;label;1;35;1;A Label!\n");
    delay(100);    
    server.write("widget;label;2;35;1;Further words\n");
    delay(100);    
    server.write("widget;dialog;Hello!\n");
    delay(100);
    client.stop();
    
/*    
    while (client.connected()) {
      if (client.available() > 0) {
  
        char thisChar = client.read();
  
        server.write(thisChar);
      }
    }
*/  

  }
  
}
