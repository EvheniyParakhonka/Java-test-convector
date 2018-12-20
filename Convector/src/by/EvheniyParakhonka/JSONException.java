package by.EvheniyParakhonka;

public class JSONException extends RuntimeException {
    private static final long serialVersionUID = 0;


    public JSONException(final String pMessage) {
        super(pMessage);
    }

    public JSONException(final String pMessage, final Throwable cause) {
        super(pMessage, cause);
    }

    public JSONException(final Throwable pCause) {
        super(pCause.getMessage(), pCause);
    }

}
