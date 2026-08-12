package slimeknights.mantle.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.block.GaugeBlock;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.mantle.client.model.FallbackModelLoader;
import slimeknights.mantle.client.model.NBTKeyModel;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.TextureColorHelper;
import slimeknights.mantle.client.model.connected.ConnectedModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.fluid.texture.ClientTextureFluidType;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.registration.MantleRegistrations;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.util.CapabilityHelper;
import slimeknights.mantle.util.OffhandCooldownTracker;
import slimeknights.mantle.util.RegistryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client event handlers. Deliberately carries no {@code bus} argument: NeoForge deprecated it and routes each listener
 * by whether its event implements {@code IModBusEvent}, so the mod bus and game bus handlers below can share a class.
 */
@EventBusSubscriber(modid = Mantle.modId, value = Dist.CLIENT)
public class ClientEvents {
  /*
   * Vanilla's attack indicator art moved out of the single gui/icons.png sheet and into the GUI sprite atlas, one
   * sprite per element. Gui declares these ids privately, so they are repeated here rather than reached for.
   */
  private static final ResourceLocation CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_background");
  private static final ResourceLocation CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
  private static final ResourceLocation HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_attack_indicator_background");
  private static final ResourceLocation HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_attack_indicator_progress");

  /** Called on construct to initiatlize things that need early entry */
  public static void onConstruct() {}

