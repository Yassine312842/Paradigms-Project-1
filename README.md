# AUI General Chat (Paradigms Project 1)

AUI General Chat is a **TCP multi-client chat application** built in **Java**, with a modern **JavaFX** client UI and a TCP server that broadcasts messages to connected clients. The UI follows AUI branding (green + white) and supports **read-only mode** when the username is left empty.

---

## Features

### Client (JavaFX)
- Modern **login screen**
  - Full-screen background image
  - Centered floating “glass” login card
  - AUI branding colors: **#0B5431 + white**
- Modern **chat screen**
  - Top green bar with **logo-only** (falls back to title if logo not found)
  - WhatsApp-like **message bubbles** (own / other / system)
  - Subtle **background image** inside the chat area (low opacity)
  - Users sidebar + refresh button
- **Read-only mode**
  - Leave username empty → connects in read-only (cannot send messages)

### Server
- Accepts multiple clients
- Broadcasts messages to all connected clients
- Supports commands:
  - `allUsers` → returns the list of active users
  - `bye` / `end` → disconnects the client

---

## Project Structure


Paradigms-Project-1/
├─ TCPServer/
│ └─ src/main/...
├─ TCPClient/
│ ├─ src/main/java/org/example/...
│ └─ src/main/resources/
│ ├─ styles.css
│ └─ images/
│ ├─ login_bg.jpg.jpg
│ ├─ aui_logo.png.png
│ └─ (optional) aui_logo_white.png
└─ PROTOCOL.md


---

## UI Assets (IMPORTANT)

Your client loads images from:


TCPClient/src/main/resources/images/


### Login (required)
- `login_bg.jpg.jpg`  → login background image
- `aui_logo.png.png`  → login logo image

### Chat Top Bar (optional, recommended)
- `aui_logo_white.png` → white/transparent logo for the top bar

If `aui_logo_white.png` is missing, the app tries other common names (including your current logo), and finally falls back to showing the title text.

### Note about “removing the white background”
JavaFX/CSS cannot remove a white rectangle baked into an image.  
To remove the logo’s white box, use a **transparent PNG**.

---

## How to Run

### 1) Run the Server
From the **TCPServer** directory:

```bash
mvn clean package
mvn exec:java

If you use IntelliJ:

Open the TCPServer module

Run the server main class

Ensure it is listening on the same host/port the client uses (default: localhost:3000)

2) Run the Client (JavaFX)

From the TCPClient directory:

mvn clean javafx:run

Then in the login page:

Server IP: localhost (or the server machine IP)

Port: 3000 (or whatever your server uses)

Username:

leave empty → read-only mode

enter a username → normal mode

Using the App
Sending Messages

Type a message and press Enter or click Send

Your own message appears immediately in the UI (the server may not echo your own message back)

Users List

Click Refresh users

The client sends the allUsers command and updates the sidebar list.

Disconnecting

Click Disconnect

The client sends bye then disconnects.

Commands

These commands are handled by the server:

allUsers → returns active users list

bye or end → disconnects

(See PROTOCOL.md for full protocol details.)

Troubleshooting
Images not showing (logo/background)

Check all of these:

Images exist inside:

TCPClient/src/main/resources/images/

Filenames match exactly:

login_bg.jpg.jpg

aui_logo.png.png

(optional) aui_logo_white.png

Rebuild and run again:

mvn clean javafx:run
“Online” but cannot type/send

You are in read-only mode:

Username was empty at login

Enter a username and reconnect.

JavaFX build issues

Make sure:

You are using the correct JDK version configured for the project

Maven is using the same JDK as your IDE

Notes

The client sends the username immediately after connecting.

If username is empty, the server considers the client read-only.

