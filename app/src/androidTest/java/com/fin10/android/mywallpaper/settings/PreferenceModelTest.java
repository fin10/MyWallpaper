package com.fin10.android.mywallpaper.settings;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fin10.android.mywallpaper.model.WallpaperDatabase;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PreferenceModelTest {

    private static final String TEST_KEY = "_test_key";
    private static final String TEST_VALUE = "_test_value";

    private boolean initialized = false;

    @Before
    public void setUp() throws Exception {
        if (!initialized) {
            WallpaperDatabase.init(InstrumentationRegistry.getTargetContext());
            initialized = true;
        }
    }

    @After
    public void tearDown() throws Exception {
        SQLite.delete(PreferenceModel.class)
                .where(PreferenceModel_Table.key.eq(TEST_KEY))
                .execute();
    }

    @Test
    public void testSetValue() throws Exception {
        Assert.assertNull(PreferenceModel.getValue(TEST_KEY));
        PreferenceModel.setValue(TEST_KEY, TEST_VALUE);

        PreferenceModel model = SQLite.select()
                .from(PreferenceModel.class)
                .where(PreferenceModel_Table.key.eq(TEST_KEY))
                .querySingle();
        Assert.assertEquals(TEST_VALUE, model.mValue);
    }

    @Test
    public void testGetValue() throws Exception {
        Assert.assertNull(PreferenceModel.getValue(TEST_KEY));

        PreferenceModel model = new PreferenceModel();
        model.mKey = TEST_KEY;
        model.mValue = TEST_VALUE;
        model.insert();

        Assert.assertEquals(TEST_VALUE, PreferenceModel.getValue(TEST_KEY));
    }
}
