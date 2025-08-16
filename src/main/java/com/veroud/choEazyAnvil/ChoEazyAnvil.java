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

        // Define scalable enchants
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

        ItemStack result = first.clone();

        // Get all enchantments from both items
        Map<Enchantment, Integer> firstEnchants = (first.getItemMeta() instanceof EnchantmentStorageMeta meta1)
                ? meta1.getStoredEnchants() : first.getEnchantments();

        Map<Enchantment, Integer> secondEnchants = (second.getItemMeta() instanceof EnchantmentStorageMeta meta2)
                ? meta2.getStoredEnchants() : second.getEnchantments();

        int extraCost = 0;

        // Add all first item enchants to result
        for (Map.Entry<Enchantment, Integer> entry : firstEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            result.removeEnchantment(ench);
            result.addUnsafeEnchantment(ench, level);
        }

        // Merge second item enchants
        for (Map.Entry<Enchantment, Integer> entry : secondEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int secondLevel = entry.getValue();
            int newLevel = secondLevel;

            if (firstEnchants.containsKey(ench)) {
                int firstLevel = firstEnchants.get(ench);
                newLevel = Math.max(firstLevel, secondLevel);
                if (firstLevel == secondLevel) newLevel++; // vanilla combine rule
            }

            if (scalableEnchants.contains(ench) && newLevel > maxLevel) newLevel = maxLevel;

            result.removeEnchantment(ench);
            result.addUnsafeEnchantment(ench, newLevel);

            // Extra cost for above-vanilla levels
            if (newLevel > ench.getMaxLevel()) {
                extraCost += (newLevel - ench.getMaxLevel()) * extraCostPerLevel;
            }
        }

        int baseCost = inv.getRepairCost();
        inv.setRepairCost(baseCost + extraCost);

        event.setResult(result);
    }
}
