package com.grounditementityhider;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemLayer;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.WildcardMatcher;

@PluginDescriptor(
	name = "Ground Item Entity Hider"
)
public class GroundItemEntityHiderPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "ground_item_entity_hider";
	private static final String LEGACY_CONFIG_GROUP = "hide_monkey_corpse";
	private static final String HIDDEN_ITEM_NAMES_KEY = "hiddenItemNames";

	@Inject
	private Client client;

	@Inject
	private GroundItemEntityHiderConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	private final Map<Integer, String> itemNameCache = new ConcurrentHashMap<>();
	private volatile List<String> hiddenItemPatterns = Collections.emptyList();

	private final RenderCallback renderCallback = new RenderCallback()
	{
		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			if (!(object instanceof ItemLayer))
			{
				return true;
			}

			ItemLayer itemLayer = (ItemLayer) object;
			return !isHiddenItem(itemLayer.getBottom())
				&& !isHiddenItem(itemLayer.getMiddle())
				&& !isHiddenItem(itemLayer.getTop());
		}
	};

	@Override
	protected void startUp() throws Exception
	{
		migrateLegacyConfig();
		updateHiddenItemPatterns();
		renderCallbackManager.register(renderCallback);
		clientThread.invokeLater(this::hideConfiguredItems);
	}

	@Override
	protected void shutDown() throws Exception
	{
		renderCallbackManager.unregister(renderCallback);
	}

	private boolean isHiddenItem(Renderable renderable)
	{
		return renderable instanceof TileItem && isHiddenItemId(((TileItem) renderable).getId());
	}

	private boolean isHiddenItemId(int itemId)
	{
		String itemName = itemNameCache.get(itemId);
		return itemName != null && matchesHiddenItemName(itemName, hiddenItemPatterns);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			hideConfiguredItems();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (CONFIG_GROUP.equals(configChanged.getGroup()))
		{
			updateHiddenItemPatterns();
			clientThread.invokeLater(this::hideConfiguredItems);
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		TileItem item = itemSpawned.getItem();
		if (item != null)
		{
			shouldHideItem(item.getId());
		}
	}

	private void updateHiddenItemPatterns()
	{
		hiddenItemPatterns = parseHiddenItemNames(config != null ? config.hiddenItemNames() : null);
	}

	private void migrateLegacyConfig()
	{
		if (configManager.getConfiguration(CONFIG_GROUP, HIDDEN_ITEM_NAMES_KEY) != null)
		{
			return;
		}

		String legacyValue = configManager.getConfiguration(LEGACY_CONFIG_GROUP, HIDDEN_ITEM_NAMES_KEY);
		if (legacyValue != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, HIDDEN_ITEM_NAMES_KEY, legacyValue);
		}
	}

	static List<String> parseHiddenItemNames(String entries)
	{
		if (entries == null || entries.trim().isEmpty())
		{
			return Collections.emptyList();
		}

		return Arrays.stream(entries.split(","))
			.map(entry -> entry.trim())
			.filter(entry -> !entry.isEmpty())
			.collect(java.util.stream.Collectors.toList());
	}

	static boolean matchesHiddenItemName(String itemName, List<String> patterns)
	{
		if (itemName == null || patterns == null || patterns.isEmpty())
		{
			return false;
		}

		String normalizedItemName = itemName.trim();
		if (normalizedItemName.isEmpty())
		{
			return false;
		}

		return patterns.stream()
			.map(pattern -> pattern.trim())
			.filter(pattern -> !pattern.isEmpty())
			.anyMatch(pattern -> WildcardMatcher.matches(pattern, normalizedItemName));
	}

	private boolean shouldHideItem(int itemId)
	{
		if (hiddenItemPatterns.isEmpty())
		{
			return false;
		}

		String itemName = itemNameCache.computeIfAbsent(itemId, id ->
		{
			ItemComposition composition = itemManager.getItemComposition(id);
			return composition == null || composition.getName() == null ? "" : composition.getName();
		});
		return matchesHiddenItemName(itemName, hiddenItemPatterns);
	}

	private void hideConfiguredItems()
	{
		if (client == null || client.getTopLevelWorldView() == null)
		{
			return;
		}

		Scene scene = client.getTopLevelWorldView().getScene();
		if (scene == null || scene.getTiles() == null)
		{
			return;
		}

		Tile[][][] tiles = scene.getTiles();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}

					List<TileItem> groundItems = tile.getGroundItems();
					if (groundItems != null)
					{
						for (TileItem item : groundItems)
						{
							shouldHideItem(item.getId());
						}
					}

				}
			}
		}
	}

	@Provides
	GroundItemEntityHiderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GroundItemEntityHiderConfig.class);
	}
}
