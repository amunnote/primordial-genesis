package local.primordial.affix;

/** A boss has one special affix, separate from its ordinary affix count. */
public enum BossAffix {
    PRIDE("pride", "傲慢",0xFFDF7E), GREED("greed", "贪婪",0xF5A623), SLOTH("sloth", "懒惰",0x9294BA),
    ENVY("envy", "嫉妒",0x43C982), WRATH("wrath", "暴怒",0xE63248), GLUTTONY("gluttony", "暴食",0xBA9162), LUST("lust", "色欲",0xF66ABC);

    private final String id;
    private final String displayName;
    private final int color;
    BossAffix(String id, String displayName,int color) { this.id = id; this.displayName = displayName; this.color=color; }
    public String id() { return id; }
    /** Chinese name kept as the fallback for the lang key affix.primordial.<id> */
    public String displayName() { return displayName; }
    public net.minecraft.network.chat.MutableComponent label() { return net.minecraft.network.chat.Component.translatableWithFallback("affix.primordial." + id, displayName); }
    /** the name in brackets, in the affix colour: 【空间】 / [Space] */
    public net.minecraft.network.chat.MutableComponent tag() { return net.minecraft.network.chat.Component.translatable("affix.primordial.tag", label()).withStyle(style -> style.withColor(color)); }
    public int color() { return color; }
    public static BossAffix byId(String id) {
        for (BossAffix affix : values()) if (affix.id.equals(id)) return affix;
        return null;
    }
}
