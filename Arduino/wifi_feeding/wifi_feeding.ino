#include <SoftwareSerial.h>

int motorPin = 8;

SoftwareSerial softSerial(2, 3); // RX, TX
String WSSID = "SOGYOSU";
String WPASS = "9220230s";
bool r;

void setup() {
  Serial.begin(9600);
  pinMode(motorPin, OUTPUT);

  espSerialSetup();
  delay(2000); // Without this delay, sometimes, the program will not start until Serial Monitor is connected
  r = espSendCommand( "AT+CIFSR" , "OK" , 5000 );
  if ( !r ) {
    r = espSendCommand( "AT+CWMODE=1" , "OK" , 5000 );
    r = espSendCommand( "AT+CWJAP=\"" + WSSID + "\",\"" + WPASS + "\"" , "OK" , 15000 );
  }
  pinMode(8, OUTPUT);
}

void loop() {
  // Wait a few seconds between measurements.
  delay(10000);

  r = espSendCommand( "AT+CIPSTART=\"TCP\",\"201310491.iptime.org\",6974" , "OK" , 5000 );
  String getRequest = "GET /iodsc/iodcontrol?action=getControl&moduleName=FEED";
  int getRequestLength = getRequest.length() + 2;
  r = espSendCommand( "AT+CIPSEND=" + String(getRequestLength) , "OK" , 5000 );
  if(espSendCommand( getRequest , "ONCE" , 3000 )){
      analogWrite(motorPin, 255);
      delay(15000);
      analogWrite(motorPin, 0);
  }
  if ( !r ) {
    Serial.println( "Something wrong...Attempting reset...");
    //espSendCommand( "AT+RST" , "ready" , 20000);
    espSendCommand( "AT+CWMODE=1" , "OK" , 5000 );
    espSendCommand( "AT+CWJAP=\"" + WSSID + "\",\"" + WPASS + "\"" , "OK" , 15000 );
  }
}

void espSerialSetup() {
  softSerial.begin(115200); // default baud rate for ESP8266
  delay(1000);
  softSerial.println("AT+CIOBAUD=9600");
  delay(1000);
  softSerial.begin(9600);
  Serial.begin(9600);
}

bool espSendCommand(String cmd, String goodResponse, unsigned long timeout) {
  Serial.println("espSendCommand( " + cmd + " , " + goodResponse + " , " + String(timeout) + " )" );
  softSerial.println(cmd);
  unsigned long tnow = millis();
  unsigned long tstart = millis();
  unsigned long execTime = 0;
  String response = "";
  char c;
  while ( true ) {
    if ( tnow > tstart + timeout ) {
      Serial.println("espSendCommand: FAILED - Timeout exceeded " + String(timeout) + " seconds" );
      if ( response.length() > 0 ) {
        Serial.println("espSendCommand: RESPONSE:");
        Serial.println( response );
      } else {
        Serial.println("espSendCommand: NO RESPONSE");
      }
      return false;
    }
    c = softSerial.read();
    if ( c >= 0 ) {
      response += String(c);
      if ( response.indexOf(goodResponse) >= 0 ) {
        execTime = ( millis() - tstart );
        Serial.println("espSendCommand: SUCCESS - Response time: " + String(execTime) + "ms");
        Serial.println("espSendCommand: RESPONSE:");
        Serial.println(response);
        while (softSerial.available() > 0) {
          Serial.write(softSerial.read());
        }
        return true;
      }
    }
    tnow = millis();
  }
}
