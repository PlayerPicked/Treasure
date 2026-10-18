package com.playerpicked.treasure.text;

import org.jetbrains.annotations.NotNull;

public enum MessageKey {
    RELOADED("general.reloaded"),
    NO_PERMISSION("general.no-permission"),
    ACTION_FAILED("general.action-failed"),
    UPDATE_AVAILABLE("general.update-available"),

    TREASURE_NO_PERMISSION("treasure.no-permission"),
    TREASURE_ON_COOLDOWN("treasure.on-cooldown"),
    TREASURE_NOT_LOADED("treasure.not-loaded"),
    TREASURE_PROTECTED("treasure.protected"),
    TOAST_TITLE("treasure.toast-title"),

    LOOT_ITEM_TAKEN("loot.item-taken"),
    LOOT_INVENTORY_FULL("loot.inventory-full"),
    LOOT_SAVED("loot.saved"),
    LOOT_EDITOR_TITLE("loot.editor-title"),
    LOOT_PREVIEW_TITLE("loot.preview-title"),

    HOLOGRAM_READY("hologram.ready"),
    HOLOGRAM_TIME_LEFT("hologram.time-left"),
    HOLOGRAM_TOO_MANY_LINES("hologram.too-many-lines"),

    TREASURE_CREATED("setup.created"),
    TREASURE_DELETED("setup.deleted"),
    TREASURE_GONE("setup.gone"),
    NAME_SET("setup.name-set"),
    PERMISSION_SET("setup.permission-set"),
    PERMISSION_CLEARED("setup.permission-cleared"),
    COOLDOWN_SET("setup.cooldown-set"),
    SIZE_SET("setup.size-set"),
    PUSHBACK_SET("setup.pushback-set"),
    LOCATION_SET("setup.location-set"),
    LOCATION_OCCUPIED("setup.location-occupied"),
    LOCATION_PICK_START("setup.location-pick"),
    INVALID_DURATION("setup.invalid-duration"),
    INVALID_SIZE("setup.invalid-size"),
    INVALID_PUSHBACK("setup.invalid-pushback"),

    PROMPT_NAME("prompts.name"),
    PROMPT_PERMISSION("prompts.permission"),
    PROMPT_COOLDOWN("prompts.cooldown"),
    PROMPT_SIZE("prompts.size"),
    PROMPT_PUSHBACK("prompts.pushback"),
    PROMPT_HOLOGRAM_LINE("prompts.hologram-line"),
    PROMPT_HOW_TO_CANCEL("prompts.how-to-cancel"),
    PROMPT_CANCELLED("prompts.cancelled"),
    PROMPT_EXPIRED("prompts.expired"),

    TRACKER_GIVEN("tracker.given"),
    TRACKER_RECEIVED("tracker.received"),
    TRACKER_ACTION_BAR("tracker.action-bar"),
    TRACKER_ITEM_NAME("tracker.item.name"),
    TRACKER_ITEM_LORE("tracker.item.lore"),
    TRACKER_FORWARD("tracker.directions.forward"),
    TRACKER_BACKWARD("tracker.directions.backward"),
    TRACKER_LEFT("tracker.directions.left"),
    TRACKER_RIGHT("tracker.directions.right"),

    DISPLAY_ON("display.toggle.on"),
    DISPLAY_OFF("display.toggle.off"),
    DISPLAY_NONE("display.none"),
    DISPLAY_NOT_SET("display.not-set"),
    DISPLAY_LOCATION("display.location"),
    DISPLAY_BLANK_LINE("display.blank-line");

    private final String route;

    MessageKey(@NotNull String route) {
        this.route = route;
    }

    public @NotNull String route() {
        return route;
    }
}
