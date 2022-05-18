#include "BLESerial.h"
#include "crc32.h"
#include "pic.h"
#include "pic2.h"
#include <BLEPeripheral.h>
#include <SPI.h>

#define PIN_LED1 2
BLESerial bleSerial;

void blePeripheralConnectHandler(BLECentral &central) {
    // central connected event handler
    Serial.print(F("Connected event, central: "));
    Serial.println(central.address());
    digitalWrite(PIN_LED1, HIGH);
}

void blePeripheralDisconnectHandler(BLECentral &central) {
    // central disconnected event handler
    Serial.print(F("Disconnected event, central: "));
    Serial.println(central.address());
    digitalWrite(PIN_LED1, LOW);
}

void setup() {
    // custom services and characteristics can be added as well
    Serial.begin(9600);
    bleSerial.setLocalName("Camera");
    bleSerial.setEventHandler(BLEConnected, blePeripheralConnectHandler);
    bleSerial.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);
    bleSerial.begin();
    pinMode(PIN_LED1, OUTPUT);
    digitalWrite(PIN_LED1, LOW);
}

void sendPicture(const uint8_t *pic, const size_t len) {
    uint32_t code = 0;
    uint8_t buffer[1 + sizeof(len) + sizeof(code)]; // 9 bytes
    buffer[0] = 0xFF;
    crc32(buffer, len, &code);
    memcpy(&buffer[1], &len, sizeof(len));
    memcpy(&buffer[1 + sizeof(len)], &code, sizeof(code));
    Serial.print(code);
    Serial.println();
    bleSerial.write(buffer, sizeof(buffer));
    bleSerial.flush();
    bleSerial.SendFile(pic, len);
}

bool pictureN = 0;
void handleMessage() {
    if (bleSerial) {
        int byte;
        while ((byte = bleSerial.read()) > 0) {
            switch (byte) {
            case 0x01:
                if (pictureN)
                    sendPicture(PICTURE1, PICTURE1_LEN);
                else
                    sendPicture(PICTURE2, PICTURE2_LEN);
                pictureN = !pictureN;
                break;
            default:
                break;
            }
        }
    }
}

void loop() {
    bleSerial.poll();
    handleMessage();
}