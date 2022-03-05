package modfest.lacrimis.block.entity;

import modfest.lacrimis.block.DrainedCryingObsidianBlock;
import modfest.lacrimis.block.ModBlocks;
import modfest.lacrimis.crafting.CrucibleRecipe;
import modfest.lacrimis.crafting.ModCrafting;
import modfest.lacrimis.entity.ModEntities;
import modfest.lacrimis.init.*;
import modfest.lacrimis.item.BottleOfTearsItem;
import modfest.lacrimis.item.ModItems;
import modfest.lacrimis.util.DuctUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Optional;

public class CrucibleEntity extends SoulTankEntity {
    private static final Box ITEM_BOX = new Box(0, 0.4, 0, 1, 1, 1);
    private static final int CRAFT_COOLDOWN = 15;

    private int craftTime = 0;
    private Direction collector = null;

    public CrucibleEntity(BlockPos pos, BlockState state) {
        super(ModEntities.crucible, pos, state, 1000, 1);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CrucibleEntity blockEntity) {
        blockEntity.runTick();
    }

    public void runTick() {
        if(world == null)
            return;

        //Catch tears from above
        if (getRelativeLevel() < 1 && this.world.getTime() % 10 == 0) {
            for (int dy = 1; dy < 5; dy++) {
                BlockPos obsidianPos = this.pos.up(dy);
                BlockState obsidianState = this.world.getBlockState(obsidianPos);
                if (obsidianState.getBlock() == ModBlocks.tearLantern)
                    break;
                if (obsidianState.getBlock() == ModBlocks.drainedCryingObsidian || obsidianState.getBlock() == Blocks.CRYING_OBSIDIAN) {
                    this.getTank().addTears(1);
                    world.setBlockState(obsidianPos, DrainedCryingObsidianBlock.removeTearState(obsidianState, 1));
                    break;
                }
            }
        }
        
        if(world.isClient || world.getServer() == null)
            return;

        //Check for tear collector
        if(collector != null && getTank().getSpace() > 10 && validCollector(collector))
            getTank().addTears(DuctUtil.locateTears(world, pos.offset(collector, 2), 10));
        else
            collector = null;

        //Locate new tear collector
        if(collector == null)
            for (Direction direction : Direction.values())
                if(validCollector(direction)) {
                    collector = direction;
                    break;
                }

        if (craftTime < CRAFT_COOLDOWN)
            craftTime++;

        //Start new crafting
        if (craftTime >= CRAFT_COOLDOWN) {
            for (ItemEntity entity : world.getNonSpectatingEntities(ItemEntity.class, ITEM_BOX.offset(pos))) {
                inventory.setStack(0, entity.getStack());

                Optional<CrucibleRecipe> optional = world.getServer()
                        .getRecipeManager()
                        .getFirstMatch(ModCrafting.CRUCIBLE_RECIPE, inventory, world);
                if (optional.isPresent()) {
                    CrucibleRecipe recipe = optional.get();
                    if (recipe.matches(inventory, world)) {
                        // Craft item
                        ItemStack remainder = inventory.getStack(0);
                        remainder.decrement(1);
                        entity.setStack(remainder);
                        ItemScatterer.spawn(world, pos.up(), new SimpleInventory(recipe.getOutput().copy()));
                        ModNetworking.sendCrucibleParticlesPacket((ServerWorld) world, this);
                        getTank().removeTears(recipe.getTears());
                        craftTime = 0;
                        break;
                    }
                } else if(entity.getStack().getItem() == ModItems.bottleOfTears && getTank().getSpace() >= BottleOfTearsItem.capacity) {
                    // Craft item
                    ItemStack remainder = inventory.getStack(0);
                    remainder.decrement(1);
                    entity.setStack(remainder);
                    ItemScatterer.spawn(world, pos.up(), new SimpleInventory(new ItemStack(Items.GLASS_BOTTLE)));
                    ModNetworking.sendCrucibleParticlesPacket((ServerWorld) world, this);
                    getTank().addTears(BottleOfTearsItem.capacity);
                    craftTime = 0;
                }
            }
        }
    }

    private boolean validCollector(Direction dir) {
        if(world == null || !(world.getBlockState(pos.offset(dir)).isOf(ModBlocks.tearCollector)))
            return false;
        return world.getBlockState(pos.offset(dir)).get(FacingBlock.FACING) == dir.getOpposite();
    }

    public int[] getAvailableSlots(Direction side) {
        return new int[]{};
    }
}
