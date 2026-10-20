package com.example.invsee;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * Меню 9x5:
 *  ряд 0: шлем | нагрудник | поножи | ботинки | -- | вторая рука | -- | -- | --
 *  ряды 1-3: основной инвентарь цели
 *  ряд 4: хотбар цели
 * Ниже обычный инвентарь того, кто смотрит.
 */
public class InvseeScreenHandler extends ScreenHandler {

    private static final int TARGET_SLOTS = 45;

    private final PlayerEntity target;

    public InvseeScreenHandler(int syncId, PlayerInventory viewerInv, PlayerEntity target) {
        super(ScreenHandlerType.GENERIC_9X5, syncId);
        this.target = target;
        PlayerInventory ti = target.getInventory();

        // Ряд 0: экипировка. Индексы PlayerInventory: 36 ботинки, 37 поножи, 38 нагрудник, 39 шлем, 40 вторая рука
        this.addSlot(new EquipSlot(ti, 39, 8, 18, EquipmentSlot.HEAD));
        this.addSlot(new EquipSlot(ti, 38, 26, 18, EquipmentSlot.CHEST));
        this.addSlot(new EquipSlot(ti, 37, 44, 18, EquipmentSlot.LEGS));
        this.addSlot(new EquipSlot(ti, 36, 62, 18, EquipmentSlot.FEET));
        this.addSlot(new FillerSlot(80, 18));
        this.addSlot(new EquipSlot(ti, 40, 98, 18, null)); // вторая рука
        this.addSlot(new FillerSlot(116, 18));
        this.addSlot(new FillerSlot(134, 18));
        this.addSlot(new FillerSlot(152, 18));

        // Ряды 1-3: основной инвентарь цели (индексы 9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(ti, 9 + col + row * 9, 8 + col * 18, 36 + row * 18));
            }
        }
        // Ряд 4: хотбар цели (индексы 0-8)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(ti, col, 8 + col * 18, 90));
        }

        // Инвентарь смотрящего
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(viewerInv, 9 + col + row * 9, 8 + col * 18, 121 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(viewerInv, col, 8 + col * 18, 179));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < TARGET_SLOTS) {
                // из инвентаря цели -> к себе
                if (!this.insertItem(stack, TARGET_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // от себя -> в инвентарь цели (броня сама наденется в нужный слот)
                if (!this.insertItem(stack, 0, TARGET_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // закрыть меню, если цель вышла с сервера
        return !target.isRemoved() && player.isAlive();
    }

    /** Слот брони / второй руки. type == null значит вторая рука. */
    private static class EquipSlot extends Slot {
        private final EquipmentSlot type;

        EquipSlot(PlayerInventory inv, int index, int x, int y, EquipmentSlot type) {
            super(inv, index, x, y);
            this.type = type;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (type == null) return true;
            EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
            return eq != null && eq.slot() == type;
        }

        @Override
        public int getMaxItemCount() {
            return type == null ? super.getMaxItemCount() : 1;
        }
    }

    /** Декоративный слот: ничего нельзя ни взять, ни положить. */
    private static class FillerSlot extends Slot {
        FillerSlot(int x, int y) {
            super(makeInv(), 0, x, y);
        }

        private static SimpleInventory makeInv() {
            SimpleInventory inv = new SimpleInventory(1);
            ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            inv.setStack(0, pane);
            return inv;
        }

        @Override public boolean canInsert(ItemStack stack) { return false; }
        @Override public boolean canTakeItems(PlayerEntity player) { return false; }
    }
}
