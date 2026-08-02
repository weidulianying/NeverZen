package shit.zen.gui.neverloseGUI.design;

import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;

/** Typography scale — every font decision references a token here. */
public final class Typography {
    private Typography() {}

    public static final FontRenderer H1   = FontPresets.axiformaBold(19f);      // page heading
    public static final FontRenderer H2   = FontPresets.axiformaBold(17f);      // card heading
    public static final FontRenderer BODY = FontPresets.axiformaRegular(15f);   // primary UI text
    public static final FontRenderer SMALL= FontPresets.axiformaRegular(13f);   // controls and metadata
    public static final FontRenderer TINY = FontPresets.axiformaRegular(12f);   // compact labels
    public static final FontRenderer ICON = FontPresets.materialIcons(16f);     // navigation icons
    public static final FontRenderer ICON_SMALL = FontPresets.materialIcons(15f); // compact action icons
    public static final FontRenderer LOGO = FontPresets.museoSans(18f);         // branding
    public static final FontRenderer MONO = FontPresets.axiformaRegular(14f);   // values / codes
}
