package com.theishiopian.parrying.Utility;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * This class contains misc methods that I have found to be useful in multiple places.
 * If a method is useful in several unrelated places, it goes in here.
 */
public class ParryModUtil
{
    public static final Random random = new Random();

    public static boolean PlayerCritical(PlayerEntity player, Entity target, float cacheStrength)
    {
        boolean attackPowerFull = cacheStrength > 0.9f;
        boolean hasFallen = player.fallDistance > 0;
        boolean inAir = !player.isOnGround();
        boolean notClimbing = !player.onClimbable();
        boolean notWet = !player.isInWater();
        boolean notBlind = !player.hasEffect(Effects.BLINDNESS);
        boolean notRiding = !player.isPassenger();
        boolean targetValid = target instanceof LivingEntity;
        boolean walking = !player.isSprinting();

        return attackPowerFull && hasFallen && inAir && notClimbing && notWet && notBlind && notRiding && targetValid && walking;
    }

    public static boolean IsWeapon(ItemStack stack)
    {
        return stack.getAttributeModifiers(EquipmentSlotType.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
    }

    @Nullable
    public static EntityRayTraceResult GetAttackTargetWithRange(@Nullable ItemStack toAttackWith, LivingEntity toAttackFrom)
    {
        float range = 2.5f;

        if(toAttackWith != null)
        {
            boolean hasRange = toAttackWith.getAttributeModifiers(EquipmentSlotType.MAINHAND).containsKey(ForgeMod.REACH_DISTANCE.get());
            if(hasRange)
            {
                range += toAttackWith.getAttributeModifiers(EquipmentSlotType.MAINHAND).get(ForgeMod.REACH_DISTANCE.get()).stream().findFirst().get().getAmount();
            }
        }

        Vector3d eyePos = toAttackFrom.getEyePosition(1);
        Vector3d lookVector = toAttackFrom.getViewVector(1.0F);
        Vector3d projection = eyePos.add(lookVector.x * range, lookVector.y * range, lookVector.z * range);
        AxisAlignedBB axisalignedbb = toAttackFrom.getBoundingBox().expandTowards(lookVector.scale(range)).inflate(1.0D, 1.0D, 1.0D);
        EntityRayTraceResult potentialTarget = ProjectileHelper.getEntityHitResult(toAttackFrom.level, toAttackFrom, eyePos, projection, axisalignedbb, ((entity) -> !entity.isSpectator() && entity.isPickable()));

        if(potentialTarget != null)
        {
            boolean unobstructed = toAttackFrom.canSee(potentialTarget.getEntity());

            if(unobstructed)return potentialTarget;
        }

        return null;
    }
}