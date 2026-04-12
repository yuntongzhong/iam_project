package com.example.iam.config;

import java.util.List;

public final class DemoCredentialCatalog {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ALICE_USERNAME = "alice";
    public static final String BOB_USERNAME = "bob";

    public static final String ADMIN_PASSWORD = "Admin#2026!Secure";
    public static final String ALICE_PASSWORD = "Alice#2026!Secure";
    public static final String BOB_PASSWORD = "Bob#2026!Audit";

    public static final String APP_A_CLIENT_ID = "app-a";
    public static final String APP_B_CLIENT_ID = "app-b";
    public static final String APP_A_CLIENT_SECRET = "AppA#2026!ClientSecret";
    public static final String APP_B_CLIENT_SECRET = "AppB#2026!ClientSecret";

    public static final List<String> SEED_USERNAMES = List.of(
            ADMIN_USERNAME,
            ALICE_USERNAME,
            BOB_USERNAME
    );

    public static final List<String> SEED_CLIENT_IDS = List.of(
            APP_A_CLIENT_ID,
            APP_B_CLIENT_ID
    );

    private DemoCredentialCatalog() {
    }
}
