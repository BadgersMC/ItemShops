package dev.enthusia.itemshops.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;
import java.util.stream.Collectors;

public final class ItemUtils {

    private ItemUtils(){}

    public static String colored(String s){ return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    public static String niceName(ItemStack is){
        if (is == null) return "Nothing";
        return niceName(is.getType());
    }
    public static String niceName(Material m){
        String n = m.name().toLowerCase(Locale.ROOT).replace('_',' ');
        String[] parts = n.split(" ");
        for (int i=0;i<parts.length;i++){
            if (parts[i].isEmpty()) continue;
            parts[i] = Character.toUpperCase(parts[i].charAt(0)) + parts[i].substring(1);
        }
        return String.join(" ", parts);
    }

    public static Material matchMaterial(String query){
        if (query == null) return null;
        String q = query.trim().toUpperCase(Locale.ROOT).replace(' ','_');
        try { return Material.valueOf(q); } catch (Exception ignored) { return null; }
    }

    public static boolean matchesSimilar(ItemStack a, ItemStack b, boolean exactMeta) {
        if (a == null || b == null || a.getType().isAir() || b.getType().isAir()) return false;
        if (exactMeta) {
            ItemStack ac = a.clone(); ac.setAmount(1);
            ItemStack bc = b.clone(); bc.setAmount(1);
            return ac.isSimilar(bc);
        }
        return a.isSimilar(b);
    }

    public static int countSimilar(Inventory inv, ItemStack template){
        return countSimilar(inv, template, false);
    }

    public static int countSimilar(Inventory inv, ItemStack template, boolean exactMeta){
        if (template == null) return 0;
        int c = 0;
        for (ItemStack s : inv.getStorageContents()){
            if (s == null || s.getType().isAir()) continue;
            if (matchesSimilar(s, template, exactMeta)) c += s.getAmount();
        }
        return c;
    }

    public static boolean canFit(Inventory inv, ItemStack template, int amount){
        return canFit(inv, template, amount, false);
    }

    public static boolean canFit(Inventory inv, ItemStack template, int amount, boolean exactMeta){
        if (template == null || template.getType().isAir() || amount <= 0) return false;
        int max = template.getMaxStackSize();
        int free = 0;
        for (ItemStack cur : inv.getStorageContents()){
            if (cur == null || cur.getType().isAir()) { free += max; continue; }
            if (matchesSimilar(cur, template, exactMeta)) free += (max - cur.getAmount());
        }
        return free >= amount;
    }

    public static int capacityTrades(Inventory inv, ItemStack template, int amount){
        return capacityTrades(inv, template, amount, false);
    }

    public static int capacityTrades(Inventory inv, ItemStack template, int amount, boolean exactMeta){
        if (template == null || template.getType().isAir() || amount <= 0) return 0;
        int max = template.getMaxStackSize();
        int free = 0;
        for (ItemStack cur : inv.getStorageContents()){
            if (cur == null || cur.getType().isAir()) { free += max; continue; }
            if (matchesSimilar(cur, template, exactMeta)) free += (max - cur.getAmount());
        }
        return Math.min(free, amount);
    }

    public static final class RemoveResult {
        private final int removed;
        private final List<ItemStack> stacks;

        public RemoveResult(int removed, List<ItemStack> stacks) {
            this.removed = removed;
            this.stacks = stacks;
        }

        public int removed() { return removed; }
        public List<ItemStack> stacks() { return stacks; }
        public boolean isComplete(int expected) { return removed >= expected; }
        public boolean isEmpty() { return removed <= 0; }
    }

    public static RemoveResult removeSimilar(Inventory inv, ItemStack template, int amount){
        return removeSimilar(inv, template, amount, false);
    }

    public static RemoveResult removeSimilar(Inventory inv, ItemStack template, int amount, boolean exactMeta){
        List<ItemStack> removed = new ArrayList<>();
        if (template == null || template.getType().isAir() || amount <= 0) {
            return new RemoveResult(0, removed);
        }
        int left = amount;
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack cur = contents[i];
            if (cur == null || cur.getType().isAir()) continue;
            if (!matchesSimilar(cur, template, exactMeta)) continue;
            int take = Math.min(left, cur.getAmount());
            ItemStack copy = cur.clone(); copy.setAmount(take);
            removed.add(copy);
            cur.setAmount(cur.getAmount() - take);
            if (cur.getAmount() <= 0) inv.setItem(i, null);
            else inv.setItem(i, cur);
            left -= take;
        }
        return new RemoveResult(amount - left, removed);
    }

