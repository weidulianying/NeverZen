package shit.zen.selfskin.provider;

import java.util.UUID;
import shit.zen.selfskin.SkinData;

public interface SkinProvider {
    SkinData getSkin(UUID uuid) throws Exception;
}
