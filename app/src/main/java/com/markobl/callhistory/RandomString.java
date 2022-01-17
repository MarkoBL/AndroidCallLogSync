package com.markobl.callhistory;

import java.util.Random;

public class RandomString {
    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";
    private static final Random random = new Random();

    public static String getRandomString(final int sizeOfRandomString)
    {
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);

        for(int i=0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}

