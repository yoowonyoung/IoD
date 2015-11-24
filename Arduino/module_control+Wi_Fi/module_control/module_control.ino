#include <SoftwareSerial.h>

SoftwareSerial softSerial(2, 3); // RX, TX
String WSSID = "raspie";
String WPASS = "11111111";
String res = "";
bool r;
int relay1 = 4;
int relay2 = 5;

void setup() {
  /* SETUP SERIAL COMMUNICATION */
  espSerialSetup();
  pinMode(relay1, OUTPUT);
  pinMode(relay2, OUTPUT);
  delay(2000); // Without this delay, sometimes, the program will not start until Serial Monitor is connected
  r = espSendCommand( "AT" , "OK" , 5000 );
  r = espSendCommand( "AT+CIFSR" , "OK" , 5000 );
  if ( !r ) {
    r = espSendCommand( "AT+CWMODE=1" , "OK" , 5000 );
    r = espSendCommand( "AT+CWJAP=\"" + WSSID + "\",\"" + WPASS + "\"" , "OK" , 15000 );
  }
  pinMode(8, OUTPUT);
  digitalWrite(relay2, HIGH);
  digitalWrite(relay1, HIGH);
}

void loop() {
  r = espSendCommand( "AT+CIPSTART=\"TCP\",\"wy.iptime.org\",8099" , "OK" , 5000 );
  String getRequest = "GET /Test/Test HTTP/1.0\r\n";
  int getRequestLength = getRequest.length() + 2;
  r = espSendCommand( "AT+CIPSEND=" + String(getRequestLength) , "OK" , 5000 );
  if(espSendCommand( getRequest , "HEATON" , 5000 )){
     Serial.println("HEATON");
     digitalWrite(relay2, LOW); // 릴레이 키기
  }else {
    int resLen = res.length();
    String testFanON = res.substring(resLen - 14, resLen - 8);
    String testFanOFF = res.substring(resLen - 15, resLen - 8);
    String testHeatOFF = res.substring(resLen - 16, resLen - 8);
    int isFanON = testFanON.equals("FANON\n");
    int isFanOFF = testFanOFF.equals("FANOFF\n");
    int isHeatOFF = testHeatOFF.equals("HEATOFF\n");
    if(isFanON == 1) {
      Serial.println("FANON");
      digitalWrite(relay1, LOW); // 릴레이 킴
    }else if(isFanOFF == 1) {
      digitalWrite(relay1, HIGH); // 릴레이 끔
    }else if(isHeatOFF == 1) {
      digitalWrite(relay2, HIGH); // 릴레이 끔
    }
    res = "";
  }
  if ( !r ) {
    Serial.println( "Something wrong...Attempting reset...");
    //espSendCommand( "AT+RST" , "ready" , 20000);
    espSendCommand( "AT+CWMODE=1" , "OK" , 5000 );
    espSendCommand( "AT+CWJAP=\"" + WSSID + "\",\"" + WPASS + "\"" , "OK" , 15000 );
  }
  delay(3000);
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
        res = response;
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
