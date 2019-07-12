package com.yoloho.schedule.util;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

public class CronExpressionTest {
    @Test
    public void getNextValidTimeAfter() throws ParseException {
        CronExpression expression = new CronExpression("0 * * * * ?");
        Date date = expression.getNextValidTimeAfter(new Date());
        assertEquals(0, (date.getTime() / 1000) % 60);
        
        expression = new CronExpression("0 1 * * * ?");
        date = expression.getNextValidTimeAfter(new Date());
        assertEquals(60, (date.getTime() / 1000) % 3600);
    }
}
