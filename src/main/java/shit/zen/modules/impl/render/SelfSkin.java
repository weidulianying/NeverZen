package shit.zen.modules.impl.render;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shit.zen.event.EventTarget;
import shit.zen.event.impl.GameTickEvent;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.selfskin.SkinCache;
import shit.zen.selfskin.SkinData;
import shit.zen.selfskin.TextureLoader;
import shit.zen.selfskin.account.Account;
import shit.zen.selfskin.account.LoginService;
import shit.zen.selfskin.account.TokenStorage;
import shit.zen.selfskin.provider.LittleSkinProvider;
import shit.zen.selfskin.provider.LocalProvider;
import shit.zen.selfskin.provider.MojangProvider;
import shit.zen.selfskin.provider.SkinProvider;
import shit.zen.selfskin.provider.UrlProvider;
import shit.zen.selfskin.provider.YggdrasilProvider;
import shit.zen.settings.impl.ActionSetting;
import shit.zen.settings.impl.BooleanSetting;
import shit.zen.settings.impl.ModeSetting;
import shit.zen.settings.impl.PasswordSetting;
import shit.zen.settings.impl.StringSetting;
import shit.zen.utils.misc.ThreadPool;

/** Lightweight local-player-only skin override. */
public final class SelfSkin extends Module {
    private static final Logger LOGGER = LogManager.getLogger(SelfSkin.class);
    public static SelfSkin INSTANCE;

    public final ModeSetting provider = new ModeSetting("Provider", "LittleSkin", "Mojang", "URL", "Local") {
        @Override
        public void setValue(String value) {
            // Migrate configs created while a custom Yggdrasil provider was available.
            super.setValue("Yggdrasil".equals(value) ? "LittleSkin" : value);
        }
    }
            .withDefault("LittleSkin");
    public final StringSetting username = new StringSetting("Username", "");
    public final StringSetting skinLocation = new StringSetting("Skin URL / Path", "");
    public final StringSetting capeLocation = new StringSetting("Cape URL / Path", "");
    public final PasswordSetting password = new PasswordSetting("Password");
    public final BooleanSetting autoLogin = new BooleanSetting("Auto Login", true);
    public final ActionSetting saveAccount = new ActionSetting("Save Account", this::loginAndSave);
    public final ActionSetting reload = new ActionSetting("Reload Skin", () -> requestReload(true));

    private final AtomicLong generation = new AtomicLong();
    private volatile ResourceLocation texture;
    private volatile ResourceLocation capeTexture;
    private volatile String model = "default";
    private volatile boolean loading;
    private volatile boolean initialLoadRequested;
    private volatile Account account;
    private SkinCache cache;
    private TokenStorage tokenStorage;
    private final TextureLoader textureLoader = new TextureLoader();
    private final LoginService loginService = new LoginService();

    public SelfSkin() {
        super("SelfSkin", Category.RENDER);
        INSTANCE = this;
        username.setVisibility(() -> !provider.is("URL") && !provider.is("Local"));
        skinLocation.setVisibility(() -> provider.is("URL") || provider.is("Local"));
        capeLocation.setVisibility(() -> provider.is("URL") || provider.is("Local"));
        password.setVisibility(() -> provider.is("LittleSkin"));
        autoLogin.setVisibility(() -> provider.is("LittleSkin"));
        saveAccount.setVisibility(() -> provider.is("LittleSkin"));
    }

    @Override
    protected void onEnable() {
        initializeStorage();
        initialLoadRequested = false;
    }

    @Override
    protected void onDisable() {
        generation.incrementAndGet();
        ResourceLocation previousSkin = texture;
        ResourceLocation previousCape = capeTexture;
        texture = null;
        capeTexture = null;
        model = "default";
        loading = false;
        releaseTexture(previousSkin);
        releaseTexture(previousCape);
    }

    @EventTarget
    public void onTick(GameTickEvent ignored) {
        if (!initialLoadRequested) {
            initialLoadRequested = true;
            requestReload(false);
        }
    }

    public ResourceLocation getTexture() {
        return isEnabled() ? texture : null;
    }

    public String getSkinModel() {
        return model;
    }

    public ResourceLocation getCapeTexture() {
        return isEnabled() ? capeTexture : null;
    }

    private void initializeStorage() {
        ensureUsername();
        if (cache != null) return;
        Path gameDirectory = mc.gameDirectory.toPath();
        cache = new SkinCache(gameDirectory);
        tokenStorage = new TokenStorage(gameDirectory);
        try {
            account = tokenStorage.load();
            if (account != null && username.getValue().isBlank()) username.setValue(account.username());
        } catch (Exception exception) {
            LOGGER.warn("Could not load SelfSkin account", exception);
        }
    }

