/*
 * Copyright (C)      2023 - Axel Strübing
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C) ?   -2022 - kwhitefoot
 * and Contributors.
 *
 * This file is part of ImapNotes3.
 *
 * ImapNotes3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.niendo.ImapNotes3.Data;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by kj on 11/1/16.
 * <p>
 * Use this instead of integers in the account configuration.  Store the name of the security type
 * instead.
 * <p>
 * The items annotated with @SuppressWarnings("unused") are used but only by code that calls the
 * from method so the analyser cannot see that they are used
 */
public enum Security {
    None("None", "", "imap", false),
    @SuppressWarnings("unused")
    SSL_TLS("SSL/TLS", "993", "imaps", false),
    @SuppressWarnings("unused")
    SSL_TLS_accept_all_certificates("SSL/TLS (accept all certificates)", "993", "imaps", true),
    @SuppressWarnings("unused")
    STARTTLS("STARTTLS", "143", "imap", false),
    @SuppressWarnings("unused")
    STARTTLS_accept_all_certificates("STARTTLS (accept all certificates)", "143", "imap", true);

    @NonNull
    static final String TAG = "IN_Security";
    // Mapping from integer.  See http://dan.clarke.name/2011/07/enum-in-java-with-int-conversion/
    @SuppressLint("UseSparseArrays") // False positive, SparseArray cannot be used here.
    private static final Map<Integer, Security> _map = new HashMap<>();

    static {
        for (Security security : Security.values())
            _map.put(security.ordinal(), security);
    }

    public final String proto;
    public final boolean acceptcrt;
    public final String defaultPort;
    private final String printable;

    Security(String printable,
             String defaultPort,
             String proto,
             boolean acceptcrt) {
        this.printable = printable;
        this.defaultPort = defaultPort;
        this.proto = proto;
        this.acceptcrt = acceptcrt;
    }

    @NonNull
    public static List<String> Printables() {
        List<String> list = new ArrayList<>();
        for (Security e : Security.values()) {
            list.add(e.printable);
        }
        return list;
    }

    @NonNull
    public static Security from(int ordinal) {
        return _map.get(ordinal);
    }

    @NonNull
    public static Security from(String name) {
        Log.d(TAG, "from: <" + name + ">");
        for (Security security : Security.values()) {
            if (Objects.equals(security.name(), name)) {
                return security;
            }
        }
        // Wasn't recognized, try using the ordinal instead, backwards compatibility.
        int i = Integer.parseInt(name);
        return from(i);
    }


}
