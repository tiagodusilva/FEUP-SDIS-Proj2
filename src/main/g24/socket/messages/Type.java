package main.g24.socket.messages;

public enum Type {
    BACKUP,        // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT> <FILEHASH> <REP_DEGREE> <FILE_SIZE>
    DELKEY,        // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT> <FILEHASH>
    DELCOPY,       // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT> <FILEHASH>
    GETFILE,       // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT> <FILEHASH>
    FILEHERE,      // <PROTOCOL> <SENDER_ID> <ID> <FILEHASH> <SIZE>
    FILEEXISTS,    // <PROTOCOL> <SENDER_ID> <FILEHASH>
    REPLICATE,     // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT> <FILEHASH> <FILE_SIZE> <REP_DEGREE>
    REPLICATED,    // <PROTOCOL> <SENDER_ID> <FILEHASH>
    ACK,           // <PROTOCOL> <SENDER_ID> <STATUS>
    REMOVED,       // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT> <FILEHASH>
    STATE,         // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT>
}
