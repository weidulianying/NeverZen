package shit.zen.selfskin.provider;

import java.util.UUID;
import shit.zen.selfskin.SkinData;

public final class UrlProvider implements SkinProvider {
    private final String url;
    private final String capeUrl;

    public UrlProvider(String url, String capeUrl) {
        this.url = url;
        this.capeUrl = capeUrl;
    }

    @Override
    public SkinData getSkin(UUID uuid) {
        return new SkinData(YggdrasilProvider.checkedHttpUri(url),
                capeUrl == null || capeUrl.isBlank() ? null : YggdrasilProvider.checkedHttpUri(capeUrl),
                "default");
    }
}
