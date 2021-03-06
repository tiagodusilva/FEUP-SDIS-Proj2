package main.g24.socket.messages;

import main.g24.chord.INode;

import java.rmi.RemoteException;

public class GetFileMessage implements ISocketMessage, ISocketFileMessage {

    // <PROTOCOL> <SENDER_ID> <SENDER_IP> <SENDER_PORT> <FILEHASH>

    public final int sender_id, sender_port;
    public final String sender_ip;
    public final String filehash;

    public GetFileMessage(int sender_id, String sender_ip, int sender_port, String filehash) {
        this.sender_id = sender_id;
        this.sender_port = sender_port;
        this.sender_ip = sender_ip;
        this.filehash = filehash;
    }

    public static GetFileMessage from(INode node, String filehash) {
        try {
            return new GetFileMessage(
                    node.get_id(),
                    node.get_address().getHostName(),
                    node.get_port(),
                    filehash
            );
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Type get_type() {
        return Type.GETFILE;
    }

    @Override
    public String gen_header() {
        return String.format("GETFILE %d %s %d %s", sender_id, sender_ip, sender_port, filehash);
    }

    @Override
    public String toString() {
        return "GETFILE " + sender_id + " " + filehash.substring(0, 6);
    }

    public static ISocketMessage from(String[] args) {
        if (args.length < 4)
            return null;

        return new GetFileMessage(
                Integer.parseInt(args[1]), // sender id
                args[2], // sender ip
                Integer.parseInt(args[3]), // sender port
                args[4] // filehash
        );
    }

    @Override
    public String get_filehash() {
        return filehash;
    }

    @Override
    public long get_size() { return -1; }

}
