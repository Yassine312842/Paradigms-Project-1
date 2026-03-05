# AUI General Chat (Paradigms Project 1)

AUI General Chat is a **TCP multi-client chat application** built in **Java**, with a modern **JavaFX** GUI for both the client and server, WhatsApp-like chat bubbles, and support for **read-only mode**.

---

## Features

### Client (JavaFX)
- Modern **login screen** with full-screen background, floating glass card, AUI branding (#0B5431 + white)
- Modern **chat screen** with WhatsApp-like **message bubbles** (own / other / system)
- **Green circle** online status indicator (turns grey on disconnect)
- Users sidebar with refresh button
- **Read-only mode** — leave username empty → can observe but cannot send messages
- Disconnect button sends `bye` and returns to login

### Server (JavaFX Dashboard)
- **Live log area** displaying events: "Server Started", "Waiting for Client", "Welcome [username]", "Client disconnected"
- **Connected clients ListView** with **random color dots** per username
- **Online status indicator** with server address display
- Accepts multiple simultaneous client connections (thread-per-client model)
- Supports commands: `allUsers`, `bye` / `end`

---

## Project Structure

```
Paradigms-Project-1/
├── TCPServer/
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/example/
│       │   ├── TCPServer.java          # Launcher (GUI or --headless)
│       │   ├── ServerApp.java          # JavaFX server dashboard
│       │   ├── ServerModel.java        # Networking & business logic
│       │   └── ClientHandler.java      # Per-client thread handler
│       └── resources/
│           ├── server.properties       # host/port configuration
│           ├── server-styles.css       # Server UI styles
│           └── META-INF/MANIFEST.MF
├── TCPClient/
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/example/
│       │   ├── ChatApp.java            # JavaFX client UI (controller+view)
│       │   ├── ClientModel.java        # Networking model
│       │   └── TCPClient.java          # Console-mode client
│       └── resources/
│           ├── styles.css              # Client UI styles
│           └── images/
│               ├── login_bg.jpg.jpg
│               ├── aui_logo.png.png
│               └── aui_logo_white.png.png
├── PROTOCOL.md
├── TESTING.md
└── README.md
```

---

## Configuration

Server IP and port are loaded from `TCPServer/src/main/resources/server.properties`:

```properties
server.host=localhost
server.ip=127.0.0.1
server.port=3000
```

---

## How to Run

### Prerequisites
- **JDK 21+** installed
- **Maven 3.8+** installed (or use IntelliJ's built-in Maven)

### 1) Run the Server

From the **TCPServer** directory:

```bash
mvn clean javafx:run
```

The server dashboard will open showing "Server Started" and "Waiting for Client".

**Headless mode** (console only):
```bash
mvn exec:java -Dexec.args="--headless"
```

### 2) Run the Client (JavaFX)

From the **TCPClient** directory:

```bash
mvn clean javafx:run
```

In the login page:
- **Server IP**: `localhost` (or the server machine IP)
- **Port**: `3000`
- **Username**: enter a name → normal mode; leave empty → read-only mode

### Using IntelliJ IDEA

1. Open `Paradigms-Project-1` as a project
2. Import `TCPServer` and `TCPClient` as Maven modules
3. Run `ServerApp.main()` for the server
4. Run `ChatApp.main()` for the client

---

## Commands

| Command | Description |
|---------|-------------|
| `allUsers` | Returns list of active users (click "Refresh users" in the UI) |
| `bye` | Disconnects the client |
| `end` | Disconnects the client |

---

## Architecture

The project follows **Model-View-Controller** separation:

| Layer | Client | Server |
|-------|--------|--------|
| **Model** | `ClientModel.java` — socket I/O | `ServerModel.java` — socket accept, broadcast |
| **View** | `ChatApp.java` — JavaFX UI | `ServerApp.java` — JavaFX Dashboard |
| **Controller** | `ChatApp.java` — event handlers | `ServerApp.java` — wires callbacks |

**Concurrency**: Thread-per-client model — each `ClientHandler` runs on its own thread.

---

## Protocol

See [PROTOCOL.md](PROTOCOL.md) for the full TCP message protocol.

**Summary**:
- Client sends username as the first line (handshake)
- Empty username → read-only mode
- Messages: `[HH:mm] username: message`
- System messages: `[HH:mm] (SYSTEM): text`

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Images not showing | Verify files exist in `TCPClient/src/main/resources/images/` with exact names |
| "Online" but cannot type | You are in read-only mode — reconnect with a username |
| JavaFX errors | Ensure JDK 21+ and `javafx-controls` dependency is resolved |
| Build fails | Run `mvn clean compile` to check for errors; ensure correct JDK in IntelliJ |
