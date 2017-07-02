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
import java.util.Map;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.event.ConnectionEvent;

import com.sun.mail.pop3.POP3Store;

import static com.github.golovnin.javamail.mock3.MockMailbox.isInbox;

public class POP3MockStore extends POP3Store {

    private volatile boolean connected;
    private MockMailbox mailbox;

    public POP3MockStore(final Session session, final URLName urlname) {
        this(session, urlname, "pop3", false);

    }

    public POP3MockStore(final Session session, final URLName url, final String name, final boolean isSSL) {
        super(session, url, name, isSSL);
    }

    @Override
    public Map<String, String> capabilities() throws MessagingException {
        return Collections.emptyMap();
    }

    private synchronized void checkConnected() throws MessagingException {
        if (!isConnected()) {
            throw new MessagingException("Not connected");
        }
    }

    @Override
    public synchronized void close() throws MessagingException {
        connected = false;
        mailbox = null;
        notifyConnectionListeners(ConnectionEvent.CLOSED);
    }

    @Override
    public synchronized void connect() throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(url.getHost(), url.getPort(), url.getUsername(), url.getPassword());
    }

    @Override
    public synchronized void connect(final String user, final String password) throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(url.getHost(), url.getPort(), user, password);
    }

    @Override
    public synchronized void connect(final String host, final String user, final String password) throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(host, url.getPort(), user, password);
    }

    @Override
    public synchronized Folder getDefaultFolder() throws MessagingException {
        checkConnected();
        return super.getDefaultFolder();
    }

    @Override
    public synchronized Folder getFolder(final String name) throws MessagingException {
        checkConnected();
        if (isInbox(name)) {
            return new POP3MockFolder(this, mailbox.getInbox());
        }

        return new POP3MockFolder(this, new MailboxFolder(name, mailbox, false));

    }

    @Override
    public synchronized Folder getFolder(final URLName url) throws MessagingException {
        checkConnected();
        return getFolder(url.getFile());
    }

    synchronized Session getSession() {
        return session;
    }

    @Override
    public synchronized boolean isConnected() {
        return connected;
    }

    @Override
    protected synchronized boolean protocolConnect(String host,
        int port, String user, String password
    ) throws MessagingException {
        mailbox = MockMailbox.get(user);
        if (mailbox.getInbox().isSimulateError()) {
            throw new MessagingException("Simulated error connecting to mailbox of " + user);
        }
        this.connected = true;
        return true;
    }

}
