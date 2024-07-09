package us.maxpowa.apc;

import com.simibubi.create.content.logistics.filter.ItemAttribute;
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
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.stream.Collectors;

@Mod("apotheoticcreation")
public class ApotheoticCreation
{
    static ItemAttribute rarityAttribute = ItemAttribute.register(new RarityAttribute(null));
    static ItemAttribute affixAttribute = ItemAttribute.register(new AffixAttribute(null));

    public static class RarityAttribute implements ItemAttribute {

        private final LootRarity rarity;
        public RarityAttribute(LootRarity rarity) {
            this.rarity = rarity;
        }

        @Override
        public boolean appliesTo(ItemStack stack) {
            DynamicHolder<LootRarity> itemRarity = AffixHelper.getRarity(stack);
            if (!itemRarity.isBound()) return false;
            return itemRarity.get() == this.rarity;
        }

        @Override
        public List<ItemAttribute> listAttributesOf(ItemStack stack) {
            DynamicHolder<LootRarity> itemRarity = AffixHelper.getRarity(stack);

            List<ItemAttribute> list = new ArrayList<>();
            if (itemRarity.isBound()) {
                list.add(new RarityAttribute(itemRarity.get()));
            }
            return list;

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
        public void writeNBT(CompoundTag nbt) {
            nbt.putInt("rarity", this.rarity.ordinal());
        }

        @Override
        public ItemAttribute readNBT(CompoundTag nbt) {
            if (nbt.contains("rarity")) {
                DynamicHolder<LootRarity> rarity = RarityRegistry.byOrdinal(nbt.getInt("rarity"));
                if (rarity.isBound())
                    return new RarityAttribute(rarity.get());
            }
            return new RarityAttribute(null);
        }
    }

    public static class AffixAttribute implements ItemAttribute {

        private static final Set<String> HIDDEN_AFFIXES = Set.of("socket", "durable");

        private final DynamicHolder<? extends Affix> affix;

        public AffixAttribute(DynamicHolder<? extends Affix> affix) {
            this.affix = affix;
        }

        @Override
        public boolean appliesTo(ItemStack stack) {
            Map<DynamicHolder<? extends Affix>, AffixInstance> affixes = AffixHelper.getAffixes(stack);
            return affixes.containsKey(affix);
        }

        @Override
        public List<ItemAttribute> listAttributesOf(ItemStack stack) {
            Map<DynamicHolder<? extends Affix>, AffixInstance> affixes = AffixHelper.getAffixes(stack);

            return affixes.keySet().stream().filter((affix) -> !HIDDEN_AFFIXES.contains(affix.getId().getPath())).map(AffixAttribute::new).collect(Collectors.toList());
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
        public void writeNBT(CompoundTag nbt) {
            ResourceLocation loc = this.affix.getId();
            nbt.putString("affix_namespace", loc.getNamespace());
            nbt.putString("affix_path", loc.getPath());
        }

        @Override
        public ItemAttribute readNBT(CompoundTag nbt) {
            if (nbt.contains("affix_namespace") && nbt.contains("affix_path")) {
                String namespace = nbt.getString("affix_namespace");
                String path = nbt.getString("affix_path");
                ResourceLocation loc = new ResourceLocation(namespace, path);
                DynamicHolder<? extends Affix> affix = AffixRegistry.INSTANCE.holder(loc);
                if (affix.isBound()) return new AffixAttribute(affix);
            }
            return new AffixAttribute(null);
        }
    }

}
