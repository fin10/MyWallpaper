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

    private static final String KEY_FOR_TEST = "_test_key";
    private static final String VALUE_FOR_TEST = "_test_value";

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
                .where(PreferenceModel_Table.key.eq(KEY_FOR_TEST))
                .execute();
    }

    @Test
    public void testSetValue() throws Exception {
        Assert.assertNull(PreferenceModel.getValue(KEY_FOR_TEST));
        PreferenceModel.setValue(KEY_FOR_TEST, VALUE_FOR_TEST);

        PreferenceModel model = SQLite.select()
                .from(PreferenceModel.class)
                .where(PreferenceModel_Table.key.eq(KEY_FOR_TEST))
                .querySingle();
        Assert.assertEquals(VALUE_FOR_TEST, model.mValue);
    }

    @Test
    public void testGetValue() throws Exception {
        Assert.assertNull(PreferenceModel.getValue(KEY_FOR_TEST));

        PreferenceModel model = new PreferenceModel();
        model.mKey = KEY_FOR_TEST;
        model.mValue = VALUE_FOR_TEST;
        model.insert();

        Assert.assertEquals(VALUE_FOR_TEST, PreferenceModel.getValue(KEY_FOR_TEST));
    }
}
