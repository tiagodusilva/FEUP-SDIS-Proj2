package main.g24;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileDetails implements Serializable {

    private final String hash;
    private final long size;
    private final int desiredRepDegree;

    private final List<Integer> copies;

    // File details on initiator
    public FileDetails(String hash, long size, int desiredRepDegree) {
        this.hash = hash;
        this.size = size;
        this.desiredRepDegree = desiredRepDegree;
        this.copies = new CopyOnWriteArrayList<>();
    }

    // File Details on store
    public FileDetails(String hash, long size) {
        this.hash = hash;
        this.size = size;
        this.desiredRepDegree = -1;
        this.copies = null;
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public int getDesiredReplication() { return desiredRepDegree; }

    public int getPerceivedReplication() { return copies.size(); }

    public boolean lacksReplication() {
        return this.getPerceivedReplication() < this.getDesiredReplication();
    }

    public int missingReplications() {
        return Math.max(0, getDesiredReplication() - getPerceivedReplication());
    }

    public List<Integer> getFileCopies() { return copies; }

    public void addCopy(int peerID) {
        if (!copies.contains(peerID))
            this.copies.add(peerID);
    }

    public void removeCopy(int peerID) {
        int idx = this.copies.indexOf(peerID);

        if (idx != -1)
            this.copies.remove(idx);
    }

    public int getLastCopy() {
        return this.copies.isEmpty() ? -1 : this.copies.get(copies.size()-1);
    }

    @Override
    public String toString() {
        return "hash=" + hash.substring(0, 6) +
                "\tsize=" + size / 1000 + "KB" +
                "\tdesiredRepDegree=" + desiredRepDegree +
                "\tcopies=" + copies;
    }
}
