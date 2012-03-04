#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

// Rewritten by Assaf Gamliel (goo.gl/E2MhJ)
// Send and Receive Minimum Arduino code.

// Arduino (ADK) description. enter here what you'll
// add to the accessory_filter.xml in the Android code.
AndroidAccessory acc("enter manufacturer here",
                     "enter model here",
                     "enter description here",
                     "enter version here",
                     "enter URL here",
                     "0000000012345678"); // Serial number
                     
int sensorPin = 0; //analog pin 0
void setup()
{
  acc.powerOn();
}

void loop()
{

  // Incoming data from Android device.
  byte msg[1];
  
  if (acc.isConnected()) 
  {
    // The reading from Android code.
    int len = acc.read(msg, sizeof(msg), 1);
    
    // Send message to Android.
    sendMessage(len);
  }
}

// Sending data to the Android device.
void sendMessage(int value)
{
  if (acc.isConnected()) 
  {
    byte msg[2];
    msg[0] = value >> 8;
    msg[1] = value & 0xff;
    
    acc.write(msg, 2);
  }
}


