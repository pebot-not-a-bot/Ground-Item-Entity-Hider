package com.grounditementityhider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.List;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;

public class GroundItemEntityHiderPluginTest
{
	@Test
	public void hiddenItemNamesSupportCommaSeparatedWildcards()
	{
		List<String> patterns = GroundItemEntityHiderPlugin.parseHiddenItemNames("Monkey corpse, Rune*, *platebody");

		assertTrue(GroundItemEntityHiderPlugin.matchesHiddenItemName("Monkey corpse", patterns));
		assertTrue(GroundItemEntityHiderPlugin.matchesHiddenItemName("Rune scimitar", patterns));
		assertTrue(GroundItemEntityHiderPlugin.matchesHiddenItemName("Adamant platebody", patterns));
		assertFalse(GroundItemEntityHiderPlugin.matchesHiddenItemName("Dragon dagger", patterns));
	}

	@Test
	public void blankEntriesAreIgnoredAndWhitespaceIsTrimmed()
	{
		List<String> patterns = GroundItemEntityHiderPlugin.parseHiddenItemNames("  , Monkey corpse , , *platebody  , ");

		assertEquals(2, patterns.size());
		assertTrue(GroundItemEntityHiderPlugin.matchesHiddenItemName("Monkey corpse", patterns));
		assertTrue(GroundItemEntityHiderPlugin.matchesHiddenItemName("Adamant platebody", patterns));
		assertFalse(GroundItemEntityHiderPlugin.matchesHiddenItemName("Dragon dagger", patterns));
	}

	@Test
	public void emptyPatternListsDoNotMatchAnything()
	{
		assertFalse(GroundItemEntityHiderPlugin.matchesHiddenItemName("Monkey corpse", Collections.emptyList()));
		assertFalse(GroundItemEntityHiderPlugin.matchesHiddenItemName("  ", Collections.singletonList("*corpse")));
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GroundItemEntityHiderPlugin.class);
		RuneLite.main(args);
	}
}