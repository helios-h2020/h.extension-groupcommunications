package eu.h2020.helios_social.modules.groupcommunications;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosEgoTag;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
	@Test
	public void useAppContext() {
		// Context of the app under test.

		String[] tags = new String[10000];
		for (int i = 0; i < tags.length; i++) {
			tags[i] = "a" + i;
		}

		String[] peers = new String[200000];
		for (int i = 0; i < peers.length; i++) {
			peers[i] = "user" + i;
		}

		LinkedList<HeliosEgoTag> egoTags = new LinkedList<>();
		Random random = new Random();

		for (String peer : peers) {
			HeliosEgoTag egoTag = new HeliosEgoTag(null, peer, "a10000",
					System.currentTimeMillis());

			egoTags.add(egoTag);

			for (int i = 0; i < 20; i++) {
				HeliosEgoTag randomEgoTag =
						new HeliosEgoTag(null, peer,
								"a" + random.nextInt(10000),
								System.currentTimeMillis());
				egoTags.add(randomEgoTag);
			}
		}

		long start = System.currentTimeMillis();

		List<HeliosEgoTag>
				filtered = egoTags.stream().filter(t -> {
			return t.getTag().equals("a999");
		}).collect(
				Collectors.toList());

		long end = System.currentTimeMillis();

		System.out.println(
				filtered.size() + " miliseconds " + (end - start));
		System.out.println("all tags size: " + egoTags.size());
		Context appContext =
				InstrumentationRegistry.getInstrumentation().getTargetContext();
		assertEquals("eu.h2020.helios_social.modules.groupcommunications.test",
				appContext.getPackageName());
	}
}
