package shit.zen.selfskin.account;

import java.util.UUID;

/** Persisted Yggdrasil identity. Passwords are deliberately absent. */
public record Account(String server, String username, UUID uuid, String accessToken, String clientToken) {
}
