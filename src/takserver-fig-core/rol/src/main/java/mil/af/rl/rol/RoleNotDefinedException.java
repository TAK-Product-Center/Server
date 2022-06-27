package mil.af.rl.rol;

public class RoleNotDefinedException extends RuntimeException {

    private static final long serialVersionUID = 7478066972945634919L;

    public RoleNotDefinedException() { }

    public RoleNotDefinedException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RoleNotDefinedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoleNotDefinedException(String message) {
        super(message);
    }

    public RoleNotDefinedException(Throwable cause) {
        super(cause);
    }
}
