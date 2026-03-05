# Backend Test Plan

1. Start server (TCPServer).
2. Start two clients with usernames and confirm both connect.
3. Send messages and verify broadcast works.
4. Run command `allUsers` from a client and verify user list is returned.
5. Start a client with empty username and verify it becomes read-only.
6. Try sending a message from read-only client and verify server blocks it.
7. Disconnect with `bye` / `end` and verify server removes user.
8. Force-close a client window and verify server does not crash and user is removed.