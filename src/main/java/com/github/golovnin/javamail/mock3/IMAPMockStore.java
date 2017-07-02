/*
 *  JavaMail Mock3 Provider - open source mock classes for mock up JavaMail
 *  =======================================================================
 *
 *  Copyright (C) 2014 by Hendrik Saly (http://saly.de)
 *
 *  Based on ideas from Kohsuke Kawaguchi's Mock-javamail
 *  (https://java.net/projects/mock-javamail)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 */
package com.github.golovnin.javamail.mock3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.event.ConnectionEvent;

import com.sun.mail.imap.IMAPStore;

import static com.github.golovnin.javamail.mock3.MockMailbox.isInbox;

public class IMAPMockStore extends IMAPStore {

    private static final Folder[] EMPTY = new Folder[0];

    private boolean connected;
    private MockMailbox mailbox;

    public IMAPMockStore(Session session, URLName urlname) {
        this(session, urlname, "imap", false);
    }

    public IMAPMockStore(Session session, URLName url, String name, boolean isSSL) {
        super(session, url, name, isSSL);
    }

    private void checkConnected() throws MessagingException {
        if (!isConnected()) {
            throw new MessagingException("Not connected");
        }
    }

    @Override
    public synchronized void close() throws MessagingException {
        this.connected = false;
        notifyConnectionListeners(ConnectionEvent.CLOSED);
    }

    @Override
    public void connect() throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(url.getHost(), url.getPort(), url.getUsername(), url.getPassword());
    }

    @Override
    public void connect(String user, String password) throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(url.getHost(), url.getPort(), user, password);
    }

    @Override
    public void connect(String host, String user, String password) throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(host, url.getPort(), user, password);
    }

    @Override
    public Folder getDefaultFolder() throws MessagingException {
        checkConnected();

        return new IMAPDefaultMockFolder(this, mailbox);
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        checkConnected();
        if (isInbox(name)) {
            return new IMAPMockFolder(this, mailbox.getInbox());
        }
        return new IMAPMockFolder(this, mailbox.getRoot().getOrAddSubFolder(name));
    }

    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        checkConnected();
        return getFolder(url.getFile());
    }

    @Override
    public Folder[] getPersonalNamespaces() throws MessagingException {
        return new Folder[] { getDefaultFolder() };
    }

    @Override
    public Quota[] getQuota(String root) throws MessagingException {
        assertNoQuota();
        return null;
    }

    Session getSession() {
        return session;
    }

    @Override
    public Folder[] getSharedNamespaces() throws MessagingException {
        return EMPTY;
    }

    @Override
    public Folder[] getUserNamespaces(String user) {
        return EMPTY;
    }

    @Override
    public boolean hasCapability(String capability) throws MessagingException {
        if (capability == null) {
            return false;
        }
        String c = capability.toUpperCase(Locale.ROOT);
        return c.startsWith("IMAP4")
            || c.startsWith("IDLE")
            || c.startsWith("ID");
    }

    @Override
    public synchronized Map<String, String> id(Map<String, String> clientParams)
        throws MessagingException
    {
        checkConnected();
        Map<String, String> id = new HashMap<>();

        id.put("name", "JavaMail Mock3");
        id.put("vendor", "Andrej Golovnin");
        id.put("support-url", "https://github.com/golovnin/javamail-mock3/issues");

        return Collections.unmodifiableMap(id);
    }

    @Override
    public void idle() throws MessagingException {
        checkConnected();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    protected boolean protocolConnect(String host, int port, String user,
        String password) throws MessagingException
    {
        mailbox = MockMailbox.get(user);
        if (mailbox.getInbox().isSimulateError()) {
            throw new MessagingException("Simulated error connecting to mailbox of " + user);
        }

        this.connected = true;

        return true;
    }

    @Override
    public void setQuota(Quota quota) throws MessagingException {
        assertNoQuota();
    }

    private static void assertNoQuota() throws MessagingException {
        throw new MessagingException("QUOTA not supported");
    }

}
