package com.theishiopian.parrying.Enchantment;

import com.theishiopian.parrying.Config.Config;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BashingEnchantment extends Enchantment
{
    public  BashingEnchantment()
    {
        super(Rarity.RARE, EnchantmentType.BREAKABLE, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND, EquipmentSlotType.OFFHAND});
    }

    public int getMinCost(int in)
    {
        return 10 + in * 5;
    }

    public int getMaxCost(int in) {
        return this.getMinCost(in) + 20;
    }

    public int getMaxLevel() {
        return 3;
    }
    
    public boolean checkCompatibility(@NotNull Enchantment toCheck)
    {
        return true;
    }

    public boolean canEnchant(ItemStack toEnchant)
    {
        return toEnchant.isShield(null) && Config.bashingEnchantEnabled.get();
    }

    public boolean canApplyAtEnchantingTable(@NotNull ItemStack toEnchant)
    {
        return canEnchant(toEnchant);
    }
}