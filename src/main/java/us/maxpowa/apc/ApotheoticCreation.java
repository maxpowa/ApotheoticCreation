package us.maxpowa.apc;

import com.simibubi.create.content.logistics.filter.ItemAttribute;
import shadows.apotheosis.adventure.affix.Affix;
import shadows.apotheosis.adventure.affix.AffixHelper;
import shadows.apotheosis.adventure.affix.AffixInstance;
import shadows.apotheosis.adventure.affix.AffixManager;
import shadows.apotheosis.adventure.loot.LootRarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import shadows.apotheosis.adventure.loot.LootRarityManager;

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
            if (stack.hasTag()) {
                assert stack.getTag() != null;
                LootRarity itemRarity;
                if (stack.getTag().contains("rarity")) {
                    itemRarity = LootRarity.byId(stack.getTag().getString("rarity"));
                } else {
                    itemRarity = AffixHelper.getRarity(stack);
                }
                return itemRarity != null;
            }
            return false;
        }

        @Override
        public List<ItemAttribute> listAttributesOf(ItemStack stack) {
            LootRarity itemRarity = null;
            if (stack.hasTag()) {
                assert stack.getTag() != null;
                if (stack.getTag().contains("rarity")) {
                    itemRarity = LootRarity.byId(stack.getTag().getString("rarity"));
                } else {
                    itemRarity = AffixHelper.getRarity(stack);
                }
            }

            List<ItemAttribute> list = new ArrayList<>();
            if (itemRarity != null) {
                list.add(new RarityAttribute(itemRarity));
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
            if (this.rarity != null) {
                nbt.putInt("rarity", this.rarity.ordinal());
            }
        }

        @Override
        public ItemAttribute readNBT(CompoundTag nbt) {
            if (nbt.contains("rarity")) {
                if( LootRarity.LIST.size() >= nbt.getInt("rarity")) {
                    LootRarity rarity = LootRarity.LIST.get(nbt.getInt("rarity"));
                    return new RarityAttribute(rarity);
                }
            }
            return new RarityAttribute(null);
        }
    }

    public static class AffixAttribute implements ItemAttribute {

        private static final Set<String> HIDDEN_AFFIXES = Set.of("socket", "durable");

        private final Affix affix;

        public AffixAttribute(Affix affix) {
            this.affix = affix;
        }

        @Override
        public boolean appliesTo(ItemStack stack) {
            Map<Affix, AffixInstance> affixes = AffixHelper.getAffixes(stack);
            return affixes.containsKey(affix);
        }

        @Override
        public List<ItemAttribute> listAttributesOf(ItemStack stack) {
            Map<Affix, AffixInstance> affixes = AffixHelper.getAffixes(stack);

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
            if (this.affix != null) {
                ResourceLocation loc = this.affix.getId();
                nbt.putString("affix_namespace", loc.getNamespace());
                nbt.putString("affix_path", loc.getPath());
            }
        }

        @Override
        public ItemAttribute readNBT(CompoundTag nbt) {
            if (nbt.contains("affix_namespace") && nbt.contains("affix_path")) {
                String namespace = nbt.getString("affix_namespace");
                String path = nbt.getString("affix_path");
                ResourceLocation loc = new ResourceLocation(namespace, path);
                Affix affix = AffixManager.INSTANCE.getOrDefault(loc, null);
                if (affix != null) return new AffixAttribute(affix);
            }
            return new AffixAttribute(null);
        }
    }

}
