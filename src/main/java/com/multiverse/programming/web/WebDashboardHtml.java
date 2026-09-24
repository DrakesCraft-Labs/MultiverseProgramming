// SPDX-License-Identifier: GPL-3.0-or-later
package com.multiverse.programming.web;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Embedded single-page web dashboard HTML loader.
 * Loads dashboard.html from classpath resources with high-performance caching.
 */
public final class WebDashboardHtml {

    private static final byte[] HTML_BYTES;
    public static final String DASHBOARD_HTML;

    static {
        byte[] bytes;
        try (InputStream in = WebDashboardHtml.class.getResourceAsStream("/dashboard.html")) {
            if (in != null) {
                bytes = in.readAllBytes();
            } else {
                bytes = "<!DOCTYPE html><html><body>Failed to load dashboard.</body></html>".getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            bytes = "<!DOCTYPE html><html><body>Failed to load dashboard.</body></html>".getBytes(StandardCharsets.UTF_8);
        }
        HTML_BYTES = bytes;
        DASHBOARD_HTML = new String(HTML_BYTES, StandardCharsets.UTF_8);
    }

    private WebDashboardHtml() {
    }

    public static byte[] getHtmlBytes() {
        return HTML_BYTES;
    }
}