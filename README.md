# JavaCard Secret storage

## Authors
    - David Gajdoš (468971@mail.muni.cz)
    - Daud Naveed (530712@mail.muni.cz)
    - Zargham Ahmad (530713@mail.muni.cz)

## Description
This is a [FI MUNI](https://www.fi.muni.cz)  [PV204](https://is.muni.cz/predmet/fi/jaro2019/PV204) course's project
for JavaCard.

The project consists of a JavaCard applet and CLI application interacting with 
the smartcard. The emphasis is on securing the stored secrets and securing
communication between application and card.

Main functionalities of the applet are:

    - Granting access to interact with applet only after entering correct PIN
    - Storing key-value pairs of secrets (encrypted)
    - Listing available keys
    - If special DURESS_PIN is entered instead of PIN, the applet will erase all stored secrets

Main functionalities of the application are:

    - Provide user-friendly communication to the applet
    - After entering correct PIN, user will be allowed to enter key-value pairs and store them in the card
