package local.primordial.affix;

/** Stable saved identifiers for ordinary affixes. */
public enum MobAffix {
    SPACE("space", "空间",0x9B8CFF), TIME("time", "时间",0xFFD166), ARMOR_PIERCE("armor_pierce", "破甲",0xFF975C),
    CAUSALITY("causality", "因果",0xF4A7FF), REGENERATION("regeneration", "再生",0x63E68B), CURSE("curse", "诅咒",0xBC68DB),
    SUMMON("summon", "召唤",0xD4E86A), SEAL("seal", "封印",0xE0B080), MAGIC("magic", "魔法",0x62BCFF), REFLECTION("reflection", "反伤",0xFF7195),
    DAMAGE_CAP("damage_cap", "限伤",0xC6D2E0), STRENGTH("strength", "力量",0xFF5555), RESISTANCE("resistance", "抗性",0xEBCB69),
    SPLIT("split", "分裂",0x50DCCA), PHYSICAL_RESISTANCE("physical_resistance", "物抗",0xA8BBCD), MAGIC_RESISTANCE("magic_resistance", "非物抗",0x789AFF),
    THUNDER("thunder", "雷霆",0xFBE276), SLOWNESS("slowness", "迟缓",0xEE81BB), SHARED("shared", "共享",0x8AE1D3), HEALING("healing", "治疗",0x77E79A),
    MALICE("malice", "恶意",0xB63CE8), DESATURATION("desaturation", "失色",0xB5B5B5), VOID("void", "虚无",0x604A9D), RUIN("ruin", "破灭",0xDF4545),
    SHATTER("shatter", "击破",0xD9A066), UNDYING("undying", "永生",0xFFE48A), BLOOD_PACT("blood_pact", "血契",0xC0283A),
    GROWTH("growth", "生长",0x8BCB4A), ENLIGHTENMENT("enlightenment", "启迪",0x7FE0FF), BEFUDDLEMENT("befuddlement", "降智",0xC88CE0),
    AMPLIFY("amplify", "增幅",0xFF9F43), MASTERY("mastery", "精通",0x5FA8E8);

    private final String id;
    private final String displayName;
    private final int color;
    MobAffix(String id, String displayName,int color) { this.id = id; this.displayName = displayName; this.color=color; }
    public String id() { return id; }
    /** Chinese name kept as the fallback for the lang key affix.primordial.<id> */
    public String displayName() { return displayName; }
    public net.minecraft.network.chat.MutableComponent label() { return net.minecraft.network.chat.Component.translatableWithFallback("affix.primordial." + id, displayName); }
    /** the name in brackets, in the affix colour: 【空间】 / [Space] */
    public net.minecraft.network.chat.MutableComponent tag() { return net.minecraft.network.chat.Component.translatable("affix.primordial.tag", label()).withStyle(style -> style.withColor(color)); }
    public int color() { return color; }
    public static MobAffix byId(String id) {
        for (MobAffix affix : values()) if (affix.id.equals(id)) return affix;
        return null;
    }
}
