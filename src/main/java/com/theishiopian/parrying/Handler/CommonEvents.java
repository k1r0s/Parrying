package com.theishiopian.parrying.Handler;

import com.theishiopian.parrying.Config.Config;
import com.theishiopian.parrying.Items.APItem;
import com.theishiopian.parrying.Items.FlailItem;
import com.theishiopian.parrying.Mechanics.*;
import com.theishiopian.parrying.Registration.ModAttributes;
import com.theishiopian.parrying.Registration.ModEffects;
import com.theishiopian.parrying.Registration.ModEnchantments;
import com.theishiopian.parrying.Utility.ParryModUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.util.List;

public class CommonEvents
{
    static float pAmount = 0;//this is dumb
    static float strength = 0;//so is this
    //these static fields transfer information between layers of method calls, in between which this information is altered or destroyed. The only other way of ensuring access
    //is to restructure the logic of the minecraft combat system, which would cause innumerable problems for compatibility im sure.
    //Why do you do this to me mojang?

    public static void OnPlayerAttackTarget(AttackEntityEvent event)
    {
        strength = event.getPlayer().getAttackStrengthScale(0.5f);
    }

    public static void OnAttackedEvent(LivingAttackEvent event)
    {
        if(!event.getEntity().level.isClientSide)
        {
            LivingEntity entity = event.getEntityLiving();
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity ? (LivingEntity) event.getSource().getEntity() : null;
            Parrying.Parry(event);
            float amount = event.getAmount();

            if(event.getSource() instanceof IndirectEntityDamageSource && event.getSource().isProjectile())
            {
                IndirectEntityDamageSource src = (IndirectEntityDamageSource) event.getSource();

                Entity e = src.getDirectEntity();

                if(e instanceof  AbstractArrowEntity)
                {
                    pAmount = amount;

                    entity.invulnerableTime = 0;
                }
            }

            if(attacker != null)
            {
                APItem weapon = attacker.getMainHandItem().getItem() instanceof APItem ? (APItem) attacker.getMainHandItem().getItem() : null;

                if(weapon != null && ArmorPenetration.IsNotBypassing())
                {
                    //yes, the attribute is there, I put it there
                    //noinspection OptionalGetWithoutIsPresent
                    float ap = (float) weapon.getAttributeModifiers(EquipmentSlotType.MAINHAND, attacker.getMainHandItem()).get(ModAttributes.AP.get()).stream().findFirst().get().getAmount();
                    ArmorPenetration.DoAPDamage(amount, strength, ap, entity, attacker, weapon instanceof FlailItem, "bludgeoning.player");
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void OnArrowImpact(ProjectileImpactEvent.Arrow event)
    {
        if(!Deflection.Deflect(event))
        {
            Arrows.DoSonicArrow(event.getArrow());
            Arrows.DoBurningArrow(event.getArrow(), event.getRayTraceResult());
        }
    }

    public static void OnHurtEvent(LivingHurtEvent event)
    {
        LivingEntity entity = event.getEntityLiving();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity ? (LivingEntity) event.getSource().getEntity() : null;

        if(entity != null)
        {
            if(Config.apPiercing.get() && ArmorPenetration.IsNotBypassing())
            {
                if(event.getSource() instanceof IndirectEntityDamageSource && event.getSource().isProjectile())
                {
                    IndirectEntityDamageSource src = (IndirectEntityDamageSource) event.getSource();

                    Entity e = src.getDirectEntity();

                    if(e instanceof  AbstractArrowEntity)
                    {
                        AbstractArrowEntity arrow = (AbstractArrowEntity)e;

                        int pLevel = arrow.getPierceLevel();

                        if(pLevel > 0)
                        {
                            //it actually will bypass the shield, this is just to trick the helper method
                            ArmorPenetration.DoAPDamage(pAmount,strength, 0.2f * pLevel, entity, attacker, false, "piercing.player");
                            event.setAmount(0);//prevent extra damage

                            //NOTE: the backstab still applies with this because the damage is applied separately inside DoAPDamage
                            //hence the need for a check if the system is doing AP
                        }

                        if(arrow instanceof ArrowEntity && Config.pickyPotionArrows.get())
                        {
                            List<EffectInstance> effects = ((ArrowEntity)arrow).potion.getEffects();

                            if(effects.size() > 0)
                            {
                                boolean hasHarm = false;
                                for (EffectInstance i : effects)
                                {
                                    if(!i.getEffect().isBeneficial())
                                    {
                                        hasHarm = true;
                                        break;
                                    }
                                }

                                if(!hasHarm)
                                {
                                    event.setAmount(0);
                                }
                            }
                        }
                    }
                }
            }

            if((!(entity instanceof PlayerEntity)) && entity.hasEffect(ModEffects.STUNNED.get()))
            {
                event.setAmount(event.getAmount() * 1.5f);
            }

            if(attacker != null && Config.cripplingEnchantEnabled.get() && EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CRIPPLING.get(), attacker) > 0)
            {
                float chance = ParryModUtil.random.nextFloat();

                if(chance <= 0.25)
                {
                    entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, (int) Math.floor(chance * 20 * 4) + 20));
                }
            }

            Backstab.DoBackstab(event, entity);
        }
    }

    public static void OnWorldTick(TickEvent.WorldTickEvent event)
    {
        if(event.world.isClientSide)return;

        Dodging.dodgeCooldown.replaceAll((k, v) -> v - 1);
        Dodging.dodgeCooldown.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    public static void OnPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if(!event.player.level.isClientSide())
        {
            if(!ParryModUtil.IsWeapon(event.player.getMainHandItem()) && ParryModUtil.IsWeapon(event.player.getOffhandItem()))
            {
                DualWielding.dualWielders.remove(event.player.getUUID());
            }
        }
    }
}