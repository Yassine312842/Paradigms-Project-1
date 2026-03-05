# Chat Protocol (TCP)

## Connection / Handshake
- Client connects to server via TCP.
- First line sent by client is the username.
- If username is empty, server assigns a READ_ONLY_* identity and enforces read-only mode.

## Commands (sent as plain text lines)
- allUsers
    - Server replies only to the requester with the active user list.
- bye / end
    - Server disconnects the client cleanly.

## Message Format (server -> clients)
- Normal messages:
    - [HH:mm] <username>: <message>
- System messages:
    - [HH:mm] (SYSTEM): <text>