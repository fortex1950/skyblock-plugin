package com.example.skyblock.island;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class IslandGenerator {

    /**
     * Generira klasicni SkyBlock otok sa sredistem u 'center': glavna travnata platforma,
     * drvo, sanduk, par pjescanih otocica s kaktusima okolo, i dvije krave koje se mogu
     * razmnozavati.
     * Vraca lokaciju na koju treba teleportirati igraca (iznad platforme).
     */
    public static Location generate(IslandType type, Location center) {
        return generateClassicIsland(center);
    }

    // ==================== KLASICNI OTOK ====================
    private static Location generateClassicIsland(Location center) {
        World world = center.getWorld();
        int bx = center.getBlockX();
        int by = center.getBlockY();
        int bz = center.getBlockZ();

        // Platforma 7x7: dirt ispod, grass na vrhu (piramidalno stanjena prema rubovima)
        // Sloj 1 (najdublji, y-2): 5x5 kamen/dirt
        setLayer(world, bx, by - 2, bz, 5, Material.DIRT);
        // Sloj 2 (y-1): 6x6 dirt
        setLayer(world, bx, by - 1, bz, 6, Material.DIRT);
        // Sloj 3 (y, gornji): 5x5 grass block
        setLayer(world, bx, by, bz, 5, Material.GRASS_BLOCK);

        // Stablo (hrast) na +2,+2 od sredista
        int treeX = bx + 2;
        int treeZ = bz + 2;
        world.getBlockAt(treeX, by, treeZ).setType(Material.GRASS_BLOCK);
        for (int i = 1; i <= 4; i++) {
            world.getBlockAt(treeX, by + i, treeZ).setType(Material.OAK_LOG);
        }
        // Kruna drveta (jednostavna)
        int topY = by + 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                world.getBlockAt(treeX + dx, topY, treeZ + dz).setType(Material.OAK_LEAVES);
                world.getBlockAt(treeX + dx, topY - 1, treeZ + dz).setType(Material.OAK_LEAVES);
            }
        }
        world.getBlockAt(treeX, topY + 1, treeZ).setType(Material.OAK_LEAVES);

        // Jezerce s vodom (2x2) na -2,-2 strani
        world.getBlockAt(bx - 2, by, bz - 2).setType(Material.WATER);
        world.getBlockAt(bx - 2, by, bz - 1).setType(Material.WATER);
        world.getBlockAt(bx - 1, by, bz - 2).setType(Material.WATER);

        // Lava izvor (za obsidian/kamen) malo dalje
        world.getBlockAt(bx + 2, by, bz - 2).setType(Material.LAVA);

        // Sanduk sa starter itemima
        Block chestBlock = world.getBlockAt(bx, by + 1, bz);
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getBlockData() instanceof Chest chestData) {
            chestData.setFacing(BlockFace.SOUTH);
            chestBlock.setBlockData(chestData);
        }
        if (chestBlock.getState() instanceof org.bukkit.block.Chest chestState) {
            Inventory inv = chestState.getBlockInventory();
            inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
            inv.addItem(new ItemStack(Material.ICE, 2));
            inv.addItem(new ItemStack(Material.OAK_SAPLING, 2));
            inv.addItem(new ItemStack(Material.DIRT, 2));
            inv.addItem(new ItemStack(Material.MELON_SEEDS, 1));
            inv.addItem(new ItemStack(Material.WHITE_WOOL, 1));
            inv.addItem(new ItemStack(Material.WHEAT_SEEDS, 2));
            inv.addItem(new ItemStack(Material.BREAD, 5));
            chestState.update();
        }

        // ---- Manji pjescani otocici (desert stil) okolo glavnog otoka, s kaktusima ----
        generateSandIslet(world, bx + 7, by - 1, bz + 6, 1);
        generateSandIslet(world, bx - 6, by - 2, bz + 8, 2);
        generateSandIslet(world, bx + 6, by - 3, bz - 7, 1);
        generateSandIslet(world, bx - 8, by - 1, bz - 5, 2);

        // ---- Dvije krave na travnatoj platformi (mogu se razmnozavati) ----
        spawnBreedingCow(world, bx - 1, by + 1, bz + 1);
        spawnBreedingCow(world, bx + 1, by + 1, bz - 1);

        return new Location(world, bx + 0.5, by + 1, bz + 0.5);
    }

    /**
     * Generira mali plutajuci pjescani otocic (poput mini pustinje) na zadanoj poziciji,
     * s 1-2 kaktusa na njemu. Otocici su odvojeni od glavnog otoka (plutaju u zraku),
     * u skladu s klasicnim SkyBlock stilom.
     */
    private static void generateSandIslet(World world, int cx, int y, int cz, int cactusCount) {
        // Sitna piramida: 3x3 sandstone ispod, 3x3 sand na vrhu
        setLayer(world, cx, y - 1, cz, 3, Material.SANDSTONE);
        setLayer(world, cx, y, cz, 3, Material.SAND);

        // Kaktus/i na otocicu (na pijesku, okruzeni zrakom kako bi ostali postavljeni)
        placeCactus(world, cx, y + 1, cz, 1 + cactusCount);
        if (cactusCount > 1) {
            placeCactus(world, cx + 1, y + 1, cz - 1, cactusCount);
        }

        // Malo suhog grmlja za desert ambijent
        world.getBlockAt(cx - 1, y + 1, cz + 1).setType(Material.DEAD_BUSH);
    }

    /** Spawna kravu na zadanoj poziciji, spremnu za razmnozavanje (nije beba, ne moze biti u ljubavi odmah bez hrane). */
    private static void spawnBreedingCow(World world, int x, int y, int z) {
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        Cow cow = (Cow) world.spawnEntity(loc, EntityType.COW);
        cow.setAdult();
        cow.setBreed(true);
        cow.setAgeLock(false);
    }

    private static void placeCactus(World world, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            world.getBlockAt(x, y + i, z).setType(Material.CACTUS);
        }
    }

    /** Postavlja kvadratni sloj blokova velicine size x size, centriran na (cx, cz), na visini y. */
    private static void setLayer(World world, int cx, int y, int cz, int size, Material material) {
        int half = size / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                world.getBlockAt(cx + dx, y, cz + dz).setType(material);
            }
        }
    }
}