  @SuppressWarnings("ConstantConditions")
  @SubscribeEvent
  static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
    if (MantleRegistrations.SIGN != null) {
      event.registerBlockEntityRenderer(MantleRegistrations.SIGN.get(), SignRenderer::new);
    }
    if (MantleRegistrations.HANGING_SIGN != null) {
      event.registerBlockEntityRenderer(MantleRegistrations.HANGING_SIGN.get(), HangingSignRenderer::new);
    }
  }

  @SuppressWarnings("removal")
  @SubscribeEvent
  static void registerListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(ModelHelper.LISTENER);
    event.registerReloadListener(new BookLoader());
    ResourceColorManager.init(event);
    FluidTooltipHandler.init(event);
    FluidTextureManager.init(event);
    event.registerReloadListener(FluidCuboid.REGISTRY);
    event.registerReloadListener(RenderItem.REGISTRY);
    event.registerReloadListener(RenderItem.STATE_REGISTRY);
    event.registerReloadListener(TextureColorHelper.RELOAD_LISTENER);
  }

  @SubscribeEvent
  static void clientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> RegistrationHelper.forEachWoodType(Sheets::addWoodType));
    BookLoader.registerBook(Mantle.getResource("test"), new FileRepository(Mantle.getResource("books/test")));
    // TODO(M12-command): MantleClientCommand.init returns with command.client
  }

  @SubscribeEvent
  static void registerModelLoaders(RegisterGeometryLoaders event) {
    // 1.21 takes a full ResourceLocation rather than a path implicitly namespaced to the active mod
    // standard models - useful in resource packs for any model
    event.register(Mantle.getResource("connected"), ConnectedModel.LOADER);
    event.register(Mantle.getResource("item_layer"), MantleItemLayerModel.LOADER);
    event.register(Mantle.getResource("colored_block"), ColoredBlockModel.LOADER);
    event.register(Mantle.getResource("fallback"), FallbackModelLoader.INSTANCE);

    // NBT dynamic models - require specific data defined in the block/item to use
    event.register(Mantle.getResource("nbt_key"), NBTKeyModel.LOADER);
    event.register(Mantle.getResource("retextured"), RetexturedModel.LOADER);
  }

  @SubscribeEvent
  static void registerGuiLayers(RegisterGuiLayersEvent event) {
    ExtraHeartRenderHandler.registerGuiLayers(event);
  }

  @SubscribeEvent
  static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    ClientTextureFluidType.registerExtensions(event);
  }

  /**
   * Draws the offhand cooldown as an attack indicator, after whichever vanilla layer the indicator style calls for.
   * Registered on the game bus; the 1.20 shape was two hand rolled {@code addListener} calls made from common setup,
   * which is unnecessary now that a single {@code @EventBusSubscriber} class can hold both buses' listeners.
   */
  @SubscribeEvent
  static void renderOffhandAttackIndicator(RenderGuiLayerEvent.Post event) {
    // must have a player, not be in spectator, and have the indicator enabled
    Minecraft minecraft = Minecraft.getInstance();
    Options settings = minecraft.options;
    AttackIndicatorStatus indicator = settings.attackIndicator().get();
    if (minecraft.player == null || minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR || indicator == AttackIndicatorStatus.OFF) {
      return;
    }

    // only care about hotbar and crosshair
    ResourceLocation layer = event.getName();
    // will be true for hotbar, false for crosshair
    boolean isHotbar = VanillaGuiLayers.HOTBAR.equals(layer);
    if (!isHotbar && !VanillaGuiLayers.CROSSHAIR.equals(layer)) {
      return;
    }

    // fetch the current cooldown
    OffhandCooldownTracker tracker = OffhandCooldownTracker.get(minecraft.player);
    if (tracker == null) {
      return;
    }
    float cooldown = tracker.getCooldown();
    if (cooldown >= 1.0f) {
      return;
    }

    // show attack indicator
    GuiGraphics graphics = event.getGuiGraphics();
    switch (indicator) {
      case CROSSHAIR:
        if (!isHotbar && minecraft.options.getCameraType().isFirstPerson()) {
          // Options#renderDebug moved onto DebugScreenOverlay, which folds the hideGui check into showDebugScreen
          if (!minecraft.getDebugOverlay().showDebugScreen() || minecraft.player.isReducedDebugInfo() || settings.reducedDebugInfo().get()) {
            // mostly cloned from vanilla attack indicator
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
            // integer division makes this a pain to line up, there might be a simplier version of this formula but I cannot think of one
            int y = (scaledHeight / 2) - 14 + (2 * (scaledHeight % 2));
            int x = minecraft.getWindow().getGuiScaledWidth() / 2 - 8;
            int width = (int)(cooldown * 17.0F);
            graphics.blitSprite(CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE, x, y, 16, 4);
            graphics.blitSprite(CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE, 16, 4, 0, 0, x, y, width, 4);
            RenderSystem.defaultBlendFunc();
          }
        }
        break;
      case HOTBAR:
        if (isHotbar && minecraft.cameraEntity == minecraft.player) {
          int centerWidth = minecraft.getWindow().getGuiScaledWidth() / 2;
          int y = minecraft.getWindow().getGuiScaledHeight() - 20;
          int x;
          // opposite of the vanilla hand location, extra bit to offset past the offhand slot
          if (minecraft.player.getMainArm() == HumanoidArm.RIGHT) {
            x = centerWidth - 91 - 22 - 32;
          } else {
            x = centerWidth + 91 + 6 + 32;
          }
          int l1 = (int)(cooldown * 19.0F);
          RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
          RenderSystem.enableBlend();
          graphics.blitSprite(HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE, x, y, 18, 18);
          graphics.blitSprite(HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE, 18, 18, 0, 18 - l1, x, y + 18 - l1, 18, l1);
          RenderSystem.disableBlend();
        }
        break;
    }
  }



  /** Renders the tooltip when targeting the gauge block */
  @SubscribeEvent
  static void renderGaugeTooltip(RenderGuiLayerEvent.Post event) {
    if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
      return;
    }
    // must not be in a screen, though chat is fine
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen != null && minecraft.screen.getClass() != ChatScreen.class) {
      return;
    }
    // must have a hit result
    if (minecraft.level == null || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
      return;
    }
    BlockHitResult blockHit = (BlockHitResult) minecraft.hitResult;
    BlockPos pos = blockHit.getBlockPos();

    // must be targeting a gauge
    BlockState targeted = minecraft.level.getBlockState(blockHit.getBlockPos());
    if (!targeted.is(MantleTags.Blocks.GAUGES)) {
      return;
    }
    BlockEntity gaugeContainer;
    Direction side;
    if (targeted.is(MantleTags.Blocks.ATTACHED_GAUGES)) {
      side = targeted.getValue(BlockStateProperties.FACING);
      gaugeContainer = minecraft.level.getBlockEntity(pos.relative(side.getOpposite()));
    } else {
      side = blockHit.getDirection();
      gaugeContainer = minecraft.level.getBlockEntity(pos);
    }
    // must have a block entity behind the gauge that is not blacklisted
    if (gaugeContainer == null || RegistryHelper.contains(BuiltInRegistries.BLOCK_ENTITY_TYPE, MantleTags.BlockEntities.GAUGE_BLACKLIST, gaugeContainer.getType())) {
      return;
    }
    // block entity must have a fluid handler
    IFluidHandler handler = CapabilityHelper.fluidHandler(gaugeContainer, side);
    if (handler == null || handler.getTanks() <= 0) {
      return;
    }
    // if the fluid is empty, just render the capacity
    FluidStack fluid = handler.getFluidInTank(0);
    List<Component> tooltip;
    if (fluid.isEmpty()) {
      tooltip = List.of(GaugeBlock.formatCapacity(handler.getTankCapacity(0)));
    } else if (RegistryHelper.contains(BuiltInRegistries.BLOCK_ENTITY_TYPE, MantleTags.BlockEntities.HIDES_GAUGE_AMOUNT, gaugeContainer.getType())) {
      // in the tag, don't show capacity
      ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
      tooltip = new ArrayList<>(3);
      tooltip.add(fluid.getDisplayName());
      FluidTooltipHandler.appendAdvanced(id, tooltip);
      tooltip.add(GaugeBlock.formatCapacity(handler.getTankCapacity(0)).withStyle(ChatFormatting.GRAY));
      tooltip.add(FluidTooltipHandler.formatModName(id));
    } else {
      // render full fluid tooltip
      tooltip = FluidTooltipHandler.getFluidTooltip(fluid);
    }

    int x = minecraft.getWindow().getGuiScaledWidth() / 2;
    int y = minecraft.getWindow().getGuiScaledHeight() / 2;
    event.getGuiGraphics().renderTooltip(minecraft.font, tooltip, Optional.empty(), x, y);
  }
}
