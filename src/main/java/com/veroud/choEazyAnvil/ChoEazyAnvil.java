package com.veroud.choEazyAnvil;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ChoEazyAnvil extends JavaPlugin implements Listener {

    private int maxLevel;
    private final Set<Enchantment> scalableEnchants = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maxLevel = getConfig().getInt("max-enchant-level", 10);
        getServer().getPluginManager().registerEvents(this, this);

        // Define scalable enchants (modern API names)
        scalableEnchants.add(Enchantment.SHARPNESS);
        scalableEnchants.add(Enchantment.SMITE);
        scalableEnchants.add(Enchantment.BANE_OF_ARTHROPODS);

        scalableEnchants.add(Enchantment.PROTECTION);
        scalableEnchants.add(Enchantment.FIRE_PROTECTION);
        scalableEnchants.add(Enchantment.BLAST_PROTECTION);
        scalableEnchants.add(Enchantment.PROJECTILE_PROTECTION);

        scalableEnchants.add(Enchantment.EFFICIENCY);
        scalableEnchants.add(Enchantment.POWER);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);  // left slot
        ItemStack second = inv.getItem(1); // right slot
        if (first == null || second == null) return;

        // Clone the vanilla result so we can safely modify it
        ItemStack result = event.getResult();
        if (result == null) return;
        result = result.clone();

        Map<Enchantment, Integer> firstEnchants = first.getEnchantments();
        Map<Enchantment, Integer> secondEnchants = second.getEnchantments();

        // Handle enchanted books too
        if (first.getItemMeta() instanceof EnchantmentStorageMeta meta1) {
            firstEnchants = meta1.getStoredEnchants();
        }
        if (second.getItemMeta() instanceof EnchantmentStorageMeta meta2) {
            secondEnchants = meta2.getStoredEnchants();
        }

        int extraCost = 0;

        for (Map.Entry<Enchantment, Integer> entry : secondEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            int newLevel = level;

            if (firstEnchants.containsKey(ench)) {
                newLevel = Math.max(level, firstEnchants.get(ench));
                if (level == firstEnchants.get(ench)) {
                    newLevel++; // vanilla rule: equal levels combine +1
                }
            }

            if (scalableEnchants.contains(ench)) {
                if (newLevel > maxLevel) newLevel = maxLevel;

                // overwrite any vanilla-applied enchant
                result.removeEnchantment(ench);
                result.addUnsafeEnchantment(ench, newLevel);

                // Add extra XP cost for levels above vanilla
                if (newLevel > ench.getMaxLevel()) {
                    extraCost += (newLevel - ench.getMaxLevel())
                            * getConfig().getInt("extra-cost-per-level", 5);
                }

            } else {
                result.removeEnchantment(ench);
                result.addEnchantment(ench, Math.min(newLevel, ench.getMaxLevel()));
            }
        }

        // Apply cost adjustment
        int baseCost = inv.getRepairCost();
        inv.setRepairCost(baseCost + extraCost);

        // Push our corrected result back into the anvil
        event.setResult(result);
    }
}
