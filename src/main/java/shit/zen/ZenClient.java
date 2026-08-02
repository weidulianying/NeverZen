package shit.zen;

import asm.patchify.loader.PatchAgent;
import asm.patchify.loader.PatchRegistry;
import java.io.File;
import java.lang.reflect.Field;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shit.zen.event.EventBus;
import shit.zen.event.EventTarget;
import shit.zen.event.impl.TickEvent;
import shit.zen.gui.IntroAnimation;
import shit.zen.manager.CommandManager;
import shit.zen.manager.ConfigManager;
import shit.zen.manager.HudManager;
import shit.zen.manager.LagManager;
import shit.zen.manager.ModuleManager;
import shit.zen.manager.TargetManager;
import shit.zen.config.ProfileAvatar;
import shit.zen.patch.BlockOcclusionCachePatch;
import shit.zen.patch.BlockPatch;
import shit.zen.patch.ChatScreenPatch;
import shit.zen.patch.ClientLevelPatch;
import shit.zen.patch.ConnectionPatch;
import shit.zen.patch.EntityPatch;
import shit.zen.patch.EntityRendererPatch;
import shit.zen.patch.FriendlyByteBufPatch;
import shit.zen.patch.GameRendererPatch;
import shit.zen.patch.HumanoidModelPatch;
import shit.zen.patch.ItemInHandLayerPatch;
import shit.zen.patch.ItemInHandRendererPatch;
import shit.zen.patch.ItemPatch;
import shit.zen.patch.KeyboardHandlerPatch;
import shit.zen.patch.KeyboardInputPatch;
import shit.zen.patch.LevelRendererPatch;
import shit.zen.patch.LivingEntityPatch;
import shit.zen.patch.LivingEntityRendererPatch;
import shit.zen.patch.LocalPlayerPatch;
import shit.zen.patch.MinecraftPatch;
import shit.zen.patch.MultiPlayerGameModePatch;
import shit.zen.patch.PacketUtilsPatch;
import shit.zen.patch.PlayerPatch;
import shit.zen.patch.PlayerTabOverlayPatch;
import shit.zen.patch.AbstractClientPlayerPatch;
import shit.zen.asm.Bootstrap;
import shit.zen.client.render.effect.KillEffectManager;
import shit.zen.utils.rotation.RotationHandler;

@Mod(value = "hey")
@Getter
@Setter
public class ZenClient extends ClientBase {
    @Getter
    public static ZenClient instance;
    public static final String CLIENT_NAME = "Zen";
    public static final String VERSION = "1.0";
    public static float serverTickRate;
    public static boolean isReady;
    public static boolean isMCPMapped;
    public static String configDir = System.getProperty("user.home") + File.separator + ".zen";
    public static String username = "";

    private EventBus eventBus;
    private RotationHandler rotationHandler;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private HudManager hudManager;
    private LagManager lagManager;
    private TargetManager targetManager;
    private KillEffectManager killEffectManager;
    private int reconnectAttempts;

    public ZenClient() {
        if (instance == null) {
            instance = this;
            this.init();
        }
    }

    private void init() {
        try {
            username = System.getProperty("user.name", "Player");
            File dir = new File(configDir);
            if (!dir.exists() && !dir.mkdirs()) {
                logger.warn("Failed to create config directory at {}", configDir);
            }
            ProfileAvatar.load();
            mc = getMcInstance();
            this.eventBus = new EventBus();
            this.killEffectManager = new KillEffectManager();
            this.eventBus.register(this.killEffectManager);
            this.rotationHandler = new RotationHandler();
            this.eventBus.register(this.rotationHandler);
            this.moduleManager = new ModuleManager();
            this.hudManager = new HudManager();
            this.commandManager = new CommandManager();
            this.configManager = new ConfigManager();
            this.lagManager = new LagManager();
            this.targetManager = new TargetManager();
            this.eventBus.register(this.hudManager);
            this.eventBus.register(this.lagManager);
            this.eventBus.register(this.targetManager);
            this.eventBus.register(this);
            this.commandManager.initCommands();
            this.eventBus.register(new IntroAnimation());
            Bootstrap.init();
            registerPatches();
            if (PatchAgent.getInstrumentation() != null) {
                PatchAgent.installPatchesAndRetransform();
            } else {
                logger.warn("agent not attached. Launch with `./gradlew runClient` so the agent jvmArg is set.");
            }
            isReady = true;
            logger.info("{} v{} initialized.", CLIENT_NAME, VERSION);
        } catch (Throwable throwable) {
            logger.error(throwable.getMessage(), throwable);
        }
    }

    private boolean moduleInit = false;

    @EventTarget
    public void onTick(TickEvent e) {
        if (isReady() && !moduleInit) {
            moduleInit = true;
            this.moduleManager.initModules();
            this.configManager.loadAll();
        }
    }

    public static boolean isReady() {
        return instance != null
                && ZenClient.instance.eventBus != null
                && isReady
                && mc != null
                && mc.player != null
                && !username.isEmpty()
                && mc.player.tickCount > 5;
    }

    public void shutdown() {
        isReady = false;
        ProfileAvatar.save();
        if (this.configManager != null) {
            this.configManager.saveAll();
        }
    }

    public static void registerPatches() {
        PatchRegistry.register(MinecraftPatch.class);
        PatchRegistry.register(MultiPlayerGameModePatch.class);
        PatchRegistry.register(LocalPlayerPatch.class);
        PatchRegistry.register(AbstractClientPlayerPatch.class);
        PatchRegistry.register(LivingEntityPatch.class);
        PatchRegistry.register(EntityPatch.class);
        PatchRegistry.register(PlayerPatch.class);
        PatchRegistry.register(ClientLevelPatch.class);
        PatchRegistry.register(ConnectionPatch.class);
        PatchRegistry.register(PacketUtilsPatch.class);
        PatchRegistry.register(KeyboardHandlerPatch.class);
        PatchRegistry.register(KeyboardInputPatch.class);
        PatchRegistry.register(ChatScreenPatch.class);
        PatchRegistry.register(EntityRendererPatch.class);
        PatchRegistry.register(LevelRendererPatch.class);
        PatchRegistry.register(BlockPatch.class);
        PatchRegistry.register(GameRendererPatch.class);
        PatchRegistry.register(ItemInHandRendererPatch.class);
        PatchRegistry.register(ItemInHandLayerPatch.class);
        PatchRegistry.register(HumanoidModelPatch.class);
        PatchRegistry.register(LivingEntityRendererPatch.class);
        PatchRegistry.register(ItemPatch.class);
        PatchRegistry.register(PlayerTabOverlayPatch.class);
        PatchRegistry.register(FriendlyByteBufPatch.class);

        // Compatibility patch for Embeddium/Sodium's BlockOcclusionCache.
        // Always registered so the transformer can catch the class when it
        // first loads. We must NOT use Class.forName() here — that would
        // load the class before our transformer is installed, preventing
        // the patch from ever being applied.
        PatchRegistry.register(BlockOcclusionCachePatch.class);
    }

    public static Minecraft getMcInstance() {
        Minecraft minecraft = null;
        try {
            Class<?> clazz = Minecraft.class;
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() != clazz) continue;
                field.setAccessible(true);
                minecraft = (Minecraft) field.get(null);
                field.setAccessible(false);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return minecraft != null ? minecraft : Minecraft.getInstance();
    }

}
