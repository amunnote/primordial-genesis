package local.primordial.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class InventoryProtection {
    private static final ThreadLocal<Integer> USER_EDIT = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> USER_DROP = ThreadLocal.withInitial(() -> false);
    private InventoryProtection() {}
    public static boolean locked(Entity wearer) {
        return wearer != null && !wearer.level().isClientSide && USER_EDIT.get() == 0 && Protection.active(wearer);
    }
    public static void userEdit(Runnable action) {
        int before = USER_EDIT.get(); USER_EDIT.set(before + 1);
        try { action.run(); } finally { if (before == 0) USER_EDIT.remove(); else USER_EDIT.set(before); }
    }
    public static boolean removes(ItemStack oldStack, ItemStack newStack) {
        return !oldStack.isEmpty() && (!ItemStack.isSameItemSameTags(oldStack, newStack)
            || newStack.getCount() < oldStack.getCount());
    }
    public static boolean allowsDrop() { return USER_DROP.get(); }
    public static boolean allowsEdit() { return USER_EDIT.get()>0; }
    public static <T> T userDrop(java.util.function.Supplier<T> action) {
        boolean beforeDrop = USER_DROP.get(); int beforeEdit = USER_EDIT.get();
        USER_DROP.set(true); USER_EDIT.set(beforeEdit + 1);
        try { return action.get(); } finally {
            if (beforeDrop) USER_DROP.set(true); else USER_DROP.remove();
            if (beforeEdit == 0) USER_EDIT.remove(); else USER_EDIT.set(beforeEdit);
        }
    }
}
