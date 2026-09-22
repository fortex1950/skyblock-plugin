package com.example.skyblock.commands;

import com.example.skyblock.SkyblockPlugin;
import com.example.skyblock.island.Island;
import com.example.skyblock.island.IslandManager;
import com.example.skyblock.island.IslandType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandCommand implements CommandExecutor, TabCompleter {

    private final SkyblockPlugin plugin;
    private final IslandManager manager;

    /** Igraci koji su zatrazili /island reset i cekaju potvrdu (uid -> vrijeme zahtjeva u ms). */
    private final Map<UUID, Long> pendingResetConfirm = new ConcurrentHashMap<>();
    private static final long RESET_CONFIRM_TIMEOUT_MS = 15_000L;

    public IslandCommand(SkyblockPlugin plugin, IslandManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ova komanda se moze koristiti samo u igri.");
            return true;
        }

        // Samostalna komanda /reset (i /otokreset) je samo precica za /island reset.
        if (command.getName().equalsIgnoreCase("reset")) {
            String[] shifted = new String[args.length + 1];
            shifted[0] = "reset";
            System.arraycopy(args, 0, shifted, 1, args.length);
            args = shifted;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
            case "kreiraj":
                handleCreate(player, args);
                break;
            case "home":
            case "tp":
            case "dom":
                handleHome(player);
                break;
            case "invite":
            case "pozovi":
                handleInvite(player, args);
                break;
            case "accept":
            case "prihvati":
                handleAccept(player);
                break;
            case "deny":
            case "odbij":
                handleDeny(player);
                break;
            case "kick":
            case "izbaci":
                handleKick(player, args);
                break;
            case "leave":
            case "napusti":
                handleLeave(player);
                break;
            case "delete":
            case "obrisi":
                handleDelete(player);
                break;
            case "reset":
                handleReset(player, args);
                break;
            case "info":
                handleInfo(player);
                break;
            case "help":
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (manager.hasIsland(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Vec imas otok! Koristi /island home za povratak.");
            return;
        }
        IslandType type = IslandType.CLASSIC;
        player.sendMessage(ChatColor.GREEN + "Generiram tvoj otok...");
        Island island = manager.createIsland(player, type);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Doslo je do greske pri kreiranju otoka.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Otok je kreiran! Ima glavni travnati dio, "
                + "pjescane otocice s kaktusima okolo i dvije krave koje se mogu razmnozavati. "
                + "Mozes pozvati jos jednog igraca na svoj otok sa /island invite <ime>.");
    }

    private void handleHome(Player player) {
        Island island = manager.getIslandOf(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Nemas otok. Napravi ga sa /island create");
            return;
        }
        player.teleport(island.getCenter());
        player.sendMessage(ChatColor.GREEN + "Teleportiran na svoj otok.");
    }

    private void handleInvite(Player player, String[] args) {
        Island island = manager.getIslandOf(player.getUniqueId());
        if (island == null || !island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Samo vlasnik otoka moze pozivati igrace.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Koristi: /island invite <ime_igraca>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Igrac nije pronadjen (mora biti online).");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ne mozes pozvati samog sebe.");
            return;
        }
        if (island.getResidentCount() >= manager.getMaxMembers()) {
            player.sendMessage(ChatColor.RED + "Tvoj otok je pun (max " + manager.getMaxMembers() + " igraca).");
            return;
        }
        if (manager.hasIsland(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Taj igrac vec ima svoj otok ili je clan drugog otoka.");
            return;
        }
        boolean ok = manager.invite(player, target);
        if (!ok) {
            player.sendMessage(ChatColor.RED + "Nije moguce poslati pozivnicu.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Pozivnica poslana igracu " + target.getName() + ".");
        target.sendMessage(ChatColor.GOLD + player.getName() + " te pozvao na svoj otok! "
                + ChatColor.GREEN + "/island accept" + ChatColor.GOLD + " za prihvatanje ili "
                + ChatColor.RED + "/island deny" + ChatColor.GOLD + " za odbijanje.");
    }

    private void handleAccept(Player player) {
        UUID ownerId = manager.getPendingInviteOwner(player.getUniqueId());
        if (ownerId == null) {
            player.sendMessage(ChatColor.RED + "Nemas aktivnu pozivnicu.");
            return;
        }
        boolean ok = manager.acceptInvite(player);
        if (!ok) {
            player.sendMessage(ChatColor.RED + "Pozivnica vise nije vazeca (otok je mozda pun).");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Pridruzio si se otoku!");
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) {
            owner.sendMessage(ChatColor.GREEN + player.getName() + " se pridruzio tvom otoku!");
        }
    }

    private void handleDeny(Player player) {
        UUID ownerId = manager.getPendingInviteOwner(player.getUniqueId());
        if (ownerId == null) {
            player.sendMessage(ChatColor.RED + "Nemas aktivnu pozivnicu.");
            return;
        }
        manager.denyInvite(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Pozivnica odbijena.");
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) {
            owner.sendMessage(ChatColor.YELLOW + player.getName() + " je odbio tvoju pozivnicu.");
        }
    }

    private void handleKick(Player player, String[] args) {
        Island island = manager.getIslandOf(player.getUniqueId());
        if (island == null || !island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Samo vlasnik otoka moze izbacivati clanove.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Koristi: /island kick <ime_igraca>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetId = target != null ? target.getUniqueId() : null;
        if (targetId == null) {
            player.sendMessage(ChatColor.RED + "Igrac nije pronadjen (mora biti online).");
            return;
        }
        boolean ok = manager.kickMember(player, targetId);
        if (!ok) {
            player.sendMessage(ChatColor.RED + "Taj igrac nije clan tvog otoka.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Izbacio si " + target.getName() + " sa otoka.");
        target.sendMessage(ChatColor.RED + "Izbacen si sa otoka igraca " + player.getName() + ".");
    }

    private void handleLeave(Player player) {
        Island island = manager.getIslandOf(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Nemas otok.");
            return;
        }
        if (island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Vlasnik ne moze napustiti svoj otok, koristi /island delete.");
            return;
        }
        manager.leaveIsland(player);
        player.sendMessage(ChatColor.YELLOW + "Napustio si otok.");
    }

    private void handleDelete(Player player) {
        Island island = manager.getIslandOf(player.getUniqueId());
        if (island == null || !island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Samo vlasnik moze obrisati otok.");
            return;
        }
        manager.deleteIsland(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Otok je obrisan iz evidencije. (Blokovi u svijetu ostaju, "
                + "samo je vise ne postoji zapis/zastita otoka.)");
    }

    private void handleReset(Player player, String[] args) {
        Island island = manager.getIslandOf(player.getUniqueId());
        if (island == null || !island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Samo vlasnik otoka moze resetirati otok.");
            return;
        }

        boolean confirmArgGiven = args.length >= 2 && args[1].equalsIgnoreCase("confirm");

        if (!confirmArgGiven) {
            pendingResetConfirm.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage(ChatColor.RED + "UPOZORENJE: Ovo ce obrisati tvoj trenutni otok iz evidencije "
                    + "i napraviti potpuno novi na novoj lokaciji. Svi clanovi tima idu s tobom na novi otok.");
            player.sendMessage(ChatColor.YELLOW + "Upisi " + ChatColor.WHITE + "/island reset confirm"
                    + ChatColor.YELLOW + " u sljedecih 15 sekundi da potvrdis.");
            return;
        }

        Long requestedAt = pendingResetConfirm.remove(player.getUniqueId());
        if (requestedAt == null || System.currentTimeMillis() - requestedAt > RESET_CONFIRM_TIMEOUT_MS) {
            player.sendMessage(ChatColor.RED + "Potvrda je istekla ili nije zatrazena. Ponovo upisi /island reset.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Resetiram tvoj otok i generiram novi...");
        Island newIsland = manager.resetIsland(player);
        if (newIsland == null) {
            player.sendMessage(ChatColor.RED + "Doslo je do greske pri resetiranju otoka.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Novi otok je spreman! Stari zapis je obrisan, "
                + "a ti (i tvoj tim, ako ga imas) ste prebaceni na svjezi otok.");
    }

    private void handleInfo(Player player) {
        Island island = manager.getIslandOf(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Nemas otok.");
            return;
        }
        Player owner = Bukkit.getPlayer(island.getOwner());
        String ownerName = owner != null ? owner.getName() : island.getOwner().toString();
        player.sendMessage(ChatColor.GOLD + "=== Info o otoku ===");
        player.sendMessage(ChatColor.YELLOW + "Tip: " + ChatColor.WHITE + island.getType().getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "Vlasnik: " + ChatColor.WHITE + ownerName);
        player.sendMessage(ChatColor.YELLOW + "Broj clanova: " + ChatColor.WHITE
                + island.getResidentCount() + "/" + manager.getMaxMembers());
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== SkyBlock komande ===");
        player.sendMessage(ChatColor.YELLOW + "/island create" + ChatColor.GRAY + " - napravi otok");
        player.sendMessage(ChatColor.YELLOW + "/island home" + ChatColor.GRAY + " - teleport na otok");
        player.sendMessage(ChatColor.YELLOW + "/island invite <igrac>" + ChatColor.GRAY + " - pozovi drugog igraca (max 2 po otoku)");
        player.sendMessage(ChatColor.YELLOW + "/island accept" + ChatColor.GRAY + " - prihvati pozivnicu");
        player.sendMessage(ChatColor.YELLOW + "/island deny" + ChatColor.GRAY + " - odbij pozivnicu");
        player.sendMessage(ChatColor.YELLOW + "/island kick <igrac>" + ChatColor.GRAY + " - izbaci clana");
        player.sendMessage(ChatColor.YELLOW + "/island leave" + ChatColor.GRAY + " - napusti tudji otok");
        player.sendMessage(ChatColor.YELLOW + "/island delete" + ChatColor.GRAY + " - obrisi svoj otok");
        player.sendMessage(ChatColor.YELLOW + "/island reset" + ChatColor.GRAY + " - obrisi i napravi novi otok (npr. nakon smrti)");
        player.sendMessage(ChatColor.YELLOW + "/island info" + ChatColor.GRAY + " - info o otoku");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("reset")) {
            if (args.length == 1 && "confirm".startsWith(args[0].toLowerCase())) {
                options.add("confirm");
            }
            return options;
        }
        if (args.length == 1) {
            for (String sub : new String[]{"create", "home", "invite", "accept", "deny", "kick", "leave", "delete", "reset", "info", "help"}) {
                if (sub.startsWith(args[0].toLowerCase())) options.add(sub);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) options.add(p.getName());
                }
            } else if (args[0].equalsIgnoreCase("reset")) {
                if ("confirm".startsWith(args[1].toLowerCase())) options.add("confirm");
            }
        }
        return options;
    }
}
