package us.maxpowa.apc;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@Mod(ApotheoticCreation.MOD_ID)
public class ApotheoticCreation
{
    public static final String MOD_ID = "apotheoticcreation";
    static final ResourceLocation RARITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "rarity");
    static final ResourceLocation AFFIX_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "affix");

    public ApotheoticCreation(IEventBus modEventBus) {
        modEventBus.addListener(this::registerHandler);
    }

    private void registerHandler(final RegisterEvent event) {
        var attributeRegistryKey = CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE.key();

        if (event.getRegistryKey() == attributeRegistryKey) {
            event.register(attributeRegistryKey, RARITY_ID, RarityAttribute.Type::new);
            event.register(attributeRegistryKey, AFFIX_ID, AffixAttribute.Type::new);
        }
    }

    public static class RarityAttribute implements ItemAttribute {
        public static final MapCodec<RarityAttribute> CODEC = LootRarity.CODEC.fieldOf("rarity")
                .xmap(RarityAttribute::new, attr -> attr.rarity);
        public static final StreamCodec<ByteBuf, RarityAttribute> STREAM_CODEC = RarityRegistry.INSTANCE.holderStreamCodec()
                .map(holder -> new RarityAttribute(holder.get()), attr -> RarityRegistry.INSTANCE.holder(attr.rarity));

        private final LootRarity rarity;

        public RarityAttribute(LootRarity rarity) {
            this.rarity = rarity;
        }

        @Override
        public boolean appliesTo(ItemStack stack, Level level) {
            DynamicHolder<LootRarity> itemRarity = AffixHelper.getRarity(stack);
            if (!itemRarity.isBound()) return false;
            return itemRarity.get() == this.rarity;
        }

        @Override
        public String getTranslationKey() {
            return "item_rarity";
        }

        @Override
        public Object[] getTranslationParameters() {
            if (this.rarity != null) {
                return new Object[]{
                        this.rarity.toComponent(),
                };
            }
            return new Object[]{};
        }

        @Override
        public ItemAttributeType getType() {
            return CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE.get(RARITY_ID);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RarityAttribute other)) return false;
            return Objects.equals(this.rarity, other.rarity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.rarity);
        }

        public static class Type implements ItemAttributeType {
            @Override
            public @NotNull ItemAttribute createAttribute() {
                return new RarityAttribute(null);
            }

            @Override
            public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
                DynamicHolder<LootRarity> itemRarity = AffixHelper.getRarity(stack);
                if (!itemRarity.isBound()) return Collections.emptyList();
                LootRarity rarity = itemRarity.get();
                return List.of(new RarityAttribute(rarity));
            }

            @Override
            public MapCodec<? extends ItemAttribute> codec() {
                return RarityAttribute.CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
                return RarityAttribute.STREAM_CODEC;
            }
        }
    }

    public static class AffixAttribute implements ItemAttribute {

        public static final MapCodec<AffixAttribute> CODEC = AffixRegistry.INSTANCE.holderCodec().fieldOf("affix")
                .xmap(AffixAttribute::new, attr -> attr.affix);
        public static final StreamCodec<ByteBuf, AffixAttribute> STREAM_CODEC = AffixRegistry.INSTANCE.holderStreamCodec()
                .map(AffixAttribute::new, attr -> attr.affix);

        private static final Set<String> HIDDEN_AFFIXES = Set.of("socket", "durable");

        private final DynamicHolder<Affix> affix;

        public AffixAttribute(DynamicHolder<Affix> affix) {
            this.affix = affix;
        }

        @Override
        public boolean appliesTo(ItemStack stack, Level level) {
            Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(stack);
            return affixes.containsKey(affix);
        }

        @Override
        public String getTranslationKey() {
            return "item_affix";
        }

        @Override
        public Object[] getTranslationParameters() {
            if (this.affix != null) {
                return new Object[]{
                    Component.translatable("affix." + this.affix.getId().toString()),
                };
            }
            return new Object[]{};
        }

        @Override
        public ItemAttributeType getType() {
            return CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE.get(AFFIX_ID);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof AffixAttribute other)) return false;
            return Objects.equals(this.affix, other.affix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.affix);
        }

        public static class Type implements ItemAttributeType {
            @Override
            public @NotNull ItemAttribute createAttribute() {
                return new AffixAttribute(null);
            }

            @Override
            public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
                return AffixHelper.getAffixes(stack).keySet().parallelStream()
                    .filter(entry -> entry.isBound() && !HIDDEN_AFFIXES.contains(entry.getId().getPath()))
                    .map(AffixAttribute::new)
                    .collect(Collectors.toList());
            }

            @Override
            public MapCodec<? extends ItemAttribute> codec() {
                return AffixAttribute.CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
                return AffixAttribute.STREAM_CODEC;
            }
        }
    }

}