    public static boolean removeSimilarVerified(Inventory inv, ItemStack template, int amount, boolean exactMeta) {
        return removeSimilar(inv, template, amount, exactMeta).isComplete(amount);
    }

    public static void rollbackRemove(Inventory inv, ItemStack template, List<ItemStack> removed){
        for (ItemStack r : removed) addExact(inv, r, r.getAmount());
    }

    public static void addExact(Inventory inv, ItemStack template, int amount){
        addExact(inv, template, amount, false);
    }

    public static void addExact(Inventory inv, ItemStack template, int amount, boolean exactMeta){
        if (template == null || template.getType().isAir() || amount <= 0) return;
        int max = template.getMaxStackSize();
        int left = amount;

        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack cur = contents[i];
            if (cur == null || cur.getType().isAir()) continue;
            if (!matchesSimilar(cur, template, exactMeta)) continue;
            int can = max - cur.getAmount();
            if (can <= 0) continue;
            int add = Math.min(can, left);
            cur.setAmount(cur.getAmount() + add);
            inv.setItem(i, cur);
            left -= add;
        }

        while (left > 0){
            int put = Math.min(left, max);
            ItemStack clone = template.clone(); clone.setAmount(put);
            inv.addItem(clone);
            left -= put;
        }
    }

    public static boolean isShulker(ItemStack stack){
        return stack != null
                && stack.hasItemMeta()
                && stack.getItemMeta() instanceof BlockStateMeta bsm
                && bsm.getBlockState() instanceof ShulkerBox;
    }

    public static List<ItemStack> getShulkerContents(ItemStack shulker){
        if (!isShulker(shulker)) return List.of();
        BlockStateMeta bsm = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox sb = (ShulkerBox) bsm.getBlockState();
        ItemStack[] arr = sb.getInventory().getContents();
        List<ItemStack> out = new ArrayList<>();
        if (arr != null) {
            for (ItemStack it : arr) {
                if (it == null || it.getType().isAir()) continue;
                out.add(it.clone());
            }
        }
        return out;
    }

    public static boolean shulkerContains(ItemStack shulker, Material mat){
        if (mat == null) return false;
        for (ItemStack it : getShulkerContents(shulker)) {
            if (it.getType() == mat) return true;
        }
        return false;
    }

    public static List<String> summarizeContents(ItemStack shulker, int maxLines){
        Map<Material,Integer> counts = new LinkedHashMap<>();
        for (ItemStack it : getShulkerContents(shulker)) {
            counts.merge(it.getType(), it.getAmount(), Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        int i = 0;
        for (var e : counts.entrySet()){
            if (i++ >= maxLines) break;
            lines.add("&7- &f" + e.getValue() + "x " + niceName(e.getKey()));
        }
        int remaining = counts.size() - Math.min(maxLines, counts.size());
        if (remaining > 0) lines.add("&8... +" + remaining + " more");
        if (lines.isEmpty()) lines.add("&8(Empty)");
        return lines;
    }

    public static boolean isSameItem(ItemStack a, ItemStack b){
        if (a == null || b == null) return false;
        ItemStack ac = a.clone(); ac.setAmount(1);
        ItemStack bc = b.clone(); bc.setAmount(1);
        return ac.isSimilar(bc);
    }
}