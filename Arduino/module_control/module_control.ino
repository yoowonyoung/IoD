#include <SoftwareSerial.h>


int relay = 13;

void setup() {
  Serial.begin(9600);
  pinMode(relay, OUTPUT);

}

void loop() {
  digitalWrite(relay, HIGH); // 릴레이를 작동
  delay(2000); // 2초 대기
  digitalWrite(relay, LOW); // 없을 경우 릴레이를 끔
  delay(2000); // 2초 대기
  digitalWrite(relay, HIGH); // 릴레이를 작동
}
