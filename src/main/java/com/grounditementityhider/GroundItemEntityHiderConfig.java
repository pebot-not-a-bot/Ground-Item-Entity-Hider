package com.grounditementityhider;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ground_item_entity_hider")
public interface GroundItemEntityHiderConfig extends Config
{
	@ConfigItem(
		keyName = "hiddenItemNames",
		name = "Hidden item names",
		description = "Comma-separated item names to hide. Use * as a wildcard, for example *monkey*"
	)
	default String hiddenItemNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiddenItemNames",
		name = "",
		description = ""
	)
	void setHiddenItemNames(String names);
}
