package dev.luna5ama.trollhack.mixins.core;

import dev.luna5ama.trollhack.manager.managers.InventoryTaskManager;
import dev.luna5ama.trollhack.mixins.accessor.AccessorSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(Container.class)
public abstract class MixinContainer {
    @Shadow
    public static boolean canAddItemToSlot(@Nullable Slot slotIn, ItemStack stack, boolean stackSizeMatters) {
        return false;
    }

    @Shadow
    public List<Slot> inventorySlots;

    @Shadow
    public abstract void detectAndSendChanges();

    @Shadow
    public abstract boolean canMergeSlot(ItemStack stack, Slot slotIn);

    @Shadow
    public abstract ItemStack transferStackInSlot(EntityPlayer playerIn, int index);

    @Shadow
    protected abstract void resetDrag();

    @Shadow
    private int dragEvent;

    @Shadow
    public static void computeStackSize(Set<Slot> dragSlotsIn, int dragModeIn, ItemStack stack, int slotStackSize) {
    }

    @Shadow
    private int dragMode;

    @Shadow
    @Final
    private Set<Slot> dragSlots;

    @Shadow
    public static int extractDragMode(int eventButton) {
        return 0;
    }

    @Shadow
    public static boolean isValidDragMode(int dragModeIn, EntityPlayer player) {
        return false;
    }

    @Shadow
    public static int getDragEvent(int clickedButton) {
        return 0;
    }

