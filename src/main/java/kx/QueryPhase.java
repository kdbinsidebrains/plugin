package kx;

public enum QueryPhase {
    ENCODING("Encoding the query"),
    SENDING("Sending the query into the instance"),
    WAITING("Waiting response from the instance."),
    RECEIVING("Receiving incoming data"),
    DECODING("Decoding response from the instance");

    private final String description;

    QueryPhase(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
