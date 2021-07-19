package pl.andus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WoodCutterState {
    public final Material material;
    public final byte meta;
    public final Location origin;
    public final Player player;
    public final ItemStack heldItem;
    public final int heldItemUnbreaking;

    public int totalFallen = 0;

    @SuppressWarnings("deprecation")
    public WoodCutterState(Block block, Player player) {
        this.material = block.getType();
        this.meta = block.getData();
        this.origin = block.getLocation();
        this.player = player;
        this.heldItem = player.getInventory().getItemInMainHand();
        this.heldItemUnbreaking = heldItem.getEnchantmentLevel(Enchantment.DURABILITY);
    }

    @SuppressWarnings("deprecation")
    public boolean isSameTree(Block block) {
        // Using deprecated ID and meta here, because the only alternative seems to be
        // creating new Tree objects. Seems too wasteful
        Material blockMat = block.getType();
        int blockMeta = block.getData();
        
        // Handle special case for large oak trees, which uses horizontal logs
        if (material == Material.DARK_OAK_WOOD && meta == 0) {
            return blockMat == Material.DARK_OAK_WOOD && (blockMeta == 0 || blockMeta == 4 || blockMeta == 8);
        }

        else return blockMat == material && blockMeta == meta;
    }
}
