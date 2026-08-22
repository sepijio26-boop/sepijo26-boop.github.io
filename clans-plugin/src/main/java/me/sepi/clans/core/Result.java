package me.sepi.clans.core;

/**
 * Simple result object used by the service layer. A non-null error key means
 * the operation failed; the key maps to a message in messages.yml.
 */
public final class Result {

    public static final Result OK = new Result(null, new String[0]);

    private final String errorKey;
    private final String[] args;

    private Result(String errorKey, String[] args) {
        this.errorKey = errorKey;
        this.args = args;
    }

    public static Result error(String key) {
        return new Result(key, new String[0]);
    }

    public static Result error(String key, String... args) {
        return new Result(key, args);
    }

    public boolean ok() {
        return errorKey == null;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public String[] getArgs() {
        return args;
    }
}
