
#include "URMSerial.h"
#include <Usb.h>
#include <AndroidAccessory.h>

AndroidAccessory acc("Marc Tan",
                  "Seeduino",
                  "Test Sonar",
                  "1.0",
                  "http://www.marctan.com",
                  "0000000012345678");

#define DISTANCE 1
#define ERROR 2
#define NOTREADY 3
#define TIMEOUT 4

URMSerial urm;

void setup() {

  Serial.begin(9600);                 
  urm.begin(11,12,9600);                 
  acc.powerOn();
  
}

void loop()
{
  if (acc.isConnected()) {
    
    unsigned int distance = getMeasurement();
    Serial.println(distance);
    
    byte msg[1];
    msg[0] = distance;
    acc.write(msg, 1);
    
    delay(50);  
  }
  
}  


int value; 
int getMeasurement()
{
  switch(urm.requestMeasurementOrTimeout(DISTANCE, value)) 
  {
  case DISTANCE: 
    return value;
    break;
  case ERROR:
    Serial.println("Error");
    break;
  case NOTREADY:
    Serial.println("Not Ready");
    break;
  case TIMEOUT:
    Serial.println("Timeout");
    break;
  } 

  return -1;
}








