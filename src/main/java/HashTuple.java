public class HashTuple {
    private String elementHash;
    private String childrenHash;

    public HashTuple(String elementHash, String childrenHash) {
        this.elementHash = elementHash;
        this.childrenHash = childrenHash;
    }

    public String getElementHash() {
        return elementHash;
    }

    public void setElementHash(String elementHash) {
        this.elementHash = elementHash;
    }

    public String getChildrenHash() {
        return childrenHash;
    }

    public void setChildrenHash(String childrenHash) {
        this.childrenHash = childrenHash;
    }
}
