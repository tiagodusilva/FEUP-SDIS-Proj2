package main.g24.socket.managers.dispatchers;

import main.g24.FileDetails;
import main.g24.Peer;
import main.g24.chord.Node;
import main.g24.socket.managers.ISocketManager;
import main.g24.socket.managers.StateSocketManager;
import main.g24.socket.managers.ReceiveFileSocket;
import main.g24.socket.managers.SendFileSocket;
import main.g24.socket.managers.SocketManager;
import main.g24.socket.messages.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;


public class DefaultSocketManagerDispatcher implements ISocketManagerDispatcher {

    private final Peer peer;

    public DefaultSocketManagerDispatcher(Peer peer) {
        this.peer = peer;
    }

    @Override
    public ISocketManager dispatch(ISocketMessage message, SelectionKey key) {
        try {
            return switch (message.get_type()) {
                case BACKUP -> {
                    BackupMessage fileMessage = (BackupMessage) message;
                    boolean hadOld = peer.isResponsibleForFile(fileMessage.filehash);

                    if (hadOld) {
                        peer.deleteFileCopies(fileMessage.get_filehash());
                    }

                    if (peer.hasCapacity(fileMessage.get_size())) {
                        peer.addFileToKey(fileMessage.get_filehash(), fileMessage.get_size(), fileMessage.get_rep_degree(), this.peer.get_id());
                        peer.addStoredFile(fileMessage.get_filehash(), fileMessage.get_size());

                        yield new ReceiveFileSocket(peer, (ISocketFileMessage) message, () -> {
                            AckMessage ack = new AckMessage(peer.get_id(), true);
                            try {
                                ack.send((SocketChannel) key.channel());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // Replication go bbbbrrrrr
                            ReplicateMessage repl = ReplicateMessage.from(peer, fileMessage.filehash, fileMessage.file_size, fileMessage.rep_degree - 1);
                            try {
                                SocketChannel socket = SocketChannel.open();
                                socket.configureBlocking(false);
                                socket.connect(peer.get_successor().get_socket_address());

                                ISocketManager repl_manager = new SocketManager(() -> {
                                    try {
                                        repl.send(socket);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                });

                                peer.getSelector().register(socket, repl_manager);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        });
                    }

                    System.err.println("BACKUP TO ANOTHER NODE DUE TO LACK OF STORAGE NOT YET IMPLEMENTED");
                    yield null;
                }

                case REPLICATE -> {
                    ReplicateMessage fileMessage = (ReplicateMessage) message;

                    if (fileMessage.origin_id == peer.get_id())
                        yield null;

                    boolean decreased = peer.hasCapacity(fileMessage.file_size);
                    if (decreased) {
                        ReplicateDispatcher.initiateReplicate(peer, fileMessage.filehash);

                        if (fileMessage.rep_degree - 1 <= 0)
                            yield null;
                    }

                    // Redirect to successor
                    SocketChannel channel = SocketChannel.open();
                    channel.configureBlocking(false);
                    channel.connect(peer.get_successor().get_socket_address());

                    ReplicateMessage chainMessage = ReplicateMessage.from(peer, fileMessage, decreased);
                    ISocketManager chainManager = new SocketManager(() -> {
                        try {
                            chainMessage.send(channel);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                    peer.getSelector().register(channel, chainManager);

                    yield null;
                }

                case REPLICATED -> {
                    ReplicatedMessage replicated = (ReplicatedMessage) message;
                    peer.addResponsible(replicated.filehash, replicated.sender_id);
                    yield null;
                }

                case DELKEY -> {
                    ISocketFileMessage deleteMessage = (ISocketFileMessage) message;
                    boolean status = peer.deleteFileCopies(deleteMessage.get_filehash());
                    AckMessage reply = new AckMessage(peer.get_id(), status);
                    reply.send((SocketChannel) key.channel());
                    yield null;
                }

                case DELCOPY -> {
                    ISocketFileMessage deleteMessage = (ISocketFileMessage) message;
                    peer.deleteFile(deleteMessage.get_filehash());
                    yield null; // delete copy messages don't require Acknowledgements
                }

                case REMOVED -> {
                    RemovedMessage removedMessage = (RemovedMessage) message;

                    // remove tracked copy from reclaimed peer
                    peer.removeTrackedCopy(removedMessage.get_filehash(), removedMessage.sender_id);
                    // send acknowledgement to reclaimed peer
                    new AckMessage(peer.get_id(), true).send((SocketChannel) key.channel());
                    // TODO: Replicate if needed
                    yield null;
                }

                case GETFILE -> {
                    GetFileMessage fileMessage = (GetFileMessage) message;
                    ISocketMessage reply;
                    ISocketManager futureManager = null;
                    Collection<Integer> peers_storing;
                    if (peer.storesFile(fileMessage.filehash)) {
                        Path path = Paths.get(peer.getStoragePath(fileMessage.filehash));
                        reply = FileHereMessage.from(peer, peer.get_id(), fileMessage.filehash, Files.size(path));
                        futureManager = new SendFileSocket(path);
                    } else if ((peers_storing = peer.findWhoStores(fileMessage.filehash)) == null || peers_storing.isEmpty()) {
                        reply = new AckMessage(peer.get_id(), false);
                    } else {
                        int chosen_peer = peers_storing.stream().findAny().get();
                        reply = FileHereMessage.from(peer, chosen_peer, fileMessage.filehash, -1);
                    }
                    reply.send((SocketChannel) key.channel());
                    yield futureManager;
                }

                case STATE -> new StateSocketManager(peer);

                case FILEEXISTS -> {
                    FileExistsMessage exists = (FileExistsMessage) message;
                    AckMessage reply = new AckMessage(peer.get_id(), peer.isResponsible(exists.filehash));
                    reply.send((SocketChannel) key.channel());
                    yield null;
                }

                case REPLLOST -> {
                    ReplicationLostMessage lost = (ReplicationLostMessage) message;
                    FileDetails fd;
                    if ((fd = peer.removeTrackedCopy(lost.filehash, lost.peer_lost)) != null && fd.lacksReplication()) {
                        ReplicateMessage replMessage = ReplicateMessage.from(peer, lost.filehash, fd.getSize(), fd.missingReplications());

                        SocketChannel socket = SocketChannel.open();
                        socket.configureBlocking(false);
                        socket.connect(peer.get_successor().get_socket_address());

                        peer.getSelector().register(socket, new SocketManager(() -> {
                            try {
                                replMessage.send(socket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }));
                    }
                    yield null;
                }

                default -> null;
            };
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
