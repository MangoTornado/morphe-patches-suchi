package suchi.morphe.extension.duchinese;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

public final class DownloadDiagnostics {
    private static final Object UNAVAILABLE = new Object();
    private static final ThreadLocal<String> FAILURE = new ThreadLocal<>();
    private static final Pattern MIME_TYPE =
            Pattern.compile("[a-zA-Z0-9!#$&^_.+-]+/[a-zA-Z0-9!#$&^_.+-]+");

    private DownloadDiagnostics() {}

    public static void capture(Object error) {
        clear();
        StringBuilder details = new StringBuilder("Volley: ");
        details.append(error == null ? "unavailable" : error.getClass().getName());
        Object response = readField(error, "x");
        if (response == null) {
            details.append("\nHTTP: unavailable (no attached response)");
        } else if (response == UNAVAILABLE) {
            details.append("\nHTTP: unavailable");
        } else {
            Object status = readField(response, "a");
            details.append("\nHTTP: ").append(status instanceof Integer ? status : "unavailable");
            Object body = readField(response, "b");
            if (body instanceof byte[]) {
                details.append("\nResponse bytes: ").append(((byte[]) body).length);
            }
            String contentType = contentType(readField(response, "c"));
            if (contentType != null) {
                details.append("\nContent-Type: ").append(contentType);
            }
        }
        if (error instanceof Throwable) {
            Throwable cause = ((Throwable) error).getCause();
            if (cause != null) {
                details.append("\nCause: ").append(cause.getClass().getName());
            }
        }
        FAILURE.set(details.toString());
    }

    public static CharSequence appendToMessage(CharSequence message, int resourceId) {
        String details = FAILURE.get();
        clear();
        return details == null || resourceId != 0x7f13020b ? message : message + "\n\n" + details;
    }

    public static void clear() {
        FAILURE.remove();
    }

    private static Object readField(Object object, String name) {
        if (object == null) return UNAVAILABLE;
        try {
            Field field = object.getClass().getField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return UNAVAILABLE;
        }
    }

    private static String contentType(Object headers) {
        if (!(headers instanceof Map)) return null;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) headers).entrySet()) {
            if (!(entry.getKey() instanceof String) ||
                    !"Content-Type".equalsIgnoreCase((String) entry.getKey()) ||
                    !(entry.getValue() instanceof String)) continue;
            String value = ((String) entry.getValue()).split(";", 2)[0].trim();
            return value.length() <= 100 && MIME_TYPE.matcher(value).matches() ? value : null;
        }
        return null;
    }
}
