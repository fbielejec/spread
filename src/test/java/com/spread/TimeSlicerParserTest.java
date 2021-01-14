package com.spread;

import org.junit.Test;

import jebl.evolution.io.ImportException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.spread.data.SpreadData;
import com.spread.exceptions.SpreadException;
import com.spread.data.Attribute;

import java.io.File;
import java.io.IOException;

import com.spread.parsers.TimeSlicerParser;
import com.spread.utils.ParsersUtils;

public class TimeSlicerParserTest {

    @Test
    public void runTest() throws IOException, ImportException, SpreadException {

        String mostRecentSamplingDate = "2021/01/12";
        double hpdLevel = 0.8;
        String path = "timeSlicer/WNV_small.trees";
        File treesfile = new File(getClass().getClassLoader().getResource(path).getFile());

        TimeSlicerParser parser = new TimeSlicerParser (treesfile.getAbsolutePath(),
                                                        1,
                                                        10,
                                                        "location",
                                                        "rate",
                                                        hpdLevel,
                                                        100,
                                                        mostRecentSamplingDate,
                                                        1.0);

        String json = parser.parse();
        Gson gson = new Gson();
        SpreadData data = gson.fromJson(json, SpreadData.class);

        assertEquals("returns correct mrsd", mostRecentSamplingDate, data.getTimeLine().getEndTime());
        assertEquals("returns correct root date", "2011/04/28", data.getTimeLine().getStartTime());

        Attribute hpdAreaAttribute = data.getAreaAttributes().stream().filter(att -> att.getId().equals(ParsersUtils.HPD.toUpperCase())).findAny().orElse(null);

        assertArrayEquals("returns correct HPD attribute range", new Double[]{hpdLevel, hpdLevel}, hpdAreaAttribute.getRange());
        assertTrue("Areas are generated", data.getLayers() .get(0).getAreas().size() > 0);

    }

}