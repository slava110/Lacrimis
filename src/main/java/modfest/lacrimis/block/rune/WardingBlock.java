package modfest.lacrimis.block.rune;

import modfest.lacrimis.init.ModBlocks;
import modfest.lacrimis.init.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WardingBlock extends CenterRuneBlock {
    public WardingBlock() {
        super(800, 3);
    }

    @Override
    protected boolean onActivate(World world, BlockPos pos, BlockPos duct, Entity entity, PlayerEntity player) {
        if(world.getBlockState(pos).getBlock() == ModBlocks.runeStone) {
            world.setBlockState(pos, ModBlocks.wardedStone.getDefaultState());
            return true;
        }
        if(world.getBlockState(pos).getBlock() == ModBlocks.wardedStone) {
            world.setBlockState(pos, ModBlocks.runeStone.getDefaultState());
            return true;
        }
        if(entity instanceof PlayerEntity) {
            boolean added = false;
            for(ItemStack item : entity.getArmorItems())
                if(EnchantmentHelper.getLevel(ModEnchantments.WARDED, item) == 0) {
                    item.addEnchantment(ModEnchantments.WARDED, 1);
                    added = true;
                }
            return added;
        }
        if(entity instanceof ItemEntity) {
            ItemStack stack = ((ItemEntity) entity).getStack();
            if(ModEnchantments.WARDED.isAcceptableItem(stack) && EnchantmentHelper.getLevel(ModEnchantments.WARDED, stack) == 0) {
                stack.addEnchantment(ModEnchantments.WARDED, 1);
                return true;
            }
        }
        return false;
    }
}
