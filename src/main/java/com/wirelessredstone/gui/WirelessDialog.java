package com.wirelessredstone.gui;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.ConnectorToolFactory;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.manager.WireViewManager;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.GroupNameParser;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** Paper dialog UI for wireless groups. Every callback resolves the group again before changing it. */
public final class WirelessDialog {
    private static final int PAGE_SIZE = 10;
    private final WirelessRedstonePlugin plugin;
    private final LinkedBulbManager bulbs;
    private final LinkedChestManager chests;
    private final CategoryManager categories;

    public WirelessDialog(WirelessRedstonePlugin plugin) {
        this.plugin = plugin;
        this.bulbs = plugin.getBulbManager();
        this.chests = plugin.getChestManager();
        this.categories = plugin.getCategoryManager();
    }

    public void open(Player player, boolean showAll) { open(player, showAll, 0, ""); }

    private void open(Player player, boolean showAll, int page, String query) {
        if (!player.hasPermission("wirelessredstone.use")) return;
        boolean all = showAll && player.hasPermission("wirelessredstone.admin");
        String search = query == null ? "" : query.trim();
        List<BaseGroup> groups = visibleGroups(player, all);
        if (!search.isEmpty()) {
            String needle = search.toLowerCase(Locale.ROOT);
            groups.removeIf(group -> !group.getDisplayName().toLowerCase(Locale.ROOT).contains(needle)
                    && (group.getDescription() == null
                    || !group.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                    && (categoryName(group) == null
                    || !categoryName(group).toLowerCase(Locale.ROOT).contains(needle)));
        }
        groups.sort(Comparator.comparingDouble((BaseGroup group) -> distanceSquared(player, group))
                .thenComparing(BaseGroup::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        int pages = Math.max(1, (groups.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, pages - 1));
        List<ActionButton> actions = new ArrayList<>();
        actions.add(button(Component.text("Search", NamedTextColor.GREEN), "Search names and descriptions", 90,
                (p, view) -> open(p, all, 0, view.getText("search"))));
        actions.add(button(Component.text("Clear", NamedTextColor.GRAY), "Clear search", 55,
                p -> open(p, all, 0, "")));
        actions.add(button(Component.text("✂ Get Circuit Tool", NamedTextColor.GREEN), "Create a new wireless group", 200,
                p -> create(p, all, current, search)));
        actions.add(button(Component.text(all || !player.hasPermission("wirelessredstone.admin")
                        ? "My groups" : "All groups", NamedTextColor.YELLOW),
                all || !player.hasPermission("wirelessredstone.admin") ? "Show your groups" : "Show every group", 75,
                p -> open(p, !all && p.hasPermission("wirelessredstone.admin"), 0, search)));
        for (int i = current * PAGE_SIZE; i < Math.min(groups.size(), (current + 1) * PAGE_SIZE); i++) {
            BaseGroup group = groups.get(i);
            UUID id = group.getGroupId();
            boolean bulb = group instanceof BulbGroup;
            String category = categoryName(group);
            Component label = Component.empty();
            if (category != null) {
                label = label.append(Component.text("▣ " + category + " / ", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));
            }
            label = label.append(Component.text(GroupNameParser.parse(group.getDisplayName()).groupName(),
                    bulb ? NamedTextColor.AQUA : NamedTextColor.WHITE));
            Component detailsTooltip = Component.text("Open group details");
            if (group.getDescription() != null) {
                detailsTooltip = detailsTooltip.append(Component.newline())
                        .append(Component.text(group.getDescription(), NamedTextColor.LIGHT_PURPLE)
                                .decorate(TextDecoration.ITALIC));
            }
            actions.add(button(label, detailsTooltip, 200, p -> edit(p, id, bulb, all, current, search)));
            actions.add(button(Component.text("Edit ✎", NamedTextColor.YELLOW), "Rename, get a tool, or remove", 75,
                    p -> edit(p, id, bulb, all, current, search)));
        }
        if (current > 0) {
            actions.add(button(Component.text("← Previous"), "Previous page", 200, p -> open(p, all, current - 1, search)));
            actions.add(button(Component.text(" "), "", 75, p -> open(p, all, current - 1, search)));
        }
        if (current + 1 < pages) {
            actions.add(button(Component.text("Next →"), "Next page", 200, p -> open(p, all, current + 1, search)));
            actions.add(button(Component.text(" "), "", 75, p -> open(p, all, current + 1, search)));
        }
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(
                "Nearest groups first • " + groups.size() + " groups • Page " + (current + 1) + "/" + pages,
                NamedTextColor.GRAY)));
        if (groups.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text(search.isEmpty()
                    ? "No groups yet. Use the Circuit Tool to create one."
                    : "No matching groups. Try another search or clear the field.", NamedTextColor.GRAY)));
        }
        show(player, "Wireless Redstone", body,
                List.of(DialogInput.text("search", Component.text("Search groups"))
                        .initial(search).maxLength(100).width(275).build()), actions, 2,
                button(Component.text("Close"), "Close", 100, Player::closeDialog));
    }

    private List<BaseGroup> visibleGroups(Player player, boolean all) {
        List<BaseGroup> groups = new ArrayList<>();
        for (BulbGroup group : bulbs.getAllGroups()) if (visible(player, group, all)) groups.add(group);
        for (ChestGroup group : chests.getAllGroups()) if (visible(player, group, all)) groups.add(group);
        return groups;
    }

    private boolean visible(Player player, BaseGroup group, boolean all) {
        return all || player.getUniqueId().equals(group.getOwnerUuid());
    }

    private double distanceSquared(Player player, BaseGroup group) {
        double nearest = Double.POSITIVE_INFINITY;
        for (Location location : group.getPlacedLocations()) {
            if (location.getWorld() != null && location.getWorld().equals(player.getWorld())) {
                nearest = Math.min(nearest, location.distanceSquared(player.getLocation()));
            }
        }
        return nearest;
    }

    private String categoryName(BaseGroup group) {
        String prefix = GroupNameParser.parse(group.getDisplayName()).categoryName();
        if (prefix != null) return prefix;
        return group.getCategoryId() == null ? null : categories.getCategoryById(group.getCategoryId())
                .map(category -> category.getName()).orElse(null);
    }

    private BaseGroup resolve(Player player, UUID id, boolean bulb) {
        BaseGroup group = bulb ? bulbs.getGroupById(id).orElse(null) : chests.getGroupById(id).orElse(null);
        if (group == null || !player.hasPermission("wirelessredstone.use")
                || (!player.hasPermission("wirelessredstone.admin") && !player.getUniqueId().equals(group.getOwnerUuid()))) {
            player.sendMessage(Component.text("That group is no longer available.", NamedTextColor.RED));
            return null;
        }
        return group;
    }

    private void edit(Player player, UUID id, boolean bulb, boolean all, int page, String search) {
        BaseGroup group = resolve(player, id, bulb);
        if (group == null) { open(player, all, page, search); return; }
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("Type: " + (bulb ? "Bulb / lamp" : "Container")
                + "  •  Placed: " + group.getPlacedCount() + "/" + group.getMaxSize(), NamedTextColor.GRAY)));
        if (bulb) body.add(DialogBody.plainMessage(Component.text("State: "
                + (((BulbGroup) group).isLit() ? "ON" : "OFF"), NamedTextColor.YELLOW)));
        if (group.getDescription() != null) body.add(DialogBody.plainMessage(Component.text(group.getDescription(), NamedTextColor.GRAY)));
        List<ActionButton> actions = new ArrayList<>();
        actions.add(button(Component.text("✂ Circuit Tool", NamedTextColor.GREEN), "Get a tool for this group", 150,
                p -> giveExistingTool(p, id, bulb)));
        actions.add(button(Component.text("Rename ✎", NamedTextColor.YELLOW), "Rename this group", 150,
                p -> rename(p, id, bulb, all, page, search)));
        actions.add(button(Component.text("Edit description", NamedTextColor.YELLOW), "Edit group description", 150,
                p -> description(p, id, bulb, all, page, search)));
        actions.add(button(Component.text("Remove group", NamedTextColor.RED), "Permanently remove the group", 150,
                p -> confirmRemove(p, id, bulb, all, page, search)));
        if (player.hasPermission("wirelessredstone.teleport")) {
            for (int i = 0; i < group.getLocations().size(); i++) {
                Location location = group.getLocation(i);
                if (location == null) continue;
                int slot = i;
                actions.add(button(Component.text("Teleport " + BaseGroup.getIndexLabel(i), NamedTextColor.AQUA),
                        "Teleport to this block\n" + location.getWorld().getName() + ": "
                                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(),
                        150, p -> teleport(p, id, bulb, slot)));
            }
        }
        show(player, group.getDisplayName(), body, List.of(), actions, 2,
                button(Component.text("← Groups"), "Back to groups", 100, p -> open(p, all, page, search)));
    }

    public void openCreate(Player player) { create(player, false, 0, ""); }

    private void create(Player player, boolean all, int page, String search) {
        List<ActionButton> actions = List.of(button(Component.text("Get Circuit Tool", NamedTextColor.GREEN),
                "Create a tool for a new group", 150, (p, view) -> {
                    String name = view.getText("name");
                    if (name == null || name.isBlank()) { p.sendMessage(Component.text("Enter a group name.", NamedTextColor.RED)); create(p, all, page, search); return; }
                    give(p, ConnectorToolFactory.createCreationModeConnectorTool(name.trim()));
                    p.sendMessage(Component.text("Circuit Tool created for " + name.trim() + ".", NamedTextColor.GREEN));
                }));
        show(player, "New Circuit Tool", List.of(DialogBody.plainMessage(Component.text(
                "Use category/name to place the new group in a category.", NamedTextColor.GRAY))),
                List.of(DialogInput.text("name", Component.text("Group name")).maxLength(64).width(300).build()),
                actions, 1, button(Component.text("← Groups"), "Back", 100, p -> open(p, all, page, search)));
    }

    private void rename(Player player, UUID id, boolean bulb, boolean all, int page, String search) {
        BaseGroup group = resolve(player, id, bulb);
        if (group == null) return;
        show(player, "Rename group", List.of(DialogBody.plainMessage(Component.text(
                "Use category/name to move the group to a category.", NamedTextColor.GRAY))),
                List.of(DialogInput.text("name", Component.text("Name"))
                        .initial(categoryName(group) != null && !GroupNameParser.parse(group.getDisplayName()).hasCategory()
                                ? categoryName(group) + "/" + group.getDisplayName() : group.getDisplayName())
                        .maxLength(64).width(300).build()),
                List.of(button(Component.text("Save", NamedTextColor.GREEN), "Save name", 150, (p, view) -> {
                    BaseGroup current = resolve(p, id, bulb);
                    if (current == null) return;
                    String name = view.getText("name");
                    if (name == null || name.isBlank()) { p.sendMessage(Component.text("Name cannot be empty.", NamedTextColor.RED)); rename(p, id, bulb, all, page, search); return; }
                    current.setCustomName(name.trim());
                    current.setCategoryId(null);
                    save(bulb);
                    edit(p, id, bulb, all, page, search);
                })), 1, button(Component.text("← Back"), "Back", 100, p -> edit(p, id, bulb, all, page, search)));
    }

    private void description(Player player, UUID id, boolean bulb, boolean all, int page, String search) {
        BaseGroup group = resolve(player, id, bulb);
        if (group == null) return;
        show(player, "Edit description", List.of(),
                List.of(DialogInput.text("description", Component.text("Description"))
                        .initial(group.getDescription() == null ? "" : group.getDescription())
                        .maxLength(256).width(300).build()),
                List.of(button(Component.text("Save", NamedTextColor.GREEN), "Save description", 150, (p, view) -> {
                    BaseGroup current = resolve(p, id, bulb);
                    if (current == null) return;
                    current.setDescription(view.getText("description"));
                    save(bulb);
                    edit(p, id, bulb, all, page, search);
                })), 1, button(Component.text("← Back"), "Back", 100, p -> edit(p, id, bulb, all, page, search)));
    }

    private void confirmRemove(Player player, UUID id, boolean bulb, boolean all, int page, String search) {
        BaseGroup group = resolve(player, id, bulb);
        if (group == null) return;
        if (!player.hasPermission("wirelessredstone.remove")) {
            player.sendMessage(Component.text("You don't have permission to remove groups.", NamedTextColor.RED));
            edit(player, id, bulb, all, page, search);
            return;
        }
        show(player, "Remove group?", List.of(DialogBody.plainMessage(Component.text(group.getDisplayName(), NamedTextColor.RED))),
                List.of(), List.of(button(Component.text("Remove permanently", NamedTextColor.RED), "Confirm removal", 150, p -> {
                    if (resolve(p, id, bulb) == null || !p.hasPermission("wirelessredstone.remove")) return;
                    if (bulb) bulbs.removeGroup(id); else chests.removeGroup(id);
                    p.sendMessage(Component.text("Group removed.", NamedTextColor.GREEN));
                    open(p, all, page, search);
                })), 1, button(Component.text("Cancel"), "Keep group", 100, p -> edit(p, id, bulb, all, page, search)));
    }

    private void teleport(Player player, UUID id, boolean bulb, int slot) {
        BaseGroup group = resolve(player, id, bulb);
        if (group == null || !player.hasPermission("wirelessredstone.teleport")) return;
        Location location = group.getLocation(slot);
        if (location == null) return;
        Location target = location.clone().add(0.5, 1, 0.5);
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.teleport(target);
    }

    private void giveExistingTool(Player player, UUID id, boolean bulb) {
        BaseGroup group = resolve(player, id, bulb);
        if (group == null) return;
        give(player, ConnectorToolFactory.createConnectorTool(id, group.getDisplayName(),
                bulb ? ConnectorToolFactory.GroupType.BULB : ConnectorToolFactory.GroupType.CHEST,
                bulb ? WireViewManager.getBulbGroupTextColor(id, bulbs.getAllPlacedGroups())
                        : WireViewManager.getChestGroupTextColor(id, chests.getAllPlacedGroups())));
        player.sendMessage(Component.text("Circuit Tool received for " + group.getDisplayName() + ".", NamedTextColor.GREEN));
    }

    private void give(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() < 0) player.getWorld().dropItemNaturally(player.getLocation(), item);
        else player.getInventory().addItem(item);
    }

    private void save(boolean bulb) { if (bulb) bulbs.saveData(); else chests.saveData(); }

    private ActionButton button(Component label, String tooltip, int width, Consumer<Player> action) {
        return button(label, Component.text(tooltip), width, action);
    }

    private ActionButton button(Component label, Component tooltip, int width, Consumer<Player> action) {
        return button(label, tooltip, width, (player, view) -> action.accept(player));
    }

    private ActionButton button(Component label, String tooltip, int width,
                                java.util.function.BiConsumer<Player, io.papermc.paper.dialog.DialogResponseView> action) {
        return button(label, Component.text(tooltip), width, action);
    }

    private ActionButton button(Component label, Component tooltip, int width,
                                java.util.function.BiConsumer<Player, io.papermc.paper.dialog.DialogResponseView> action) {
        return ActionButton.builder(label).tooltip(tooltip).width(width)
                .action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player player && player.isOnline()) action.accept(player, view);
                }, ClickCallback.Options.builder().uses(1).build())).build();
    }

    private void show(Player player, String title, List<DialogBody> body,
                      List<io.papermc.paper.registry.data.dialog.input.DialogInput> inputs,
                      List<ActionButton> actions, int columns, ActionButton exit) {
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title)).body(body).inputs(inputs).build())
                .type(DialogType.multiAction(actions).columns(columns).exitAction(exit).build())));
    }
}
