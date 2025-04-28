package com.wiqer.redis.util;

import java.lang.management.ManagementFactory;

import java.util.Calendar;

public class WinterId implements Uid {

    private static final char[] pid;
    private static volatile WinterId INSTANCE;

    static {
        pid = (ManagementFactory.getRuntimeMXBean().getName().split("@")[0] + "_").toCharArray();
    }

    private Sequencer sequencer;

    private WinterId() {
        sequencer = new Sequencer();
    }

    public static final WinterId instance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (WinterId.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new WinterId();
            return INSTANCE;
        }
    }


    @Override
    public byte[] generateBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String generate() {
        return sequencer.next();
    }

    @Override
    public long generateLong() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String generateDigits() {
        return generate();
    }

    class Sequencer {
        final StringBuilder cache = new StringBuilder();
        int sequence;
        long lastTime;
        int pidLength;
        Calendar now = Calendar.getInstance();

        public Sequencer() {
            cache.append(pid);
            pidLength = pid.length;
        }

        synchronized String next() {
            long time = System.currentTimeMillis();
            if (time > lastTime) {
                sequence = 0;
                lastTime = time;
            } else {
                sequence += 1;
            }
            now.setTimeInMillis(time);
            format(cache, now, sequence);
            String result = cache.toString();
            cache.setLength(pidLength);
            return result;
        }

        void format(StringBuilder cache, Calendar now, int sequence) {
            int year = now.get(Calendar.YEAR);
            cache.append(year);
            int month = now.get(Calendar.MONTH) + 1;
            if (month >= 10) {
                cache.append(month);
            } else {
                cache.append('0').append(month);
            }
            int dayInMonth = now.get(Calendar.DAY_OF_MONTH);
            if (dayInMonth >= 10) {
                cache.append(dayInMonth);
            } else {
                cache.append(0).append(dayInMonth);
            }
            int hour = now.get(Calendar.HOUR_OF_DAY);
            if (hour >= 10) {
                cache.append(hour);
            } else {
                cache.append('0').append(hour);
            }
            int minute = now.get(Calendar.MINUTE);
            if (minute >= 10) {
                cache.append(minute);
            } else {
                cache.append('0').append(minute);
            }
            int seconds = now.get(Calendar.SECOND);
            if (seconds >= 10) {
                cache.append(seconds);
            } else {
                cache.append('0').append(seconds);
            }
            int millSeconds = now.get(Calendar.MILLISECOND);
            if (millSeconds >= 100) {
                cache.append(millSeconds);
            } else if (millSeconds >= 10) {
                cache.append(0).append(millSeconds);
            } else {
                cache.append(0).append(0).append(millSeconds);
            }
            cache.append('_').append(sequence);
        }
    }
}
