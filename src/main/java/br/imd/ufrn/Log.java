package br.imd.ufrn;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Log curto no stdout/stderr (sem framework). Erros sempre; sucesso só quando pedido. */
public final class Log {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {
    }

    public static void info(String message) {
        System.out.println(stamp() + " " + message);
    }

    public static void error(String message) {
        System.err.println(stamp() + " [ERROR] " + message);
    }

    public static void error(String message, Throwable cause) {
        System.err.println(stamp() + " [ERROR] " + message + " | " + describe(cause));
    }

    public static String describe(Throwable cause) {
        if (cause == null) {
            return "sem causa";
        }
        String type = cause.getClass().getSimpleName();
        String detail = cause.getMessage();
        Throwable root = cause.getCause();
        if (root != null && root != cause) {
            return type + ": " + detail + " (causa: " + root.getClass().getSimpleName() + ": " + root.getMessage() + ")";
        }
        return type + ": " + detail;
    }

    private static String stamp() {
        return LocalTime.now().format(TIME);
    }
}
