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
    private int extraCostPerLevel;
    private final Set<Enchantment> scalableEnchants = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maxLevel = getConfig().getInt("max-enchant-level", 10);
        extraCostPerLevel = getConfig().getInt("extra-cost-per-level", 5);
        getServer().getPluginManager().registerEvents(this, this);

        // Scalable enchants
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
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);
        if (first == null || second == null) return;

        ItemStack result = event.getResult();
        if (result == null) return;
        result = result.clone();

        // Read enchants from items or books
        Map<Enchantment, Integer> firstEnchants = (first.getItemMeta() instanceof EnchantmentStorageMeta meta1)
                ? meta1.getStoredEnchants() : first.getEnchantments();

        Map<Enchantment, Integer> secondEnchants = (second.getItemMeta() instanceof EnchantmentStorageMeta meta2)
                ? meta2.getStoredEnchants() : second.getEnchantments();

        int extraCost = 0;

        for (Map.Entry<Enchantment, Integer> entry : secondEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            int newLevel = level;

            if (firstEnchants.containsKey(ench)) {
                newLevel = Math.max(level, firstEnchants.get(ench));
                if (level == firstEnchants.get(ench)) newLevel++; // vanilla combine rule
            }

            if (scalableEnchants.contains(ench)) {
                if (newLevel > maxLevel) newLevel = maxLevel;

                result.removeEnchantment(ench);
                result.addUnsafeEnchantment(ench, newLevel);

                if (newLevel > ench.getMaxLevel()) {
                    extraCost += (newLevel - ench.getMaxLevel()) * extraCostPerLevel;
                }

            } else {
                result.removeEnchantment(ench);
                result.addEnchantment(ench, Math.min(newLevel, ench.getMaxLevel()));
            }
        }

        // Apply repair cost WITHOUT any cap to lift "Too Expensive!"
        int baseCost = inv.getRepairCost();
        inv.setRepairCost(baseCost + extraCost);

        // Set the result
        event.setResult(result);
    }
}
