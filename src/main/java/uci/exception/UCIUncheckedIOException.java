package uci.exception;

public class UCIUncheckedIOException extends UCIRuntimeException {

    public UCIUncheckedIOException(Throwable cause) {
        super(cause);
    }

    public UCIUncheckedIOException(String message) {
        super(message);
    }
}
