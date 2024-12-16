#include <Wire.h>
#include "Adafruit_Trellis.h"

#define NUMTRELLIS 1
#define numKeys (NUMTRELLIS * 16)

Adafruit_Trellis matrix0 = Adafruit_Trellis();
Adafruit_TrellisSet trellis = Adafruit_TrellisSet(&matrix0);

const int MaxNumLeds = 10;
int selectedLEDsToLight[MaxNumLeds];
int playerInput[MaxNumLeds];
int currentLevel = 3;
bool playerInputActive = false;
int playerIndex = 0;

int ledLightDuration = 4000;  // 4 sec
unsigned long lastLedLightTime = 0;

//  Game states
enum GameState {
  DISPLAY_PHASE,
  INPUT_PHASE,
  GAME_OVER
};

GameState currentState = DISPLAY_PHASE;

void setup() {
  Serial.begin(9600);
  delay(500);

  randomSeed(analogRead(0));

  trellis.begin(0x74);  // I2C address of the Trellis

  // Cool light chain effect
  for (int i = 0; i < numKeys; i++) {
    trellis.setLED(i);
    trellis.writeDisplay();
    delay(50);
  }

  for (int i = 0; i < numKeys; i++) {
    trellis.clrLED(i);
    trellis.writeDisplay();
    delay(50);
  }

  // Randomly select LEDs
  for (int i = 0; i < MaxNumLeds; i++) {
    selectedLEDsToLight[i] = i;
  }
  shuffleArray(selectedLEDsToLight, MaxNumLeds);
  printArray();

  displayLevelLEDs(currentLevel);
}

void loop() {
  delay(30);

  switch (currentState) {
    case DISPLAY_PHASE:
      if (millis() - lastLedLightTime >= ledLightDuration) {
        trellis.clear();
        trellis.writeDisplay();
        lastLedLightTime = millis();
        playerInputActive = true;
        playerIndex = 0;
        currentState = INPUT_PHASE;
        Serial.println("Input phase starts!");
      }
      break;

    case INPUT_PHASE:
      if (trellis.readSwitches()) {
        for (int i = 0; i < numKeys; i++) {
          if (trellis.justPressed(i)) {
            Serial.print("Button pressed: ");
            Serial.println(i);

            if (i == selectedLEDsToLight[playerIndex]) {
              Serial.println("Correct button!");
              trellis.setLED(i);
              playerInput[playerIndex++] = i;

              if (playerIndex >= currentLevel) {
                if (playerWonLevel()) {
                  Serial.println("Level passed!");
                  currentLevel++;
                  if (currentLevel > MaxNumLeds) {
                    Serial.println("Game won!");
                    currentState = GAME_OVER;
                  } else {
                    displayLevelLEDs(currentLevel);
                    currentState = DISPLAY_PHASE;
                  }
                }
              }
            } else {
              Serial.println("Wrong button. Game over.");
              currentState = GAME_OVER;
            }
          }
        }
        trellis.writeDisplay();
      }
      break;

    case GAME_OVER:
      Serial.println("Game over");
      resetGame();
      break;
  }
}

void shuffleArray(int *array, int size) {
  for (int i = size - 1; i > 0; i--) {
    int j = random(0, i + 1);
    int temp = array[i];
    array[i] = array[j];
    array[j] = temp;
  }
}

void printArray() {
  Serial.println("Random LED sequence:");
  for (int i = 0; i < MaxNumLeds; i++) {
    Serial.print(selectedLEDsToLight[i]);
    Serial.print(" ");
  }
  Serial.println();
}

void displayLevelLEDs(int currentLevel) {
  for (int i = 0; i < currentLevel; i++) {
    trellis.setLED(selectedLEDsToLight[i]);
    Serial.print("LED ");
    Serial.print(selectedLEDsToLight[i]);
    Serial.println(" is lighted up.");
  }
  trellis.writeDisplay();
  lastLedLightTime = millis();
}

bool playerWonLevel() {
  for (int i = 0; i < currentLevel; i++) {
    if (playerInput[i] != selectedLEDsToLight[i]) {
      return false;
    }
  }
  return true;
}

void resetGame() {
  currentLevel = 3;
  shuffleArray(selectedLEDsToLight, MaxNumLeds);
  printArray();
  displayLevelLEDs(currentLevel);
  currentState = DISPLAY_PHASE;
}
