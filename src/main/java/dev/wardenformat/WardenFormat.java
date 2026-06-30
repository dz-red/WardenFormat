package dev.wardenformat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WardenFormat extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    private NamespacedKey formatKey;

    @Override
    public void onEnable() {
        formatKey = new NamespacedKey(this, "format_text");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("itemformat").setExecutor(this);
        getCommand("itemformat").setTabCompleter(this);
        getCommand("lore").setExecutor(this);
        getCommand("lore").setTabCompleter(this);
        getLogger().info("WardenFormat включён");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("itemformat")) {
            return handleItemformat(sender, args);
        }
        if (cmd.getName().equalsIgnoreCase("lore")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Только для игроков.");
                return true;
            }
            return handleLore(p, args);
        }
        return false;
    }

    private boolean handleItemformat(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("info")) return false;

        String[] lines = {
            "&8&m-----------------------------",
            "&e&lФорматирование предметов в наковальне",
            "&8&m-----------------------------",
            "",
            "&7Используй &e&<код>&7 в самом начале предмета для форматирования.",
            "",
            "&f&lЦВЕТА:",
            "  &00 &8Чёрный    &11 &8Тёмно-синий  &22 &8Тёмно-зелёный",
            "  &33 &8Бирюзовый  &44 &8Тёмно-красный &55 &8Фиолетовый",
            "  &66 &8Золотой    &77 &8Серый        &88 &8Тёмно-серый",
            "  &99 &8Синий      &aa &8Зелёный      &bb &8Голубой",
            "  &cc &8Красный    &dd &8Розовый      &ee &8Жёлтый",
            "  &ff &8Белый",
            "",
            "&f&lСТИЛИ:",
            "  &fl &8— &lЖирный",
            "  &fo &8— &oКурсив",
            "  &fn &8— &nПодчёркнутый",
            "  &fm &8— &mЗачёркнутый",
            "  &fk &8— &kОбфускация",
            "  &fr &8— Сброс форматирования",
            "",
            "&7Чтобы убрать форматирование — начни с &r&r.",
            "&8&m-----------------------------"
        };

        for (String line : lines) {
            sender.sendMessage(SERIALIZER.deserialize(line));
        }
        return true;
    }

    private boolean handleLore(Player p, String[] args) {
        if (args.length == 0) {
            p.sendMessage(SERIALIZER.deserialize("&eИспользование:"));
            p.sendMessage(SERIALIZER.deserialize("&7/lore add <текст> &8— добавить строку"));
            p.sendMessage(SERIALIZER.deserialize("&7/lore set <номер> <текст> &8— заменить строку"));
            p.sendMessage(SERIALIZER.deserialize("&7/lore remove <номер> &8— удалить строку"));
            p.sendMessage(SERIALIZER.deserialize("&7/lore clear &8— очистить описание"));
            return true;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            p.sendMessage(SERIALIZER.deserialize("&cВозьми предмет в руку."));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    p.sendMessage(SERIALIZER.deserialize("&cИспользование: /lore add \"текст\""));
                    return true;
                }
                String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (!raw.startsWith("\"") || !raw.endsWith("\"") || raw.length() < 2) {
                    p.sendMessage(SERIALIZER.deserialize("&cТекст должен быть в кавычках: /lore add \"текст\""));
                    return true;
                }
                String text = raw.substring(1, raw.length() - 1);
                lore.add(loreLine(text));
                p.sendMessage(SERIALIZER.deserialize("&aДобавлена строка &7" + lore.size() + "&a."));
            }
            case "set" -> {
                if (args.length < 3) { p.sendMessage(SERIALIZER.deserialize("&cУкажи номер и текст.")); return true; }
                int idx = parseIndex(args[1], lore.size(), p);
                if (idx < 0) return true;
                String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                lore.set(idx, loreLine(text));
                p.sendMessage(SERIALIZER.deserialize("&aСтрока &7" + (idx + 1) + "&a обновлена."));
            }
            case "remove" -> {
                if (args.length < 2) { p.sendMessage(SERIALIZER.deserialize("&cУкажи номер строки.")); return true; }
                int idx = parseIndex(args[1], lore.size(), p);
                if (idx < 0) return true;
                lore.remove(idx);
                p.sendMessage(SERIALIZER.deserialize("&aСтрока &7" + (idx + 1) + "&a удалена."));
            }
            case "clear" -> {
                lore.clear();
                p.sendMessage(SERIALIZER.deserialize("&aОписание очищено."));
            }
            default -> { return false; }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    private Component loreLine(String text) {
        return SERIALIZER.deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }

    private int parseIndex(String arg, int size, Player p) {
        try {
            int n = Integer.parseInt(arg);
            if (n < 1 || n > size) {
                p.sendMessage(SERIALIZER.deserialize("&cНомер строки от 1 до " + size + "."));
                return -1;
            }
            return n - 1;
        } catch (NumberFormatException e) {
            p.sendMessage(SERIALIZER.deserialize("&cНомер строки должен быть числом."));
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("itemformat") && args.length == 1) {
            return List.of("info");
        }
        if (cmd.getName().equalsIgnoreCase("lore") && args.length == 1) {
            return List.of("add", "set", "remove", "clear");
        }
        return List.of();
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent e) {
        String text = e.getInventory().getRenameText();
        ItemStack input = e.getInventory().getItem(0);

        if ((text == null || text.isEmpty()) && input != null && input.hasItemMeta()) {
            String saved = input.getItemMeta().getPersistentDataContainer()
                    .get(formatKey, PersistentDataType.STRING);
            if (saved != null && e.getView().getPlayer() instanceof Player player) {
                player.sendMessage(SERIALIZER.deserialize("&8[&7Текущий формат: &r" + saved + "&8]"));
            }
        }

        if (text == null || text.isEmpty()) return;

        ItemStack result = e.getResult();
        if (result == null || result.getType().isAir()) return;

        if (!text.contains("&") && input != null && input.hasItemMeta()) {
            String saved = input.getItemMeta().getPersistentDataContainer()
                    .get(formatKey, PersistentDataType.STRING);
            if (saved != null) {
                String prefix = extractFormatPrefix(saved);
                if (!prefix.isEmpty()) text = prefix + text;
            }
        }

        if (!text.contains("&")) return;

        Component name = SERIALIZER.deserialize(text)
                .decoration(TextDecoration.ITALIC, false);

        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;
        meta.displayName(name);
        meta.getPersistentDataContainer().set(formatKey, PersistentDataType.STRING, text);
        result.setItemMeta(meta);
        e.setResult(result);
    }

    private String extractFormatPrefix(String raw) {
        StringBuilder prefix = new StringBuilder();
        int i = 0;
        while (i < raw.length() - 1) {
            if (raw.charAt(i) == '&') {
                char code = Character.toLowerCase(raw.charAt(i + 1));
                if ("0123456789abcdefklmnor".indexOf(code) >= 0) {
                    if (code == 'r') break;
                    prefix.append('&').append(raw.charAt(i + 1));
                    i += 2;
                    continue;
                }
            }
            break;
        }
        return prefix.toString();
    }
}
