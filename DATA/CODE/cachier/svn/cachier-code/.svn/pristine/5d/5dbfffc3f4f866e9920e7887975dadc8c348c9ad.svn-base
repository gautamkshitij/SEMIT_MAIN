package org.zm;

public class CachierException extends Exception {

    public static enum Reason {
	NOT_ENOUGH_SPACE_IN_CACHE, NO_LINKER, NO_COPIER, NO_MOVER, NO_DELETER, NO_SIZER
    }

    private Reason cause;

    /**
     * @param cause
     */
    public CachierException(Reason cause) {
	super();
	this.cause = cause;
    }

    public Reason getReason() {
	return cause;
    }

}
