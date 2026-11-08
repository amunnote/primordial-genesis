package local.primordial.affix;

/** The unambiguous level formulas; encounter selection rules are configured separately. */
public final class AffixScaling {
    private AffixScaling() {}
    public static int spaceChance(int level) { return cappedPercent(level, 1, 30); }
    public static int revivalHealthPercent(int level) { return cappedPercent(level, 2, 100); }
    public static int curseChance(int level) { return cappedPercent(level, 2, 100); }
    public static int summonChance(int level) { return cappedPercent(level, 2, 100); }
    public static int sealChance(int level) { return cappedPercent(level, 1, 30); }
    public static int reflectPercent(int level) { return cappedPercent(level, 1, 50); }
    public static int resistancePercent(int level) { return cappedPercent(level, 1, 30); }
    public static int regenerationPercent(int level) { return Math.min(10, ceilTenth(level)); }
    public static int potionTier(int level) { return Math.min(3, ceilTenth(level)); }
    public static int healingPercent(int level) { return Math.min(30,10+5*(Math.max(0,level)/10)); }
    public static int shatterChance(int level) { return cappedPercent(level, 1, 30); }
    public static int slownessTier(int level) { return Math.min(3,1+Math.max(0,level)/10); }
    private static int ceilTenth(int level) { return (int) ((Math.max(0L, level) + 9) / 10); }
    private static int cappedPercent(int level, int factor, int maximum) {
        return (int) Math.min(maximum, Math.max(0L, level) * factor);
    }
}