    private void ensureUsername() {
        String configuredUsername = username.getValue();
        if (configuredUsername != null && !configuredUsername.isBlank()) return;
        String sessionUsername = mc.getUser().getName();
        if (sessionUsername != null && !sessionUsername.isBlank()) {
            username.setValue(sessionUsername);
        }
    }

    private void loginAndSave() {
        initializeStorage();
        char[] secret = password.getValue() == null ? new char[0] : password.getValue().clone();
        password.clear();
        ThreadPool.submit(() -> {
            try {
                Account loggedIn = loginService.login(LittleSkinProvider.API_ROOT, username.getValue(), secret);
                tokenStorage.save(loggedIn);
                account = loggedIn;
                username.setValue(loggedIn.username());
                LOGGER.info("SelfSkin login succeeded for {}", loggedIn.username());
                requestReload(true);
            } catch (Exception exception) {
                LOGGER.error("SelfSkin login failed: {}", exception.getMessage());
            } finally {
                Arrays.fill(secret, '\0');
            }
        });
    }

    public void requestReload(boolean force) {
        if (!isEnabled() || loading) return;
        initializeStorage();
        loading = true;
        long requestedGeneration = generation.incrementAndGet();
        ThreadPool.submit(() -> loadAsync(requestedGeneration, force));
    }

    private void loadAsync(long requestedGeneration, boolean force) {
        try {
            SkinProvider selectedProvider = createProvider();
            UUID uuid = resolveUuid(selectedProvider);
            SkinData skin = selectedProvider.getSkin(uuid);
            byte[] png = cache.load(skin.textureUri(), force);
            byte[] capePng = skin.capeUri() == null ? null : cache.load(skin.capeUri(), force);
            String skinKey = textureKey("skin", skin.textureUri().toString(), requestedGeneration);
            String capeKey = skin.capeUri() == null ? null
                    : textureKey("cape", skin.capeUri().toString(), requestedGeneration);
            CompletableFuture<ResourceLocation> skinUpload = textureLoader.uploadSkin(png, skinKey);
            skinUpload.thenCompose(location -> {
                if (capePng == null) return CompletableFuture.completedFuture(new LoadedTextures(location, null));
                return textureLoader.uploadCape(capePng, capeKey)
                        .thenApply(cape -> new LoadedTextures(location, cape))
                        .whenComplete((ignored, throwable) -> {
                            if (throwable != null) releaseTexture(location);
                        });
            }).whenComplete((loaded, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("SelfSkin texture upload failed", throwable);
                } else if (generation.get() == requestedGeneration && isEnabled()) {
                    ResourceLocation previousSkin = texture;
                    ResourceLocation previousCape = capeTexture;
                    texture = loaded.skin();
                    capeTexture = loaded.cape();
                    model = skin.model();
                    releaseTexture(previousSkin);
                    releaseTexture(previousCape);
                    LOGGER.info("SelfSkin loaded {} skin from {}{}", skin.model(), skin.textureUri(),
                            skin.capeUri() == null ? "" : " and cape from " + skin.capeUri());
                } else if (loaded != null) {
                    releaseTexture(loaded.skin());
                    releaseTexture(loaded.cape());
                }
                loading = false;
            });
        } catch (Exception exception) {
            loading = false;
            LOGGER.error("SelfSkin load failed: {}", exception.getMessage());
        }
    }

    private SkinProvider createProvider() {
        return switch (provider.getValue()) {
            case "Mojang" -> new MojangProvider();
            case "URL" -> new UrlProvider(skinLocation.getValue(), capeLocation.getValue());
            case "Local" -> new LocalProvider(skinLocation.getValue(), capeLocation.getValue());
            default -> new LittleSkinProvider();
        };
    }

    private UUID resolveUuid(SkinProvider selectedProvider) throws Exception {
        if (selectedProvider instanceof UrlProvider || selectedProvider instanceof LocalProvider) {
            return mc.player.getUUID();
        }
        if (account != null && LittleSkinProvider.API_ROOT.equals(account.server())
                && (autoLogin.getValue() ? loginService.validate(account) : true)) {
            return account.uuid();
        }
        String name = username.getValue();
        if (selectedProvider instanceof MojangProvider mojang) return mojang.resolveUuid(name);
        return ((YggdrasilProvider) selectedProvider).resolveUuid(name);
    }

    private static String textureKey(String type, String source, long generation) throws Exception {
        return type + "/" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((source + "#" + generation).getBytes(StandardCharsets.UTF_8)));
    }

    private void releaseTexture(ResourceLocation location) {
        if (location != null) mc.execute(() -> mc.getTextureManager().release(location));
    }

    private record LoadedTextures(ResourceLocation skin, ResourceLocation cape) {}
}
