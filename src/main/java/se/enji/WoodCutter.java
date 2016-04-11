package se.enji;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class WoodCutter extends JavaPlugin implements Listener {
	FileConfiguration config;
	WoodCutterPrism prism;
	Random random;

	boolean needAxe;
	boolean mustSneak;
	boolean recordPrismEvents;

	List<?> breakable = Arrays.asList(Material.LOG, Material.LOG_2);
	List<?> surroundable = Arrays.asList(Material.LOG, Material.LOG_2, Material.DIRT, Material.GRASS);

	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		config = getConfig();
		config.options().copyDefaults(true);
		
		needAxe = config.getBoolean("needAxe");
		mustSneak = config.getBoolean("mustSneak");
		recordPrismEvents = config.getBoolean("recordPrismEvents");

		Plugin prismPlugin = getServer().getPluginManager().getPlugin("Prism");
		if (recordPrismEvents && prismPlugin != null) {
			prism = new WoodCutterPrism(prismPlugin);
		} else {
			prism = null;
		}

		random = new Random();

		saveConfig();
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent e) {
		Player p = e.getPlayer();
		
		if (!p.hasPermission("woodcutter.use")) {
			return;
		}
		
		if (!breakable.contains(e.getBlock().getType())) {
			return;
		}

		if (needAxe && !isHoldingAxe(p)) {
			return;
		}
		
		Location l = e.getBlock().getLocation();
		
		if (!surroundable.contains(l.subtract(0.0, 1.0, 0.0).getBlock().getType()) || !surroundable.contains(l.add(0.0, 1.0, 0.0).getBlock().getType())) {
			return;
		}
		
		if (mustSneak && !p.isSneaking()) {
			return;
		}

		WoodCutterState state = new WoodCutterState(e.getBlock(), p);
		
		columnRemove(state, l);
	}

	private void columnRemove(WoodCutterState state, Location location) {
		boolean logsLeft = true;
		int fallen = 0;
		location.subtract(0.0, 1.0, 0.0);
		
		while (logsLeft) {
			Block block = location.add(0.0,1.0,0.0).getBlock();
			
			if (state.isSameTree(block)) {
				if (prism != null) {
					prism.recordBreak(block, state.player);
				}
				block.breakNaturally(state.heldItem);
				fallen++;
				state.totalFallen++;
			}
			
			else logsLeft = false;

			for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++) {
				columnCheck(state, block, x, z);
			}
		}

		durabilityCheck(state, fallen);
	}

	private void columnCheck(WoodCutterState state, Block block, double xOffset, double zOffset) {
		Location newLocation = block.getLocation().subtract(xOffset, 0, zOffset);

		int totalXOffset = Math.abs(state.origin.getBlockX() - newLocation.getBlockX());
		int totalZOffset = Math.abs(state.origin.getBlockZ() - newLocation.getBlockZ());

		if (totalXOffset > 5 || totalZOffset > 5) {
			return;
		}

		if (state.totalFallen < 150 && state.isSameTree(newLocation.getBlock())) {
			columnRemove(state, newLocation);
		}
	}

	private void durabilityCheck(WoodCutterState state, int fallen) {
		if (!isHoldingAxe(state.player) || state.heldItem.getAmount() == 0) {
			return;
		}

		if (state.heldItemUnbreaking > 0) {
			// http://minecraft.gamepedia.com/Enchantment#Enchantments
			int chance = 100 / (state.heldItemUnbreaking + 1);
			int oldFallen = fallen;

			for (int i = 0; i < oldFallen; i++) {
				if (random.nextInt(100) > chance) fallen--;
			}
		}

		short newDurability = (short)(state.heldItem.getDurability() + fallen);

		if (newDurability < maxDurability(state.heldItem.getType())) {
			state.heldItem.setDurability(newDurability);
		} else {
			state.heldItem.setAmount(0);
			state.player.getInventory().setItemInMainHand(null);
		}
	}

	private boolean isHoldingAxe(Player p) {
		Material held = p.getInventory().getItemInMainHand().getType();
		return held.toString().endsWith("_AXE");
	}
	
	private short maxDurability(Material m) {
		short durability;
		switch (m) {
			case GOLD_AXE:
				durability = 33;
				break;
			case WOOD_AXE:
				durability = 60;
				break;
			case STONE_AXE:
				durability = 132;
				break;
			case IRON_AXE:
				durability = 251;
				break;
			case DIAMOND_AXE:
				durability = 1562;
				break;
			default:
				durability = 0;
				break;
		}
		return durability;
	}
}