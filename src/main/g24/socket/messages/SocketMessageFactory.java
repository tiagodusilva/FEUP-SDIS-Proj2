package main.g24.socket.messages;

public abstract class SocketMessageFactory {

    public static ISocketMessage from(String message) {
        String[] args = message.split(" ");

        if (args.length < 1)
            return null;

       return switch (Type.valueOf(args[0])) {
           case ACK -> AckMessage.from(args);
           case BACKUP -> BackupMessage.from(args);
           case REPLICATE -> ReplicateMessage.from(args);
           case DELKEY -> DeleteKeyMessage.from(args);
           case DELCOPY -> DeleteCopyMessage.from(args);
           default -> null;
       };
    }
}
