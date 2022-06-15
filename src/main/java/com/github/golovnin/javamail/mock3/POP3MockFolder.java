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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.mail.FetchProfile;
import jakarta.mail.FolderClosedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.MethodNotSupportedException;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.event.MessageChangedEvent;

import com.sun.mail.pop3.POP3MockFolder0;

public class POP3MockFolder extends POP3MockFolder0 implements
    MailboxFolder.MailboxEventListener
{

    private final MailboxFolder mailboxFolder;
    private final UUID objectId = UUID.randomUUID();
    private volatile boolean opened;

    protected POP3MockFolder(POP3MockStore store, MailboxFolder mailboxFolder) {
        super(store);
        this.mailboxFolder = mailboxFolder;
        this.mailboxFolder.addMailboxEventListener(this);
    }

    private synchronized void checkClosed() {
        if (opened) {
            throw new IllegalStateException("This operation is not allowed on an open folder " + objectId);
        }
    }

    private synchronized void checkOpened() throws FolderClosedException {
        if (!opened) {
            throw new IllegalStateException("This operation is not allowed on a closed folder " + objectId);
        }
    }

    @Override
    public synchronized void close(boolean expunge) throws MessagingException {
        checkOpened();

        if (expunge) {
            mailboxFolder.expunge();
        }

        opened = false;

        notifyConnectionListeners(ConnectionEvent.CLOSED);
    }

    @Override
    public void fetch(Message[] msgs, FetchProfile fp) throws MessagingException {
        // just do nothing
    }

    @Override
    public void folderCreated(MailboxFolder mf) {
        // not valid for pop3
    }

    @Override
    public void folderDeleted(MailboxFolder mf) {
        // not valid for pop3
    }

    @Override
    public void folderRenamed(String from, MailboxFolder to) {
        // not valid for pop3
    }

    @Override
    public synchronized Message getMessage(int msgnum) throws MessagingException {
        checkOpened();
        return new MockMessage(mailboxFolder.getByMsgNum(msgnum), this);
    }

    @Override
    public synchronized int getMessageCount() throws MessagingException {
        return mailboxFolder.getMessageCount();
    }

    @Override
    public synchronized Message[] getMessages() throws MessagingException {
        checkOpened();
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= mailboxFolder.getMessageCount(); i++) {
            Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized Message[] getMessages(int low, int high) throws MessagingException {
        checkOpened();
        List<Message> messages = new ArrayList<>();
        for (int i = low; i <= high; i++) {
            Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized Message[] getMessages(int[] msgnums) throws MessagingException {
        checkOpened();

        List<Integer> idlist = new ArrayList<>();
        for (int value : msgnums) {
            idlist.add(value);
        }

        List<Message> messages = new ArrayList<>();

        for (int i = 1; i <= mailboxFolder.getMessageCount(); i++) {
            if (!idlist.contains(new Integer(i))) {
                continue;
            }

            Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized int getSize() throws MessagingException {
        checkOpened();
        return mailboxFolder.getSizeInBytes();
    }

    @Override
    public synchronized int[] getSizes() throws MessagingException {
        checkOpened();
        int count = getMessageCount();
        int[] sizes = new int[count];

        for (int i = 1; i <= count; i++) {
            sizes[i - 1] = getMessage(i).getSize();
        }

        return sizes;
    }

    @Override
    public synchronized String getUID(Message msg) throws MessagingException {
        checkOpened();
        return String.valueOf(((MockMessage) msg).getMockid());
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    @Override
    public InputStream listCommand() throws MessagingException, IOException {
        throw new MethodNotSupportedException();
    }

    @Override
    public void messageAdded(MailboxFolder mf, MockMessage msg) {
        // ignore
        // TODO JavaMail impl seems to not fire a event here for pop3, so we
        // ignore it
    }

    @Override
    public void messageChanged(MailboxFolder mf, MockMessage msg,
        boolean headerChanged, boolean flagsChanged
    ) {
        notifyMessageChangedListeners(MessageChangedEvent.FLAGS_CHANGED, msg);

    }

    @Override
    public void messageExpunged(MailboxFolder mf, MockMessage msg, boolean removed) {
        // not valid for pop3

    }

    @Override
    public synchronized void open(int mode) throws MessagingException {
        checkClosed();
        opened = true;
        notifyConnectionListeners(ConnectionEvent.OPENED);
    }

    @Override
    public void uidInvalidated() {
        // not valid for pop3
    }

}
