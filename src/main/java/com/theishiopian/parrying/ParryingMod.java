package com.theishiopian.parrying;

import com.theishiopian.parrying.Config.Config;
import com.theishiopian.parrying.Entity.Render.RenderDagger;
import com.theishiopian.parrying.Entity.Render.RenderSpear;
import com.theishiopian.parrying.Handler.ClientEvents;
import com.theishiopian.parrying.Handler.CommonEvents;
import com.theishiopian.parrying.Network.DodgePacket;
import com.theishiopian.parrying.Network.LeftClickPacket;
import com.theishiopian.parrying.Network.SwingPacket;
import com.theishiopian.parrying.Recipes.EnabledCondition;
import com.theishiopian.parrying.Registration.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * This is the main mod class. There's not much to say about it.
 */
@Mod(ParryingMod.MOD_ID)
public class ParryingMod
{
    public static final String MOD_ID = "parrying";
    public static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation netName = new ResourceLocation(MOD_ID, "network");
    public static final SimpleChannel channel;
    private static final int VERSION = 4;//protocol version, bump whenever adding new network packets or changing existing ones

    static
    {
        channel = NetworkRegistry.ChannelBuilder.named(netName)
                .clientAcceptedVersions(s -> Objects.equals(s, String.valueOf(VERSION)))
                .serverAcceptedVersions(s -> Objects.equals(s, String.valueOf(VERSION)))
                .networkProtocolVersion(() -> String.valueOf(VERSION))
                .simpleChannel();

        channel.messageBuilder(LeftClickPacket.class, 1)
                .decoder(LeftClickPacket::fromBytes)
                .encoder(LeftClickPacket::toBytes)
                .consumer(LeftClickPacket::handle)
                .add();

        channel.messageBuilder(DodgePacket.class, 2)
                .decoder(DodgePacket::fromBytes)
                .encoder(DodgePacket::toBytes)
                .consumer(DodgePacket::handle)
                .add();

        channel.messageBuilder(SwingPacket.class, 3)
                .decoder(SwingPacket::fromBytes)
                .encoder(SwingPacket::toBytes)
                .consumer(SwingPacket::handle)
                .add();
    }

    public ParryingMod()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::OnAttackedEvent);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::OnPlayerAttackTarget);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::OnArrowImpact);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::OnHurtEvent);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::OnWorldTick);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::OnPlayerTick);

        ModTriggers.Init();
        ModParticles.PARTICLE_TYPES.register(bus);
        ModSoundEvents.SOUND_EVENTS.register(bus);
        ModEnchantments.ENCHANTMENTS.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModAttributes.ATTRIBUTES.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
        {
            bus.addListener(ClientEvents::OnRegisterParticlesEvent);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::ClientSetup);
        });

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::CommonSetup);
    }

    @OnlyIn(Dist.CLIENT)
    public void ClientSetup(FMLClientSetupEvent event)
    {
        if(Config.flailEnabled.get())ModItems.RegisterOverrides();

        MinecraftForge.EVENT_BUS.addListener(ClientEvents::OnLeftMouse);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::OnKeyPressed);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::OnClick);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::OnTooltip);
        //MinecraftForge.EVENT_BUS.addListener(ClientEvents::OnPlayerTick);

        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SPEAR.get(), RenderSpear::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.DAGGER.get(), RenderDagger::new);
    }

    public void CommonSetup(FMLCommonSetupEvent event)
    {
        //here, I am registering new crafting conditions
        //first I make a new EnabledCondition, and then I make a Serializer that is "inside" that object
        //you can get the enclosing object (EnabledCondition) via "EnabledCondition.this", at least locally
        CraftingHelper.register(new EnabledCondition("maces_enabled", Config.maceEnabled::get).new Serializer());
        CraftingHelper.register(new EnabledCondition("hammers_enabled", Config.hammerEnabled::get).new Serializer());
        CraftingHelper.register(new EnabledCondition("flails_enabled", Config.flailEnabled::get).new Serializer());
        CraftingHelper.register(new EnabledCondition("spears_enabled", Config.spearEnabled::get).new Serializer());
        CraftingHelper.register(new EnabledCondition("daggers_enabled", Config.daggerEnabled::get).new Serializer());
        CraftingHelper.register(new EnabledCondition("is_chainmail_craftable", Config.isChainmailCraftable::get).new Serializer());
    }
}