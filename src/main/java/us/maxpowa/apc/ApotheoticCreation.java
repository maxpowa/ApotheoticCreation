package us.maxpowa.apc;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@Mod(ApotheoticCreation.MOD_ID)
public class ApotheoticCreation
{
    public static final String MOD_ID = "apotheoticcreation";
    @SuppressWarnings("removal")
    static final ResourceLocation RARITY_ID = new ResourceLocation(MOD_ID, "rarity");
    @SuppressWarnings("removal")
    static final ResourceLocation AFFIX_ID = new ResourceLocation(MOD_ID, "affix");

    public ApotheoticCreation() {
        //noinspection removal
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerHandler);
    }

    private void registerHandler(final RegisterEvent event) {
        var attributeRegistryKey = CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE.key();

        if (event.getRegistryKey() == attributeRegistryKey) {
            event.register(attributeRegistryKey, RARITY_ID, RarityAttribute.Type::new);
            event.register(attributeRegistryKey, AFFIX_ID, AffixAttribute.Type::new);
        }
    }

    public static class RarityAttribute implements ItemAttribute {
        private LootRarity rarity;

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
        public void save(CompoundTag nbt) {
            if (this.rarity != null) {
                nbt.putInt("rarity", this.rarity.ordinal());
            }
        }

        @Override
        public void load(CompoundTag nbt) {
            if (nbt.contains("rarity")) {
                DynamicHolder<LootRarity> rarity = RarityRegistry.byOrdinal(nbt.getInt("rarity"));
                if (rarity.isBound())
                    this.rarity = rarity.get();
            }
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
        }
    }

    public static class AffixAttribute implements ItemAttribute {

        private static final Set<String> HIDDEN_AFFIXES = Set.of("socket", "durable");

        private DynamicHolder<? extends Affix> affix;

        public AffixAttribute(DynamicHolder<? extends Affix> affix) {
            this.affix = affix;
        }

        @Override
        public boolean appliesTo(ItemStack stack, Level level) {
            Map<DynamicHolder<? extends Affix>, AffixInstance> affixes = AffixHelper.getAffixes(stack);
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
        public void save(CompoundTag nbt) {
            ResourceLocation loc = this.affix.getId();
            nbt.putString("affix_namespace", loc.getNamespace());
            nbt.putString("affix_path", loc.getPath());
        }

        @Override
        public void load(CompoundTag nbt) {
            if (nbt.contains("affix_namespace") && nbt.contains("affix_path")) {
                String namespace = nbt.getString("affix_namespace");
                String path = nbt.getString("affix_path");
                @SuppressWarnings("removal") ResourceLocation loc = new ResourceLocation(namespace, path);
                DynamicHolder<? extends Affix> affix = AffixRegistry.INSTANCE.holder(loc);
                if (affix.isBound()) {
                    this.affix = affix;
                }
            }
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
        }
    }

}