    @Shadow
    public abstract boolean canDragIntoSlot(Slot slotIn);

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    public void Inject$slotClick$HEAD(
        int slotId,
        int dragType,
        ClickType clickTypeIn,
        EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        cir.cancel();
        synchronized (InventoryTaskManager.INSTANCE) {
            ItemStack itemstack = ItemStack.EMPTY;
            InventoryPlayer inventoryplayer = player.inventory;

            if (clickTypeIn == ClickType.QUICK_CRAFT) {
                int j1 = this.dragEvent;
                this.dragEvent = getDragEvent(dragType);

                if ((j1 != 1 || this.dragEvent != 2) && j1 != this.dragEvent) {
                    this.resetDrag();
                } else if (inventoryplayer.getItemStack().isEmpty()) {
                    this.resetDrag();
                } else if (this.dragEvent == 0) {
                    this.dragMode = extractDragMode(dragType);

                    if (isValidDragMode(this.dragMode, player)) {
                        this.dragEvent = 1;
                        this.dragSlots.clear();
                    } else {
                        this.resetDrag();
                    }
                } else if (this.dragEvent == 1) {
                    Slot slot7 = this.inventorySlots.get(slotId);
                    ItemStack itemstack12 = inventoryplayer.getItemStack();

                    if (slot7 != null && canAddItemToSlot(slot7, itemstack12, true) && slot7.isItemValid(itemstack12) && (this.dragMode == 2 || itemstack12.getCount() > this.dragSlots.size()) && this.canDragIntoSlot(slot7)) {
                        this.dragSlots.add(slot7);
                    }
                } else if (this.dragEvent == 2) {
                    if (!this.dragSlots.isEmpty()) {
                        ItemStack itemstack9 = inventoryplayer.getItemStack().copy();
                        int k1 = inventoryplayer.getItemStack().getCount();

                        for (Slot slot8 : this.dragSlots) {
                            ItemStack itemstack13 = inventoryplayer.getItemStack();

                            if (slot8 != null && canAddItemToSlot(slot8, itemstack13, true) && slot8.isItemValid(itemstack13) && (this.dragMode == 2 || itemstack13.getCount() >= this.dragSlots.size()) && this.canDragIntoSlot(slot8)) {
                                ItemStack itemstack14 = itemstack9.copy();
                                int j3 = slot8.getHasStack() ? slot8.getStack().getCount() : 0;
                                computeStackSize(this.dragSlots, this.dragMode, itemstack14, j3);
                                int k3 = Math.min(itemstack14.getMaxStackSize(), slot8.getItemStackLimit(itemstack14));

                                if (itemstack14.getCount() > k3) {
                                    itemstack14.setCount(k3);
                                }

                                k1 -= itemstack14.getCount() - j3;
                                slot8.putStack(itemstack14);
                            }
                        }

                        itemstack9.setCount(k1);
                        inventoryplayer.setItemStack(itemstack9);
                    }

                    this.resetDrag();
                } else {
                    this.resetDrag();
                }
            } else if (this.dragEvent != 0) {
                this.resetDrag();
            } else if ((clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
                if (slotId == -999) {
                    if (!inventoryplayer.getItemStack().isEmpty()) {
                        if (dragType == 0) {
                            player.dropItem(inventoryplayer.getItemStack(), true);
                            inventoryplayer.setItemStack(ItemStack.EMPTY);
                        }

                        if (dragType == 1) {
                            player.dropItem(inventoryplayer.getItemStack().splitStack(1), true);
                        }
                    }
                } else if (clickTypeIn == ClickType.QUICK_MOVE) {
                    if (slotId < 0) {
                        cir.setReturnValue(ItemStack.EMPTY);
                        return;
                    }

                    Slot slot5 = this.inventorySlots.get(slotId);

                    if (slot5 == null || !slot5.canTakeStack(player)) {
                        cir.setReturnValue(ItemStack.EMPTY);
                        return;
                    }

                    for (ItemStack itemstack7 = this.transferStackInSlot(player, slotId); !itemstack7.isEmpty() && ItemStack.areItemsEqual(slot5.getStack(), itemstack7); itemstack7 = this.transferStackInSlot(player, slotId)) {
                        itemstack = itemstack7.copy();
                    }
                } else {
                    if (slotId < 0) {
                        cir.setReturnValue(ItemStack.EMPTY);
                        return;
                    }

                    Slot slot6 = this.inventorySlots.get(slotId);

                    if (slot6 != null) {
                        ItemStack itemstack8 = slot6.getStack();
                        ItemStack itemstack11 = inventoryplayer.getItemStack();

                        if (!itemstack8.isEmpty()) {
                            itemstack = itemstack8.copy();
                        }

                        if (itemstack8.isEmpty()) {
                            if (!itemstack11.isEmpty() && slot6.isItemValid(itemstack11)) {
                                int i3 = dragType == 0 ? itemstack11.getCount() : 1;

                                if (i3 > slot6.getItemStackLimit(itemstack11)) {
                                    i3 = slot6.getItemStackLimit(itemstack11);
                                }

                                slot6.putStack(itemstack11.splitStack(i3));
                            }
                        } else if (slot6.canTakeStack(player)) {
                            if (itemstack11.isEmpty()) {
                                if (itemstack8.isEmpty()) {
                                    slot6.putStack(ItemStack.EMPTY);
                                    inventoryplayer.setItemStack(ItemStack.EMPTY);
                                } else {
                                    int l2 = dragType == 0 ? itemstack8.getCount() : (itemstack8.getCount() + 1) / 2;
                                    inventoryplayer.setItemStack(slot6.decrStackSize(l2));

                                    if (itemstack8.isEmpty()) {
                                        slot6.putStack(ItemStack.EMPTY);
                                    }

                                    slot6.onTake(player, inventoryplayer.getItemStack());
                                }
                            } else if (slot6.isItemValid(itemstack11)) {
                                if (itemstack8.getItem() == itemstack11.getItem() && itemstack8.getMetadata() == itemstack11.getMetadata() && ItemStack.areItemStackTagsEqual(itemstack8, itemstack11)) {
                                    int k2 = dragType == 0 ? itemstack11.getCount() : 1;

                                    if (k2 > slot6.getItemStackLimit(itemstack11) - itemstack8.getCount()) {
                                        k2 = slot6.getItemStackLimit(itemstack11) - itemstack8.getCount();
                                    }

                                    if (k2 > itemstack11.getMaxStackSize() - itemstack8.getCount()) {
                                        k2 = itemstack11.getMaxStackSize() - itemstack8.getCount();
                                    }

                                    itemstack11.shrink(k2);
                                    itemstack8.grow(k2);
                                } else if (itemstack11.getCount() <= slot6.getItemStackLimit(itemstack11)) {
                                    slot6.putStack(itemstack11);
                                    inventoryplayer.setItemStack(itemstack8);
                                }
                            } else if (itemstack8.getItem() == itemstack11.getItem() && itemstack11.getMaxStackSize() > 1 && (!itemstack8.getHasSubtypes() || itemstack8.getMetadata() == itemstack11.getMetadata()) && ItemStack.areItemStackTagsEqual(itemstack8, itemstack11) && !itemstack8.isEmpty()) {
                                int j2 = itemstack8.getCount();

                                if (j2 + itemstack11.getCount() <= itemstack11.getMaxStackSize()) {
                                    itemstack11.grow(j2);
                                    itemstack8 = slot6.decrStackSize(j2);

                                    if (itemstack8.isEmpty()) {
                                        slot6.putStack(ItemStack.EMPTY);
                                    }

                                    slot6.onTake(player, inventoryplayer.getItemStack());
                                }
                            }
                        }

                        slot6.onSlotChanged();
                    }
                }
            } else if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
                Slot slot4 = this.inventorySlots.get(slotId);
                ItemStack itemstack6 = inventoryplayer.getStackInSlot(dragType);
                ItemStack itemstack10 = slot4.getStack();

                if (!itemstack6.isEmpty() || !itemstack10.isEmpty()) {
                    if (itemstack6.isEmpty()) {
                        if (slot4.canTakeStack(player)) {
                            inventoryplayer.setInventorySlotContents(dragType, itemstack10);
                            ((AccessorSlot) slot4).trollOnSwapCraft(itemstack10.getCount());
                            slot4.putStack(ItemStack.EMPTY);
                            slot4.onTake(player, itemstack10);
                        }
                    } else if (itemstack10.isEmpty()) {
                        if (slot4.isItemValid(itemstack6)) {
                            int l1 = slot4.getItemStackLimit(itemstack6);

                            if (itemstack6.getCount() > l1) {
                                slot4.putStack(itemstack6.splitStack(l1));
                            } else {
                                slot4.putStack(itemstack6);
                                inventoryplayer.setInventorySlotContents(dragType, ItemStack.EMPTY);
                            }
                        }
                    } else if (slot4.canTakeStack(player) && slot4.isItemValid(itemstack6)) {
                        int i2 = slot4.getItemStackLimit(itemstack6);

                        if (itemstack6.getCount() > i2) {
                            slot4.putStack(itemstack6.splitStack(i2));
                            slot4.onTake(player, itemstack10);

                            if (!inventoryplayer.addItemStackToInventory(itemstack10)) {
                                player.dropItem(itemstack10, true);
                            }
                        } else {
                            slot4.putStack(itemstack6);
                            inventoryplayer.setInventorySlotContents(dragType, itemstack10);
                            slot4.onTake(player, itemstack10);
                        }
                    }
                }
            } else if (clickTypeIn == ClickType.CLONE && player.capabilities.isCreativeMode && inventoryplayer.getItemStack().isEmpty() && slotId >= 0) {
                Slot slot3 = this.inventorySlots.get(slotId);

                if (slot3 != null && slot3.getHasStack()) {
                    ItemStack itemstack5 = slot3.getStack().copy();
                    itemstack5.setCount(itemstack5.getMaxStackSize());
                    inventoryplayer.setItemStack(itemstack5);
                }
            } else if (clickTypeIn == ClickType.THROW && inventoryplayer.getItemStack().isEmpty() && slotId >= 0) {
                Slot slot2 = this.inventorySlots.get(slotId);

                if (slot2 != null && slot2.getHasStack() && slot2.canTakeStack(player)) {
                    ItemStack itemstack4 = slot2.decrStackSize(dragType == 0 ? 1 : slot2.getStack().getCount());
                    slot2.onTake(player, itemstack4);
                    player.dropItem(itemstack4, true);
                }
            } else if (clickTypeIn == ClickType.PICKUP_ALL && slotId >= 0) {
                Slot slot = this.inventorySlots.get(slotId);
                ItemStack itemstack1 = inventoryplayer.getItemStack();

                if (!itemstack1.isEmpty() && (slot == null || !slot.getHasStack() || !slot.canTakeStack(player))) {
                    int i = dragType == 0 ? 0 : this.inventorySlots.size() - 1;
                    int j = dragType == 0 ? 1 : -1;

                    for (int k = 0; k < 2; ++k) {
                        for (int l = i; l >= 0 && l < this.inventorySlots.size() && itemstack1.getCount() < itemstack1.getMaxStackSize(); l += j) {
                            Slot slot1 = this.inventorySlots.get(l);

                            if (slot1.getHasStack() && canAddItemToSlot(slot1, itemstack1, true) && slot1.canTakeStack(player) && this.canMergeSlot(itemstack1, slot1)) {
                                ItemStack itemstack2 = slot1.getStack();

                                if (k != 0 || itemstack2.getCount() != itemstack2.getMaxStackSize()) {
                                    int i1 = Math.min(itemstack1.getMaxStackSize() - itemstack1.getCount(), itemstack2.getCount());
                                    ItemStack itemstack3 = slot1.decrStackSize(i1);
                                    itemstack1.grow(i1);

                                    if (itemstack3.isEmpty()) {
                                        slot1.putStack(ItemStack.EMPTY);
                                    }

                                    slot1.onTake(player, itemstack3);
                                }
                            }
                        }
                    }
                }

                this.detectAndSendChanges();
            }

            cir.setReturnValue(itemstack);
        }
    }
}
